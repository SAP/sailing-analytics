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
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.tracking.CourseDesignChangedListener;
import com.sap.sailing.domain.tractracadapter.CourseUpdateResponse;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.ControlPointJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.CourseBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.CourseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.GateJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.MarkJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.WaypointJsonSerializer;

public class CourseDesignChangedByRaceCommitteeHandler implements CourseDesignChangedListener {
    
    private final static Logger logger = Logger.getLogger(CourseDesignChangedByRaceCommitteeHandler.class.getName());
    
    private JsonSerializer<CourseBase> courseSerializer;
    private JsonDeserializer<CourseUpdateResponse> courseUpdateDeserializer;
    private final URI courseDesignUpdateURI;
    private final String tracTracUsername;
    private final String tracTracPassword;
    private final Serializable tracTracEventId;
    private final Serializable raceId;
    
    private final static String HttpPostRequestMethod = "POST";
    private final static String ContentType = "Content-Type";
    private final static String ContentLength = "Content-Length";
    private final static String ContentTypeApplicationJson = "application/json";
    private final static String EncodingUtf8 = "UTF-8";
    private final static String CourseUpdateUrlTemplate = "%s?eventid=%s&raceid=%s&username=%s&password=%s";
    private final static String ResponseCodeForFailure = "FAILURE";
    
    public CourseDesignChangedByRaceCommitteeHandler(URI courseDesignUpdateURI, String tracTracUsername, String tracTracPassword, Serializable tracTracEventId, Serializable raceId) {
        this.courseDesignUpdateURI = courseDesignUpdateURI;
        this.tracTracUsername = tracTracUsername;
        this.tracTracPassword = tracTracPassword;
        this.tracTracEventId = tracTracEventId;
        this.raceId = raceId;
        this.courseSerializer = new CourseJsonSerializer(
                new CourseBaseJsonSerializer(
                        new WaypointJsonSerializer(
                                new ControlPointJsonSerializer(
                                        new MarkJsonSerializer(), 
                                        new GateJsonSerializer(new MarkJsonSerializer())))));
        this.courseUpdateDeserializer = new CourseUpdateResponseDeserializer();
    }

    @Override
    public void courseDesignChanged(CourseBase newCourseDesign) throws MalformedURLException, IOException {
        JSONObject serializedCourseDesign = courseSerializer.serialize(newCourseDesign);
        String payload = serializedCourseDesign.toJSONString();
        URL currentCourseDesignURL = buildCourseUpdateURL();
        logger.info("Using " + currentCourseDesignURL.toString() + " for the course update!");
        logger.info("Payload is " + payload);
        HttpURLConnection connection = (HttpURLConnection) currentCourseDesignURL.openConnection();
        try {
            setConnectionProperties(connection, payload);
            sendWithPayload(connection, payload);
            BufferedReader reader = getResponseOnCourseUpdateFromTracTrac(connection);
            try {
                Object responseBody = JSONValue.parseWithException(reader);
                JSONObject responseObject = Helpers.toJSONObjectSafe(responseBody);

                checkAndLogCourseUpdateResponse(responseObject);
            } catch (ParseException pe) {
                pe.printStackTrace();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            } else {
                logger.severe("Connection to TracTrac Course Update URL " + currentCourseDesignURL.toString() + " could not be established");
            }
        }
    }

    private void checkAndLogCourseUpdateResponse(JSONObject responseObject) throws JsonDeserializationException {
        CourseUpdateResponse courseUpdateResponse = courseUpdateDeserializer.deserialize(responseObject);
        if (courseUpdateResponse.getStatus().equals(ResponseCodeForFailure)) {
            logger.severe("Failed to send new course to TracTrac, got following response: " + courseUpdateResponse.getMessage());
        } else {
            logger.info("Successfully sent course update to TracTrac with response: " + courseUpdateResponse.getMessage());
        }
    }

    private BufferedReader getResponseOnCourseUpdateFromTracTrac(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader;
    }

    private void sendWithPayload(HttpURLConnection connection, String payload) throws IOException {
        DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        writer.writeBytes(payload);
        writer.flush();
        writer.close();
    }

    private void setConnectionProperties(HttpURLConnection connection, String payload) throws ProtocolException {
        connection.setRequestMethod(HttpPostRequestMethod);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty(ContentType, ContentTypeApplicationJson);
        connection.addRequestProperty(ContentLength, String.valueOf(payload.getBytes().length));
    }
    
    private URL buildCourseUpdateURL() throws MalformedURLException, UnsupportedEncodingException {
        String url = String.format(CourseUpdateUrlTemplate, 
                this.courseDesignUpdateURI.toString(), 
                URLEncoder.encode(this.tracTracEventId.toString(), EncodingUtf8), 
                URLEncoder.encode(this.raceId.toString(), EncodingUtf8),
                URLEncoder.encode(tracTracUsername, EncodingUtf8),
                URLEncoder.encode(tracTracPassword, EncodingUtf8));
        return new URL(url);
    }
}
