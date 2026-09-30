package com.sap.sailing.domain.common.tracking.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.AbstractPosition;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.AbstractSpeedWithAbstractBearingImpl;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

/**
 * A compact representation of a GPS fix which collects all primitive-typed attributes in one object to avoid
 * memory overhead otherwise created by using too many individual fine-grained objects.<p>
 * 
 * Objects of this type are assumed to be contained in at most one {@link DynamicGPSFixTrackImpl}. It is
 * therefore permissible to cache information about validity to speed up the otherwise expensive
 * {@link DynamicGPSFixTrackImpl#isValid(PartialNavigableSetView, GPSFix)} computation.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class VeryCompactGPSFixImpl extends AbstractCompactGPSFixImpl {
    private static final long serialVersionUID = 3943572107870245437L;

    private static final Logger logger = Logger.getLogger(VeryCompactGPSFixImpl.class.getName());
    
    /**
     * See {@link CompactPositionHelper}
     */
    private final int latDegScaled;

    /**
     * See {@link CompactPositionHelper}
     */
    private final int lngDegScaled;
    
    /**
     * When <code>{@link #whatIsCached}&amp;{@link #IS_ESTIMATED_SPEED_CACHED} != 0</code>, this field tells the estimated speed's
     * true "bearing" (true course over ground) in degrees, scaled into a short value using {@link CompactPositionHelper}.
     */
    private short cachedEstimatedSpeedBearingInDegreesScaled;

    /**
     * When <code>{@link #whatIsCached}&amp;{@link #IS_ESTIMATED_SPEED_CACHED} != 0</code>, this field tells the estimated speed
     * in knots, scaled into a short value using {@link CompactPositionHelper}.
     */
    private short cachedEstimatedSpeedInKnotsScaled;
    
    public class CompactPosition extends AbstractPosition {
        private static final long serialVersionUID = 5621506820766614178L;

        @Override
        public double getLatDeg() {
            return CompactPositionHelper.getLatDeg(latDegScaled);
        }

        @Override
        public double getLngDeg() {
            return CompactPositionHelper.getLngDeg(lngDegScaled);
        }
    }
    
    private class VeryCompactEstimatedSpeedBearing extends AbstractBearing {
        private static final long serialVersionUID = 8549231429037883121L;

        @Override
        public double getDegrees() {
            return CompactPositionHelper.getDegreeBearing(cachedEstimatedSpeedBearingInDegreesScaled);
        }
    }
    
    private class VeryCompactEstimatedSpeed extends AbstractSpeedWithAbstractBearingImpl {
        private static final long serialVersionUID = -5871855443391817248L;

        @Override
        public Bearing getBearing() {
            return new VeryCompactEstimatedSpeedBearing();
        }

        @Override
        public double getKnots() {
            return CompactPositionHelper.getKnotSpeed(cachedEstimatedSpeedInKnotsScaled);
        }
    }
    
    public VeryCompactGPSFixImpl(Position position, TimePoint timePoint) {
        super(timePoint);
        latDegScaled = CompactPositionHelper.getLatDegScaled(position);
        lngDegScaled = CompactPositionHelper.getLngDegScaled(position);
    }

    public VeryCompactGPSFixImpl(GPSFix gpsFix) {
        this(gpsFix.getPosition(), gpsFix.getTimePoint());
    }

    @Override
    public Position getPosition() {
        return new CompactPosition();
    }

    @Override
    public SpeedWithBearing getCachedEstimatedSpeed() {
        assert isEstimatedSpeedCached();
        return new VeryCompactEstimatedSpeed();
    }

    /**
     * Under rare circumstances, caching the speed may fail for the compact representation of a GPS fix.
     * This can happen if the speed estimated exceeds the range that can be represented in this compact
     * form which is less than in the original form where the speed amount is represented as a {@code double}
     * value. In this case, a warning message is logged with level {@link Level#FINER}, and the speed remains
     * uncached.
     */
    @Override
    public synchronized SpeedWithBearing cacheEstimatedSpeed(SpeedWithBearing estimatedSpeed) {
        try {
            cachedEstimatedSpeedInKnotsScaled = CompactPositionHelper.getKnotSpeedScaled(estimatedSpeed);
            cachedEstimatedSpeedBearingInDegreesScaled = CompactPositionHelper.getDegreeBearingScaled(estimatedSpeed.getBearing());
            super.cacheEstimatedSpeed(estimatedSpeed);
            final SpeedWithBearing veryCompactEstimatedSpeed = getCachedEstimatedSpeed();
            return new KnotSpeedWithBearingImpl(veryCompactEstimatedSpeed.getKnots(),
                    new DegreeBearingImpl(veryCompactEstimatedSpeed.getBearing().getDegrees()));
        } catch (CompactionNotPossibleException e) {
            logger.log(Level.FINER, "Cannot cache estimated speed "+estimatedSpeed+" in compact fix:", e);
            return estimatedSpeed;
        }
    }
}
