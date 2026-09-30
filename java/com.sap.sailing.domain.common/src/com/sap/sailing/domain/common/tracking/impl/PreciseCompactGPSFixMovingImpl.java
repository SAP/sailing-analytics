package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.AbstractPosition;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.AbstractSpeedWithAbstractBearingImpl;

/**
 * A memory-conserving representation of a {@link GPSFixMoving} object that produces the fine-grained
 * objects for {@link Position}, {@link SpeedWithBearing}, {@link Bearing} and {@link TimePoint} dynamically
 * as thin wrappers around this object which holds all elementary attributes required. This saves several
 * object references and object headers.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class PreciseCompactGPSFixMovingImpl extends AbstractCompactGPSFixMovingImpl {
    private static final long serialVersionUID = 761582024504236533L;
    private final double latDeg;
    private final double lngDeg;
    private final double knotSpeed;
    private final double degBearing;
    
    /**
     * Valid only if {@link #trueHeadingDegSet} is {@code true}
     */
    private final double trueHeadingDeg;
    private final boolean trueHeadingDegSet;

    /**
     * When <code>{@link #whatIsCached}&amp;{@link #IS_ESTIMATED_SPEED_CACHED} != 0</code>, this field tells the estimated speed's
     * true "bearing" (true course over ground) in degrees.
     */
    private double cachedEstimatedSpeedBearingInDegrees;

    /**
     * When <code>{@link #whatIsCached}&amp;{@link #IS_ESTIMATED_SPEED_CACHED} != 0</code>, this field tells the estimated speed
     * in knots.
     */
    private double cachedEstimatedSpeedInKnots;
    

    public class PreciseCompactSpeedWithBearing extends AbstractCompactSpeedWithBearing implements SpeedWithBearing {
        private static final long serialVersionUID = 1802065090733146728L;

        @Override
        public double getKnots() {
            return knotSpeed;
        }

        @Override
        public Bearing getBearing() {
            return new PreciseCompactBearing();
        }
    }
    
    private class PreciseCompactBearing extends AbstractBearing {
        private static final long serialVersionUID = -6474909210513108635L;

        @Override
        public double getDegrees() {
            return degBearing;
        }
    }
    
    private class PreciseCompactTrueHeading extends AbstractBearing {
        private static final long serialVersionUID = -1837422977159746992L;

        @Override
        public double getDegrees() {
            return trueHeadingDeg;
        }
    }
    
    public class PreciseCompactPosition extends AbstractPosition {
        private static final long serialVersionUID = 5621506820766614178L;

        @Override
        public double getLatDeg() {
            return latDeg;
        }

        @Override
        public double getLngDeg() {
            return lngDeg;
        }
    }
    
    private class PreciseCompactEstimatedSpeedBearing extends AbstractBearing {
        private static final long serialVersionUID = 8549231429037883121L;

        @Override
        public double getDegrees() {
            return cachedEstimatedSpeedBearingInDegrees;
        }
    }
    
    public class PreciseCompactEstimatedSpeed extends AbstractSpeedWithAbstractBearingImpl {
        private static final long serialVersionUID = -5871855443391817248L;

        @Override
        public Bearing getBearing() {
            return new PreciseCompactEstimatedSpeedBearing();
        }

        @Override
        public double getKnots() {
            return cachedEstimatedSpeedInKnots;
        }
    }
    
    public PreciseCompactGPSFixMovingImpl(Position position, TimePoint timePoint, SpeedWithBearing speed, Bearing optionalTrueHeading) {
        super(timePoint);
        latDeg = position.getLatDeg();
        lngDeg = position.getLngDeg();
        knotSpeed = speed==null?0:speed.getKnots();
        degBearing = speed==null?0:speed.getBearing().getDegrees();
        if (optionalTrueHeading == null) {
            trueHeadingDegSet = false;
            trueHeadingDeg = 0.0;
        } else {
            trueHeadingDegSet = true;
            trueHeadingDeg = optionalTrueHeading.getDegrees();
        }
    }
    
    public PreciseCompactGPSFixMovingImpl(GPSFixMoving gpsFixMoving) {
        this(gpsFixMoving.getPosition(), gpsFixMoving.getTimePoint(), gpsFixMoving.getSpeed(), gpsFixMoving.getOptionalTrueHeading());
    }

    @Override
    public SpeedWithBearing getSpeed() {
        return new PreciseCompactSpeedWithBearing();
    }

    @Override
    public Position getPosition() {
        return new PreciseCompactPosition();
    }
    
    @Override
    public Bearing getOptionalTrueHeading() {
        return trueHeadingDegSet ? new PreciseCompactTrueHeading() : null;
    }

    @Override
    public SpeedWithBearing getCachedEstimatedSpeed() {
        assert isEstimatedSpeedCached();
        return new PreciseCompactEstimatedSpeed();
    }

    @Override
    public SpeedWithBearing cacheEstimatedSpeed(SpeedWithBearing estimatedSpeed) {
        cachedEstimatedSpeedBearingInDegrees = estimatedSpeed.getBearing().getDegrees();
        cachedEstimatedSpeedInKnots = estimatedSpeed.getKnots();
        return super.cacheEstimatedSpeed(estimatedSpeed);
    }
}
