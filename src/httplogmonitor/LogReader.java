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
 * @author shambhu
 */
public class LogReader extends Thread{
    private static final Logger logger = Logger.getLogger("LogReader");
    private String logFile;
    private final File file;
    private long offset;
    private int lineCount;
    private boolean ended;
    private WatchService watchService; 
    private ArrayDeque<String> lines;
    private LinkedBlockingQueue<HttpObject> mostHitsURLQueue;
    private LinkedBlockingQueue<HttpObject> alertURLQueue;
    private LinkedBlockingQueue<Statistics> statsQueue;
    private LinkedBlockingQueue<Alert> alertQueue;
    private Statistics stats;
    private int threshold;

    /**
     * Allows output of a file that is being updated by another process.
     * @param file to watch and read
     * @param maxTimeToWaitInSeconds max timeout; after this long without changes,
     * watching will stop. If =0, watch will continue until <code>stop()</code>
     * is called.
     */
    public LogReader(String logFile, LinkedBlockingQueue<HttpObject> mostHitsURLQueue, LinkedBlockingQueue<HttpObject> alertURLQueue, 
            LinkedBlockingQueue<Statistics> statsQueue, LinkedBlockingQueue<Alert> alertQueue, int threshold) throws IOException {
        this.offset = 0;
        this.lineCount = 0;
        this.ended = false;
        this.watchService = null; 
        this.lines = new ArrayDeque<>();
        this.logFile = logFile;
        this.file = new File(logFile);
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
     * Stop watch.
     */
    private void stopWatchService() {
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

    private class FileWatcher extends Thread{
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
    
    @Override
    public void run() {
        Thread statsThread = new Thread(new SendStats());
        statsThread.start();
        updateOffset();
        new Thread(new FileWatcher()).start();  
        while ( ! this.isInterrupted()) {
            while (linesAvailable()) {
                try {
                    HttpObject newLog = new HttpObject();
                    newLog = newLog.parseLog(getLine());
                    if(newLog == null)
                        continue;
                    updateStats(newLog);
                    mostHitsURLQueue.put(newLog);
                    alertURLQueue.put(newLog);
                    int alertURLQueueSize = alertURLQueue.size();
                    if(alertURLQueueSize > threshold)
                        alertQueue.put(new Alert(new Date(), alertURLQueueSize, true));
                } catch (InterruptedException ex) {
                    Logger.getLogger(LogReader.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(LogReader.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
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
