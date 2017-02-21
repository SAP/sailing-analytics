package com.sap.sailing.domain.racelogtracking.impl;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.impl.AbstractRaceTrackingConnectivityParametersHandler;
import com.sap.sailing.server.RacingEventService;
import com.sap.sse.common.TypeBasedServiceFinder;

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
public class RaceLogConnectivityParamsHandler extends AbstractRaceTrackingConnectivityParametersHandler {
    private static final String FLEET_NAME = "fleetName";
    private static final String RACE_COLUMN_NAME = "raceColumnName";
    private static final String LEADERBOARD_NAME = "leaderboardName";
    private static final String DELAY_TO_LIVE_IN_MILLIS = "delayToLiveInMillis";
    private final DomainFactory domainFactory;
    private final RacingEventService racingEventService;

    public RaceLogConnectivityParamsHandler(RacingEventService racingEventService) {
        super();
        this.racingEventService = racingEventService;
        this.domainFactory = racingEventService.getBaseDomainFactory();
    }

    @Override
    public Map<String, Object> mapFrom(RaceTrackingConnectivityParameters params) {
        assert params instanceof RaceLogConnectivityParams;
        final RaceLogConnectivityParams rlParams = (RaceLogConnectivityParams) params;
        final Map<String, Object> result = getKey(params);
        result.put(DELAY_TO_LIVE_IN_MILLIS, rlParams.getDelayToLiveInMillis());
        addWindTrackingParameters(rlParams, result);
        return result;
    }

    @Override
    public RaceTrackingConnectivityParameters mapTo(Map<String, Object> map) throws MalformedURLException, URISyntaxException {
        final RegattaLeaderboard leaderboard = (RegattaLeaderboard) racingEventService.getLeaderboardByName((String) map.get(LEADERBOARD_NAME));
        final RaceColumn raceColumn = leaderboard.getRaceColumnByName((String) map.get(RACE_COLUMN_NAME));
        return new RaceLogConnectivityParams(racingEventService, leaderboard.getRegatta(), raceColumn, 
                raceColumn.getFleetByName((String) map.get(FLEET_NAME)), leaderboard,
                ((Number) map.get(DELAY_TO_LIVE_IN_MILLIS)).longValue(), domainFactory, isTrackWind(map),
                isCorrectWindDirectionByMagneticDeclination(map));
    }

    @Override
    public Map<String, Object> getKey(RaceTrackingConnectivityParameters params) {
        assert params instanceof RaceLogConnectivityParams;
        final RaceLogConnectivityParams rlParams = (RaceLogConnectivityParams) params;
        final Map<String, Object> result = new HashMap<>();
        result.put(TypeBasedServiceFinder.TYPE, params.getTypeIdentifier());
        result.put(LEADERBOARD_NAME, rlParams.getLeaderboard().getName());
        result.put(RACE_COLUMN_NAME, rlParams.getRaceColumn().getName());
        result.put(FLEET_NAME, rlParams.getFleet().getName());
        return result;
    }
}
