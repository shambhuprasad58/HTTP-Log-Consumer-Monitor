/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alerttestpackage;

import httplogmonitor.AlertTracker;
import httplogmonitor.LogReader;
import httplogmonitorutil.Alert;
import httplogmonitorutil.HttpObject;
import httplogmonitorutil.Statistics;
import httplogmonitorutil.UserPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author root
 */
public class AlertLogicTest {
    
    private LinkedBlockingQueue<UserPreferences> preferenceQueue;
    private LinkedBlockingQueue<HttpObject> mostHitsURLQueue;
    private LinkedBlockingQueue<ArrayList<HttpObject>> mostHitsTopURL;
    private LinkedBlockingQueue<HttpObject> alertURLQueue;
    private LinkedBlockingQueue<Alert> alertQueue;
    private LinkedBlockingQueue<Statistics> statsQueue;
    private int threshold;
    private String logFile;
    private long alertGapInMillisecond;
    
    public AlertLogicTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() throws IOException {
        preferenceQueue = new LinkedBlockingQueue<>();
        mostHitsURLQueue = new LinkedBlockingQueue<>();
        mostHitsTopURL = new LinkedBlockingQueue<>();
        alertURLQueue = new LinkedBlockingQueue<>();
        alertQueue = new LinkedBlockingQueue<>();
        statsQueue = new LinkedBlockingQueue<>();
        threshold=100;
        this.alertGapInMillisecond = 5*1000;
        this.logFile = "testLog.txt";
        File file = new File(logFile);
        if(!file.exists())
        {
            file.createNewFile();
        }
        else
        {
            file.delete();
            file.createNewFile();
        }
        this.logFile = file.getAbsolutePath();
        new Thread(new LogReader(logFile, mostHitsURLQueue, alertURLQueue, statsQueue, alertQueue, threshold)).start();
        new Thread(new AlertTracker(alertURLQueue, threshold, alertQueue, alertGapInMillisecond)).start();
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void alertTest() throws IOException, InterruptedException
    {
        Thread.sleep(500);
        writeToFile(105);
        Thread.sleep(2*1000);
        boolean alert = false;
        while(!alertQueue.isEmpty())
        {
            Alert al = alertQueue.take();
            alert = alert || al.getAlertType();
        }
        assertTrue(alert);
        
        writeToFile(105);
        Thread.sleep(3*1000);
        while(!alertQueue.isEmpty())
        {
            Alert al = alertQueue.take();
            assertTrue(al.getAlertType());
        }
        
        Thread.sleep(3*1000);
        while(!alertQueue.isEmpty())
        {
            Alert al = alertQueue.take();
            alert = alert && al.getAlertType();
        }
        assertFalse(alert);
    }
    
    public void writeToFile(int logCount) throws IOException
    {
        HTTPLog log = new HTTPLog();
        FileOutputStream fileOutputStream = new FileOutputStream(logFile, true);
        FileLock lock = fileOutputStream.getChannel().lock();
        for(int i=0;i<logCount;i++)
        {
            String newLog = log.generateNewLog()+"\n";
            byte[] bytes = newLog.getBytes("UTF-8");
            fileOutputStream.write(bytes);
        }
        lock.release();
    }
    
}
