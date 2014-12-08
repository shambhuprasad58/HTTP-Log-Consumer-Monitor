/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httplogmonitor;

import httplogmonitorutil.HttpObject;
import httplogmonitorutil.Utility;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shambhu
 */
public class MostHitsCalculator extends TimerTask
{
    private LinkedBlockingQueue<HttpObject> mostHitsURLQueue;
    private LinkedBlockingQueue<ArrayList<HttpObject>> mostHitsTopURL;
    
    public MostHitsCalculator(LinkedBlockingQueue<HttpObject> mostHitsURLQueue, LinkedBlockingQueue<ArrayList<HttpObject>> mostHitsTopURL)
    {
        this.mostHitsURLQueue = mostHitsURLQueue;
        this.mostHitsTopURL = mostHitsTopURL;
    }
    
    @Override
    public void run() 
    {
        try {
            if(mostHitsURLQueue.isEmpty())
                return;
            HashMap<String, HttpObject> HitCountMapping = new HashMap<>();
            while(!mostHitsURLQueue.isEmpty())
            {
                HttpObject thisHit = mostHitsURLQueue.take();
                String section = Utility.getSection(thisHit.getUrl());
                if(HitCountMapping.containsKey(section))
                {
                    HttpObject temp = HitCountMapping.get(section);
                    temp.setHitCount(temp.getHitCount()+1);
                    HitCountMapping.put(section, temp);
                }
                else
                {
                    thisHit.setHitCount(1);
                    HitCountMapping.put(section, thisHit);
                }
            }
            ArrayList<HttpObject> topHits = new ArrayList<>();
            Iterator<Map.Entry<String, HttpObject>> iterator = HitCountMapping.entrySet().iterator();
            while(iterator.hasNext())
            {
                topHits.add(iterator.next().getValue());
                removeMinimum(topHits);
            }
            Collections.sort(topHits);
            mostHitsTopURL.put(topHits);
        } catch (InterruptedException ex) {
            Logger.getLogger(MostHitsCalculator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void removeMinimum(ArrayList<HttpObject> list)
    {
        if(list.size() <= 10)
            return;
        list.remove(Collections.min(list));
    }
}
