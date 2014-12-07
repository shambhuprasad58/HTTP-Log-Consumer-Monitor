/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httplogmonitor;

import httplogmonitorutil.Alert;
import httplogmonitorutil.HttpObject;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author root
 */
public class AlertTracker implements Runnable
{
    private LinkedBlockingQueue<HttpObject> alertURLQueue;
    private LinkedBlockingQueue<Alert> alertQueue;
    private int threshold;
    private long alertGapInMillisecond;

    public AlertTracker(LinkedBlockingQueue<HttpObject> alertURLQueue, int threshold, LinkedBlockingQueue<Alert> alertQueue, long alertGapInMillisecond) 
    {
        this.alertURLQueue = alertURLQueue;
        this.threshold = threshold;
        this.alertQueue = alertQueue;
        this.alertGapInMillisecond = alertGapInMillisecond;
    }
    
    @Override
    public void run() 
    {
        while(true)
        {
            try 
            {
                HttpObject lastURL = alertURLQueue.element();
                long diff = new Date().getTime() - lastURL.getHittingTime().getTime();
                if(diff < (alertGapInMillisecond-100))                                          //100 milliseconds early wake up
                    Thread.sleep(alertGapInMillisecond - diff -100);
                if(alertURLQueue.size() <= threshold)
                    alertQueue.put(new Alert(new Date(), alertURLQueue.size(), false));
                alertURLQueue.remove();
            } catch (InterruptedException ex) {
                Logger.getLogger(AlertTracker.class.getName()).log(Level.SEVERE, null, ex);
            }
            catch(NoSuchElementException ex)
            {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex1) {
                    Logger.getLogger(AlertTracker.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
    }
}
