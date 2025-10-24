package com.sap.sailing.domain.markpassingcalculation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.concurrent.ConcurrentWeakHashMap;

/**
 * A cache for approximate waypoint positions that has a configurable {@link TimeRange} resolution. Requests that fall
 * into the same time range will be responded to using the previous response for the middle of that time range. Time
 * ranges are arranged not to overlap for the same waypoint by putting the center of each time range to a whole multiple
 * of the resolution, starting at Unix-time 0.
 * <p>
 * 
 * The cache is weak for the {@link Waypoint} objects, so if a course change eliminates a waypoint, this cache will
 * eventually let go of the position records for those waypoints.
 * <p>
 * 
 * The cache listens for mark fixes, indicating new knowledge about a mark's position. All waypoints referencing this
 * mark will have their cache entries removed for those time ranges that have an overlap with the time range affected by
 * the new mark fix.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class WaypointPositionAndDistanceCache {
    /**
     * Weak keys are the waypoints; values are time-ordered maps mapping the center of a time range
     * to the approximate waypoint position at that time.
     */
    private final ConcurrentWeakHashMap<ControlPoint, SortedMap<TimePoint, Position>> waypointPositionCache;
    
    /**
     * Weak keys are the marks; values are time-ordered maps mapping the center of a time range
     * to the mark position at that time.
     */
    private final ConcurrentWeakHashMap<Mark, SortedMap<TimePoint, Position>> markPositionCache;
    
    /**
     * Caches distances between the approximate positions cached in {@link #waypointPositionCache}. For two-mark
     * control points those are the positions in the middle between the two marks. As soon as an entry
     * is invalidated in {@link #waypointPositionCache}, it is also invalidated here. Keys always occur
     * symmetrically under the map's monitor being held, i.e., if (w1, w2) is a cache key then so is
     * (w2, w1), with equal value.
     */
    private final ConcurrentMap<Pair<ControlPoint, ControlPoint>, SortedMap<TimePoint, Distance>> distanceCache;
    
    /**
     * Caches minimum distances between the positions cached in {@link #waypointPositionCache}. For two-mark control
     * points those distances are the shortest distance between any two mark pairs from the two waypoints. Note that
     * this is not the absolute minimum distance to be sailed, e.g., if the two waypoints are lines, where their closest
     * approximation point may lie somewhere on the middle of at least one line. As soon as an entry is invalidated in
     * {@link #waypointPositionCache}, it is also invalidated here. Keys always occur symmetrically under the map's
     * monitor being held, i.e., if (w1, w2) is a cache key then so is (w2, w1), with equal value.
     */
    private final ConcurrentMap<Pair<ControlPoint, ControlPoint>, SortedMap<TimePoint, Distance>> minimumDistanceCache;
    
    /**
     * The duration of the time ranges whose center time points serve as keys for the {@link NavigableMap}s used as
     * values in the {@link #waypointPositionCache} structure. Each cache entry extends half this duration into the past and into the
     * future, or more formally: if <code>t</code> is the time point marking the center of the time range, the time
     * range for the cache entry is defined as <code>[t-timeRangeResolution/2, t+timeRangeResolution/2]</code> if the
     * millisecond representation of this duration is even, and
     * <code>[t-timeRangeResolution/2-1, t+timeRangeResolution/2]</code> for odd durations.
     */
    private final Duration timeRangeResolution;

    private final TrackedRace trackedRace;
    
    /**
     * A synchronized list
     */
    private final List<Waypoint> waypoints;
    
    public WaypointPositionAndDistanceCache(TrackedRace race, Duration timeRangeResolution) {
        this.trackedRace = race;
        this.waypointPositionCache = new ConcurrentWeakHashMap<>();
        this.markPositionCache = new ConcurrentWeakHashMap<>();
        this.distanceCache = new ConcurrentHashMap<>();
        this.minimumDistanceCache = new ConcurrentHashMap<>();
        this.timeRangeResolution = timeRangeResolution;
        this.waypoints = Collections.synchronizedList(new ArrayList<>());
        final Course course = this.trackedRace.getRace().getCourse();
        course.lockForRead(); // obtain waypoints list copy and register listener atomically so as to not miss any updates
        try {
            Util.addAll(course.getWaypoints(), this.waypoints);
            this.trackedRace.addListener(new AbstractRaceChangeListener() {
                @Override
                public void markPositionChanged(GPSFix fix, Mark mark, boolean firstInTrack, AddResult addedOrReplaced) {
                    invalidate(mark, fix);
                }

                @Override
                public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
                    waypoints.add(zeroBasedIndex, waypointThatGotAdded);
                }

                @Override
                public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
                    assert waypoints.get(zeroBasedIndex) == waypointThatGotRemoved;
                    waypoints.remove(zeroBasedIndex);
                }
            });
        } finally {
            course.unlockAfterRead();
        }
    }
    
    public Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint) {
        return getApproximateResult(waypoint.getControlPoint(), timePoint, waypointPositionCache,
                roundedToTimeRangeCenter->trackedRace.getApproximatePosition(waypoint, roundedToTimeRangeCenter),
                /* alternateKeySupplier */ null);
    }
    
    public Position getApproximatePosition(Mark mark, TimePoint timePoint) {
        return getApproximateResult(mark, timePoint, markPositionCache,
                roundedToTimeRangeCenter->trackedRace.getOrCreateTrack(mark).getEstimatedPosition(roundedToTimeRangeCenter, /* extrapolate */ false),
                /* alternateKeySupplier */ null);
    }
    
    /**
     * @param resultCalculator
     *            function that computes the result to be cached, assuming the caller knows the key which it just passed
     *            to this method
     * @param alternateKeySupplier
     *            if a cache entry was computed using <code>resultCalculator</code>, it can optionally be stored under
     *            the additional key supplied by this object. If the supplier is <code>null</code> or supplies <code>null</code>,
     *            no additional cache entry except one for the standard <code>key</code> will be produced
     */
    private <K, R> R getApproximateResult(K cacheKey, TimePoint timePoint, Map<K, SortedMap<TimePoint, R>> cacheMap,
            Function<TimePoint, R> resultCalculator, Supplier<K> alternateKeySupplier) {
        SortedMap<TimePoint, R> map = cacheMap.get(cacheKey);
        final R cachedResult;
        final TimePoint roundedToTimeRangeCenter = roundToResolution(timePoint);
        if (map == null) {
            map = Collections.synchronizedSortedMap(new TreeMap<TimePoint, R>());
            cacheMap.put(cacheKey, map);
            cachedResult = null;
        } else {
            cachedResult = map.get(roundedToTimeRangeCenter);
        }
        final R result;
        if (cachedResult == null) {
            result = computeResult(resultCalculator, roundedToTimeRangeCenter);
            map.put(roundedToTimeRangeCenter, result);
            K alternateKey;
            if (alternateKeySupplier != null && (alternateKey=alternateKeySupplier.get()) != null) {
                SortedMap<TimePoint, R> mapForAlternateKey = cacheMap.get(alternateKey);
                if (mapForAlternateKey == null) {
                    mapForAlternateKey = Collections.synchronizedSortedMap(new TreeMap<TimePoint, R>());
                    cacheMap.put(alternateKey, mapForAlternateKey);
                }
                mapForAlternateKey.put(roundedToTimeRangeCenter, result);
            }
        } else {
            result = cachedResult;
        }
        return result;
    }

    /**
     * This trivial method exists for the sake of testability. A test can subclass and redefine to easily intercept and
     * take note that a re-calculation actually took place.
     */
    protected <R> R computeResult(Function<TimePoint, R> resultCalculator, final TimePoint roundedToTimeRangeCenter) {
        return resultCalculator.apply(roundedToTimeRangeCenter);
    }
    
    public Distance getApproximateDistance(Waypoint w1, Waypoint w2, TimePoint timePoint) {
        return getApproximateResult(new Pair<>(w1.getControlPoint(), w2.getControlPoint()), timePoint, distanceCache,
                roundedToTimeRangeCenter->getApproximatePosition(w1, roundedToTimeRangeCenter).getDistance(
                        getApproximatePosition(w2, roundedToTimeRangeCenter)),
                        /* alternate key supplier */ ()->new Pair<>(w2.getControlPoint(), w1.getControlPoint()));
    }
    
    public Distance getMinimumDistance(Waypoint w1, Waypoint w2, TimePoint timePoint) {
        return getApproximateResult(new Pair<>(w1.getControlPoint(), w2.getControlPoint()), timePoint, minimumDistanceCache,
                roundedToTimeRangeCenter->{
                    Distance minDist = null;
                    for (final Mark w1m : w1.getMarks()) {
                        for (final Mark w2m : w2.getMarks()) {
                            final Position approximatePositionW1m = getApproximatePosition(w1m, roundedToTimeRangeCenter);
                            final Position approximatePositionW2m = getApproximatePosition(w2m, roundedToTimeRangeCenter);
                            if (approximatePositionW1m != null && approximatePositionW2m != null) {
                                final Distance w1mToW2m = approximatePositionW1m.getDistance(approximatePositionW2m);
                                if (minDist == null || minDist.compareTo(w1mToW2m) > 0) {
                                    minDist = w1mToW2m;
                                }
                            }
                        }
                    }
                    return minDist;
                },
                /* alternate key supplier */ ()->new Pair<>(w2.getControlPoint(), w1.getControlPoint()));
    }
    
    /**
     * Invalidates those elements of the cache affected by the position fix for the mark. This considers for which time
     * range the {@link GPSFixTrack#getEstimatedPosition(TimePoint, boolean)} result will be affected by the fix and checks
     * which cache entries overlap. It invalidates the cache entries for those waypoints using the <code>mark</code>.
     */
    private void invalidate(Mark mark, GPSFix fix) {
        TimeRange affectedTimeRange = trackedRace.getOrCreateTrack(mark).getEstimatedPositionTimePeriodAffectedBy(fix);
        final SortedMap<TimePoint, Position> map = markPositionCache.get(mark);
        if (map != null) {
            removeAffectedEntries(mark, affectedTimeRange, map, /* entryRemovedCallback */ null);
        }
        synchronized (waypoints) { // it's a synchronized list, so grabbing the list's monitor will grant us exclusive access here
            for (Waypoint waypoint : waypoints) {
                if (Util.contains(waypoint.getMarks(), mark)) {
                    invalidate(waypoint.getControlPoint(), affectedTimeRange);
                }
            }
        }
    }

    private void invalidate(final ControlPoint controlPoint, TimeRange affectedTimeRange) {
        final SortedMap<TimePoint, Position> map = waypointPositionCache.get(controlPoint);
        if (map != null) {
            removeAffectedEntries(controlPoint, affectedTimeRange, map, this::controlPointCacheMapEntryRemoved);
        }
    }

    private <T> void removeAffectedEntries(T controlPoint, TimeRange affectedTimeRange,
            SortedMap<TimePoint, Position> map, EntryRemovedCallback<T> entryRemovedCallback) {
        final SortedMap<TimePoint, Position> tailMap = map.tailMap(roundToResolution(affectedTimeRange.from()));
        synchronized (map) {
            for (Iterator<Entry<TimePoint, Position>> i=tailMap.entrySet().iterator(); i.hasNext(); ) {
                final Entry<TimePoint, Position> e = i.next();
                final TimePoint timePoint = e.getKey();
                if (timePoint.after(affectedTimeRange.to())) {
                    break;
                }
                assert timePoint.equals(roundToResolution(timePoint));
                i.remove();
                if (entryRemovedCallback != null) {
                    entryRemovedCallback.cacheMapEntryRemoved(controlPoint, timePoint);
                }
            }
        }
    }
    
    @FunctionalInterface
    private interface EntryRemovedCallback<T> {
        void cacheMapEntryRemoved(T controlPoint, final TimePoint timePoint);
    }
    
    private void controlPointCacheMapEntryRemoved(ControlPoint controlPoint, final TimePoint timePoint) {
        synchronized (waypoints) {
            for (Waypoint otherWaypoint : waypoints) {
                ControlPoint otherControlPoint = otherWaypoint.getControlPoint();
                if (otherControlPoint != controlPoint) {
                    for (ConcurrentMap<Pair<ControlPoint, ControlPoint>, SortedMap<TimePoint, Distance>> cache : Arrays.asList(distanceCache, minimumDistanceCache)) {
                        final Map<TimePoint, Distance> distanceMap = cache.get(new Pair<>(controlPoint, otherControlPoint));
                        if (distanceMap != null) {
                            distanceMap.remove(timePoint);
                        }
                        final Map<TimePoint, Distance> otherDistanceMap = cache.get(new Pair<>(otherControlPoint, controlPoint));
                        if (otherDistanceMap != null) {
                            otherDistanceMap.remove(timePoint);
                        }
                    }
                }
            }
        }
    }

    /**
     * Rounds the <code>timePoint</code> to the next time range center which is then a valid key for a {@link #waypointPositionCache} value.<p>
     * 
     * Default scope for testability in fragment.
     */
    TimePoint roundToResolution(TimePoint timePoint) {
        long mod = timePoint.asMillis() % timeRangeResolution.asMillis();
        long div = timePoint.asMillis() / timeRangeResolution.asMillis();
        // example with odd resolution 3ms; Intervals [2, 4], [5, 7], ... 
        // example with even resolution 4ms; Intervals [2, 5], [6, 9], ...
        final long roundedMillis;
        if (mod <= (timeRangeResolution.asMillis()-1)/2) {
            roundedMillis = div * timeRangeResolution.asMillis();
        } else {
            roundedMillis = (div+1) * timeRangeResolution.asMillis();
        }
        return new MillisecondsTimePoint(roundedMillis);
    }
}
