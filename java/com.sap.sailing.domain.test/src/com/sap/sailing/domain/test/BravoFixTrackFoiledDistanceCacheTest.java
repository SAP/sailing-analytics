package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.sensordata.BravoExtendedSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.BravoExtendedFixImpl;
import com.sap.sailing.domain.common.tracking.impl.DoubleVectorFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.shared.tracking.impl.TimeRangeCache;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.DynamicBravoFixTrack;
import com.sap.sailing.domain.tracking.impl.BravoFixTrackImpl;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * See bug4629. This test reproduces an order of fix insertion into a {@link BravoFixTrack}, cache invalidation,
 * cache value calculation and cache value insertion that with bug 4629 existing will lead to an inconsistent
 * cache entry that should have been invalidated by the fix insertion.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class BravoFixTrackFoiledDistanceCacheTest {
    private DynamicBravoFixTrack<CourseArea> track;
    private DynamicGPSFixMovingTrackImpl<CourseArea> gpsTrack;
    private TimeRangeCacheWithParallelTestSupport<CourseArea> foilingDistanceCache;
    
    /**
     * Supports blocking and releasing calls to {@link #invalidateAllAtOrLaterThan(TimePoint)} and
     * {@link #cache(TimePoint, TimePoint, Object)}, so as to force lock acquisition and release
     * in a specific order.
     * 
     * @author Axel Uhl (d043530)
     *
     * @param <T>
     */
    private static class TimeRangeCacheWithParallelTestSupport<T> extends TimeRangeCache<T> {
        private static Map<String, TimeRangeCacheWithParallelTestSupport<?>> caches = new HashMap<>();
        private int callsToCache;
        private int callsToInvalidateAllAtOrLaterThan;
        private CyclicBarrier cacheBarrier;
        private CyclicBarrier invalidateBarrier;
        private CyclicBarrier cacheInformBarrier;
        
        public TimeRangeCacheWithParallelTestSupport(String nameForLockLogging) {
            super(nameForLockLogging);
            caches.put(nameForLockLogging, this);
        }
        
        static public <T> TimeRangeCacheWithParallelTestSupport<T> getCacheByName(String nameForLockLogging) {
            @SuppressWarnings("unchecked")
            TimeRangeCacheWithParallelTestSupport<T> timeRangeCacheWithParallelTestSupport = (TimeRangeCacheWithParallelTestSupport<T>) caches.get(nameForLockLogging);
            return timeRangeCacheWithParallelTestSupport;
        }

        @Override
        public void invalidateAllAtOrLaterThan(TimePoint timePoint) {
            super.invalidateAllAtOrLaterThan(timePoint);
            callsToInvalidateAllAtOrLaterThan++;
            if (invalidateBarrier != null) {
                try {
                    invalidateBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void cache(TimePoint from, TimePoint to, T result) {
            try {
                if (cacheInformBarrier != null) {
                    cacheInformBarrier.await();
                }
                if (cacheBarrier != null) {
                        cacheBarrier.await();
                }
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            super.cache(from, to, result);
            callsToCache++;
        }

        public int getCallsToCache() {
            return callsToCache;
        }

        public int getCallsToInvalidateAllAtOrLaterThan() {
            return callsToInvalidateAllAtOrLaterThan;
        }

        public void waitForCacheInvalidation() throws InterruptedException, BrokenBarrierException {
            invalidateBarrier.await();
            invalidateBarrier = null;
        }

        public void allowWaitingForCacheInvalidation() {
            invalidateBarrier = new CyclicBarrier(2);
        }

        public void letFoilingDistanceCacheContinueWithCaching() throws InterruptedException, BrokenBarrierException {
            cacheBarrier.await();
            cacheBarrier = null;
        }

        public void letFoilingDistanceCacheWaitBeforeCaching() {
            cacheBarrier = new CyclicBarrier(2);
        }

        public void letFoilingDistanceCacheInformUsBeforeCaching() {
            cacheInformBarrier = new CyclicBarrier(2);
        }
        
        public void waitForCacheToBeEntered() throws InterruptedException, BrokenBarrierException {
            cacheInformBarrier.await();
            cacheInformBarrier = null;
        }
    }
    
    @BeforeEach
    public void setUp() {
        final CourseAreaImpl courseArea = new CourseAreaImpl("Test", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null);
        gpsTrack = new DynamicGPSFixMovingTrackImpl<>(courseArea, /* millisecondsOverWhichToAverage */ 15000);
        track = new BravoFixTrackImpl<CourseArea>(courseArea, "test", /* hasExtendedFixes */ true, gpsTrack) {
            private static final long serialVersionUID = 1473560197177750211L;

            @Override
            protected <T> TimeRangeCache<T> createTimeRangeCache(CourseArea trackedItem, final String cacheName) {
                return new TimeRangeCacheWithParallelTestSupport<>(cacheName);
            }
        };
        track.add(createFix(1000l, /* rideHeightPort */ 0.6, /* rideHeightStarboard */ 0.6, /* heel */ 10., /* pitch */ 5.));
        track.add(createFix(2000l, /* rideHeightPort */ 0.6, /* rideHeightStarboard */ 0.6, /* heel */ 10., /* pitch */ 5.));
        track.add(createFix(3000l, /* rideHeightPort */ 0.6, /* rideHeightStarboard */ 0.6, /* heel */ 10., /* pitch */ 5.));
        gpsTrack.add(createGPSFix(1000l, 0, 0, 0, 1));
        gpsTrack.add(createGPSFix(2000l, 1./3600./60., 0, 0, 1));
        gpsTrack.add(createGPSFix(3000l, 2./3600./60., 0, 0, 1));
        foilingDistanceCache = TimeRangeCacheWithParallelTestSupport.getCacheByName("foilingDistanceCache");
    }
    
    @Test
    public void testDistanceSpentFoiling() throws InterruptedException, ExecutionException, BrokenBarrierException {
        assertEquals(new NauticalMileDistance(2./3600.).getMeters(), track.getDistanceSpentFoiling(t(1000l), t(3000l)).getMeters(), 0.01);
        assertEquals(1, foilingDistanceCache.getCallsToCache());
        assertEquals(6, foilingDistanceCache.getCallsToInvalidateAllAtOrLaterThan()); // the three sensor and three GPS fixes
        assertEquals(new NauticalMileDistance(2./3600.).getMeters(), track.getDistanceSpentFoiling(t(1000l), t(3000l)).getMeters(), 0.01);
        assertEquals(1, foilingDistanceCache.getCallsToCache()); // still the same perfect cache hit, no new cached value
        track.add(createFix(2500l, /* rideHeightPort */ 0.6, /* rideHeightStarboard */ 0.6, /* heel */ 10., /* pitch */ 5.));
        assertEquals(7, foilingDistanceCache.getCallsToInvalidateAllAtOrLaterThan()); // now one more sensor fix
        assertEquals(new NauticalMileDistance(2./3600.).getMeters(), track.getDistanceSpentFoiling(t(1000l), t(3000l)).getMeters(), 0.01);
        assertEquals(2, foilingDistanceCache.getCallsToCache()); // had to be re-calculated and then was expected to be put to cache
        gpsTrack.add(createGPSFix(4000l, 3./3600./60., 0, 0, 1));
        assertEquals(8, foilingDistanceCache.getCallsToInvalidateAllAtOrLaterThan()); // now one more GPS fix
        
        // now modify the cache such that it will stop before updating the cache
        foilingDistanceCache.letFoilingDistanceCacheWaitBeforeCaching();
        foilingDistanceCache.letFoilingDistanceCacheInformUsBeforeCaching();
        FutureTask<Distance> getDistanceFuture = new FutureTask<>(()->track.getDistanceSpentFoiling(t(1000l), t(4000l)));
        new Thread(getDistanceFuture).start();
        foilingDistanceCache.waitForCacheToBeEntered();
        // now insert another sensor fix at t(4000l) that will have to invalidate the result of the previous request;
        // adding the fix will trigger a cache invalidation; the TimeRangeCache.cache(...) call caused by the query above
        // is still blocked:
        FutureTask<Boolean> addFuture = new FutureTask<>(()->track.add(createFix(4000l, /* rideHeightPort */ 0.6, /* rideHeightStarboard */ 0.6, /* heel */ 10., /* pitch */ 5.)));
        foilingDistanceCache.allowWaitingForCacheInvalidation();
        new Thread(addFuture).start();
        // with the fix for bug4629 the cache invalidation won't be reached because fix addition will require the write lock
        // which isn't possible until the cache update has succeeded which now happens under the track's read lock.
        Thread.sleep(500); // to continue to let the old broken version fail more or less reliably by waiting for track.add(...) to reach the invalidation
        // So let the request from above continue with its call to cache(...) which eventually will release the track's read lock...
        foilingDistanceCache.letFoilingDistanceCacheContinueWithCaching();
        // ...so that now the cache invalidation will finally get on its way
        foilingDistanceCache.waitForCacheInvalidation();
        // wait until the caching has completed:
        getDistanceFuture.get();
        // and until adding the fixes has completed
        addFuture.get();
        // Now for the getDistanceFuture, either it has delivered the new value already because it was passed by
        // the addition of the fix, or it delivered the old value, but then the cache entry will have been invalidated.
        // Now ask again; if the invalidation worked correctly, we should get a greater result now:
        assertEquals(new NauticalMileDistance(3./3600.).getMeters(), track.getDistanceSpentFoiling(t(1000l), t(4000l)).getMeters(), 0.01);
    }

    private BravoExtendedFixImpl createFix(long timePointAsMillis, Double rideHeightPort, Double rideHeightStarboard, Double heel, Double pitch) {
        final Double[] fixData = new Double[Collections.max(Arrays.asList(
                BravoExtendedSensorDataMetadata.HEEL.getColumnIndex()+1,
                BravoExtendedSensorDataMetadata.PITCH.getColumnIndex()+1,
                BravoExtendedSensorDataMetadata.RIDE_HEIGHT_PORT_HULL.getColumnIndex()+1,
                BravoExtendedSensorDataMetadata.RIDE_HEIGHT_STBD_HULL.getColumnIndex()+1))];
        fixData[BravoExtendedSensorDataMetadata.HEEL.getColumnIndex()] = heel;
        fixData[BravoExtendedSensorDataMetadata.PITCH.getColumnIndex()] = pitch;
        fixData[BravoExtendedSensorDataMetadata.RIDE_HEIGHT_PORT_HULL.getColumnIndex()] = rideHeightPort;
        fixData[BravoExtendedSensorDataMetadata.RIDE_HEIGHT_STBD_HULL.getColumnIndex()] = rideHeightStarboard;
        return new BravoExtendedFixImpl(new DoubleVectorFixImpl(t(timePointAsMillis), fixData));
    }
    
    private GPSFixMoving createGPSFix(long timePointAsMillis, double lat, double lng, double cogInDeg, double sogInKnots) {
        return new GPSFixMovingImpl(new DegreePosition(lat, lng), new MillisecondsTimePoint(timePointAsMillis),
                new KnotSpeedWithBearingImpl(sogInKnots, new DegreeBearingImpl(cogInDeg)), /* optionalTrueHeading */ null);
    }

    private MillisecondsTimePoint t(long timePointAsMillis) {
        return new MillisecondsTimePoint(timePointAsMillis);
    }
}
