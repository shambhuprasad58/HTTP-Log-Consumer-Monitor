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
 * @author shambhu
 */
public class Sniffer implements Runnable{
    
    private LinkedBlockingQueue<UserPreferences> preferenceQueue;
    private LinkedBlockingQueue<HttpObject> mostHitsURLQueue;
    private LinkedBlockingQueue<ArrayList<HttpObject>> mostHitsTopURL;
    private LinkedBlockingQueue<HttpObject> alertURLQueue;
    private LinkedBlockingQueue<Alert> alertQueue;
    private LinkedBlockingQueue<Statistics> statsQueue;
    private int threshold;
    private String logFile;
    private long timeGapInMillisecond;
    private long alertGapInMillisecond;
    
    public Sniffer(LinkedBlockingQueue<UserPreferences> preferenceQueue, LinkedBlockingQueue<HttpObject> mostHitsURLQueue, 
            LinkedBlockingQueue<HttpObject> alertURLQueue, LinkedBlockingQueue<Alert> alertQueue, LinkedBlockingQueue<Statistics> statsQueue, 
                    LinkedBlockingQueue<ArrayList<HttpObject>> mostHitsTopURL)
    {
        this.preferenceQueue = preferenceQueue;
        this.mostHitsURLQueue = mostHitsURLQueue;
        this.mostHitsTopURL = mostHitsTopURL;
        this.alertURLQueue = alertURLQueue;
        this.alertQueue = alertQueue;
        this.statsQueue = statsQueue;
        this.threshold = -1;
        this.timeGapInMillisecond = -1;
        this.alertGapInMillisecond = -1;
        this.logFile = "";
    }
    
    @Override
    public void run()
    {
        Thread logThread = null;
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
            if(preference.getAlertGapInMillisecond() != alertGapInMillisecond || preference.getThreshold() != this.threshold)
            {
                restartAlertTracker(preference.getAlertGapInMillisecond(), preference.getThreshold(), alertThread);
            }
            if(!preference.getLogFile().equals(logFile) || preference.getThreshold() != this.threshold)
            {
                restartLogging(preference.getLogFile(), logThread, preference.getThreshold());
            }
            if(preference.getTimeGapInMillisecond() != timeGapInMillisecond)
            {
                restartMostHitsCalculator(preference.getTimeGapInMillisecond(), mostHitsTimer, mostHitsTimerTask);
            }
            } catch (InterruptedException ex) {
                Logger.getLogger(Sniffer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Sniffer.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
    }
    
    private void restartLogging(String newLogFile, Thread logThread, int newThreshold) throws IOException
    {
        logFile = newLogFile;
        this.threshold = newThreshold;
        if(logThread != null)
            logThread.interrupt();
        LogReader logReader = new LogReader(logFile, mostHitsURLQueue, alertURLQueue, statsQueue, alertQueue, threshold);
        logThread = new Thread(logReader);
        logThread.start();
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
        if(alertThread != null)
            alertThread.interrupt();
        alertThread = new Thread(new AlertTracker(alertURLQueue, newThreshold, alertQueue, alertGapInMillisecond));
        alertThread.start();
    }
}
