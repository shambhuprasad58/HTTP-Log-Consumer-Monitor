/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httplogmonitor;

import httplogmonitorui.HomeFrame;
import httplogmonitorutil.Alert;
import httplogmonitorutil.HttpObject;
import httplogmonitorutil.Statistics;
import httplogmonitorutil.UserPreferences;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shambhu
 */
public class HTTPLogConsumerMonitor {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        LinkedBlockingQueue<UserPreferences> preferenceQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<HttpObject> mostHitsURLQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<ArrayList<HttpObject>> mostHitsTopURL = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<HttpObject> alertURLQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<Alert> alertQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<Statistics> statsQueue = new LinkedBlockingQueue<>();
        Sniffer sniffer = new Sniffer(preferenceQueue, mostHitsURLQueue, alertURLQueue, alertQueue, statsQueue, mostHitsTopURL);
        Thread snifferThread = new Thread(sniffer);
        snifferThread.start();
        HomeFrame.begin(preferenceQueue, alertQueue, statsQueue, mostHitsTopURL);
//        Thread displayUIThread = new Thread(new DisplayUI(preferenceQueue, alertQueue, statsQueue, mostHitsTopURL));
//        displayUIThread.start();
        try 
        {
            snifferThread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(HTTPLogConsumerMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static class DisplayUI implements Runnable
    {
        private LinkedBlockingQueue<UserPreferences> preferenceQueue;
        private LinkedBlockingQueue<ArrayList<HttpObject>> mostHitsTopURL;
        private LinkedBlockingQueue<Alert> alertQueue;
        private LinkedBlockingQueue<Statistics> statsQueue;
        
        public DisplayUI(LinkedBlockingQueue<UserPreferences> preferenceQueue, LinkedBlockingQueue<Alert> alertQueue, 
                LinkedBlockingQueue<Statistics> statsQueue, LinkedBlockingQueue<ArrayList<HttpObject>> mostHitsTopURL)
        {
            this.preferenceQueue = preferenceQueue;
            this.mostHitsTopURL = mostHitsTopURL;
            this.alertQueue = alertQueue;
            this.statsQueue = statsQueue;
        }
        @Override
        public void run() {
                HomeFrame.begin(preferenceQueue, alertQueue, statsQueue, mostHitsTopURL);
        }
        
    }
    
}
