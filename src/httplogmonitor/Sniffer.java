/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httplogmonitor;

import httplogmonitorutil.Alert;
import httplogmonitorutil.HttpObject;
import httplogmonitorutil.Statistics;
import httplogmonitorutil.UserPreferences;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author root
 */
public class Sniffer implements Runnable{
    
    LinkedBlockingQueue<UserPreferences> preferenceQueue;
    LinkedBlockingQueue<HttpObject> mostHitsURLQueue;
    LinkedBlockingQueue<ArrayList<HttpObject>> mostHitsTopURL;
    LinkedBlockingQueue<HttpObject> alertURLQueue;
    LinkedBlockingQueue<Alert> alertQueue;
    LinkedBlockingQueue<Statistics> statsQueue;
    int threshold;
    String logFile = "";
    long timeGapInMillisecond = 0;
    long alertGapInMillisecond = 0;
    
    public Sniffer(LinkedBlockingQueue<UserPreferences> preferenceQueue, LinkedBlockingQueue<HttpObject> mostHitsURLQueue, LinkedBlockingQueue<HttpObject> alertURLQueue, LinkedBlockingQueue<Alert> alertQueue, LinkedBlockingQueue<Statistics> statsQueue, LinkedBlockingQueue<ArrayList<HttpObject>> mostHitsTopURL)
    {
        this.preferenceQueue = preferenceQueue;
        this.mostHitsURLQueue = mostHitsURLQueue;
        this.mostHitsTopURL = mostHitsTopURL;
        this.alertURLQueue = alertURLQueue;
        this.alertQueue = alertQueue;
        this.statsQueue = statsQueue;
    }
    
    @Override
    public void run()
    {
        Thread loopThread = null;
        Thread alertThread = null;
        TimerTask mostHitsTimerTask = null;
        Timer mostHitsTimer = null;
        while(true)
        {
            UserPreferences preference;
            try {
                preference = preferenceQueue.take();
            if(!preferenceQueue.isEmpty())
                continue;
            if(!preference.getLogFile().equals(logFile))
            {
                restartLogging(preference.getLogFile(), loopThread);
            }
            if(preference.getTimeGapInMillisecond() != timeGapInMillisecond)
            {
                restartMostHitsCalculator(preference.getTimeGapInMillisecond(), mostHitsTimer, mostHitsTimerTask);
            }
            if(preference.getAlertGapInMillisecond() != alertGapInMillisecond || preference.getThreshold() != this.threshold)
            {
                restartAlertTracker(preference.getAlertGapInMillisecond(), preference.getThreshold(), alertThread);
            }
            } catch (InterruptedException ex) {
                Logger.getLogger(Sniffer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void restartLogging(String newLogFile, Thread loopThread)
    {
        logFile = newLogFile;
        if(loopThread != null)
            loopThread.interrupt();
        loopThread = new Thread(new RunLoop());
        loopThread.start();
    }
    
    private void restartMostHitsCalculator(long newTimeGapInMillisecond, Timer mostHitsTimer, TimerTask mostHitsTimerTask)
    {
        this.timeGapInMillisecond = newTimeGapInMillisecond;
        if(mostHitsTimer != null)
            mostHitsTimer.cancel();
        mostHitsTimerTask = new MostHitsCalculator(mostHitsURLQueue, mostHitsTopURL);
        mostHitsTimer = new Timer(true);
        mostHitsTimer.scheduleAtFixedRate(mostHitsTimerTask, 0, timeGapInMillisecond);
    }
    
    private void restartAlertTracker(long newAlertGapInMillisecond, int newThreshold, Thread alertThread)
    {
        alertGapInMillisecond = newAlertGapInMillisecond;
        this.threshold = newThreshold;
        if(alertThread != null)
            alertThread.interrupt();
        alertThread = new Thread(new AlertTracker(alertURLQueue, threshold, alertQueue, alertGapInMillisecond));
        alertThread.start();
    }
    
    private class RunLoop implements Runnable
    {
        
        @Override
        public void run()
        {
            try {
                LogReader logReader = new LogReader(logFile, Integer.MAX_VALUE/10000, mostHitsURLQueue, alertURLQueue, statsQueue, alertQueue, threshold);
                logReader.reader();
            } catch (IOException ex) {
                Logger.getLogger(Sniffer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(Sniffer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
