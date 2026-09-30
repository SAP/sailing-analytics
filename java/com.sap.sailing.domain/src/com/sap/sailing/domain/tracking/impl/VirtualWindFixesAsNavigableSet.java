package com.sap.sailing.domain.tracking.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.WindTrackImpl.DummyWind;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.util.impl.AbstractUnmodifiableNavigableSet;
import com.sap.sse.shared.util.impl.DescendingNavigableSet;

/**
 * Emulates a collection of {@link Wind} fixes for a {@link TrackedRace}. Subclasses have to specify a resolution and a
 * way to compute a wind fix for a given time point in the form of an implementation of the
 * {@link #getWind(Position, TimePoint)} method.
 * <p>
 * 
 * If not constrained by a {@link #from} and/or a {@link #to} time point, an equidistant time field is assumed, starting
 * at {@link TrackedRace#getStart()} and leading up to {@link TrackedRace#getTimePointOfNewestEvent()}. If
 * {@link TrackedRace#getStart()} returns <code>null</code>, {@link Long#MAX_VALUE} is used as the {@link #from} time
 * point, pushing the start to the more or less infinite future ("end of the universe"). If no event was received yet
 * and hence {@link TrackedRace#getTimePointOfNewestEvent()} returns <code>null</code>, the {@link #to} end is assumed
 * to be the beginning of the epoch (1970-01-01T00:00:00).
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public abstract class VirtualWindFixesAsNavigableSet extends AbstractUnmodifiableNavigableSet<Wind> {
    private static final long serialVersionUID = -7191543758261688727L;

    /**
     * The time resolution is one second.
     */
    private final long resolutionInMilliseconds;

    private final TrackedRace trackedRace;

    private final TimePoint from;

    private final TimePoint to;
    
    private final WindTrack track;

    protected VirtualWindFixesAsNavigableSet(WindTrack track, TrackedRace trackedRace, long resolutionInMilliseconds) {
        this(track, trackedRace, null, null, resolutionInMilliseconds);
    }
    
    /**
     * Compute the {@link Wind} fix for the specified position and time point to deliver in this virtual track.
     */
    abstract protected Wind getWind(Position p, TimePoint timePoint);

    /**
     * @param from expected to be an integer multiple of {@link #resolutionInMilliseconds} or <code>null</code>
     * @param to expected to be an integer multiple of {@link #resolutionInMilliseconds} or <code>null</code>
     */
    protected VirtualWindFixesAsNavigableSet(WindTrack track, TrackedRace trackedRace,
            TimePoint from, TimePoint to, long resolutionInMilliseconds) {
        this.track = track;
        this.trackedRace = trackedRace;
        assert from == null || from.asMillis() % resolutionInMilliseconds == 0;
        this.from = from;
        assert to == null || to.asMillis() % resolutionInMilliseconds == 0;
        this.to = to;
        this.resolutionInMilliseconds = resolutionInMilliseconds;
    }
    
    public long getResolutionInMilliseconds() {
        return resolutionInMilliseconds;
    }

    protected WindTrack getTrack() {
        return track;
    }
    
    protected TrackedRace getTrackedRace() {
        return trackedRace;
    }

    protected TimePoint lowerToResolution(TimePoint timePoint) {
        TimePoint result;
        final TimePoint timePointOfLastEvent = getTrackedRace().getTimePointOfNewestEvent();
        if (timePointOfLastEvent != null && timePoint.compareTo(timePointOfLastEvent) > 0) {
            result = lowerToResolution(timePointOfLastEvent);
        } else {
            final long minuend = timePoint.asMillis() <= 0 ? getResolutionInMilliseconds()+1 : 1;
            result = new MillisecondsTimePoint((timePoint.asMillis() - minuend) / getResolutionInMilliseconds()
                    * getResolutionInMilliseconds());
        }
        return result;
    }

    protected TimePoint floorToResolution(TimePoint timePoint) {
        TimePoint result;
        final TimePoint timePointOfLastEvent = getTrackedRace().getTimePointOfNewestEvent();
        if (timePointOfLastEvent != null && timePoint.compareTo(timePointOfLastEvent) > 0) {
            result = floorToResolution(timePointOfLastEvent);
        } else {
            final long minuend = timePoint.asMillis() < 0 ? getResolutionInMilliseconds() : 0;
            result = new MillisecondsTimePoint((timePoint.asMillis() - minuend) / getResolutionInMilliseconds()
                    * getResolutionInMilliseconds());
        }
        return result;
    }

    protected TimePoint ceilingToResolution(TimePoint timePoint) {
        TimePoint result;
        final TimePoint startOfTracking = getTrackedRace().getStartOfTracking();
        if (startOfTracking != null && timePoint.compareTo(startOfTracking) < 0) {
            result = ceilingToResolution(startOfTracking);
        } else {
            final long minuend = timePoint.asMillis() <= 0 ? getResolutionInMilliseconds()+1 : 1;
            result = new MillisecondsTimePoint(((timePoint.asMillis() - minuend) / getResolutionInMilliseconds() + 1)
                    * getResolutionInMilliseconds());
        }
        return result;
    }

    protected TimePoint higherToResolution(TimePoint timePoint) {
        TimePoint result;
        final TimePoint startOfTracking = getTrackedRace().getStartOfTracking();
        if (startOfTracking != null && timePoint.compareTo(startOfTracking) < 0) {
            result = higherToResolution(startOfTracking);
        } else {
            final long minuend = timePoint.asMillis() < 0 ? getResolutionInMilliseconds()-1 : 0;
            result = new MillisecondsTimePoint(((timePoint.asMillis() - minuend) / getResolutionInMilliseconds() + 1)
                    * getResolutionInMilliseconds());
        }
        return result;
    }

    /**
     * The time point starting from and including which the GPS fixes are considered in the race's tracks. Returns the
     * value of {@link #from} unless it is <code>null</code>. In this case, the time point of the
     * {@link TrackedRace#getStartOfRace() race start}, {@link #floorToResolution(Wind) floored to the resolution of this set}
     * will be returned instead. If no valid start time can be obtained from the race, <code>Long.MAX_VALUE</code> is
     * returned instead.
     */
    protected TimePoint getFrom() {
        final TimePoint startOfRace = getTrackedRace().getStartOfRace();
        return from == null ? startOfRace == null ? new MillisecondsTimePoint(Long.MAX_VALUE)
                : floorToResolution(startOfRace.minus(TrackedRaceImpl.TIME_BEFORE_START_TO_TRACK_WIND_MILLIS)) : from;
    }

    /**
     * Time point up to and including which the GPS fixes are considered in the race's tracks. Returns the value of
     * {@link #to} unless it is <code>null</code>. In this case, the time point of the
     * {@link TrackedRace#getTimePointOfNewestEvent() time point of the newest event},
     * {@link #ceilingToResolution(Wind) ceiled to the resolution of this set} will be returned instead. If no valid
     * time of a newest event can be obtained from the race, <code>MillisecondsTimePoint(1)</code> is returned instead.
     */
    protected TimePoint getTo() {
        return getToInternal() == null ? getTrackedRace().getTimePointOfNewestEvent() == null ? new MillisecondsTimePoint(1)
                : ceilingToResolution(getTrackedRace().getTimePointOfNewestEvent()) : getToInternal();
    }

    @Override
    public Wind lower(Wind w) {
        TimePoint timePoint = lowerToResolution(w.getTimePoint());
        return timePoint.compareTo(getFrom()) < 0 ? null : getWind(w.getPosition(), timePoint);
    }

    @Override
    public Wind floor(Wind w) {
        TimePoint timePoint = floorToResolution(w.getTimePoint());
        return timePoint.compareTo(getFrom()) < 0 ? null : getWind(w.getPosition(), timePoint);
    }

    @Override
    public Wind ceiling(Wind w) {
        TimePoint timePoint = ceilingToResolution(w.getTimePoint());
        return timePoint.compareTo(getTo()) > 0 ? null : getWind(w.getPosition(), timePoint);
    }

    @Override
    public Wind higher(Wind w) {
        TimePoint timePoint = higherToResolution(w.getTimePoint());
        return timePoint.compareTo(getTo()) > 0 ? null : getWind(w.getPosition(), timePoint);
    }

    @Override
    public Iterator<Wind> iterator() {
        return new Iterator<Wind>() {
            private TimePoint timePoint = getFrom();

            @Override
            public boolean hasNext() {
                return timePoint.compareTo(getTo()) <= 0;
            }

            @Override
            public Wind next() {
                Wind result = floor(createDummyWindFix(timePoint));
                timePoint = new MillisecondsTimePoint(timePoint.asMillis() + getResolutionInMilliseconds());
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public NavigableSet<Wind> descendingSet() {
        return new DescendingNavigableSet<Wind>(this);
    }

    @Override
    public Iterator<Wind> descendingIterator() {
        return new Iterator<Wind>() {
            private TimePoint timePoint = lowerToResolution(getTo());

            @Override
            public boolean hasNext() {
                return timePoint.compareTo(getFrom()) >= 0;
            }

            @Override
            public Wind next() {
                Wind result = floor(new DummyWind(timePoint));
                timePoint = new MillisecondsTimePoint(timePoint.asMillis() - getResolutionInMilliseconds());
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public NavigableSet<Wind> subSet(Wind fromElement, boolean fromInclusive, Wind toElement, boolean toInclusive) {
        return createSubset(getTrack(), getTrackedRace(), fromInclusive ? ceilingToResolution(fromElement.getTimePoint())
                : higherToResolution(fromElement.getTimePoint()), toInclusive ? floorToResolution(toElement.getTimePoint())
                : lowerToResolution(toElement.getTimePoint()));
    }

    /**
     * Create an instance of the same class as <code>this</code> object
     */
    abstract protected NavigableSet<Wind> createSubset(WindTrack track, TrackedRace trackedRace, TimePoint from, TimePoint to);

    @Override
    public NavigableSet<Wind> headSet(Wind toElement, boolean inclusive) {
        return createSubset(getTrack(), getTrackedRace(), /* from */ null,
                inclusive ? ceilingToResolution(toElement.getTimePoint()) : lowerToResolution(toElement.getTimePoint()));
    }

    @Override
    public NavigableSet<Wind> tailSet(Wind fromElement, boolean inclusive) {
        return createSubset(getTrack(), getTrackedRace(), inclusive ? floorToResolution(fromElement.getTimePoint())
                : higherToResolution(fromElement.getTimePoint()),
        /* to */ null);
    }

    @Override
    public SortedSet<Wind> subSet(Wind fromElement, Wind toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<Wind> headSet(Wind toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<Wind> tailSet(Wind fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super Wind> comparator() {
        return WindComparator.INSTANCE;
    }

    @Override
    public Wind first() {
        return floor(new DummyWind(getFrom()));
    }

    @Override
    public Wind last() {
        return ceiling(new DummyWind(getTo()));
    }

    @Override
    public int size() {
        return (int) ((getTo().asMillis() - getFrom().asMillis()) / getResolutionInMilliseconds());
    }

    @Override
    public boolean contains(Object o) {
        boolean result = false;
        if (o instanceof Wind) {
            Wind wind = (Wind) o;
            result = wind.getTimePoint().asMillis() % getResolutionInMilliseconds() == 0
                    && wind.getTimePoint().compareTo(getFrom()) >= 0 && wind.getTimePoint().compareTo(getTo()) < 0;
        }
        return result;
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[size()];
        int i = 0;
        for (Wind w : this) {
            result[i++] = w;
        }
        return result;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Object[] result = a;
        if (result.length < size()) {
            result = new Object[size()];
        }
        int i = 0;
        for (Wind w : this) {
            result[i++] = w;
        }
        @SuppressWarnings("unchecked")
        T[] tResult = (T[]) result;
        return tResult;
    }

    protected TimePoint getToInternal() {
        return to;
    }

    protected DummyWind createDummyWindFix(TimePoint timePoint) {
        return new DummyWind(timePoint);
    }
}

