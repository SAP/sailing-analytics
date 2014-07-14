package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.racelog.tracking.EmptyGPSFixStore;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.Util;

public class UpdateMarkPassingTest {
    @Test
    public void testMarkPassingsForWaypointInOrder() {
        Waypoint waypoint = new WaypointImpl(new MarkImpl("Test Mark"));
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getName()).thenReturn("Test Race");
        Course c = new CourseImpl("Test Course", Collections.singleton(waypoint));
        when(race.getCourse()).thenReturn(c);
        Competitor competitor = TrackBasedTest.createCompetitor("Test Competitor");
        when(race.getBoatClass()).thenReturn(new BoatClassImpl("49er", /* typicallyStartsUpwind */ true));
        when(race.getCompetitors()).thenReturn(Collections.singleton(competitor));
        DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(
        /* trackedRegatta */new DynamicTrackedRegattaImpl(new RegattaImpl("test", null, new HashSet<Series>(), false, null,
                "test", null)), race, Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, EmptyGPSFixStore.INSTANCE,
        /* delayToLiveInMillis */1000, /* millisecondsOverWhichToAverageWind */30000,
        /* millisecondsOverWhichToAverageSpeed */30000);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = now.plus(1000);
        trackedRace.updateMarkPassings(competitor, Arrays.asList(new MarkPassing[] { new MarkPassingImpl(now, waypoint, competitor) }));
        trackedRace.updateMarkPassings(competitor, Arrays.asList(new MarkPassing[] { new MarkPassingImpl(later, waypoint, competitor) }));
        Iterable<MarkPassing> waypointPassings = trackedRace.getMarkPassingsInOrder(waypoint);
        assertEquals(1, Util.size(waypointPassings));
        assertEquals(later, waypointPassings.iterator().next().getTimePoint());
    }
}
