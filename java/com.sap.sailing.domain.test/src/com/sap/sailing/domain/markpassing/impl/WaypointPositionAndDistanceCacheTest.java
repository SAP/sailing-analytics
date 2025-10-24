package com.sap.sailing.domain.markpassing.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.markpassingcalculation.impl.WaypointPositionAndDistanceCache;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class WaypointPositionAndDistanceCacheTest {
    private WaypointPositionAndDistanceCache cache;
    private int recalculations;
    private DynamicTrackedRace trackedRace;
    /**
     * A time point rouded to be at the center of a cache time range.
     */
    private TimePoint now;
    private Waypoint start;
    private Waypoint windwardWaypoint;
    private Waypoint finish;
    private Duration timeRangeResolution;
    private Mark pinEnd;
    private Mark startBoat;
    private Mark windward;
    private ControlPointWithTwoMarks startFinish;
    
    @BeforeEach
    public void setUp() {
        final RaceDefinition race = mock(RaceDefinition.class);
        when(race.getCompetitors()).thenReturn(Collections.emptyList());
        when(race.getBoatClass()).thenReturn(DomainFactory.INSTANCE.getOrCreateBoatClass("29er"));
        pinEnd = new MarkImpl("Pin End");
        startBoat = new MarkImpl("Start Boat");
        windward = new MarkImpl("Windward");
        startFinish = new ControlPointWithTwoMarksImpl(UUID.randomUUID(), pinEnd, startBoat, "Start/Finish",
                "Start/Finish");
        start = new WaypointImpl(startFinish);
        windwardWaypoint = new WaypointImpl(windward);
        finish = new WaypointImpl(startFinish);
        Course course = new CourseImpl("Course name", Arrays.asList(start, windwardWaypoint, finish));
        when(race.getCourse()).thenReturn(course);
        final TrackedRegatta trackedRegatta = mock(TrackedRegatta.class);
        final Regatta regatta = mock(Regatta.class);
        when(regatta.getName()).thenReturn("Regatta name");
        when(trackedRegatta.getRegatta()).thenReturn(regatta);
        trackedRace = new DynamicTrackedRaceImpl(trackedRegatta, race, Collections.emptyList(), EmptyWindStore.INSTANCE,
                /* delayToLiveInMillis */ 8000, /* millisecondsOverWhichToAverageWind */ 30000,
                /* millisecondsOverWhichToAverageSpeed */ 15000, /* delayForCacheInvalidationOfWindEstimation */ 10000,
                /* useInternalMarkPassingAlgorithm */ false, OneDesignRankingMetric::new, mock(RaceLogAndTrackedRaceResolver.class), null, /* markPassingRaceFingerprintRegistry */ null);
        timeRangeResolution = Duration.ONE_MINUTE;
        now = new MillisecondsTimePoint(MillisecondsTimePoint.now().asMillis() / timeRangeResolution.asMillis() * timeRangeResolution.asMillis());
        trackedRace.getOrCreateTrack(pinEnd).addGPSFix(new GPSFixImpl(new DegreePosition(0, -0.0000001), now));
        trackedRace.getOrCreateTrack(startBoat).addGPSFix(new GPSFixImpl(new DegreePosition(0, 0.0000001), now));
        trackedRace.getOrCreateTrack(windward).addGPSFix(new GPSFixImpl(new DegreePosition(1, 0), now));
        recalculations = 0;
        cache = new WaypointPositionAndDistanceCache(trackedRace, timeRangeResolution) {
            @Override
            protected <R> R computeResult(Function<TimePoint, R> resultCalculator, TimePoint roundedToTimeRangeCenter) {
                recalculations++;
                return super.computeResult(resultCalculator, roundedToTimeRangeCenter);
            }
        };
    }
    
    @Test
    public void simpleTest() {
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        assertEquals(3, recalculations); // one for the position of each waypoint, one for the distance; second request coming from cache
    }

    @Test
    public void testBackAndForth() {
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        assertEquals(60, cache.getApproximateDistance(windwardWaypoint, start, now).getNauticalMiles(), 0.01);
        assertEquals(3, recalculations); // one for the position of each waypoint, one for the distance which creates two symmetrical cache entries
    }

    @Test
    public void testWithDifferentTimesStillInRange() {
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now.plus(timeRangeResolution.divide(4))).getNauticalMiles(), 0.01);
        assertEquals(3, recalculations); // one for the position of each waypoint, one for the distance which creates two symmetrical cache entries
        // the second query is expected to come from the cache because its time is still in range of the previous cache entries
    }

    @Test
    public void testNewMarkPositionAffectingEntry() {
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        trackedRace.getOrCreateTrack(windward).add(new GPSFixImpl(new DegreePosition(1, 0).translateGreatCircle(
                new DegreeBearingImpl(0), new KnotSpeedImpl(20).travel(now, now.plus(timeRangeResolution.divide(10)))),
                now.plus(timeRangeResolution.divide(10)))); // does not affect the cache interval
        trackedRace.getOrCreateTrack(windward).add(new GPSFixImpl(new DegreePosition(2, 0), now.plus(1 /* ms */))); // affects the cache interval
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        assertEquals(5, recalculations); // one for each waypoint, one for the distance; second request recalculates except start/finish position
    }

    /**
     * A white-box text for bug 3364: when multiple cache entries exist in the map, invalidating a cache entry can cause a
     * ConcurrentModificationException.
     */
    @Test
    public void testInvalidateByNewMarkPositionWithMultipleMarkPositionsInCache() {
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        trackedRace.getOrCreateTrack(windward).add(new GPSFixImpl(new DegreePosition(1, 0).translateGreatCircle(
                new DegreeBearingImpl(0), new KnotSpeedImpl(20).travel(now, now.plus(timeRangeResolution.divide(10)))),
                now.plus(timeRangeResolution.divide(10)))); // does not affect the cache interval
        trackedRace.getOrCreateTrack(windward).add(new GPSFixImpl(new DegreePosition(1, 0).translateGreatCircle(
                new DegreeBearingImpl(0), new KnotSpeedImpl(20).travel(now, now.plus(timeRangeResolution.times(2)))),
                now.plus(timeRangeResolution.times(2))));
        trackedRace.getOrCreateTrack(windward).add(new GPSFixImpl(new DegreePosition(1, 0).translateGreatCircle(
                new DegreeBearingImpl(0), new KnotSpeedImpl(20).travel(now, now.plus(timeRangeResolution.times(3)))),
                now.plus(timeRangeResolution.times(3))));
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        // with no interpolation on the mark tracks of type GPSFixTrackImpl, asking for time point "timeRangeResolution"
        // will base the result on the last fix *before* that point in time, in this case the fix set for "windward" at
        // time point "timeRangeResolution.divide(10)"
        assertEquals(60+new NauticalMileDistance(20).scale(timeRangeResolution.divide(10).divide(Duration.ONE_HOUR)).getNauticalMiles(), cache.getApproximateDistance(start, windwardWaypoint, now.plus(timeRangeResolution)).getNauticalMiles(), 0.01);
        assertEquals(60+new NauticalMileDistance(20).scale(timeRangeResolution.times(2).divide(Duration.ONE_HOUR)).getNauticalMiles(), cache.getApproximateDistance(start, windwardWaypoint, now.plus(timeRangeResolution.times(2))).getNauticalMiles(), 0.01);
        trackedRace.getOrCreateTrack(windward).add(new GPSFixImpl(new DegreePosition(1, 0).translateGreatCircle(
                new DegreeBearingImpl(0), new KnotSpeedImpl(20).travel(now, now.plus(timeRangeResolution.minus(1)))),
                now.plus(timeRangeResolution.minus(1)))); // cause cache invalidation with several entries in the map
        recalculations = 0;
        assertEquals(60+new NauticalMileDistance(20).scale(timeRangeResolution.divide(Duration.ONE_HOUR)).getNauticalMiles(), cache.getApproximateDistance(start, windwardWaypoint, now.plus(timeRangeResolution)).getNauticalMiles(), 0.01);
        assertEquals(2, recalculations); // distance calculation also requires position calculation, therefore 2, not 1
    }

    @Test
    public void testNewMarkPositionAffectingEntryOppositeOrder() {
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        trackedRace.getOrCreateTrack(windward).add(new GPSFixImpl(new DegreePosition(1, 0).translateGreatCircle(
                new DegreeBearingImpl(0), new KnotSpeedImpl(20).travel(now, now.plus(timeRangeResolution.divide(10)))),
                now.plus(timeRangeResolution.divide(10)))); // does not affect the cache interval
        trackedRace.getOrCreateTrack(windward).add(new GPSFixImpl(new DegreePosition(2, 0), now.plus(1 /* ms */))); // affects the cache interval
        assertEquals(60, cache.getApproximateDistance(windwardWaypoint, start, now).getNauticalMiles(), 0.01);
        assertEquals(5, recalculations); // one for each waypoint, one for the distance; second request recalculates except start/finish position
    }

    @Test
    public void testNewMarkPositionNotAffectingEntry() {
        trackedRace.getOrCreateTrack(windward).add(new GPSFixImpl(new DegreePosition(1, 0).translateGreatCircle(
                new DegreeBearingImpl(0), new KnotSpeedImpl(20).travel(now, now.plus(timeRangeResolution))),
                now.plus(timeRangeResolution)));
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        trackedRace.getOrCreateTrack(windward).add(new GPSFixImpl(new DegreePosition(1, 0).translateGreatCircle(
                new DegreeBearingImpl(0), new KnotSpeedImpl(20).travel(now, now.plus(timeRangeResolution.times(2)))),
                now.plus(timeRangeResolution.times(2)))); // only affects back to now.plus(timeRangeResolution) but not all the way back to now
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        assertEquals(60, cache.getApproximateDistance(windwardWaypoint, start, now).getNauticalMiles(), 0.01);
        assertEquals(3, recalculations); // one for the position of each waypoint, one for the distance; second / third request from cache
    }

    @Test
    public void testTwoLapCourseWithSameControlPointsButDifferentWaypoints() {
        Waypoint leewardGate = new WaypointImpl(startFinish, PassingInstruction.Gate);
        Waypoint windward2 = new WaypointImpl(windward);
        trackedRace.getRace().getCourse().addWaypoint(2, leewardGate);
        trackedRace.getRace().getCourse().addWaypoint(3, windward2);
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        assertEquals(60, cache.getApproximateDistance(start, windwardWaypoint, now).getNauticalMiles(), 0.01);
        assertEquals(60, cache.getApproximateDistance(leewardGate, windward2, now).getNauticalMiles(), 0.01);
        assertEquals(3, recalculations); // one for the position of each waypoint, one for the distance; second and third request coming from cache
    }
}
