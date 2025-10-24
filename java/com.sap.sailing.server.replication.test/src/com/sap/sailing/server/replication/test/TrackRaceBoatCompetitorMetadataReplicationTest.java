package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.test.AbstractTracTracLiveTest;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tractracadapter.impl.DomainFactoryImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.CreateTrackedRace;
import com.sap.sailing.server.operationaltransformation.UpdateSeries;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.replication.OperationExecutionListener;
import com.sap.sse.replication.OperationWithResult;

public class TrackRaceBoatCompetitorMetadataReplicationTest extends AbstractServerReplicationTest {
    private TrackedRace masterTrackedRace;
    private RegattaAndRaceIdentifier raceIdentifier;
    private RaceHandle racesHandle;
    private final boolean[] notifier = new boolean[1];
    private RaceTrackingConnectivityParameters trackingParams;

    @Test
    public void testTearDownIsNotBeingCalledWhenSetUpFailsWithAnException() {
        // no-op
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        URL paramURL = new URL("http://event.tractrac.com/events/event_20150818_Bundesliga/4c54e750-27c2-0133-5064-60a44ce903c3.txt");
        URI liveURI = AbstractTracTracLiveTest.getLiveURI();
        URI storedURI = AbstractTracTracLiveTest.getStoredURI();
        URI courseDesignUpdateURI = AbstractTracTracLiveTest.getCourseDesignUpdateURI();
        String tracTracUsername = AbstractTracTracLiveTest.getTracTracUsername();
        String tracTracPassword = AbstractTracTracLiveTest.getTracTracPassword();
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2015, 8, 22, 9, 23, 57);
        MillisecondsTimePoint startOfTracking = new MillisecondsTimePoint(cal.getTimeInMillis());
        cal.set(2015, 8, 22, 15, 26, 34);
        MillisecondsTimePoint endOfTracking = new MillisecondsTimePoint(cal.getTimeInMillis());
        master.addOperationExecutionListener(new OperationExecutionListener<RacingEventService>() {
            @Override
            public <T> void executed(OperationWithResult<RacingEventService, T> operation) {
                if (operation instanceof CreateTrackedRace) {
                    synchronized (notifier) {
                        notifier[0] = true;
                        notifier.notifyAll();
                    }
                }
            }
        });
        trackingParams = new DomainFactoryImpl(master.getBaseDomainFactory())
                .createTrackingConnectivityParameters(paramURL, liveURI, storedURI, courseDesignUpdateURI,
                        startOfTracking, endOfTracking, /* delayToLiveInMillis */
                        0l, /* offsetToStartTimeOfSimulatedRace */null, /*ignoreTracTracMarkPassings*/ false, EmptyRaceLogStore.INSTANCE,
                        EmptyRegattaLogStore.INSTANCE, tracTracUsername, tracTracPassword, "", "", /* trackWind */ false, /* correctWindDirectionByMagneticDeclination */ false,
                        /* preferReplayIfAvailable */ false, /* timeoutInMillis */ (int) RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                        /* useOfficialEventsToUpdateRaceLog */ false, /* liveURIFromConfiguration */ null, /* storedURIFromConfiguration */ null);
    }

    private void startTracking() throws Exception, InterruptedException {
        startTrackingOnMaster();
        waitForTrackRaceReplicationTrigger();
        raceIdentifier = racesHandle.getRaceTracker().getRaceIdentifier();
        masterTrackedRace = master.getTrackedRace(raceIdentifier);
    }

    private void startTrackingOnMaster() throws Exception {
        final CourseArea courseArea = master.getBaseDomainFactory().getOrCreateCourseArea(UUID.randomUUID(), "Course Area", /* centerPosition */ null, /* radius */ null);
        // in production, a course area creation based on an event's venue creation would be
        // replicated; in test set-ups, the course area needs to be "replicated" manually:
        replica.getBaseDomainFactory().getOrCreateCourseArea(courseArea.getId(), courseArea.getName(), /* centerPosition */ null, /* radius */ null);
        final Regatta regatta = master.createRegatta("Test regatta", "J/70",
                /* canBoatsOfCompetitorsChangePerRace==true because it's a league race we're using for this test */ true,
                CompetitorRegistrationType.CLOSED, /* registrationLinkSecret */ null,
                /* startDate */ null, /* endDate */ null, UUID.randomUUID(),
                /* start with no series */ Collections.emptySet(),
                /* persistent */ true, new LowPoint(),
                /* defaultCourseAreaId */ courseArea.getId(),
                /* buoyZoneRadiusInHullLengths */ 2., /* useStartTimeInference */ false, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, /* rankingMetricConstructor */ OneDesignRankingMetric::new);
        final RegattaName regattaIdentifier = new RegattaName(regatta.getName());
        master.apply(new UpdateSeries(regattaIdentifier, "Default", "Default", /* isMedal */ false, /* isFleetsCanRunInParallel */ false,
                /* resultDiscardingThresholds */ null, /* startsWithZeroScore */ false, /* firstColumnIsNonDiscardableCarryForward */ false,
                /* hasSplitFleetContiguousScoring */ false, /* hasCrossFleetMergedRanking */ false, /* maximumNumberOfDiscards */ null,
                /* oneAlwaysStaysOne */ false, Arrays.asList(new FleetDTO("Red", 0, Color.RED), new FleetDTO("Green", 0, Color.GREEN), new FleetDTO("Blue", 0, Color.BLUE))));
        racesHandle = master.addRace(/* regattaToAddTo */ regattaIdentifier, trackingParams, /* timeoutInMilliseconds */ 60000,
                new DefaultRaceTrackingHandler());
    }

    private void waitForTrackRaceReplicationTrigger() throws InterruptedException, IllegalAccessException {
        while (!notifier[0]) {
            synchronized (notifier) {
                notifier.wait();
            }
        }
        replicaReplicator.waitUntilQueueIsEmpty();
    }

    @Test
    public void testStartTrackingRaceReplication() throws Exception {
        final String boat1CompetitorName = "CYC";
        final String boat1Name = "Boot 1";
        final String boat1Color = "#141414";
        final String boat2CompetitorName = "SVI";
        final String boat2Name = "Boot 2";
        final String boat2Color = "#606060";
        final String boat3CompetitorName = "BYCÜ";
        final String boat3Name = "Boot 3";
        final String boat3Color = "#0169EF";
        startTracking();
        Thread.sleep(5000);
        TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        assertNotNull(replicaTrackedRace);
        Iterable<Competitor> masterCompetitors = masterTrackedRace.getRace().getCompetitors();
        Iterable<Competitor> replicaCompetitors = replicaTrackedRace.getRace().getCompetitors();
        assertNotSame(masterTrackedRace, replicaTrackedRace);
        assertNotSame(masterTrackedRace.getRace(), replicaTrackedRace.getRace());
        assertEquals(Util.size(masterCompetitors), 6);
        assertEquals(Util.size(masterCompetitors), Util.size(replicaCompetitors));
        for (Competitor competitor : masterCompetitors) {
            Competitor replicaCompetitor = findCompetitor(replicaCompetitors, competitor);
            Boat competitorBoat = masterTrackedRace.getBoatOfCompetitor(competitor);
            switch (competitorBoat.getSailID()) {
                case boat1CompetitorName:
                    compareBoatOfCompetitors(masterTrackedRace.getRace().getBoatOfCompetitor(competitor),
                            replicaTrackedRace.getRace().getBoatOfCompetitor(replicaCompetitor), boat1Name, boat1Color);
                    break;
                case boat2CompetitorName:
                    compareBoatOfCompetitors(masterTrackedRace.getRace().getBoatOfCompetitor(competitor),
                            replicaTrackedRace.getRace().getBoatOfCompetitor(replicaCompetitor), boat2Name, boat2Color);
                    break;
                case boat3CompetitorName:
                    compareBoatOfCompetitors(masterTrackedRace.getRace().getBoatOfCompetitor(competitor),
                            replicaTrackedRace.getRace().getBoatOfCompetitor(replicaCompetitor), boat3Name, boat3Color);
                    break;
            }
        }
    }

    private void compareBoatOfCompetitors(Boat masterBoat, Boat replicaBoat, String expectedBoatName, String expectedBoatColor) {
        assertNotNull(masterBoat);
        assertNotNull(replicaBoat);
        assertEquals(masterBoat.getName(), replicaBoat.getName());
        assertEquals(masterBoat.getColor(), replicaBoat.getColor());
        assertEquals(expectedBoatName, replicaBoat.getName());
        assertEquals(expectedBoatColor, replicaBoat.getColor().getAsHtml());
    }

    private Competitor findCompetitor(Iterable<Competitor> competitors, Competitor otherCompetitor) {
        for (Competitor competitor : competitors) {
            if(competitor.getId().equals(otherCompetitor.getId())) {
                return competitor;
            }
        }
        return null;
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        if (racesHandle != null) {
            racesHandle.getRaceTracker().stop(/* preemptive */ false);
        }
        super.tearDown();
    }
}
