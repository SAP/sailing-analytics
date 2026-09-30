package com.sap.sailing.windestimation.integration;

import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.transformer.ConvertableToManeuverForEstimation;
import com.sap.sailing.windestimation.data.transformer.ManeuverForEstimationTransformer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

/**
 * Conversion helper class for {@link ManeuverForEstimationTransformer} to convert {@link CompleteManeuverCurve} to
 * {@link ManeuverForEstimation}.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ConvertableManeuverForEstimationAdapterForCompleteManeuverCurve
        implements ConvertableToManeuverForEstimation {

    private final CompleteManeuverCurve maneuver;
    private final Position maneuverPosition;
    private final boolean hasMarkPassingData;
    private final Duration intervalBetweenFirstFixOfCurveAndPreviousFix;
    private final Duration intervalBetweenLastFixOfCurveAndNextFix;
    private final Duration longestIntervalBetweenTwoFixes;
    private final Double courseChangeInDegreesWithinTurningSectionOfPreviousManeuver;
    private final Double courseChangeInDegreesWithinTurningSectionOfNextManeuver;
    private final Duration durationFromPreviousManeuverEndToManeuverStart;
    private final Duration durationFromManeuverEndToNextManeuverStart;
    private final Double targetTackAngleInDegrees;
    private final Double targetJibeAngleInDegrees;

    public ConvertableManeuverForEstimationAdapterForCompleteManeuverCurve(CompleteManeuverCurve maneuver,
            Position maneuverPosition, boolean hasMarkPassingData, Duration longestIntervalBetweenTwoFixes,
            Double courseChangeInDegreesWithinTurningSectionOfPreviousManeuver,
            Double courseChangeInDegreesWithinTurningSectionOfNextManeuver,
            Duration intervalBetweenFirstFixOfCurveAndPreviousFix, Duration intervalBetweenLastFixOfCurveAndNextFix,
            Duration durationFromPreviousManeuverEndToManeuverStart,
            Duration durationFromManeuverEndToNextManeuverStart, Double targetTackAngleInDegrees,
            Double targetJibeAngleInDegrees) {
        this.maneuver = maneuver;
        this.maneuverPosition = maneuverPosition;
        this.hasMarkPassingData = hasMarkPassingData;
        this.longestIntervalBetweenTwoFixes = longestIntervalBetweenTwoFixes;
        this.courseChangeInDegreesWithinTurningSectionOfPreviousManeuver = courseChangeInDegreesWithinTurningSectionOfPreviousManeuver;
        this.courseChangeInDegreesWithinTurningSectionOfNextManeuver = courseChangeInDegreesWithinTurningSectionOfNextManeuver;
        this.intervalBetweenFirstFixOfCurveAndPreviousFix = intervalBetweenFirstFixOfCurveAndPreviousFix;
        this.intervalBetweenLastFixOfCurveAndNextFix = intervalBetweenLastFixOfCurveAndNextFix;
        this.durationFromPreviousManeuverEndToManeuverStart = durationFromPreviousManeuverEndToManeuverStart;
        this.durationFromManeuverEndToNextManeuverStart = durationFromManeuverEndToNextManeuverStart;
        this.targetTackAngleInDegrees = targetTackAngleInDegrees;
        this.targetJibeAngleInDegrees = targetJibeAngleInDegrees;
    }

    @Override
    public double getCourseChangeInDegreesWithinTurningSection() {
        return maneuver.getMainCurveBoundaries().getDirectionChangeInDegrees();
    }

    @Override
    public double getCourseChangeInDegrees() {
        return maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getDirectionChangeInDegrees();
    }

    @Override
    public Double getCourseChangeInDegreesWithinTurningSectionOfPreviousManeuver() {
        return courseChangeInDegreesWithinTurningSectionOfPreviousManeuver;
    }

    @Override
    public Double getCourseChangeInDegreesWithinTurningSectionOfNextManeuver() {
        return courseChangeInDegreesWithinTurningSectionOfNextManeuver;
    }

    @Override
    public boolean isMarkPassing() {
        return maneuver.isMarkPassing();
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingBefore() {
        return maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingBefore();
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingAfter() {
        return maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingAfter();
    }

    @Override
    public Duration getLongestIntervalBetweenTwoFixes() {
        return longestIntervalBetweenTwoFixes;
    }

    @Override
    public Duration getIntervalBetweenFirstFixOfCurveAndPreviousFix() {
        return intervalBetweenFirstFixOfCurveAndPreviousFix;
    }

    @Override
    public Duration getIntervalBetweenLastFixOfCurveAndNextFix() {
        return intervalBetweenLastFixOfCurveAndNextFix;
    }

    @Override
    public Duration getDurationFromPreviousManeuverEndToManeuverStart() {
        return durationFromPreviousManeuverEndToManeuverStart;
    }

    @Override
    public Duration getDurationFromManeuverEndToNextManeuverStart() {
        return durationFromManeuverEndToNextManeuverStart;
    }

    @Override
    public Speed getLowestSpeed() {
        return maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getLowestSpeed();
    }

    @Override
    public Speed getHighestSpeedWithinTurningSection() {
        return maneuver.getMainCurveBoundaries().getHighestSpeed();
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingBeforeWithinTurningSection() {
        return maneuver.getMainCurveBoundaries().getSpeedWithBearingBefore();
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingAfterWithinTurningSection() {
        return maneuver.getMainCurveBoundaries().getSpeedWithBearingAfter();
    }

    @Override
    public Double getTargetTackAngleInDegrees() {
        return targetTackAngleInDegrees;
    }

    @Override
    public Double getTargetJibeAngleInDegrees() {
        return targetJibeAngleInDegrees;
    }

    @Override
    public boolean hasMarkPassingData() {
        return hasMarkPassingData;
    }

    @Override
    public double getMaxTurningRateInDegreesPerSecond() {
        return maneuver.getMainCurveBoundaries().getMaxTurningRateInDegreesPerSecond();
    }

    @Override
    public Bearing getMiddleCourse() {
        return maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getMiddleCourse();
    }

    @Override
    public Position getPosition() {
        return maneuverPosition;
    }

    @Override
    public TimePoint getTimePoint() {
        return maneuver.getMainCurveBoundaries().getTimePoint();
    }

}
