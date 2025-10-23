package com.sap.sailing.domain.tractracadapter.persistence.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.impl.AbstractRaceTrackingConnectivityParametersHandler;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.impl.RaceTrackingConnectivityParametersImpl;
import com.sap.sailing.domain.tractracadapter.impl.TracTracConfigurationImpl;
import com.sap.sailing.domain.tractracadapter.impl.TracTracRaceTrackerImpl;
import com.sap.sailing.domain.tractracadapter.persistence.MongoObjectFactory;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SessionUtils;
import com.tractrac.model.lib.api.event.CreateModelException;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.util.lib.api.exceptions.TimeOutException;

/**
 * Handles mapping TracTrac connectivity parameters from and to a map with {@link String} keys. The
 * "param URL" is considered the {@link #getKey(RaceTrackingConnectivityParameters) key} for these objects.<p>
 * 
 * Lives in the same package as {@link RaceTrackingConnectivityParameters} for package-private access to
 * its members.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TracTracConnectivityParamsHandler extends AbstractRaceTrackingConnectivityParametersHandler {
    private static final Logger logger = Logger.getLogger(TracTracConnectivityParamsHandler.class.getName());

    private static final String USE_INTERNAL_MARK_PASSING_ALGORITHM = "useInternalMarkPassingAlgorithm";
    private static final String USE_OFFICIAL_EVENTS_TO_UPDATE_RACE_LOG = "useOfficialEventsToUpdateRaceLog";
    private static final String TRAC_TRAC_API_TOKEN = "tracTracApiToken";
    private static final String STORED_URI = "storedURI";
    private static final String STORED_URI_FROM_CONFIGURATION = "storedURIFromConfiguration";
    private static final String START_OF_TRACKING_MILLIS = "startOfTrackingMillis";
    private static final String RACE_VISIBILITY = "raceVisibility";
    private static final String RACE_STATUS = "raceStatus";
    private static final String PARAM_URL = "paramURL";
    private static final String OFFSET_TO_START_TIME_OF_SIMULATED_RACE_MILLIS = "offsetToStartTimeOfSimulatedRaceMillis";
    private static final String LIVE_URI = "liveURI";
    private static final String LIVE_URI_FROM_CONFIGURATION = "liveURIFromConfiguration";
    private static final String END_OF_TRACKING_MILLIS = "endOfTrackingMillis";
    private static final String DELAY_TO_LIVE_IN_MILLIS = "delayToLiveInMillis";
    private static final String COURSE_DESIGN_UPDATE_URI = "courseDesignUpdateURI";
    private final RaceLogStore raceLogStore;
    private final RegattaLogStore regattaLogStore;
    private final DomainFactory domainFactory;
    private final MongoObjectFactory tractracMongoObjectFactory;
    private final SecurityService securityService;

    public TracTracConnectivityParamsHandler(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore,
            DomainFactory domainFactory, MongoObjectFactory tractracMongoObjectFactory,
            SecurityService securityService) {
        super();
        this.raceLogStore = raceLogStore;
        this.regattaLogStore = regattaLogStore;
        this.domainFactory = domainFactory;
        this.tractracMongoObjectFactory = tractracMongoObjectFactory;
        this.securityService = securityService;
    }

    @Override
    public Map<String, Object> mapFrom(RaceTrackingConnectivityParameters params) throws MalformedURLException {
        assert params instanceof RaceTrackingConnectivityParametersImpl;
        final RaceTrackingConnectivityParametersImpl ttParams = (RaceTrackingConnectivityParametersImpl) params;
        final Map<String, Object> result = getKey(params);
        result.put(COURSE_DESIGN_UPDATE_URI, ttParams.getUpdateURI()==null?null:ttParams.getUpdateURI().toString());
        result.put(DELAY_TO_LIVE_IN_MILLIS, ttParams.getDelayToLiveInMillis());
        result.put(END_OF_TRACKING_MILLIS, ttParams.getEndOfTracking()==null?null:ttParams.getEndOfTracking().asMillis());
        result.put(LIVE_URI, ttParams.getLiveURI()==null?null:ttParams.getLiveURI().toString());
        result.put(OFFSET_TO_START_TIME_OF_SIMULATED_RACE_MILLIS, ttParams.getOffsetToStartTimeOfSimulatedRace()==null?null:ttParams.getOffsetToStartTimeOfSimulatedRace().asMillis());
        result.put(RACE_STATUS, ttParams.getRaceStatus());
        result.put(RACE_VISIBILITY, ttParams.getRaceVisibility());
        result.put(START_OF_TRACKING_MILLIS, ttParams.getStartOfTracking()==null?null:ttParams.getStartOfTracking().asMillis());
        result.put(STORED_URI, ttParams.getStoredURI()==null?null:ttParams.getStoredURI().toString());
        result.put(TRAC_TRAC_API_TOKEN, ttParams.getTracTracApiToken());
        result.put(USE_INTERNAL_MARK_PASSING_ALGORITHM, ttParams.isUseInternalMarkPassingAlgorithm());
        result.put(USE_OFFICIAL_EVENTS_TO_UPDATE_RACE_LOG, ttParams.isUseOfficialEventsToUpdateRaceLog());
        result.put(LIVE_URI_FROM_CONFIGURATION, ttParams.getLiveURIFromConfiguration()==null?null:ttParams.getLiveURIFromConfiguration().toString());
        result.put(STORED_URI_FROM_CONFIGURATION, ttParams.getStoredURIFromConfiguration()==null?null:ttParams.getStoredURIFromConfiguration().toString());
        addWindTrackingParameters(ttParams, result);
        return result;
    }

    @Override
    public RaceTrackingConnectivityParameters mapTo(Map<String, Object> map) throws Exception {
        return new RaceTrackingConnectivityParametersImpl(
                new URL(map.get(PARAM_URL).toString()),
                map.get(LIVE_URI) == null ? null : new URI(map.get(LIVE_URI).toString()),
                map.get(STORED_URI) == null ? null : new URI(map.get(STORED_URI).toString()),
                map.get(COURSE_DESIGN_UPDATE_URI) == null ? null : new URI(map.get(COURSE_DESIGN_UPDATE_URI).toString()),
                map.get(START_OF_TRACKING_MILLIS) == null ? null : new MillisecondsTimePoint(((Number) map.get(START_OF_TRACKING_MILLIS)).longValue()),
                map.get(END_OF_TRACKING_MILLIS) == null ? null : new MillisecondsTimePoint(((Number) map.get(END_OF_TRACKING_MILLIS)).longValue()),
                ((Number) map.get(DELAY_TO_LIVE_IN_MILLIS)).longValue(),
                map.get(OFFSET_TO_START_TIME_OF_SIMULATED_RACE_MILLIS) == null ? null : new MillisecondsDurationImpl(((Number) map.get(OFFSET_TO_START_TIME_OF_SIMULATED_RACE_MILLIS)).longValue()),
                (Boolean) map.get(USE_INTERNAL_MARK_PASSING_ALGORITHM),
                raceLogStore, regattaLogStore, domainFactory,
                map.get(TRAC_TRAC_API_TOKEN)==null?null:map.get(TRAC_TRAC_API_TOKEN).toString(),
                map.get(RACE_STATUS)==null?null:map.get(RACE_STATUS).toString(),
                map.get(RACE_VISIBILITY)==null?null:map.get(RACE_VISIBILITY).toString(), isTrackWind(map),
                isCorrectWindDirectionByMagneticDeclination(map), /* preferReplayIfAvailable */ true,
                /* default timeout for obtaining IRace object from params URL */ (int) RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                map.get(USE_OFFICIAL_EVENTS_TO_UPDATE_RACE_LOG) == null ? false : (Boolean) map.get(USE_OFFICIAL_EVENTS_TO_UPDATE_RACE_LOG),
                map.get(LIVE_URI_FROM_CONFIGURATION) == null ? null : new URI(map.get(LIVE_URI_FROM_CONFIGURATION).toString()),
                map.get(STORED_URI_FROM_CONFIGURATION) == null ? null : new URI(map.get(STORED_URI_FROM_CONFIGURATION).toString()));
    }

    @Override
    public Map<String, Object> getKey(RaceTrackingConnectivityParameters params) throws MalformedURLException {
        assert params instanceof RaceTrackingConnectivityParametersImpl;
        final RaceTrackingConnectivityParametersImpl ttParams = (RaceTrackingConnectivityParametersImpl) params;
        final Map<String, Object> result = new HashMap<>();
        result.put(TypeBasedServiceFinder.TYPE, params.getTypeIdentifier());
        result.put(PARAM_URL, TracTracRaceTrackerImpl.getParamURLStrippedOfRandomParam(new URL(ttParams.getParamURL().toString())).toString());
        return result;
    }

    @Override
    public RaceTrackingConnectivityParameters resolve(RaceTrackingConnectivityParameters params) throws Exception {
        assert params instanceof RaceTrackingConnectivityParametersImpl;
        final RaceTrackingConnectivityParametersImpl ttParams = (RaceTrackingConnectivityParametersImpl) params;
        RaceTrackingConnectivityParametersImpl result = new RaceTrackingConnectivityParametersImpl(
                ttParams.getParamURL(), ttParams.getLiveURI(), ttParams.getStoredURI(),
                ttParams.getUpdateURI(), ttParams.getStartOfTracking(), ttParams.getEndOfTracking(),
                ttParams.getDelayToLiveInMillis(), ttParams.getOffsetToStartTimeOfSimulatedRace(),
                ttParams.isUseInternalMarkPassingAlgorithm(), raceLogStore, regattaLogStore, domainFactory,
                ttParams.getTracTracApiToken(), ttParams.getRaceStatus(),
                ttParams.getRaceVisibility(), ttParams.isTrackWind(),
                ttParams.isCorrectWindDirectionByMagneticDeclination(), ttParams.isPreferReplayIfAvailable(),
                ttParams.getTimeoutInMillis(), ttParams.isUseOfficialEventsToUpdateRaceLog(),
                ttParams.getLiveURIFromConfiguration(), ttParams.getStoredURIFromConfiguration());
        updatePersistentTracTracConfiguration(result);
        return result;
    }

    private void updatePersistentTracTracConfiguration(RaceTrackingConnectivityParametersImpl params)
            throws MalformedURLException, IOException, ParseException, CreateModelException, URISyntaxException, TimeOutException {
        final IRace tractracRace = params.getTractracRace();
        final String jsonURL = tractracRace.getParameterSet().getParameter("eventJSON");
        if (jsonURL == null) {
            logger.warning("No eventJSON field set in TracTrac race "+tractracRace.getName()+". Cannot add configuration to list.");
        } else {
            final String creatorName = SessionUtils.getPrincipal().toString();
            final TracTracConfigurationImpl tracTracConfiguration = new TracTracConfigurationImpl(creatorName, tractracRace.getEvent().getName(), jsonURL,
                    // FIXME bug5983: stored/live URIs should be captured in the configuration only if they were specified explicitly when the parameters were created
                    (params.getLiveURIFromConfiguration() == null ? null : params.getLiveURIFromConfiguration().toString()),
                    /* stored URI */ params.isReplayRace(tractracRace) ? null // we mainly want to enable the user to list the event's races again in case they are removed;
                        : (params.getStoredURIFromConfiguration() == null ? null : params.getStoredURIFromConfiguration().toString()), // live/stored stuff comes from the tracking params
                    params.getUpdateURI()==null?null:params.getUpdateURI().toString(), params.getTracTracApiToken());
            tractracMongoObjectFactory.updateTracTracConfiguration(tracTracConfiguration, /* isTracTracApiTokenAvailable */ true);
            securityService.setDefaultOwnershipIfNotSet(tracTracConfiguration.getIdentifier());
        }
    }
}
