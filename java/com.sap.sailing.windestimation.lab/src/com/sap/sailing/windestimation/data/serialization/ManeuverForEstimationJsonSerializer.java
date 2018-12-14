package com.sap.sailing.windestimation.data.serialization;

import org.json.simple.JSONObject;

import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.BoatClassJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DetailedBoatClassJsonSerializer;
import com.sap.sailing.windestimation.data.LabelledManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;

public class ManeuverForEstimationJsonSerializer implements JsonSerializer<ManeuverForEstimation> {

    public static final String TIMEPOINT = "unixTime";
    public static final String POSITION_LATITUDE = "posLat";
    public static final String POSITION_LONGITUDE = "posLng";
    public static final String MIDDLE_COURSE = "middleCourse";
    public static final String SPEED_BEFORE = "speedBefore";
    public static final String SPEED_AFTER = "speedAfter";
    public static final String COURSE_BEFORE = "courseBefore";
    public static final String COURSE_AFTER = "courseAfter";
    public static final String COURSE_AT_LOWEST_SPEED = "courseLowestSpeed";
    public static final String AVG_SPEED_BEFORE = "avgSpeedBefore";
    public static final String AVG_SPEED_AFTER = "avgSpeedAfter";
    public static final String AVG_COURSE_BEFORE = "avgCourseBefore";
    public static final String AVG_COURSE_AFTER = "avgCourseAfter";
    public static final String COURSE_CHANGE = "maneuverAngle";
    public static final String COURSE_CHANGE_MAIN_CURVE = "mainCurveAngle";
    public static final String MAX_TURNING_RATE = "maxTurnRate";
    public static final String DEVIATION_FROM_OPTIMAL_TACK_ANGLE = "deviationTackAngle";
    public static final String DEVIATION_FROM_OPTIMAL_JIBE_ANGLE = "deviationJibeAngle";
    public static final String SPEED_LOSS_RATIO = "speedLoss";
    public static final String SPEED_GAIN_RATIO = "speedGain";
    public static final String LOWEST_VS_EXITING_SPEED_RATIO = "lowestVsExitingSpeedRatio";
    public static final String CLEAN = "clean";
    public static final String CLEAN_BEFORE = "cleanBefore";
    public static final String CLEAN_AFTER = "cleanAfter";
    public static final String MANEUVER_CATEGORY = "category";
    public static final String SCALED_SPEED_BEFORE_IN_KNOTS = "scaledSpeedBefore";
    public static final String SCALED_SPEED_AFTER_IN_KNOTS = "scaledSpeedAfter";
    public static final String BOAT_CLASS = "boatClass";
    public static final String MANEUVER_TYPE = "type";
    public static final String WIND_COURSE = "windCourse";
    public static final String WIND_SPEED = "windSpeed";
    public static final String RELATIVE_BEARING_TO_NEXT_MARK_BEFORE_IN_DEGREES = "nextMarkBefore";
    public static final String RELATIVE_BEARING_TO_NEXT_MARK_AFTER_IN_DEGREES = "nextMarkAfter";
    public static final String MARK_PASSING = "markPassing";

    private final BoatClassJsonSerializer boatClassSerializer = new DetailedBoatClassJsonSerializer();

    @Override
    public JSONObject serialize(ManeuverForEstimation maneuver) {
        JSONObject json = new JSONObject();
        json.put(TIMEPOINT, maneuver.getManeuverTimePoint().asMillis());
        json.put(POSITION_LATITUDE, maneuver.getManeuverPosition().getLatDeg());
        json.put(POSITION_LONGITUDE, maneuver.getManeuverPosition().getLngDeg());
        json.put(MIDDLE_COURSE, maneuver.getMiddleCourse().getDegrees());
        json.put(SPEED_BEFORE, maneuver.getSpeedWithBearingBefore().getKnots());
        json.put(SPEED_AFTER, maneuver.getSpeedWithBearingAfter().getKnots());
        json.put(COURSE_BEFORE, maneuver.getSpeedWithBearingBefore().getBearing().getDegrees());
        json.put(COURSE_AFTER, maneuver.getSpeedWithBearingAfter().getBearing().getDegrees());
        json.put(COURSE_AT_LOWEST_SPEED, maneuver.getCourseAtLowestSpeed().getDegrees());
        json.put(AVG_SPEED_BEFORE, maneuver.getAverageSpeedWithBearingBefore() == null ? null
                : maneuver.getAverageSpeedWithBearingBefore().getKnots());
        json.put(AVG_SPEED_AFTER, maneuver.getAverageSpeedWithBearingAfter() == null ? null
                : maneuver.getAverageSpeedWithBearingAfter().getKnots());
        json.put(AVG_COURSE_BEFORE,
                maneuver.getAverageSpeedWithBearingBefore() == null ? maneuver.getAverageSpeedWithBearingBefore()
                        : maneuver.getAverageSpeedWithBearingBefore().getBearing().getDegrees());
        json.put(AVG_COURSE_AFTER, maneuver.getAverageSpeedWithBearingAfter() == null ? null
                : maneuver.getAverageSpeedWithBearingAfter().getBearing().getDegrees());
        json.put(COURSE_CHANGE, maneuver.getCourseChangeInDegrees());
        json.put(COURSE_CHANGE_MAIN_CURVE, maneuver.getCourseChangeWithinMainCurveInDegrees());
        json.put(MAX_TURNING_RATE, maneuver.getMaxTurningRateInDegreesPerSecond());
        json.put(DEVIATION_FROM_OPTIMAL_TACK_ANGLE, maneuver.getDeviationFromOptimalTackAngleInDegrees());
        json.put(DEVIATION_FROM_OPTIMAL_JIBE_ANGLE, maneuver.getDeviationFromOptimalJibeAngleInDegrees());
        json.put(SPEED_LOSS_RATIO, maneuver.getSpeedLossRatio());
        json.put(SPEED_GAIN_RATIO, maneuver.getSpeedGainRatio());
        json.put(LOWEST_VS_EXITING_SPEED_RATIO, maneuver.getLowestSpeedVsExitingSpeedRatio());
        json.put(CLEAN, maneuver.isClean());
        json.put(CLEAN_BEFORE, maneuver.isCleanBefore());
        json.put(CLEAN_AFTER, maneuver.isCleanAfter());
        json.put(MANEUVER_CATEGORY, maneuver.getManeuverCategory().name());
        json.put(SCALED_SPEED_BEFORE_IN_KNOTS, maneuver.getScaledSpeedBefore());
        json.put(SCALED_SPEED_AFTER_IN_KNOTS, maneuver.getScaledSpeedAfter());
        json.put(BOAT_CLASS, boatClassSerializer.serialize(maneuver.getBoatClass()));
        json.put(RELATIVE_BEARING_TO_NEXT_MARK_BEFORE_IN_DEGREES, maneuver.getRelativeBearingToNextMarkBefore());
        json.put(RELATIVE_BEARING_TO_NEXT_MARK_AFTER_IN_DEGREES, maneuver.getRelativeBearingToNextMarkAfter());
        json.put(MARK_PASSING, maneuver.isMarkPassing());
        if (maneuver instanceof LabelledManeuverForEstimation) {
            LabelledManeuverForEstimation labelledManeuver = (LabelledManeuverForEstimation) maneuver;
            json.put(MANEUVER_TYPE,
                    labelledManeuver.getManeuverType() == null ? null : labelledManeuver.getManeuverType().name());
            json.put(WIND_SPEED, labelledManeuver.getWind().getKnots());
            json.put(WIND_COURSE, labelledManeuver.getWind().getBearing().getDegrees());
        }
        return json;
    }

}
