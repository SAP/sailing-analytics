package com.sap.sailing.domain.tracking.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;

import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.GPSTrackListener;
import com.sap.sailing.util.impl.ArrayListNavigableSet;
import com.sap.sailing.util.impl.LockUtil;
import com.sap.sailing.util.impl.NamedReentrantReadWriteLock;

/**
 * Re-calculating the maximum speed over a {@link GPSFixTrack} is time consuming. When the track grows the way it
 * usually grows (extending at the end), caching can help to reduce the computational effort. The API for computing the
 * maximum speed (
 * {@link GPSFixTrack#getMaximumSpeedOverGround(com.sap.sailing.domain.common.TimePoint, com.sap.sailing.domain.common.TimePoint)}
 * ) allows clients to specify an interval for which the top speed is to be computed. Therefore, this cache offers a
 * similar API which supports interval-based queries.
 * <p>
 * 
 * The cache assumes that queries are usually posed with one of a small set of "from" time points, such as a leg's start
 * or the race starting time. The "to" time points are expected to vary, particularly to grow in case a "live" query is
 * made, or to represent one of a few more or less fixed time points such as a competitor's leg finishing times. With
 * this assumption it seems reasonable to structure the cache such that the "from" time point is a key into a map that
 * stores results for this "from" value. The various results for the same "from" entry are stored in the navigable set
 * with ascending "to" times. When a query comes in, the best fit is determined by looking up the latest "to" that is
 * earlier than or equal to the requested "to." If such an entry is found, the search for a maximum speed can be
 * restricted to the interval between the cache entry's "to" and the "to" time point requested.
 * <p>
 * 
 * When a new GPS fix is recorded for the track for which this is a max-speed cache, invalidation takes place. For this,
 * the cache {@link GPSTrackListener listens} for GPS fixes being added to the track. For the fix added, we'd like to
 * find the time interval within which a
 * {@link GPSFixTrackImpl#getFixesRelevantForSpeedEstimation(TimePoint, NavigableSet)} with a {@link TimePoint} from
 * that interval delivers the fix added in its result. All overlapping cached intervals will then be cropped to the
 * remaining valid interval if their maximum speed time point was in the remaining interval, or removed from the cache
 * otherwise.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class MaxSpeedCache<ItemType, FixType extends GPSFix> implements GPSTrackListener<ItemType, FixType> {
    private static final long serialVersionUID = 8825205750854940612L;
    
    private final GPSFixTrackImpl<ItemType, FixType> track;
    
    /**
     * Keys are the "from" time points as passed to {@link #getMaxSpeed(TimePoint, TimePoint)}. Values are navigable
     * sets of pairs whose {@link Pair#getA() a} component is the "to" parameter as passed to
     * {@link #getMaxSpeed(TimePoint, TimePoint)}, and whose {@link Pair#getB() b} component is the track's fix where
     * the averaged maximum speed for that interval was achieved together with the averaged speed at that point. The
     * navigable set is ordered according to ascending <code>to</code> time points, yielding shorter cache intervals
     * before longer intervals.
     */
    private final Map<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>> cache;
    
    private final NamedReentrantReadWriteLock lock;
    
    public MaxSpeedCache(GPSFixTrackImpl<ItemType, FixType> track) {
        this.track = track;
        track.addListener(this);
        cache = new HashMap<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>>();
        lock = new NamedReentrantReadWriteLock(MaxSpeedCache.class.getSimpleName()+" for track of "+track.getTrackedItem(), /* fair */ false);
    }

    /**
     * Find the invalidation interval such that getFixesRelevantForSpeedEstimation, when passed any time point from that
     * interval, produces "fix", then find all cache entries that overlap with this interval and if the max fix is
     * outside of the invalidation interval, crop cache entry's interval such that it's overlap-free, otherwise remove.
     */
    @Override
    public void gpsFixReceived(FixType fix, ItemType item) {
        // find the invalidation interval such that getFixesRelevantForSpeedEstimation, when passed any time point from that interval, produces "fix"
        Pair<TimePoint, TimePoint> invalidationInterval = track.getTimeIntervalWhoseEstimatedSpeedMayHaveChangedAfterAddingFix(fix);
        LockUtil.lockForWrite(lock);
        HashMap<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>> additionalCacheEntries =
                new HashMap<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>>();
        try {
            // find all cache entries that overlap with this interval
            for (Iterator<Map.Entry<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>>> i = cache.entrySet().iterator(); i.hasNext(); ) {
                Entry<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>> next = i.next();
                additionalCacheEntries.putAll(invalidateEntriesAndReturnAdditionalCacheEntries(next, invalidationInterval));
                if (next.getValue().isEmpty()) {
                    i.remove();
                }
            }
            mergeAdditionalCacheEntries(additionalCacheEntries);
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }
    }

    /**
     * Returns <code>true</code> in order to skip serialization of the cache as a listener. The cache itself shall not be serialized.
     * It is re-established during the deserialization of the owning track.
     */
    @Override
    public boolean isTransient() {
        return true;
    }

    private void mergeAdditionalCacheEntries(
            Map<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>> additionalCacheEntries) {
        assert lock.writeLock().isHeldByCurrentThread();
        for (Map.Entry<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>> additionalCacheEntry : additionalCacheEntries.entrySet()) {
            NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>> existingEntry = cache.get(additionalCacheEntry.getKey());
            if (existingEntry == null) {
                cache.put(additionalCacheEntry.getKey(), additionalCacheEntry.getValue()); 
            } else {
                existingEntry.addAll(additionalCacheEntry.getValue());
            }
        }
    }

    private Map<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>> invalidateEntriesAndReturnAdditionalCacheEntries(
            Entry<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>> cacheEntry,
            Pair<TimePoint, TimePoint> invalidationInterval) {
        assert lock.writeLock().isHeldByCurrentThread();
        // cannot modify cache here because caller is in an iteration over cache's entry set; request additions by returning them
        Map<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>> result = new HashMap<>();
        if (!cacheEntry.getKey().after(invalidationInterval.getB())) {
            // invalidation can only become necessary if the cache entry doesn't start after the end of the invalidation interval
            TimePoint croppedFrom; // start of the remaining valid part of the cache entries
            if (cacheEntry.getKey().before(invalidationInterval.getA())) {
                croppedFrom = cacheEntry.getKey();
            } else {
                croppedFrom = invalidationInterval.getB(); // cache entry starts in the invalidationInterval; earliest valid point is end of invalidation interval
            }
            // now scan all entries whose "to" is at or after the invalidationInterval's start:
            for (Iterator<Pair<TimePoint, Pair<FixType, Speed>>> toAndResultIter = cacheEntry.getValue().tailSet(
                    new Pair<TimePoint, Pair<FixType, Speed>>(invalidationInterval.getA(), null), /* inclusive */ true).iterator();
                    toAndResultIter.hasNext(); ) {
                Pair<TimePoint, Pair<FixType, Speed>> toAndResult = toAndResultIter.next();
                // cacheEntry's from is before or in the invalidation interval; the current "to" in this loop iteration is in or after the interval
                // and at or after "from"; in any case the record needs to be deleted:
                toAndResultIter.remove();
                // if the current "to" is at or after croppedFrom, check if the old max fix is in the cropped interval; if so, request
                // creation of a new cache entry:
                if (toAndResult.getB() != null) {
                    final TimePoint maxFixTimePoint = toAndResult.getB().getA().getTimePoint();
                    if (!toAndResult.getA().before(croppedFrom) && !maxFixTimePoint.before(croppedFrom)
                            && !maxFixTimePoint.after(toAndResult.getA())) {
                        addEntryToMap(croppedFrom, toAndResult.getA(), toAndResult.getB(), result);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        LockUtil.lockForWrite(lock);
        try {
            cache.clear();
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }
    }

    public Pair<FixType, Speed> getMaxSpeed(TimePoint from, TimePoint to) {
        Pair<FixType, Speed> result = null;
        if (!to.before(from)) {
            result = cacheLookup(from, to);
            if (result == null) {
                result = computeMaxSpeed(from, to);
                cache(from, to, result);
            }
        }
        return result;
    }

    /**
     * If a cache entry exists for "from" whose interval ends at or before <code>to</code>, use it and if necessary compute the
     * maximum in the missing tail, producing another cache entry for both, the tail and the extended interval. Otherwise, <code>null</code>
     * is returned.
     */
    private Pair<FixType, Speed> cacheLookup(TimePoint from, TimePoint to) {
        assert !from.after(to);
        Pair<FixType, Speed> result = null;
        NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>> entry;
        Pair<TimePoint, Pair<FixType, Speed>> entryForLongestSubseries = null;
        LockUtil.lockForRead(lock);
        try {
            entry = cache.get(from);
            if (entry != null) {
                entryForLongestSubseries = entry.floor(new Pair<TimePoint, Pair<FixType, Speed>>(to, null));
            }
        } finally {
            LockUtil.unlockAfterRead(lock);
        }
        if (entryForLongestSubseries != null && entryForLongestSubseries.getB() != null) {
            TimePoint entryTo = entryForLongestSubseries.getA();
            // avoid endless recursion in case entryTo is not after from:
            if (entryTo.equals(to) || entryTo.after(from)) {
                if (entryTo.before(to)) {
                    Pair<FixType, Speed> maxInMissingTail = getMaxSpeed(entryTo, to);
                    if (maxInMissingTail != null && maxInMissingTail.getB().compareTo(entryForLongestSubseries.getB().getB()) > 0) {
                        // the maximum speed is in the tail that was not part of the interval retrieved from the cache
                        result = maxInMissingTail;
                    } else {
                        result = entryForLongestSubseries.getB(); // the interval from the cache also holds the maximum
                                                                  // for the extended interval
                    }
                    cache(from, to, result); // produce new
                } else {
                    result = entryForLongestSubseries.getB();
                }
            }
        }
        return result;
    }

    private void cache(TimePoint from, TimePoint to, Pair<FixType, Speed> fixAtMaxSpeed) {
        LockUtil.lockForWrite(lock);
        try {
            addEntryToMap(from, to, fixAtMaxSpeed, cache);
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }
    }

    private void addEntryToMap(TimePoint from, TimePoint to, Pair<FixType, Speed> fixAtMaxSpeed,
            Map<TimePoint, NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>> map) {
        NavigableSet<Pair<TimePoint, Pair<FixType, Speed>>> setForFrom = map.get(from);
        if (setForFrom == null) {
            setForFrom = new ArrayListNavigableSet<Pair<TimePoint, Pair<FixType, Speed>>>(
                    new Comparator<Pair<TimePoint, Pair<FixType, Speed>>>() {
                        @Override
                        public int compare(Pair<TimePoint, Pair<FixType, Speed>> o1,
                                Pair<TimePoint, Pair<FixType, Speed>> o2) {
                            return o1.getA().compareTo(o2.getA());
                        }
                    });
            map.put(from, setForFrom);
        }
        setForFrom.add(new Pair<TimePoint, Pair<FixType, Speed>>(to, fixAtMaxSpeed));
    }
    
    /**
     * @return <code>null</code>, if no fix exists in the interval specified
     */
    private Pair<FixType, Speed> computeMaxSpeed(TimePoint from, TimePoint to) {
        track.lockForRead();
        try {
            // fetch all fixes on this leg so far and determine their maximum speed
            Iterator<FixType> iter = track.getFixesIterator(from, /* inclusive */ true);
            Speed max = Speed.NULL;
            FixType maxSpeedFix = null;
            if (iter.hasNext()) {
                while (iter.hasNext()) {
                    FixType fix = iter.next();
                    if (fix.getTimePoint().after(to)) {
                        break;
                    }
                    Speed averagedSpeedAtFixTime = track.getEstimatedSpeed(fix.getTimePoint());
                    if (averagedSpeedAtFixTime != null && averagedSpeedAtFixTime.compareTo(max) > 0) {
                        max = averagedSpeedAtFixTime;
                        maxSpeedFix = fix;
                    }
                }
            }
            return maxSpeedFix == null ? null : new Pair<FixType, Speed>(maxSpeedFix, max);
        } finally {
            track.unlockAfterRead();
        }
    }
}
