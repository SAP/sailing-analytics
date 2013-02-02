package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;

import difflib.PatchFailedException;

public class CourseTest {
    @Test
    public void testEmptyCourse() {
        Iterable<Waypoint> waypoints = Collections.emptyList();
        Course course = new CourseImpl("Test Course", waypoints);
        assertEquals(0, Util.size(course.getWaypoints()));
        assertEquals(0, Util.size(course.getLegs()));
        assertNull(course.getFirstWaypoint());
        assertNull(course.getLastWaypoint());
        assertWaypointIndexes(course);
    }

    @Test
    public void testCourseWithOneWaypoint() {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        waypoints.add(new WaypointImpl(new MarkImpl("Test Mark")));
        Course course = new CourseImpl("Test Course", waypoints);
        assertEquals(1, Util.size(course.getWaypoints()));
        assertEquals(0, Util.size(course.getLegs()));
        assertWaypointIndexes(course);
    }
    
    @Test
    public void testAddWaypointToCourseWithOneWaypoint() {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        waypoints.add(new WaypointImpl(new MarkImpl("Test Mark")));
        Course course = new CourseImpl("Test Course", waypoints);
        assertEquals(1, Util.size(course.getWaypoints()));
        assertEquals(0, Util.size(course.getLegs()));
        assertWaypointIndexes(course);
        course.addWaypoint(1, new WaypointImpl(new MarkImpl("Second Mark")));
        assertWaypointIndexes(course);
        assertEquals(2, Util.size(course.getWaypoints()));
        assertEquals(1, Util.size(course.getLegs()));
    }

    @Test
    public void testAddWaypointToEmptyCourse() {
        Iterable<Waypoint> waypoints = Collections.emptyList();
        Course course = new CourseImpl("Test Course", waypoints);
        assertEquals(0, Util.size(course.getWaypoints()));
        assertEquals(0, Util.size(course.getLegs()));
        assertWaypointIndexes(course);
        course.addWaypoint(0, new WaypointImpl(new MarkImpl("First Mark")));
        assertWaypointIndexes(course);
        assertEquals(1, Util.size(course.getWaypoints()));
        assertEquals(0, Util.size(course.getLegs()));
    }

    @Test
    public void testRemoveWaypointFromCourseWithOneWaypoint() {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        waypoints.add(new WaypointImpl(new MarkImpl("Test Mark")));
        waypoints.add(new WaypointImpl(new MarkImpl("Second Mark")));
        Course course = new CourseImpl("Test Course", waypoints);
        assertEquals(2, Util.size(course.getWaypoints()));
        assertEquals(1, Util.size(course.getLegs()));
        assertWaypointIndexes(course);
        course.removeWaypoint(1);
        assertWaypointIndexes(course);
        assertEquals(1, Util.size(course.getWaypoints()));
        assertEquals(0, Util.size(course.getLegs()));
    }
    
    @Test
    public void testRemoveWaypointToEmptyCourse() {
        Iterable<Waypoint> waypoints = Collections.emptyList();
        Course course = new CourseImpl("Test Course", waypoints);
        course.addWaypoint(0, new WaypointImpl(new MarkImpl("First Mark")));
        assertEquals(1, Util.size(course.getWaypoints()));
        assertEquals(0, Util.size(course.getLegs()));
        assertWaypointIndexes(course);
        course.removeWaypoint(0);
        assertWaypointIndexes(course);
        assertEquals(0, Util.size(course.getWaypoints()));
        assertEquals(0, Util.size(course.getLegs()));
    }

    @Test
    public void testInsertWaypointToCourseWithTwoWaypoints() {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        final WaypointImpl wp1 = new WaypointImpl(new MarkImpl("Test Mark 1"));
        waypoints.add(wp1);
        final WaypointImpl wp2 = new WaypointImpl(new MarkImpl("Test Mark 2"));
        waypoints.add(wp2);
        Course course = new CourseImpl("Test Course", waypoints);
        assertEquals(2, Util.size(course.getWaypoints()));
        assertEquals(1, Util.size(course.getLegs()));
        final WaypointImpl wp1_5 = new WaypointImpl(new MarkImpl("Test Mark 1.5"));
        assertWaypointIndexes(course);
        course.addWaypoint(1, wp1_5);
        assertWaypointIndexes(course);
        assertEquals(3, Util.size(course.getWaypoints()));
        assertEquals(2, Util.size(course.getLegs()));
        assertTrue(Util.equals(Arrays.asList(new Waypoint[] { wp1, wp1_5, wp2 }), course.getWaypoints()));
        assertEquals(0, course.getIndexOfWaypoint(wp1));
        assertEquals(1, course.getIndexOfWaypoint(wp1_5));
        assertEquals(2, course.getIndexOfWaypoint(wp2));
    }

    @Test
    public void testInsertWaypointAsFirstInCourseWithFormerlyTwoWaypoints() {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        final WaypointImpl wp1 = new WaypointImpl(new MarkImpl("Test Mark 1"));
        waypoints.add(wp1);
        final WaypointImpl wp2 = new WaypointImpl(new MarkImpl("Test Mark 2"));
        waypoints.add(wp2);
        Course course = new CourseImpl("Test Course", waypoints);
        assertEquals(2, Util.size(course.getWaypoints()));
        assertEquals(1, Util.size(course.getLegs()));
        final WaypointImpl wp0_5 = new WaypointImpl(new MarkImpl("Test Mark .5"));
        assertWaypointIndexes(course);
        course.addWaypoint(0, wp0_5);
        assertWaypointIndexes(course);
        assertEquals(3, Util.size(course.getWaypoints()));
        assertEquals(2, Util.size(course.getLegs()));
        assertEquals(wp0_5, course.getLegs().get(0).getFrom());
        assertEquals(wp1, course.getLegs().get(0).getTo());
        assertEquals(wp1, course.getLegs().get(1).getFrom());
        assertEquals(wp2, course.getLegs().get(1).getTo());
        assertTrue(Util.equals(Arrays.asList(new Waypoint[] { wp0_5, wp1, wp2 }), course.getWaypoints()));
        assertEquals(0, course.getIndexOfWaypoint(wp0_5));
        assertEquals(1, course.getIndexOfWaypoint(wp1));
        assertEquals(2, course.getIndexOfWaypoint(wp2));
    }

    @Test
    public void testMovingWaypointFoward() throws PatchFailedException {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        final WaypointImpl wp1 = new WaypointImpl(new MarkImpl("Test Mark 1"));
        waypoints.add(wp1);
        final WaypointImpl wp2 = new WaypointImpl(new MarkImpl("Test Mark 2"));
        waypoints.add(wp2);
        final WaypointImpl wp3 = new WaypointImpl(new MarkImpl("Test Mark 3"));
        waypoints.add(wp3);
        Course course = new CourseImpl("Test Course", waypoints);
        assertEquals(3, Util.size(course.getWaypoints()));
        assertEquals(2, Util.size(course.getLegs()));
        assertWaypointIndexes(course);
        course.update(Arrays.asList(wp2.getMarks().iterator().next(), wp3.getMarks().iterator().next(), wp1.getMarks().iterator().next()),
                DomainFactory.INSTANCE);
        assertWaypointIndexes(course);
    }

    @Test
    public void testRemoveWaypointFromCourseWithThreeWaypoints() {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        final WaypointImpl wp1 = new WaypointImpl(new MarkImpl("Test Mark 1"));
        waypoints.add(wp1);
        final WaypointImpl wp2 = new WaypointImpl(new MarkImpl("Test Mark 2"));
        waypoints.add(wp2);
        final WaypointImpl wp3 = new WaypointImpl(new MarkImpl("Test Mark 3"));
        waypoints.add(wp3);
        Course course = new CourseImpl("Test Course", waypoints);
        assertEquals(3, Util.size(course.getWaypoints()));
        assertEquals(2, Util.size(course.getLegs()));
        assertWaypointIndexes(course);
        course.removeWaypoint(1);
        assertWaypointIndexes(course);
        assertEquals(2, Util.size(course.getWaypoints()));
        assertEquals(1, Util.size(course.getLegs()));
        assertEquals(wp1, course.getLegs().get(0).getFrom());
        assertEquals(wp3, course.getLegs().get(0).getTo());
        assertTrue(Util.equals(Arrays.asList(new Waypoint[] { wp1, wp3 }), course.getWaypoints()));
        assertEquals(0, course.getIndexOfWaypoint(wp1));
        assertEquals(-1, course.getIndexOfWaypoint(wp2));
        assertEquals(1, course.getIndexOfWaypoint(wp3));
    }

    @Test
    public void testRemoveFirstWaypointFromCourseWithThreeWaypoints() {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        final WaypointImpl wp1 = new WaypointImpl(new MarkImpl("Test Mark 1"));
        waypoints.add(wp1);
        final WaypointImpl wp2 = new WaypointImpl(new MarkImpl("Test Mark 2"));
        waypoints.add(wp2);
        final WaypointImpl wp3 = new WaypointImpl(new MarkImpl("Test Mark 3"));
        waypoints.add(wp3);
        Course course = new CourseImpl("Test Course", waypoints);
        assertEquals(3, Util.size(course.getWaypoints()));
        assertEquals(2, Util.size(course.getLegs()));
        assertWaypointIndexes(course);
        course.removeWaypoint(0);
        assertWaypointIndexes(course);
        assertEquals(2, Util.size(course.getWaypoints()));
        assertEquals(1, Util.size(course.getLegs()));
        assertEquals(wp2, course.getLegs().get(0).getFrom());
        assertEquals(wp3, course.getLegs().get(0).getTo());
        assertTrue(Util.equals(Arrays.asList(new Waypoint[] { wp2, wp3 }), course.getWaypoints()));
        assertEquals(-1, course.getIndexOfWaypoint(wp1));
        assertEquals(0, course.getIndexOfWaypoint(wp2));
        assertEquals(1, course.getIndexOfWaypoint(wp3));
    }

    @Test
    public void testRemoveWaypointWithTrackedRaceListening() {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        final WaypointImpl wp1 = new WaypointImpl(new MarkImpl("Test Mark 1"));
        waypoints.add(wp1);
        final WaypointImpl wp2 = new WaypointImpl(new MarkImpl("Test Mark 2"));
        waypoints.add(wp2);
        final WaypointImpl wp3 = new WaypointImpl(new MarkImpl("Test Mark 3"));
        waypoints.add(wp3);
        Course course = new CourseImpl("Test Course", waypoints);
        assertWaypointIndexes(course);
        final Set<CompetitorImpl> hasso = Collections.singleton(AbstractLeaderboardTest.createCompetitor("Hasso"));
        DynamicTrackedRace trackedRace = new DynamicTrackedRaceImpl(/* trackedRegatta */ null,
                new RaceDefinitionImpl("Test Race", course, new BoatClassImpl("49er", /* upwind start */ true),
                        hasso),
                        EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                        /* millisecondsOverWhichToAverageWind */ 30000,
                        /* millisecondsOverWhichToAverageSpeed */ 8000,
                        EmptyRaceLogStore.INSTANCE);
        assertLegStructure(course, trackedRace);
        course.removeWaypoint(0);
        assertLegStructure(course, trackedRace);
        assertWaypointIndexes(course);
    }

    private void assertWaypointIndexes(Course course) {
        int i=0;
        for (Waypoint waypoint : course.getWaypoints()) {
            assertEquals("expected index for waypoint "+waypoint.getName()+" to be "+i+" but was "+course.getIndexOfWaypoint(waypoint),
                    i, course.getIndexOfWaypoint(waypoint));
            i++;
        }
    }

    @Test
    public void testInsertWaypointWithTrackedRaceListening() {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        final WaypointImpl wp1 = new WaypointImpl(new MarkImpl("Test Mark 1"));
        waypoints.add(wp1);
        final WaypointImpl wp2 = new WaypointImpl(new MarkImpl("Test Mark 2"));
        waypoints.add(wp2);
        Course course = new CourseImpl("Test Course", waypoints);
        final Set<CompetitorImpl> hasso = Collections.singleton(AbstractLeaderboardTest.createCompetitor("Hasso"));
        DynamicTrackedRace trackedRace = new DynamicTrackedRaceImpl(/* trackedRegatta */ null,
                new RaceDefinitionImpl("Test Race", course, new BoatClassImpl("49er", /* upwind start */ true),
                        hasso),
                        EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                        /* millisecondsOverWhichToAverageWind */ 30000,
                        /* millisecondsOverWhichToAverageSpeed */ 8000,
                        EmptyRaceLogStore.INSTANCE);
        assertLegStructure(course, trackedRace);
        final WaypointImpl wp1_5 = new WaypointImpl(new MarkImpl("Test Mark 1.5"));
        assertWaypointIndexes(course);
        course.addWaypoint(0, wp1_5);
        assertWaypointIndexes(course);
        assertLegStructure(course, trackedRace);
    }

    private void assertLegStructure(Course course, DynamicTrackedRace trackedRace) {
        assertEquals(Util.size(course.getLegs()), Util.size(trackedRace.getTrackedLegs()));
        Iterator<Leg> legIter = course.getLegs().iterator();
        Iterator<TrackedLeg> trackedLegIter = trackedRace.getTrackedLegs().iterator();
        while (legIter.hasNext()) {
            assertTrue(trackedLegIter.hasNext());
            Leg leg = legIter.next();
            TrackedLeg trackedLeg = trackedLegIter.next();
            assertSame(leg, trackedLeg.getLeg());
        }
    }
}
