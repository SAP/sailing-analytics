package com.sap.sse.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;

import com.sap.sse.common.Duration;

public class HttpUrlConnectionHelper {
    private static final int HTTP_MAX_REDIRECTS = 5;

    /**
     * Redirects the connection using the <code>Location</code> header. Make sure to set
     * the timeout if you expect the response to take longer.
     */
    public static URLConnection redirectConnection(URL url, Duration timeout,
            Consumer<URLConnection> preConnectionModifier) throws MalformedURLException, IOException {
        return redirectConnection(url, timeout, /* optional request method */ null, preConnectionModifier, 
                /* post-connect modifier */ null, /* optional output stream consumer */ Optional.empty());
    }
    
    /**
     * Redirects the connection using the <code>Location</code> header.
     */
    public static URLConnection redirectConnectionWithBearerToken(URL url, String optionalBearerToken) throws MalformedURLException, IOException {
        return redirectConnectionWithBearerToken(url, Duration.ONE_MINUTE.times(10), /* default HTTP method */ null, optionalBearerToken, null, /* optional output stream consumer */ Optional.empty());
    }
    
    /**
     * Redirects the connection using the <code>Location</code> header.
     */
    public static URLConnection redirectConnectionWithBearerToken(URL url, String optionalRequestMethod,
            String optionalBearerToken) throws MalformedURLException, IOException {
        return redirectConnectionWithBearerToken(url, Duration.ONE_MINUTE.times(10), optionalRequestMethod, optionalBearerToken, null, /* optional output stream consumer */ Optional.empty());
    }

    public static URLConnection redirectConnectionWithBearerToken(URL url, Duration timeout,
            String optionalRequestMethod, String optionalBearerToken, String optionalContentType,
            Consumer<URLConnection> preConnectionModifier, Consumer<URLConnection> postConnectModifier,
            Optional<OutputStreamConsumer> optionalOutputStreamConsumer)
            throws MalformedURLException, IOException {
        return redirectConnection(url, timeout, optionalRequestMethod, t -> {
            if (optionalBearerToken != null && !optionalBearerToken.isEmpty()) {
                t.setRequestProperty("Authorization", "Bearer " + optionalBearerToken);
            }
            if (optionalContentType != null && !optionalContentType.isEmpty()) {
                t.setRequestProperty("Content-Type", optionalContentType);
            }
            if (preConnectionModifier != null) {
                preConnectionModifier.accept(t);
            }
        }, postConnectModifier, optionalOutputStreamConsumer);
    }
    
    /**
     * Create a URLConnection with the given parameters. If HTTP redirects are returned it will follow them.
     * 
     * @param url the url to connect to
     * @param timeout the read timeout
     * @param optionalRequestMethod the request type, ignored when null
     * @param optionalBearerToken bearer token for auth, ignored when null
     * @param optionalContentType ttp content type, ignored when null
     * @return the URLConnection already in open state
     * @throws MalformedURLException when URL is malformed
     * @throws IOException general io exception, e.g. connect is failing
     */
    public static URLConnection redirectConnectionWithBearerToken(URL url, Duration timeout,
            String optionalRequestMethod, String optionalBearerToken, String optionalContentType,
            Optional<OutputStreamConsumer> optionalOutputStreamConsumer)
            throws MalformedURLException, IOException {
        return redirectConnection(url, timeout, optionalRequestMethod, t -> {
            if (optionalBearerToken != null && !optionalBearerToken.isEmpty()) {
                t.setRequestProperty("Authorization", "Bearer " + optionalBearerToken);
            }
            if (optionalContentType != null && !optionalContentType.isEmpty()) {
                t.setRequestProperty("Content-Type", optionalContentType);
            }
        }, /* post-connect modifier */ null, optionalOutputStreamConsumer);
    }

    /**
     * Redirects the connection using the <code>Location</code> header. Make sure to set
     * the timeout if you expect the response to take longer.
     */
    public static URLConnection redirectConnectionWithBearerToken(URL url, Duration timeout,
            String optionalRequestMethod, String optionalBearerToken) throws MalformedURLException, IOException {
        return redirectConnectionWithBearerToken(url, timeout, optionalRequestMethod, optionalBearerToken, null, /* optional output stream consumer */ Optional.empty());
    }

    /**
     * Redirects the connection using the <code>Location</code> header. Make sure to set
     * the timeout if you expect the response to take longer.
     */
    public static URLConnection redirectConnectionWithBearerToken(URL url, Duration timeout, String optionalBearerToken)
            throws MalformedURLException, IOException {
        return redirectConnectionWithBearerToken(url, timeout, /* request method */ null, optionalBearerToken, null, /* optional output stream consumer */ Optional.empty());
    }
    
    @FunctionalInterface
    public interface OutputStreamConsumer {
        void accept(OutputStream outputStream) throws IOException;
    }
    
    /**
     * Redirects the connection using the <code>Location</code> header. Make sure to set the timeout if you expect the
     * response to take longer.
     * 
     * @param optionalOutputStreamConsumer
     *            if present, the URL connection's output stream will be obtained and passed to the consumer when it
     *            becomes available for POST/PUT requests.
     */
    public static URLConnection redirectConnection(URL url, Duration timeout, String optionalRequestMethod,
            Consumer<URLConnection> preConnectionModifier, Consumer<URLConnection> postConnectModifier,
            Optional<OutputStreamConsumer> optionalOutputStreamConsumer) throws MalformedURLException, IOException {
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
                ((HttpURLConnection) urlConnection).setRequestMethod(optionalRequestMethod);
            }
            urlConnection.setReadTimeout((int) timeout.asMillis());
            if (postConnectModifier != null) {
                postConnectModifier.accept(urlConnection);
            }
            if (urlConnection instanceof HttpURLConnection) {
                final HttpURLConnection connection = (HttpURLConnection) urlConnection;
                connection.setInstanceFollowRedirects(false);
                if (optionalRequestMethod != null && (optionalRequestMethod.equals("POST") || optionalRequestMethod.equals("PUT")) &&
                            optionalOutputStreamConsumer.isPresent()) {
                    final OutputStream outputStream = connection.getOutputStream();
                    optionalOutputStreamConsumer.get().accept(outputStream);
                }
                if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM
                        || connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP
                        || connection.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER
                        || connection.getResponseCode() == 307 /* HTTP_TEMP_REDIRECT */
                        || connection.getResponseCode() == 308 /* HTTP_PERM_REDIRECT */) {
                    String location = connection.getHeaderField("Location");
                    nextUrl = new URL(nextUrl, location);
                    connection.disconnect();
                } else {
                    break; // connected successfully
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
        return redirectConnection(url, Duration.ONE_MINUTE.times(10), optionalRequestMethod, /* pre-connect modifier */ null,
                /* post-connect modifier */ null, /* optional output stream consumer */ Optional.empty());
    }
    
    public static Charset getCharsetFromConnectionOrDefault(URLConnection connection, String defaultCharsetName) {
        Charset cs;
        try {
            final String contentType = connection.getContentType();
            final Charset charsetFromConnection = contentType == null ? null : org.apache.http.entity.ContentType.parse(contentType).getCharset();
            cs = charsetFromConnection == null ? Charset.forName(defaultCharsetName) : charsetFromConnection;
        } catch (org.apache.http.ParseException | UnsupportedCharsetException e) {
            cs = Charset.forName(defaultCharsetName);
        }
        return cs;
    }
    
    public static Charset getCharsetFromHttpEntity(HttpEntity entity, String defaultCharsetName) {
        final ContentType contentType = ContentType.getOrDefault(entity);
        final Charset charsetFromContentType = contentType.getCharset();
        return charsetFromContentType == null ? Charset.forName(defaultCharsetName) : charsetFromContentType;
    }
}
