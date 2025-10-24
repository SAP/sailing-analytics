package com.sap.sailing.domain.tracking.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.CourseChange;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.confidence.Weigher;
import com.sap.sailing.domain.common.confidence.impl.PositionAndTimePointWeigher;
import com.sap.sailing.domain.common.tracking.impl.CompactPositionHelper;
import com.sap.sailing.domain.common.tracking.impl.CompactionNotPossibleException;
import com.sap.sailing.domain.common.tracking.impl.PreciseCompactWindImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactWindImpl;
import com.sap.sailing.domain.confidence.ConfidenceBasedWindAverager;
import com.sap.sailing.domain.confidence.ConfidenceFactory;
import com.sap.sailing.domain.shared.tracking.impl.TrackImpl;
import com.sap.sailing.domain.tracking.WindListener;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.util.MappingIterator;
import com.sap.sse.shared.util.impl.ArrayListNavigableSet;

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
    
    protected final Weigher<Pair<Position, TimePoint>> weigher;
    
    /**
     * Listeners won't be serialized.
     */
    private transient Set<WindListener> listeners;

    /**
     * If {@code true}, {@link Wind} fixes will be compacted in a way that they remain precisely
     * equal to their original, without rounding or range limitations different from those of the
     * original {@link Wind} fix. Lossy compaction, in contrast, may use a more compact form that,
     * however, has stricter limits on ranges and precisions. See {@link CompactPositionHelper} for
     * details on lossy compaction.
     */
    private final boolean losslessCompaction;

    public WindTrackImpl(long millisecondsOverWhichToAverage, boolean useSpeed, String nameForReadWriteLock) {
        this(millisecondsOverWhichToAverage, DEFAULT_BASE_CONFIDENCE, useSpeed, nameForReadWriteLock);
    }
    
    /**
     * Uses lossy compaction. See {@link CompactPositionHelper}. Use {@link #WindTrackImpl(long, double, boolean, String, boolean)}
     * to select lossless compaction.
     * 
     * @param baseConfidence
     *            the confidence to attribute to the raw wind fixes in this track
     * @param useSpeed
     *            whether the wind speed described by the fixes in this track are usable at all; example for an unusable
     *            wind speed would be that of an estimation that only estimates the wind direction and uses some default
     *            value for the speed
     */
    public WindTrackImpl(long millisecondsOverWhichToAverage, double baseConfidence, boolean useSpeed, String nameForReadWriteLock) {
        this(millisecondsOverWhichToAverage, baseConfidence, useSpeed, nameForReadWriteLock, /* losslessCompaction */ false);
    }
    
    /**
     * Uses lossy compaction. See {@link CompactPositionHelper}.
     * 
     * @param baseConfidence
     *            the confidence to attribute to the raw wind fixes in this track
     * @param useSpeed
     *            whether the wind speed described by the fixes in this track are usable at all; example for an unusable
     *            wind speed would be that of an estimation that only estimates the wind direction and uses some default
     *            value for the speed
     * @param losslessCompaction
     *            whether or not to use lossless compaction; see {@link CompactPositionHelper} for details on lossy
     *            compaction
     */
    public WindTrackImpl(long millisecondsOverWhichToAverage, double baseConfidence, boolean useSpeed, String nameForReadWriteLock, boolean losslessCompaction) {
        super(new ArrayListNavigableSet<Timed>(WindComparator.INSTANCE), nameForReadWriteLock);
        this.baseConfidence = baseConfidence;
        this.millisecondsOverWhichToAverage = millisecondsOverWhichToAverage;
        listeners = new HashSet<WindListener>();
        this.useSpeed = useSpeed;
        this.losslessCompaction = losslessCompaction;
        this.weigher = createPositionAndTimePointWeigher();
    }
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        listeners = new HashSet<WindListener>();
    }
    
    /**
     * By default, wind tracks respond with a valid result to {@link #getAveragedWindWithConfidence(Position, TimePoint)},
     * so <code>null</code> is the result of this default implementation.
     */
    @Override
    public Duration getResolutionOutsideOfWhichNoFixWillBeReturned() {
        return null;
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
    public boolean add(Wind fix) {
        return super.add(fix);
    }

    @Override
    public void add(Iterable<Wind> fixesToAdd) {
        final List<Wind> fixesAdded = new ArrayList<>(Util.size(fixesToAdd));
        for (final Wind wind : fixesToAdd) {
            final Wind compactWind = compactify(wind);
            if (super.add(compactWind, /* replace */ false)) {
                fixesAdded.add(compactWind);
            }
        }
        if (!fixesAdded.isEmpty()) {
            notifyListenersAboutReceive(fixesAdded);
        }
    }

    @Override
    public boolean add(Wind wind, boolean replace) {
        final Wind compactWind = compactify(wind);
        final boolean result = super.add(compactWind, replace);
        notifyListenersAboutReceive(compactWind);
        return result;
    }

    private Wind compactify(Wind wind) {
        Wind compactWind;
        try {
            compactWind = losslessCompaction ? new PreciseCompactWindImpl(wind) : new VeryCompactWindImpl(wind);
        } catch (CompactionNotPossibleException e) {
            logger.log(Level.FINE, "Couldn't compact wind fix "+wind+". Using lossless compactification instead.", e);
            compactWind = new PreciseCompactWindImpl(wind);
        }
        return compactWind;
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

    private void notifyListenersAboutReceive(Iterable<Wind> winds) {
        synchronized (listeners) {
            for (WindListener listener : listeners) {
                try {
                    listener.windDataReceived(winds);
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
        final WindWithConfidence<Util.Pair<Position, TimePoint>> estimatedWindUnsynchronized = getAveragedWindUnsynchronized(p, at);
        return estimatedWindUnsynchronized == null ? null : estimatedWindUnsynchronized.getObject();
    }
    
    @Override
    public WindWithConfidence<Util.Pair<Position, TimePoint>> getAveragedWindWithConfidence(Position p, TimePoint at) {
        return getAveragedWindUnsynchronized(p, at);
    }

    /**
     * This method implements the functionality of the {@link #getAveragedWind(Position, TimePoint)} interface method.
     * It does so by collecting wind fixes around the <code>at</code> time point up to half interval length specified by
     * {@link #getMillisecondsOverWhichToAverageWind()} in each direction, starting to count at the first fix found in
     * each direction. If available, at least the fix time-wise closest before and the fix time-wise closest after
     * {@code at} will be picked up, which may lead to an overall interval length that exceeds
     * {@link #getMillisecondsOverWhichToAverageWind()}.
     * <p>
     * 
     * If the track is empty, {@code null} is returned. Otherwise, the wind fixes in the interval constructed are
     * averaged using a {@link ConfidenceBasedWindAverager}.
     * <p>
     * 
     * Not being <code>synchronized</code>, it does not obtain this object's monitor. Subclasses may use this carefully
     * if they can guarantee there are no concurrency issues with the internal fixes while iterating over the result of
     * {@link #getInternalFixes()}.
     * 
     * @param p
     *            if <code>null</code>, the averaged position of the original wind fixes is returned; otherwise,
     *            <code>p</code> is used as the result's position and may be used for confidence determination.
     */
    protected WindWithConfidence<Pair<Position, TimePoint>> getAveragedWindUnsynchronized(Position p, TimePoint at) {
        lockForRead();
        try {
            List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesToAverage = new ArrayList<WindWithConfidence<Util.Pair<Position, TimePoint>>>();
            // don't measure speed with separate confidence; return confidence obtained from averaging bearings
            ConfidenceBasedWindAverager<Pair<Position, TimePoint>> windAverager = ConfidenceFactory.INSTANCE.createWindAverager(weigher);
            Util.Pair<Position, TimePoint> relativeTo = new Util.Pair<Position, TimePoint>(p, at);
            // pick one left if possible and extend to half averaging interval length
            final Iterator<WindWithConfidence<Pair<Position, TimePoint>>> beforeIter = getInternalFixesLimitedHeadSetDescendingIterator(at);
            collectFixesInOneDirection(beforeIter, windFixesToAverage);
            // pick one right if possible and extend to half averaging interval length
            final Iterator<WindWithConfidence<Pair<Position, TimePoint>>> afterIter = getInternalFixesLimitedTailSetIterator(at);
            collectFixesInOneDirection(afterIter, windFixesToAverage);
            if (windFixesToAverage.isEmpty()) {
                return null;
            } else {
                WindWithConfidence<Util.Pair<Position, TimePoint>> average = windAverager.getAverage(windFixesToAverage, relativeTo);
                return average;
            }
        } finally {
            unlockAfterRead();
        }
    }

    protected Iterator<WindWithConfidence<Pair<Position, TimePoint>>> getInternalFixesLimitedHeadSetDescendingIterator(TimePoint endingAt) {
        return new MappingIterator<>(getInternalFixes().headSet(new DummyWind(endingAt), /* inclusive */false).descendingIterator(), wind->createWindWithConfidence(wind));
    }

    protected Iterator<WindWithConfidence<Pair<Position, TimePoint>>> getInternalFixesLimitedTailSetIterator(TimePoint startingAt) {
        return new MappingIterator<>(getInternalFixes().tailSet(new DummyWind(startingAt), /* inclusive */true).iterator(), wind->createWindWithConfidence(wind));
    }

    private void collectFixesInOneDirection(final Iterator<WindWithConfidence<Pair<Position, TimePoint>>> fixIter,
            Collection<WindWithConfidence<Pair<Position, TimePoint>>> windFixesToAverage) {
        long distanceToFirst = 0;
        WindWithConfidence<Pair<Position, TimePoint>> nextWindFix = null;
        final TimePoint firstTimePointInDirection;
        if (fixIter.hasNext()) {
            nextWindFix = fixIter.next();
            firstTimePointInDirection = nextWindFix.getObject().getTimePoint();
            distanceToFirst = Math.abs(firstTimePointInDirection.asMillis() - nextWindFix.getObject().getTimePoint().asMillis());
            windFixesToAverage.add(nextWindFix);
        } else {
            firstTimePointInDirection = null;
        }
        // and extend to at most half the averaging interval
        long newDistanceToFirst;
        if (fixIter.hasNext()) {
            nextWindFix = fixIter.next();
            newDistanceToFirst = Math.abs(firstTimePointInDirection.asMillis() - nextWindFix.getObject().getTimePoint().asMillis());
        } else {
            nextWindFix = null;
            newDistanceToFirst = distanceToFirst;
        }
        // go up to half the interval:
        while (nextWindFix != null && newDistanceToFirst <= getMillisecondsOverWhichToAverageWind()/2) {
            windFixesToAverage.add(nextWindFix);
            distanceToFirst = newDistanceToFirst;
            if (fixIter.hasNext()) {
                nextWindFix = fixIter.next();
                newDistanceToFirst = Math.abs(firstTimePointInDirection.asMillis() - nextWindFix.getObject().getTimePoint().asMillis());
            } else {
                nextWindFix = null;
                newDistanceToFirst = distanceToFirst;
            }
        }
    }
    
    private PositionAndTimePointWeigher createPositionAndTimePointWeigher() {
        return new PositionAndTimePointWeigher(WIND_HALF_CONFIDENCE_DURATION, WIND_HALF_CONFIDENCE_DISTANCE);
    }

    private WindWithConfidenceImpl<Pair<Position, TimePoint>> createWindWithConfidence(Wind wind) {
        return new WindWithConfidenceImpl<Pair<Position, TimePoint>>(wind,
                getConfidenceOfInternalWindFixUnsynchronized(wind),
                /* relativeTo */ new Pair<Position, TimePoint>(wind.getPosition(), wind.getTimePoint()),
                useSpeed);
    }
    
    protected boolean isUseSpeed() {
        return useSpeed;
    }

    /**
     * Gets the confidence for the specified wind fix. Not being <code>synchronized</code>, it does not obtain this
     * object's monitor. Subclasses may use this carefully if they can guarantee there are no concurrency issues with
     * the internal fixes while iterating over the result of {@link #getInternalFixes()}.
     */
    protected double getConfidenceOfInternalWindFixUnsynchronized(Wind windFix) {
        return getBaseConfidence();
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
        private final Position position;
        public DummyWind(TimePoint timePoint) {
            this(timePoint, /* position */ null);
        }
        public DummyWind(TimePoint timePoint, Position position) {
            super(timePoint);
            this.position = position;
        }
        @Override
        public Position getPosition() {
            return position;
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
        public Duration getDuration(Distance distance) {
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
        @Override
        public double getStatuteMilesPerHour() {
            return 0;
        }
        @Override
        public Distance travel(Duration duration) {
            return null;
        }
        @Override
        public double divide(Speed speed) {
            return 0;
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
            // map through "compactify" to account for possible rounding / accuracy differences and still achieve "equals"
            getInternalRawFixes().remove(compactify(wind));
        } finally {
            unlockAfterWrite();
        }
        notifyListenersAboutRemoval(wind);
    }

}
