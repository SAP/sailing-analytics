package com.sap.sailing.windestimation.data.serialization;

import org.json.simple.JSONObject;

import com.sap.sailing.server.gateway.serialization.impl.BoatClassJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DetailedBoatClassJsonSerializer;
import com.sap.sailing.windestimation.data.LabeledManeuverForEstimation;
import com.sap.sse.shared.json.JsonSerializer;

public class LabeledManeuverForEstimationJsonSerializer implements JsonSerializer<LabeledManeuverForEstimation> {

    public static final String TIMEPOINT = "unixTime";
    public static final String POSITION_LATITUDE = "posLat";
    public static final String POSITION_LONGITUDE = "posLng";
    public static final String MIDDLE_COURSE = "middleCourse";
    public static final String SPEED_BEFORE = "speedBefore";
    public static final String SPEED_AFTER = "speedAfter";
    public static final String COURSE_BEFORE = "courseBefore";
    public static final String COURSE_AFTER = "courseAfter";
    public static final String COURSE_CHANGE = "maneuverAngle";
    public static final String COURSE_CHANGE_MAIN_CURVE = "mainCurveAngle";
    public static final String MAX_TURNING_RATE = "maxTurnRate";
    public static final String DEVIATION_FROM_OPTIMAL_TACK_ANGLE = "deviationTackAngle";
    public static final String DEVIATION_FROM_OPTIMAL_JIBE_ANGLE = "deviationJibeAngle";
    public static final String SPEED_LOSS_RATIO = "speedLoss";
    public static final String SPEED_GAIN_RATIO = "speedGain";
    public static final String LOWEST_VS_EXITING_SPEED_RATIO = "lowestVsExitingSpeedRatio";
    public static final String CLEAN = "clean";
    public static final String MANEUVER_CATEGORY = "category";
    public static final String SCALED_SPEED_BEFORE_IN_KNOTS = "scaledSpeedBefore";
    public static final String SCALED_SPEED_AFTER_IN_KNOTS = "scaledSpeedAfter";
    public static final String MANEUVER_TYPE = "type";
    public static final String WIND_COURSE = "windCourse";
    public static final String WIND_SPEED = "windSpeed";
    public static final String MARK_PASSING = "markPassing";
    public static final String BOAT_CLASS = "boatClass";
    public static final String MARK_PASSING_DATA_AVAILABLE = "markPassingDataAvailable";
    public static final String REGATTA_NAME = "regattaName";
    public static final String COMPETITOR_NAME = "competitorName";

    private final BoatClassJsonSerializer boatClassSerializer = new DetailedBoatClassJsonSerializer();

    @Override
    public JSONObject serialize(LabeledManeuverForEstimation maneuver) {
        JSONObject json = new JSONObject();
        json.put(TIMEPOINT, maneuver.getManeuverTimePoint().asMillis());
        json.put(POSITION_LATITUDE, maneuver.getManeuverPosition().getLatDeg());
        json.put(POSITION_LONGITUDE, maneuver.getManeuverPosition().getLngDeg());
        json.put(MIDDLE_COURSE, maneuver.getMiddleCourse().getDegrees());
        json.put(SPEED_BEFORE, maneuver.getSpeedWithBearingBefore().getKnots());
        json.put(SPEED_AFTER, maneuver.getSpeedWithBearingAfter().getKnots());
        json.put(COURSE_BEFORE, maneuver.getSpeedWithBearingBefore().getBearing().getDegrees());
        json.put(COURSE_AFTER, maneuver.getSpeedWithBearingAfter().getBearing().getDegrees());
        json.put(COURSE_CHANGE, maneuver.getCourseChangeInDegrees());
        json.put(COURSE_CHANGE_MAIN_CURVE, maneuver.getCourseChangeWithinMainCurveInDegrees());
        json.put(MAX_TURNING_RATE, maneuver.getMaxTurningRateInDegreesPerSecond());
        json.put(DEVIATION_FROM_OPTIMAL_TACK_ANGLE, maneuver.getDeviationFromOptimalTackAngleInDegrees());
        json.put(DEVIATION_FROM_OPTIMAL_JIBE_ANGLE, maneuver.getDeviationFromOptimalJibeAngleInDegrees());
        json.put(SPEED_LOSS_RATIO, maneuver.getSpeedLossRatio());
        json.put(SPEED_GAIN_RATIO, maneuver.getSpeedGainRatio());
        json.put(LOWEST_VS_EXITING_SPEED_RATIO, maneuver.getLowestSpeedVsExitingSpeedRatio());
        json.put(CLEAN, maneuver.isClean());
        json.put(MANEUVER_CATEGORY, maneuver.getManeuverCategory().name());
        json.put(SCALED_SPEED_BEFORE_IN_KNOTS, maneuver.getScaledSpeedBefore());
        json.put(SCALED_SPEED_AFTER_IN_KNOTS, maneuver.getScaledSpeedAfter());
        json.put(MARK_PASSING, maneuver.isMarkPassing());
        json.put(BOAT_CLASS, boatClassSerializer.serialize(maneuver.getBoatClass()));
        json.put(MARK_PASSING_DATA_AVAILABLE, maneuver.isMarkPassingDataAvailable());
        json.put(MANEUVER_TYPE, maneuver.getManeuverType() == null ? null : maneuver.getManeuverType().name());
        json.put(WIND_SPEED, maneuver.getWind().getKnots());
        json.put(WIND_COURSE, maneuver.getWind().getBearing().getDegrees());
        json.put(REGATTA_NAME, maneuver.getRegattaName());
        json.put(COMPETITOR_NAME, maneuver.getCompetitorName());
        return json;
    }

}
