package com.sap.sailing.server.replication.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.server.operationaltransformation.AddDefaultRegatta;
import com.sap.sailing.server.operationaltransformation.AddRaceDefinition;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;

public class ServerReplicationTest extends AbstractServerReplicationTest {
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
        CreateFlexibleLeaderboard createTestLeaderboard = new CreateFlexibleLeaderboard(leaderboardName, discardThresholds, new LowPoint());
        assertNull(master.getLeaderboardByName(leaderboardName));
        master.apply(createTestLeaderboard);
        final Leaderboard masterLeaderboard = master.getLeaderboardByName(leaderboardName);
        assertNotNull(masterLeaderboard);
        Thread.sleep(1000); // wait 1s for JMS to deliver the message and the message to be applied
        final Leaderboard replicaLeaderboard = replica.getLeaderboardByName(leaderboardName);
        assertNotNull(replicaLeaderboard);
        assertTrue(Arrays.equals(masterLeaderboard.getResultDiscardingRule().getDiscardIndexResultsStartingWithHowManyRaces(),
                replicaLeaderboard.getResultDiscardingRule().getDiscardIndexResultsStartingWithHowManyRaces()));
    }

    @Test
    public void testLeaderboardRemovalReplication() throws InterruptedException {
        final String leaderboardName = LeaderboardNameConstants.DEFAULT_LEADERBOARD_NAME;
        assertNotNull(replica.getLeaderboardByName(leaderboardName));
        assertNotNull(master.getLeaderboardByName(leaderboardName));
        RemoveLeaderboard removeDefaultLeaderboard = new RemoveLeaderboard(leaderboardName);
        master.apply(removeDefaultLeaderboard);
        final Leaderboard masterLeaderboard = master.getLeaderboardByName(leaderboardName);
        assertNull(masterLeaderboard);
        Thread.sleep(1000); // wait 1s for JMS to deliver the message and the message to be applied
        final Leaderboard replicaLeaderboard = replica.getLeaderboardByName(leaderboardName);
        assertNull(replicaLeaderboard);
    }

    @Test
    public void testWaypointRemovalReplication() throws InterruptedException {
        final String boatClassName = "49er";
        // FIXME use master DomainFactory; see bug 592
        final DomainFactory masterDomainFactory = DomainFactory.INSTANCE;
        BoatClass boatClass = masterDomainFactory.getOrCreateBoatClass(boatClassName);
        final String baseEventName = "Test Event";
        AddDefaultRegatta addEventOperation = new AddDefaultRegatta(baseEventName, boatClassName);
        Regatta regatta = master.apply(addEventOperation);
        final String raceName = "Test Race";
        final CourseImpl masterCourse = new CourseImpl("Test Course", new ArrayList<Waypoint>());
        RaceDefinition race = new RaceDefinitionImpl(raceName, masterCourse, boatClass,
                new ArrayList<Competitor>());
        AddRaceDefinition addRaceOperation = new AddRaceDefinition(new RegattaName(regatta.getName()), race);
        master.apply(addRaceOperation);
        masterCourse.addWaypoint(0, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateBuoy("Buoy1")));
        masterCourse.addWaypoint(1, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateBuoy("Buoy2")));
        masterCourse.addWaypoint(2, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateBuoy("Buoy3")));
        masterCourse.removeWaypoint(1);
        Thread.sleep(1000); // wait 1s for JMS to deliver the message and the message to be applied
        Regatta replicaEvent = replica.getRegatta(new RegattaName(regatta.getName()));
        assertNotNull(replicaEvent);
        RaceDefinition replicaRace = replicaEvent.getRaceByName(raceName);
        assertNotNull(replicaRace);
        Course replicaCourse = replicaRace.getCourse();
        assertEquals(2, Util.size(replicaCourse.getWaypoints()));
        assertEquals("Buoy1", replicaCourse.getFirstWaypoint().getBuoys().iterator().next().getName());
        assertEquals("Buoy3", replicaCourse.getLastWaypoint().getBuoys().iterator().next().getName());
    }
}
