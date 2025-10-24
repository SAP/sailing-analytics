package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.ShortTimeWindCache;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * See also bug 3195
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ShortTimeWindCacheTest {
    private static final long CACHE_ENTRY_LIFETIME_IN_MILLIS = /* preserveHowManyMilliseconds */ 100;

    public static class MyShortTimeWindCache extends ShortTimeWindCache {
        public MyShortTimeWindCache(TrackedRaceImpl trackedRace, long preserveHowManyMilliseconds) {
            super(trackedRace, preserveHowManyMilliseconds);
        }
        
        public WindWithConfidence<com.sap.sse.common.Util.Pair<Position, TimePoint>> getWindWithConfidence(Position p,
                TimePoint at, Set<WindSource> windSourcesToExclude) {
            return super.getWindWithConfidence(p, at, windSourcesToExclude);
        }
    }
    
    private MyShortTimeWindCache cache;
    private TrackedRaceImpl trackedRace;
    private TimePoint now;
    private WindWithConfidence<Pair<Position, TimePoint>> result;
    
    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() {
        now = MillisecondsTimePoint.now();
        trackedRace = mock(TrackedRaceImpl.class);
        cache = new MyShortTimeWindCache(trackedRace, CACHE_ENTRY_LIFETIME_IN_MILLIS);
        result = new WindWithConfidenceImpl<Pair<Position, TimePoint>>(
                new WindImpl(new DegreePosition(54, 8), now, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(123))), 0.123, new Pair<>(null, now), /* useSpeed */true);
        final RaceDefinition race = mock(RaceDefinition.class);
        when(race.getName()).thenReturn("Mocked Test Race");
        when(trackedRace.getRace()).thenReturn(race);
        when(trackedRace.getWindWithConfidenceUncached(/* position */ any(), same(now), (Iterable<WindSource>) isNull())).thenAnswer(new Answer<WindWithConfidence<Pair<Position, TimePoint>>>() {
            @Override
            public WindWithConfidence<Pair<Position, TimePoint>> answer(InvocationOnMock invocation) throws Throwable {
                return result;
            }
        });
    }
    
    @Test
    public void simpleSetupTest() throws InterruptedException {
        final Position p1 = null;
        final WindWithConfidence<Pair<Position, TimePoint>> firstResult = cache.getWindWithConfidence(p1, now, /* windSourcesToExclude */ null);
        assertSame(result, firstResult);
        Thread.sleep(CACHE_ENTRY_LIFETIME_IN_MILLIS/10); // wait a bit to make sure that the next cache entry will certainly not be removed by the timer removing the first entry
        final Position p2 = new DegreePosition(54, 8);
        final WindWithConfidence<Pair<Position, TimePoint>> secondResult = cache.getWindWithConfidence(p2, now, /* windSourcesToExclude */ null);
        assertSame(result, secondResult);
        Thread.sleep(CACHE_ENTRY_LIFETIME_IN_MILLIS/10); // wait a bit to make sure that the next cache entry will certainly not be removed by the timer removing the first entry
        final Position p3 = new DegreePosition(55, 9);
        final WindWithConfidence<Pair<Position, TimePoint>> thirdResult = cache.getWindWithConfidence(p3, now, /* windSourcesToExclude */ null);
        assertSame(result, thirdResult);
        final WindWithConfidence<Pair<Position, TimePoint>> oldResult = result;
        result = null; // this is the new response of trackedRace.getWindWithConfidenceUncached
        // assert that the trackedRace now responds with null
        assertNull(trackedRace.getWindWithConfidenceUncached(/* position */ null, now, /* windSourcesToExclude */ null));
        // assert that the cache still holds the cached values:
        final WindWithConfidence<Pair<Position, TimePoint>> firstResult_2 = cache.getWindWithConfidence(p1, now, /* windSourcesToExclude */ null);
        assertSame(oldResult, firstResult_2);
        final WindWithConfidence<Pair<Position, TimePoint>> secondResult_2 = cache.getWindWithConfidence(p2, now, /* windSourcesToExclude */ null);
        assertSame(oldResult, secondResult_2);
        final WindWithConfidence<Pair<Position, TimePoint>> thirdResult_2 = cache.getWindWithConfidence(p3, now, /* windSourcesToExclude */ null);
        assertSame(oldResult, thirdResult_2);
        // now wait for the cache entries to be expired
        Thread.sleep(CACHE_ENTRY_LIFETIME_IN_MILLIS+1000l);
        final WindWithConfidence<Pair<Position, TimePoint>> firstResult_3 = cache.getWindWithConfidence(p1, now, /* windSourcesToExclude */ null);
        assertNull(firstResult_3); // this one even works with bug3195 unfixed because the first element is removed properly from "order" and "cache"
        final WindWithConfidence<Pair<Position, TimePoint>> secondResult_3 = cache.getWindWithConfidence(p2, now, /* windSourcesToExclude */ null);
        assertNull(secondResult_3); // this one fails with bug3195 unfixed because the entry was removed while "peeking" into "order" using a pollFirst() call
                                    // but the entry remained in "cache"
        final WindWithConfidence<Pair<Position, TimePoint>> thirdResult_3 = cache.getWindWithConfidence(p3, now, /* windSourcesToExclude */ null);
        assertNull(thirdResult_3);
    }
}
