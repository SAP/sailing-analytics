package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.ManeuverCurveWithUnstableCourseAndSpeedWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.ManeuverMainCurveWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.impl.CompleteManeuverCurveWithEstimationDataImpl;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.CompleteManeuverCurveWithEstimationDataJsonSerializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class CompleteManeuverCurveWithEstimationDataJsonDeserializer
        implements JsonDeserializer<CompleteManeuverCurveWithEstimationData> {

    private final ManeuverMainCurveWithEstimationDataJsonDeserializer mainCurveDeserializer;
    private final ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonDeserializer curveWithUnstableCourseAndSpeedDeserializer;
    private final WindJsonDeserializer windDeserializer;
    private final PositionJsonDeserializer positionDeserializer;

    public CompleteManeuverCurveWithEstimationDataJsonDeserializer(
            ManeuverMainCurveWithEstimationDataJsonDeserializer mainCurveDeserializer,
            ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonDeserializer curveWithUnstableCourseAndSpeedDeserializer,
            WindJsonDeserializer windDeserializer, PositionJsonDeserializer positionDeserializer) {
        this.mainCurveDeserializer = mainCurveDeserializer;
        this.curveWithUnstableCourseAndSpeedDeserializer = curveWithUnstableCourseAndSpeedDeserializer;
        this.windDeserializer = windDeserializer;
        this.positionDeserializer = positionDeserializer;
    }

    @Override
    public CompleteManeuverCurveWithEstimationData deserialize(JSONObject object) throws JsonDeserializationException {
        Position position = positionDeserializer
                .deserialize((JSONObject) object.get(CompleteManeuverCurveWithEstimationDataJsonSerializer.POSITION));
        boolean markPassing = (Boolean) object.get(CompleteManeuverCurveWithEstimationDataJsonSerializer.MARK_PASSING);
        ManeuverMainCurveWithEstimationData mainCurve = mainCurveDeserializer
                .deserialize((JSONObject) object.get(CompleteManeuverCurveWithEstimationDataJsonSerializer.MAIN_CURVE));
        ManeuverCurveWithUnstableCourseAndSpeedWithEstimationData curveWithUnstableCourseAndSpeed = curveWithUnstableCourseAndSpeedDeserializer
                .deserialize((JSONObject) object.get(
                        CompleteManeuverCurveWithEstimationDataJsonSerializer.CURVE_WITH_UNSTABLE_COURSE_AND_SPEED));
        JSONObject windJson = (JSONObject) object.get(CompleteManeuverCurveWithEstimationDataJsonSerializer.WIND);
        Wind wind = windJson == null ? null : windDeserializer.deserialize(windJson);
        Integer tackingCount = getInteger(
                object.get(CompleteManeuverCurveWithEstimationDataJsonSerializer.TACKING_COUNT));
        Integer jibingCount = getInteger(
                object.get(CompleteManeuverCurveWithEstimationDataJsonSerializer.JIBING_COUNT));
        Boolean maneuverStartsByRunningAwayFromWind = (Boolean) object
                .get(CompleteManeuverCurveWithEstimationDataJsonSerializer.MANEUVER_STARTS_BY_RUNNING_AWAY_FROM_WIND);
        Double relativeBearingToNextMarkBeforeManeuver = (Double) object.get(
                CompleteManeuverCurveWithEstimationDataJsonSerializer.RELATIVE_BEARING_TO_NEXT_MARK_BEFORE_MANEUVER);
        Double relativeBearingToNextMarkAfterManeuver = (Double) object.get(
                CompleteManeuverCurveWithEstimationDataJsonSerializer.RELATIVE_BEARING_TO_NEXT_MARK_AFTER_MANEUVER);
        Double closestDistanceToMarkInMeters = (Double) object
                .get(CompleteManeuverCurveWithEstimationDataJsonSerializer.CLOSEST_DISTANCE_TO_MARK);
        Double deviationFromTargetTackAngle = (Double) object
                .get(CompleteManeuverCurveWithEstimationDataJsonSerializer.DEVIATION_FROM_TARGET_TACK_ANGLE);
        Double deviationFromTargetJibeAngle = (Double) object
                .get(CompleteManeuverCurveWithEstimationDataJsonSerializer.DEVIATION_FROM_TARGET_JIBE_ANGLE);
        return new CompleteManeuverCurveWithEstimationDataImpl(position, mainCurve, curveWithUnstableCourseAndSpeed,
                wind, tackingCount, jibingCount, maneuverStartsByRunningAwayFromWind,
                convertBearing(relativeBearingToNextMarkBeforeManeuver),
                convertBearing(relativeBearingToNextMarkAfterManeuver), markPassing,
                convertDistance(closestDistanceToMarkInMeters), deviationFromTargetTackAngle,
                deviationFromTargetJibeAngle);
    }

    private Bearing convertBearing(Double degrees) {
        return degrees == null ? null : new DegreeBearingImpl(degrees);
    }

    private Distance convertDistance(Double meters) {
        return meters == null ? null : new MeterDistance(meters);
    }

    public static Integer getInteger(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Integer) {
            return (Integer) object;
        }
        return ((Long) object).intValue();
    }

}
