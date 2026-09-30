package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.Wind;
import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.AbstractPosition;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;

/**
 * Keeps the precise values of lat/lng and speed/course.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class PreciseCompactWindImpl extends AbstractCompactWindImpl {
    private static final long serialVersionUID = -5059956032663387929L;
    private final double latDeg;
    private final double lngDeg;
    private final double speedInKnots;
    private final double degBearing;

    public class CompactPosition extends AbstractPosition {
        private static final long serialVersionUID = 5621506820766614178L;

        @Override
        public double getLatDeg() {
            return latDeg;
        }

        @Override
        public double getLngDeg() {
            return lngDeg;
        }
        
        @Override
        public boolean equals(Object o) {
            return this==o || o instanceof Position && getLatDeg() == ((Position) o).getLatDeg() && getLngDeg() == ((Position) o).getLngDeg();
        }
    }

    private class CompactBearing extends AbstractBearing {
        private static final long serialVersionUID = -6474909210513108635L;

        @Override
        public double getDegrees() {
            return degBearing;
        }
    }

    public PreciseCompactWindImpl(Wind wind) {
        super(wind);
        if (wind.getBearing() == null) {
            degBearing = 0;
        } else {
            this.degBearing = wind.getBearing().getDegrees();
        }
        if (wind.getPosition() == null) {
            this.latDeg = 0;
            this.lngDeg = 0;
        } else {
            this.latDeg = wind.getPosition().getLatDeg();
            this.lngDeg = wind.getPosition().getLngDeg();
        }
        this.speedInKnots = wind.getKnots();
    }

    @Override
    public double getKnots() {
        return speedInKnots;
    }

    @Override
    protected Position getCompactPosition() {
        return new CompactPosition();
    }

    @Override
    protected Bearing getCompactBearing() {
        return new CompactBearing();
    }
}
