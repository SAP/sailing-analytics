package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.maneuverdetection.ManeuverMainCurveWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverMainCurveWithEstimationDataImpl;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverMainCurveWithEstimationDataJsonSerializer;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverMainCurveWithEstimationDataJsonDeserializer extends ManeuverCurveBoundariesJsonDeserializer {

    @Override
    public ManeuverMainCurveWithEstimationData deserialize(JSONObject object) throws JsonDeserializationException {
        ManeuverCurveBoundaries boundaries = super.deserialize(object);
        Double courseAtLowestSpeed = (Double) object
                .get(ManeuverMainCurveWithEstimationDataJsonSerializer.COURSE_AT_LOWEST_SPEED);
        Double courseAtHighestSpeed = (Double) object
                .get(ManeuverMainCurveWithEstimationDataJsonSerializer.COURSE_AT_HIGHEST_SPEED);
        Long lowestSpeedTimePointMillis = (Long) object
                .get(ManeuverMainCurveWithEstimationDataJsonSerializer.LOWEST_SPEED_TIMEPOINT);
        Long highestSpeedTimePointMillis = (Long) object
                .get(ManeuverMainCurveWithEstimationDataJsonSerializer.HIGHEST_SPEED_TIMEPOINT);
        Double courseAtMaxTurningRate = (Double) object
                .get(ManeuverMainCurveWithEstimationDataJsonSerializer.COURSE_AT_MAX_TURNING_RATE);
        Long maxTurningRateTimePointMillis = (Long) object
                .get(ManeuverMainCurveWithEstimationDataJsonSerializer.MAX_TURNING_RATE_TIMEPOINT);
        Double avgTurningRateInDegreesPerSecond = (Double) object
                .get(ManeuverMainCurveWithEstimationDataJsonSerializer.AVG_TURNING_RATE_IN_DEGREES_PER_SECOND);
        Double maxTurningRateInDegreesPerSecond = (Double) object
                .get(ManeuverMainCurveWithEstimationDataJsonSerializer.MAX_TURNING_RATE_IN_DEGREES_PER_SECOND);
        Integer gpsFixesCount = CompleteManeuverCurveWithEstimationDataJsonDeserializer
                .getInteger(object.get(ManeuverMainCurveWithEstimationDataJsonSerializer.GPS_FIXES_COUNT));
        Double longestIntervalBetweenTwoFixes = (Double) object
                .get(ManeuverMainCurveWithEstimationDataJsonSerializer.LONGEST_INTERVAL_BETWEEN_TWO_FIXES);
        return new ManeuverMainCurveWithEstimationDataImpl(boundaries.getTimePointBefore(),
                boundaries.getTimePointAfter(), boundaries.getSpeedWithBearingBefore(),
                boundaries.getSpeedWithBearingAfter(), boundaries.getDirectionChangeInDegrees(),
                new KnotSpeedWithBearingImpl(boundaries.getLowestSpeed().getKnots(),
                        new DegreeBearingImpl(courseAtLowestSpeed)),
                new MillisecondsTimePoint(lowestSpeedTimePointMillis),
                new KnotSpeedWithBearingImpl(boundaries.getHighestSpeed().getKnots(),
                        new DegreeBearingImpl(courseAtHighestSpeed)),
                new MillisecondsTimePoint(highestSpeedTimePointMillis),
                new MillisecondsTimePoint(maxTurningRateTimePointMillis), maxTurningRateInDegreesPerSecond,
                new DegreeBearingImpl(courseAtMaxTurningRate), avgTurningRateInDegreesPerSecond, gpsFixesCount,
                new MillisecondsDurationImpl((long) (longestIntervalBetweenTwoFixes * 1000.0)));
    }

}
