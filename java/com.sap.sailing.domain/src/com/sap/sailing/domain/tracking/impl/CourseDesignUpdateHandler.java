package com.sap.sailing.domain.tracking.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.tracking.CourseDesignChangedListener;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.ControlPointJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.CourseBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.CourseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.GateJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.MarkJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.WaypointJsonSerializer;
import com.sap.sse.shared.json.JsonSerializer;

public class CourseDesignUpdateHandler extends UpdateHandler implements CourseDesignChangedListener {
    private final static String ACTION = "update_course";
    
    private final static Logger logger = Logger.getLogger(CourseDesignUpdateHandler.class.getName());
    private final JsonSerializer<CourseBase> courseSerializer;
    
    public CourseDesignUpdateHandler(URI updateURI, String tracTracApiToken, Serializable eventId, Serializable raceId) {
        super(updateURI, ACTION, tracTracApiToken, eventId, raceId);
        this.courseSerializer = new CourseJsonSerializer(
                new CourseBaseJsonSerializer(
                        new WaypointJsonSerializer(
                                new ControlPointJsonSerializer(
                                        new MarkJsonSerializer(), 
                                        new GateJsonSerializer(new MarkJsonSerializer())))));
    }

    @Override
    public void courseDesignChanged(final CourseBase newCourseDesign) throws MalformedURLException, IOException {
        if (!isActive()) {
            logger.info("Not sending course update to TracTrac because no URL has been provided.");
            return;
        }
        final CourseBase newCourseDesignWithExistingControlPoints = replaceControlPointsByMatchingExistingControlPoints(newCourseDesign);
        JSONObject serializedCourseDesign = courseSerializer.serialize(newCourseDesignWithExistingControlPoints);
        String payload = serializedCourseDesign.toJSONString();
        URL currentCourseDesignURL = buildUpdateURL();
        logger.info("Using " + eraseSecurityRelatedValuesFromURL(currentCourseDesignURL.toString()) + " for the course update!");
        logger.info("Payload is " + payload);
        HttpURLConnection connection = (HttpURLConnection) currentCourseDesignURL.openConnection();
        try {
            setConnectionPropertiesAndSendWithPayload(connection, payload);
            try {
                checkAndLogUpdateResponse(connection);
            } catch (ParseException e) {
                logger.log(Level.SEVERE, "Error trying to send course update to TracTrac", e);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            } else {
                logger.severe("Connection to TracTrac Course Update URL " +
                        eraseSecurityRelatedValuesFromURL(currentCourseDesignURL.toString()) + " could not be established");
            }
        }
    }
    
    /**
     * This default implementation returns the {@code courseDesign} unchanged. Subclasses have the possibility to
     * override this method in order to replace individual course marks provided in a detailed course design,
     * based on the actual course marks available.
     */
    protected CourseBase replaceControlPointsByMatchingExistingControlPoints(CourseBase courseDesign) {
        return courseDesign;
    }
}
