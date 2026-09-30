package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

/**
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverCurveBoundariesImpl implements ManeuverCurveBoundaries {
    private static final long serialVersionUID = 1097529837L;
    private final TimePoint timePointBefore;
    private final TimePoint timePointAfter;
    private final SpeedWithBearing speedWithBearingBefore;
    private final SpeedWithBearing speedWithBearingAfter;
    private final double directionChangeInDegrees;
    private final Speed lowestSpeed;
    private final Speed highestSpeed;

    public ManeuverCurveBoundariesImpl(TimePoint timePointBefore, TimePoint timePointAfter,
            SpeedWithBearing speedWithBearingBefore, SpeedWithBearing speedWithBearingAfter,
            double directionChangeInDegrees, Speed lowestSpeed, Speed highestSpeed) {
        this.timePointBefore = timePointBefore;
        this.timePointAfter = timePointAfter;
        this.speedWithBearingBefore = speedWithBearingBefore;
        this.speedWithBearingAfter = speedWithBearingAfter;
        this.directionChangeInDegrees = directionChangeInDegrees;
        this.lowestSpeed = lowestSpeed;
        this.highestSpeed = highestSpeed;
    }

    @Override
    public TimePoint getTimePointBefore() {
        return timePointBefore;
    }

    @Override
    public TimePoint getTimePointAfter() {
        return timePointAfter;
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingBefore() {
        return speedWithBearingBefore;
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingAfter() {
        return speedWithBearingAfter;
    }

    @Override
    public double getDirectionChangeInDegrees() {
        return directionChangeInDegrees;
    }

    @Override
    public Speed getLowestSpeed() {
        return lowestSpeed;
    }

    @Override
    public Speed getHighestSpeed() {
        return highestSpeed;
    }

    @Override
    public String toString() {
        return "Starting at time point " + timePointBefore + ", ending at time point " + timePointAfter
                + ". Speed before curve " + speedWithBearingBefore + " speed after curve " + speedWithBearingAfter
                + ". Lowest speed within curve: " + lowestSpeed + ". Course changed by " + directionChangeInDegrees
                + "deg.";
    }
}