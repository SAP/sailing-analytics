package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.shared.tracking.LineDetails;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixTrackImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.TrackedLegImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class StarbordSideOfStartLineRecognitionTest {
    private TimePoint now;

    @BeforeEach
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
        when(trackedRace.getStartLine(now)).thenCallRealMethod();
        when(trackedRace.getStarboardMarkOfStartlineOrSingleStartMark(now)).thenCallRealMethod();
        Mark m = trackedRace.getStarboardMarkOfStartlineOrSingleStartMark(now);
        assertNull(m);
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
        when(trackedRace.getApproximatePosition(startWaypoint, now)).thenCallRealMethod();
        when(trackedRace.getStartLine(now)).thenCallRealMethod();
        LineDetails startLine = trackedRace.getStartLine(now);
        assertNull(startLine);
    }
    
    public static abstract class MockedTrackedRaceImpl extends DynamicTrackedRaceImpl {
        /**
         * 
         */
        private static final long serialVersionUID = -8007932232555073829L;

        public MockedTrackedRaceImpl() {
            super(null, null, Collections.<Sideline> emptyList(), null, 0, 0, 0, 0, false, OneDesignRankingMetric::new,
                    mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        }
        
        @Override
        protected Mark getStarboardMarkOfStartlineOrSingleStartMark(TimePoint at) {
            return super.getStarboardMarkOfStartlineOrSingleStartMark(at);
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
        when(windwardWaypoint.getMarks()).thenReturn(Arrays.asList(new Mark[] { windward }));
        Course course = new CourseImpl("testforSimpleFirstLeg", Arrays.asList(new Waypoint[] { startWaypoint, windwardWaypoint }));
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getCourse()).thenReturn(course);
        MockedTrackedRaceImpl trackedRace = mock(MockedTrackedRaceImpl.class);
        when(trackedRace.getRace()).thenReturn(race);
        when(trackedRace.getOrCreateTrack(startPort)).thenReturn(startPortTrack);
        when(trackedRace.getOrCreateTrack(windward)).thenReturn(windwardTrack);
        when(trackedRace.getStartLine(now)).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(startWaypoint, now)).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(windwardWaypoint, now)).thenCallRealMethod();
        when(trackedRace.getStarboardMarkOfStartlineOrSingleStartMark(now)).thenCallRealMethod();

        Position p = trackedRace.getOrCreateTrack(trackedRace.getStarboardMarkOfStartlineOrSingleStartMark(now)).
                getEstimatedPosition(now, /* extrapolate */ false);
        assertEquals(startPortPosition, p);
    }

    @Test
    public void testForSimpleFirstLeg() {
        setUp();
        Position startPortPosition = new DegreePosition(0, 0);
        Position startStarboardPosition = new DegreePosition(0, 1);
        Position windwardPosition = new DegreePosition(10, 0.5);
        MockedTrackedRaceImpl trackedRace = createTrackedRaceWithMarkPositions(startPortPosition, startStarboardPosition, windwardPosition);
        Position p = trackedRace.getOrCreateTrack(trackedRace.getStartLine(now).getStarboardMarkWhileApproachingLine()).
                getEstimatedPosition(now, /* extrapolate */ false);
        PositionAssert.assertPositionEquals(startStarboardPosition, p, 0.0000001);
    }

    @Test
    public void testForSimpleFirstLegOtherWay() {
        setUp();
        Position startPortPosition = new DegreePosition(0, 0);
        Position startStarboardPosition = new DegreePosition(0, 1);
        Position windwardPosition = new DegreePosition(-10, 0.5);
        MockedTrackedRaceImpl trackedRace = createTrackedRaceWithMarkPositions(startPortPosition, startStarboardPosition, windwardPosition);
        
        Position p = trackedRace.getOrCreateTrack(trackedRace.getStartLine(now).getStarboardMarkWhileApproachingLine()).
                getEstimatedPosition(now, /* extrapolate */ false);
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
        when(trackedRace.getStartLine(now)).thenCallRealMethod();
        TrackedLegImpl trackedLeg = new TrackedLegImpl(trackedRace, course.getFirstLeg(), /* competitors */ new ArrayList<Competitor>());
        when(trackedRace.getTrackedLeg(course.getFirstLeg())).thenReturn(trackedLeg);
        when(trackedRace.getApproximatePosition(startWaypoint, now)).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(windwardWaypoint, now)).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(ArgumentMatchers.eq(startWaypoint), ArgumentMatchers.eq(now), ArgumentMatchers.any(MarkPositionAtTimePointCache.class))).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(ArgumentMatchers.eq(windwardWaypoint), ArgumentMatchers.eq(now), ArgumentMatchers.any(MarkPositionAtTimePointCache.class))).thenCallRealMethod();
        return trackedRace;
    }

}
