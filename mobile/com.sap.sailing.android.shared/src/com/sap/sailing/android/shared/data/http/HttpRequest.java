package com.sap.sailing.android.shared.data.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.sap.sailing.android.shared.R;
import com.sap.sailing.android.shared.logging.ExLog;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public abstract class HttpRequest {

    private final static String TAG = HttpRequest.class.getName();

    private final static int lowestOkCode = HttpURLConnection.HTTP_OK;
    private final static int lowestRedirectCode = HttpURLConnection.HTTP_MULT_CHOICE;

    private static void validateHttpResponseCode(HttpURLConnection connection) throws IOException {
        int statusCode = connection.getResponseCode();
        if (statusCode != -1) {
            if (statusCode >= lowestOkCode && statusCode < lowestRedirectCode) {
                return;
            }
            throw new IOException(String.format("Request response had error code %d.", statusCode));
        }
        throw new IOException("Request response had no valid status.");
    }

    public interface HttpRequestProgressListener {
        void onHttpProgress(float progress);
    }

    private final HttpRequestProgressListener listener;
    private final URL url;
    private final Context context;
    private boolean isCancelled;
    protected SharedPreferences pref;

    public HttpRequest(Context context, URL url) {
        this(context, url, null);
    }

    public HttpRequest(Context context, URL url, HttpRequestProgressListener listener) {
        this.url = url;
        this.listener = listener;
        this.isCancelled = false;
        this.context = context;
        this.pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getUrlAsString() {
        return url == null ? null : url.toString();
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void cancel() {
        this.isCancelled = true;
    }

    /**
     * Returns a copied {@link InputStream} of the server's response. You must close this stream when done.
     */
    public InputStream execute() throws IOException {
        ExLog.i(context, TAG, String.format("(Request %d) Executing HTTP request on %s.", this.hashCode(), url));

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept-Encoding", "");

        String accessToken = pref.getString(context.getString(R.string.preference_access_token_key), null);
        if (accessToken != null) {
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        }

        BufferedInputStream responseInputStream = null;
        try {
            try {
                responseInputStream = doRequest(connection);
            } catch (FileNotFoundException fnfe) {
                if (HttpURLConnection.HTTP_UNAUTHORIZED == connection.getResponseCode()) {
                    throw new UnauthorizedException(connection.getHeaderField("WWW-Authenticate"));
                }
                // 404 errors...
                throw new FileNotFoundException(context.getString(R.string.http_request_exception, this.hashCode(),
                        fnfe.getMessage(), connection.getResponseCode(), connection.getResponseMessage()));
            }

            validateHttpResponseCode(connection);

            InputStream copiedResponseInputStream = readAndCopyResponse(connection, responseInputStream);

            if (copiedResponseInputStream != null) {
                ExLog.i(context, TAG, context.getString(R.string.http_request_executed, this.hashCode()));
            } else {
                ExLog.i(context, TAG, context.getString(R.string.http_request_aborted, this.hashCode()));
            }

            connection.disconnect();
            return copiedResponseInputStream;
        } catch (IOException e) {
            ExLog.i(context, TAG, context.getString(R.string.http_request_failed, this.hashCode()));
            throw e;
        } finally {
            safeClose(responseInputStream);
        }
    }

    protected InputStream readAndCopyResponse(HttpURLConnection connection, BufferedInputStream inputStream)
            throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (!readResponse(connection, inputStream, outputStream)) {
            return null;
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    protected boolean readResponse(HttpURLConnection connection, BufferedInputStream inputStream,
            OutputStream outputStream) throws IOException {
        int fileLength = connection.getContentLength();
        byte data[] = new byte[4096];
        long total = 0;
        int count;
        while ((count = inputStream.read(data)) != -1) {
            if (isCancelled()) {
                return false;
            }
            total += count;
            // publishing the progress....
            if (fileLength > 0 && listener != null) {
                listener.onHttpProgress(total / (float) fileLength);
            }
            outputStream.write(data, 0, count);
        }
        return true;
    }

    protected void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                ExLog.ex(context, TAG, e);
            }
        }
    }

    protected abstract BufferedInputStream doRequest(HttpURLConnection connection) throws IOException;

}
