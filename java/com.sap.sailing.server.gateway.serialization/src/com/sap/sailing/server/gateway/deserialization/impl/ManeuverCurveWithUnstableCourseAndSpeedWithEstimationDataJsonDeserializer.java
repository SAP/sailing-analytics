package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.maneuverdetection.ManeuverCurveWithUnstableCourseAndSpeedWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataImpl;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer;
import com.sap.sse.common.Duration;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.shared.json.JsonDeserializationException;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonDeserializer
        extends ManeuverCurveBoundariesJsonDeserializer {

    @Override
    public ManeuverCurveWithUnstableCourseAndSpeedWithEstimationData deserialize(JSONObject object)
            throws JsonDeserializationException {
        ManeuverCurveBoundaries boundaries = super.deserialize(object);
        Double avgSpeedBeforeInKnots = (Double) object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.AVERAGE_SPEED_BEFORE_IN_KNOTS);
        Double avgCogBefore = (Double) object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.AVERAGE_COURSE_BEFORE_IN_DEGREES);
        Double secondsBefore = (Double) object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.DURATION_FROM_PREVIOUS_MANEUVER_IN_SECONDS);
        Double avgSpeedAfterInKnots = (Double) object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.AVERAGE_SPEED_AFTER_IN_KNOTS);
        Double avgCogAfter = (Double) object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.AVERAGE_COURSE_AFTER_IN_DEGREES);
        Double secondsAfter = (Double) object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.DURATION_TO_NEXT_MANEUVER_IN_SECONDS);
        Integer gpsFixesCountBefore = CompleteManeuverCurveWithEstimationDataJsonDeserializer.getInteger(object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.GPS_FIXES_COUNT_FROM_PREVIOUS_MANEUVER_IN_SECONDS));
        Integer gpsFixesCountAfter = CompleteManeuverCurveWithEstimationDataJsonDeserializer.getInteger(object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.GPS_FIXES_COUNT_TO_NEXT_MANEUVER_IN_SECONDS));
        Integer gpsFixesCount = CompleteManeuverCurveWithEstimationDataJsonDeserializer.getInteger(
                object.get(ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.GPS_FIXES_COUNT));
        Double longestIntervalBetweenTwoFixes = (Double) object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.LONGEST_INTERVAL_BETWEEN_TWO_FIXES);
        Double intervalBetweenLastFixOfCurveAndNextFix = (Double) object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.INTERVAL_BETWEEN_LAST_FIX_OF_CURVE_AND_NEXT_FIX);
        Double intervalBetweenFirstFixOfCurveAndPreviousFix = (Double) object.get(
                ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer.INTERVAL_BETWEEN_FIRST_FIX_OF_CURVE_AND_PREVIOUS_FIX);
        return new ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataImpl(boundaries.getTimePointBefore(),
                boundaries.getTimePointAfter(), boundaries.getSpeedWithBearingBefore(),
                boundaries.getSpeedWithBearingAfter(), boundaries.getDirectionChangeInDegrees(),
                boundaries.getLowestSpeed(), boundaries.getHighestSpeed(),
                convertSpeedWithBearing(avgSpeedBeforeInKnots, avgCogBefore), convertDuration(secondsBefore),
                gpsFixesCountBefore, convertSpeedWithBearing(avgSpeedAfterInKnots, avgCogAfter),
                convertDuration(secondsAfter), gpsFixesCountAfter, gpsFixesCount,
                convertDuration(longestIntervalBetweenTwoFixes),
                convertDuration(intervalBetweenLastFixOfCurveAndNextFix),
                convertDuration(intervalBetweenFirstFixOfCurveAndPreviousFix));
    }

    private SpeedWithBearing convertSpeedWithBearing(Double speedInKnots, Double cog) {
        return speedInKnots == null || cog == null ? null
                : new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(cog));
    }

    private Duration convertDuration(Double seconds) {
        return seconds == null ? null : new MillisecondsDurationImpl((long) (seconds * 1000.0));
    }

}
