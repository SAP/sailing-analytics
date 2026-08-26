package com.sap.sse.common;

import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.datamining.annotations.Statistic;

public interface SpeedWithBearing extends Speed {
    @Statistic(messageKey="bearing", resultDecimals=1)
    Bearing getBearing();

    /**
     * Traveling at this speed starting at time <code>from</code> in position <code>pos</code> until time
     * </code>to</code>, how far have we traveled? If <code>to</code> is before </code>from</code>, the speed will be
     * applied in reverse.
     */
    default Position travelTo(Position pos, TimePoint from, TimePoint to) {
        return travelTo(pos, from.until(to));
    }
    
    default Position travelTo(Position from, Duration duration) {
        return from.translateGreatCircle(getBearing(), travel(duration));
    }

    /**
     * Computes the minimal (in terms of bearing change) course and speed change required to reach the
     * target speed and bearing specified.
     */
    CourseChange getCourseChangeRequiredToReach(SpeedWithBearing targetSpeedWithBearing);

    SpeedWithBearing applyCourseChange(CourseChange courseChange);

    /**
     * Projects this speed onto <code>bearing</code>. The speed will keep its sign, regardless of whether the <code>bearing</code> points in
     * the opposite direction or not. The resulting speed can never be greater than this speed. If this speed's bearing is orthogonal to
     * <code>bearing</code>, a zero speed will result.
     * 
     * @param position the position at which to perform the projection; the angle between the bearings depends on the position
     */
    Speed projectTo(Position position, Bearing bearing);

    /**
     * Adds two directed speeds onto each other using vector addition.
     */
    SpeedWithBearing add(SpeedWithBearing other);
    
    default SpeedWithBearing scale(double d) {
        return new KnotSpeedWithBearingImpl(getKnots(), getBearing());
    }
}
