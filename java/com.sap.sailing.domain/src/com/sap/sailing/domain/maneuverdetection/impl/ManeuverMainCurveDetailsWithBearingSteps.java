package com.sap.sailing.domain.maneuverdetection.impl;

import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.SpeedWithBearingStepsIterable;
import com.sap.sailing.domain.tracking.impl.ManeuverCurveBoundariesImpl;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

/**
 * Contains detailed information about the maneuver main curve, including its boundaries, speed and bearing steps within
 * the curve, max. recorded turning rate with corresponding time point.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverMainCurveDetailsWithBearingSteps extends ManeuverCurveBoundariesImpl {
    private static final long serialVersionUID = 5448900675868938268L;
    private final TimePoint timePoint;
    private final double maxTurningRateInDegreesPerSecond;
    private final SpeedWithBearingStepsIterable speedWithBearingSteps;

    public ManeuverMainCurveDetailsWithBearingSteps(TimePoint timePointBefore, TimePoint timePointAfter,
            TimePoint timePoint, SpeedWithBearing speedWithBearingBefore, SpeedWithBearing speedWithBearingAfter,
            double directionChangeInDegrees, double maxTurningRateInDegreesPerSecond, Speed lowestSpeed,
            Speed highestSpeed, SpeedWithBearingStepsIterable speedWithBearingSteps) {
        super(timePointBefore, timePointAfter, speedWithBearingBefore, speedWithBearingAfter, directionChangeInDegrees,
                lowestSpeed, highestSpeed);
        this.timePoint = timePoint;
        this.maxTurningRateInDegreesPerSecond = maxTurningRateInDegreesPerSecond;
        this.speedWithBearingSteps = speedWithBearingSteps;
    }

    /**
     * Gets the list of bearing steps which was used for computation of curve details.
     * 
     * @return The bearing steps of the curve
     */
    public SpeedWithBearingStepsIterable getSpeedWithBearingSteps() {
        return speedWithBearingSteps;
    }

    /**
     * Gets the computed time point of the corresponding curve. The time point refers to a position within maneuver,
     * where the highest course change has been recorded.
     * 
     * @return The computed maneuver time point
     */
    public TimePoint getTimePoint() {
        return timePoint;
    }

    /**
     * Gets the maximal turning rate recorded within the curve which was recorded at {@link #getTimePoint()}.
     * 
     * @return The maximal turning rate in degrees per second
     */
    public double getMaxTurningRateInDegreesPerSecond() {
        return maxTurningRateInDegreesPerSecond;
    }

    public ManeuverCurveBoundaries extractCurveBoundariesOnly() {
        return new ManeuverCurveBoundariesImpl(getTimePointBefore(), getTimePointAfter(), getSpeedWithBearingBefore(),
                getSpeedWithBearingAfter(), getDirectionChangeInDegrees(), getLowestSpeed(), getHighestSpeed());
    }

}
