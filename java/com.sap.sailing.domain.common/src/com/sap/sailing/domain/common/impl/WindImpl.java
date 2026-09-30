package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.Wind;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public class WindImpl extends KnotSpeedWithBearingImpl implements Wind {
    private static final long serialVersionUID = 5431592324949471980L;
    private final Position position;
    private final TimePoint timepoint;

    public WindImpl(Position p, TimePoint at, SpeedWithBearing windSpeedWithBearing) {
        super(windSpeedWithBearing.getKnots(), windSpeedWithBearing.getBearing());
        this.position = p;
        this.timepoint = at;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public TimePoint getTimePoint() {
        return timepoint;
    }

    @Override
    public Bearing getFrom() {
        return getBearing().reverse();
    }
    
    public static int hashCode(double latDeg, double lngDeg, long timePointAsMillis) {
        return (31 * (int) (timePointAsMillis & Integer.MAX_VALUE)) ^ (int) (Math.round(latDeg)*Math.round(lngDeg));
    }
    
    /**
     * Wind hash is determined based on time point and position only to speed this up a little.
     */
    @Override
    public int hashCode() {
        return hashCode(position.getLatDeg(), position.getLngDeg(), timepoint==null?0:timepoint.asMillis());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof Wind))
            return false;
        Wind other = (Wind) obj;
        if (position == null) {
            if (other.getPosition() != null)
                return false;
        } else if (!position.equals(other.getPosition()))
            return false;
        if (timepoint == null) {
            if (other.getTimePoint() != null)
                return false;
        } else if (!timepoint.equals(other.getTimePoint()))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return ""+getTimePoint()+"@"+getPosition()+": "+getKnots()+"kn from "+getFrom();
    }
    
}
