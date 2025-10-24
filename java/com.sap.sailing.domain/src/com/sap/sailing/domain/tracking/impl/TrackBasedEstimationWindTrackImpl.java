package com.sap.sailing.domain.tracking.impl;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.shared.tracking.impl.TrackImpl;
import com.sap.sailing.domain.tracking.DynamicSensorFixTrack;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.AbstractTimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.SerializableComparator;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.shared.util.impl.ArrayListNavigableSet;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * A virtual wind track that computes and caches the wind bearing based on the boat tracks recorded in the tracked race
 * for which this wind track is constructed. It has a fixed time resolution as defined by the constant
 * {@link #RESOLUTION_IN_MILLISECONDS}. When asked for the wind at a time at which the wind cannot be estimated, the raw
 * fixes will have <code>null</code> as value for this time. These <code>null</code> "fixes" are at the same time
 * considered "outliers" by the {@link #getInternalFixes()} operation which filters them from the "smoothened" view.
 * With this, the view with "outliers" removed contains all those fixes for which the wind bearing was successfully
 * computed from the tracked race's boat tracks.
 * <p>
 * 
 * The estimation is integrated into the {@link WindTrackImpl} concepts by redefining the {@link #getInternalRawFixes()}
 * method such that it returns an {@link EstimatedWindFixesAsNavigableSet} object. It computes its values by asking back
 * to {@link #getEstimatedWindDirection(TimePoint)} which first performs a cache look-up. In case of a cache
 * miss it determines the result based on {@link TrackedRace#getEstimatedWindDirection(TimePoint)}.
 * <p>
 * 
 * Caching is done using the base class's {@link TrackImpl#fixesConsideredAffectedByFinder} field which is made accessible through
 * {@link #getCachedFixes()}. This track observes the {@link TrackedRace} for which it provides wind estimations.
 * Whenever a change occurs, all fixes whose derivation is potentially affected by the change are removed from the
 * cache. For new GPS fixes arriving this is the time span used for
 * {@link TrackedRace#getMillisecondsOverWhichToAverageSpeed() averaging speeds}. For a new mark passing, all fixes
 * between the old and new mark passing times as well as those
 * {@link TrackedRace#getMillisecondsOverWhichToAverageSpeed()} before and after this time period are removed from the
 * cache. If the {@link #speedAveragingChanged(long, long) speed averaging changes}, the entire cache is cleared.<p>
 * 
 * Note the {@link #getMillisecondsOverWhichToAverageWind() reduced averaging interval} used by this track type.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class TrackBasedEstimationWindTrackImpl extends VirtualWindTrackImpl {
    private static final long serialVersionUID = -4397496421917807499L;

    private static final SpeedWithBearing defaultSpeedWithBearing = new KnotSpeedWithBearingImpl(0, new DegreeBearingImpl(0));
    
    private final static Duration RESOLUTION = Duration.ONE_SECOND;

    private final EstimatedWindFixesAsNavigableSet virtualInternalRawFixes;

    private final NavigableSet<TimePoint> timePointsWithCachedNullResult;
    
    private final NavigableSet<WindWithConfidence<TimePoint>> cache;
    
    /**
     * Access to {@link #cache} and {@link #timePointsWithCachedNullResult} and
     * {@link #timePointsWithCachedNullResultFastContains} is guarded by this lock.
     */
    private final NamedReentrantReadWriteLock cacheLock;
    
    /**
     * Access to {@link #scheduledRefreshInterval} is guarded by this lock. In particular, when a client
     * holds this lock the client can be sure that {@link InvalidationInterval#isSet()} implies that a
     * cache update is scheduled or already running.
     */
    private final NamedReentrantReadWriteLock scheduledRefreshIntervalLock;
    
    /**
     * A copy of the {@link #timePointsWithCachedNullResult} contents offering fast contains checks.
     */
    private final HashSet<TimePoint> timePointsWithCachedNullResultFastContains;

    /**
     * When mark and boat position changes are received, they cause the cache to be invalidated a certain time interval
     * around the time point of the event. If the cache invalidation happens immediately, this can cause significant
     * load on the server. Delaying the cache refresh just a little will reduce server load, sacrificing some accuracy
     * of the wind estimation which can carefully be traded by this parameter.
     */
    private final long delayForCacheInvalidationInMilliseconds;
    
    private static class InvalidationInterval implements Serializable {
        private static final long serialVersionUID = -6406690520919193690L;
        /**
         * Either both {@link #start} and {@link #end} are <code>null</code>, or both are non-<code>null</code>
         */
        private WindWithConfidence<TimePoint> start;
        /**
         * Either both {@link #start} and {@link #end} are <code>null</code>, or both are non-<code>null</code>
         */
        private TimePoint end;
        public InvalidationInterval() {
            super();
        }
        public WindWithConfidence<TimePoint> getStart() {
            return start;
        }
        public TimePoint getEnd() {
            return end;
        }
        public void clear() {
            start = null;
            end = null;
        }
        public boolean isSet() {
            return start != null && end != null;
        }
        
        /**
         * Checks whether the interval starting at {@link #start} and ending at {@link #end} is fully contained in this
         * interval. Neither of the two arguments is allowed to be <code>null</code>. The interval is contained in this
         * interval if this interval {@link #isSet() is set} and this interval's {@link #getStart() start} is at or
         * before <code>start</code> and this interval's {@link #getEnd() end} is at or after <code>end</code>.
         */
        public boolean contains(TimePoint start, TimePoint end) {
            return isSet() && !getStart().getObject().getTimePoint().after(start) && !end.after(getEnd());
        }
        /**
         * Neither of the two arguments is allowed to be <code>null</code>.
         */
        public void set(WindWithConfidence<TimePoint> startOfInvalidation, TimePoint endOfInvalidation) {
            assert startOfInvalidation != null;
            assert endOfInvalidation != null;
            this.start = startOfInvalidation;
            this.end = endOfInvalidation;
        }
        /**
         * Neither of the two arguments is allowed to be <code>null</code>.
         */
        public void extend(WindWithConfidence<TimePoint> startOfInvalidation, TimePoint endOfInvalidation) {
            assert startOfInvalidation != null;
            assert endOfInvalidation != null;
            if (startOfInvalidation.getObject().getTimePoint().compareTo(start.getObject().getTimePoint()) < 0) {
                this.start = startOfInvalidation;
            }
            if (endOfInvalidation.compareTo(end) > 0) {
                end = endOfInvalidation;
            }
        }
        
        /**
         * Leaves this object unchanged. Returns a new {@link InvalidationInterval} object. If the interval defined by
         * <code>start</code> and <code>end</code> does not exceed this interval, the resulting interval will have
         * {@link #isSet()}<code>==false</code>. Otherwise, the resulting interval will contain all time ranges from
         * <code>start</code> to <code>end</code> (inclusive) that are not in this interval. In particular, if
         * <code>start</code>..<code>end</code> exceeds this interval on both ends, the resulting interval will start at
         * <code>start</code> and end at <code>end</code>. If this interval has {@link #isSet()}<code>==false</code>,
         * the resulting interval will range from <code>start</code> to <code>end</code>.
         * 
         * @param start must not be <code>null</code>
         * @param end must not be <code>null</code>
         */
        public InvalidationInterval subtract(WindWithConfidence<TimePoint> start, TimePoint end) {
            assert start != null;
            assert end != null;
            final InvalidationInterval result = new InvalidationInterval();
            if (!isSet()) {
                result.set(start, end);
            } else {
                final WindWithConfidence<TimePoint> newStart;
                final TimePoint startTimePoint = getStart().getObject().getTimePoint();
                if (start.getObject().getTimePoint().before(startTimePoint)) {
                    newStart = start;
                } else {
                    // don't go beyond the end of time, avoiding overflow
                    newStart = getDummyFixWithConfidence(getEnd().asMillis()==Long.MAX_VALUE?getEnd():getEnd().plus(1));
                }
                final TimePoint newEnd;
                if (end.after(getEnd())) {
                    newEnd = end;
                } else {
                    // avoid underflow
                    newEnd = startTimePoint.asMillis()==0?startTimePoint:startTimePoint.minus(1);
                }
                if (!newStart.getObject().getTimePoint().after(newEnd)) {
                    result.set(newStart, newEnd);
                } // else, the resulting interval remains unset
            }
            return result;
        }
    }
    
    /**
     * {@link #scheduleCacheRefresh(WindWithConfidence, TimePoint)} synchronizes on this object before changing it and
     * when actually invalidating the cache. When a querying thread holds at least the read lock of {@link #cacheLock}
     * and the refresh interval it {@link InvalidationInterval#isSet() is set}, an invalidation is currently running or
     * has been scheduled.
     */
    private final InvalidationInterval scheduledRefreshInterval;

    private final CacheInvalidationRaceChangeListener listener;

    /**
     * @param delayForCacheInvalidationInMilliseconds
     *            When mark and boat position changes are received, they cause the cache to be invalidated a certain
     *            time interval around the time point of the event. If the cache invalidation happens immediately, this
     *            can cause significant load on the server. Delaying the cache refresh just a little will reduce server
     *            load, sacrificing some accuracy of the wind estimation which can carefully be traded by this
     *            parameter. When the delay is set to 0, the cache contents for the affected time interval are
     *            immediately removed and will be re-computed upon the next request. If a positive delay is specified,
     *            the cache contents for the interval affected will be re-computed and will be replaced in the cache
     *            when the new results are available. Clients therefore won't have to wait for the valued to be
     *            re-computed but will be served from the old cache values until they will have been replaced.
     */
    public TrackBasedEstimationWindTrackImpl(TrackedRace trackedRace, long millisecondsOverWhichToAverage,
            double baseConfidence, long delayForCacheInvalidationInMilliseconds) {
        super(trackedRace, millisecondsOverWhichToAverage, baseConfidence,
                WindSourceType.TRACK_BASED_ESTIMATION.useSpeed());
        this.delayForCacheInvalidationInMilliseconds = delayForCacheInvalidationInMilliseconds;
        this.scheduledRefreshInterval = new InvalidationInterval();
        cache = new ArrayListNavigableSet<WindWithConfidence<TimePoint>>(
                new SerializableComparator<WindWithConfidence<TimePoint>>() {
                    private static final long serialVersionUID = 5760349397418542705L;

                    @Override
                    public int compare(WindWithConfidence<TimePoint> o1, WindWithConfidence<TimePoint> o2) {
                        return o1.getObject().getTimePoint().compareTo(o2.getObject().getTimePoint());
                    }
                });
        cacheLock = new NamedReentrantReadWriteLock(TrackBasedEstimationWindTrackImpl.class.getSimpleName()
                + " cacheLock for race " + trackedRace.getRace().getName(), /* fair */false);
        scheduledRefreshIntervalLock = new NamedReentrantReadWriteLock(
                TrackBasedEstimationWindTrackImpl.class.getSimpleName() + " scheduledRefreshIntervalLock for race "
                        + trackedRace.getRace().getName(), /* fair */false);
        virtualInternalRawFixes = new EstimatedWindFixesAsNavigableSet(trackedRace);
        listener = new CacheInvalidationRaceChangeListener();
        trackedRace.addListener(listener); // in particular, race status changes will be notified, unblocking waiting computations after LOADING phase
        this.timePointsWithCachedNullResult = new ArrayListNavigableSet<TimePoint>(
                AbstractTimePoint.TIMEPOINT_COMPARATOR);
        this.timePointsWithCachedNullResultFastContains = new HashSet<TimePoint>();
    }
    
    /**
     * Uses the {@link #cacheLock} and {@link #scheduledRefreshIntervalLock} to make serialization on this object thread
     * safe by avoiding the cache being updated while being written.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        LockUtil.lockForRead(scheduledRefreshIntervalLock);
        LockUtil.lockForRead(cacheLock);
        lockForRead();
        try {
            s.defaultWriteObject();
        } finally {
            unlockAfterRead();
            LockUtil.unlockAfterRead(cacheLock);
            LockUtil.unlockAfterRead(scheduledRefreshIntervalLock);
        }
    }
    
    /**
     * Constructs this track with cache invalidation happening after half the
     * {@link TrackedRace#getMillisecondsOverWhichToAverageWind() wind averaging interval specified by the tracked race}
     * . Good for test cases; shouldn't be use if you don't want to overload the server.
     */
    public TrackBasedEstimationWindTrackImpl(TrackedRace trackedRace, long millisecondsOverWhichToAverage,
            double baseConfidence) {
        this(trackedRace, millisecondsOverWhichToAverage, baseConfidence, /* delayForCacheInvalidationInMilliseconds */
                trackedRace.getMillisecondsOverWhichToAverageWind() / 2);
    }
    
    @Override
    protected EstimatedWindFixesAsNavigableSet getInternalRawFixes() {
        return virtualInternalRawFixes;
    }

    private NavigableSet<WindWithConfidence<TimePoint>> getCachedFixes() {
        return cache;
    }
    
    /**
     * The caller must <em>not</em> hold the read lock of {@link #cacheLock} when calling this method (unless the caller
     * also holds the corresponding write lock) because this method will acquire the corresponding write lock, and
     * upgrading from a read lock to a write lock is not possible.
     */
    protected void cache(TimePoint timePoint, WindWithConfidence<TimePoint> fix) {
        // can't use lockForWrite() here because caching can happen while holding the read lock, and the lock can't be
        // upgraded. But lockForRead() and synchronization will do the job because all invalidations lock the write lock,
        // and all contains() checks and get() calls use synchronization too.
        LockUtil.lockForWrite(cacheLock);
        try {
            if (fix == null) {
                timePointsWithCachedNullResult.add(timePoint);
                timePointsWithCachedNullResultFastContains.add(timePoint);
            } else {
                cache.add(fix);
            }
        } finally {
            LockUtil.unlockAfterWrite(cacheLock);
        }
    }
    
    /**
     * Schedules a cache invalidation for the time interval specified. The scheduling delay is configured during construction of
     * this track. The longer the scheduling delay, the less load this track will cause for the server because invalidations will
     * be bundled, and during live mode the incoming requests for a time point close to the time for which new data is received
     * will not be massively delayed by having to re-calculate the estimation over and over again.
     */
    private void scheduleCacheRefresh(WindWithConfidence<TimePoint> startOfInvalidation, TimePoint endOfInvalidation) {
        final boolean alreadyContained;
        LockUtil.lockForRead(scheduledRefreshIntervalLock);
        try {
            alreadyContained = scheduledRefreshInterval.contains(startOfInvalidation.getObject().getTimePoint(), endOfInvalidation);
        } finally {
            LockUtil.unlockAfterRead(scheduledRefreshIntervalLock);
        }
        if (!alreadyContained) {
            LockUtil.lockForWrite(scheduledRefreshIntervalLock);
            try {
                if (!scheduledRefreshInterval.isSet()) {
                    // according to the invariant this implies [1]==null
                    scheduledRefreshInterval.set(startOfInvalidation, endOfInvalidation);
                    startSchedulerForCacheRefresh();
                } else {
                    // this means that an invalidation or incremental refresh is already scheduled; as long as we're
                    // synchronized on scheduledInvalidationInterval we can safely extend the interval; the invalidation
                    // won't start before we release the lock
                    scheduledRefreshInterval.extend(startOfInvalidation, endOfInvalidation);
                }
            } finally {
                LockUtil.unlockAfterWrite(scheduledRefreshIntervalLock);
            }
        }
    }
    
    /**
     * Invalidates the cache based on {@link #scheduledRefreshInterval} and when done
     * {@link InvalidationInterval#clear() clears} the invalidation interval, indicating that currently no scheduler is
     * running.
     */
    private void invalidateCache() {
        LockUtil.lockForWrite(scheduledRefreshIntervalLock);
        LockUtil.lockForWrite(cacheLock);
        try {
            Iterator<WindWithConfidence<TimePoint>> iter = (scheduledRefreshInterval.getStart() == null ? getCachedFixes()
                    : getCachedFixes().tailSet(scheduledRefreshInterval.getStart(), /* inclusive */true)).iterator();
            while (iter.hasNext()) {
                WindWithConfidence<TimePoint> next = iter.next();
                if (scheduledRefreshInterval.getEnd() == null || next.getObject().getTimePoint().compareTo(scheduledRefreshInterval.getEnd()) < 0) {
                    iter.remove();
                } else {
                    break;
                }
            }
            Iterator<TimePoint> nullIter = (scheduledRefreshInterval.getStart() == null ? timePointsWithCachedNullResult
                    : timePointsWithCachedNullResult.tailSet(scheduledRefreshInterval.getStart().getObject().getTimePoint(), /* inclusive */
                    true)).iterator();
            while (nullIter.hasNext()) {
                TimePoint next = nullIter.next();
                if (scheduledRefreshInterval.getEnd() == null || next.compareTo(scheduledRefreshInterval.getEnd()) < 0) {
                    nullIter.remove();
                    timePointsWithCachedNullResultFastContains.remove(next);
                } else {
                    break;
                }
            }
            scheduledRefreshInterval.clear();
        } finally {
            LockUtil.unlockAfterWrite(cacheLock);
            LockUtil.unlockAfterWrite(scheduledRefreshIntervalLock);
        }
    }

    /**
     * Incrementally replaces the cache elements based on {@link #scheduledRefreshInterval} using freshly computed values.
     */
    private void refreshCacheIncrementally() {
        Set<WindWithConfidence<TimePoint>> windFixesToRecalculate = new HashSet<WindWithConfidence<TimePoint>>();
        Set<TimePoint> cachedNullResultsToRecalculate = new HashSet<TimePoint>();
        final WindWithConfidence<TimePoint> refreshIntervalStart;
        final TimePoint refreshIntervalEnd;
        LockUtil.lockForRead(scheduledRefreshIntervalLock);
        LockUtil.lockForRead(cacheLock);
        try {
            refreshIntervalStart = scheduledRefreshInterval.getStart();
            Iterator<WindWithConfidence<TimePoint>> iter = (refreshIntervalStart == null ? getCachedFixes()
                    : getCachedFixes().tailSet(refreshIntervalStart, /* inclusive */true)).iterator();
            Iterator<TimePoint> nullIter = (refreshIntervalStart == null ? timePointsWithCachedNullResult
                    : timePointsWithCachedNullResult.tailSet(refreshIntervalStart.getObject().getTimePoint(), /* inclusive */
                    true)).iterator();
            WindWithConfidence<TimePoint> nextFixToRecalculate = null;
            refreshIntervalEnd = scheduledRefreshInterval.getEnd();
            while (iter.hasNext() &&
                    ((nextFixToRecalculate = iter.next()).getObject().getTimePoint().compareTo(refreshIntervalEnd) < 0) ||
                    refreshIntervalEnd == null) {
                windFixesToRecalculate.add(nextFixToRecalculate);
            }
            TimePoint nextNullResultToRecalculate = null;
            while (nullIter.hasNext() &&
                    ((nextNullResultToRecalculate = nullIter.next()).compareTo(refreshIntervalEnd) < 0) ||
                    refreshIntervalEnd == null) {
                cachedNullResultsToRecalculate.add(nextNullResultToRecalculate);
            }
        } finally {
            LockUtil.unlockAfterRead(cacheLock);
            LockUtil.unlockAfterRead(scheduledRefreshIntervalLock);
        }
        Set<TimePoint> nullRemovals = new HashSet<TimePoint>();
        Set<TimePoint> nullInsertions = new HashSet<TimePoint>();
        Map<TimePoint, WindWithConfidence<TimePoint>> cacheInsertions = new HashMap<TimePoint, WindWithConfidence<TimePoint>>();
        for (TimePoint cachedNullResultToRecalculate : cachedNullResultsToRecalculate) {
            WindWithConfidence<TimePoint> replacementFix = getTrackedRace()
                    .getEstimatedWindDirectionWithConfidence(cachedNullResultToRecalculate);
            if (replacementFix != null) {
                nullRemovals.add(cachedNullResultToRecalculate);
                cacheInsertions.put(cachedNullResultToRecalculate, replacementFix);
            } // else no action required because the result is still null
        }
        for (WindWithConfidence<TimePoint> windFixToRecalculate : windFixesToRecalculate) {
            TimePoint timePoint = windFixToRecalculate.getObject().getTimePoint();
            WindWithConfidence<TimePoint> replacementFix = getTrackedRace()
                    .getEstimatedWindDirectionWithConfidence(timePoint);
            if (replacementFix == null) {
                nullInsertions.add(timePoint);
            } else {
                cacheInsertions.put(timePoint, replacementFix);
            }
        }
        // apply the computed cache deltas
        LockUtil.lockForWrite(cacheLock);
        try {
            for (TimePoint nullRemoval : nullRemovals) {
                timePointsWithCachedNullResult.remove(nullRemoval);
                timePointsWithCachedNullResultFastContains.remove(nullRemoval);
            }
            for (TimePoint nullInsertion : nullInsertions) {
                cache(nullInsertion, null);
            }
            for (WindWithConfidence<TimePoint> cacheRemoval : windFixesToRecalculate) {
                getCachedFixes().remove(cacheRemoval);
            }
            for (Map.Entry<TimePoint, WindWithConfidence<TimePoint>> cacheInsertion : cacheInsertions.entrySet()) {
                cache(cacheInsertion.getKey(), cacheInsertion.getValue());
            }
        } finally {
            LockUtil.unlockAfterWrite(cacheLock);
        }
        LockUtil.lockForWrite(scheduledRefreshIntervalLock);
        try {
            // now remove the interval for which the cache was refreshed; note that in between the interval may have been extended;
            // for the extended part of the interval that hasn't been refreshed during this run, a new refresh needs to be
            // requested
            InvalidationInterval remainingRefreshInterval = scheduledRefreshInterval.subtract(refreshIntervalStart, refreshIntervalEnd);
            scheduledRefreshInterval.clear();
            if (remainingRefreshInterval.isSet()) {
                scheduleCacheRefresh(remainingRefreshInterval.getStart(), remainingRefreshInterval.getEnd());
            }
        } finally {
            LockUtil.unlockAfterWrite(scheduledRefreshIntervalLock);
        }
    }

    private void startSchedulerForCacheRefresh() {
        if (delayForCacheInvalidationInMilliseconds == 0) {
            invalidateCache();
        } else {
            ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor().
                    schedule(()->{
                        // no locking required here; the incremental cache refresh protects the inner cache structures from concurrent modifications
                        if (getTrackedRace().getStatus().getStatus() == TrackedRaceStatusEnum.LOADING) {
                            // during loading, only invalidate the cache after the interval expired but don't trigger incremental re-calculation
                            invalidateCache();
                        } else {
                            refreshCacheIncrementally();
                        }
                    }, delayForCacheInvalidationInMilliseconds, TimeUnit.MILLISECONDS);
        }
    }

    private void clearCache() {
        LockUtil.lockForWrite(cacheLock);
        try {
            cache.clear();
            timePointsWithCachedNullResult.clear();
            timePointsWithCachedNullResultFastContains.clear();
        } finally {
            LockUtil.unlockAfterWrite(cacheLock);
        }
    }

    /**
     * Looks up wind data in the {@link #getCachedFixes() cache} and the {@link #getTimePointsWithCachedNullResult()
     * null store} first. Only if nothing is found for the time point requested, the
     * {@link TrackedRace#getEstimatedWindDirection(TimePoint) wind estimation algorithm} is used to compute
     * it. The result will then be added to the cache.
     */
    private WindWithConfidence<TimePoint> getEstimatedWindDirection(TimePoint timePoint) {
        WindWithConfidence<TimePoint> cachedFix = null;
        WindWithConfidence<TimePoint> result = null;
        final boolean nullResultCacheContains;
        LockUtil.lockForRead(cacheLock);
        try {
            nullResultCacheContains = nullResultCacheContains(timePoint);
            if (nullResultCacheContains) {
                result = null;
            } else {
                cachedFix = cache.floor(getDummyFixWithConfidence(timePoint));
            }
        } finally {
            LockUtil.unlockAfterRead(cacheLock);
        }
        if (!nullResultCacheContains) {
            if (cachedFix == null || !cachedFix.getObject().getTimePoint().equals(timePoint)) {
                result = getTrackedRace().getEstimatedWindDirectionWithConfidence(timePoint);
                cache(timePoint, result);
            } else {
                result = cachedFix;
            }
        }
        return result;
    }

    private static WindWithConfidence<TimePoint> getDummyFixWithConfidence(TimePoint timePoint) {
        return new WindWithConfidenceImpl<TimePoint>(new WindImpl(null, timePoint, defaultSpeedWithBearing), 0,
                timePoint, /* useSpeed */false);
    }

    private boolean nullResultCacheContains(TimePoint timePoint) {
        assertReadLock();
        LockUtil.lockForRead(cacheLock);
        try {
            return timePointsWithCachedNullResultFastContains.contains(timePoint);
        } finally {
            LockUtil.unlockAfterRead(cacheLock);
        }
    }

    /**
     * The default action of this listener is to call {@link #clearCache} on the wind track. Some changes don't require this
     * and therefore some methods are overridden to do nothing or to react in a more specific way than simply clearing the
     * cache.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    private class CacheInvalidationRaceChangeListener extends AbstractRaceChangeListener implements Serializable {
        private static final long serialVersionUID = -6623310087193133466L;
        
        private boolean suspended;
        
        public CacheInvalidationRaceChangeListener() {
            if (getTrackedRace().getStatus().getStatus() == TrackedRaceStatusEnum.LOADING) {
                suspended = true;
                clearCache();
            } else {
                suspended = false;
            }
        }

        @Override
        protected void defaultAction() {
            clearCache();
        }
        
        @Override
        public void windDataReceived(Wind wind, WindSource windSource) {
            if (!suspended) {
                invalidateForNewWind(wind, windSource);
            }
        }

        @Override
        public void startOfRaceChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
            // no action required; we're calculating based on each competitor's individual start time, not the start of
            // race
        }

        @Override
        public void finishedTimeChanged(TimePoint oldFinishedTime, TimePoint newFinishedTime) {
            // no action required; we're calculating based on each competitor's individual finishing time, not the finishing of
            // the race
        }

        @Override
        public void startTimeReceivedChanged(TimePoint startTimeReceived) {
        }

        @Override
        public void delayToLiveChanged(long delayToLiveInMillis) {
        }

        private void invalidateForNewWind(Wind wind, WindSource windSource) {
            WindTrack windTrack = getTrackedRace().getOrCreateWindTrack(windSource);
            // check what the next fixes before and after the one affected are; if they are further than the
            // averagingInterval away, extend the invalidation interval accordingly because the entire span up
            // to the next fix may be influenced by adding/removing a fix in a sparsely occupied track. See
            // WindTrackImpl.getAveragedWindUnsynchronized(Position p, TimePoint at)
            long averagingInterval = getTrackedRace().getMillisecondsOverWhichToAverageWind();
            final TimePoint timePoint = wind.getTimePoint();
            // See WindComparator; if time is equal, position is compared; for dummy fixes this will create arbitrary
            // order, so ensure time point cannot
            // accidentally be equal
            Wind lastFixBefore = windTrack.getLastFixBefore(timePoint.minus(1)); // subtract one millisecond to be sure
                                                                                 // to be before a fix just inserted
            Wind firstFixAfter = windTrack.getFirstFixAfter(timePoint.plus(1)); // add one millisecond to be sure to be
                                                                                // after a fix just inserted
            final WindWithConfidence<TimePoint> startOfInvalidation;
            if (lastFixBefore == null) {
                startOfInvalidation = getTrackedRace().getStartOfTracking() == null ? getDummyFixWithConfidence(new MillisecondsTimePoint(
                        0l)) : getDummyFixWithConfidence(getTrackedRace().getStartOfTracking());
            } else if (lastFixBefore.getTimePoint().before(timePoint.minus(averagingInterval))) {
                startOfInvalidation = new WindWithConfidenceImpl<TimePoint>(lastFixBefore, 1.0, timePoint, windSource
                        .getType().useSpeed());
            } else {
                startOfInvalidation = getDummyFixWithConfidence(timePoint.minus(averagingInterval));
            }
            final TimePoint endOfInvalidation;
            if (firstFixAfter == null) {
                endOfInvalidation = getTrackedRace().getEndOfTracking() == null ? new MillisecondsTimePoint(
                        Long.MAX_VALUE) : getTrackedRace().getEndOfTracking();
            } else if (firstFixAfter.getTimePoint().after(timePoint.plus(averagingInterval))) {
                endOfInvalidation = firstFixAfter.getTimePoint();
            } else {
                endOfInvalidation = timePoint.plus(averagingInterval);
            }
            scheduleCacheRefresh(startOfInvalidation, endOfInvalidation);
        }

        @Override
        public void windDataRemoved(Wind wind, WindSource windSource) {
            if (!suspended) {
                invalidateForNewWind(wind, windSource);
            }
        }

        @Override
        public void competitorPositionChanged(GPSFixMoving fix, Competitor competitor, AddResult addedOrReplaced) {
            if (!suspended) {
                long averagingInterval = getTrackedRace().getMillisecondsOverWhichToAverageSpeed();
                WindWithConfidence<TimePoint> startOfInvalidation = getDummyFixWithConfidence(new MillisecondsTimePoint(fix
                        .getTimePoint().asMillis() - averagingInterval));
                TimePoint endOfInvalidation = new MillisecondsTimePoint(fix.getTimePoint().asMillis() + averagingInterval);
                scheduleCacheRefresh(startOfInvalidation, endOfInvalidation);
            }
        }

        @Override
        public void statusChanged(TrackedRaceStatus newStatus, TrackedRaceStatus oldStatus) {
            if (oldStatus.getStatus() == TrackedRaceStatusEnum.LOADING) {
                if (newStatus.getStatus() != TrackedRaceStatusEnum.LOADING && newStatus.getStatus() != TrackedRaceStatusEnum.REMOVED) {
                    suspended = false;
                }
            } else if (newStatus.getStatus() == TrackedRaceStatusEnum.LOADING) {
                suspended = true;
                clearCache();
            }
            // This virtual wind track's cache can cope with an empty cache after the LOADING phase and populates the
            // cache upon request. Invalidation during the LOADING phase happens by clearing the entire cache.
        }

        @Override
        public void markPassingReceived(Competitor competitor, Map<Waypoint, MarkPassing> oldMarkPassings,
                Iterable<MarkPassing> markPassings) {
            if (!suspended) {
                long averagingInterval = getTrackedRace().getMillisecondsOverWhichToAverageSpeed();
                WindWithConfidence<TimePoint> startOfInvalidation;
                TimePoint endOfInvalidation;
                for (MarkPassing markPassing : markPassings) {
                    MarkPassing oldMarkPassing = oldMarkPassings.get(markPassing.getWaypoint());
                    if (oldMarkPassing != markPassing) {
                        if (oldMarkPassing == null) {
                            startOfInvalidation = getDummyFixWithConfidence(new MillisecondsTimePoint(markPassing
                                    .getTimePoint().asMillis() - averagingInterval));
                            endOfInvalidation = new MillisecondsTimePoint(markPassing.getTimePoint().asMillis()
                                    + averagingInterval);
                        } else {
                            TimePoint[] interval = new TimePoint[] { oldMarkPassing.getTimePoint(),
                                    markPassing.getTimePoint() };
                            Arrays.sort(interval);
                            startOfInvalidation = getDummyFixWithConfidence(new MillisecondsTimePoint(
                                    interval[0].asMillis() - averagingInterval));
                            endOfInvalidation = new MillisecondsTimePoint(interval[1].asMillis() + averagingInterval);
                        }
                        scheduleCacheRefresh(startOfInvalidation, endOfInvalidation);
                    }
                }
            }
        }

        @Override
        public void markPositionChanged(GPSFix fix, Mark mark, boolean firstInTrack, AddResult addedOrReplaced) {
            assert fix != null && fix.getTimePoint() != null;
            if (!suspended) {
                // A mark position change can mean a leg type change. The interval over which the wind estimation is
                // affected
                // depends on how the GPS track computes the estimated mark position. Ask it:
                TimeRange interval = getTrackedRace().getOrCreateTrack(mark).getEstimatedPositionTimePeriodAffectedBy(fix);
                WindWithConfidence<TimePoint> startOfInvalidation = getDummyFixWithConfidence(interval.from());
                TimePoint endOfInvalidation = interval.to();
                scheduleCacheRefresh(startOfInvalidation, endOfInvalidation);
            }
        }
        
        @Override
        public void competitorSensorFixAdded(Competitor competitor, String trackName, SensorFix fix, AddResult addedOrReplaced) {
            // no action required
        }
        
        @Override
        public void competitorSensorTrackAdded(DynamicSensorFixTrack<Competitor, ?> track) {
            // no action required
        }
    }

    @Override
    public String toString() {
        lockForRead();
        try {
            return "This is the " + this.getClass().getName() + " object from " + virtualInternalRawFixes.getFrom()
                    + " to " + virtualInternalRawFixes.getTo() + " for race " + getTrackedRace();
        } finally {
            unlockAfterRead();
        }
    }
    
    /**
     * Emulates a collection of {@link Wind} fixes for a {@link TrackedRace}, computed using
     * {@link TrackedRace#getEstimatedWindDirection(TimePoint)}. If not constrained
     * by a {@link #from} and/or a {@link #to} time point, an equidistant time field is assumed, starting at
     * {@link TrackedRace#getStart()} and leading up to {@link TrackedRace#getTimePointOfNewestEvent()}. If
     * {@link TrackedRace#getStart()} returns <code>null</code>, {@link Long#MAX_VALUE} is used as the {@link #from}
     * time point, pushing the start to the more or less infinite future ("end of the universe"). If no event was
     * received yet and hence {@link TrackedRace#getTimePointOfNewestEvent()} returns <code>null</code>, the {@link #to}
     * end is assumed to be the beginning of the epoch (1970-01-01T00:00:00).
     * 
     * @author Axel Uhl (d043530)
     * 
     */
    public class EstimatedWindFixesAsNavigableSet extends VirtualWindFixesAsNavigableSet {
        private static final long serialVersionUID = -6902341522276949873L;

        public EstimatedWindFixesAsNavigableSet(TrackedRace trackedRace) {
            this(trackedRace, null, null);
        }
        
        /**
         * @param from expected to be an integer multiple of {@link #getResolutionInMilliseconds()} or <code>null</code>
         * @param to expected to be an integer multiple of {@link #getResolutionInMilliseconds()} or <code>null</code>
         */
        private EstimatedWindFixesAsNavigableSet(TrackedRace trackedRace,
                TimePoint from, TimePoint to) {
            super(TrackBasedEstimationWindTrackImpl.this, trackedRace, from, to, RESOLUTION.asMillis());
        }
        
        protected TrackBasedEstimationWindTrackImpl getTrack() {
            return (TrackBasedEstimationWindTrackImpl) super.getTrack();
        }
        
        @Override
        protected Wind getWind(Position p, TimePoint timePoint) {
            final WindWithConfidence<TimePoint> estimatedWindDirectionWithConfidence = getWindWithConfidence(timePoint);
            return estimatedWindDirectionWithConfidence == null ? null : estimatedWindDirectionWithConfidence.getObject();
        }
        
        protected WindWithConfidence<TimePoint> getWindWithConfidence(TimePoint timePoint) {
            return getTrack().getEstimatedWindDirection(timePoint);
        }

        @Override
        protected NavigableSet<Wind> createSubset(WindTrack track, TrackedRace trackedRace, TimePoint from, TimePoint to) {
            return new EstimatedWindFixesAsNavigableSet(trackedRace, from, to);
        }

    }
    
    private abstract class EstimationIterator implements Iterator<WindWithConfidence<Pair<Position, TimePoint>>> {
        protected TimePoint t;
        private WindWithConfidence<TimePoint> nextEstimatedWindWithTimeBasedConfidence;
        
        EstimationIterator(TimePoint t) {
            this.t = t;
            nextEstimatedWindWithTimeBasedConfidence = advance();
        }
        
        protected abstract WindWithConfidence<TimePoint> advance();

        @Override
        public boolean hasNext() {
            return nextEstimatedWindWithTimeBasedConfidence != null;
        }
        
        protected WindWithConfidence<TimePoint> tryToGetNext() {
            return getEstimatedWindDirection(t);
        }
        
        @Override
        public WindWithConfidence<Pair<Position, TimePoint>> next() {
            if (nextEstimatedWindWithTimeBasedConfidence == null) {
                throw new NoSuchElementException();
            }
            final WindWithConfidence<Pair<Position, TimePoint>> result = createWindWithTimeAndPositionBasedConfidence(nextEstimatedWindWithTimeBasedConfidence);
            nextEstimatedWindWithTimeBasedConfidence = advance();
            return result;
        }
    }
    
    /**
     * Limits the head set to a length of 2*{@link #getMillisecondsOverWhichToAverageWind()} to avoid
     * an expensive search for valid estimation fixes in a huge or maybe even open-ended time range.
     */
    @Override
    protected Iterator<WindWithConfidence<Pair<Position, TimePoint>>> getInternalFixesLimitedHeadSetDescendingIterator(TimePoint endingAt) {
        final TimePoint startingAt = endingAt.minus(2*getMillisecondsOverWhichToAverageWind());
        return new EstimationIterator(getInternalRawFixes().floorToResolution(endingAt)) {
            @Override
            protected WindWithConfidence<TimePoint> advance() {
                WindWithConfidence<TimePoint> next;
                if (t.before(startingAt)) {
                    next = null;
                } else {
                    do {
                        next = tryToGetNext();
                        t = getInternalRawFixes().lowerToResolution(t);
                    } while (next == null && !t.before(startingAt));
                }
                return next;
            }
        };
    }

    /**
     * Limits the tail set to a length of 2*{@link #getMillisecondsOverWhichToAverageWind()} to avoid
     * an expensive search for valid estimation fixes in a huge or maybe even open-ended time range.
     */
    @Override
    protected Iterator<WindWithConfidence<Pair<Position, TimePoint>>> getInternalFixesLimitedTailSetIterator(TimePoint startingAt) {
        final TimePoint endingAt = startingAt.plus(2*getMillisecondsOverWhichToAverageWind());
        return new EstimationIterator(getInternalRawFixes().ceilingToResolution(startingAt)) {
            @Override
            protected WindWithConfidence<TimePoint> advance() {
                WindWithConfidence<TimePoint> next;
                if (t.after(endingAt)) {
                    next = null;
                } else {
                    do {
                        next = tryToGetNext();
                        t = getInternalRawFixes().higherToResolution(t);
                    } while (next == null && !t.after(endingAt));
                }
                return next;
            }
        };
    }

    protected WindWithConfidenceImpl<Pair<Position, TimePoint>> createWindWithTimeAndPositionBasedConfidence(
            final WindWithConfidence<TimePoint> estimatedWindWithTimeBasedConfidence) {
        return new WindWithConfidenceImpl<Pair<Position, TimePoint>>(
                estimatedWindWithTimeBasedConfidence.getObject(), estimatedWindWithTimeBasedConfidence.getConfidence(),
                new Pair<>(estimatedWindWithTimeBasedConfidence.getObject().getPosition(), estimatedWindWithTimeBasedConfidence.getObject().getTimePoint()),
                isUseSpeed());
    }

    /**
     * Forwards the information about the wind fix received to the {@link #listener} which will then adjust this
     * track's cache.
     */
    public void windDataReceived(WindImpl wind, WindSource realWindSource) {
        listener.windDataReceived(wind, realWindSource);
    }

    @Override
    public Duration getResolutionOutsideOfWhichNoFixWillBeReturned() {
        return RESOLUTION;
    }

}
