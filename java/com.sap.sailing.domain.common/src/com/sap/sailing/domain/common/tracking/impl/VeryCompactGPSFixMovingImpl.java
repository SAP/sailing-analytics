package com.sap.sailing.domain.common.tracking.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
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
 * A memory-conserving representation of a {@link GPSFixMoving} object that produces the fine-grained
 * objects for {@link Position}, {@link SpeedWithBearing}, {@link Bearing} and {@link TimePoint} dynamically
 * as thin wrappers around this object which holds all elementary attributes required. This saves several
 * object references and object headers.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class VeryCompactGPSFixMovingImpl extends AbstractCompactGPSFixMovingImpl {
    private static final long serialVersionUID = 3977983319207618335L;
    
    private static final Logger logger = Logger.getLogger(VeryCompactGPSFixMovingImpl.class.getName());

    /**
     * See {@link CompactPositionHelper}
     */
    private final int latDegScaled;

    /**
     * See {@link CompactPositionHelper}
     */
    private final int lngDegScaled;
    
    /**
     * See {@link CompactPositionHelper}
     */
    private final short speedInKnotsScaled;

    /**
     * See {@link CompactPositionHelper}
     */
    private final short degreeBearingScaled;
    
    /**
     * See {@link CompactPositionHelper}; valid if and only if {@link #trueHeadingDegreesSet} is {@code true}.
     */
    private final short trueHeadingDegreesScaled;
    
    private final boolean trueHeadingDegreesSet;
    
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

    public class VeryCompactPosition extends AbstractPosition {
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
    
    public class VeryCompactSpeedWithBearing extends AbstractCompactSpeedWithBearing {
        private static final long serialVersionUID = 1802065090733146728L;

        @Override
        public double getKnots() {
            return CompactPositionHelper.getKnotSpeed(speedInKnotsScaled);
        }

        @Override
        public Bearing getBearing() {
            return new VeryCompactBearing();
        }
    }
    
    private class VeryCompactBearing extends AbstractBearing {
        private static final long serialVersionUID = 8167886382067060570L;

        @Override
        public double getDegrees() {
            return CompactPositionHelper.getDegreeBearing(degreeBearingScaled);
        }
    }
    
    private class VeryCompactTrueHeading extends AbstractBearing {
        private static final long serialVersionUID = 1130980861113826462L;

        @Override
        public double getDegrees() {
            return CompactPositionHelper.getDegreeBearing(trueHeadingDegreesScaled);
        }
    }
    
    private class VeryCompactEstimatedSpeedBearing extends AbstractBearing {
        private static final long serialVersionUID = 8549231429037883121L;

        @Override
        public double getDegrees() {
            return CompactPositionHelper.getDegreeBearing(cachedEstimatedSpeedBearingInDegreesScaled);
        }
    }

    public class VeryCompactEstimatedSpeed extends AbstractSpeedWithAbstractBearingImpl {
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
    
    public VeryCompactGPSFixMovingImpl(Position position, TimePoint timePoint, SpeedWithBearing speed, Bearing optionalTrueHeading) throws CompactionNotPossibleException {
        super(timePoint);
        latDegScaled = CompactPositionHelper.getLatDegScaled(position);
        lngDegScaled = CompactPositionHelper.getLngDegScaled(position);
        if (speed == null) {
            speedInKnotsScaled = 0;
            degreeBearingScaled = 0;
        } else {
            speedInKnotsScaled = CompactPositionHelper.getKnotSpeedScaled(speed);
            degreeBearingScaled = CompactPositionHelper.getDegreeBearingScaled(speed.getBearing());
        }
        if (optionalTrueHeading == null) {
            trueHeadingDegreesSet = false;
            trueHeadingDegreesScaled = 0;
        } else {
            trueHeadingDegreesSet = true;
            trueHeadingDegreesScaled = CompactPositionHelper.getDegreeBearingScaled(optionalTrueHeading);
        }
    }
    
    public VeryCompactGPSFixMovingImpl(GPSFixMoving gpsFixMoving) throws CompactionNotPossibleException {
        this(gpsFixMoving.getPosition(), gpsFixMoving.getTimePoint(), gpsFixMoving.getSpeed(), gpsFixMoving.getOptionalTrueHeading());
    }

    @Override
    public SpeedWithBearing getSpeed() {
        return new VeryCompactSpeedWithBearing();
    }
    
    @Override
    public Bearing getOptionalTrueHeading() {
        return trueHeadingDegreesSet ? new VeryCompactTrueHeading() : null;
    }

    @Override
    public Position getPosition() {
        return new VeryCompactPosition();
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
