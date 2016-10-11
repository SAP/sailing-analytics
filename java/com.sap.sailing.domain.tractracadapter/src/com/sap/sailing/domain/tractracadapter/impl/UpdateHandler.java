package com.sap.sailing.domain.tractracadapter.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
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

import com.sap.sailing.domain.tractracadapter.UpdateResponse;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;

public class UpdateHandler {
    
    private final static Logger logger = Logger.getLogger(UpdateHandler.class.getName());
    
    private JsonDeserializer<UpdateResponse> updateDeserializer;
    private final URI baseURI;
    private final String tracTracUsername;
    private final String tracTracPassword;
    private final Serializable tracTracEventId;
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
    private final static String UpdateUrlTemplate = "%s%s?eventid=%s&raceid=%s&username=%s&password=%s";
    
    public UpdateHandler(URI updateURI, String action, String tracTracUsername, String tracTracPassword, Serializable tracTracEventId, Serializable raceId) {
        this.baseURI = updateURI;
        this.action = action;
        this.tracTracUsername = tracTracUsername;
        this.tracTracPassword = tracTracPassword;
        this.tracTracEventId = tracTracEventId;
        this.raceId = raceId;
        this.updateDeserializer = new UpdateResponseDeserializer();
        if (updateURI != null && !updateURI.toString().equals("")) {
            logger.info("Activating TracTrac update handler "+this+" for race with ID "+raceId);
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
    
    /**
     * @return a new list that the caller may extend to add more parameters; the list returned contains tbe basic
     *         parameters {@code eventid}, {@code raceid}, {@code username} and {@code password}.
     */
    protected List<BasicNameValuePair> getDefaultParametersAsNewList() {
        final List<BasicNameValuePair> result = new ArrayList<>();
        result.add(new BasicNameValuePair("eventid", tracTracEventId.toString()));
        result.add(new BasicNameValuePair("raceid", this.raceId.toString()));
        result.add(new BasicNameValuePair("username", tracTracUsername));
        result.add(new BasicNameValuePair("password", tracTracPassword));
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
                URLEncoder.encode(this.tracTracEventId.toString(), EncodingUtf8), 
                URLEncoder.encode(this.raceId.toString(), EncodingUtf8),
                URLEncoder.encode(tracTracUsername, EncodingUtf8),
                URLEncoder.encode(tracTracPassword, EncodingUtf8));
        
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
        BufferedReader reader = getResponseOnUpdateFromTracTrac(connection);
        parseAndLogResponse(reader);
    }

    protected void parseAndLogResponse(BufferedReader reader)
            throws IOException, ParseException, JsonDeserializationException {
        Object responseBody = JSONValue.parseWithException(reader);
        JSONObject responseObject = Helpers.toJSONObjectSafe(responseBody);
        UpdateResponse updateResponse = updateDeserializer.deserialize(responseObject);
        if (updateResponse.getStatus().equals(ResponseCodeForFailure)) {
            logger.severe("Failed to send data to TracTrac, got following response: " + updateResponse.getMessage());
        } else {
            logger.info("Successfully sent data to TracTrac with response: " + updateResponse.getMessage());
        }
    }

    private BufferedReader getResponseOnUpdateFromTracTrac(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader;
    }

    protected void sendWithPayload(HttpURLConnection connection, String payload) throws IOException {
        DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        writer.writeBytes(payload);
        writer.flush();
        writer.close();
    }

    protected void setConnectionProperties(HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod(HttpGetRequestMethod);
        connection.setDoOutput(false);
        connection.setUseCaches(false);
    }
    
    protected void setConnectionProperties(HttpURLConnection connection, String payload) throws ProtocolException {
        connection.setRequestMethod(HttpPostRequestMethod);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty(ContentType, ContentTypeApplicationJson);
        connection.addRequestProperty(ContentLength, String.valueOf(payload.getBytes().length));
    }
    
    protected boolean isActive() {
        return this.active;
    }
}
