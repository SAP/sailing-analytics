package com.sap.sse.common;

import java.io.Serializable;
import java.util.Date;

import com.sap.sse.common.impl.MillisecondsTimePoint;

public interface TimePoint extends Comparable<TimePoint>, Serializable {
    // JavaScript / ECMAScript Date can only handle a value range of -100,000,000 days before
    // and after the beginning of the Epoch. If we want these constants to serialize losslessly
    // to JavaScript / GWT, we need to accept this constraint.
    TimePoint BeginningOfTime = new MillisecondsTimePoint(-100000000l * 24 * 3600);
    TimePoint EndOfTime = new MillisecondsTimePoint(100000000l * 24 * 3600);
    
    long asMillis();

    Date asDate();
    
    /**
     * @return the time point corresponding to {@code date} or {@code null} if {@code date} is {@code null}
     */
    static TimePoint of(Date date) {
        return date == null ? null : new MillisecondsTimePoint(date);
    }
    
    static TimePoint of(Long millis) {
        return millis == null ? null : of((long) millis);
    }
    
    static TimePoint of(long millis) {
        return new MillisecondsTimePoint(millis);
    }
    
    static TimePoint now() {
        return MillisecondsTimePoint.now();
    }

    /**
     * @return a time point that is {@code milliseconds} later than {@code this} time point, except in case the
     *         resulting time point would be after the {@link #EndOfTime} in which case {@link #EndOfTime} is returned
     *         instead of provoking an overflow with wrap-around.
     */
    TimePoint plus(long milliseconds);
    
    /**
     * @return a time point that is {@code duration} later than {@code this} time point, except in case the
     *         resulting time point would be after the {@link #EndOfTime} in which case {@link #EndOfTime} is returned
     *         instead of provoking an overflow with wrap-around.
     */
    TimePoint plus(Duration duration);
    
    /**
     * @return a time point that is {@code milliseconds} earlier than {@code this} time point, except in case the
     *         resulting time point would be before the {@link #BeginningOfTime} in which case {@link #BeginningOfTime} is returned
     *         instead of provoking an underflow with wrap-around.
     */
    TimePoint minus(long milliseconds);
    
    /**
     * @return a time point that is {@code duration} earlier than {@code this} time point, except in case the
     *         resulting time point would be before the {@link #BeginningOfTime} in which case {@link #BeginningOfTime} is returned
     *         instead of provoking an underflow with wrap-around.
     */
    TimePoint minus(Duration duration);
    
    /**
     * The duration that {@code later} is after {@code this}. May be negative if {@code later} is before {@code this}.
     */
    Duration until(TimePoint later);
    
    boolean after(TimePoint other);
    
    boolean before(TimePoint other);

    /**
     * @return a {@link TimePoint} such that when used to set the time of a calendar, the seconds and milliseconds will
     *         match that of {@code syncToClosestSecondsAndMillisOf} and the time difference to {@code this} time point is
     *         minimal. In particular, the time difference will always be less than or equal to 30s.
     */
    TimePoint getNearestModuloOneMinute(TimePoint syncToClosestSecondsAndMillisOf);

    /**
     * The resolution of this time point representation, i.e. the smallest positive {@link Duration} by which two
     * distinct instants of this implementation can differ. Adding exactly this duration to a time point yields the next
     * distinct representable instant, so {@code t.plus(t.getResolution())} is the smallest exclusive {@link TimeRange}
     * end that still keeps {@code t} inside the range {@code [t, t.plus(resolution))}. This is the resolution-safe way
     * to convert an <em>inclusive</em> end into an <em>exclusive</em> one; see the master data import/export path that
     * relies on it. All current implementations are backed by milliseconds and therefore return
     * {@link Duration#ONE_MILLISECOND}; an implementation with a finer (e.g. nanosecond) resolution must override this
     * accordingly.
     */
    Duration getResolution();

    /**
     * The next distinct representable instant after this one, i.e. {@code this.plus(getResolution())}. Because
     * {@link TimeRange#to()} is <em>exclusive</em>, this is the smallest exclusive range end that still keeps this
     * instant inside {@code [this, this.plusResolution())}; it is therefore the resolution-safe way to turn an
     * <em>inclusive</em> upper bound into an <em>exclusive</em> one, and more generally to obtain an instant that is
     * strictly, but minimally, after this one (e.g. "just after a fix"). Saturation at
     * {@link TimePoint#EndOfTime} follows {@link #plus(Duration)}.
     */
    TimePoint plusResolution();

    /**
     * The previous distinct representable instant before this one, i.e. {@code this.minus(getResolution())}; the
     * counterpart of {@link #plusResolution()} for obtaining an instant that is strictly, but minimally, before this
     * one (e.g. "just before a fix"). Saturation at {@link TimePoint#BeginningOfTime} follows {@link #minus(Duration)}.
     */
    TimePoint minusResolution();

}
