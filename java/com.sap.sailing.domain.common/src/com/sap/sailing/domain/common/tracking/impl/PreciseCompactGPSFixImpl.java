package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.AbstractPosition;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.AbstractSpeedWithAbstractBearingImpl;

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
public class PreciseCompactGPSFixImpl extends AbstractCompactGPSFixImpl {
    private static final long serialVersionUID = 8167588584536992501L;
    
    private final double latDeg;
    private final double lngDeg;
    
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
    
    private class PreciseCompactEstimatedSpeed extends AbstractSpeedWithAbstractBearingImpl {
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
    
    public PreciseCompactGPSFixImpl(Position position, TimePoint timePoint) {
        super(timePoint);
        latDeg = position.getLatDeg();
        lngDeg = position.getLngDeg();
    }
    
    public PreciseCompactGPSFixImpl(GPSFix gpsFix) {
        this(gpsFix.getPosition(), gpsFix.getTimePoint());
    }

    @Override
    public Position getPosition() {
        return new PreciseCompactPosition();
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
