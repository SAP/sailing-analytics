package com.sap.sailing.domain.maneuverdetection;

import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.datamining.annotations.Statistic;

/**
 * Contains maneuver the information about the main curve of maneuver, which is regarded as relevant for maneuver
 * classification algorithms, such as the wind estimation.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface ManeuverMainCurveWithEstimationData extends ManeuverCurveBoundaries {

    /**
     * Gets the lowest speed measured within the main curve with corresponding course.
     */
    @Override
    SpeedWithBearing getLowestSpeed();

    /**
     * Gets the time point at which the lowest speed was recorded within the main curve.
     */
    TimePoint getLowestSpeedTimePoint();

    @Statistic(messageKey = "PercentageOfMainCurveProgressUntilLowestSpeed", resultDecimals = 4)
    default double getPercentageOfMainCurveProgressUntilLowestSpeed() {
        return getTimePointBefore().until(getLowestSpeedTimePoint()).asSeconds() / getDuration().asSeconds();
    }

    /**
     * Gets the highest speed measured within the main curve with corresponding course.
     */
    @Override
    SpeedWithBearing getHighestSpeed();

    /**
     * Gets the time point at which the highest speed was recorded within the main curve.
     */
    TimePoint getHighestSpeedTimePoint();

    @Statistic(messageKey = "PercentageOfMainCurveProgressUntilHighestSpeed", resultDecimals = 4)
    default double getPercentageOfMainCurveProgressUntilHighestSpeed() {
        return getTimePointBefore().until(getHighestSpeedTimePoint()).asSeconds() / getDuration().asSeconds();
    }

    /**
     * Gets the time point at which the maximal turning rate was recorded within the main curve.
     */
    TimePoint getTimePointOfMaxTurningRate();

    @Statistic(messageKey = "PercentageOfMainCurveProgressUntilMaxTurningRate", resultDecimals = 4)
    default double getPercentageOfMainCurveProgressUntilMaxTurningRate() {
        return getTimePointBefore().until(getTimePointOfMaxTurningRate()).asSeconds() / getDuration().asSeconds();
    }

    /**
     * Gets the maximal turning rate in degrees per second measured within the main curve.
     */
    @Statistic(messageKey = "MaxTurningRateInDegreesPerSecond", resultDecimals = 4)
    double getMaxTurningRateInDegreesPerSecond();

    /**
     * Gets the average turning rate recorded within the maneuver main curve. It is calculated by absolute course change
     * within main curve divided by maneuver main curve duration.
     */
    @Statistic(messageKey = "AvgTurningRateInDegreesPerSecond", resultDecimals = 4)
    double getAvgTurningRateInDegreesPerSecond();

    /**
     * Gets the course at which the maximal turning rate was measured within the main curve.
     */
    Bearing getCourseAtMaxTurningRate();

    /**
     * Gets the number of GPS-fixes contained within the maneuver main curve.
     */
    int getGpsFixesCount();

    /**
     * Gets the longest duration between two GPS-fixes contained within maneuver main curve.
     */
    Duration getLongestIntervalBetweenTwoFixes();

}
