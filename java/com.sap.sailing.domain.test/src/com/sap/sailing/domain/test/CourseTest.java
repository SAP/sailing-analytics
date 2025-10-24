package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

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
        List<com.sap.sse.common.Util.Pair<ControlPoint, PassingInstruction>> courseToUpdate = new ArrayList<com.sap.sse.common.Util.Pair<ControlPoint, PassingInstruction>>();
        courseToUpdate.add(new com.sap.sse.common.Util.Pair<ControlPoint, PassingInstruction>(wp2.getMarks().iterator().next(), wp2.getPassingInstructions()));
        courseToUpdate.add(new com.sap.sse.common.Util.Pair<ControlPoint, PassingInstruction>(wp3.getMarks().iterator().next(), wp3.getPassingInstructions()));
        courseToUpdate.add(new com.sap.sse.common.Util.Pair<ControlPoint, PassingInstruction>(wp1.getMarks().iterator().next(), wp1.getPassingInstructions()));
        course.update(courseToUpdate, new HashMap<>(), course.getOriginatingCourseTemplateIdOrNull(),
                DomainFactory.INSTANCE);
        assertWaypointIndexes(course);
    }
    
    /**
     * This test tries to replicate the behavior described in bug 2223. The course was
     * "RC-Black Conical -> Orange -> White Gate -> Red -> Yellow -> Finish Pole-Cylinder" and the delta was
     * "[[DeleteDelta, position: 1, lines: [Orange, White Gate]], [InsertDelta, position: 4, lines: [White Gate, Red]]]"
     * which was computed from the new sequence of control points
     * "[Control, RC-Black Conical, Control, Red, Control, White Gate, Control, Red, Control, Yellow, Control, Finish Pole-Cylinder]".
     */
    @Test
    public void testWaypointDeleteWithSubsequentInsertInOnePatch() throws PatchFailedException {
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        final WaypointImpl rcBlackConical = new WaypointImpl(new ControlPointWithTwoMarksImpl(new MarkImpl("RC"),
                new MarkImpl("Black Conical"), "RC-Black Conical", "RC-Black Conical"));
        waypoints.add(rcBlackConical);
        final WaypointImpl orange = new WaypointImpl(new MarkImpl("Orange"));
        waypoints.add(orange);
        final WaypointImpl whiteGate = new WaypointImpl(new ControlPointWithTwoMarksImpl(new MarkImpl("White L"),
                new MarkImpl("White R"), "White Gate", "White Gate"));
        waypoints.add(whiteGate);
        final WaypointImpl red = new WaypointImpl(new MarkImpl("Red"));
        waypoints.add(red);
        final WaypointImpl yellow = new WaypointImpl(new MarkImpl("Yellow"));
        waypoints.add(yellow);
        final WaypointImpl finishPoleCylinder = new WaypointImpl(new ControlPointWithTwoMarksImpl(
                new MarkImpl("Finish Pole"), new MarkImpl("Cylinder"), "Finish Pole-Cylinder", "Finish Pole-Cylinder"));
        waypoints.add(finishPoleCylinder);
        Course course = new CourseImpl("Race 24", waypoints);
        assertWaypointIndexes(course);
        List<com.sap.sse.common.Util.Pair<ControlPoint, PassingInstruction>> courseToUpdate = new ArrayList<com.sap.sse.common.Util.Pair<ControlPoint, PassingInstruction>>();
        courseToUpdate.add(new Pair<>(rcBlackConical.getControlPoint(), rcBlackConical.getPassingInstructions()));
        courseToUpdate.add(new Pair<>(red.getControlPoint(), red.getPassingInstructions()));
        courseToUpdate.add(new Pair<>(whiteGate.getControlPoint(), whiteGate.getPassingInstructions()));
        courseToUpdate.add(new Pair<>(red.getControlPoint(), red.getPassingInstructions()));
        courseToUpdate.add(new Pair<>(yellow.getControlPoint(), yellow.getPassingInstructions()));
        courseToUpdate.add(new Pair<>(finishPoleCylinder.getControlPoint(), finishPoleCylinder.getPassingInstructions()));
        course.update(courseToUpdate, new HashMap<>(), course.getOriginatingCourseTemplateIdOrNull(),
                DomainFactory.INSTANCE);
        assertWaypointIndexes(course);
        List<ControlPoint> newControlPoints = new ArrayList<>();
        for (Waypoint newWp : course.getWaypoints()) {
            newControlPoints.add(newWp.getControlPoint());
        }
        assertTrue(Util.equals(Arrays.asList(
                rcBlackConical.getControlPoint(),
                red.getControlPoint(),
                whiteGate.getControlPoint(),
                red.getControlPoint(),
                yellow.getControlPoint(),
                finishPoleCylinder.getControlPoint()),
            newControlPoints));
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
        final BoatClass boatClass = new BoatClassImpl("505", /* upwind start */true);
        final CompetitorWithBoat hasso = AbstractLeaderboardTest.createCompetitorWithBoat("Hasso");
        final Map<Competitor,Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(hasso, hasso.getBoat());
        DynamicTrackedRace trackedRace = new DynamicTrackedRaceImpl(/* trackedRegatta */new DynamicTrackedRegattaImpl(
                new RegattaImpl("test", null, true, CompetitorRegistrationType.CLOSED, null, null,
                        new HashSet<Series>(), false, null, "test", null, OneDesignRankingMetric::new,
                        /* registrationLinkSecret */ UUID.randomUUID().toString())),
                new RaceDefinitionImpl("Test Race", course, boatClass, competitorsAndBoats),
                Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */3000,
                /* millisecondsOverWhichToAverageWind */30000,
                /* millisecondsOverWhichToAverageSpeed */8000, /*useMarkPassingCalculator*/ false, OneDesignRankingMetric::new,
                mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        assertLegStructure(course, trackedRace);
        course.removeWaypoint(0);
        assertLegStructure(course, trackedRace);
        assertWaypointIndexes(course);
    }

    private void assertWaypointIndexes(Course course) {
        int i=0;
        for (Waypoint waypoint : course.getWaypoints()) {
            assertEquals(i,
                    course.getIndexOfWaypoint(waypoint), "expected index for waypoint "+waypoint.getName()+" to be "+i+" but was "+course.getIndexOfWaypoint(waypoint));
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
        final BoatClass boatClass = new BoatClassImpl("505", /* upwind start */true);
        final CompetitorWithBoat hasso = AbstractLeaderboardTest.createCompetitorWithBoat("Hasso");
        final Map<Competitor,Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(hasso, hasso.getBoat());
        DynamicTrackedRace trackedRace = new DynamicTrackedRaceImpl(/* trackedRegatta */ new DynamicTrackedRegattaImpl(
                new RegattaImpl("test", null, true, CompetitorRegistrationType.CLOSED, null, null,
                        new HashSet<Series>(), false, null, "test", null, OneDesignRankingMetric::new,
                        /* registrationLinkSecret */ UUID.randomUUID().toString())),
                new RaceDefinitionImpl("Test Race", course, boatClass, competitorsAndBoats), Collections.<Sideline> emptyList(),
                EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                        /* millisecondsOverWhichToAverageWind */ 30000,
                        /* millisecondsOverWhichToAverageSpeed */ 8000, /*useMarkPassingCalculator*/ false, OneDesignRankingMetric::new,
                        mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
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
