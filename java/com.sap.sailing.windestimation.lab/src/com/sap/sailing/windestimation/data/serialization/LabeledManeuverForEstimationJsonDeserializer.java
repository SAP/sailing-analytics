package com.sap.sailing.windestimation.data.serialization;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.server.gateway.deserialization.impl.BoatClassJsonDeserializer;
import com.sap.sailing.windestimation.data.LabeledManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverCategory;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class LabeledManeuverForEstimationJsonDeserializer implements JsonDeserializer<LabeledManeuverForEstimation> {

    private final BoatClassJsonDeserializer boatClassDeserializer = new BoatClassJsonDeserializer(
            DomainFactory.INSTANCE);

    @Override
    public LabeledManeuverForEstimation deserialize(JSONObject object) throws JsonDeserializationException {
        long maneuverTimePointMillis = (long) object.get(LabeledManeuverForEstimationJsonSerializer.TIMEPOINT);
        double positionLatitude = (double) object.get(LabeledManeuverForEstimationJsonSerializer.POSITION_LATITUDE);
        double positionLongitude = (double) object.get(LabeledManeuverForEstimationJsonSerializer.POSITION_LONGITUDE);
        double middleCourseInDegrees = (double) object.get(LabeledManeuverForEstimationJsonSerializer.MIDDLE_COURSE);
        double speedBeforeInKnots = (double) object.get(LabeledManeuverForEstimationJsonSerializer.SPEED_BEFORE);
        double speedAfterInKnots = (double) object.get(LabeledManeuverForEstimationJsonSerializer.SPEED_AFTER);
        double cogBefore = (double) object.get(LabeledManeuverForEstimationJsonSerializer.COURSE_BEFORE);
        double cogAfter = (double) object.get(LabeledManeuverForEstimationJsonSerializer.COURSE_AFTER);
        double courseChangeInDegrees = (double) object.get(LabeledManeuverForEstimationJsonSerializer.COURSE_CHANGE);
        double courseChangeMainCurveInDegrees = (double) object
                .get(LabeledManeuverForEstimationJsonSerializer.COURSE_CHANGE_MAIN_CURVE);
        double maxTurningRateInDegrees = (double) object
                .get(LabeledManeuverForEstimationJsonSerializer.MAX_TURNING_RATE);
        Double deviationFromOptimalTackAngleInDegrees = (Double) object
                .get(LabeledManeuverForEstimationJsonSerializer.DEVIATION_FROM_OPTIMAL_TACK_ANGLE);
        Double deviationFromOptimalJibeAngleInDegrees = (Double) object
                .get(LabeledManeuverForEstimationJsonSerializer.DEVIATION_FROM_OPTIMAL_JIBE_ANGLE);
        double speedLossRatio = (double) object.get(LabeledManeuverForEstimationJsonSerializer.SPEED_LOSS_RATIO);
        double speedGainRatio = (double) object.get(LabeledManeuverForEstimationJsonSerializer.SPEED_GAIN_RATIO);
        double lowestVsExitingSpeedRatio = (double) object
                .get(LabeledManeuverForEstimationJsonSerializer.LOWEST_VS_EXITING_SPEED_RATIO);
        boolean clean = (boolean) object.get(LabeledManeuverForEstimationJsonSerializer.CLEAN);
        ManeuverCategory maneuverCategory = ManeuverCategory
                .valueOf((String) object.get(LabeledManeuverForEstimationJsonSerializer.MANEUVER_CATEGORY));
        double scaledSpeedBefore = (double) object
                .get(LabeledManeuverForEstimationJsonSerializer.SCALED_SPEED_BEFORE_IN_KNOTS);
        double scaledSpeedAfter = (double) object
                .get(LabeledManeuverForEstimationJsonSerializer.SCALED_SPEED_AFTER_IN_KNOTS);
        BoatClass boatClass = boatClassDeserializer
                .deserialize((JSONObject) object.get(LabeledManeuverForEstimationJsonSerializer.BOAT_CLASS));
        boolean markPassing = (boolean) object.get(LabeledManeuverForEstimationJsonSerializer.MARK_PASSING);
        boolean markPassingDataAvailable = (boolean) object
                .get(LabeledManeuverForEstimationJsonSerializer.MARK_PASSING_DATA_AVAILABLE);
        String maneuverTypeStr = (String) object.get(LabeledManeuverForEstimationJsonSerializer.MANEUVER_TYPE);
        ManeuverTypeForClassification maneuverType = maneuverTypeStr == null ? null
                : ManeuverTypeForClassification.valueOf(maneuverTypeStr);
        Double windSpeedInKnots = (Double) object.get(LabeledManeuverForEstimationJsonSerializer.WIND_SPEED);
        Double windCourse = (Double) object.get(LabeledManeuverForEstimationJsonSerializer.WIND_COURSE);
        String regattaName = (String) object.get(LabeledManeuverForEstimationJsonSerializer.REGATTA_NAME);
        String competitorName = (String) object.get(LabeledManeuverForEstimationJsonSerializer.COMPETITOR_NAME);
        MillisecondsTimePoint maneuverTimePoint = new MillisecondsTimePoint(maneuverTimePointMillis);
        DegreePosition maneuverPosition = new DegreePosition(positionLatitude, positionLongitude);
        LabeledManeuverForEstimation maneuver = new LabeledManeuverForEstimation(maneuverTimePoint, maneuverPosition,
                new DegreeBearingImpl(middleCourseInDegrees),
                new KnotSpeedWithBearingImpl(speedBeforeInKnots, new DegreeBearingImpl(cogBefore)),
                new KnotSpeedWithBearingImpl(speedAfterInKnots, new DegreeBearingImpl(cogAfter)), courseChangeInDegrees,
                courseChangeMainCurveInDegrees, maxTurningRateInDegrees, deviationFromOptimalTackAngleInDegrees,
                deviationFromOptimalJibeAngleInDegrees, speedLossRatio, speedGainRatio, lowestVsExitingSpeedRatio,
                clean, maneuverCategory, scaledSpeedBefore, scaledSpeedAfter, markPassing, boatClass,
                markPassingDataAvailable, maneuverType,
                new WindImpl(maneuverPosition, maneuverTimePoint,
                        new KnotSpeedWithBearingImpl(windSpeedInKnots, new DegreeBearingImpl(windCourse))),
                regattaName, competitorName);
        return maneuver;
    }

}
