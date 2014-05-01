package com.sap.sailing.domain.tracking.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Timed;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.CourseChange;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.confidence.ConfidenceBasedWindAverager;
import com.sap.sailing.domain.confidence.ConfidenceFactory;
import com.sap.sailing.domain.confidence.impl.PositionAndTimePointWeigher;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindListener;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.util.impl.ArrayListNavigableSet;

/**
 * Records {@link Wind} objects over time and offers to average the last so many of them into an
 * estimated, stabilized wind bearing/direction.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class WindTrackImpl extends TrackImpl<Wind> implements WindTrack {
    private static final long serialVersionUID = 6882509533928049084L;

    private final static Logger logger = Logger.getLogger(WindTrackImpl.class.getName());
    
    protected final static double DEFAULT_BASE_CONFIDENCE = 0.9;
    
    private final double baseConfidence;
    
    private long millisecondsOverWhichToAverage;
    
    private final boolean useSpeed;
    
    /**
     * Listeners won't be serialized.
     */
    private transient Set<WindListener> listeners;

    public WindTrackImpl(long millisecondsOverWhichToAverage, boolean useSpeed, String nameForReadWriteLock) {
        this(millisecondsOverWhichToAverage, DEFAULT_BASE_CONFIDENCE, useSpeed, nameForReadWriteLock);
    }
    
    /**
     * @param baseConfidence
     *            the confidence to attribute to the raw wind fixes in this track
     * @param useSpeed
     *            whether the wind speed described by the fixes in this track are usable at all; example for an unusable
     *            wind speed would be that of an estimation that only estimates the wind direction and uses some default
     *            value for the speed
     */
    public WindTrackImpl(long millisecondsOverWhichToAverage, double baseConfidence, boolean useSpeed, String nameForReadWriteLock) {
        super(new ArrayListNavigableSet<Timed>(WindComparator.INSTANCE), nameForReadWriteLock);
        this.baseConfidence = baseConfidence;
        this.millisecondsOverWhichToAverage = millisecondsOverWhichToAverage;
        listeners = new HashSet<WindListener>();
        this.useSpeed = useSpeed;
    }
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        listeners = new HashSet<WindListener>();
    }
    
    @Override
    protected Wind getDummyFix(TimePoint timePoint) {
        return new DummyWind(timePoint);
    }

    @Override
    public void setMillisecondsOverWhichToAverage(long millisecondsOverWhichToAverage) {
        long oldMillis = millisecondsOverWhichToAverage;
        this.millisecondsOverWhichToAverage = millisecondsOverWhichToAverage;
        notifyListenersAboutAveragingChange(oldMillis, millisecondsOverWhichToAverage);
    }
    
    @Override
    public long getMillisecondsOverWhichToAverageWind() {
        return millisecondsOverWhichToAverage;
    }

    @Override
    public boolean add(Wind wind) {
        final CompactWindImpl compactWind = new CompactWindImpl(wind);
        final boolean result = super.add(compactWind);
        notifyListenersAboutReceive(compactWind);
        return result;
    }

    private void notifyListenersAboutReceive(Wind wind) {
        synchronized (listeners) {
            for (WindListener listener : listeners) {
                try {
                    listener.windDataReceived(wind);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "WindListener " + listener + " threw exception " + e.getMessage());
                    logger.log(Level.SEVERE, "notifyListenersAboutReceive(Wind)", e);
                }
            }
        }
    }

    private void notifyListenersAboutAveragingChange(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        synchronized (listeners) {
            for (WindListener listener : listeners) {
                try {
                    listener.windAveragingChanged(oldMillisecondsOverWhichToAverage, newMillisecondsOverWhichToAverage);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "WindListener " + listener + " threw exception " + e.getMessage());
                    logger.log(Level.SEVERE, "notifyListenersAboutAveragingChange(long, long)", e);
                }
            }
        }
    }

    private void notifyListenersAboutRemoval(Wind wind) {
        synchronized (listeners) {
            for (WindListener listener : listeners) {
                try {
                    listener.windDataRemoved(wind);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "WindListener " + listener + " threw exception " + e.getMessage());
                    logger.log(Level.SEVERE, "notifyListenersAboutRemoval(Wind)", e);
                }
            }
        }
    }

    /**
     * Tries to find at least {@link #millisecondsOverWhichToAverage} worth of wind data. The algorithm
     * oscillates in the wind track around <code>at</code>, starting with fixes left and right that are
     * closest to <code>at</code>. In each direction, left and right, we count the interval covered so far,
     * starting at the fix nearest to <code>at</code>. We progress on the side where we are still closer to
     * <code>at</code>, unless there are no more fixes in that direction. From all fixes found this way,
     * the speed average and the bearing average are computed separately and combined into a new {@link Wind}
     * object.
     */
    @Override
    public Wind getAveragedWind(Position p, TimePoint at) {
        final WindWithConfidence<Pair<Position, TimePoint>> estimatedWindUnsynchronized = getAveragedWindUnsynchronized(p, at);
        return estimatedWindUnsynchronized == null ? null : estimatedWindUnsynchronized.getObject();
    }
    
    @Override
    public WindWithConfidence<Pair<Position, TimePoint>> getAveragedWindWithConfidence(Position p, TimePoint at) {
        return getAveragedWindUnsynchronized(p, at);
    }

    /**
     * This method implements the functionality of the {@link #getAveragedWind(Position, TimePoint)} interface method.
     * It does so by collecting (smoothened, outliers removed)) wind fixes around the <code>at</code> time point up to
     * an interval length as specified by {@link #getMillisecondsOverWhichToAverageWind()}. At least one fix that is
     * closest to <code>at</code> will be picked up. If the track is empty, <code>null</code> is returned. Otherwise,
     * the wind fixes in the interval constructed are averaged using a {@link ConfidenceBasedWindAverager}.
     * 
     * <p>
     * However, not being <code>synchronized</code>, it does not obtain this object's monitor. Subclasses may use this
     * carefully if they can guarantee there are no concurrency issues with the internal fixes while iterating over the
     * result of {@link #getInternalFixes()}.
     * 
     * @param p
     *            if <code>null</code>, the averaged position of the original wind fixes is returned; otherwise,
     *            <code>p</code> is used as the result's position and may be used for confidence determination.
     */
    protected WindWithConfidence<Pair<Position, TimePoint>> getAveragedWindUnsynchronized(Position p, TimePoint at) {
        lockForRead();
        try {
            List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesToAverage = new ArrayList<WindWithConfidence<Pair<Position, TimePoint>>>();
            // don't measure speed with separate confidence; return confidence obtained from averaging bearings
            ConfidenceBasedWindAverager<Pair<Position, TimePoint>> windAverager = ConfidenceFactory.INSTANCE
                    .createWindAverager(new PositionAndTimePointWeigher(
                    /* halfConfidenceAfterMilliseconds */getMillisecondsOverWhichToAverageWind() / 10));
            DummyWind atTimed = new DummyWind(at);
            Pair<Position, TimePoint> relativeTo = new Pair<Position, TimePoint>(p, at);
            NavigableSet<Wind> beforeSet = getInternalFixes().headSet(atTimed, /* inclusive */false);
            NavigableSet<Wind> afterSet = getInternalFixes().tailSet(atTimed, /* inclusive */true);
            Iterator<Wind> beforeIter = beforeSet.descendingIterator();
            Iterator<Wind> afterIter = afterSet.iterator();
            long beforeDistanceToAt = 0;
            long afterDistanceToAt = 0;
            TimePoint beforeIntervalEnd = null;
            TimePoint afterIntervalStart = null;
            long beforeIntervalLength = 0;
            long afterIntervalLength = 0;
            Wind beforeWind = null;
            if (beforeIter.hasNext()) {
                beforeWind = beforeIter.next();
                beforeDistanceToAt = at.asMillis() - beforeWind.getTimePoint().asMillis();
            }
            Wind afterWind = null;
            if (afterIter.hasNext()) {
                afterWind = afterIter.next();
                afterDistanceToAt = afterWind.getTimePoint().asMillis() - at.asMillis();
            }
            do {
                if (beforeWind != null && (beforeDistanceToAt <= afterDistanceToAt || afterWind == null)) {
                    windFixesToAverage.add(new WindWithConfidenceImpl<Pair<Position, TimePoint>>(beforeWind,
                            getBaseConfidence(), new Pair<Position, TimePoint>(beforeWind.getPosition(), beforeWind
                                    .getTimePoint()), useSpeed));
                    if (beforeIntervalEnd == null) {
                        beforeIntervalEnd = beforeWind.getTimePoint();
                    }
                    if (beforeIter.hasNext()) {
                        beforeWind = beforeIter.next();
                        beforeDistanceToAt = at.asMillis() - beforeWind.getTimePoint().asMillis();
                        beforeIntervalLength = beforeIntervalEnd.asMillis() - beforeWind.getTimePoint().asMillis();
                    } else {
                        beforeWind = null;
                    }
                } else if (afterWind != null) {
                    windFixesToAverage.add(new WindWithConfidenceImpl<Pair<Position, TimePoint>>(afterWind,
                            getBaseConfidence(), new Pair<Position, TimePoint>(afterWind.getPosition(), afterWind
                                    .getTimePoint()), useSpeed));
                    if (afterIntervalStart == null) {
                        afterIntervalStart = afterWind.getTimePoint();
                    }
                    if (afterIter.hasNext()) {
                        afterWind = afterIter.next();
                        afterDistanceToAt = afterWind.getTimePoint().asMillis() - at.asMillis();
                        afterIntervalLength = afterWind.getTimePoint().asMillis() - afterIntervalStart.asMillis();
                    } else {
                        afterWind = null;
                    }
                }
            } while (beforeIntervalLength + afterIntervalLength < getMillisecondsOverWhichToAverageWind()
                    && (beforeWind != null || afterWind != null));
            if (windFixesToAverage.isEmpty()) {
                return null;
            } else {
                WindWithConfidence<Pair<Position, TimePoint>> average = windAverager.getAverage(windFixesToAverage, relativeTo);
                return average;
            }
        } finally {
            unlockAfterRead();
        }
    }

    /**
     * The base confidence attributed to this track. 1.0 would mean that the individual fixes stored by this track
     * represent <em>the truth</em>. 0.0 means "no relevance at all."
     */
    private double getBaseConfidence() {
        return baseConfidence;
    }
    
    @Override
    public String toString() {
        lockForRead();
        try {
            StringBuilder result = new StringBuilder();
            synchronized (this) {
                for (Wind wind : getRawFixes()) {
                    result.append(wind);
                    result.append(" avg(");
                    result.append(getMillisecondsOverWhichToAverageWind());
                    if (wind == null) {
                        result.append("ms)");
                    } else {
                        result.append("ms): ");
                        result.append(getAveragedWind(wind.getPosition(), wind.getTimePoint()));
                    }
                    result.append("\n");
                }
            }
            return result.toString();
        } finally {
            unlockAfterRead();
        }
    }
    
    public String toCSV() {
        lockForRead();
        try {
            StringBuilder result = new StringBuilder();
            synchronized (this) {
                for (Wind wind : getRawFixes()) {
                    append(result, wind);
                    Wind estimate = getAveragedWind(wind.getPosition(), wind.getTimePoint());
                    append(result, estimate);
                    result.append("\n");
                }
            }
            return result.toString();
        } finally {
            unlockAfterRead();
        }
    }

    private void append(StringBuilder result, Wind wind) {
        result.append(wind.getTimePoint().asMillis());
        result.append("\t");
        result.append(wind.getKnots());
        result.append("\t");
        result.append(wind.getFrom().getDegrees());
        result.append("\t");
    }

    protected static class DummyWind extends DummyTimed implements Wind {
        private static final long serialVersionUID = -311172509910032149L;
        public DummyWind(TimePoint timePoint) {
            super(timePoint);
        }
        @Override
        public Position getPosition() {
            return null;
        }
        @Override
        public Bearing getBearing() {
            return null;
        }
        @Override
        public Position travelTo(Position pos, TimePoint from, TimePoint to) {
            return null;
        }
        @Override
        public double getKnots() {
            return 0;
        }
        @Override
        public double getMetersPerSecond() {
            return 0;
        }
        @Override
        public double getKilometersPerHour() {
            return 0;
        }
        @Override
        public Distance travel(TimePoint from, TimePoint to) {
            return null;
        }
        @Override
        public SpeedWithBearing add(SpeedWithBearing other) {
            return null;
        }
        @Override
        public int compareTo(Speed o) {
            return 0;
        }
        @Override
        public Bearing getFrom() {
            return null;
        }
        @Override
        public double getBeaufort() {
            return 0;
        }
        @Override
        public SpeedWithBearing applyCourseChange(CourseChange courseChange) {
            return null;
        }
        @Override
        public CourseChange getCourseChangeRequiredToReach(SpeedWithBearing targetSpeedWithBearing) {
            return null;
        }
        @Override
        public Speed projectTo(Position position, Bearing bearing) {
            return null;
        }
    }

    @Override
    public void addListener(WindListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void remove(Wind wind) {
        lockForWrite();
        try {
            getInternalRawFixes().remove(wind);
        } finally {
            unlockAfterWrite();
        }
        notifyListenersAboutRemoval(wind);
    }

}
