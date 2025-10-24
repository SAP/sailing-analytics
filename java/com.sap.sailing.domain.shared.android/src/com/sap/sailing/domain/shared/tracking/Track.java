package com.sap.sailing.domain.shared.tracking;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.shared.tracking.impl.TimeRangeCache;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.common.scalablevalue.ScalableValue;

/**
 * A track records {@link Timed} items for an object of type <code>ItemType</code>. It allows clients to ask for a value
 * close to a given {@link TimePoint}. The track manages a time-based set of raw fixes. An implementation may have an
 * understanding of how to eliminate outliers. For example, if a track implementation knows it's tracking boats, it may
 * consider fixes that the boat cannot possibly have reached due to its speed and direction change limitations as
 * outliers. The set of fixes with outliers filtered out can be obtained using {@link #getFixes} whereas
 * {@link #getRawFixes()} returns the unfiltered, raw fixes. If an implementation has no idea what an outlier is,
 * both methods will return the same fix sequence.<p>
 * 
 * With tracks, concurrency is an important issue. Threads may want to modify a track while other threads may want to
 * read from it. Several methods such as {@link #getLastFixAtOrBefore(TimePoint)} return a single fix and can manage
 * concurrency internally. However, those methods returning a collection of fixes, such as {@link #getFixes()} or an
 * iterator over a collection of fixes, such as {@link #getFixesIterator(TimePoint, boolean)}, need special treatment.
 * Until we internalize such iterations (see bug 824, http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=824),
 * callers need to manage a read lock which is part of a {@link ReadWriteLock} managed by this track. Callers do so
 * by calling {@link #lockForRead} and {@link #unlockAfterRead}.
 * 
 * @author Axel Uhl (d043530)
 */
public interface Track<FixType extends Timed> extends Serializable {
    /**
     * An adding function to be used together with {@link Track#getValueSum(TimePoint, TimePoint, Object, Adder, TimeRangeCache, TimeRangeValueCalculator)}.
     * 
     * @author Axel Uhl (D043530)
     *
     * @param <T>
     */
    static interface Adder<T> {
        /**
         * Adds two elements of type {@code T}. Neither argument must be {@code null}.
         */
        T add(T t1, T t2);
    }
    
    static interface TimeRangeValueCalculator<T> {
        /**
         * Calculates a value for fixes across a time range. When the method is called,
         * a read lock will previously have been {@link Track#lockForRead obtained} before,
         * so an implementing class does not need to worry about acquiring the lock.
         */
        T calculate(TimePoint from, TimePoint to);
    }
    
    /**
     * Locks this track for reading by the calling thread. If the thread already holds the lock for this track,
     * the hold count will be incremented. Make sure to call {@link #unlockAfterRead()} in a <code>finally</code>
     * block to release the lock under all possible circumstances. Failure to do so will inevitably lead to
     * deadlocks!
     */
    void lockForRead();
    
    /**
     * Decrements the hold count for this track's read lock for the calling thread. If it goes to zero, the lock will be
     * released and other readers or a writer can obtain the lock. Make sure to call this method in a
     * <code>finally</code> block for each {@link #lockForRead()} invocation.
     */
    void unlockAfterRead();
    
    /**
     * Callers must have called {@link #lockForRead()} before calling this method. This will be checked, and an exception
     * will be thrown in case the caller has failed to do so.
     * 
     * @return the smoothened fixes
     */
    Iterable<FixType> getFixes();

    /**
     * Callers must have called {@link #lockForRead()} before calling this method. This will be checked, and an exception
     * will be thrown in case the caller has failed to do so.
     * 
     * @return The smoothened fixes between from and to.
     */
    Iterable<FixType> getFixes(TimePoint from, boolean fromInclusive, TimePoint to, boolean toInclusive);

    /**
     * Callers must have called {@link #lockForRead()} before calling this method. This will be checked, and an exception
     * will be thrown in case the caller has failed to do so.
     */
    Iterable<FixType> getRawFixes();

    /**
     * Returns <code>null</code> if no such fix exists.      
     */
    FixType getLastFixAtOrBefore(TimePoint timePoint);

    /**
     * Returns <code>null</code> if no such fix exists.      
     */
    FixType getLastFixBefore(TimePoint timePoint);
    
    /**
     * Returns <code>null</code> if no such fix exists.      
     */
    FixType getLastRawFixAtOrBefore(TimePoint timePoint);

    /**
     * Returns <code>null</code> if no such fix exists.      
     */
    FixType getFirstFixAtOrAfter(TimePoint timePoint);

    /**
     * Returns <code>null</code> if no such fix exists.      
     */
    FixType getFirstRawFixAtOrAfter(TimePoint timePoint);

    /**
     * Returns <code>null</code> if no such fix exists.      
     */
    FixType getLastRawFixBefore(TimePoint timePoint);

    /**
     * Returns <code>null</code> if no such fix exists.      
     */
    FixType getFirstRawFixAfter(TimePoint timePoint);
    
    /**
     * Returns <code>null</code> if no such fix exists.      
     */
    FixType getFirstFixAfter(TimePoint timePoint);
    
    /**
     * The first fix in this track or <code>null</code> if the track is empty. The fix returned may
     * be an outlier that is not returned by calls operating on the smoothened version of the track.
     */
    FixType getFirstRawFix();
    
    /**
     * The last fix in this track or <code>null</code> if the track is empty. The fix returned may
     * be an outlier that is not returned by calls operating on the smoothened version of the track.
     */
    FixType getLastRawFix();
    
    /**
     * Interpolates an aspect of the fixes in this track for a given {@code timePoint}. If {@code timePoint} matches
     * exactly a fix in this track, that fix is used. If this track is empty, {@code null} is returned. If the
     * {@code timePoint} is after the last fix of this track, the last fix is used; if before the first fix, the first
     * fix is used.
     * <p>
     * 
     * The fix(es) are converted to {@link ScalableValue}s using the {@code converter} which gives callers a choice
     * which aspect of the fixes to project and interpolate. If more than one value results because two fixes (one
     * before, one after) are used, linear interpolation based on the fixes' time points takes place.
     * <p>
     * 
     * Example: for a track of {@link GPSFixMoving} fixes the course over ground shall be determined for a given time
     * point. The call would look like this:
     * {@code getInterpolatedValue(timePoint, f->new ScalableBearing(f.getSpeed().getBearing()))}
     * 
     * @return the projected interpolated value, typed by what the {@link ScalableValue#divide(double)} method returns.
     */
    <InternalType, ValueType> ValueType getInterpolatedValue(TimePoint timePoint,
            Function<FixType, ScalableValue<InternalType, ValueType>> converter);

    /**
     * Returns an iterator starting at the first fix after <code>startingAt</code> (or "at or after" in case
     * <code>inclusive</code> is <code>true</code>). The fixes returned by the iterator exclude outliers (see
     * also {@link #getFixes()} and returns the remaining fixes without any smoothening or dampening applied.
     * 
     * Callers must have called {@link #lockForRead()} before calling this method. This will be checked, and an exception
     * will be thrown in case the caller has failed to do so.
     */
    Iterator<FixType> getFixesIterator(TimePoint startingAt, boolean inclusive);
    
    /**
     * Returns an iterator starting at the first fix after <code>startingAt</code> (or "at or after" in case
     * <code>inclusive</code> is <code>true</code>) and that ends at the <code>endingAt</code> time point or just before
     * in case <code>endingAtIncluive</code> is false. The fixes returned by the iterator are the smoothened fixes (see
     * also {@link #getFixes()}, without any smoothening or dampening applied.
     * 
     * Callers must have called {@link #lockForRead()} before calling this method. This will be checked, and an
     * exception will be thrown in case the caller has failed to do so.
     * 
     * @param startingAt
     *            if <code>null</code>, starts with the first fix available
     * @param endingAt
     *            if <code>null</code>., ends with the last fix available
     */
    Iterator<FixType> getFixesIterator(TimePoint startingAt, boolean startingAtInclusive, TimePoint endingAt, boolean endingAtInclusive);
    
    /**
     * Returns an iterator starting at the first raw fix after <code>startingAt</code> (or "at or after" in case
     * <code>inclusive</code> is <code>true</code>). The fixes returned by the iterator are the raw fixes (see also
     * {@link #getRawFixes()}, without any smoothening or dampening applied.
     * 
     * Callers must have called {@link #lockForRead()} before calling this method. This will be checked, and an exception
     * will be thrown in case the caller has failed to do so.
     */
    Iterator<FixType> getRawFixesIterator(TimePoint startingAt, boolean inclusive);

    /**
     * Returns an iterator starting at the first raw fix after <code>startingAt</code> (or "at or after" in case
     * <code>startingAtInclusive</code> is <code>true</code>) and ending at the <code>endingAt</code> time point or just before
     * in case <code>endingAtIncluive</code> is false. The fixes returned by the iterator are the raw fixes (see also
     * {@link #getRawFixes()}, without any smoothening or dampening applied.
     * 
     * Callers must have called {@link #lockForRead()} before calling this method. This will be checked, and an exception
     * will be thrown in case the caller has failed to do so.
     */
    Iterator<FixType> getRawFixesIterator(TimePoint startingAt, boolean startingAtInclusive, TimePoint endingAt, boolean endingAtInclusive);

    /**
     * Returns a descending iterator starting at the first fix before <code>startingAt</code> (or "at or before" in case
     * <code>inclusive</code> is <code>true</code>). The fixes returned by the iterator are the smoothened fixes (see
     * also {@link #getFixes()}, without any smoothening or dampening applied.
     * 
     * Callers must have called {@link #lockForRead()} before calling this method. This will be checked, and an exception
     * will be thrown in case the caller has failed to do so.
     */
    Iterator<FixType> getFixesDescendingIterator(TimePoint startingAt, boolean inclusive);

    /**
     * Returns a descending iterator starting at the first raw fix before <code>startingAt</code> (or "at or before" in case
     * <code>inclusive</code> is <code>true</code>). The fixes returned by the iterator are the raw fixes (see also
     * {@link #getRawFixes()}, without any smoothening or dampening applied.
     * 
     * Callers must have called {@link #lockForRead()} before calling this method. This will be checked, and an exception
     * will be thrown in case the caller has failed to do so.
     */
    Iterator<FixType> getRawFixesDescendingIterator(TimePoint startingAt, boolean inclusive);
    
    /**
     * @return the average duration between two fixes (outliers removed) in this track or <code>null</code> if there is not
     * more than one fix in the track
     */
    Duration getAverageIntervalBetweenFixes();
    
    /**
     * @return the average duration between two fixes (outliers <em>not</em> removed) in this track or <code>null</code> if there is not
     * more than one raw fix in the track
     */
    Duration getAverageIntervalBetweenRawFixes();
    
    <T> T getValueSum(TimePoint from, TimePoint to, T nullElement, Adder<T> adder, TimeRangeCache<T> cache, TimeRangeValueCalculator<T> valueCalculator);
    
    /**
     * @return the number of raw fixes contained in the Track.
     */
    int size();
    
    /**
     * Tells whether the collection of {@link #getRawFixes() raw fixes} (no outliers removed) is empty
     */
    boolean isEmpty();

}
