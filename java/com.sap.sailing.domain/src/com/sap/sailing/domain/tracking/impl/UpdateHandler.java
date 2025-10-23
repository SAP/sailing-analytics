package com.sap.sailing.domain.tracking.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;
import com.sap.sse.util.HttpUrlConnectionHelper;

/**
 * An update handler can be registered as a listener on a {@link TrackedRace} and can then propagate information to
 * an external service. This can, e.g., be a tracking partner's system that is interested in receiving race status
 * updates such as new start times or postponements.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class UpdateHandler {
    private final static Logger logger = Logger.getLogger(UpdateHandler.class.getName());
    
    private JsonDeserializer<UpdateResponse> updateDeserializer;
    private final URI baseURI;
    private final String tracTracApiToken;
    private final Serializable eventId;
    private final Serializable raceId;
    private final String action;
    private final boolean active;
    
    private final static String HttpPostRequestMethod = "POST";
    private final static String HttpGetRequestMethod = "GET";
    private final static String ContentType = "Content-Type";
    private final static String ContentLength = "Content-Length";
    private final static String ContentTypeApplicationJson = "application/json";
    private final static String EncodingUtf8 = "UTF-8";
    private final static String ResponseCodeForFailure = "FAILURE";
    private final static String UpdateUrlTemplate = "%s%s?eventid=%s&raceid=%s";
    
    public UpdateHandler(URI updateURI, String action, String tracTracApiToken, Serializable eventId, Serializable raceId) {
        this.baseURI = updateURI;
        this.action = action;
        this.tracTracApiToken = tracTracApiToken;
        this.eventId = eventId;
        this.raceId = raceId;
        this.updateDeserializer = new UpdateResponseDeserializer();
        if (Util.hasLength(tracTracApiToken)) {
            logger.info("Activating update handler "+this+" for race with ID "+raceId);
            this.active = true;
        } else {
            this.active = false;
        }
    }
    
    protected URI getBaseURI() {
        return baseURI;
    }
    
    protected URI getActionURI() throws URISyntaxException {
        return getActionURI(action);
    }
    
    protected URI getActionURI(String action) throws URISyntaxException {
        return new URI(baseURI.getScheme(), baseURI.getHost(), baseURI.getPath()+(baseURI.getPath().endsWith("/")?"":"/")+action, baseURI.getFragment());
    }
    
    protected String eraseSecurityRelatedValuesFromURL(String url) {
        return url.replaceAll("password=([^&]*)&", "password=****&");
    }
    
    /**
     * @return a new list that the caller may extend to add more parameters; the list returned contains the basic
     *         parameters {@code eventid}, {@code raceid}, {@code username} and {@code password}.
     */
    protected List<BasicNameValuePair> getDefaultParametersAsNewList() {
        final List<BasicNameValuePair> result = new ArrayList<>();
        result.add(new BasicNameValuePair("eventid", eventId.toString()));
        result.add(new BasicNameValuePair("raceid", this.raceId.toString()));
        return result;
    }

    protected URL buildUpdateURL(Map<String, String> additionalParameters) throws MalformedURLException, UnsupportedEncodingException {
        String serverUpdateURI = this.baseURI.toString();
        // make sure that the update URI always ends with a slash
        if (!serverUpdateURI.endsWith("/")) {
            serverUpdateURI = serverUpdateURI + "/";
        }
        String url = String.format(UpdateUrlTemplate, 
                serverUpdateURI.toString(),
                this.action,
                URLEncoder.encode(this.eventId.toString(), EncodingUtf8), 
                URLEncoder.encode(this.raceId.toString(), EncodingUtf8));
        
        for (Entry<String, String> entry : additionalParameters.entrySet()) {
            url += String.format("&%s=%s", 
                    URLEncoder.encode(entry.getKey(), EncodingUtf8),
                    URLEncoder.encode(entry.getValue(), EncodingUtf8));
        }
        return new URL(url);
    }
    
    protected URL buildUpdateURL() throws MalformedURLException, UnsupportedEncodingException {
        return buildUpdateURL(new HashMap<String, String>());
    }
    
    protected void checkAndLogUpdateResponse(HttpURLConnection connection) throws IOException, ParseException {
        connection.setConnectTimeout(10000/*milliseconds*/);
        connection.connect();
        BufferedReader reader = getResponseOnUpdateFromProvider(connection);
        parseAndLogResponse(reader);
    }

    protected void parseAndLogResponse(BufferedReader reader)
            throws IOException, ParseException, JsonDeserializationException {
        Object responseBody = JSONValue.parseWithException(reader);
        JSONObject responseObject = Helpers.toJSONObjectSafe(responseBody);
        UpdateResponse updateResponse = updateDeserializer.deserialize(responseObject);
        if (updateResponse.getStatus().equals(ResponseCodeForFailure)) {
            logger.severe("Failed to send data to provider, got following response: " + updateResponse.getMessage());
        } else {
            logger.info("Successfully sent data to provider with response: " + updateResponse.getMessage());
        }
    }

    private BufferedReader getResponseOnUpdateFromProvider(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        final Charset charset = HttpUrlConnectionHelper.getCharsetFromConnectionOrDefault(connection, "UTF-8");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        return reader;
    }

    protected HttpURLConnection setConnectionProperties(HttpURLConnection connection) throws IOException {
        return followRedirects(connection, c->{
            c.setRequestMethod(HttpGetRequestMethod);
            c.setDoOutput(false);
            c.setUseCaches(false);
        });
    }
    
    protected HttpURLConnection setConnectionPropertiesAndSendWithPayload(HttpURLConnection connection, String payload) throws IOException {
        return followRedirects(connection, c-> {
            c.setRequestMethod(HttpPostRequestMethod);
            c.setDoOutput(true);
            c.setUseCaches(false);
            c.setRequestProperty(ContentType, ContentTypeApplicationJson);
            c.addRequestProperty(ContentLength, String.valueOf(payload.getBytes().length));
            if (Util.hasLength(tracTracApiToken)) {
                c.addRequestProperty("Authorization", "Bearer " + tracTracApiToken);
            }
            DataOutputStream writer = new DataOutputStream(c.getOutputStream());
            writer.writeBytes(payload);
            writer.flush();
            writer.close();
        });
    }
    
    @FunctionalInterface
    private static interface ConnectionConsumer {
        void consume(HttpURLConnection connection) throws IOException;
    }
    
    private HttpURLConnection followRedirects(HttpURLConnection connection, ConnectionConsumer connectionConsumer) throws IOException {
        // Initial timeout needs to be big enough to allow the first parts of the response to reach this server
        final int HTTP_MAX_REDIRECTS = 5;
        int redirects = 0;
        int responseCode;
        do {
            connectionConsumer.consume(connection);
            connection.setReadTimeout((int) Duration.ONE_MINUTE.times(10).asMillis());
            responseCode = connection.getResponseCode();
            if ((connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM
                || connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) && redirects < HTTP_MAX_REDIRECTS) {
                String location = connection.getHeaderField("Location");
                final URL nextUrl = new URL(location);
                connection = (HttpURLConnection) nextUrl.openConnection();
                redirects++;
            }
        } while ((responseCode == HttpURLConnection.HTTP_MOVED_PERM
                || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) && redirects < HTTP_MAX_REDIRECTS);
        return connection;
    }

    protected boolean isActive() {
        return this.active;
    }
}
