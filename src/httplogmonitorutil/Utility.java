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
public class Utility {
    public static String getSection(String url)
    {
        String section = url;
        if(url.contains("http://"))
            section = section.replaceFirst("http://", "~___~");
        String[] parts = section.split("/");
        if(parts.length > 1)
            section = parts[0] + "/" + parts[1];
        else
            section = parts[0];
        if(url.contains("~___~"))
            section = section.replaceFirst("~___~", "http://");
        return section;
    }
    
    public static String removeExtra(String url)
    {
        return url.replaceFirst("http://", "").replaceFirst("www.", "");
    }
    
}
