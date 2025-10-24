package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveCourse;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class LeaderboardDTOCalculationReuseCacheTest {
    @Test
    public void testNullWindCaching() {
        final TimePoint now = MillisecondsTimePoint.now();
        final LeaderboardDTOCalculationReuseCache cache = new LeaderboardDTOCalculationReuseCache(now);
        final Competitor competitor = mock(Competitor.class);
        @SuppressWarnings("unchecked")
        final GPSFixTrack<Competitor, GPSFixMoving> track = mock(GPSFixTrack.class);
        final Position p = new DegreePosition(123, 12);
        when(track.getEstimatedPosition(now, false)).thenReturn(p);
        final TrackedRace trackedRace = mock(TrackedRace.class);
        when(trackedRace.getTrack(competitor)).thenReturn(track);
        when(trackedRace.getWind(p, now)).thenReturn(null);
        assertNull(cache.getWind(trackedRace, competitor, now));
    }
    
    @Test
    public void testRaceSpecificityOfTotalORCCourseCaching() {
        final TimePoint now = MillisecondsTimePoint.now();
        final LeaderboardDTOCalculationReuseCache cache = new LeaderboardDTOCalculationReuseCache(now);
        final TrackedRace trackedRace1 = mock(TrackedRace.class);
        final ORCPerformanceCurveCourse totalCourseRace1 = mock(ORCPerformanceCurveCourse.class);
        when(totalCourseRace1.getLegs()).thenReturn(Collections.emptySet());
        final TrackedRace trackedRace2 = mock(TrackedRace.class);
        final ORCPerformanceCurveCourse totalCourseRace2 = mock(ORCPerformanceCurveCourse.class);
        when(totalCourseRace2.getLegs()).thenReturn(Collections.emptySet());
        assertSame(totalCourseRace1, cache.getTotalCourse(trackedRace1, ()->totalCourseRace1));
        assertSame(totalCourseRace2, cache.getTotalCourse(trackedRace2, ()->totalCourseRace2));
    }
}
