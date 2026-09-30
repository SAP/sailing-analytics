package com.sap.sailing.windestimation.data.transformer;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

/**
 * Conversion helper class to convert {@link CompleteManeuverCurveWithEstimationData} instances to
 * {@link LabeledManeuverForEstimation}.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ConvertableManeuverForEstimationAdapterForCompleteManeuverCurveWithEstimationData
        implements ConvertableToLabeledManeuverForEstimation {

    private final CompleteManeuverCurveWithEstimationData maneuver;
    private final Double courseChangeInDegreesWithinTurningSectionOfPreviousManeuver;
    private final Double courseChangeInDegreesWithinTurningSectionOfNextManeuver;

    public ConvertableManeuverForEstimationAdapterForCompleteManeuverCurveWithEstimationData(
            CompleteManeuverCurveWithEstimationData maneuver,
            Double courseChangeInDegreesWithinTurningSectionOfPreviousManeuver,
            Double courseChangeInDegreesWithinTurningSectionOfNextManeuver) {
        this.maneuver = maneuver;
        this.courseChangeInDegreesWithinTurningSectionOfPreviousManeuver = courseChangeInDegreesWithinTurningSectionOfPreviousManeuver;
        this.courseChangeInDegreesWithinTurningSectionOfNextManeuver = courseChangeInDegreesWithinTurningSectionOfNextManeuver;
    }

    @Override
    public double getCourseChangeInDegreesWithinTurningSection() {
        return maneuver.getMainCurve().getDirectionChangeInDegrees();
    }

    @Override
    public boolean isMarkPassing() {
        return maneuver.isMarkPassing();
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingBefore() {
        return maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingBefore();
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingAfter() {
        return maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter();
    }

    @Override
    public double getCourseChangeInDegrees() {
        return maneuver.getCurveWithUnstableCourseAndSpeed().getDirectionChangeInDegrees();
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
    public Duration getLongestIntervalBetweenTwoFixes() {
        return maneuver.getCurveWithUnstableCourseAndSpeed().getLongestIntervalBetweenTwoFixes();
    }

    @Override
    public Duration getIntervalBetweenFirstFixOfCurveAndPreviousFix() {
        return maneuver.getCurveWithUnstableCourseAndSpeed().getIntervalBetweenFirstFixOfCurveAndPreviousFix();
    }

    @Override
    public Duration getIntervalBetweenLastFixOfCurveAndNextFix() {
        return maneuver.getCurveWithUnstableCourseAndSpeed().getIntervalBetweenLastFixOfCurveAndNextFix();
    }

    @Override
    public Duration getDurationFromPreviousManeuverEndToManeuverStart() {
        return maneuver.getCurveWithUnstableCourseAndSpeed().getDurationFromPreviousManeuverEndToManeuverStart();
    }

    @Override
    public Duration getDurationFromManeuverEndToNextManeuverStart() {
        return maneuver.getCurveWithUnstableCourseAndSpeed().getDurationFromManeuverEndToNextManeuverStart();
    }

    @Override
    public Speed getLowestSpeed() {
        return maneuver.getCurveWithUnstableCourseAndSpeed().getLowestSpeed();
    }

    @Override
    public Speed getHighestSpeedWithinTurningSection() {
        return maneuver.getMainCurve().getHighestSpeed();
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingBeforeWithinTurningSection() {
        return maneuver.getMainCurve().getSpeedWithBearingBefore();
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingAfterWithinTurningSection() {
        return maneuver.getMainCurve().getSpeedWithBearingAfter();
    }

    @Override
    public Double getTargetTackAngleInDegrees() {
        return maneuver.getTargetTackAngleInDegrees();
    }

    @Override
    public Double getTargetJibeAngleInDegrees() {
        return maneuver.getTargetJibeAngleInDegrees();
    }

    @Override
    public boolean hasMarkPassingData() {
        return maneuver.getRelativeBearingToNextMarkAfterManeuver() != null
                || maneuver.getRelativeBearingToNextMarkBeforeManeuver() != null;
    }

    @Override
    public double getMaxTurningRateInDegreesPerSecond() {
        return maneuver.getMainCurve().getMaxTurningRateInDegreesPerSecond();
    }

    @Override
    public Bearing getMiddleCourse() {
        return maneuver.getCurveWithUnstableCourseAndSpeed().getMiddleCourse();
    }

    @Override
    public Position getPosition() {
        return maneuver.getPosition();
    }

    @Override
    public TimePoint getTimePoint() {
        return maneuver.getTimePoint();
    }

    @Override
    public Wind getWind() {
        return maneuver.getWind();
    }

    @Override
    public ManeuverType getManeuverTypeForCompleteManeuverCurve() {
        return maneuver.getManeuverTypeForCompleteManeuverCurve();
    }

    public static List<ConvertableToLabeledManeuverForEstimation> getConvertableManeuvers(
            List<CompleteManeuverCurveWithEstimationData> maneuvers) {
        List<ConvertableToLabeledManeuverForEstimation> convertableManeuvers = new ArrayList<>(maneuvers.size());
        CompleteManeuverCurveWithEstimationData previousManeuver = null;
        CompleteManeuverCurveWithEstimationData currentManeuver = null;
        for (CompleteManeuverCurveWithEstimationData nextManeuver : maneuvers) {
            if (currentManeuver != null) {
                Double courseChangeInDegreesWithinTurningSectionOfPreviousManeuver = previousManeuver == null ? null
                        : previousManeuver.getMainCurve().getDirectionChangeInDegrees();
                Double courseChangeInDegreesWithinTurningSectionOfNextManeuver = nextManeuver.getMainCurve()
                        .getDirectionChangeInDegrees();
                ConvertableManeuverForEstimationAdapterForCompleteManeuverCurveWithEstimationData convertableManeuver = new ConvertableManeuverForEstimationAdapterForCompleteManeuverCurveWithEstimationData(
                        currentManeuver, courseChangeInDegreesWithinTurningSectionOfPreviousManeuver,
                        courseChangeInDegreesWithinTurningSectionOfNextManeuver);
                convertableManeuvers.add(convertableManeuver);
            }
            previousManeuver = currentManeuver;
            currentManeuver = nextManeuver;
        }
        if (currentManeuver != null) {
            Double courseChangeInDegreesWithinTurningSectionOfPreviousManeuver = previousManeuver == null ? null
                    : previousManeuver.getMainCurve().getDirectionChangeInDegrees();
            Double courseChangeInDegreesWithinTurningSectionOfNextManeuver = null;
            ConvertableManeuverForEstimationAdapterForCompleteManeuverCurveWithEstimationData convertableManeuver = new ConvertableManeuverForEstimationAdapterForCompleteManeuverCurveWithEstimationData(
                    currentManeuver, courseChangeInDegreesWithinTurningSectionOfPreviousManeuver,
                    courseChangeInDegreesWithinTurningSectionOfNextManeuver);
            convertableManeuvers.add(convertableManeuver);
        }
        return convertableManeuvers;
    }

}
