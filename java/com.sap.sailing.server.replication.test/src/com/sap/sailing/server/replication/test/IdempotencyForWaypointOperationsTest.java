package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AddWaypoint;
import com.sap.sailing.server.operationaltransformation.RemoveWaypoint;
import com.sap.sse.common.Util;

/**
 * See also bug 2223 (http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=2223). The application of
 * {@link AddWaypoint} and {@link RemoveWaypoint} needs to be idempotent so that when the initial load
 * already contains the serialized results of the application of these operations on the master, the operations
 * must simply have no effect on the replica.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class IdempotencyForWaypointOperationsTest {
    private RacingEventService service;
    private RaceDefinition race;
    private Course course;
    
    @BeforeEach
    public void setUp() {
        service = mock(RacingEventService.class);
        race = mock(RaceDefinition.class);
        when(service.getRace(any(RegattaAndRaceIdentifier.class))).thenReturn(race);
        final Iterable<Waypoint> waypoints = Arrays.<Waypoint>asList(
                new WaypointImpl(new MarkImpl("M1")),
                new WaypointImpl(new MarkImpl("M2")),
                new WaypointImpl(new MarkImpl("M3")),
                new WaypointImpl(new MarkImpl("M4")),
                new WaypointImpl(new MarkImpl("M5")),
                new WaypointImpl(new MarkImpl("M6"))
                );
        course = new CourseImpl("Course name", waypoints);
        when(race.getCourse()).thenReturn(course);
    }
    
    @Test
    public void testCourseIsAlright() {
        assertEquals(6, Util.size(course.getWaypoints()));
    }
    
    @Test
    public void testAddWaypointIsIdempotent() throws Exception {
        final WaypointImpl newWaypoint = new WaypointImpl(new MarkImpl("M1.5"));
        final List<Waypoint> waypointsAfterAdd = new ArrayList<>();
        for (Waypoint wp : course.getWaypoints()) {
            waypointsAfterAdd.add(wp);
        }
        waypointsAfterAdd.add(1, newWaypoint);
        AddWaypoint op = new AddWaypoint(new RegattaNameAndRaceName("abc", "def"), 1, newWaypoint, waypointsAfterAdd);
        op.internalApplyTo(service);
        final Iterable<Waypoint> waypointsAfterFirstAdd = course.getWaypoints();
        assertSame(newWaypoint, Util.get(waypointsAfterFirstAdd, 1));
        op.internalApplyTo(service);
        assertEquals(waypointsAfterFirstAdd, course.getWaypoints());
    }

    @Test
    public void testRemoveWaypointIsIdempotent() throws Exception {
        int oldNumberOfWaypoints = Util.size(course.getWaypoints());
        RemoveWaypoint op = new RemoveWaypoint(new RegattaNameAndRaceName("abc", "def"), 1, Util.get(course.getWaypoints(), 1));
        op.internalApplyTo(service);
        final Iterable<Waypoint> waypointsAfterFirstRemove = course.getWaypoints();
        assertEquals(oldNumberOfWaypoints-1, Util.size(waypointsAfterFirstRemove));
        op.internalApplyTo(service);
        assertEquals(waypointsAfterFirstRemove, course.getWaypoints());
    }
}
