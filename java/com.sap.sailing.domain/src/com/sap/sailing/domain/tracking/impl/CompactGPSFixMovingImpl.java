package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.common.AbstractBearing;
import com.sap.sailing.domain.common.AbstractSpeedImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.CourseChange;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.AbstractSpeedWithAbstractBearingImpl;
import com.sap.sailing.domain.common.impl.AbstractSpeedWithBearingImpl;
import com.sap.sailing.domain.tracking.GPSFixMoving;

/**
 * A memory-conserving representation of a {@link GPSFixMoving} object that produces the fine-grained
 * objects for {@link Position}, {@link SpeedWithBearing}, {@link Bearing} and {@link TimePoint} dynamically
 * as thin wrappers around this object which holds all elementary attributes required. This saves several
 * object references and object headers.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CompactGPSFixMovingImpl extends CompactGPSFixImpl implements GPSFixMoving {
    private static final long serialVersionUID = 761582024504236533L;
    private final double knotSpeed;
    private final double degBearing;
    
    private class CompactSpeedWithBearing extends AbstractSpeedImpl implements SpeedWithBearing {
        private static final long serialVersionUID = 1802065090733146728L;

        @Override
        public double getKnots() {
            return knotSpeed;
        }

        @Override
        public Bearing getBearing() {
            return new CompactBearing();
        }

        @Override
        public Position travelTo(Position pos, TimePoint from, TimePoint to) {
            return pos.translateGreatCircle(getBearing(), this.travel(from, to));
        }
        
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
    }
    
    private class CompactBearing extends AbstractBearing {
        private static final long serialVersionUID = -6474909210513108635L;

        @Override
        public double getDegrees() {
            return degBearing;
        }

        @Override
        public double getRadians() {
            return degBearing / 180. * Math.PI;
        }
    }
    
    public CompactGPSFixMovingImpl(Position position, TimePoint timePoint, SpeedWithBearing speed) {
        super(position, timePoint);
        knotSpeed = speed.getKnots();
        degBearing = speed.getBearing().getDegrees();
    }
    
    public CompactGPSFixMovingImpl(GPSFixMoving gpsFixMoving) {
        this(gpsFixMoving.getPosition(), gpsFixMoving.getTimePoint(), gpsFixMoving.getSpeed());
    }

    @Override
    public SpeedWithBearing getSpeed() {
        return new CompactSpeedWithBearing();
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
        return super.equals(other) && other instanceof GPSFixMoving && getSpeed().equals(((GPSFixMoving) other).getSpeed());
    }
}
