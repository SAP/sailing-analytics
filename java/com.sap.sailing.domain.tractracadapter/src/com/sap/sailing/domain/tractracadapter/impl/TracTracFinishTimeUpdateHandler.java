package com.sap.sailing.domain.tractracadapter.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;

public class TracTracFinishTimeUpdateHandler extends UpdateHandler {
    private final static Logger logger = Logger.getLogger(TracTracFinishTimeUpdateHandler.class.getName());

    private final static Duration STOP_TRACKING_THIS_MUCH_AFTER_RACE_FINISH = Duration.ONE_MINUTE.times(2);
    private final static String ACTION_STOP_TRACKING = "end_tracking";
    private final static String FIELD_TRACKING_END_TIME = "tracking_end_time";

    /**
     * The regatta is required in order to query {@link Regatta#isControlTrackingFromStartAndFinishTimes()} when
     * a new start time is received.
     */
    private final Regatta regatta;
    
    public TracTracFinishTimeUpdateHandler(URI updateURI, String tracTracUsername, String tracTracPassword,
            Serializable tracTracEventId, Serializable raceId, Regatta regatta) {
        super(updateURI, ACTION_STOP_TRACKING, tracTracUsername, tracTracPassword, tracTracEventId, raceId);
        this.regatta = regatta;
    }
    
    public Listener getListener() {
        return new Listener();
    }
    
    private class Listener extends AbstractRaceChangeListener {
        @Override
        public void finishedTimeChanged(TimePoint oldFinishedTime, TimePoint newFinishedTime) {
            if (isActive() && newFinishedTime != null) {
                if (regatta.isControlTrackingFromStartAndFinishTimes()) {
                    try {
                        // make sure tracking is started on TracTrac's side and the start of tracking time is set
                        // to five minutes before start:
                        final URI stopTrackingURI = getActionURI(ACTION_STOP_TRACKING);
                        final HttpPost request = new HttpPost(stopTrackingURI);
                        final List<BasicNameValuePair> params = getDefaultParametersAsNewList();
                        params.add(new BasicNameValuePair(FIELD_TRACKING_END_TIME, String.valueOf(newFinishedTime.plus(STOP_TRACKING_THIS_MUCH_AFTER_RACE_FINISH).asMillis())));
                        request.setEntity(new UrlEncodedFormEntity(params));
                        final HttpClient client = new SystemDefaultHttpClient();
                        logger.info("Using " + stopTrackingURI.toString() + " to stop tracking");
                        final HttpResponse response = client.execute(request);
                        try {
                            parseAndLogResponse(new BufferedReader(new InputStreamReader(response.getEntity().getContent())));
                        } catch (ParseException e) {
                            logger.log(Level.INFO, "Error parsing TracTrac response for stop tracking", e);
                        }
                    } catch (IOException | URISyntaxException ioe) {
                        logger.log(Level.INFO, "Exception trying to stop TracTrac tracking", ioe);
                    }
                }
            }
        }
    }
}
