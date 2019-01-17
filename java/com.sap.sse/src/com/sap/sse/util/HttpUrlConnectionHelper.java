package com.sap.sse.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Consumer;

import com.sap.sse.common.Duration;

public class HttpUrlConnectionHelper {
    private static final int HTTP_MAX_REDIRECTS = 5;

    /**
     * Redirects the connection using the <code>Location</code> header. Make sure to set
     * the timeout if you expect the response to take longer.
     */
    public static URLConnection redirectConnection(URL url, Duration timeout,
            Consumer<URLConnection> preConnectionModifier) throws MalformedURLException, IOException {
        return redirectConnection(url, timeout, /* optional request method */ null, preConnectionModifier);
    }
    
    /**
     * Redirects the connection using the <code>Location</code> header. Make sure to set
     * the timeout if you expect the response to take longer.
     */
    public static URLConnection redirectConnection(URL url, Duration timeout, String optionalRequestMethod,
            Consumer<URLConnection> preConnectionModifier) throws MalformedURLException, IOException {
        URLConnection urlConnection = null;
        URL nextUrl = url;
        for (int counterOfRedirects = 0; counterOfRedirects <= HTTP_MAX_REDIRECTS; counterOfRedirects++) {
            urlConnection = nextUrl.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0...");
            if (preConnectionModifier != null) {
                preConnectionModifier.accept(urlConnection);
            }
            urlConnection.setDoOutput(true);
            if (optionalRequestMethod != null) {
                ((HttpURLConnection)urlConnection).setRequestMethod(optionalRequestMethod);
            }
            urlConnection.setReadTimeout((int) timeout.asMillis());
            if (urlConnection instanceof HttpURLConnection) {
                final HttpURLConnection connection = (HttpURLConnection) urlConnection;
                connection.setInstanceFollowRedirects(false);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM
                        || connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String location = connection.getHeaderField("Location");
                    nextUrl = new URL(nextUrl, location);
                    connection.disconnect();
                } else {
                    break;
                }
            } else {
                break; // no HTTP URL connection; we need to use what we have...
            }
        }
        return urlConnection;
    }
    
    public static URLConnection redirectConnection(URL url) throws MalformedURLException, IOException {
        return redirectConnection(url, Duration.ONE_MINUTE.times(10), null);
    }
    
    public static URLConnection redirectConnection(URL url, String optionalRequestMethod) throws MalformedURLException, IOException {
        return redirectConnection(url, Duration.ONE_MINUTE.times(10), optionalRequestMethod, null);
    }
}
