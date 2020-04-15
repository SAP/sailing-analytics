package com.sap.sailing.domain.tractracadapter.persistence.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.util.HttpUrlConnectionHelper;

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
    private static final String USE_INTERNAL_MARK_PASSING_ALGORITHM = "useInternalMarkPassingAlgorithm";
    private static final String USE_OFFICIAL_EVENTS_TO_UPDATE_RACE_LOG = "useOfficialEventsToUpdateRaceLog";
    private static final String TRAC_TRAC_USERNAME = "tracTracUsername";
    private static final String TRAC_TRAC_PASSWORD = "tracTracPassword";
    private static final String STORED_URI = "storedURI";
    private static final String START_OF_TRACKING_MILLIS = "startOfTrackingMillis";
    private static final String RACE_VISIBILITY = "raceVisibility";
    private static final String RACE_STATUS = "raceStatus";
    private static final String PARAM_URL = "paramURL";
    private static final String OFFSET_TO_START_TIME_OF_SIMULATED_RACE_MILLIS = "offsetToStartTimeOfSimulatedRaceMillis";
    private static final String LIVE_URI = "liveURI";
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
        result.put(COURSE_DESIGN_UPDATE_URI, ttParams.getCourseDesignUpdateURI()==null?null:ttParams.getCourseDesignUpdateURI().toString());
        result.put(DELAY_TO_LIVE_IN_MILLIS, ttParams.getDelayToLiveInMillis());
        result.put(END_OF_TRACKING_MILLIS, ttParams.getEndOfTracking()==null?null:ttParams.getEndOfTracking().asMillis());
        result.put(LIVE_URI, ttParams.getLiveURI()==null?null:ttParams.getLiveURI().toString());
        result.put(OFFSET_TO_START_TIME_OF_SIMULATED_RACE_MILLIS, ttParams.getOffsetToStartTimeOfSimulatedRace()==null?null:ttParams.getOffsetToStartTimeOfSimulatedRace().asMillis());
        result.put(RACE_STATUS, ttParams.getRaceStatus());
        result.put(RACE_VISIBILITY, ttParams.getRaceVisibility());
        result.put(START_OF_TRACKING_MILLIS, ttParams.getStartOfTracking()==null?null:ttParams.getStartOfTracking().asMillis());
        result.put(STORED_URI, ttParams.getStoredURI()==null?null:ttParams.getStoredURI().toString());
        result.put(TRAC_TRAC_PASSWORD, ttParams.getTracTracPassword());
        result.put(TRAC_TRAC_USERNAME, ttParams.getTracTracUsername().toString());
        result.put(USE_INTERNAL_MARK_PASSING_ALGORITHM, ttParams.isUseInternalMarkPassingAlgorithm());
        result.put(USE_OFFICIAL_EVENTS_TO_UPDATE_RACE_LOG, ttParams.isUseOfficialEventsToUpdateRaceLog());
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
                map.get(TRAC_TRAC_USERNAME)==null?null:map.get(TRAC_TRAC_USERNAME).toString(),
                map.get(TRAC_TRAC_PASSWORD)==null?null:map.get(TRAC_TRAC_PASSWORD).toString(),
                map.get(RACE_STATUS)==null?null:map.get(RACE_STATUS).toString(),
                map.get(RACE_VISIBILITY)==null?null:map.get(RACE_VISIBILITY).toString(), isTrackWind(map),
                isCorrectWindDirectionByMagneticDeclination(map), /* preferReplayIfAvailable */ true,
                /* default timeout for obtaining IRace object from params URL */ (int) RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                map.get(USE_OFFICIAL_EVENTS_TO_UPDATE_RACE_LOG) == null ? false : (Boolean) map.get(USE_OFFICIAL_EVENTS_TO_UPDATE_RACE_LOG));
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
                ttParams.getCourseDesignUpdateURI(), ttParams.getStartOfTracking(), ttParams.getEndOfTracking(),
                ttParams.getDelayToLiveInMillis(), ttParams.getOffsetToStartTimeOfSimulatedRace(),
                ttParams.isUseInternalMarkPassingAlgorithm(), raceLogStore, regattaLogStore, domainFactory,
                ttParams.getTracTracUsername(), ttParams.getTracTracPassword(), ttParams.getRaceStatus(),
                ttParams.getRaceVisibility(), ttParams.isTrackWind(),
                ttParams.isCorrectWindDirectionByMagneticDeclination(), ttParams.isPreferReplayIfAvailable(),
                ttParams.getTimeoutInMillis(), ttParams.isUseOfficialEventsToUpdateRaceLog());
        updatePersistentTracTracConfiguration(result);
        return result;
    }

    private void updatePersistentTracTracConfiguration(RaceTrackingConnectivityParametersImpl params)
            throws MalformedURLException, IOException, ParseException {
        final String EVENT_MANAGER_HOSTNAME_PREFIX = "em.";
        final URL paramsJsonUrl = new URL(params.getParamURL().getProtocol(),
                params.getParamURL().getHost().startsWith(EVENT_MANAGER_HOSTNAME_PREFIX) ? params.getParamURL().getHost() : EVENT_MANAGER_HOSTNAME_PREFIX+params.getParamURL().getHost(),
                        "/events/"+params.getTractracRace().getEvent().getId().toString()+"/races/"+params.getTractracRace().getId().toString()+".json");
        final URLConnection conn = HttpUrlConnectionHelper.redirectConnection(paramsJsonUrl);
        final JSONObject paramsJson = (JSONObject) new JSONParser().parse(new InputStreamReader(conn.getInputStream()));
        final String jsonURL = (String) paramsJson.get("eventJSON");
        final String creatorName = SessionUtils.getPrincipal().toString();
        final TracTracConfigurationImpl tracTracConfiguration = new TracTracConfigurationImpl(creatorName, params.getTractracRace().getEvent().getName(), jsonURL,
                /* live URI */ null, /* stored URI */ null, // we mainly want to enable the user to list the event's races again in case they are removed; live/stored stuff comes from the tracking params
                params.getCourseDesignUpdateURI()==null?null:params.getCourseDesignUpdateURI().toString(), params.getTracTracUsername(), params.getTracTracPassword());
        tractracMongoObjectFactory.updateTracTracConfiguration(tracTracConfiguration);
        final OwnershipAnnotation existingOwnership = securityService.getOwnership(tracTracConfiguration.getIdentifier());
        if (existingOwnership == null || existingOwnership.getAnnotation() == null ||
                (existingOwnership.getAnnotation().getTenantOwner() == null &&
                    existingOwnership.getAnnotation().getUserOwner() == null)) {
            securityService.setDefaultOwnership(tracTracConfiguration.getIdentifier(), tracTracConfiguration.getName());
        }
    }
}
