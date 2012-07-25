package com.sap.sailing.domain.tracking.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.util.impl.ArrayListNavigableSet;
import com.sap.sailing.util.impl.LockUtil;

/**
 * Computing {@link #getDistanceTraveled(TimePoint, TimePoint)} is more expensive the longer the track is and the
 * higher the tracking frequency. It is called regularly, usually for the same start time. Keeping to re-evaluate
 * all distances between all GPS fixes, even if they remain constant, is a waste of resources, particularly when we
 * assume that usually GPS fixes are appended to a track and not usually randomly inserted, even though this can
 * happen.
 * <p>
 * 
 * This cache looks "backwards." It contains pairs whose first component represents a <code>to</code> parameter used
 * in {@link #getDistanceTraveled(TimePoint, TimePoint)}. It is ordered by this component. The second component is a
 * navigable, ordered set of pairs where the first pair component represents a <code>from</code> parameter used in
 * {@link #getDistanceTraveled(TimePoint, TimePoint)} and the second pair component represents the result of
 * {@link #getDistanceTraveled(TimePoint, TimePoint)} for this parameter combination. Note that the cache does only
 * store distances for <code>from</code> time points actually passed to
 * {@link #getDistanceTraveled(TimePoint, TimePoint)} and does not cache all interim combinations collected while
 * computing a result. As such, it differs from a typical "dynamic programming" algorithm which would store all
 * in-between values too. Assuming that <code>from</code> values are sparse (leg start, race start, ...), there
 * doesn't seem to be much use in storing all the other in-between values too.
 * <p>
 * 
 * For implementation efficiency in combination with using a {@link ArrayListNavigableSet} for the values and in
 * order to be able to efficiently extend a cache entry for a single <code>to</code> fix, the navigable sets
 * containing the <code>from</code> fixes and distances are ordered such that earlier fixes come later in the set.
 * This way, extending the cache entry for a <code>to</code> fix to an earlier <code>from</code> fix only requires
 * appending to the set.
 * <p>
 * 
 * <b>Invalidation</b>: When a new fix is added to this track, all distance cache entries for fixes at or later than
 * the new fix's time point are removed from this cache. Additionally, the fix insertion may have an impact on the
 * {@link #getEarlierFixesWhoseValidityMayBeAffected(GPSFix) previous fix's} validity (track smoothing) and
 * therefore on its selection for distance aggregation. Therefore, if fix addition turned the previous fix invalid,
 * the cache entries for the time points at or after the previous fix also need to be removed.
 * <p>
 * 
 * <b>Cache use</b>: When {@link #getDistanceTraveled(TimePoint, TimePoint)} is called, it first looks for a cache
 * entry for the <code>to</code> parameter. If one is found, the earliest entry in the navigable set for the
 * navigable set of <code>from</code> and distance values that is at or after the requested <code>from</code> time
 * point is determined. If such an entry exists, the distance is remembered and the algorithm is repeated
 * recursively, using the <code>from</code> value found in the cache as the new <code>to</code> value, and the
 * <code>from</code> value originally passed to {@link #getDistanceTraveled(TimePoint, TimePoint)} as
 * <code>from</code> again. If no entry is found in the cache entry for <code>to</code> that is at or after the
 * requested <code>from</code> time, the distance is computed by iterating the smoothened fixes.
 * <p>
 * 
 * If a cache entry for <code>to</code> is not found, the latest cache entry before it is looked up. If one is
 * found, the distance between the <code>to</code> time point requested and the <code>to</code> time point found in
 * the cache is computed by iterating the smoothened fixes for this interval. If none is found, the distance is
 * computed by iterating backwards all the way to <code>from</code>.
 * <p>
 * 
 * Once the {@link #getDistanceTraveled(TimePoint, TimePoint)} has computed its value, it adds the result to the
 * cache.
 * 
 * @author Axel Uhl (D043530)
 */
public class DistanceCache {
    private final NavigableSet<Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>>> distanceCache;
    
    private final ReadWriteLock lock;
    
    private static final Comparator<Pair<TimePoint, ?>> timePointInPairComparator = new Comparator<Pair<TimePoint, ?>>() {
        @Override
        public int compare(Pair<TimePoint, ?> o1, Pair<TimePoint, ?> o2) {
            return o1.getA().compareTo(o2.getA());
        }
    };

    public DistanceCache() {
        lock = new ReentrantReadWriteLock(/* fair */ true);
        this.distanceCache = new ArrayListNavigableSet<Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>>>(timePointInPairComparator);
    }
    
    /**
     * Looks up the entry closest to but no later than <code>to</code>. If not found, <code>null</code> is returned. If
     * found, the earliest pair of from/distance that is at or after <code>from</code> will be returned, together with
     * the <code>to</code> value of the entry. If there is no entry that is at or after <code>from</code>,
     * <code>null</code> is returned.
     */
    public Pair<TimePoint, Pair<TimePoint, Distance>> getEarliestFromAndDistanceAtOrAfterFrom(TimePoint from, TimePoint to) {
        LockUtil.lock(lock.readLock());
        try {
            Pair<TimePoint, Pair<TimePoint, Distance>> result = null;
            Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>> entryForTo = distanceCache.ceiling(createDummy(to));
            if (entryForTo != null) {
                result = new Pair<TimePoint, Pair<TimePoint, Distance>>(entryForTo.getA(),
                        entryForTo.getB().floor(new Pair<TimePoint, Distance>(from, null)));
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Removes all cache entries that have a <code>to</code> time point that is at or after <code>timePoint</code>.
     */
    public void invalidateAllAtOrLaterThan(TimePoint timePoint) {
        LockUtil.lock(lock.writeLock());
        try {
            Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>> dummy = createDummy(timePoint);
            NavigableSet<Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>>> toRemove = distanceCache.tailSet(dummy, /* inclusive */ true);
            for (Iterator<Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>>> i=toRemove.iterator(); i.hasNext(); ) {
                Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>> entryToRemove = i.next();
                assert entryToRemove.getA().compareTo(timePoint) >= 0;
                i.remove();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private NavigableSet<Pair<TimePoint, Distance>> getEntryForTo(TimePoint to) {
        NavigableSet<Pair<TimePoint, Distance>> result = null;
        Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>> dummyForTo = createDummy(to);
        Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>> entryForTo = distanceCache.floor(dummyForTo);
        if (entryForTo != null && entryForTo.getA().equals(to)) {
            result = entryForTo.getB();
        }
        return result;
    }
    
    public void cache(TimePoint from, TimePoint to, Distance distance) {
        LockUtil.lock(lock.writeLock());
        try {
            NavigableSet<Pair<TimePoint, Distance>> entryForTo = getEntryForTo(to);
            if (entryForTo == null) {
                entryForTo = new ArrayListNavigableSet<Pair<TimePoint, Distance>>(timePointInPairComparator);
                Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>> pairForTo = new Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>>(
                        to, entryForTo);
                distanceCache.add(pairForTo);
            }
            entryForTo.add(new Pair<TimePoint, Distance>(from, distance));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>> createDummy(TimePoint to) {
        return new Pair<TimePoint, NavigableSet<Pair<TimePoint, Distance>>>(to, null);
    }
}
