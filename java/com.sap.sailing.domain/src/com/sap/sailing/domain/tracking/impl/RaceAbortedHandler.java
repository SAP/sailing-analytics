package com.sap.sailing.domain.tracking.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.tracking.RaceAbortedListener;

public class RaceAbortedHandler extends UpdateHandler implements RaceAbortedListener {
    
    private final static String ACTION = "update_race_status";
    
    private final static Logger logger = Logger.getLogger(RaceAbortedHandler.class.getName());
    
    public RaceAbortedHandler(URI updateURI, String tracTracApiToken, Serializable eventId, Serializable raceId) {
        super(updateURI, ACTION, tracTracApiToken, eventId, raceId);
    }

    @Override
    public void raceAborted(Flags flag) throws MalformedURLException, IOException {
        if (!isActive()) {
            logger.info("Not sending race abort notification to TracTrac because no URL has been provided.");
            return;
        }
        final String raceStatus;
        if (flag == Flags.AP) {
            raceStatus = "POSTPONED";
        } else {
            raceStatus = "ABORTED";
        }
        Map<String, String> additionalArgs = new HashMap<>();
        additionalArgs.put("race_status", raceStatus);
        URL raceAbortedURL = buildUpdateURL(additionalArgs);
        logger.info("Using " + eraseSecurityRelatedValuesFromURL(raceAbortedURL.toString()) + " for the race aborted notification!");
        HttpURLConnection connection = (HttpURLConnection) raceAbortedURL.openConnection();
        try {
            connection = setConnectionProperties(connection);
            try {
                checkAndLogUpdateResponse(connection);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            } else {
                logger.severe("Connection to TracTrac race aborted URL " +
                        eraseSecurityRelatedValuesFromURL(raceAbortedURL.toString()) + " could not be established");
            }
        }
    }
}
