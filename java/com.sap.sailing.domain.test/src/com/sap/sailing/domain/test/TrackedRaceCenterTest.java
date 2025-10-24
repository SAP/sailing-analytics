package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixTrackImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.TrackedLegImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Tests the calculation of the center of the course, particularly the
 * {@link TrackedRace#getCenterOfCourse(com.sap.sse.common.TimePoint)} method.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class TrackedRaceCenterTest {
    private Mark mark1;
    private Mark mark2;
    private Waypoint wp1;
    private Waypoint wp2;
    private Course course;
    private DynamicGPSFixTrack<Mark, GPSFix> mark1Track;
    private DynamicGPSFixTrack<Mark, GPSFix> mark2Track;
    private DynamicTrackedRaceImpl trackedRace;

    @BeforeEach
    public void setUp() {
        trackedRace = mock(DynamicTrackedRaceImpl.class);
        when(trackedRace.getCenterOfCourse(ArgumentMatchers.any(TimePoint.class))).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(ArgumentMatchers.any(Waypoint.class), ArgumentMatchers.any(TimePoint.class))).thenCallRealMethod();
        when(trackedRace.getApproximatePosition(ArgumentMatchers.any(Waypoint.class), ArgumentMatchers.any(TimePoint.class), ArgumentMatchers.any(MarkPositionAtTimePointCache.class))).thenCallRealMethod();
        RaceDefinition race = mock(RaceDefinition.class);
        when(trackedRace.getRace()).thenReturn(race);
        mark1 = new MarkImpl("1");
        mark2 = new MarkImpl("2");
        wp1 = new WaypointImpl(mark1);
        wp2 = new WaypointImpl(mark2);
        course = new CourseImpl("The Course", Arrays.asList(wp1, wp2));
        when(race.getCourse()).thenReturn(course);
        mark1Track = new DynamicGPSFixTrackImpl<Mark>(mark1, /* millisecondsOverWhichToAverage */ 10);
        mark2Track = new DynamicGPSFixTrackImpl<Mark>(mark1, /* millisecondsOverWhichToAverage */ 10);
        when(trackedRace.getOrCreateTrack(mark1)).thenReturn(mark1Track);
        when(trackedRace.getOrCreateTrack(mark2)).thenReturn(mark2Track);
        final TrackedLeg trackedLeg = new TrackedLegImpl(trackedRace, course.getLeg(0), Collections.emptySet());
        when(trackedRace.getTrackedLeg(course.getLeg(0))).thenReturn(trackedLeg);
    }
    
    @Test
    public void testSimpleAverage() {
        TimePoint now = MillisecondsTimePoint.now();
        Position mark1Pos = new DegreePosition(10, 10);
        mark1Track.add(new GPSFixImpl(mark1Pos, now));
        Position mark2Pos = new DegreePosition(20, 10);
        mark2Track.add(new GPSFixImpl(mark2Pos, now));
        Position center = trackedRace.getCenterOfCourse(now);
        assertEquals(15, center.getLatDeg(), 0.00001);
        assertEquals(10, center.getLngDeg(), 0.00001);
    }

    @Test
    public void testTriangle() {
        Mark mark3 = new MarkImpl("3");
        Waypoint wp3 = new WaypointImpl(mark3);
        course.addWaypoint(2, wp3);
        Waypoint wp4 = new WaypointImpl(mark1);
        course.addWaypoint(3, wp4);
        final TrackedLeg wp2ToWp3 = new TrackedLegImpl(trackedRace, course.getLeg(1), Collections.emptySet());
        when(trackedRace.getTrackedLeg(course.getLeg(1))).thenReturn(wp2ToWp3);
        final TrackedLeg wp3ToWp4 = new TrackedLegImpl(trackedRace, course.getLeg(2), Collections.emptySet());
        when(trackedRace.getTrackedLeg(course.getLeg(2))).thenReturn(wp3ToWp4);
        DynamicGPSFixTrackImpl<Mark> mark3Track = new DynamicGPSFixTrackImpl<Mark>(mark3, /* millisecondsOverWhichToAverage */ 10);
        when(trackedRace.getOrCreateTrack(mark3)).thenReturn(mark3Track);
        TimePoint now = MillisecondsTimePoint.now();
        Position mark1Pos = new DegreePosition(0, 0);
        mark1Track.add(new GPSFixImpl(mark1Pos, now));
        Position mark2Pos = new DegreePosition(0, 10);
        mark2Track.add(new GPSFixImpl(mark2Pos, now));
        Position mark3Pos = new DegreePosition(10, 5);
        mark3Track.add(new GPSFixImpl(mark3Pos, now));
        Position center = trackedRace.getCenterOfCourse(now);
        assertTrue(2 < center.getLatDeg() && center.getLatDeg() < 8);
        assertEquals(5, center.getLngDeg(), 0.00001);
    }
}
