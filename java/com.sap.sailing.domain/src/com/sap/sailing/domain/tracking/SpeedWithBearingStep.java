package com.sap.sailing.domain.tracking;

import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.Timed;

/**
 * Represents a speed with bearing step within a certain part of a GPS track. It consists of time point, speed with
 * bearing, and course change in degrees. The latter is calculated as course change between the bearing of the previous
 * step and this step. If there is no previous step, the course change in degrees value must be zero.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface SpeedWithBearingStep extends Timed {

    /**
     * Gets speed with bearing at the time point of this step.
     * 
     * @return Speed with bearing at this step
     */
    SpeedWithBearing getSpeedWithBearing();

    /**
     * Gets the course change performed from the preceding step to this step.
     * 
     * @return The course change in degrees between the preceding step and this step
     */
    double getCourseChangeInDegrees();

    /**
     * Gets the turning rate of this step, which is calculated by {@link #getCourseChangeInDegrees()} divided by
     * duration from the preceding step to this step.
     * 
     * @return The turning rate of this step
     */
    double getTurningRateInDegreesPerSecond();
}
