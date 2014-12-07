/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httplogmonitorutil;

import java.util.Date;

/**
 *
 * @author root
 */
public class Alert {
    private Date alertTime;
    private int hitCount;
    private boolean alertType;                  // true for above threshold, false for below threshold
    
    public Alert(){}
    public Alert(Date alertTime, int hitCount, boolean alertType)
    {
        this.alertTime = alertTime;
        this.hitCount = hitCount;
        this.alertType = alertType;
    }
    public Date getAlertTime()
    {
        return this.alertTime;
    }
    public int getHitCount()
    {
        return this.hitCount;
    }
    public boolean getAlertType()
    {
        return this.alertType;
    }
}
