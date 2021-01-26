package com.sap.sailing.windestimation.data.serialization;

import org.json.simple.JSONObject;

import com.sap.sailing.windestimation.data.ManeuverCategory;
import com.sap.sailing.windestimation.data.ManeuverForDataAnalysis;
import com.sap.sailing.windestimation.data.ManeuverTypeForDataAnalysis;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class ManeuverForDataAnalysisJsonDeserializer implements JsonDeserializer<ManeuverForDataAnalysis> {

    @Override
    public ManeuverForDataAnalysis deserialize(JSONObject object) throws JsonDeserializationException {
        ManeuverTypeForDataAnalysis maneuverType = ManeuverTypeForDataAnalysis
                .valueOf((String) object.get(ManeuverForDataAnalysisJsonSerializer.MANEUVER_TYPE));
        double absoluteTotalCourseChangeInDegrees = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.ABSOLUTE_TOTAL_COURSE_CHANGE_IN_DEGREES);
        double absoluteTotalCourseChangeWithinMainCurveInDegrees = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.ABSOLUTE_TOTAL_COURSE_CHANGE_WITHIN_MAIN_CURVE_IN_DEGREES);
        double speedInSpeedOutRatio = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.SPEED_IN_SPEED_OUT_RATIO);
        double oversteeringInDegrees = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.OVERSTEERING_IN_DEGREES);
        double speedLossRatio = (double) object.get(ManeuverForDataAnalysisJsonSerializer.SPEED_LOSS_RATIO);
        double speedGainRatio = (double) object.get(ManeuverForDataAnalysisJsonSerializer.SPEED_GAIN_RATIO);
        double lowestSpeedVsExitingSpeedRatio = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.LOWEST_SPEED_VS_EXITING_SPEED_RATIO);
        double maximalTurningRateInDegreesPerSecond = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.MAXIMAL_TURNING_RATE_IN_DEGREES_PER_SECOND);
        Double deviationFromOptimalTackAngleInDegrees = (Double) object
                .get(ManeuverForDataAnalysisJsonSerializer.DEVIATION_FROM_OPTIMAL_TACK_ANGLE_IN_DEGREES);
        Double deviationFromOptimalJibeAngleInDegrees = (Double) object
                .get(ManeuverForDataAnalysisJsonSerializer.DEVIATION_FROM_OPTIMAL_JIBE_ANGLE_IN_DEGREES);
        Double relativeBearingToNextMarkBeforeInDegrees = (Double) object
                .get(ManeuverForDataAnalysisJsonSerializer.RELATIVE_BEARING_TO_NEXT_MARK_BEFORE_IN_DEGREES);
        Double relativeBearingToNextMarkAfterInDegrees = (Double) object
                .get(ManeuverForDataAnalysisJsonSerializer.RELATIVE_BEARING_TO_NEXT_MARK_AFTER_IN_DEGREES);
        boolean markPassing = ((int) object.get(ManeuverForDataAnalysisJsonSerializer.MARK_PASSING)) == 1;
        double mainCurveDurationInSeconds = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.MAIN_CURVE_DURATION_IN_SECONDS);
        double maneuverDurationInSeconds = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.MANEUVER_DURATION_IN_SECONDS);
        double recoveryPhaseDurationInSeconds = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.RECOVERY_PHASE_DURATION_IN_SECONDS);
        boolean clean = (boolean) object.get(ManeuverForDataAnalysisJsonSerializer.CLEAN);
        ManeuverCategory maneuverCategory = ManeuverCategory
                .valueOf((String) object.get(ManeuverForDataAnalysisJsonSerializer.MANEUVER_CATEGORY));
        double twaBeforeInDegrees = (double) object.get(ManeuverForDataAnalysisJsonSerializer.TWA_BEFORE_IN_DEGREES);
        double twaAfterInDegrees = (double) object.get(ManeuverForDataAnalysisJsonSerializer.TWA_AFTER_IN_DEGREES);
        double twsInKnots = (double) object.get(ManeuverForDataAnalysisJsonSerializer.TWS_IN_KNOTS);
        double speedBeforeInKnots = (double) object.get(ManeuverForDataAnalysisJsonSerializer.SPEED_BEFORE_IN_KNOTS);
        double speedAfterInKnots = (double) object.get(ManeuverForDataAnalysisJsonSerializer.SPEED_AFTER_IN_KNOTS);
        double scaledSpeedBeforeInKnots = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.SCALED_SPEED_BEFORE_IN_KNOTS);
        double scaledSpeedAfterInKnots = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.SCALED_SPEED_AFTER_IN_KNOTS);
        double twaAtMiddleCourseInDegrees = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.TWA_AT_MIDDLE_COURSE_IN_DEGREES);
        double twaAtMiddleCourseMainCurveInDegrees = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.TWA_AT_MIDDLE_COURSE_MAIN_CURVE_IN_DEGREES);
        double twaAtLowestSpeedInDegrees = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.TWA_AT_LOWEST_SPEED_IN_DEGREES);
        double twaAtMaxTurningRateInDegrees = (double) object
                .get(ManeuverForDataAnalysisJsonSerializer.TWA_AT_MAX_TURNING_RATE_IN_DEGREES);
        boolean starboardManeuver = (boolean) object.get(ManeuverForDataAnalysisJsonSerializer.STARBOARD_MANEUVER);

        return new ManeuverForDataAnalysis(maneuverType, absoluteTotalCourseChangeInDegrees,
                absoluteTotalCourseChangeWithinMainCurveInDegrees, speedInSpeedOutRatio, oversteeringInDegrees,
                speedLossRatio, speedGainRatio, lowestSpeedVsExitingSpeedRatio, maximalTurningRateInDegreesPerSecond,
                deviationFromOptimalTackAngleInDegrees, deviationFromOptimalJibeAngleInDegrees,
                relativeBearingToNextMarkBeforeInDegrees, relativeBearingToNextMarkAfterInDegrees,
                mainCurveDurationInSeconds, maneuverDurationInSeconds, recoveryPhaseDurationInSeconds, clean,
                maneuverCategory, twaBeforeInDegrees, twaAfterInDegrees, twsInKnots, speedBeforeInKnots,
                speedAfterInKnots, twaAtMiddleCourseInDegrees, twaAtMiddleCourseMainCurveInDegrees,
                twaAtLowestSpeedInDegrees, twaAtMaxTurningRateInDegrees, starboardManeuver, scaledSpeedBeforeInKnots,
                scaledSpeedAfterInKnots, markPassing);
    }

}
