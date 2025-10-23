package com.sap.sailing.domain.tracking.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.tracking.StartTimeChangedListener;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.util.HttpUrlConnectionHelper;
import com.sap.sse.util.LaxRedirectStrategyForAllRedirectResponseCodes;

public class StartTimeUpdateHandler extends UpdateHandler implements StartTimeChangedListener {
    private final static Logger logger = Logger.getLogger(StartTimeUpdateHandler.class.getName());

    private final static String ACTION = "update_race_start_time";
    private final static String ACTION_START_TRACKING = "start_tracking";
    private final static String FIELD_RACE_START_TIME = "race_start_time";
    private final static String FIELD_TRACKING_START_TIME = "tracking_start_time";

    /**
     * The regatta is required in order to query {@link Regatta#isControlTrackingFromStartAndFinishTimes()} when
     * a new start time is received.
     */
    private final Regatta regatta;

    private final RaceAbortedHandler raceAbortedHandler;
    
    public StartTimeUpdateHandler(URI updateURI, String tracTracApiToken,
            Serializable tracTracEventId, Serializable raceId, Regatta regatta) {
        super(updateURI, ACTION, tracTracApiToken, tracTracEventId, raceId);
        this.raceAbortedHandler = new RaceAbortedHandler(updateURI,tracTracApiToken, tracTracEventId, raceId);
        this.regatta = regatta;
    }

    @Override
    public void startTimeChanged(TimePoint newStartTime) throws MalformedURLException, IOException, URISyntaxException {
        if (isActive()) {
            if (newStartTime == null) {
                /* notify race status as POSTPONED according to Jorge's comment https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4708#c5 :
                 * The method that @frank has commented has to work:
                 *    http://em.aws.tractrac.com/update_race_status?eventid=5f2f20f0-6cb2-0136-9eca-60a44ce903c3&raceid=81c082a0-7b1f-0136-166e-028f184941da&username=trac%40sapsailing.com&password=sap0912&race_status=POSTPONED
                 * This method changes the race start time to null.
                 */
                raceAbortedHandler.raceAborted(Flags.AP); // will send POSTPONED
            } else {
                HashMap<String, String> additionalParameters = new HashMap<String, String>();
                additionalParameters.put(FIELD_RACE_START_TIME, String.valueOf(newStartTime.asMillis()));
                URL startTimeUpdateURL = buildUpdateURL(additionalParameters);
                
                logger.info("Using " + eraseSecurityRelatedValuesFromURL(startTimeUpdateURL.toString()) + " for the start time update!");
                HttpURLConnection connection = (HttpURLConnection) startTimeUpdateURL.openConnection();
                try {
                    connection = setConnectionProperties(connection);
                    try {
                        checkAndLogUpdateResponse(connection);
                    } catch (ParseException e) {
                        logger.log(Level.INFO, "Error parsing TracTrac response for start time update", e);
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    } else {
                        logger.severe("Connection to TracTrac Course Update URL " +
                                eraseSecurityRelatedValuesFromURL(startTimeUpdateURL.toString()) + " could not be established");
                    }
                }
                if (regatta.isControlTrackingFromStartAndFinishTimes()) {
                    // make sure tracking is started on TracTrac's side and the start of tracking time is set
                    // to five minutes before start:
                    final URI startTrackingURI = getActionURI(ACTION_START_TRACKING);
                    final HttpPost request = new HttpPost(startTrackingURI);
                    final List<BasicNameValuePair> params = getDefaultParametersAsNewList();
                    params.add(new BasicNameValuePair(FIELD_TRACKING_START_TIME, String.valueOf(newStartTime.minus(
                            TrackedRace.START_TRACKING_THIS_MUCH_BEFORE_RACE_START).asMillis())));
                    request.setEntity(new UrlEncodedFormEntity(params));
                    final HttpClient client = HttpClientBuilder.create()
                            .setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes())
                            .build();
                    logger.info("Using " + eraseSecurityRelatedValuesFromURL(startTrackingURI.toString()) + " to start tracking");
                    final HttpResponse response = client.execute(request);
                    try {
                        parseAndLogResponse(new BufferedReader(new InputStreamReader(response.getEntity().getContent(),
                                HttpUrlConnectionHelper.getCharsetFromHttpEntity(response.getEntity(), "UTF-8"))));
                    } catch (ParseException e) {
                        logger.log(Level.INFO, "Error parsing TracTrac response for start tracking", e);
                    }
                }
            }
        }
    }
}
