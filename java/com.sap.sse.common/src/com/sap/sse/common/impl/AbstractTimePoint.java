package com.sap.sse.common.impl;

import java.util.Comparator;
import java.util.Date;

import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;

public abstract class AbstractTimePoint implements TimePoint {
    private static final long serialVersionUID = 8825508619301420378L;

    public static Comparator<TimePoint> TIMEPOINT_COMPARATOR = new SerializableComparator<TimePoint>() {
        private static final long serialVersionUID = 7644726881387366025L;

        @Override
        public int compare(TimePoint o1, TimePoint o2) {
            long o1Millis = o1.asMillis();
            long o2Millis = o2.asMillis();
            return o1Millis<o2Millis?-1:o1Millis==o2Millis?0:1;
        }
    };

    @Override
    public int compareTo(TimePoint o) {
        return TIMEPOINT_COMPARATOR.compare(this, o);
    }
    
    @Override
    public Date asDate() {
        Date result = getDateFromCache();
        if (result == null) {
            result = new Date(asMillis());
            cacheDate(result);
        }
        return result;
    }
    
    @Override
    public TimePoint plus(Duration duration) {
        final TimePoint result;
        if (duration != null) {
            result = plus(duration.asMillis());
        } else {
            result = this;
        }
        return result;
    }

    @Override
    public TimePoint minus(Duration duration) {
        final TimePoint result;
        if (duration != null) {
            result = minus(duration.asMillis());
        } else {
            result = this;
        }
        return result;
    }

    @Override
    public Duration until(TimePoint later) {
        return new MillisecondsDurationImpl(later.asMillis()-asMillis());
    }

    @Override
    public TimePoint plus(long milliseconds) {
        final TimePoint result;
        if (milliseconds > 0) {
            if (EndOfTime.asMillis()-milliseconds >= asMillis()) {
                result = new MillisecondsTimePoint(asMillis()+milliseconds);
            } else {
                result = EndOfTime;
            }
        } else if (asMillis() < milliseconds) {
            result = BeginningOfTime;
        } else {
            result = new MillisecondsTimePoint(asMillis()+milliseconds);
        }
        return result;
    }
    
    @Override
    public TimePoint minus(long milliseconds) {
        return plus(-milliseconds);
    }

    @Override
    public boolean after(TimePoint other) {
        return this.compareTo(other) > 0;
    }

    @Override
    public boolean before(TimePoint other) {
        return this.compareTo(other) < 0;
    }

    @Override
    public int hashCode() {
        return (int) (asMillis() & Integer.MAX_VALUE);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof TimePoint) {
            return compareTo((TimePoint) obj) == 0;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        long millis = asMillis() % 1000l;
        final String fullSeconds = asDate().toString();
        final String result;
        if (millis == 0) {
            result = fullSeconds;
        } else {
            result = fullSeconds + " (+"+millis+"ms)";
        }
        return result;
    }

    /**
     * A subclass may implement a cache for the {@link Date} representation by overriding this method as well
     * as {@link #cacheDate(Date)}.
     */
    protected Date getDateFromCache() {
        return null;
    }

    /**
     * A subclass may implement a cache for the {@link Date} representation by overriding this method as well
     * as {@link #getDate()}.
     */
    protected void cacheDate(Date date) {}

    @Override
    public TimePoint getNearestModuloOneMinute(final TimePoint syncToClosestSecondsAndMillisOf) {
        final Duration diffModOneMinute = this.until(syncToClosestSecondsAndMillisOf).mod(Duration.ONE_MINUTE);
        final Duration diffToAdd;
        if (diffModOneMinute.compareTo(Duration.ONE_SECOND.times(30l)) > 0) {
            diffToAdd = Duration.ONE_MINUTE.minus(diffModOneMinute).times(-1l);
        } else if (diffModOneMinute.compareTo(Duration.ONE_SECOND.times(-30l)) < 0) {
            diffToAdd = Duration.ONE_MINUTE.minus(diffModOneMinute.times(-1l));
        } else {
            diffToAdd = diffModOneMinute;
        }
        final TimePoint result = this.plus(diffToAdd);
        assert this.until(result).abs().compareTo(Duration.ONE_SECOND.times(30)) <= 0;
        assert syncToClosestSecondsAndMillisOf.asMillis() % 60000 == result.asMillis() % 60000;
        return result;
    }

    /**
     * All {@link AbstractTimePoint} implementations are backed by milliseconds (see {@link #asMillis()}), so the
     * smallest distinguishable difference between two instants is one millisecond. A subclass with a finer resolution
     * must override this.
     */
    @Override
    public Duration getResolution() {
        return Duration.ONE_MILLISECOND;
    }

    @Override
    public TimePoint plusResolution() {
        return plus(getResolution());
    }

    @Override
    public TimePoint minusResolution() {
        return minus(getResolution());
    }
}
