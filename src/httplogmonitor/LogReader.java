/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httplogmonitor;

import httplogmonitorutil.Alert;
import httplogmonitorutil.HttpObject;
import httplogmonitorutil.Statistics;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author root
 */
public class LogReader {
    private static final Logger logger = Logger.getLogger("LogReader");
    private String logFile;
    private long tooLateTime = -1;
    private final long maxMsToWait;
    private final File file;
    private long offset = 0;
    private int lineCount = 0;
    private boolean ended = false;
    private WatchService watchService = null; 
    ArrayDeque<String> lines = new ArrayDeque<>();
    LinkedBlockingQueue<HttpObject> mostHitsURLQueue;
    LinkedBlockingQueue<HttpObject> alertURLQueue;
    LinkedBlockingQueue<Statistics> statsQueue;
    LinkedBlockingQueue<Alert> alertQueue;
    Statistics stats;
    int threshold;

    /**
     * Allows output of a file that is being updated by another process.
     * @param file to watch and read
     * @param maxTimeToWaitInSeconds max timeout; after this long without changes,
     * watching will stop. If =0, watch will continue until <code>stop()</code>
     * is called.
     */
    public LogReader(String logFile, long maxTimeToWaitInSeconds, LinkedBlockingQueue<HttpObject> mostHitsURLQueue, LinkedBlockingQueue<HttpObject> alertURLQueue, LinkedBlockingQueue<Statistics> statsQueue, LinkedBlockingQueue<Alert> alertQueue, int threshold) throws IOException {
        this.logFile = logFile;
        this.file = new File(logFile);
        this.maxMsToWait = maxTimeToWaitInSeconds * 1000;
        this.mostHitsURLQueue = mostHitsURLQueue;
        this.alertURLQueue = alertURLQueue;
        this.statsQueue = statsQueue;
        this.alertQueue = alertQueue;
        stats = new Statistics(0,0,0,0);
        this.threshold = threshold;
        skipOldLogs();
    }

    private void skipOldLogs() throws FileNotFoundException, IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(file));
        while (true) {
            String line = br.readLine();
            if (line != null) {
                offset += line.length() + 1; 
            } else {
                break;
            }
        }
        br.close();
    }
    /**
     * Start watch.
     */
    private void start() {
        updateOffset();
        // listens for FS events
        new Thread(new FileWatcher()).start();  
        if (maxMsToWait != 0) {
            // kills FS event listener after timeout
            new Thread(new WatchDog()).start();
        }     
    }

    /**
     * Stop watch.
     */
    private void stop() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ex) {
                logger.info("Error closing watch service");
            }
            watchService = null;
        }
    }

    private synchronized void updateOffset() {
        tooLateTime = System.currentTimeMillis() + maxMsToWait;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            br.skip(offset);            
            while (true) {
                String line = br.readLine();
                if (line != null) {
                    lines.push(line);
                    // this may need tweaking if >1 line terminator char
                    offset += line.length() + 1; 
                } else {
                    break;
                }
            }
            br.close();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error reading", ex);
        }        
    }

    /**
     * @return true if lines are available to read
     */
    private boolean linesAvailable() {
        return ! lines.isEmpty();
    }

    /**
     * @return next unread line
     */
    private synchronized String getLine() {
        if (lines.isEmpty()) {
            return null;
        } else {
            lineCount ++;
            return lines.removeLast();
        }
    }

    /**
     * @return true if no more lines will ever be available, 
     * because stop() has been called or the timeout has expired
     */
    private boolean hasEnded() {
        return ended;
    }

    /**
     * @return next line that will be returned; zero-based
     */
    private int getLineNumber() {
        return lineCount;
    }

    private class WatchDog implements Runnable {
        @Override
        public void run() {
            while (System.currentTimeMillis() < tooLateTime) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // do nothing
                }
            }
            stop();
        }
    }

    private class FileWatcher implements Runnable {
        private final Path path = file.toPath().getParent();
        @Override
        public void run() {
            try {
                watchService = path.getFileSystem().newWatchService();
                path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    WatchKey watchKey = watchService.take();
                    if ( ! watchKey.reset()) {
                        stop();
                        break;
                    } else if (! watchKey.pollEvents().isEmpty()) {
                        updateOffset();
                    }
                    Thread.sleep(500);
                }
            } catch (InterruptedException ex) {
                logger.info("Tail interrupted");
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Tail failed", ex);
            } catch (ClosedWatchServiceException ex) {
                // no warning required - this was a call to stop()
            }
            ended = true;
        }
    }

    public void reader() throws InterruptedException
    {
        Thread statsThread = new Thread(new SendStats());
        statsThread.start();
        start();
        while ( ! hasEnded()) {
            while (linesAvailable()) {
                HttpObject newLog = new HttpObject();
                newLog = newLog.parseLine(getLine());
                updateStats(newLog);
                mostHitsURLQueue.put(newLog);
                alertURLQueue.put(newLog);
                int alertURLQueueSize = alertURLQueue.size();
                if(alertURLQueueSize > threshold)
                    alertQueue.put(new Alert(new Date(), alertURLQueueSize, true));
            }
            Thread.sleep(500);
        }
    }
    private void updateStats(HttpObject newLog)
    {
        try
        {
            stats.setHitCount(stats.getHitCount()+1);
            stats.updateKiloBytesDownloaded(newLog.getBytesDownloaded());
            if(Integer.parseInt(newLog.getStatus()) < 400)
                stats.updateSuccessfulHits(1);
        }
        catch(Exception ex)
        {
            Logger.getLogger(Sniffer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    private class SendStats implements Runnable
    {

        @Override
        public void run() 
        {
            while(true)
            {
                try {
                    statsQueue.put(stats);
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Sniffer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
    }
}
