package com.sap.sailing.domain.tractracadapter.impl;

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

public class TracTracRaceAbortedHandler extends UpdateHandler implements RaceAbortedListener {
    
    private final static String ACTION = "update_race_status";
    
    private final static Logger logger = Logger.getLogger(TracTracRaceAbortedHandler.class.getName());
    
    public TracTracRaceAbortedHandler(URI updateURI, String tracTracUsername, String tracTracPassword, Serializable tracTracEventId, Serializable raceId) {
        super(updateURI, ACTION, tracTracUsername, tracTracPassword, tracTracEventId, raceId);
    }

    @Override
    public void raceAborted(Flags flag) throws MalformedURLException, IOException {
        if (!isActive()) {
            logger.info("Not sending course update to TracTrac because no URL has been provided.");
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
        logger.info("Using " + raceAbortedURL.toString() + " for the race aborted notification!");
        HttpURLConnection connection = (HttpURLConnection) raceAbortedURL.openConnection();
        try {
            setConnectionProperties(connection);
            try {
                checkAndLogUpdateResponse(connection);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            } else {
                logger.severe("Connection to TracTrac race aborted URL " + raceAbortedURL.toString() + " could not be established");
            }
        }
    }
}
