package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class GPSFixMovingImpl extends GPSFixImpl implements GPSFixMoving {
    private static final long serialVersionUID = 6508021498142383100L;
    private final SpeedWithBearing speed;
    private final Bearing optionalTrueHeading;
    
    public GPSFixMovingImpl(Position position, TimePoint timePoint, SpeedWithBearing speed, Bearing optionalTrueHeading) {
        super(position, timePoint);
        this.speed = speed;
        this.optionalTrueHeading = optionalTrueHeading;
    }

    @Override
    public SpeedWithBearing getSpeed() {
        return speed;
    }

    @Override
    public Bearing getOptionalTrueHeading() {
        return optionalTrueHeading;
    }

    @Override
    public String toString() {
        return super.toString()+" with "+getSpeed();
    }
    
    @Override
    public int hashCode() {
        return super.hashCode() ^ getSpeed().hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        return super.equals(other) && other instanceof GPSFixMoving && getSpeed().equals(((GPSFixMoving) other).getSpeed());
    }
    
    public static GPSFixMovingImpl create(double lonDeg, double latDeg, long timeMillis,
            double speedInKnots, double bearingDeg, Double optionalTrueHeadingDeg) {
        return new GPSFixMovingImpl(new DegreePosition(latDeg, lonDeg),
                new MillisecondsTimePoint(timeMillis), new KnotSpeedWithBearingImpl(
                        speedInKnots, new DegreeBearingImpl(bearingDeg)),
                optionalTrueHeadingDeg==null?null:new DegreeBearingImpl(optionalTrueHeadingDeg));
    }
}
