package com.sap.sailing.domain.tracking;

import java.io.Serializable;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Represents the entering and exiting time point of a maneuver curve with corresponding speeds and courses.
 * Additionally, the total course change of the curve which is calculated throughout the iteration of bearing steps of
 * the maneuver curve is also available.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface ManeuverCurveBoundaries extends Serializable {

    /**
     * Gets the computed time point of curve start.
     * 
     * @return The time point of curve start
     */
    TimePoint getTimePointBefore();

    /**
     * Gets the computed time point of curve end.
     * 
     * @return The time point of curve end
     */
    TimePoint getTimePointAfter();

    /**
     * Gets the speed with bearing at curve start.
     * 
     * @return The speed with bearing at curve start
     */
    SpeedWithBearing getSpeedWithBearingBefore();

    /**
     * Gets the speed with bearing at curve end.
     * 
     * @return The speed with bearing at curve end
     */
    SpeedWithBearing getSpeedWithBearingAfter();

    /**
     * Gets the middle course between {@link #getSpeedWithBearingBefore()} and {@link #getSpeedWithBearingAfter()}.
     */
    default Bearing getMiddleCourse() {
        double middleCourseDeg = (getSpeedWithBearingBefore().getBearing().getDegrees()
                + getDirectionChangeInDegrees() / 2) % 360;
        if (middleCourseDeg < 0) {
            middleCourseDeg += 360;
        }
        return new DegreeBearingImpl(middleCourseDeg);
    }

    /**
     * Gets the total course change performed within the curve in degrees. The port side course changes are negative.
     * 
     * @return The total course change in degrees
     */
    double getDirectionChangeInDegrees();

    /**
     * Gets the duration of the curve.
     */
    default Duration getDuration() {
        return getTimePointBefore().until(getTimePointAfter());
    }

    /**
     * Gets the lowest speed sailed within the maneuver curve.
     * 
     * @return The lowest speed within maneuver curve
     */
    Speed getLowestSpeed();

    /**
     * Gets the highest speed sailed within the maneuver curve.
     * 
     * @return The highest speed within maneuver curve
     */
    Speed getHighestSpeed();

}
