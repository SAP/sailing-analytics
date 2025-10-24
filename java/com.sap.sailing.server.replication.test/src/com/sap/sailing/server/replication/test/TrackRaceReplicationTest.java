package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogExcludeWindSourcesEventImpl;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.FlexibleRaceColumn;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.test.AbstractTracTracLiveTest;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.ConnectTrackedRaceToLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateTrackedRace;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.replication.OperationExecutionListener;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.shared.util.Wait;

public class TrackRaceReplicationTest extends AbstractServerReplicationTest {
    private static final Logger logger = Logger.getLogger(TrackRaceReplicationTest.class.getName());
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
        final String eventID = "event_20110505_SailingTea";
        final String raceID = "bd8c778e-7c65-11e0-8236-406186cbf87c";
        URL paramURL = AbstractTracTracLiveTest.getParamURL(eventID, raceID);
        URI liveURI = AbstractTracTracLiveTest.getLiveURI();
        URI storedURI = AbstractTracTracLiveTest.getStoredURI();
        URI courseDesignUpdateURI = AbstractTracTracLiveTest.getCourseDesignUpdateURI();
        String tracTracUsername = AbstractTracTracLiveTest.getTracTracUsername();
        String tracTracPassword = AbstractTracTracLiveTest.getTracTracPassword();
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2011, 05, 23, 13, 14, 31);
        MillisecondsTimePoint startOfTracking = new MillisecondsTimePoint(cal.getTimeInMillis());
        cal.set(2011, 05, 23, 15, 14, 31);
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
        trackingParams = com.sap.sailing.domain.tractracadapter.DomainFactory.INSTANCE
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
        racesHandle = master.addRace(/* regattaToAddTo */ null, trackingParams, /* timeoutInMilliseconds */ 60000,
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
        final String leaderboardName = "Test Leaderboard";
        Leaderboard masterLeaderboard = master.apply(new CreateFlexibleLeaderboard(leaderboardName, null, new int[0], new LowPoint(), null));
        final String columnName = "R1";
        master.apply(new AddColumnToLeaderboard(columnName, leaderboardName, /* medalRace */ false));
        final Fleet defaultFleet = masterLeaderboard.getFleet(null);
        master.apply(new ConnectTrackedRaceToLeaderboardColumn(leaderboardName, columnName, defaultFleet.getName(),
                new RegattaNameAndRaceName("Academy Tracking 2011 (STG)", "weym470may122011")));
        startTracking();
        Thread.sleep(3000);
        TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        assertNotNull(replicaTrackedRace);
        assertNotSame(masterTrackedRace, replicaTrackedRace);
        assertNotSame(masterTrackedRace.getRace(), replicaTrackedRace.getRace());
        assertEquals(Util.size(masterTrackedRace.getRace().getCompetitors()), Util.size(replicaTrackedRace.getRace().getCompetitors()));
        Leaderboard replicaLeaderboard = replica.getLeaderboardByName(leaderboardName);
        RaceColumn column = replicaLeaderboard.getRaceColumnByName(columnName);
        assertNotNull(column);
        assertSame(replicaTrackedRace, column.getTrackedRace(replicaLeaderboard.getFleet(null)));
    }

    @Test
    public void testReassignmentToLeaderboardReplication() throws Exception {
        logger.info("Running testReassignmentToLeaderboardReplication()");
        final String leaderboardName = "Test Leaderboard";
        Leaderboard masterLeaderboard = master.apply(new CreateFlexibleLeaderboard(leaderboardName, null, new int[0], new LowPoint(), null));
        final String columnName = "R1";
        RaceColumn masterColumn = master.apply(new AddColumnToLeaderboard(columnName, leaderboardName, /* medalRace */ false));
        final Fleet defaultFleet = masterLeaderboard.getFleet(null);
        // set the race identifier in the column; the tracked race
        // doesn't exist yet, but the race identifier is recorded already
        // anyway. When the tracked race then is loaded it is expected
        // to automatically be linked to the leaderboard column.
        master.apply(new ConnectTrackedRaceToLeaderboardColumn(leaderboardName, columnName, defaultFleet.getName(),
                new RegattaNameAndRaceName("Academy Tracking 2011 (STG)", "weym470may122011")));
        startTracking();
        Thread.sleep(1000);
        assertNotNull(masterColumn.getTrackedRace(defaultFleet)); // ensure the re-assignment worked on the master
        TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        assertNotNull(replicaTrackedRace);
        assertNotSame(masterTrackedRace, replicaTrackedRace);
        assertNotSame(masterTrackedRace.getRace(), replicaTrackedRace.getRace());
        assertEquals(Util.size(masterTrackedRace.getRace().getCompetitors()), Util.size(replicaTrackedRace.getRace().getCompetitors()));
        Leaderboard replicaLeaderboard = replica.getLeaderboardByName(leaderboardName);
        assertNotNull(replicaLeaderboard);
        RaceColumn column = replicaLeaderboard.getRaceColumnByName(columnName);
        assertNotNull(column);
        assertSame(replicaTrackedRace, column.getTrackedRace(defaultFleet));
        logger.info("Done running testReassignmentToLeaderboardReplication()");
    }
    
    @Test
    public void testRaceTimeReplication() throws InterruptedException, Exception {
        logger.info("Running testRaceTimeReplication()");
        startTracking();
        // now wait at least until the start of tracking time has been received on the master copy
        boolean receivedStartAndEndOfTracking = master.getTrackedRace(raceIdentifier).getStartOfTracking() != null &&
                master.getTrackedRace(raceIdentifier).getEndOfTracking() != null;
        while (!receivedStartAndEndOfTracking) { // relying on the Timeout rule for this test
            Thread.sleep(10);
            receivedStartAndEndOfTracking = master.getTrackedRace(raceIdentifier).getStartOfTracking() != null &&
                    master.getTrackedRace(raceIdentifier).getEndOfTracking() != null;
        }
        Thread.sleep(1000);
        logger.info("verifying replica's state");
        TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        assertEquals(masterTrackedRace.getStartOfTracking(), replicaTrackedRace.getStartOfTracking());
        assertEquals(masterTrackedRace.getEndOfTracking(), replicaTrackedRace.getEndOfTracking());
        TimePoint now = MillisecondsTimePoint.now();
        assertFalse(now.equals(replicaTrackedRace.getStartOfRace()));
        Thread.sleep(3000);
        ((DynamicTrackedRace) masterTrackedRace).setStartTimeReceived(now);
        Thread.sleep(3000);
        assertEquals(now, replicaTrackedRace.getStartOfRace());
        logger.info("Done running testRaceTimeReplication()");
    }

    @Test
    public void testWindSourcesToExcludeReplication() throws InterruptedException, Exception {
        try {
            logger.info("Running testWindSourcesToExcludeReplication()");
            startTracking();
            final FlexibleLeaderboard lb = master.apply(new CreateFlexibleLeaderboard("LB", "LB Display Name", new int[0], new LowPoint(), Collections.emptySet()));
            final FlexibleRaceColumn r1 = (FlexibleRaceColumn) master.apply(new AddColumnToLeaderboard("R1", lb.getName(), /* medal race */ false));
            master.apply(new ConnectTrackedRaceToLeaderboardColumn(lb.getName(), r1.getName(), "Default", raceIdentifier));
            Thread.sleep(1000);
            final TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
            assertTrue(Util.isEmpty(masterTrackedRace.getWindSourcesToExclude()));
            assertTrue(Util.isEmpty(replicaTrackedRace.getWindSourcesToExclude()));
            if (Wait.wait(()->masterTrackedRace.getAttachedRaceLogs().iterator().hasNext(),
                    Optional.of(Duration.ONE_MINUTE), Duration.ONE_SECOND,
                    Level.INFO, "Waiting for race log(s) attached")) {
                final RaceLog defaultRaceLog = masterTrackedRace.getAttachedRaceLogs().iterator().next();
                defaultRaceLog.add(new RaceLogExcludeWindSourcesEventImpl(TimePoint.now(), new LogEventAuthorImpl("Me", 0), 0,
                        Collections.singleton(new WindSourceImpl(WindSourceType.WEB))));
                assertEquals(1, Util.size(masterTrackedRace.getWindSourcesToExclude()));
                assertEquals(WindSourceType.WEB, masterTrackedRace.getWindSourcesToExclude().iterator().next().getType());
                Thread.sleep(1000);
                assertEquals(1, Util.size(replicaTrackedRace.getWindSourcesToExclude()));
                assertEquals(WindSourceType.WEB, replicaTrackedRace.getWindSourcesToExclude().iterator().next().getType());
            } else {
                fail("Didn't find race log for "+raceIdentifier);
            }
        } finally {
            logger.info("Done running testWindSourcesToExcludeReplication()");
        }
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
