package com.sap.sailing.domain.tractracadapter.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackerManager;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.JSONService;
import com.sap.sailing.domain.tractracadapter.RaceRecord;
import com.sap.sailing.domain.tractracadapter.TracTracAdapter;
import com.sap.sailing.domain.tractracadapter.TracTracConfiguration;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class TracTracAdapterImpl implements TracTracAdapter {
    private final static Logger logger = Logger.getLogger(TracTracAdapter.class.getName());
    
    private final DomainFactory tractracDomainFactory;

    /**
     * The globally used configuration of the time delay (in milliseconds) to the 'live' timepoint used for each new
     * tracked race.
     */
    private final long delayToLiveInMillis;

    public TracTracAdapterImpl(com.sap.sailing.domain.base.DomainFactory baseDomainFactory) {
        super();
        this.tractracDomainFactory = new DomainFactoryImpl(baseDomainFactory);
        delayToLiveInMillis = TrackedRace.DEFAULT_LIVE_DELAY_IN_MILLISECONDS;
    }

    @Override
    public DomainFactory getTracTracDomainFactory() {
        return tractracDomainFactory;
    }
    
    @Override
    public RaceHandle addTracTracRace(TrackerManager trackerManager, URL paramURL, URI liveURI, URI storedURI,
            URI courseDesignUpdateURI, RaceLogStore raceLogStore, RegattaLogStore regattaLogStore,
            long timeoutInMilliseconds, String tracTracApiToken, String raceStatus, String raceVisibility,
            boolean trackWind, boolean correctWindDirectionByMagneticDeclination, int timeoutInMillis, boolean useOfficialEventsToUpdateRaceLog,
            RaceTrackingHandler raceTrackingHandler) throws Exception {
        return trackerManager.addRace(
                /* regattaToAddTo */null,
                getTracTracDomainFactory().createTrackingConnectivityParameters(paramURL, liveURI, storedURI,
                        courseDesignUpdateURI,
                        /* startOfTracking */null,
                        /* endOfTracking */null, delayToLiveInMillis, /* offsetToStartTimeOfSimulatedRace */null, /* ignoreTracTracMarkPassings */ false,
                        raceLogStore, regattaLogStore, tracTracApiToken, raceStatus, raceVisibility, trackWind, correctWindDirectionByMagneticDeclination,
                        /* preferReplayIfAvailable */ false, timeoutInMillis, useOfficialEventsToUpdateRaceLog,
                        /* liveURIFromConfiguration */ null, /* storedURIFromConfiguration */ null),
                timeoutInMilliseconds, raceTrackingHandler);
    }

    @Override
    public RaceHandle addTracTracRace(TrackerManager trackerManager, RegattaIdentifier regattaToAddTo, URL paramURL,
            URI liveURI, URI storedURI, URI courseDesignUpdateURI, TimePoint startOfTracking, TimePoint endOfTracking,
            RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, long timeoutInMilliseconds,
            Duration offsetToStartTimeOfSimulatedRace, boolean useInternalMarkPassingAlgorithm, String tracTracApiToken,
            String raceStatus, String raceVisibility, boolean trackWind, boolean correctWindDirectionByMagneticDeclination,
            boolean useOfficialEventsToUpdateRaceLog, URI liveURIFromConfiguration, URI storedURIFromConfiguration) throws Exception {
        return trackerManager.addRace(
                regattaToAddTo,
                getTracTracDomainFactory().createTrackingConnectivityParameters(paramURL, liveURI, storedURI,
                        courseDesignUpdateURI, startOfTracking, endOfTracking, delayToLiveInMillis,
                        offsetToStartTimeOfSimulatedRace, useInternalMarkPassingAlgorithm, raceLogStore, regattaLogStore, tracTracApiToken,
                        raceStatus, raceVisibility, trackWind, correctWindDirectionByMagneticDeclination, /* preferReplayIfAvailable */ false,
                        (int) timeoutInMilliseconds, useOfficialEventsToUpdateRaceLog, liveURIFromConfiguration, storedURIFromConfiguration), timeoutInMilliseconds);
    }

    @Override
    public Util.Pair<String, List<RaceRecord>> getTracTracRaceRecords(URL jsonURL, boolean loadClientParams)
            throws IOException, ParseException, org.json.simple.parser.ParseException, URISyntaxException {
        logger.info("Retrieving TracTrac race records from " + jsonURL);
        JSONService jsonService = getTracTracDomainFactory().parseJSONURLWithRaceRecords(jsonURL, loadClientParams);
        logger.info("OK retrieving TracTrac race records from " + jsonURL);
        return new Util.Pair<String, List<RaceRecord>>(jsonService.getEventName(), jsonService.getRaceRecords());
    }

    @Override
    public RaceRecord getSingleTracTracRaceRecord(URL jsonURL, String raceId, boolean loadClientParams)
            throws Exception {
        JSONService service = getTracTracDomainFactory().parseJSONURLForOneRaceRecord(jsonURL, raceId, loadClientParams);
        if (!service.getRaceRecords().isEmpty()) {
            return service.getRaceRecords().get(0);
        }
        return null;
    }
    
    @Override
    public TracTracConfiguration createTracTracConfiguration(String creatorName, String name, String jsonURL,
            String liveDataURI,
            String storedDataURI, String courseDesignUpdateURI, String tracTracApiToken) {
        return getTracTracDomainFactory().createTracTracConfiguration(creatorName, name, jsonURL, liveDataURI,
                storedDataURI, courseDesignUpdateURI, tracTracApiToken);
    }

}
