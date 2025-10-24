package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotableForRaceLogTrackingException;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.domain.racelogtracking.impl.RaceLogConnectivityParams;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceLogConnectivityParamsLoadAndStoreTest extends AbstractConnectivityParamsLoadAndStoreTest {
    public RaceLogConnectivityParamsLoadAndStoreTest() throws UnknownHostException, MongoException {
        super();
    }

    @Test
    public void testStoreAndLoadRaceLogTrackingParams() throws MalformedURLException, URISyntaxException, NotDenotableForRaceLogTrackingException, InterruptedException, ExecutionException {
        // set up
        final long delayToLiveInMillis = 3000;
        final boolean trackWind = true;
        final boolean correctWindDirectionByMagneticDeclination = true;
        final FleetImpl fleet = new FleetImpl("Default");
        final SeriesImpl theSeries = new SeriesImpl("Default", /* isMedal */ false, /* isFleetsCanRunInParallel */ true,
                Arrays.<Fleet>asList(fleet), Collections.emptyList(), racingEventService);
        final Iterable<? extends Series> series = Arrays.<Series>asList(theSeries);
        final Regatta regatta = racingEventService.createRegatta("My Regatta", "12mR", true,
                CompetitorRegistrationType.CLOSED, /* registrationLinkSecret */ null, MillisecondsTimePoint.now(),
                MillisecondsTimePoint.now().plus(Duration.ONE_DAY), UUID.randomUUID(), series, /* persistent */ true,
                new LowPoint(), racingEventService.getBaseDomainFactory().getOrCreateCourseArea(UUID.randomUUID(), "Default", /* centerPosition */ null, /* radius */ null).getId(),
                /* buoyZoneRadiusInHullLengths */ 2.0,
                /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        theSeries.addRaceColumn("R1", racingEventService);
        theSeries.addRaceColumn("R2", racingEventService);
        theSeries.addRaceColumn("R3", racingEventService);
        final RegattaLeaderboard leaderboard = racingEventService.addRegattaLeaderboard(regatta.getRegattaIdentifier(), /* leaderboardDisplayName */ null, new int[] { 5, 9 });
        RaceLogTrackingAdapterFactory.INSTANCE.getAdapter(domainObjectFactory.getBaseDomainFactory()).denoteAllRacesForRaceLogTracking(racingEventService, leaderboard,
                /* race name prefix */ null);
        final RaceLogConnectivityParams rlParams = new RaceLogConnectivityParams(
                racingEventService.getServerAuthor(), regatta, leaderboard.getRaceColumnByName("R2"), fleet, leaderboard,
                delayToLiveInMillis, domainObjectFactory.getBaseDomainFactory(), trackWind,
                correctWindDirectionByMagneticDeclination);
        // store
        mongoObjectFactory.addConnectivityParametersForRaceToRestore(rlParams);
        // load
        final Set<RaceTrackingConnectivityParameters> connectivityParametersForRacesToRestore = new HashSet<>();
        domainObjectFactory.loadConnectivityParametersForRacesToRestore(params -> connectivityParametersForRacesToRestore.add(params))
                .waitForCompletionOfCallbacksForAllParameters();
        // compare
        assertEquals(1, Util.size(connectivityParametersForRacesToRestore));
        final RaceTrackingConnectivityParameters paramsReadFromDB = connectivityParametersForRacesToRestore.iterator().next();
        assertTrue(paramsReadFromDB instanceof RaceLogConnectivityParams);
        RaceLogConnectivityParams raceLogParamsReadFromDB = (RaceLogConnectivityParams) paramsReadFromDB;
        assertEquals(delayToLiveInMillis, raceLogParamsReadFromDB.getDelayToLiveInMillis());
        assertSame(leaderboard, raceLogParamsReadFromDB.getLeaderboard());
        assertSame(theSeries.getRaceColumnByName("R2"), raceLogParamsReadFromDB.getRaceColumn());
        assertSame(fleet, raceLogParamsReadFromDB.getFleet());
        assertEquals(rlParams.getTrackerID(), raceLogParamsReadFromDB.getTrackerID());
        assertEquals(rlParams.isTrackWind(), raceLogParamsReadFromDB.isTrackWind());
        assertEquals(rlParams.isCorrectWindDirectionByMagneticDeclination(), raceLogParamsReadFromDB.isCorrectWindDirectionByMagneticDeclination());
        // remove again
        mongoObjectFactory.removeConnectivityParametersForRaceToRestore(rlParams);
        final Set<RaceTrackingConnectivityParameters> connectivityParametersForRacesToRestore2 = new HashSet<>();
        domainObjectFactory.loadConnectivityParametersForRacesToRestore(params->connectivityParametersForRacesToRestore2.add(params))
            .waitForCompletionOfCallbacksForAllParameters();
        assertTrue(connectivityParametersForRacesToRestore2.isEmpty());
    }
}
