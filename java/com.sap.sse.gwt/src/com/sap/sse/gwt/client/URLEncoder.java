package com.sap.sse.gwt.client;

import com.google.gwt.http.client.URL;

/**
 * A very basic URL encoder that also works in a GWT environment where <code>java.net.URLEncoder</code> is not available.
 * It is based on {@link URL#encodeQueryString(String)}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class URLEncoder {
    
    private URLEncoder() { }

    public static String encodeQueryString(String url) {
        return URL.encodeQueryString(url);
    }
    
}
