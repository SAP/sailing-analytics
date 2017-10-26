package com.sap.sailing.domain.maneuverdetection.impl;

import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

/**
 * Represents the computed details about the extension of a curve boundary which might refer to either start, or end of
 * the curve.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
class CurveBoundaryExtension {
    private final TimePoint extensionTimePoint;
    private final SpeedWithBearing speedWithBearingAtExtensionTimePoint;
    private final double courseChangeInDegreesWithinExtensionArea;

    public CurveBoundaryExtension(TimePoint extensionTimePoint, SpeedWithBearing speedWithBearingAtExtensionTimePoint,
            double totalCourseChangeInDegreesExtension) {
        this.extensionTimePoint = extensionTimePoint;
        this.speedWithBearingAtExtensionTimePoint = speedWithBearingAtExtensionTimePoint;
        this.courseChangeInDegreesWithinExtensionArea = totalCourseChangeInDegreesExtension;
    }

    /**
     * Gets the computed time point which supposes to extend the time range of the curve.
     * 
     * @return Either {@code timePointBefore}, or {@code timePointAfter} of a curve, depending on the callers request.
     */
    public TimePoint getExtensionTimePoint() {
        return extensionTimePoint;
    }

    /**
     * Gets the computed speed with bearing measured at {@link #getExtensionTimePoint()}.
     * 
     * @return Either speed with bearing at {@code timePointBefore}, or {@code timePointAfter} of the curve, depending
     *         on the callers request.
     */
    public SpeedWithBearing getSpeedWithBearingAtExtensionTimePoint() {
        return speedWithBearingAtExtensionTimePoint;
    }

    /**
     * Gets the course change between the {@link #getExtensionTimePoint()} and the previous starting/ending time point
     * of the curve.
     * 
     * @return The total course change in degrees within the extension area
     */
    public double getCourseChangeInDegreesWithinExtensionArea() {
        return courseChangeInDegreesWithinExtensionArea;
    }
}