package com.sap.sailing.domain.tractracadapter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackerManager;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tractracadapter.impl.Simulator;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public interface TracTracAdapter {
    static final String NAME = "TracTrac";
    static final String DEFAULT_URL = "https://www.tractrac.com/";

    DomainFactory getTracTracDomainFactory();
    
    /**
     * If not already tracking the URL/URI/URI combination, adds a single race tracker and starts tracking the race,
     * using the race's parameter URL which delivers the single configuration text file for that race. While the result
     * of passing this URL to the TracTrac <code>KeyValue.setup</code> is a TracTrac <code>Event</code>, those events
     * only manage a single race. In our domain model, we group those races into a single instance of our
     * {@link Regatta} class.
     * <p>
     * 
     * If this is the first race of an event, the {@link Regatta} is created as well. If the {@link RaceDefinition} for
     * the race already exists, it isn't created again. Also, if a {@link TracTracRaceTracker} for the given race
     * already exists, it is not added again.
     * <p>
     * 
     * Note that when the race identified by <code>paramURL</code>, <code>liveURI</code> and <code>storedURI</code> is
     * already being tracked, then regardless of the <code>windStore</code> selection the existing tracker will be used
     * and its race handle will be returned. A log message will indicate a potential wind store mismatch (based on
     * {@link WindStore#equals(Object)}).
     * 
     * @param trackerManager
     *            the race will be added to that manager
     * @param timeoutInMilliseconds
     *            if the race definition is not received for the race within this time, the race tracker for that race
     *            is stopped; use -1 to wait forever
     */
    RaceHandle addTracTracRace(TrackerManager trackerManager, URL paramURL, URI liveURI, URI storedURI,
            URI courseDesignUpdateURI, RaceLogStore raceLogStore, RegattaLogStore regattaLogStore,
            long timeoutInMilliseconds, String tracTracApiToken, String raceStatus, String raceVisibility,
            boolean trackWind, boolean correctWindDirectionByMagneticDeclination, int timeoutInMillis, boolean useOfficialEventsToUpdateRaceLog,
            RaceTrackingHandler raceTrackingHandler)
                    throws MalformedURLException, FileNotFoundException, URISyntaxException, Exception;

    /**
     * Same as {@link #addTracTracRace(URL, URI, URI, WindStore, long)}, only that start and end of tracking are
     * specified which may help reducing the amount of stored data (particularly mark positions) that needs to be
     * loaded.
     * 
     * @param trackerManager
     *            the race will be added to that manager
     * @param regattaToAddTo
     *            if <code>null</code>, the regatta into which the race has previously been loaded will be looked up; if
     *            found, the race will be loaded into that regatta; otherwise, an existing regatta by the name of the
     *            TracTrac event with the boat class name appended in parentheses will be looked up; if not found, a
     *            default regatta with that name will be created, with a single default series and a single default
     *            fleet. If a valid {@link RegattaIdentifier} is specified, a regatta lookup is performed with that
     *            identifier; if the regatta is found, it is used to add the races to. Otherwise, a default regatta as
     *            described above will be created and used.
     * @param offsetToStartTimeOfSimulatedRace
     *            if non-<code>null</code>, the {@link Simulator} will be used with this duration as start offset
     */
    RaceHandle addTracTracRace(TrackerManager trackerManager, RegattaIdentifier regattaToAddTo, URL paramURL,
            URI liveURI, URI storedURI, URI courseDesignUpdateURI, TimePoint trackingStartTime,
            TimePoint trackingEndTime, RaceLogStore raceLogStore, RegattaLogStore regattaLogStore,
            long timeoutForReceivingRaceDefinitionInMilliseconds, Duration offsetToStartTimeOfSimulatedRace,
            boolean useInternalMarkPassingAlgorithm, String tracTracApiToken, String raceStatus,
            String raceVisibility, boolean trackWind, boolean correctWindDirectionByMagneticDeclination,
            boolean useOfficialEventsToUpdateRaceLog, URI liveURIFromConfiguration,
            URI storedURIFromConfiguration)
            throws MalformedURLException, FileNotFoundException, URISyntaxException, Exception;

    /**
     * For the JSON URL of an account / event, lists the paramURLs that can be used for
     * {@link #addTracTracRace(URL, URI, URI, WindStore, long)} calls to individually start tracking races of this
     * event, rather than tracking <em>all</em> races in the event which is hardly ever useful. The returned pair's
     * first component is the event name.
     * 
     * @param loadClientParams
     *            shall the properties from the clientparams.php file such as liveURI and storedURI already be loaded?
     *            Generally, this is not necessary as the
     *            {@link #addTracTracRace(TrackerManager, RegattaIdentifier, URL, URI, URI, URI, TimePoint, TimePoint, RaceLogStore, WindStore, long, boolean, String, String)}
     *            and {@link #addTracTracRace(TrackerManager, URL, URI, URI, URI, RaceLogStore, WindStore, long, String, String)} will
     *            fetch the JSON and clientparams.php documents to work with up-to-date data.
     */
    Util.Pair<String, List<RaceRecord>> getTracTracRaceRecords(URL jsonURL, boolean loadClientParams) throws IOException,
            ParseException, org.json.simple.parser.ParseException, URISyntaxException;

    RaceRecord getSingleTracTracRaceRecord(URL jsonURL, String raceId, boolean loadClientParams) throws Exception;

    TracTracConfiguration createTracTracConfiguration(String creatorName, String name, String jsonURL,
            String liveDataURI, String storedDataURI, String courseDesignUpdateURI, String tracTracApiToken);
    
}
