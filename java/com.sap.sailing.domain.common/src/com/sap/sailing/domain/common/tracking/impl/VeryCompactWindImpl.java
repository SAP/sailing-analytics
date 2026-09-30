package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.Wind;
import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.AbstractPosition;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;

/**
 * Uses scaled, rounded internal representations of the values for lat/lng and speed/course. See
 * {@link CompactPositionHelper} for the details of how value ranges and accuracies are affected.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class VeryCompactWindImpl extends AbstractCompactWindImpl {
    private static final long serialVersionUID = -5059956032663387929L;
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

    private class VeryCompactBearing extends AbstractBearing {
        private static final long serialVersionUID = -6474909210513108635L;

        @Override
        public double getDegrees() {
            return CompactPositionHelper.getDegreeBearing(degreeBearingScaled);
        }
    }

    public VeryCompactWindImpl(Wind wind) throws CompactionNotPossibleException {
        super(wind);
        if (wind.getBearing() == null) {
            degreeBearingScaled = 0;
        } else {
            this.degreeBearingScaled = CompactPositionHelper.getDegreeBearingScaled(wind.getBearing());
        }
        if (wind.getPosition() == null) {
            this.latDegScaled = 0;
            this.lngDegScaled = 0;
        } else {
            this.latDegScaled = CompactPositionHelper.getLatDegScaled(wind.getPosition());
            this.lngDegScaled = CompactPositionHelper.getLngDegScaled(wind.getPosition());
        }
        this.speedInKnotsScaled = CompactPositionHelper.getKnotSpeedScaled(wind);
    }

    @Override
    public double getKnots() {
        return CompactPositionHelper.getKnotSpeed(speedInKnotsScaled);
    }

    @Override
    protected Position getCompactPosition() {
        return new VeryCompactPosition();
    }

    @Override
    protected Bearing getCompactBearing() {
        return new VeryCompactBearing();
    }
}
