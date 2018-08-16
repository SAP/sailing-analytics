package com.sap.sailing.windestimation.data.deserializer;

import org.json.simple.JSONObject;

import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.windestimation.data.ManeuverCategory;
import com.sap.sailing.windestimation.data.ManeuverForClassification;
import com.sap.sailing.windestimation.data.ManeuverForClassificationImpl;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;

public class ManeuverForClassificationJsonDeserializer implements JsonDeserializer<ManeuverForClassification> {

    @Override
    public ManeuverForClassification deserialize(JSONObject object) throws JsonDeserializationException {
        ManeuverTypeForClassification maneuverType = ManeuverTypeForClassification
                .valueOf((String) object.get(ManeuverForClassificationJsonSerializer.MANEUVER_TYPE));
        double absoluteTotalCourseChangeInDegrees = (double) object
                .get(ManeuverForClassificationJsonSerializer.ABSOLUTE_TOTAL_COURSE_CHANGE_IN_DEGREES);
        double speedInSpeedOutRatio = (double) object
                .get(ManeuverForClassificationJsonSerializer.SPEED_IN_SPEED_OUT_RATIO);
        double oversteeringInDegrees = (double) object
                .get(ManeuverForClassificationJsonSerializer.OVERSTEERING_IN_DEGREES);
        double speedLossRatio = (double) object.get(ManeuverForClassificationJsonSerializer.SPEED_LOSS_RATIO);
        double speedGainRatio = (double) object.get(ManeuverForClassificationJsonSerializer.SPEED_GAIN_RATIO);
        double lowestSpeedVsExitingSpeedRatio = (double) object
                .get(ManeuverForClassificationJsonSerializer.LOWEST_SPEED_VS_EXITING_SPEED_RATIO);
        double maximalTurningRateInDegreesPerSecond = (double) object
                .get(ManeuverForClassificationJsonSerializer.MAXIMAL_TURNING_RATE_IN_DEGREES_PER_SECOND);
        Double deviationFromOptimalTackAngleInDegrees = (Double) object
                .get(ManeuverForClassificationJsonSerializer.DEVIATION_FROM_OPTIMAL_TACK_ANGLE_IN_DEGREES);
        Double deviationFromOptimalJibeAngleInDegrees = (Double) object
                .get(ManeuverForClassificationJsonSerializer.DEVIATION_FROM_OPTIMAL_JIBE_ANGLE_IN_DEGREES);
        double highestAbsoluteDeviationOfBoatsCourseToBearingFromBoatToNextWaypointInDegrees = (double) object.get(
                ManeuverForClassificationJsonSerializer.HIGHEST_ABSOLUTE_DEVIATION_OF_BOATS_COURSE_TO_BEARING_FROM_BOAT_TO_NEXT_WAYPOINT_IN_DEGREES);
        double mainCurveDurationInSeconds = (double) object
                .get(ManeuverForClassificationJsonSerializer.MAIN_CURVE_DURATION_IN_SECONDS);
        double maneuverDurationInSeconds = (double) object
                .get(ManeuverForClassificationJsonSerializer.MANEUVER_DURATION_IN_SECONDS);
        double recoveryPhaseDurationInSeconds = (double) object
                .get(ManeuverForClassificationJsonSerializer.RECOVERY_PHASE_DURATION_IN_SECONDS);
        double timeLossInSeconds = (double) object.get(ManeuverForClassificationJsonSerializer.TIME_LOSS_IN_SECONDS);
        boolean clean = (boolean) object.get(ManeuverForClassificationJsonSerializer.CLEAN);
        ManeuverCategory maneuverCategory = ManeuverCategory
                .valueOf((String) object.get(ManeuverForClassificationJsonSerializer.MANEUVER_CATEGORY));
        double twaBeforeInDegrees = (double) object.get(ManeuverForClassificationJsonSerializer.TWA_BEFORE_IN_DEGREES);
        double twaAfterInDegrees = (double) object.get(ManeuverForClassificationJsonSerializer.TWA_AFTER_IN_DEGREES);
        double twsInKnots = (double) object.get(ManeuverForClassificationJsonSerializer.TWS_IN_KNOTS);
        double speedBeforeInKnots = (double) object.get(ManeuverForClassificationJsonSerializer.SPEED_BEFORE_IN_KNOTS);
        double speedAfterInKnots = (double) object.get(ManeuverForClassificationJsonSerializer.SPEED_AFTER_IN_KNOTS);

        return new ManeuverForClassificationImpl(maneuverType, absoluteTotalCourseChangeInDegrees, speedInSpeedOutRatio,
                oversteeringInDegrees, speedLossRatio, speedGainRatio, lowestSpeedVsExitingSpeedRatio,
                maximalTurningRateInDegreesPerSecond, deviationFromOptimalTackAngleInDegrees,
                deviationFromOptimalJibeAngleInDegrees,
                highestAbsoluteDeviationOfBoatsCourseToBearingFromBoatToNextWaypointInDegrees,
                mainCurveDurationInSeconds, maneuverDurationInSeconds, recoveryPhaseDurationInSeconds,
                timeLossInSeconds, clean, maneuverCategory, twaBeforeInDegrees, twaAfterInDegrees, twsInKnots,
                speedBeforeInKnots, speedAfterInKnots);
    }

}
