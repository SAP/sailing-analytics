package com.sap.sailing.server.replication.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.server.operationaltransformation.AddLeaderboardGroupToEvent;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboardGroupFromEvent;

public class EventReplicationTest extends AbstractServerReplicationTest {

    @Test
    public void testEventReplication() throws InterruptedException {
        final String eventName = "ESS Masquat";
        final String venueName = "Masquat, Oman";
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());
        final boolean isPublic = false;
        List<String> regattas = new ArrayList<String>();
        regattas.add("Day1");
        regattas.add("Day2");
        Event masterEvent = master.addEvent(eventName, eventStartDate, eventEndDate, venueName, isPublic, UUID.randomUUID());

        Thread.sleep(1000);
        Event replicatedEvent = replica.getEvent(masterEvent.getId());
        assertNotNull(replicatedEvent);
        assertEquals(replicatedEvent.getName(), eventName);
        assertEquals(replicatedEvent.getStartDate(), eventStartDate);
        assertEquals(replicatedEvent.getEndDate(), eventEndDate);
        assertEquals(replicatedEvent.getVenue().getName(), venueName);
    }
    
    @Test
    public void testAddAndRemoveLeaderboardGroupToEventReplication() throws InterruptedException {
        final String eventName = "ESS Masquat";
        final String venueName = "Masquat, Oman";
        final boolean isPublic = false;
        List<String> regattas = new ArrayList<String>();
        regattas.add("Day1");
        regattas.add("Day2");
        final Event masterEvent = master.addEvent(eventName, null, null, venueName, isPublic, UUID.randomUUID());
        final String leaderboardGroupName = "LGName";
        master.apply(new CreateLeaderboardGroup(leaderboardGroupName, "LGDescription", /* displayGroupsInReverseOrder */false, /* leaderboardNames */
                Collections.<String> emptyList(), /* overallLeaderboardDiscardThresholds */null, /* overallLeaderboardScoringSchemeType */
                null));
        master.apply(new AddLeaderboardGroupToEvent(masterEvent.getId(), leaderboardGroupName));
        Thread.sleep(1000);
        Event replicatedEvent = replica.getEvent(masterEvent.getId());
        assertEquals(1, Util.size(replicatedEvent.getLeaderboardGroups()));
        assertEquals(leaderboardGroupName, replicatedEvent.getLeaderboardGroups().iterator().next().getName());
        assertTrue(master.apply(new RemoveLeaderboardGroupFromEvent(masterEvent.getId(), leaderboardGroupName)));
        Thread.sleep(1000);
        assertTrue(Util.isEmpty(replicatedEvent.getLeaderboardGroups()));
    }
    
    @Test
    public void testEventReplicationWithNullStartAndEndDate() throws InterruptedException {
        final String eventName = "ESS Masquat";
        final String venueName = "Masquat, Oman";
        final boolean isPublic = false;
        List<String> regattas = new ArrayList<String>();
        regattas.add("Day1");
        regattas.add("Day2");
        Event masterEvent = master.addEvent(eventName, null, null, venueName, isPublic, UUID.randomUUID());

        Thread.sleep(1000);
        Event replicatedEvent = replica.getEvent(masterEvent.getId());
        assertNotNull(replicatedEvent);
        assertEquals(replicatedEvent.getName(), eventName);
        assertNull(replicatedEvent.getStartDate());
        assertNull(replicatedEvent.getEndDate());
        assertEquals(replicatedEvent.getVenue().getName(), venueName);
    }

    @Test
    public void testCourseAreaReplication() throws InterruptedException {
        final String eventName = "ESS Singapur";
        final String venueName = "Singapur, Singapur";
        final boolean isPublic = false;
        final String courseArea = "Alpha";
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());
        Event masterEvent = master.addEvent(eventName, eventStartDate, eventEndDate, venueName, isPublic, UUID.randomUUID());
        CourseArea masterCourseArea = master.addCourseArea(masterEvent.getId(), courseArea, UUID.randomUUID());

        Thread.sleep(1000);
        Event replicatedEvent = replica.getEvent(masterEvent.getId());
        assertNotNull(replicatedEvent);
        assertEquals(replicatedEvent.getName(), eventName);
        assertEquals(Util.size(replicatedEvent.getVenue().getCourseAreas()), 1);

        CourseArea replicatedCourseArea = Util.get(replicatedEvent.getVenue().getCourseAreas(), 0);
        assertEquals(replicatedCourseArea.getId(), masterCourseArea.getId());
        assertEquals(replicatedCourseArea.getName(), courseArea);
    }
}
