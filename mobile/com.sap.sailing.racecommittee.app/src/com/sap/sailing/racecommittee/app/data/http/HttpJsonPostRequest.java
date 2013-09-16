package com.sap.sailing.racecommittee.app.data.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class HttpJsonPostRequest extends HttpRequest {
    public final static String ContentType = "application/json;charset=UTF-8";

    private String requestBody;

    public HttpJsonPostRequest(URL requestUrl, String body) {
        super(requestUrl);
        this.requestBody = body;
    }

    @Override
    protected BufferedInputStream execute(HttpURLConnection connection) throws IOException {
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(0);

        connection.setRequestProperty("Content-Type", ContentType);
        connection.setRequestProperty("Accept", ContentType);

        OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
        sendBody(outputStream);
        outputStream.close();

        return new BufferedInputStream(connection.getInputStream());
    }

    private void sendBody(OutputStream outputStream) throws IOException {
        outputStream.write(requestBody.getBytes(Charset.forName("UTF-8")));
    }
}
