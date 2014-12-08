/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httplogmonitorutil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shambhu
 */
public class HttpObject implements Comparable<HttpObject>{
    private String url;
    private Date hittingTime;
    private int hitCount;
    private long bytesDownloaded;
    private String host, method, path, status;
    private SimpleDateFormat formatter;
    
    public HttpObject()
    {
        formatter = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss Z");
    }
    
    public String getUrl()
    {
        return this.url;
    }
    public String getHost()
    {
        return this.host;
    }
    public String getMethod()
    {
        return this.method;
    }
    public String getPath()
    {
        return this.path;
    }
    public String getStatus()
    {
        return this.status;
    }
    public long getBytesDownloaded()
    {
        return this.bytesDownloaded;
    }
    public Date getHittingTime()
    {
        return this.hittingTime;
    }
    public int getHitCount()
    {
        return this.hitCount;
    }
    public void setHitCount(int hitCount)
    {
        this.hitCount = hitCount;
    }
    public HttpObject parseLog(String log)
    {
        try
        {
            String[] parts = log.split(" ");
            if(!parts[0].equals("-"))
                this.host = parts[0];
            String dateTime = parts[3]+" "+parts[4];
            dateTime = dateTime.replaceAll("\\[", "");
            dateTime = dateTime.replaceAll("\\]", "");
            try {
                this.hittingTime = formatter.parse(dateTime);
            } catch (ParseException ex) {
                Logger.getLogger(HttpObject.class.getName()).log(Level.SEVERE, null, ex);
                this.hittingTime = new Date();
            }
            this.method = parts[5].replace("\"", "");
            this.path = parts[6];
            this.url = this.host+this.path;
            this.status = parts[8];
            try
            {
                this.bytesDownloaded = Long.parseLong(parts[9]);
            }
            catch(Exception ex)
            {
                this.bytesDownloaded = 0;
            }
            return this;
        }
        catch(Exception ex)
        {
            Logger.getLogger(HttpObject.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Skipped a badly formatted log.");
            return null;
        }
    }

    @Override
    public int compareTo(HttpObject o) {
        return((Integer)o.getHitCount()).compareTo(this.hitCount);
    }
}
