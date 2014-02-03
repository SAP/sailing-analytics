package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixTrackImpl;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;

public class StarbordSideOfStartLineRecognitionTest {
    private MillisecondsTimePoint now;

    @Before
    public void setUp() {
        now = MillisecondsTimePoint.now();
    }

    @Test
    public void testForEmptyCourse() {
        setUp();
        Course course = new CourseImpl("testforSimpleFirstLeg", Arrays.asList(new Waypoint[0]));
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getCourse()).thenReturn(course);
        MockedTrackedRaceImpl trackedRace = mock(MockedTrackedRaceImpl.class);
        when(trackedRace.getRace()).thenReturn(race);
        when(trackedRace.getStarboardMarkOfStartlinePosition(now)).thenCallRealMethod();
        Position p = trackedRace.getStarboardMarkOfStartlinePosition(now);
        assertNull(p);
    }
    
    @Test
    public void testForCourseWithOnlyOneWaypoint() {
        setUp();
        Position startPortPosition = new DegreePosition(0, 0);
        Position startStarboardPosition = new DegreePosition(0, 1);
        Waypoint startWaypoint = mock(Waypoint.class);
        Mark startPort = mock(Mark.class);
        DynamicGPSFixTrack<Mark, GPSFix> startPortTrack = new DynamicGPSFixTrackImpl<Mark>(startPort, /* millisecondsOverWhichToAverage */ 10000);
        startPortTrack.addGPSFix(new GPSFixImpl(startPortPosition, now));
        Mark startStarboard = mock(Mark.class);
        DynamicGPSFixTrack<Mark, GPSFix> startStarboardTrack = new DynamicGPSFixTrackImpl<Mark>(startStarboard, /* millisecondsOverWhichToAverage */ 10000);
        startStarboardTrack.addGPSFix(new GPSFixImpl(startStarboardPosition, now));
        when(startWaypoint.getMarks()).thenReturn(Arrays.asList(new Mark[] { startPort, startStarboard }));
        Course course = new CourseImpl("testforSimpleFirstLeg", Arrays.asList(new Waypoint[] { startWaypoint }));
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getCourse()).thenReturn(course);
        MockedTrackedRaceImpl trackedRace = mock(MockedTrackedRaceImpl.class);
        when(trackedRace.getRace()).thenReturn(race);
        when(trackedRace.getOrCreateTrack(startPort)).thenReturn(startPortTrack);
        when(trackedRace.getOrCreateTrack(startStarboard)).thenReturn(startStarboardTrack);
        when(trackedRace.getStarboardMarkOfStartlinePosition(now)).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(startWaypoint, now)).thenCallRealMethod();
        Position p = trackedRace.getStarboardMarkOfStartlinePosition(now);
        assertNull(p);
    }
    
    public static abstract class MockedTrackedRaceImpl extends TrackedRaceImpl {
        /**
         * 
         */
        private static final long serialVersionUID = -8007932232555073829L;

        public MockedTrackedRaceImpl() {
            super(null, null, Collections.<Sideline> emptyList(), null, 0, 0, 0, 0);
        }
        
    }

    @Test
    public void testForSimpleFirstLegWithSingleStartMark() {
        setUp();
        Position startPortPosition = new DegreePosition(0, 0);
        Position windwardPosition = new DegreePosition(10, 0.5);
        Waypoint startWaypoint = mock(Waypoint.class);
        Mark startPort = mock(Mark.class);
        DynamicGPSFixTrack<Mark, GPSFix> startPortTrack = new DynamicGPSFixTrackImpl<Mark>(startPort, /* millisecondsOverWhichToAverage */ 10000);
        startPortTrack.addGPSFix(new GPSFixImpl(startPortPosition, now));
        when(startWaypoint.getMarks()).thenReturn(Arrays.asList(new Mark[] { startPort }));
        Waypoint windwardWaypoint = mock(Waypoint.class);
        Mark windward = mock(Mark.class);
        DynamicGPSFixTrack<Mark, GPSFix> windwardTrack = new DynamicGPSFixTrackImpl<Mark>(windward, /* millisecondsOverWhichToAverage */ 10000);
        windwardTrack.addGPSFix(new GPSFixImpl(windwardPosition, now));
        when(windwardWaypoint.getMarks()).thenReturn(Arrays.asList(new Mark[] { windward}));
        Course course = new CourseImpl("testforSimpleFirstLeg", Arrays.asList(new Waypoint[] { startWaypoint, windwardWaypoint }));
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getCourse()).thenReturn(course);
        MockedTrackedRaceImpl trackedRace = mock(MockedTrackedRaceImpl.class);
        when(trackedRace.getRace()).thenReturn(race);
        when(trackedRace.getOrCreateTrack(startPort)).thenReturn(startPortTrack);
        when(trackedRace.getOrCreateTrack(windward)).thenReturn(windwardTrack);
        when(trackedRace.getStarboardMarkOfStartlinePosition(now)).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(startWaypoint, now)).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(windwardWaypoint, now)).thenCallRealMethod();
        
        Position p = trackedRace.getStarboardMarkOfStartlinePosition(now);
        assertEquals(startPortPosition, p);
    }

    @Test
    public void testForSimpleFirstLeg() {
        setUp();
        Position startPortPosition = new DegreePosition(0, 0);
        Position startStarboardPosition = new DegreePosition(0, 1);
        Position windwardPosition = new DegreePosition(10, 0.5);
        MockedTrackedRaceImpl trackedRace = createTrackedRaceWithMarkPositions(startPortPosition, startStarboardPosition, windwardPosition);
        
        Position p = trackedRace.getStarboardMarkOfStartlinePosition(now);
        assertEquals(startStarboardPosition, p);
    }

    @Test
    public void testForSimpleFirstLegOtherWay() {
        setUp();
        Position startPortPosition = new DegreePosition(0, 0);
        Position startStarboardPosition = new DegreePosition(0, 1);
        Position windwardPosition = new DegreePosition(-10, 0.5);
        MockedTrackedRaceImpl trackedRace = createTrackedRaceWithMarkPositions(startPortPosition, startStarboardPosition, windwardPosition);
        
        Position p = trackedRace.getStarboardMarkOfStartlinePosition(now);
        assertEquals(startPortPosition, p);
    }

    private MockedTrackedRaceImpl createTrackedRaceWithMarkPositions(Position startPortPosition,
            Position startStarboardPosition, Position windwardPosition) {
        Waypoint startWaypoint = mock(Waypoint.class);
        Mark startPort = mock(Mark.class);
        DynamicGPSFixTrack<Mark, GPSFix> startPortTrack = new DynamicGPSFixTrackImpl<Mark>(startPort, /* millisecondsOverWhichToAverage */ 10000);
        startPortTrack.addGPSFix(new GPSFixImpl(startPortPosition, now));
        Mark startStarboard = mock(Mark.class);
        DynamicGPSFixTrack<Mark, GPSFix> startStarboardTrack = new DynamicGPSFixTrackImpl<Mark>(startStarboard, /* millisecondsOverWhichToAverage */ 10000);
        startStarboardTrack.addGPSFix(new GPSFixImpl(startStarboardPosition, now));
        when(startWaypoint.getMarks()).thenReturn(Arrays.asList(new Mark[] { startPort, startStarboard }));
        Waypoint windwardWaypoint = mock(Waypoint.class);
        Mark windward = mock(Mark.class);
        DynamicGPSFixTrack<Mark, GPSFix> windwardTrack = new DynamicGPSFixTrackImpl<Mark>(windward, /* millisecondsOverWhichToAverage */ 10000);
        windwardTrack.addGPSFix(new GPSFixImpl(windwardPosition, now));
        when(windwardWaypoint.getMarks()).thenReturn(Arrays.asList(new Mark[] { windward}));
        Course course = new CourseImpl("testforSimpleFirstLeg", Arrays.asList(new Waypoint[] { startWaypoint, windwardWaypoint }));
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getCourse()).thenReturn(course);
        MockedTrackedRaceImpl trackedRace = mock(MockedTrackedRaceImpl.class);
        when(trackedRace.getRace()).thenReturn(race);
        when(trackedRace.getOrCreateTrack(startPort)).thenReturn(startPortTrack);
        when(trackedRace.getOrCreateTrack(startStarboard)).thenReturn(startStarboardTrack);
        when(trackedRace.getOrCreateTrack(windward)).thenReturn(windwardTrack);
        when(trackedRace.getStarboardMarkOfStartlinePosition(now)).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(startWaypoint, now)).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(windwardWaypoint, now)).thenCallRealMethod();
        return trackedRace;
    }

}
