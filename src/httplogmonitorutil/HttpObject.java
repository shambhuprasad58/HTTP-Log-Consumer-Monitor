/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httplogmonitorutil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author root
 */
public class HttpObject implements Comparator<HttpObject>, Comparable<HttpObject>{
    private String url;
    private Date hittingTime;
    private int hitCount;
    private long bytesDownloaded;
    private String host, method, path, status;
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss Z");
    
    public HttpObject(){}
    public HttpObject(String url, Date hittingTime, int hitCount, String host, String method, String path, String status)
    {
        this.url = url;
        this.hittingTime = hittingTime;
        this.hitCount = hitCount;
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
    public HttpObject parseLine(String line)
    {
        if(!line.split(" ")[0].equals("-"))
            this.host = line.split(" ")[0];
        String dateTime = line.split("\\[")[1];
        dateTime = dateTime.split("\\]")[0];
        try {
            this.hittingTime = formatter.parse(dateTime);
        } catch (ParseException ex) {
            Logger.getLogger(HttpObject.class.getName()).log(Level.SEVERE, null, ex);
            this.hittingTime = new Date();
        }
        this.method = line.split(" ")[5].replace("\"", "");
        this.path = line.split(" ")[6];
        this.url = this.host+this.path;
        this.status = line.split(" ")[8];
        try
        {
            this.bytesDownloaded = Long.parseLong(line.split(" ")[9]);
        }
        catch(Exception ex)
        {
            this.bytesDownloaded = 0;
        }
        return this;
    }

    @Override
    public int compare(HttpObject o1, HttpObject o2) {
        return o2.getHitCount() - o1.getHitCount();
    }

    @Override
    public int compareTo(HttpObject o) {
        return((Integer)o.getHitCount()).compareTo(this.hitCount);
    }
    
}
