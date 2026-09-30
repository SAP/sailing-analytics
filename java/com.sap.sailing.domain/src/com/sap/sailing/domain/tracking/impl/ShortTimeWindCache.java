package com.sap.sailing.domain.tracking.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.shared.util.impl.ApproximateTime;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * Caches wind information across a short duration of a few seconds, based on position and time point. A separate timer
 * keeps invalidating records after a configurable duration. When the cache runs empty, the timer is stopped. When new entries
 * appear, the timer is started again.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ShortTimeWindCache {
    private static final Logger logger = Logger.getLogger(ShortTimeWindCache.class.getName());
    private final ConcurrentMap<Triple<Position, TimePoint, Set<WindSource>>,
                                WindWithConfidence<com.sap.sse.common.Util.Pair<Position, TimePoint>>> cache;

    /**
     * The keys of {@link #cache} in the order in which to invalidate them, keyed by the time they were entered into the cache.
     */
    private final ConcurrentLinkedDeque<Pair<Long, Triple<Position, TimePoint, Set<WindSource>>>> order;
    
    /**
     * Creation and removal / cancellation of the timer is synchronized using {@link #order}. If this handle is
     * {@code null}, any task that was previously scheduled by this object has been canceled. Otherwise, an uncanceled
     * {@link CacheInvalidator} task is still scheduled to execute at a fixed rate. It can be cancelled using this
     * future. 
     */
    private ScheduledFuture<?> invalidatorHandle;
    
    private final long preserveHowManyMilliseconds;
    private final TrackedRaceImpl trackedRace;
    
    private long hits;
    private long misses;
    
    private class CacheInvalidator implements Runnable {
        @Override
        public void run() {
            long oldestToKeep = System.currentTimeMillis() - preserveHowManyMilliseconds;
            Pair<Long, Triple<Position, TimePoint, Set<WindSource>>> next;
            while ((next = order.peekFirst()) != null && next.getA() < oldestToKeep) {
                order.pollFirst();
                cache.remove(next.getB());
            }
            synchronized (order) {
                if (order.isEmpty()) {
                    invalidatorHandle.cancel(/* mayInterruptIfRunning */ false);
                    invalidatorHandle = null;
                }
            }
        }
    }
    
    public ShortTimeWindCache(TrackedRaceImpl trackedRace, long preserveHowManyMilliseconds) {
        this.trackedRace = trackedRace;
        this.preserveHowManyMilliseconds = preserveHowManyMilliseconds;
        cache = new ConcurrentHashMap<>();
        order = new ConcurrentLinkedDeque<>();
    }
    
    private void add(Triple<Position, TimePoint, Set<WindSource>> key, WindWithConfidence<com.sap.sse.common.Util.Pair<Position, TimePoint>> wind) {
        cache.put(key, wind);
        synchronized (order) {
            final long timestamp;
            if (preserveHowManyMilliseconds > 1000) {
                timestamp = ApproximateTime.approximateNow().asMillis();
            } else {
                timestamp = System.currentTimeMillis();
            }
            order.add(new Pair<Long, Triple<Position, TimePoint, Set<WindSource>>>(timestamp, key));
            ensureTimerIsRunning();
        }
    }
    
    public void clearCache() {
        cache.clear();
        order.clear();
    }

    public void clearCacheEntriesAtPositionsAndTimePointsWithWindSource(
            List<Pair<Position, TimePoint>> changedWindMeasurements, WindSource windSourceWithChange) {
        Triple<Position, TimePoint, Set<WindSource>> cachedKey = order.peekFirst().getB();
        // Set with excluded wind sources must not include the windSourceWithChange
        if (!cachedKey.getC().contains(windSourceWithChange)
                && changedWindMeasurements.contains(new Pair<>(cachedKey.getA(), cachedKey.getB()))) {
            order.pollFirst();
            cache.remove(cachedKey);
        }
    }

    public void clearCacheEntriesWithWindSource(WindSource windSourceWithChange) {
        Triple<Position, TimePoint, Set<WindSource>> cachedKey = order.peekFirst().getB();
        // Set with excluded wind sources must not include the windSourceWithChange
        if (!cachedKey.getC().contains(windSourceWithChange)) {
            order.pollFirst();
            cache.remove(cachedKey);
        }
    }
    
    protected WindWithConfidence<com.sap.sse.common.Util.Pair<Position, TimePoint>> getWindWithConfidence(Position p,
            TimePoint at, Set<WindSource> windSourcesToExclude) {
        WindWithConfidence<com.sap.sse.common.Util.Pair<Position, TimePoint>> wind;
        final Triple<Position, TimePoint, Set<WindSource>> key = new Triple<Position, TimePoint, Set<WindSource>>(p, at, windSourcesToExclude);
        wind = cache.get(key);
        if (wind == null) {
            misses++;
            wind = trackedRace.getWindWithConfidenceUncached(p, at, windSourcesToExclude);
            if (wind != null) {
                add(key, wind);
            }
        } else {
            hits++;
        }
        if ((hits+misses) % 100000l == 0 && logger.isLoggable(Level.FINE)) {
            logger.fine("hits: " + hits + ", misses: " + misses);
        }
        return wind;
    }
    
    /**
     * Must be called while owning the {@link #order} monitor (synchronized).
     * The task of cleaning the caches is so short and on the other hand so
     * important, especially during server start-up, that it shouldn't have to
     * compete with millions of other background tasks (as they are generated
     * during a system start-up) but rather should execute timely, according
     * to schedule. That's why we schedule them with the default foreground
     * executor.
     */
    private void ensureTimerIsRunning() {
        if (invalidatorHandle == null) {
            invalidatorHandle = ThreadPoolUtil.INSTANCE.getDefaultForegroundTaskThreadPoolExecutor().
                    scheduleAtFixedRate(new CacheInvalidator(), /* delay */ preserveHowManyMilliseconds, preserveHowManyMilliseconds, TimeUnit.MILLISECONDS);
        }
    }

}
