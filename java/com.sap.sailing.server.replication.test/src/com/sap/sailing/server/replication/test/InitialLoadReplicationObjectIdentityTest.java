package com.sap.sailing.server.replication.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogWindFixEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.media.MediaDB;
import com.sap.sailing.domain.persistence.media.MediaDBFactory;
import com.sap.sailing.domain.racelog.tracking.EmptySensorFixStore;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.test.PositionAssert;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AddDefaultRegatta;
import com.sap.sailing.server.operationaltransformation.AddRaceDefinition;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.impl.ReplicationReceiverImpl;
import com.sap.sse.replication.testsupport.AbstractServerReplicationTestSetUp.ReplicationServiceTestImpl;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.testsupport.SecurityServiceMockFactory;

public class InitialLoadReplicationObjectIdentityTest extends AbstractServerReplicationTest {
    private Pair<ReplicationServiceTestImpl<RacingEventService>, ReplicationMasterDescriptor> replicationDescriptorPair;
    
    private static class RacingEventServiceWithSecurityService extends RacingEventServiceImpl {
        private final SecurityService securityService;
        
        private RacingEventServiceWithSecurityService(final DomainObjectFactory domainObjectFactory, MongoObjectFactory mongoObjectFactory,
            MediaDB mediaDB, WindStore windStore, SensorFixStore sensorFixStore, boolean restoreTrackedRaces, SecurityService securityService) {
            super(domainObjectFactory, mongoObjectFactory, mediaDB, windStore, sensorFixStore, restoreTrackedRaces);
            this.securityService = securityService;
        }

        @Override
        public SecurityService getSecurityService() {
            return securityService;
        }
    }
    
    /**
     * Drops the test DB. Sets up master and replica, starts the JMS message broker and registers the replica with the master.
     */
    @SuppressWarnings("unchecked")
    @BeforeEach
    @Override
    public void setUp() throws Exception {
        persistenceSetUp(/* dropDB */ true);
        final SecurityService securityService = SecurityServiceMockFactory.mockSecurityService();
        Mockito.when(securityService.setOwnershipCheckPermissionForObjectCreationAndRevertOnError(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(Callable.class)))
                .thenAnswer(i -> i.getArgument(3, Callable.class).call());
        this.master = new RacingEventServiceWithSecurityService(PersistenceFactory.INSTANCE.getDomainObjectFactory(testSetUp.mongoDBService, DomainFactory.INSTANCE), PersistenceFactory.INSTANCE
                        .getMongoObjectFactory(testSetUp.mongoDBService),
                MediaDBFactory.INSTANCE.getMediaDB(testSetUp.mongoDBService), EmptyWindStore.INSTANCE,
                EmptySensorFixStore.INSTANCE, /* restoreTrackedRaces */ false, securityService);
        this.replica = new RacingEventServiceWithSecurityService(PersistenceFactory.INSTANCE.getDomainObjectFactory(testSetUp.mongoDBService, DomainFactory.INSTANCE), PersistenceFactory.INSTANCE
                        .getMongoObjectFactory(testSetUp.mongoDBService),
                MediaDBFactory.INSTANCE.getMediaDB(testSetUp.mongoDBService), EmptyWindStore.INSTANCE,
                EmptySensorFixStore.INSTANCE, /* restoreTrackedRaces */ false, securityService);
    }
    
    private void performReplicationSetup() throws Exception {
        try {
            replicationDescriptorPair = basicSetUp(/* dropDB */ false, this.master, this.replica);
        } catch (Exception e) {
            e.printStackTrace();
            tearDown();
        }
    }
    
    @Test
    public void testInitialLoad() throws Exception {
        /* Event */
        String eventName = "Monster Event";
        String venue = "Default Venue";
        List<String> courseAreaNames = new ArrayList<String>();
        courseAreaNames.add("Default");
        final UUID eventId = UUID.randomUUID();
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());

        Event event = master.addEvent(eventName, /* eventDescription */ null, eventStartDate, eventEndDate, venue, false, eventId);
        assertNotNull(master.getEvent(eventId));
        assertNull(replica.getEvent(eventId));
        
        /* Regatta */
        final String baseEventName = "Kiel Week 2012";
        final String boatClassName = "49er";
        final Iterable<Series> series = Collections.emptyList();
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName),
                boatClassName, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, UUID.randomUUID(), series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                /* course area ID */ (Serializable) null,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        assertNotNull(master.getRegatta(masterRegatta.getRegattaIdentifier()));
        assertTrue(master.getAllRegattas().iterator().hasNext());
        assertNull(replica.getRegatta(masterRegatta.getRegattaIdentifier()));
        
        /* RaceDefinition and DynamicTrackedRace */
        RaceDefinition masterRace = new RaceDefinitionImpl("Test Race", new CourseImpl("Test Course", Collections.<Waypoint>emptyList()),
                masterRegatta.getBoatClass(), Collections.<Competitor,Boat>emptyMap());
        masterRegatta.addRace(masterRace);
        DynamicTrackedRace masterTrackedRace = master.createTrackedRace(new RegattaNameAndRaceName(masterRegatta.getName(), masterRace.getName()),
                master.getWindStore(), /* delayToLiveInMillis */ 3000,
                /* millisecondsOverWhichToAverageWind */ 15000, /* millisecondsOverWhichToAverageSpeed */ 10000, /*ignoreTracTracMarkPassings*/ false, null);
        masterTrackedRace.setStartOfTrackingReceived(MillisecondsTimePoint.now());
        /* Leaderboard */
        final String leaderboardName = "Great Leaderboard";
        final int[] discardThresholds = new int[] { 17, 23 };
        CreateFlexibleLeaderboard createTestLeaderboard = new CreateFlexibleLeaderboard(leaderboardName, null, discardThresholds, new LowPoint(), null);
        assertNull(master.getLeaderboardByName(leaderboardName));
        FlexibleLeaderboard masterLeaderboard = master.apply(createTestLeaderboard);
        assertNotNull(master.getLeaderboardByName(leaderboardName));
        assertNull(replica.getLeaderboardByName(leaderboardName));
        masterLeaderboard.addRace(masterTrackedRace, "R1", /* medalRace */ false);
        RaceColumn masterR1 = masterLeaderboard.getRaceColumnByName("R1");
        final RaceLog masterR1RaceLog = masterR1.getRaceLog(masterR1.getFleets().iterator().next());
        assertNotNull(masterR1RaceLog);
        final WindImpl masterWindFix = new WindImpl(new DegreePosition(12, 13), MillisecondsTimePoint.now(), 
                new KnotSpeedWithBearingImpl(12.7, new DegreeBearingImpl(123)));
        masterR1RaceLog.add(new RaceLogWindFixEventImpl(MillisecondsTimePoint.now(), new LogEventAuthorImpl("Me", 0),
                /* passId */ 1, masterWindFix, /* isMagnetic */ false));
        // assert that the wind fix shows up in the tracked race now because the tracked race has the DynamicTrackedRaceLogListener
        // propagating the wind fix race log event into the wind track for source RACECOMMITTEE
        WindTrack masterRCWindFixes = masterTrackedRace.getOrCreateWindTrack(new WindSourceImpl(WindSourceType.RACECOMMITTEE));
        masterRCWindFixes.lockForRead();
        try {
            PositionAssert.assertWindEquals(masterWindFix, masterRCWindFixes.getRawFixes().iterator().next(), /* pos deg delta */ 0.0000001, /* bearing deg delta */ 0.01, /* knot speed delta */ 0.01);
        } finally {
            masterRCWindFixes.unlockAfterRead();
        }

        /* LeaderboardGroup */
        final String leaderBoardGroupName = "Great Leaderboard Group";
        List<String> leaderboardNames = new ArrayList<String>();
        leaderboardNames.add(leaderboardName);
        int[] overallLeaderboardDiscardThresholds = new int[] {};
        ScoringSchemeType overallLeaderboardScoringSchemeType = ScoringSchemeType.HIGH_POINT;
        LeaderboardGroup leaderboardGroup = master.addLeaderboardGroup(UUID.randomUUID(), leaderBoardGroupName, "Some descriptive Description",
                "displayName", false, leaderboardNames, overallLeaderboardDiscardThresholds, overallLeaderboardScoringSchemeType);
        assertNotNull(master.getLeaderboardGroupByName(leaderBoardGroupName));
        assertNull(replica.getLeaderboardGroupByName(leaderBoardGroupName));
        
        event.addLeaderboardGroup(leaderboardGroup);
        
        /* Media Library */
        Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        assignedRaces.add(new RegattaNameAndRaceName("49er", "R1"));
        MediaTrack mediaTrack1 = new MediaTrack("title-1", "url", MillisecondsTimePoint.now(), MillisecondsDurationImpl.ONE_HOUR, MimeType.mp4, assignedRaces);
        master.mediaTrackAdded(mediaTrack1);
        MediaTrack mediaTrack2 = new MediaTrack("title-2", "url", MillisecondsTimePoint.now(), MillisecondsDurationImpl.ONE_HOUR, MimeType.ogv, assignedRaces);
        master.mediaTrackAdded(mediaTrack2);
        MediaTrack mediaTrack3 = new MediaTrack("title-3", "url", MillisecondsTimePoint.now(), MillisecondsDurationImpl.ONE_HOUR, MimeType.mp4, assignedRaces);
        master.mediaTrackAdded(mediaTrack3);
        
        /* fire up replication */
        performReplicationSetup();
        ReplicationMasterDescriptor the_master = replicationDescriptorPair.getB(); /* master descriptor */
        ReplicationReceiverImpl replicator = replicationDescriptorPair.getA()
                .startToReplicateFromButDontYetFetchInitialLoad(the_master, /* startReplicatorSuspended */true);
        replicationDescriptorPair.getA().initialLoad();
        replicator.setSuspended(false);
        synchronized (replicator) {
            while (!replicator.isQueueEmptyOrStopped()) {
                replicator.wait();
            }
        }

        Event replicaEvent = replica.getEvent(eventId);
        assertNotNull(replicaEvent);
        Regatta replicaRegatta = replica.getRegatta(masterRegatta.getRegattaIdentifier());
        assertNotNull(replicaRegatta);
        LeaderboardGroup replicaLeaderboardGroup = replica.getLeaderboardGroupByName(leaderBoardGroupName);
        assertNotNull(replicaLeaderboardGroup);
        assertNotNull(replica.getLeaderboardByName(leaderboardName));
        assertTrue(replica.getAllRegattas().iterator().hasNext());
        assertSame(replicaLeaderboardGroup, replicaEvent.getLeaderboardGroups().iterator().next());
        assertThat(Util.size(replica.getAllMediaTracks()), is(3));
        Leaderboard replicaLeaderboard = replica.getLeaderboardByName(leaderboardName);
        RaceColumn replicaR1 = replicaLeaderboard.getRaceColumnByName("R1");
        TrackedRace replicaTrackedRace = replicaR1.getTrackedRace(replicaR1.getFleets().iterator().next());
        assertNotNull(replicaTrackedRace);
        RaceLog replicaR1RaceLog = replicaR1.getRaceLog(replicaR1.getFleets().iterator().next());
        replicaR1RaceLog.lockForRead();
        try {
            RaceLogEvent replicaRaceLogEvent = replicaR1RaceLog.getRawFixes().iterator().next();
            assertTrue(replicaRaceLogEvent instanceof RaceLogWindFixEvent);
        } finally {
            replicaR1RaceLog.unlockAfterRead();
        }
        // now (see bug 2506) add a wind fix event to the race log and check that it arrived at the wind track
        final WindImpl replicaWindFix = new WindImpl(new DegreePosition(14, 15), MillisecondsTimePoint.now(), 
                new KnotSpeedWithBearingImpl(22.9, new DegreeBearingImpl(155)));
        replicaR1RaceLog.add(new RaceLogWindFixEventImpl(MillisecondsTimePoint.now(), new LogEventAuthorImpl("Me", 0),
                /* passId */ 1, replicaWindFix, /* isMagnetic */ false));
        // assert that the wind fix shows up in the tracked race now because the tracked race has the DynamicTrackedRaceLogListener
        // propagating the wind fix race log event into the wind track for source RACECOMMITTEE
        WindTrack replicaRCWindFixes = replicaTrackedRace.getOrCreateWindTrack(new WindSourceImpl(WindSourceType.RACECOMMITTEE));
        replicaRCWindFixes.lockForRead();
        try {
            Iterator<Wind> replicaRCWindFixesIter = replicaRCWindFixes.getRawFixes().iterator();
            replicaRCWindFixesIter.next();
            PositionAssert.assertWindEquals(replicaWindFix, replicaRCWindFixesIter.next(), /* pos deg delta */ 0.0000001, /* bearing deg delta */ 0.01, /* knot speed delta */ 0.02);
        } finally {
            replicaRCWindFixes.unlockAfterRead();
        }

    }

    @Test
    public void testSameCompetitorInTwoRacesReplication() throws Exception {
        performReplicationSetup();
        final String boatClassName = "49er";
        final DomainFactory masterDomainFactory = master.getBaseDomainFactory();
        BoatClass boatClass = masterDomainFactory.getOrCreateBoatClass(boatClassName);
        final String baseEventName = "Test Event";
        AddDefaultRegatta addEventOperation = new AddDefaultRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName, 
                /*startDate*/ null, /*endDate*/ null, UUID.randomUUID());
        Regatta regatta = master.apply(addEventOperation);
        final String raceName1 = "Test Race 1";
        final String raceName2 = "Test Race 2";
        CompetitorWithBoat competitor = TrackBasedTest.createCompetitorWithBoat("The Same Competitor");
        final CourseImpl masterCourse = new CourseImpl("Test Course", new ArrayList<Waypoint>());
        final Map<Competitor,Boat> competitors = new HashMap<>();
        competitors.put(competitor, competitor.getBoat());
        RaceDefinition race1 = new RaceDefinitionImpl(raceName1, masterCourse, boatClass, competitors);
        AddRaceDefinition addRaceOperation1 = new AddRaceDefinition(new RegattaName(regatta.getName()), race1);
        master.apply(addRaceOperation1);
        replicationDescriptorPair.getA().startToReplicateFrom(replicationDescriptorPair.getB());
        RaceDefinition race2 = new RaceDefinitionImpl(raceName2, masterCourse, boatClass, competitors);
        AddRaceDefinition addRaceOperation2 = new AddRaceDefinition(new RegattaName(regatta.getName()), race2);
        master.apply(addRaceOperation2);
        Thread.sleep(3000); // wait 1s for messaging to deliver the message and the message to be applied
        Regatta replicaEvent = replica.getRegatta(new RegattaName(regatta.getName()));
        RaceDefinition replicaRace1 = replicaEvent.getRaceByName(raceName1);
        RaceDefinition replicaRace2 = replicaEvent.getRaceByName(raceName2);
        assertSame(replicaRace1.getCompetitors().iterator().next(), replicaRace2.getCompetitors().iterator().next());
    }
}
