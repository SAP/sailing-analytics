package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sse.common.AbstractSpeedImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.CourseChange;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.AbstractSpeedWithAbstractBearingImpl;
import com.sap.sse.common.impl.AbstractSpeedWithBearingImpl;

/**
 * A memory-conserving representation of a {@link GPSFixMoving} object that produces the fine-grained
 * objects for {@link Position}, {@link SpeedWithBearing}, {@link Bearing} and {@link TimePoint} dynamically
 * as thin wrappers around this object which holds all elementary attributes required. This saves several
 * object references and object headers.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class AbstractCompactGPSFixMovingImpl extends AbstractCompactGPSFixImpl implements GPSFixMoving {
    private static final long serialVersionUID = 3977983319207618335L;

    protected abstract class AbstractCompactSpeedWithBearing extends AbstractSpeedImpl implements SpeedWithBearing {
        private static final long serialVersionUID = 1802065090733146728L;

        @Override
        public abstract double getKnots();

        @Override
        public SpeedWithBearing applyCourseChange(CourseChange courseChange) {
            return AbstractSpeedWithBearingImpl.applyCourseChange(this, courseChange);
        }

        @Override
        public CourseChange getCourseChangeRequiredToReach(SpeedWithBearing targetSpeedWithBearing) {
            return AbstractSpeedWithBearingImpl.getCourseChangeRequiredToReach(getSpeed(), targetSpeedWithBearing);
        }

        @Override
        public SpeedWithBearing add(SpeedWithBearing other) {
            return AbstractSpeedWithAbstractBearingImpl.add(this, other);
        }

        @Override
        public String toString() {
            return super.toString()+" to "+getBearing().getDegrees()+"°";
        }
        
        @Override
        public int hashCode() {
            return super.hashCode() ^ getBearing().hashCode();
        }
        
        @Override
        public boolean equals(Object object) {
            return super.equals(object) && object instanceof SpeedWithBearing
                    && getBearing().equals(((SpeedWithBearing) object).getBearing());
        }

        @Override
        public Speed projectTo(Position position, Bearing projectTo) {
            return AbstractSpeedWithAbstractBearingImpl.projectTo(this, position, projectTo);
        }
        
        @Override
        public SpeedWithBearing scale(double d) {
            return SpeedWithBearing.super.scale(d);
        }
    }
    
    public AbstractCompactGPSFixMovingImpl(TimePoint timePoint) {
        super(timePoint);
    }
    
    public AbstractCompactGPSFixMovingImpl(GPSFixMoving gpsFixMoving) throws CompactionNotPossibleException {
        this(gpsFixMoving.getTimePoint());
    }

    @Override
    public String toString() {
        return super.toString() + " with " + getSpeed();
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ getSpeed().hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        return super.equals(other) && other instanceof GPSFixMoving && getSpeed().equals(((GPSFixMoving) other).getSpeed())
                && Util.equalsWithNull(getOptionalTrueHeading(), ((GPSFixMoving) other).getOptionalTrueHeading());
    }
}
