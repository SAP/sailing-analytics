package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.AbstractSpeedImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.CourseChange;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TimePoint;

public abstract class AbstractSpeedWithAbstractBearingImpl extends AbstractSpeedImpl implements SpeedWithBearing {
    private static final long serialVersionUID = 6136100417593538013L;

    @Override
    public Position travelTo(Position pos, TimePoint from, TimePoint to) {
        return pos.translateGreatCircle(getBearing(), this.travel(from, to));
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
    public SpeedWithBearing add(SpeedWithBearing other) {
        return AbstractSpeedWithAbstractBearingImpl.add(this, other);
    }

    @Override
    public CourseChange getCourseChangeRequiredToReach(SpeedWithBearing targetSpeedWithBearing) {
        return AbstractSpeedWithBearingImpl.getCourseChangeRequiredToReach(this, targetSpeedWithBearing);
    }

    public static CourseChange getCourseChangeRequiredToReach(SpeedWithBearing from, SpeedWithBearing to) {
        double courseChangeInDegrees = to.getBearing().getDegrees() - from.getBearing().getDegrees();
        if (courseChangeInDegrees < -180.) {
            courseChangeInDegrees += 360.;
        } else if (courseChangeInDegrees > 180.) {
            courseChangeInDegrees -= 360.;
        }
        double speedChangeInKnots = to.getKnots() - from.getKnots();
        return new CourseChangeImpl(courseChangeInDegrees, speedChangeInKnots);
    }
    
    @Override
    public SpeedWithBearing applyCourseChange(CourseChange courseChange) {
        return applyCourseChange(this, courseChange);
    }
    
    public static SpeedWithBearing applyCourseChange(SpeedWithBearing from, CourseChange courseChange) {
        double newBearingDeg = from.getBearing().getDegrees() + courseChange.getCourseChangeInDegrees();
        if (newBearingDeg < 0) {
            newBearingDeg += 360;
        } else if (newBearingDeg > 360) {
            newBearingDeg -= 360;
        }
        Bearing newBearing = new DegreeBearingImpl(newBearingDeg);
        double newSpeedInKnots = from.getKnots()+courseChange.getSpeedChangeInKnots();
        return new KnotSpeedWithBearingImpl(newSpeedInKnots, newBearing);
    }

    private final static TimePoint start = new MillisecondsTimePoint(0);
    private final static TimePoint end = start.plus(60000);

    public static Speed projectTo(SpeedWithBearing speedWithBearing, Position position, Bearing projectTo) {
        Position traveledOneMinute = speedWithBearing.travelTo(position, start, end);
        Position traveledToProjected = traveledOneMinute.projectToLineThrough(position, projectTo);
        Distance projectedDistance = position.getDistance(traveledToProjected);
        return projectedDistance.inTime(end.asMillis() - start.asMillis());
    }

    @Override
    public Speed projectTo(Position position, Bearing projectTo) {
        return projectTo(this, position, projectTo);
    }

    public static SpeedWithBearing add(SpeedWithBearing first, SpeedWithBearing other) {
        double x = first.getMetersPerSecond()*Math.cos(first.getBearing().getRadians()) + other.getMetersPerSecond()*Math.cos(other.getBearing().getRadians());
        double y = first.getMetersPerSecond()*Math.sin(first.getBearing().getRadians()) + other.getMetersPerSecond()*Math.sin(other.getBearing().getRadians());
        double metersPerSecond = Math.sqrt(x*x+y*y);
        double directionRad = (2*Math.PI+Math.atan2(y, x))%(2*Math.PI);
        return new MeterPerSecondSpeedWithDegreeBearingImpl(metersPerSecond, new RadianBearingImpl(directionRad));
    }
}
