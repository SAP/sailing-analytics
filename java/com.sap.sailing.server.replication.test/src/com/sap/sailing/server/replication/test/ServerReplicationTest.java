package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;
import com.sap.sailing.server.operationaltransformation.AddDefaultRegatta;
import com.sap.sailing.server.operationaltransformation.AddRaceDefinition;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;
import com.sap.sse.common.Util;

public class ServerReplicationTest extends AbstractServerReplicationTest {
    private static final String LEADERBOARDNAME = "TESTLEADERBOARD";

    @Test
    public void testBasicInitialLoad() throws Exception {
        assertNotSame(master, replica);
        assertEquals(Util.size(master.getAllRegattas()), Util.size(replica.getAllRegattas()));
        assertEquals(master.getLeaderboardGroups().size(), replica.getLeaderboardGroups().size());
        assertEquals(master.getLeaderboards().size(), replica.getLeaderboards().size());
        assertEquals(master.getLeaderboards().keySet(), replica.getLeaderboards().keySet());
    }
    
    @Test
    public void testLeaderboardCreationReplication() throws InterruptedException {
        Thread.sleep(1000); // wait 1s for JMS to deliver any recovered messages; there should be none
        final String leaderboardName = "My new leaderboard";
        assertNull(replica.getLeaderboardByName(leaderboardName));
        final int[] discardThresholds = new int[] { 17, 23 };
        CreateFlexibleLeaderboard createTestLeaderboard = new CreateFlexibleLeaderboard(leaderboardName, null, discardThresholds, new LowPoint(), null);
        assertNull(master.getLeaderboardByName(leaderboardName));
        master.apply(createTestLeaderboard);
        final Leaderboard masterLeaderboard = master.getLeaderboardByName(leaderboardName);
        assertNotNull(masterLeaderboard);
        Thread.sleep(1000); // wait 1s for JMS to deliver the message and the message to be applied
        final Leaderboard replicaLeaderboard = replica.getLeaderboardByName(leaderboardName);
        assertNotNull(replicaLeaderboard);
        assertTrue(Arrays.equals(((ThresholdBasedResultDiscardingRule) masterLeaderboard.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces(),
                ((ThresholdBasedResultDiscardingRule) replicaLeaderboard.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces()));
    }

    @Test
    public void testLeaderboardRemovalReplication() throws InterruptedException {
        RacingEventServiceOperation<FlexibleLeaderboard> addLeaderboardOp = new CreateFlexibleLeaderboard(LEADERBOARDNAME,null, new int[] { 5 },
                new LowPoint(), null);
        master.apply(addLeaderboardOp);
        Thread.sleep(1000); // wait 1s for JMS to deliver the message and the message to be applied
        assertNotNull(replica.getLeaderboardByName(LEADERBOARDNAME));
        assertNotNull(master.getLeaderboardByName(LEADERBOARDNAME));
        RemoveLeaderboard removeDefaultLeaderboard = new RemoveLeaderboard(LEADERBOARDNAME);
        master.apply(removeDefaultLeaderboard);
        final Leaderboard masterLeaderboard = master.getLeaderboardByName(LEADERBOARDNAME);
        assertNull(masterLeaderboard);
        Thread.sleep(1000); // wait 1s for JMS to deliver the message and the message to be applied
        final Leaderboard replicaLeaderboard = replica.getLeaderboardByName(LEADERBOARDNAME);
        assertNull(replicaLeaderboard);
    }

    @Test
    public void testWaypointRemovalReplication() throws InterruptedException {
        final String boatClassName = "49er";
        final DomainFactory masterDomainFactory = testSetUp.getMaster().getBaseDomainFactory();
        BoatClass boatClass = masterDomainFactory.getOrCreateBoatClass(boatClassName);
        final String baseEventName = "Test Event";
        AddDefaultRegatta addRegattaOperation = new AddDefaultRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName,
                /*startDate*/ null, /*endDate*/ null, UUID.randomUUID());
        Regatta regatta = master.apply(addRegattaOperation);
        final String raceName = "Test Race";
        final CourseImpl masterCourse = new CourseImpl("Test Course", new ArrayList<Waypoint>());
        RaceDefinition race = new RaceDefinitionImpl(raceName, masterCourse, boatClass, Collections.<Competitor,Boat>emptyMap());
        AddRaceDefinition addRaceOperation = new AddRaceDefinition(new RegattaName(regatta.getName()), race);
        master.apply(addRaceOperation);
        masterCourse.addWaypoint(0, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateMark("Mark1"), /*passingInstruction*/ null));
        masterCourse.addWaypoint(1, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateMark("Mark2"), /*passingInstruction*/ null));
        masterCourse.addWaypoint(2, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateMark("Mark3"), /*passingInstruction*/ null));
        masterCourse.removeWaypoint(1);
        Thread.sleep(3000); // wait 1s for JMS to deliver the message and the message to be applied
        Regatta replicaEvent = replica.getRegatta(new RegattaName(regatta.getName()));
        assertNotNull(replicaEvent);
        RaceDefinition replicaRace = replicaEvent.getRaceByName(raceName);
        assertNotNull(replicaRace);
        Course replicaCourse = replicaRace.getCourse();
        assertEquals(2, Util.size(replicaCourse.getWaypoints()));
        assertEquals("Mark1", replicaCourse.getFirstWaypoint().getMarks().iterator().next().getName());
        assertEquals("Mark3", replicaCourse.getLastWaypoint().getMarks().iterator().next().getName());
    }
}
