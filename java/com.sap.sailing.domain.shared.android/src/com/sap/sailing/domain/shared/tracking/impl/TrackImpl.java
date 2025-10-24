package com.sap.sailing.domain.shared.tracking.impl;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.function.Function;

import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.shared.tracking.FixAcceptancePredicate;
import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.shared.util.impl.ArrayListNavigableSet;
import com.sap.sse.shared.util.impl.UnmodifiableNavigableSet;

public class TrackImpl<FixType extends Timed> implements Track<FixType> {
    private static final long serialVersionUID = -4075853657857657528L;
    /**
     * The fixes, ordered by their time points
     */
    private final ArrayListNavigableSet<Timed> fixes;

    private final NamedReentrantReadWriteLock readWriteLock;

    protected static class DummyTimed implements Timed {
        private static final long serialVersionUID = 6047311973718918856L;
        private final TimePoint timePoint;
        public DummyTimed(TimePoint timePoint) {
            super();
            this.timePoint = timePoint;
        }
        @Override
        public TimePoint getTimePoint() {
            return timePoint;
        }
        @Override
        public String toString() {
            return timePoint.toString();
        }
    }
    
    public TrackImpl(String nameForReadWriteLock) {
        this(new ArrayListNavigableSet<Timed>(TimedComparator.INSTANCE), nameForReadWriteLock);
    }
    
    protected TrackImpl(ArrayListNavigableSet<Timed> fixes, String nameForReadWriteLock) {
        this.readWriteLock = new NamedReentrantReadWriteLock(nameForReadWriteLock, /* fair */ false);
        this.fixes = fixes;
    }
    
    /**
     * Synchronize the serialization such that no fixes are added while serializing
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        lockForRead();
        try {
            s.defaultWriteObject();
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public void lockForRead() {
        LockUtil.lockForRead(readWriteLock);
    }

    @Override
    public void unlockAfterRead() {
        LockUtil.unlockAfterRead(readWriteLock);
    }
    
    protected void lockForWrite() {
        LockUtil.lockForWrite(readWriteLock);
    }
    
    protected void unlockAfterWrite() {
        LockUtil.unlockAfterWrite(readWriteLock);
    }

    /**
     * Callers that want to iterate over the collection returned need to use {@link #lockForRead()} and {@link #unlockAfterRead()}
     * to avoid {@link ConcurrentModificationException}s. Should they modify the structure returned, they have to use
     * {@link #lockForWrite()} and {@link #unlockAfterWrite()}, respectively.
     */
    protected NavigableSet<FixType> getInternalRawFixes() {
        @SuppressWarnings("unchecked")
        NavigableSet<FixType> result = (NavigableSet<FixType>) fixes;
        return result;
    }

    /**
     * asserts that the calling thread holds at least one of read and write lock
     */
    protected void assertReadLock() {
        if (readWriteLock.getReadHoldCount() < 1 && readWriteLock.getWriteHoldCount() < 1) {
            throw new IllegalStateException("Caller must obtain read lock using lockForRead() before calling this method");
        }
    }
    
    protected void assertWriteLock() {
        if (readWriteLock.getWriteHoldCount() < 1) {
            throw new IllegalStateException("Caller must obtain write lock using lockForWrite() before calling this method");
        }
    }

    /**
     * Callers that want to iterate over the collection returned need to use {@link #lockForRead()} and
     * {@link #unlockAfterRead()} to avoid {@link ConcurrentModificationException}s.
     * 
     * @return the smoothened fixes ordered by their time points; this implementation simply delegates to
     *         {@link #getInternalRawFixes()} because for only {@link Timed} fixes we can't know how to remove outliers.
     *         Subclasses that constrain the <code>FixType</code> may provide smoothening implementations.
     */
    protected NavigableSet<FixType> getInternalFixes() {
        NavigableSet<FixType> result = getInternalRawFixes();
        return result;
    }

    /**
     * Iterates the fixes with outliers getting skipped, in the order of their time points.
     * Relies on {@link #getInternalFixes()} to void the track view from outliers.
     */
    @Override
    public NavigableSet<FixType> getFixes() {
        assertReadLock();
        return new UnmodifiableNavigableSet<FixType>(getInternalFixes());
    }
    
    @Override
    public Iterable<FixType> getFixes(TimePoint from, boolean fromInclusive, TimePoint to, boolean toInclusive) {
        return getFixes().subSet(getDummyFix(from), fromInclusive, getDummyFix(to), toInclusive);
    }

    /**
     * Iterates over the raw sequence of fixes, all potential outliers included
     */
    @Override
    public NavigableSet<FixType> getRawFixes() {
        assertReadLock();
        return new UnmodifiableNavigableSet<FixType>(getInternalRawFixes());
    }

    @Override
    public FixType getLastFixAtOrBefore(TimePoint timePoint) {
        return getLastFixAtOrBefore(timePoint, /* fixAcceptancePredicate == null means accept all */ null);
    }
    
    private FixType getLastFixAtOrBefore(TimePoint timePoint, FixAcceptancePredicate<FixType> fixAcceptancePredicate) {
        lockForRead();
        try {
            final NavigableSet<FixType> headSet = getInternalFixes().headSet(getDummyFix(timePoint), /* inclusive */ true);
            for (final Iterator<FixType> i=headSet.descendingIterator(); i.hasNext(); ) {
                final FixType next = i.next();
                if (fixAcceptancePredicate == null || fixAcceptancePredicate.isAcceptFix(next)) {
                    return next;
                }
            }
            return null;
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public FixType getLastFixBefore(TimePoint timePoint) {
        lockForRead();
        try {
            return (FixType) getInternalFixes().lower(getDummyFix(timePoint));
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public FixType getLastRawFixAtOrBefore(TimePoint timePoint) {
        lockForRead();
        try {
            return (FixType) getInternalRawFixes().floor(getDummyFix(timePoint));
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public FixType getFirstRawFixAtOrAfter(TimePoint timePoint) {
        lockForRead();
        try {
            return (FixType) getInternalRawFixes().ceiling(getDummyFix(timePoint));
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public FixType getFirstFixAtOrAfter(TimePoint timePoint) {
        return getFirstFixAtOrAfter(timePoint, /* fixAcceptancePredicate==null means accept all fixes */ null);
    }

    private FixType getFirstFixAtOrAfter(TimePoint timePoint, FixAcceptancePredicate<FixType> fixAcceptancePredicate) {
        lockForRead();
        try {
            final NavigableSet<FixType> tailSet = getInternalFixes().tailSet(getDummyFix(timePoint), /* inclusive */ true);
            for (final FixType next : tailSet) {
                if (fixAcceptancePredicate == null || fixAcceptancePredicate.isAcceptFix(next)) {
                    return next;
                }
            }
            return null;
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public FixType getLastRawFixBefore(TimePoint timePoint) {
        lockForRead();
        try {
            return (FixType) getInternalRawFixes().lower(getDummyFix(timePoint));
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public FixType getFirstFixAfter(TimePoint timePoint) {
        lockForRead();
        try {
            return (FixType) getInternalFixes().higher(getDummyFix(timePoint));
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public FixType getFirstRawFixAfter(TimePoint timePoint) {
        lockForRead();
        try {
            return (FixType) getInternalRawFixes().higher(getDummyFix(timePoint));
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public FixType getFirstRawFix() {
        lockForRead();
        try {
            if (getInternalFixes().isEmpty()) {
                return null;
            } else {
                return (FixType) getInternalFixes().first();
            }
        } finally {
            unlockAfterRead();
        }
    }
    
    @Override
    public FixType getLastRawFix() {
        lockForRead();
        try {
            if (getInternalRawFixes().isEmpty()) {
                return null;
            } else {
                return (FixType) getInternalRawFixes().last();
            }
        } finally {
            unlockAfterRead();
        }
    }
    
    /**
     * @param fixAcceptancePredicate
     *            if not {@code null}, adjacent fixes will be skipped as long as this predicate does not
     *            {@link FixAcceptancePredicate#isAcceptFix(Object) accept} the fix. This can, e.g., be used to skip
     *            fixes that don't have values in a dimension required. If {@code null}, the next fixes left and right
     *            (including the exact {@code timePoint} if a fix exists there) will be used without further check.
     */
    private Pair<FixType, FixType> getSurroundingFixes(TimePoint timePoint, FixAcceptancePredicate<FixType> fixAcceptancePredicate) {
        FixType left = getLastFixAtOrBefore(timePoint, fixAcceptancePredicate);
        FixType right = getFirstFixAtOrAfter(timePoint, fixAcceptancePredicate);
        com.sap.sse.common.Util.Pair<FixType, FixType> result = new com.sap.sse.common.Util.Pair<>(left, right);
        return result;
    }

    /**
     * Calculates a linear interpolation of values based on their time points and a target time point that is expected
     * to be in between (inclusive) the two time points for the two values. If the two time points for the two values
     * are equal, the average of the two values is returned.
     */
    private <V, T> T timeBasedAverage(TimePoint timePoint, ScalableValue<V, T> value1, TimePoint timePoint1, ScalableValue<V, T> value2, TimePoint timePoint2) {
        final T acc;
        if (timePoint1.equals(timePoint2)) {
            acc = value1.add(value2).divide(2);
        } else {
            long timeDiff1 = Math.abs(timePoint1.asMillis() - timePoint.asMillis());
            long timeDiff2 = Math.abs(timePoint2.asMillis() - timePoint.asMillis());
            acc = value1.multiply(timeDiff2).add(value2.multiply(timeDiff1)).divide(timeDiff1 + timeDiff2);
        }
        return acc;
    }

    @Override
    public <InternalType, ValueType> ValueType getInterpolatedValue(TimePoint timePoint,
            Function<FixType, ScalableValue<InternalType, ValueType>> converter) {
        return getInterpolatedValue(timePoint, converter, /* fixAcceptancePredicate==null means accept all */ null);
    }

    protected <InternalType, ValueType> ValueType getInterpolatedValue(TimePoint timePoint,
            Function<FixType, ScalableValue<InternalType, ValueType>> converter, FixAcceptancePredicate<FixType> fixAcceptancePredicate) {
        final ValueType result;
        Pair<FixType, FixType> fixPair = getSurroundingFixes(timePoint, fixAcceptancePredicate);
        if (fixPair.getA() == null) {
            if (fixPair.getB() == null) {
                result = null;
            } else {
                result = converter.apply(fixPair.getB()).divide(1);
            }
        } else {
            if (fixPair.getB() == null || fixPair.getA() == fixPair.getB()) {
                result = converter.apply(fixPair.getA()).divide(1);
            } else {
                result = timeBasedAverage(timePoint,
                        converter.apply(fixPair.getA()), fixPair.getA().getTimePoint(),
                        converter.apply(fixPair.getB()), fixPair.getB().getTimePoint());
            }
        }
        return result;
    }

    @Override
    public Iterator<FixType> getFixesIterator(TimePoint startingAt, boolean inclusive) {
        assertReadLock();
        return getTimeConstrainedFixesIterator(getInternalFixes(), startingAt, inclusive, /* endingAt */ null, /* endingAtInclusive */ false);
    }

    @Override
    public Iterator<FixType> getFixesIterator(TimePoint startingAt, boolean startingAtInclusive, TimePoint endingAt,
            boolean endingAtInclusive) {
        assertReadLock();
        return getTimeConstrainedFixesIterator(getInternalFixes(), startingAt, startingAtInclusive, endingAt, endingAtInclusive);
    }

    @Override
    public Iterator<FixType> getFixesDescendingIterator(TimePoint startingAt, boolean inclusive) {
        assertReadLock();
        Iterator<FixType> result = (Iterator<FixType>) getInternalFixes().headSet(
                getDummyFix(startingAt), inclusive).descendingIterator();
        return result;
    }

    /**
     * Creates a dummy fix that conforms to <code>FixType</code>. This in particular means that subclasses
     * instantiating <code>FixType</code> with a specific class need to redefine this method so as to return
     * a dummy fix complying with their instantiation type used for <code>FixType</code>. Otherwise, a
     * {@link ClassCastException} may result upon certain operations performed with the fix returned by
     * this method.
     */
    protected FixType getDummyFix(TimePoint timePoint) {
        @SuppressWarnings("unchecked")
        FixType result = (FixType) new DummyTimed(timePoint);
        return result;
    }

    @Override
    public Iterator<FixType> getRawFixesIterator(TimePoint startingAt, boolean inclusive) {
        assertReadLock();
        return getTimeConstrainedFixesIterator(getInternalRawFixes(), startingAt, inclusive, /* endingAt */ null, /* endingAtInclusive */ false);
    }

    private Iterator<FixType> getTimeConstrainedFixesIterator(NavigableSet<FixType> set, TimePoint startingAt, boolean startingAtInclusive,
            TimePoint endingAt, boolean endingAtInclusive) {
        assertReadLock();
        if (startingAt != null && endingAt != null) {
            set = set.subSet(getDummyFix(startingAt), startingAtInclusive, getDummyFix(endingAt), endingAtInclusive);
        } else if (endingAt != null) {
            set = set.headSet(getDummyFix(endingAt), endingAtInclusive);
        } else  if (startingAt != null) {
            set = set.tailSet(getDummyFix(startingAt), startingAtInclusive);
        }
        Iterator<FixType> result = set.iterator();
        return result;
    }
    
    @Override
    public Iterator<FixType> getRawFixesIterator(TimePoint startingAt, boolean startingAtInclusive,
            TimePoint endingAt, boolean endingAtInclusive) {
        assertReadLock();
        return getTimeConstrainedFixesIterator(getInternalRawFixes(), startingAt, startingAtInclusive, endingAt, endingAtInclusive);
    }

    @Override
    public Iterator<FixType> getRawFixesDescendingIterator(TimePoint startingAt, boolean inclusive) {
        assertReadLock();
        Iterator<FixType> result = (Iterator<FixType>) getInternalRawFixes().headSet(
                getDummyFix(startingAt), inclusive).descendingIterator();
        return result;
    }

    protected boolean add(FixType fix) {
        return add(fix, /* replace */ false);
    }

    /**
     * @return {@code true} if the fix was added or replaced; {@code false} in case no change was performed
     */
    protected boolean add(FixType fix, boolean replace) {
        lockForWrite();
        try {
            final AddResult addResult = addWithoutLocking(fix, replace);
            return addResult == AddResult.ADDED || addResult == AddResult.REPLACED;
        } finally {
            unlockAfterWrite();
        }
    }

    /**
     * The caller must ensure to hold the write lock for this track when calling this method.
     * 
     * @param replace
     *            whether or not to replace an existing fix in the track that is equal to {@link #fix} as defined by the
     *            comparator used for the {@link #fixes} set. By default this is a comparator only comparing the
     *            fixes' time stamps. Subclasses may use different comparator implementations.
     */
    protected AddResult addWithoutLocking(FixType fix, boolean replace) {
        final AddResult result;
        final boolean added = getInternalRawFixes().add(fix);
        if (!added && replace) {
            getInternalRawFixes().remove(fix);
            result = getInternalRawFixes().add(fix) ? AddResult.REPLACED : AddResult.NOT_ADDED;
        } else {
            result = added ? AddResult.ADDED : AddResult.NOT_ADDED;
        }
        return result;
    }

    @Override
    public Duration getAverageIntervalBetweenFixes() {
        lockForRead();
        try {
            final Duration result;
            final int size = getRawFixes().size();
            if (size > 1) {
                result = getRawFixes().first().getTimePoint().until(getRawFixes().last().getTimePoint()).divide(size-1);
            } else {
                result = null;
            }
            return result;
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public Duration getAverageIntervalBetweenRawFixes() {
        lockForRead();
        try {
            final Duration result;
            final int size = getRawFixes().size();
            if (size > 1) {
                result = getRawFixes().first().getTimePoint().until(getRawFixes().last().getTimePoint()).divide(size-1);
            } else {
                result = null;
            }
            return result;
        } finally {
            unlockAfterRead();
        }
    }
    
    @Override
    public <T> T getValueSum(TimePoint from, TimePoint to, T nullElement, Adder<T> adder, TimeRangeCache<T> cache, TimeRangeValueCalculator<T> valueCalculator) {
        return getValueSumRecursively(from, to, /* recursionLevel */ 0, nullElement, adder, cache, valueCalculator);
    }
    
    private <T> T getValueSumRecursively(TimePoint from, TimePoint to, int recursionDepth, T nullElement,
            Adder<T> adder, TimeRangeCache<T> cache, TimeRangeValueCalculator<T> valueCalculator) {
        T result;
        if (!from.before(to)) {
            result = nullElement;
        } else {
            boolean perfectCacheHit = false;
            lockForRead();
            try {
                Util.Pair<TimePoint, Util.Pair<TimePoint, T>> bestCacheEntry = cache.getEarliestFromAndResultAtOrAfterFrom(from, to);
                if (bestCacheEntry != null) {
                    perfectCacheHit = true; // potentially a cache hit; but if it doesn't span the full interval, it's not perfect; see below
                    // compute the missing stretches between best cache entry's "from" and our "from" and the cache
                    // entry's "to" and our "to"
                    T valueFromFromToBeginningOfCacheEntry = nullElement;
                    T valueFromEndOfCacheEntryToTo = nullElement;
                    if (!bestCacheEntry.getB().getA().equals(from)) {
                        assert bestCacheEntry.getB().getA().after(from);
                        perfectCacheHit = false;
                        valueFromFromToBeginningOfCacheEntry = getValueSumRecursively(from, bestCacheEntry
                                .getB().getA(), recursionDepth + 1, nullElement, adder, cache, valueCalculator);
                    }
                    if (!bestCacheEntry.getA().equals(to)) {
                        assert bestCacheEntry.getA().before(to);
                        perfectCacheHit = false;
                        valueFromEndOfCacheEntryToTo = getValueSumRecursively(bestCacheEntry.getA(), to,
                                recursionDepth + 1, nullElement, adder, cache, valueCalculator);
                    }
                    if (valueFromEndOfCacheEntryToTo == null || bestCacheEntry.getB().getB() == null) {
                        result = null;
                    } else {
                        result = adder.add(adder.add(valueFromFromToBeginningOfCacheEntry, bestCacheEntry.getB().getB()), 
                                valueFromEndOfCacheEntryToTo);
                    }
                } else {
                    if (from.compareTo(to) < 0) {
                        result = valueCalculator.calculate(from, to);
                    } else {
                        result = nullElement;
                    }
                }
                // run the cache update while still holding the read lock; this avoids bug4629 where a cache invalidation
                // caused by fix insertions can come after the result calculation and before the cache update
                if (!perfectCacheHit && recursionDepth == 0) {
                    cache.cache(from, to, result);
                }
            } finally {
                unlockAfterRead();
            }
        }
        return result;
    }


    
    @Override
    public int size() {
        return fixes.size();
    }

    @Override
    public boolean isEmpty() {
        return fixes.isEmpty();
    }
}
