package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.leaderboard.DelayedLeaderboardCorrections;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.media.MediaDBFactory;
import com.sap.sailing.domain.racelog.tracking.EmptySensorFixStore;
import com.sap.sailing.domain.test.AbstractLeaderboardTest;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRank;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.ConnectTrackedRaceToLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardMaxPointsReason;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.replication.testsupport.AbstractServerReplicationTestSetUp.ReplicationServiceTestImpl;

public class DelayedLeaderboardCorrectionsReplicationTest extends AbstractServerReplicationTest {
    private static final Logger logger = Logger.getLogger(DelayedLeaderboardCorrectionsReplicationTest.class.getName());
    
    private static final String Q2 = "Q2";
    private ReplicationService replicaReplicator;
    private ReplicationMasterDescriptor masterDescriptor;
    
    @BeforeEach
    public void setUp() throws FileNotFoundException, UnknownHostException {
        final MongoDBService mongoDBService = MongoDBService.INSTANCE;
        mongoDBService.getDB().drop();
    }
    
    /**
     * When a leaderboard is loaded from the persistent store, the corrections are keyed by competitor names.
     * These corrections cannot truly be applied to the leaderboard unless a {@link Competitor} object with the
     * name loaded from the store is associated through a {@link TrackedRace} with a {@link RaceColumn} of that
     * leaderboard. Until that happens, there is a {@link DelayedLeaderboardCorrections} object that is attached
     * as a {@link RaceColumnListener} to the leaderboard. It needs to be replicated in the initial load, and
     * tracked race associations to {@link RaceColumn}s need to trigger the resolution of the delayed corrections
     * also in the replica. This is what this test verifies.
     */
    @Test
    public void testDelayedLeaderboardCorrectionsReplication() throws Exception {
        // prepare a leaderboard with corrections on master by creating through RacingEventService which stores it; then
        // create a new RacingEventServiceImpl which is expected to load the leaderboard, but only with DelayedLeaderboardCorrections.
        // then start initial load, wait until finished; then link a tracked race to leaderboard's RaceColumn on master and
        // verify that on the replica it is associated to the RaceColumn as well, and the DelayedLeaderboardCorrections are
        // resolved properly on the replica.
        final DomainFactory domainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        BoatClass boatClass = domainFactory.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);
        CompetitorWithBoat hassoWithBoat = AbstractLeaderboardTest.createCompetitorWithBoat("Dr. Hasso Plattner"); // don't create competitor using CompetitorStore
        final DynamicTrackedRace q2YellowTrackedRace = new MockedTrackedRaceWithFixedRank(hassoWithBoat, /* rank */ 1, /* started */ false, boatClass) {
            private static final long serialVersionUID = 1234L;
            @Override
            public RegattaAndRaceIdentifier getRaceIdentifier() {
                return new RegattaNameAndRaceName("Kieler Woche (5o5)", "Yellow Race 2");
            }
        };
        master = createRacingEventServiceWithOneMockedTrackedRace(q2YellowTrackedRace, domainFactory);
        final String leaderboardName = "My new leaderboard";
        final int[] discardThresholds = new int[] { 19, 44 };
        CreateFlexibleLeaderboard createTestLeaderboard = new CreateFlexibleLeaderboard(leaderboardName, null, discardThresholds, new LowPoint(), null);
        assertNull(master.getLeaderboardByName(leaderboardName));
        master.apply(createTestLeaderboard);
        final Leaderboard masterLeaderboard = master.getLeaderboardByName(leaderboardName);
        assertNotNull(masterLeaderboard);
        master.apply(new AddColumnToLeaderboard(Q2, leaderboardName, /* medalRace */ false)); // uses leaderboard's default fleet 
        master.apply(new ConnectTrackedRaceToLeaderboardColumn(masterLeaderboard.getName(), Q2, /* default fleet */
                masterLeaderboard.getFleet(null).getName(), q2YellowTrackedRace.getRaceIdentifier()));
        master.apply(new UpdateLeaderboardMaxPointsReason(masterLeaderboard.getName(), Q2, hassoWithBoat.getId().toString(),
                MaxPointsReason.DNF, MillisecondsTimePoint.now()));
        assertEquals(MaxPointsReason.DNF, masterLeaderboard.getMaxPointsReason(hassoWithBoat,
                masterLeaderboard.getRaceColumnByName(Q2), MillisecondsTimePoint.now()));

        // re-load new master from persistence
        master = createRacingEventServiceWithOneMockedTrackedRace(q2YellowTrackedRace, domainFactory);
        DomainFactoryImpl replicaDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        replica = createRacingEventServiceWithOneMockedTrackedRace(q2YellowTrackedRace, replicaDomainFactory);
        final Leaderboard masterLeaderboardReloaded = master.getLeaderboardByName(leaderboardName);
        assertNotNull(masterLeaderboardReloaded);
        // expecting the correction to not be in the leaderboard because the competitor was not found in the
        // domain factory's competitor store while loading and therefore the score correction could not immediately
        // be applied to the leaderboard's real score correction
        assertEquals(MaxPointsReason.NONE, masterLeaderboardReloaded.getMaxPointsReason(hassoWithBoat,
                masterLeaderboardReloaded.getRaceColumnByName(Q2), MillisecondsTimePoint.now()));

        // replicate the re-loaded environment
        Pair<ReplicationServiceTestImpl<RacingEventService>, ReplicationMasterDescriptor> descriptors = basicSetUp(
                /* dropDB */false, master, replica);
        replicaReplicator = descriptors.getA();
        masterDescriptor = descriptors.getB();
        // starting to replicate will clear the competitor store used by the replica in this test;
        // the competitor will have to be looked up again after it was received by the replica.
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        Thread.sleep(1000);

        Leaderboard replicaLeaderboard = replica.getLeaderboardByName(leaderboardName);
        assertNotNull(replicaLeaderboard);
        assertNotNull(replicaLeaderboard.getRaceColumnByName(Q2));
        // so far, the replica should also only have the delayed corrections since during deserializing them the
        // replicatedHasso competitor was not yet known to the replica's competitor store:
        assertEquals(MaxPointsReason.NONE, replicaLeaderboard.getMaxPointsReason(hassoWithBoat,
                replicaLeaderboard.getRaceColumnByName(Q2), MillisecondsTimePoint.now()));
        
        logger.info("hasso object ID hash: "+System.identityHashCode(hassoWithBoat));
        Leaderboard newMasterLeaderboard = master.getLeaderboardByName(masterLeaderboard.getName());
        SettableScoreCorrection newMasterScoreCorrections = newMasterLeaderboard.getScoreCorrection();
        RaceColumn newMasterQ2 = newMasterLeaderboard.getRaceColumnByName(Q2);
        newMasterScoreCorrections.getCompetitorsThatHaveCorrectionsIn(newMasterQ2);
        StringBuilder unexpectedCompetitors = new StringBuilder();
        unexpectedCompetitors.append("Unexpected competitors having score corrections (expected no competitor to have a real score correction at this point: ");
        for (Competitor unexpectedCompetitor : newMasterScoreCorrections.getCompetitorsThatHaveCorrectionsIn(newMasterQ2)) {
            unexpectedCompetitors.append(unexpectedCompetitor);
            unexpectedCompetitors.append(" with object ID ");
            unexpectedCompetitors.append(System.identityHashCode(unexpectedCompetitor));
            unexpectedCompetitors.append(", ");
        }
        assertTrue(Util.isEmpty(newMasterScoreCorrections.getCompetitorsThatHaveCorrectionsIn(newMasterQ2)), unexpectedCompetitors.toString()); // no score corrections applied yet
        // now connect the tracked race again to the leaderboard column in the re-loaded environment
        master.apply(new ConnectTrackedRaceToLeaderboardColumn(masterLeaderboard.getName(), Q2, /* default fleet */
                masterLeaderboard.getFleet(null).getName(), q2YellowTrackedRace.getRaceIdentifier()));
        logger.info("got score correction for competitor "+System.identityHashCode(newMasterScoreCorrections.getCompetitorsThatHaveCorrectionsIn(newMasterQ2).iterator().next()));
        assertTrue(Util.contains(newMasterScoreCorrections.getCompetitorsThatHaveCorrectionsIn(newMasterQ2), hassoWithBoat)); // now score corrections must have been applied from Delayed... 
        // now the delayed corrections are expected to have been resolved:
        assertEquals(MaxPointsReason.DNF, master.getLeaderboardByName(leaderboardName).getMaxPointsReason(hassoWithBoat,
                master.getLeaderboardByName(leaderboardName).getRaceColumnByName(Q2), MillisecondsTimePoint.now()));
        Thread.sleep(1000); // wait for the tracked race to column connection to be replicated
        assertNotNull(replicaLeaderboard.getRaceColumnByName(Q2).getTrackedRace(hassoWithBoat));
        assertEquals(MaxPointsReason.DNF, replicaLeaderboard.getMaxPointsReason(hassoWithBoat,
                replicaLeaderboard.getRaceColumnByName(Q2), MillisecondsTimePoint.now()));
    }
    
    private RacingEventServiceImpl createRacingEventServiceWithOneMockedTrackedRace(final DynamicTrackedRace q2YellowTrackedRace,
            DomainFactory domainFactory) {
        return new RacingEventServiceImpl(PersistenceFactory.INSTANCE.getDomainObjectFactory(MongoDBService.INSTANCE, domainFactory), PersistenceFactory.INSTANCE
                .getMongoObjectFactory(MongoDBService.INSTANCE), MediaDBFactory.INSTANCE.getMediaDB(MongoDBService.INSTANCE), EmptyWindStore.INSTANCE, EmptySensorFixStore.INSTANCE, /* restoreTrackedRaces */ false) {
            @Override
            public DynamicTrackedRace getExistingTrackedRace(RegattaAndRaceIdentifier raceIdentifier) {
                return q2YellowTrackedRace;
            }
        };
    }
}
