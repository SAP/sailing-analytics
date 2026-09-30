package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.AbstractSpeedWithAbstractBearingImpl;
import com.sap.sse.common.impl.AbstractTimePoint;

public abstract class AbstractCompactWindImpl extends AbstractSpeedWithAbstractBearingImpl implements Wind {
    private static final long serialVersionUID = -5059956032663387929L;
    
    /**
     * bit mask for {@link #flags}, telling the bit encoding whether the position is {@code null}.
     */
    private static final byte POSITION_IS_NULL = 1<<0;

    /**
     * bit mask for {@link #flags}, telling the bit encoding whether the bearing is {@code null}.
     */
    private static final byte BEARING_IS_NULL = 1<<1;

    /**
     * bit mask for {@link #flags}, telling the bit encoding whether the time point is {@code null}.
     */
    private static final byte TIME_POINT_IS_NULL = 1<<2;
    
    /**
     * See the bit mask values such as {@link #POSITION_IS_NULL}, {@link #BEARING_IS_NULL} and {@link #TIME_POINT_IS_NULL}.
     */
    private final byte flags;
    
    private final long timePointAsMillis;

    private class CompactTimePoint extends AbstractTimePoint implements TimePoint {
        private static final long serialVersionUID = -2470922642359937437L;

        @Override
        public long asMillis() {
            return timePointAsMillis;
        }
    }
    
    public AbstractCompactWindImpl(Wind wind) {
        final boolean bearingIsNull;
        final boolean positionIsNull;
        final boolean timePointIsNull;
        bearingIsNull = wind.getBearing() == null;
        positionIsNull = wind.getPosition() == null;
        if (wind.getTimePoint() == null) {
            timePointIsNull = true;
            this.timePointAsMillis = 0;
        } else {
            timePointIsNull = false;
            this.timePointAsMillis = wind.getTimePoint().asMillis();
        }
        flags = (byte) ((byte) (bearingIsNull ? BEARING_IS_NULL : (byte) 0) | (positionIsNull ? POSITION_IS_NULL : (byte) 0) | (timePointIsNull ? TIME_POINT_IS_NULL : (byte) 0));
    }

    @Override
    public Position getPosition() {
        if ((flags & POSITION_IS_NULL) != 0) {
            return null;
        } else {
            return getCompactPosition();
        }
    }

    protected abstract Position getCompactPosition();

    @Override
    public TimePoint getTimePoint() {
        if ((flags & TIME_POINT_IS_NULL) != 0) {
            return null;
        } else {
            return new CompactTimePoint();
        }
    }

    @Override
    public Bearing getBearing() {
        if ((flags & BEARING_IS_NULL) != 0) {
            return null;
        } else {
            return getCompactBearing();
        }
    }

    protected abstract Bearing getCompactBearing();

    /**
     * Make abstract to force implementing subclasses to implement this method.
     */
    @Override
    public abstract double getKnots();

    @Override
    public Bearing getFrom() {
        if (getBearing() == null) {
            return null;
        } else {
            return getBearing().reverse();
        }
    }

    @Override
    public int hashCode() {
        return WindImpl.hashCode(getPosition().getLatDeg(), getPosition().getLngDeg(), (flags&TIME_POINT_IS_NULL) != 0?0:timePointAsMillis);
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
        return Util.equalsWithNull(getPosition(), other.getPosition()) && Util.equalsWithNull(getTimePoint(), other.getTimePoint());
    }

    @Override
    public String toString() {
        return ""+getTimePoint()+"@"+getPosition()+": "+getKnots()+"kn from "+getFrom();
    }
}
