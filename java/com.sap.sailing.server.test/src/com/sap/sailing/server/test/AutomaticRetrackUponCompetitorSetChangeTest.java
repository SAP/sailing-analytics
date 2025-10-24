package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.osgi.framework.BundleContext;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.media.MediaDBFactory;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.racelog.tracking.EmptySensorFixStore;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.test.AbstractTracTracLiveTest;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.impl.RacingEventServiceImpl.ConstructorParameters;
import com.sap.sailing.server.operationaltransformation.AddColumnToSeries;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboard;
import com.sap.sailing.server.operationaltransformation.UpdateSeries;
import com.sap.sailing.server.testsupport.SecurityBundleTestWrapper;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.security.SecurityService;

/**
 * See also bug 5219 (https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5219).
 *
 * @author Axel Uhl (D043530)
 *
 */
public class AutomaticRetrackUponCompetitorSetChangeTest {
    private static final String FIRST_RACE_COLUMN_NAME = "R1";
    private static final String BLUE_FLEET_NAME = "Blue";
    private static final String GREEN_FLEET_NAME = "Green";
    private static final String RED_FLEET_NAME = "Red";
    private TrackedRace trackedRace;
    private RegattaAndRaceIdentifier raceIdentifier;
    private RaceHandle racesHandle;
    private RacingEventServiceImpl service;
    private MongoDBService mongoDBService;
    private MongoObjectFactory mongoObjectFactory;
    private RegattaName regattaIdentifier;

    @BeforeEach
    public void setUp() throws Exception {
        final BundleContext contextMock = mock(BundleContext.class);
        when(contextMock.createFilter(ArgumentMatchers.any())).thenReturn(null);
        mongoDBService = MongoDBService.INSTANCE;
        mongoDBService.getDB().drop();
        mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(mongoDBService);
        final SecurityService securityService = new SecurityBundleTestWrapper().initializeSecurityServiceForTesting();
        service = new RacingEventServiceImpl((final RaceLogAndTrackedRaceResolver raceLogResolver)-> {
            return new ConstructorParameters() {
                private final DomainFactory baseDomainFactory = new DomainFactoryImpl(raceLogResolver);
                @Override public DomainObjectFactory getDomainObjectFactory() { return PersistenceFactory.INSTANCE.getDomainObjectFactory(mongoDBService, baseDomainFactory); }
                @Override public MongoObjectFactory getMongoObjectFactory() { return mongoObjectFactory; }
                @Override public DomainFactory getBaseDomainFactory() { return baseDomainFactory; }
                @Override public CompetitorAndBoatStore getCompetitorAndBoatStore() { return getBaseDomainFactory().getCompetitorAndBoatStore(); }
            };
        }, MediaDBFactory.INSTANCE.getMediaDB(mongoDBService), EmptyWindStore.INSTANCE,
                EmptySensorFixStore.INSTANCE, null, null, /* sailingNotificationService */ null,
                /* trackedRaceStatisticsCache */ null, /* restoreTrackedRaces */ false,
                /* security service tracker */ new FullyInitializedReplicableTracker<SecurityService>(contextMock, (String) "class", null, null) {
                    @Override
                    public SecurityService getInitializedService(long timeoutInMillis) throws InterruptedException {
                        return securityService;
                    }
        }, /* sharedSailingData */ null, /* replicationServiceTracker */ null,
                /* scoreCorrectionProviderServiceTracker */ null, /* competitorProviderServiceTracker */ null,
                /* resultUrlRegistryServiceTracker */ null);
        final Regatta regatta = service.createRegatta("Test regatta", "J/70",
                /* canBoatsOfCompetitorsChangePerRace==true because it's a league race we're using for this test */ true,
                CompetitorRegistrationType.CLOSED, /* registrationLinkSecret */ null,
                /* startDate */ null, /* endDate */ null, UUID.randomUUID(),
                /* start with no series */ Collections.emptySet(),
                /* persistent */ true, new LowPoint(),
                /* defaultCourseAreaId */ service.getBaseDomainFactory()
                        .getOrCreateCourseArea(UUID.randomUUID(), "Course Area", /* centerPosition */ null, /* radius */ null).getId(),
                /* buoyZoneRadiusInHullLengths */ 2., /* useStartTimeInference */ false, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ true, /* rankingMetricConstructor */ OneDesignRankingMetric::new);
        regattaIdentifier = new RegattaName(regatta.getName());
        final String seriesName = "Default";
        service.apply(new UpdateSeries(regattaIdentifier, seriesName, seriesName, /* isMedal */ false, /* isFleetsCanRunInParallel */ false,
                /* resultDiscardingThresholds */ null, /* startsWithZeroScore */ false, /* firstColumnIsNonDiscardableCarryForward */ false,
                /* hasSplitFleetContiguousScoring */ false, /* hasCrossFleetMergedRanking */ false, /* maximumNumberOfDiscards */ null, /* oneAlwaysStaysOne */ false,
                Arrays.asList(new FleetDTO(RED_FLEET_NAME, 0, Color.RED), new FleetDTO(GREEN_FLEET_NAME, 0, Color.GREEN), new FleetDTO(BLUE_FLEET_NAME, 0, Color.BLUE))));
        service.apply(new CreateRegattaLeaderboard(regattaIdentifier, /* leaderboardDisplayName */ null, new int[0]));
        service.apply(new AddColumnToSeries(regattaIdentifier, seriesName, FIRST_RACE_COLUMN_NAME));
    }

    private void startTrackingTracTrac() throws Exception {
        URL paramURL = new URL("http://event.tractrac.com/events/event_20150818_Bundesliga/4c54e750-27c2-0133-5064-60a44ce903c3.txt");
        URI liveURI = AbstractTracTracLiveTest.getLiveURI();
        URI storedURI = new URI("http://event.tractrac.com/events/event_20150818_Bundesliga/datafiles/4c54e750-27c2-0133-5064-60a44ce903c3.mtb");
        URI courseDesignUpdateURI = AbstractTracTracLiveTest.getCourseDesignUpdateURI();
        String tracTracUsername = AbstractTracTracLiveTest.getTracTracUsername();
        String tracTracPassword = AbstractTracTracLiveTest.getTracTracPassword();
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2015, 8, 22, 9, 23, 57);
        MillisecondsTimePoint startOfTracking = new MillisecondsTimePoint(cal.getTimeInMillis());
        cal.set(2015, 8, 22, 15, 26, 34);
        MillisecondsTimePoint endOfTracking = new MillisecondsTimePoint(cal.getTimeInMillis());
        final RaceTrackingConnectivityParameters trackingParams = new com.sap.sailing.domain.tractracadapter.impl.DomainFactoryImpl(service.getBaseDomainFactory())
                .createTrackingConnectivityParameters(paramURL, liveURI, storedURI, courseDesignUpdateURI,
                        startOfTracking, endOfTracking, /* delayToLiveInMillis */
                        0l, /* offsetToStartTimeOfSimulatedRace */null, /*ignoreTracTracMarkPassings*/ false, EmptyRaceLogStore.INSTANCE,
                        EmptyRegattaLogStore.INSTANCE, tracTracUsername, tracTracPassword, "", "", /* trackWind */ false, /* correctWindDirectionByMagneticDeclination */ false,
                        /* preferReplayIfAvailable */ false, /* timeoutInMillis */ (int) RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                        /* useOfficialEventsToUpdateRaceLog */ false, /* liveURIFromConfiguration */ null, /* storedURIFromConfiguration */ null);
        racesHandle = service.addRace(/* regattaToAddTo */ regattaIdentifier, trackingParams, /* timeoutInMilliseconds */ 60000,
                new DefaultRaceTrackingHandler());
        waitForRace();
    }

    /**
     * Waits for the race to show up and initializes {@link #raceIdentifier} and {@link #trackedRace}.
     */
    private void waitForRace() {
        // wait for the race to show up
        RaceDefinition race = racesHandle.getRace(RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS);
        if (race == null) {
            fail("Waiting for tracked race timed out");
        }
        raceIdentifier = racesHandle.getRaceTracker().getRaceIdentifier();
        trackedRace = service.getTrackedRace(raceIdentifier);
    }

    @Test
    public void testStartTracTracTrackingAndRetrack() throws Exception {
        startTrackingTracTrac();
        final RaceDefinition race = trackedRace.getRace();
        Iterable<Competitor> masterCompetitors = race.getCompetitors();
        assertEquals(Util.size(masterCompetitors), 6);
        final RaceHandle newHandle = service.updateRaceCompetitors(trackedRace.getTrackedRegatta().getRegatta(), race);
        final RaceDefinition newRace = newHandle.getRace(RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS);
        assertNotNull(newRace);
        assertEquals(race.getId(), newRace.getId());
        assertNotSame(race, newRace);
        racesHandle = newHandle; // ensure that tearDown tears down the correct tracker
    }

    @Test
    public void testStartRaceLogTrackingAndAddCompetitor() throws NotDenotedForRaceLogTrackingException, Exception {
        final RaceLog raceLog = service.getRaceLog(regattaIdentifier.getRegattaName(), FIRST_RACE_COLUMN_NAME, RED_FLEET_NAME);
        assertNotNull(raceLog);
        final RegattaLog regattaLog = service.getRegatta(regattaIdentifier).getRegattaLog();
        final CompetitorWithBoat firstCompetitor = TrackBasedTest.createCompetitorWithBoat("First Competitor");
        final CompetitorWithBoat secondCompetitor = TrackBasedTest.createCompetitorWithBoat("Second Competitor");
        final RegattaLogRegisterCompetitorEventImpl firstCompetitorRegistrationEvent = new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), service.getServerAuthor(), firstCompetitor);
        regattaLog.add(firstCompetitorRegistrationEvent);
        final RaceLogTrackingAdapter factory = RaceLogTrackingAdapterFactory.INSTANCE.getAdapter(service.getBaseDomainFactory());
        final Leaderboard leaderboard = service.getLeaderboardByName(regattaIdentifier.getRegattaName());
        final RaceColumn raceColumn = leaderboard.getRaceColumnByName(FIRST_RACE_COLUMN_NAME);
        final Fleet redFleet = raceColumn.getFleetByName(RED_FLEET_NAME);
        factory.denoteAllRacesForRaceLogTracking(service, leaderboard, /* prefix */ "R");
        racesHandle = factory.startTracking(service, leaderboard, raceColumn, redFleet, /* trackWind */ false,
                /* correctWindDirectionByMagneticDeclination */ true, new DefaultRaceTrackingHandler());
        waitForRace();
        final RegattaAndRaceIdentifier raceIdentifier = trackedRace.getRaceIdentifier();
        assertEquals(1, Util.size(trackedRace.getRace().getCompetitors()));
        assertSame(firstCompetitor, trackedRace.getRace().getCompetitors().iterator().next());
        regattaLog.add(new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), service.getServerAuthor(), secondCompetitor));
        final RaceTracker raceTracker = getRaceTracker(raceIdentifier);
        final RaceDefinition newRace = raceTracker.getRace();
        assertEquals(2, Util.size(newRace.getCompetitors()));
        assertTrue(Util.contains(newRace.getCompetitors(), firstCompetitor));
        assertTrue(Util.contains(newRace.getCompetitors(), secondCompetitor));
        // now revoke the registration of the first competitor and verify that it disappears:
        regattaLog.revokeEvent(service.getServerAuthor(), firstCompetitorRegistrationEvent);
        final RaceTracker newRaceTracker = getRaceTracker(raceIdentifier);
        final RaceDefinition newNewRace = newRaceTracker.getRace();
        assertEquals(1, Util.size(newNewRace.getCompetitors()));
        assertFalse(Util.contains(newNewRace.getCompetitors(), firstCompetitor));
        assertTrue(Util.contains(newNewRace.getCompetitors(), secondCompetitor));
    }

    private RaceTracker getRaceTracker(final RegattaAndRaceIdentifier raceIdentifier)
            throws InterruptedException, ExecutionException, TimeoutException {
        final CompletableFuture<RaceTracker> raceTrackerFuture = new CompletableFuture<>();
        service.getRaceTrackerByRegattaAndRaceIdentifier(raceIdentifier, raceTracker->raceTrackerFuture.complete(raceTracker));
        final RaceTracker raceTracker = raceTrackerFuture.get(RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        return raceTracker;
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (racesHandle != null) {
            racesHandle.getRaceTracker().stop(/* preemptive */ false);
        }
    }
}
