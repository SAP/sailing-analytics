package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CompetitorWithBoat;
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
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class UpdateMarkPassingTest {
    @Test
    public void testMarkPassingsForWaypointInOrder() {
        Waypoint waypoint = new WaypointImpl(new MarkImpl("Test Mark"));
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getName()).thenReturn("Test Race");
        Course c = new CourseImpl("Test Course", Collections.singleton(waypoint));
        when(race.getCourse()).thenReturn(c);
        CompetitorWithBoat competitor = TrackBasedTest.createCompetitorWithBoat("Test Competitor");
        when(race.getBoatOfCompetitor(competitor)).thenReturn(competitor.getBoat());
        when(race.getBoatClass()).thenReturn(new BoatClassImpl("49er", /* typicallyStartsUpwind */ true));
        when(race.getCompetitors()).thenReturn(Collections.singleton(competitor));
        DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(
                /* trackedRegatta */new DynamicTrackedRegattaImpl(new RegattaImpl("test", null, true,
                        CompetitorRegistrationType.CLOSED, null, null, new HashSet<Series>(), false, null, "test", null,
                        OneDesignRankingMetric::new, /* registrationLinkSecret */ UUID.randomUUID().toString())),
                race, Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */1000,
                /* millisecondsOverWhichToAverageWind */30000, /* millisecondsOverWhichToAverageSpeed */30000,
                /* useMarkPassingCalculator */ false, OneDesignRankingMetric::new,
                mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = now.plus(1000);
        trackedRace.updateMarkPassings(competitor, Arrays.asList(new MarkPassing[] { new MarkPassingImpl(now, waypoint, competitor) }));
        trackedRace.updateMarkPassings(competitor, Arrays.asList(new MarkPassing[] { new MarkPassingImpl(later, waypoint, competitor) }));
        Iterable<MarkPassing> waypointPassings = trackedRace.getMarkPassingsInOrder(waypoint);
        assertEquals(1, Util.size(waypointPassings));
        assertEquals(later, waypointPassings.iterator().next().getTimePoint());
    }
}
