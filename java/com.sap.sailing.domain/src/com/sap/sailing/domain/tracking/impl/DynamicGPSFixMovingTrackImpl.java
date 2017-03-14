package com.sap.sailing.domain.tracking.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.impl.SpeedWithBearingWithConfidenceImpl;
import com.sap.sailing.domain.base.impl.SpeedWithConfidenceImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.confidence.BearingWithConfidence;
import com.sap.sailing.domain.common.confidence.BearingWithConfidenceCluster;
import com.sap.sailing.domain.common.confidence.ConfidenceBasedAverager;
import com.sap.sailing.domain.common.confidence.ConfidenceFactory;
import com.sap.sailing.domain.common.confidence.HasConfidence;
import com.sap.sailing.domain.common.confidence.Weigher;
import com.sap.sailing.domain.common.confidence.impl.BearingWithConfidenceImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.CompactPositionHelper;
import com.sap.sailing.domain.common.tracking.impl.CompactionNotPossibleException;
import com.sap.sailing.domain.common.tracking.impl.PreciseCompactGPSFixMovingImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sse.common.TimePoint;

public class DynamicGPSFixMovingTrackImpl<ItemType> extends GPSFixTrackImpl<ItemType, GPSFixMoving> implements DynamicGPSFixTrack<ItemType, GPSFixMoving> {
    private static final Logger logger = Logger.getLogger(DynamicGPSFixMovingTrackImpl.class.getName());
    
    private static final long serialVersionUID = 9111448573301259784L;
    private static final double MAX_SPEED_FACTOR_COMPARED_TO_MEASURED_SPEED_FOR_FILTERING = 2;

    /**
     * Uses lossy compaction of fixes. See {@link CompactPositionHelper}. Use {@link #DynamicGPSFixMovingTrackImpl(Object, long, boolean)}
     * to configure lossless compaction.
     */
    public DynamicGPSFixMovingTrackImpl(ItemType trackedItem, long millisecondsOverWhichToAverage) {
        this(trackedItem, millisecondsOverWhichToAverage, /* losslessCompaction */ false);
    }
    
    /**
     * Uses lossy compaction. Use {@link #DynamicGPSFixMovingTrackImpl(Object, long, Speed, boolean)} to configure
     * lossless compaction.
     * 
     * @param maxSpeedForSmoothening
     *            pass <code>null</code> if you don't want speed-based smoothening
     */
    public DynamicGPSFixMovingTrackImpl(ItemType trackedItem, long millisecondsOverWhichToAverage, Speed maxSpeedForSmoothening) {
        this(trackedItem, millisecondsOverWhichToAverage, maxSpeedForSmoothening, /* losslessCompaction */ false);
    }

    /**
     * @param maxSpeedForSmoothening
     *            pass <code>null</code> if you don't want speed-based smoothening
     * @param losslessCompaction
     *            If {@code true}, {@link Wind} fixes will be compacted in a way that they remain precisely equal to
     *            their original, without rounding or range limitations different from those of the original
     *            {@link Wind} fix. Lossy compaction, in contrast, may use a more compact form that, however, has
     *            stricter limits on ranges and precisions. See {@link CompactPositionHelper} for details on lossy
     *            compaction.
     */
    public DynamicGPSFixMovingTrackImpl(ItemType trackedItem, long millisecondsOverWhichToAverage, Speed maxSpeedForSmoothening, boolean losslessCompaction) {
        super(trackedItem, millisecondsOverWhichToAverage, maxSpeedForSmoothening, losslessCompaction);
    }

    /**
     * @param losslessCompaction
     *            If {@code true}, {@link Wind} fixes will be compacted in a way that they remain precisely equal to
     *            their original, without rounding or range limitations different from those of the original
     *            {@link Wind} fix. Lossy compaction, in contrast, may use a more compact form that, however, has
     *            stricter limits on ranges and precisions. See {@link CompactPositionHelper} for details on lossy
     *            compaction.
     */
    public DynamicGPSFixMovingTrackImpl(ItemType trackedItem, long millisecondsOverWhichToAverage, boolean losslessCompaction) {
        super(trackedItem, millisecondsOverWhichToAverage, losslessCompaction);
    }

    /**
     * This redefinition packs the <code>gpsFix</code> into a more compact representation that conserves
     * memory compared to the original, "naive" implementation. It gets along with a single object.
     */
    @Override
    public void addGPSFix(GPSFixMoving gpsFix) {
        add(gpsFix, /* replace */ true);
    }
    
    @Override
    public boolean add(GPSFixMoving fix) {
        return super.add(fix); // ends up calling this.add(fix, false) where conversion in CompactGPSFixMovingImpl will happen
    }

    @Override
    public boolean add(GPSFixMoving fix, boolean replace) {
        GPSFixMoving compactFix;
        try {
            compactFix = isLosslessCompaction() ? new PreciseCompactGPSFixMovingImpl(fix) : new VeryCompactGPSFixMovingImpl(fix);
        } catch (CompactionNotPossibleException e) {
            logger.log(Level.FINE, "Couldn't compact fix "+fix+". Using losslessly-compacted fix instead.", e);
            compactFix = new PreciseCompactGPSFixMovingImpl(fix);
        }
        return super.add(compactFix, replace);
    }

    /**
     * Interpolates linearly between the two fixes based on their time difference and distance. This
     * intentionally ignores the {@link GPSFixMoving#getSpeed() speed values} provided by the fixes
     * themselves for performance reasons.
     */
    @Override
    protected Position getEstimatedPositionBetweenTwoValidFixes(TimePoint timePoint, GPSFixMoving lastFixAtOrBefore, GPSFixMoving firstFixAtOrAfter) {
        assert lastFixAtOrBefore != null;
        assert firstFixAtOrAfter != null;
        assert !timePoint.before(lastFixAtOrBefore.getTimePoint());
        assert !timePoint.after(firstFixAtOrAfter.getTimePoint());
        final Position result;
        final SpeedWithBearing estimatedSpeed = estimateSpeedOnTimeDifferenceAndDistanceOnly(lastFixAtOrBefore, firstFixAtOrAfter);
        Distance distance = estimatedSpeed.travel(lastFixAtOrBefore.getTimePoint(), timePoint);
        result = lastFixAtOrBefore.getPosition().translateGreatCircle(
                estimatedSpeed.getBearing(), distance);
        return result;
    }

    private SpeedWithBearing estimateSpeedOnTimeDifferenceAndDistanceOnly(GPSFixMoving fix1, GPSFixMoving fix2) {
        assert fix1 != null && fix2 != null;
        final Distance distance = fix1.getPosition().getDistance(fix2.getPosition());
        return new KnotSpeedWithBearingImpl(distance.inTime(fix1.getTimePoint().until(fix2.getTimePoint())).getKnots(),
                fix1.getPosition().getBearingGreatCircle(fix2.getPosition()));
    }

    @Override
    protected SpeedWithBearingWithConfidence<TimePoint> getEstimatedSpeed(TimePoint at,
            NavigableSet<GPSFixMoving> fixesToUseForSpeedEstimation, Weigher<TimePoint> weigher) {
        lockForRead();
        try {
            List<GPSFixMoving> relevantFixes = getFixesRelevantForSpeedEstimation(at, fixesToUseForSpeedEstimation);
            List<SpeedWithConfidence<TimePoint>> speeds = new ArrayList<SpeedWithConfidence<TimePoint>>();
            BearingWithConfidenceCluster<TimePoint> bearingCluster = new BearingWithConfidenceCluster<TimePoint>(weigher);
            if (!relevantFixes.isEmpty()) {
                int i=0;
                GPSFixMoving last = relevantFixes.get(i);
                // if speed is within reasonable bounds, add fix's own speed/bearing; this also works if only one
                // "relevant" fix is found; exclude SOG/COG of fixes with SOG/COG==0/0
                if ((last.getSpeed().getBearing().getDegrees() != 0 || last.getSpeed().getKnots() > 0) && (maxSpeedForSmoothing == null || last.getSpeed().compareTo(maxSpeedForSmoothing) <= 0)) {
                    SpeedWithConfidenceImpl<TimePoint> speedWithConfidence = new SpeedWithConfidenceImpl<TimePoint>(
                            last.getSpeed(),
                            /* original confidence */0.9, last.getTimePoint());
                    speeds.add(speedWithConfidence);
                    bearingCluster.add(new BearingWithConfidenceImpl<TimePoint>(last.getSpeed().getBearing(), /* confidence */
                    0.9, last.getTimePoint()));
                }
                while (i<relevantFixes.size()-1) {
                    // add to average the position and time difference
                    GPSFixMoving next = relevantFixes.get(++i);
                    aggregateSpeedAndBearingFromLastToNext(speeds, bearingCluster, last, next);
                    // add to average the speed and bearing provided by the GPSFixMoving
                    // if speed is within reasonable bounds, add fix's own speed/bearing; this also works if only one
                    // "relevant" fix is found; exclude announced SOG/COG if 0/0
                    if ((last.getSpeed().getBearing().getDegrees() != 0 || last.getSpeed().getKnots() > 0)
                            && (maxSpeedForSmoothing == null || next.getSpeed().compareTo(maxSpeedForSmoothing) <= 0)) {
                        SpeedWithConfidenceImpl<TimePoint> computedSpeedWithConfidence = new SpeedWithConfidenceImpl<TimePoint>(
                                next.getSpeed(), /* original confidence */0.9, next.getTimePoint());
                        speeds.add(computedSpeedWithConfidence);
                        bearingCluster.add(new BearingWithConfidenceImpl<TimePoint>(next.getSpeed().getBearing(), /* confidence */
                        0.9, next.getTimePoint()));
                    }
                    last = next;
                }
            }
            ConfidenceBasedAverager<Double, Speed, TimePoint> speedAverager = ConfidenceFactory.INSTANCE
                    .createAverager(weigher);
            HasConfidence<Double, Speed, TimePoint> speedWithConfidence = speedAverager.getAverage(speeds, at);
            BearingWithConfidence<TimePoint> bearingAverage = bearingCluster.getAverage(at);
            Bearing bearing = bearingAverage == null ? null : bearingAverage.getObject();
            SpeedWithBearing avgSpeed = (speedWithConfidence == null || bearing == null) ? null
                    : new KnotSpeedWithBearingImpl(speedWithConfidence.getObject().getKnots(), bearing);
            SpeedWithBearingWithConfidence<TimePoint> result = speedWithConfidence == null || bearingAverage == null ? null
                    : new SpeedWithBearingWithConfidenceImpl<TimePoint>(
                            avgSpeed,
                            /* confidence */((speedWithConfidence == null ? 0.0 : speedWithConfidence.getConfidence()) + (bearingAverage == null ? 0.0
                                    : bearingAverage.getConfidence())) / 2., at);
            return result;
        } finally {
            unlockAfterRead();
        }
    }
    
    /**
     * In addition to the base class implementation, we may have the speed and bearing as measured by the device (the
     * special speed/bearing combination 0.0/0.0 is simply ignored, as are fix-provided speed values that exceed
     * {@link #maxSpeedForSmoothing}). If the adjacent fixes are within the averaging interval defined by
     * {@link GPSFixTrackImpl#getMillisecondsOverWhichToAverageSpeed()}, we use the device-measured speed and compare it
     * with the speed computed based on the time stamp and distance between previous and next fix. If the ratio between
     * the higher and the lower of the two speeds exceeds
     * {@link #MAX_SPEED_FACTOR_COMPARED_TO_MEASURED_SPEED_FOR_FILTERING}, the fix is considered invalid.
     */
    @Override
    protected boolean isValid(NavigableSet<GPSFixMoving> rawFixes, GPSFixMoving e) {
        assertReadLock();
        final boolean isValid;
        if (e.isValidityCached()) {
            isValid = e.isValidCached();
        } else {
            boolean fixHasValidSogAndCog = (e.getSpeed().getMetersPerSecond() != 0.0 || e.getSpeed().getBearing().getDegrees() != 0.0) &&
                    (maxSpeedForSmoothing == null || e.getSpeed().compareTo(maxSpeedForSmoothing) <= 0);

            GPSFixMoving previous = rawFixes.lower(e);
            final boolean atLeastOnePreviousFixInRange = previous != null && e.getTimePoint().asMillis() - previous.getTimePoint().asMillis() <= getMillisecondsOverWhichToAverageSpeed();
            Speed speedToPrevious = null;
            boolean foundValidPreviousFixInRange = false;
            while (previous != null && !foundValidPreviousFixInRange && e.getTimePoint().asMillis() - previous.getTimePoint().asMillis() <= getMillisecondsOverWhichToAverageSpeed()) {
                speedToPrevious = previous.getPosition().getDistance(e.getPosition())
                        .inTime(e.getTimePoint().asMillis() - previous.getTimePoint().asMillis());
                final double speedToPreviousFactor;
                if (speedToPrevious.getMetersPerSecond() >= e.getSpeed().getMetersPerSecond()) {
                    speedToPreviousFactor = speedToPrevious.getMetersPerSecond() / e.getSpeed().getMetersPerSecond();
                } else {
                    speedToPreviousFactor = e.getSpeed().getMetersPerSecond() / speedToPrevious.getMetersPerSecond();
                }
                foundValidPreviousFixInRange = (maxSpeedForSmoothing == null || speedToPrevious.compareTo(maxSpeedForSmoothing) <= 0)
                        && (!fixHasValidSogAndCog || speedToPreviousFactor <= MAX_SPEED_FACTOR_COMPARED_TO_MEASURED_SPEED_FOR_FILTERING);
                previous = rawFixes.lower(previous);
            }
            boolean foundValidNextFixInRange = false;
            boolean atLeastOneNextFixInRange = false;
            // only spend the effort to calculate the "next"-related predicate if the "previous"-related part of the disjunction below isn't already false
            if (!atLeastOnePreviousFixInRange || foundValidPreviousFixInRange) {
                GPSFixMoving next = rawFixes.higher(e);
                atLeastOneNextFixInRange = next != null && next.getTimePoint().asMillis() - e.getTimePoint().asMillis() <= getMillisecondsOverWhichToAverageSpeed();
                Speed speedToNext = null;
                while (next != null && !foundValidNextFixInRange && next.getTimePoint().asMillis() - e.getTimePoint().asMillis() <= getMillisecondsOverWhichToAverageSpeed()) {
                    speedToNext = e.getPosition().getDistance(next.getPosition())
                            .inTime(next.getTimePoint().asMillis() - e.getTimePoint().asMillis());
                    final double speedToNextFactor;
                    if (speedToNext.getMetersPerSecond() >= e.getSpeed().getMetersPerSecond()) {
                        speedToNextFactor = speedToNext.getMetersPerSecond() / e.getSpeed().getMetersPerSecond();
                    } else {
                        speedToNextFactor = e.getSpeed().getMetersPerSecond() / speedToNext.getMetersPerSecond();
                    }
                    foundValidNextFixInRange = (maxSpeedForSmoothing == null || speedToNext.compareTo(maxSpeedForSmoothing) <= 0)
                            && (!fixHasValidSogAndCog || speedToNextFactor <= MAX_SPEED_FACTOR_COMPARED_TO_MEASURED_SPEED_FOR_FILTERING);
                    next = rawFixes.higher(next);
                }
            }
            isValid = (!atLeastOnePreviousFixInRange || foundValidPreviousFixInRange) && (!atLeastOneNextFixInRange || foundValidNextFixInRange);
            e.cacheValidity(isValid);
        }
        return isValid;
    }

    @Override
    public void setMillisecondsOverWhichToAverage(long millisecondsOverWhichToAverage) {
        super.setMillisecondsOverWhichToAverage(millisecondsOverWhichToAverage);
    }
    
}
