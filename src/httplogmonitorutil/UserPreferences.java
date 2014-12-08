/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httplogmonitorutil;

/**
 *
 * @author shambhu
 */
public class UserPreferences {
    private int threshold;
    private String logFile;
    private long timeGapInMillisecond, alertGapInMillisecond;
    
    public UserPreferences(){};
    public UserPreferences(int threshold, String logFile,long timeGapInMillisecond, long alertGapInMillisecond)
    {
        this.threshold = threshold;
        this.logFile = logFile;
        this.timeGapInMillisecond = timeGapInMillisecond;
        this.alertGapInMillisecond = alertGapInMillisecond;
    }
    
    public int getThreshold()
    {
        return this.threshold;
    }
    public String getLogFile()
    {
        return this.logFile;
    }
    public long getTimeGapInMillisecond()
    {
        return this.timeGapInMillisecond;
    }
    public long getAlertGapInMillisecond()
    {
        return this.alertGapInMillisecond;
    }
}
