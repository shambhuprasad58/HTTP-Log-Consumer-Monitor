/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httplogmonitorutil;

/**
 *
 * @author root
 */
public class Statistics 
{
    private long hitCount;
    private long kiloBytesDownloaded;
    private long kiloBytesUploaded;
    private long successfulHits;
    
    public Statistics(){};
    public Statistics(long hitCount, long kiloBytesDownloaded, long kiloBytesUploaded, long successfulHits)
    {
        this.hitCount = hitCount;
        this.kiloBytesDownloaded = kiloBytesDownloaded;
        this.kiloBytesUploaded = kiloBytesUploaded;
        this.successfulHits = successfulHits;
    }
    
    public long getHitCount()
    {
        return this.hitCount;
    }
    public long getKiloBytesDownloaded()
    {
        return this.kiloBytesDownloaded;
    }
    public long getKiloBytesUploaded()
    {
        return this.kiloBytesUploaded;
    }
    public long getSuccessfulHits()
    {
        return this.successfulHits;
    }
    
    public void setHitCount(long hitCount)
    {
        this.hitCount = hitCount;
    }
    public void updateKiloBytesDownloaded(long kiloBytes)
    {
        this.kiloBytesDownloaded += kiloBytes;
    }
    public void updateKiloBytesUploaded(long kiloBytes)
    {
        this.kiloBytesUploaded += kiloBytes;
    }
    public void updateSuccessfulHits(int count)
    {
        this.successfulHits += count;
    }
}
