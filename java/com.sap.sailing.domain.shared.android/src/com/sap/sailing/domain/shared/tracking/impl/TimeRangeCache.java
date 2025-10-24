package com.sap.sailing.domain.shared.tracking.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.NavigableSet;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.shared.util.impl.ArrayListNavigableSet;

/**
 * This cache looks "backwards." It contains pairs whose first component represents a <code>to</code> parameter used in
 * a calculation for a time range. It is ordered by this component. The second component is a navigable, ordered set of
 * pairs where the first pair component represents a <code>from</code> parameter used in the calculation's time range
 * and the second pair component represents the result of the calculation for this parameter combination.
 * <p>
 * 
 * For implementation efficiency in combination with using an {@link ArrayListNavigableSet} for the values and in order
 * to be able to efficiently extend a cache entry for a single <code>to</code> fix, the navigable sets containing the
 * <code>from</code> fixes and results are ordered such that earlier fixes come later in the set. This way, extending
 * the cache entry for a <code>to</code> fix to an earlier <code>from</code> fix only requires appending to the set.
 * <p>
 * 
 * <b>Invalidation</b>: When a new fix is added to the track, all cache entries for fixes at or later than the new fix's
 * time point are removed from this cache. Additionally, the fix insertion may have an impact on the
 * {@link #getEarliestFromAndResultAtOrAfterFrom(TimePoint, TimePoint) previous fix's} validity (track smoothing) and
 * therefore on its selection for result aggregation. Therefore, if fix addition turned the previous fix invalid, the
 * cache entries for the time points at or after the previous fix also need to be removed.
 * <p>
 * 
 * <b>Cache use</b>: When a result across a time range is to be computed the calculating method should first look for a
 * cache entry for the <code>to</code> parameter. If one is found, the earliest entry in the navigable set for the
 * navigable set of <code>from</code> and result values that is at or after the requested <code>from</code> time point
 * is determined. If such an entry exists, the result is remembered and the algorithm is repeated recursively, using the
 * <code>from</code> value found in the cache as the new <code>to</code> value, and the <code>from</code> value
 * originally passed to the calculating method as <code>from</code> again. If no entry is found in the cache entry for
 * <code>to</code> that is at or after the requested <code>from</code> time, the result has to be computed "from
 * scratch."
 * <p>
 * 
 * If a cache entry for <code>to</code> is not found, the latest cache entry before it is looked up. If one is found,
 * the result for the time range between the <code>to</code> time point requested and the <code>to</code> time point
 * found in the cache is computed by iterating the smoothened fixes for this interval. If none is found, the result is
 * computed by iterating backwards all the way to <code>from</code>.
 * <p>
 * 
 * Once the calculating method has computed its value, it should {@link #cache(TimePoint, TimePoint, Object) add} the
 * result to the cache.
 * 
 * @author Axel Uhl (D043530)
 */
public class TimeRangeCache<T> {
    public static final int MAX_SIZE = 100;
    
    private final NavigableSet<Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>>> timeRangeCache;
    
    /**
     * The cache is to have limited size. Eviction shall happen based on a least-recently-used strategy. Usage is
     * defined as having been returned by {@link #getEarliestFromAndResultAtOrAfterFrom(TimePoint, TimePoint)} or
     * having been added by {@link #cache(TimePoint, TimePoint, Object)}.
     * <p>
     * 
     * When an eldest entry is asked to be expunged from this map and the map has more than {@link #MAX_SIZE} elements,
     * the expunging will be admitted, and the entry is removed from the {@link #timeRangeCache} core structure. Reading
     * and writing this structure must happen under the {@link #lock write lock} because also reading the linked hash
     * map that counts access as "use" has a modifying effect on its internal structures.
     * <p>
     * 
     * The key pairs are from/to pairs. Note that this is in some sense "the opposite direction" compared to the
     * alignment of the {@link #timeRangeCache} structure which has as its outer keys the "to" time point.<p>
     * 
     * Read access is to be <code>synchronized<code> using this field's mutex; write access only happens under the
     * {@link #lock write lock} and therefore will have no contenders.
     */
    private final LinkedHashMap<Util.Pair<TimePoint, TimePoint>, Void> lruCache;
    
    private final NamedReentrantReadWriteLock lock;
    
    private static final Comparator<Util.Pair<TimePoint, ?>> timePointInPairComparator = new Comparator<Util.Pair<TimePoint, ?>>() {
        @Override
        public int compare(Util.Pair<TimePoint, ?> o1, Util.Pair<TimePoint, ?> o2) {
            return o1.getA().compareTo(o2.getA());
        }
    };

    public TimeRangeCache(String nameForLockLogging) {
        lock = new NamedReentrantReadWriteLock("lock for TimeRangeCache for "+nameForLockLogging, /* fair */ true);
        this.timeRangeCache = new ArrayListNavigableSet<Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>>>(timePointInPairComparator);
        this.lruCache = new LinkedHashMap<Util.Pair<TimePoint, TimePoint>, Void>(/* initial capacity */ 10, /* load factor */ 0.75f,
                /* access-based ordering */ true) {
            private static final long serialVersionUID = -6568235517111733193L;

            @Override
            protected boolean removeEldestEntry(Entry<Pair<TimePoint, TimePoint>, Void> eldest) {
                final boolean expunge = size() > MAX_SIZE;
                if (expunge) {
                    removeCacheEntry(eldest.getKey().getA(), eldest.getKey().getB());
                }
                return expunge;
            }
        };
    }
    
    public int size() {
        return lruCache.size();
    }
    
    private void removeCacheEntry(TimePoint from, TimePoint to) {
        assert lock.getWriteHoldCount() == 1; // we can be sure we are alone here; this only happens when adding a new entry, holding the write lock
        Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>> entryForTo = timeRangeCache.floor(createDummy(to));
        if (entryForTo.getA().equals(to)) {
            Pair<TimePoint, T> entryForFrom = entryForTo.getB().ceiling(new Util.Pair<TimePoint, T>(from, null));
            if (entryForFrom.getA().equals(from)) {
                entryForTo.getB().remove(entryForFrom);
                if (entryForTo.getB().isEmpty()) {
                    timeRangeCache.remove(entryForTo);
                }
            }
        }
    }
    
    /**
     * Looks up the entry closest to but no later than <code>to</code>. If not found, <code>null</code> is returned. If
     * found, the earliest pair of from/result that is at or after <code>from</code> will be returned, together with
     * the <code>to</code> value of the entry. If there is no entry that is at or after <code>from</code>,
     * <code>null</code> is returned.
     */
    public Util.Pair<TimePoint, Util.Pair<TimePoint, T>> getEarliestFromAndResultAtOrAfterFrom(TimePoint from, TimePoint to) {
        LockUtil.lockForRead(lock);
        try {
            Util.Pair<TimePoint, Util.Pair<TimePoint, T>> result = null;
            Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>> entryForTo = timeRangeCache.floor(createDummy(to));
            if (entryForTo != null) {
                final Util.Pair<TimePoint, T> fromCeiling = entryForTo.getB().ceiling(new Util.Pair<TimePoint, T>(from, null));
                if (fromCeiling != null) {
                    result = new Util.Pair<TimePoint, Util.Pair<TimePoint, T>>(entryForTo.getA(), fromCeiling);
                }
            }
            // no writer can be active because we're holding the read lock; read access on the lruCache is synchronized using
            // the lruCache's mutex; this is necessary because we're using access-based LRU pinging where even getting an entry
            // modifies the internal parts of the data structure which is not thread safe.
            synchronized (lruCache) { // ping the "perfect match" although it may not even have existed in the cache
                lruCache.get(new Util.Pair<TimePoint, TimePoint>(from, to));
            }
            return result;
        } finally {
            LockUtil.unlockAfterRead(lock);
        }
    }
    
    /**
     * Removes all cache entries that have a <code>to</code> time point that is at or after <code>timePoint</code>.
     */
    public void invalidateAllAtOrLaterThan(TimePoint timePoint) {
        LockUtil.lockForWrite(lock);
        try {
            Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>> dummy = createDummy(timePoint);
            NavigableSet<Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>>> toRemove = timeRangeCache.tailSet(dummy, /* inclusive */ true);
            for (Iterator<Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>>> i=toRemove.iterator(); i.hasNext(); ) {
                Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>> entryToRemove = i.next();
                assert entryToRemove.getA().compareTo(timePoint) >= 0;
                for (Pair<TimePoint, T> fromAndResult : entryToRemove.getB()) {
                    lruCache.remove(new Util.Pair<>(fromAndResult.getA(), entryToRemove.getA()));
                }
                i.remove();
            }
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }
    }
    
    private NavigableSet<Util.Pair<TimePoint, T>> getEntryForTo(TimePoint to) {
        NavigableSet<Util.Pair<TimePoint, T>> result = null;
        Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>> dummyForTo = createDummy(to);
        Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>> entryForTo = timeRangeCache.floor(dummyForTo);
        if (entryForTo != null && entryForTo.getA().equals(to)) {
            result = entryForTo.getB();
        }
        return result;
    }
    
    public void cache(TimePoint from, TimePoint to, T result) {
        LockUtil.lockForWrite(lock);
        try {
            NavigableSet<Util.Pair<TimePoint, T>> entryForTo = getEntryForTo(to);
            if (entryForTo == null) {
                entryForTo = new ArrayListNavigableSet<Util.Pair<TimePoint, T>>(timePointInPairComparator);
                Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>> pairForTo = new Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>>(
                        to, entryForTo);
                timeRangeCache.add(pairForTo);
            }
            entryForTo.add(new Util.Pair<TimePoint, T>(from, result));
            lruCache.put(new Util.Pair<TimePoint, TimePoint>(from, to), null);
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }
    }
    
    private Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>> createDummy(TimePoint to) {
        return new Util.Pair<TimePoint, NavigableSet<Util.Pair<TimePoint, T>>>(to, null);
    }

    /**
     * Removes all contents from this cache
     */
    public void clear() {
        LockUtil.lockForWrite(lock);
        try {
            timeRangeCache.clear();
            lruCache.clear();
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }

    }
}
