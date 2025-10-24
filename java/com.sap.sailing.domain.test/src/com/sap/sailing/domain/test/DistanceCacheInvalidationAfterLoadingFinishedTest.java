package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackingDataLoader;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * See bug 3648: When a race is loading, cache invalidation is suspended which also affects the distance cache.
 * However, for the distance cache, during resuming caching operations there was a cache invalidation missing.
 * This tests, failing before the fix for bug 3648, ensures that this type of invalidation actually happens.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class DistanceCacheInvalidationAfterLoadingFinishedTest extends TrackBasedTest {
    private CompetitorWithBoat competitor;
    private DynamicTrackedRace trackedRace;
    
    @BeforeEach
    public void setUp() {
        competitor = createCompetitorWithBoat("Test Competitor");
        Map<Competitor, Boat> competitorsAndBoats = TrackBasedTest.createCompetitorAndBoatsMap(competitor);
        trackedRace = createTestTrackedRace("Test Regatta", "Test Race", "505", competitorsAndBoats, MillisecondsTimePoint.now(), /* useMarkPassingCalculator */ false);
    }
    
    @Test
    public void testDistanceCalculationWhileLoading() {
        final TimePoint now = MillisecondsTimePoint.now();
        final TrackingDataLoader tdl = new TrackingDataLoader() {};
        trackedRace.updateMarkPassings(competitor, Collections.singleton(new MarkPassingImpl(now, trackedRace.getRace().getCourse().getFirstWaypoint(), competitor)));
        // no distance traveled yet because there is no fix yet; this may be cached for now
        assertEquals(Distance.NULL, trackedRace.getDistanceTraveled(competitor, now));
        trackedRace.onStatusChanged(tdl, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, /* progress */ 0.1));
        assertEquals(Distance.NULL, trackedRace.getDistanceTraveled(competitor, now.plus(Duration.ONE_SECOND.times(2))));
        final Position startPos = new DegreePosition(0, 0);
        final DegreeBearingImpl bearing = new DegreeBearingImpl(10);
        final KnotSpeedWithBearingImpl speed = new KnotSpeedWithBearingImpl(10, bearing);
        trackedRace.recordFix(competitor, new GPSFixMovingImpl(startPos, now, speed, /* optionalTrueHeading */ null));
        final Distance distance = speed.travel(Duration.ONE_SECOND);
        trackedRace.recordFix(competitor, new GPSFixMovingImpl(startPos.translateGreatCircle(bearing, distance), now.plus(Duration.ONE_SECOND), speed, /* optionalTrueHeading */ null));
        trackedRace.onStatusChanged(tdl, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, /* progress */ 1.0));
        assertEquals(distance.getMeters(), trackedRace.getDistanceTraveled(competitor, now.plus(Duration.ONE_SECOND.times(2))).getMeters(), 0.01); // ask 1s after the second fix
    }
}
