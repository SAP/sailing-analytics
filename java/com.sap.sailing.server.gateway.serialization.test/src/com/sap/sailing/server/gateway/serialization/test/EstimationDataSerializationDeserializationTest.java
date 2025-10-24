package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.BoatHullType;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.ManeuverMainCurveWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.impl.CompleteManeuverCurveWithEstimationDataImpl;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataImpl;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverMainCurveWithEstimationDataImpl;
import com.sap.sailing.server.gateway.deserialization.impl.BoatClassJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.CompleteManeuverCurveWithEstimationDataJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.ManeuverMainCurveWithEstimationDataJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.ManeuverWindJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.PositionJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.CompleteManeuverCurveWithEstimationDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DetailedBoatClassJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverMainCurveWithEstimationDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverWindJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PositionJsonSerializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class EstimationDataSerializationDeserializationTest {

    private static final double DELTA = 0.000001;
    protected SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");

    @Test
    public void testCompleteManeuverCurveWithEstimationData() throws ParseException, JsonDeserializationException {
        MillisecondsTimePoint timePointMainCurveBefore = new MillisecondsTimePoint(
                dateFormat.parse("06/23/2011-15:28:24"));
        MillisecondsTimePoint timePointMainCurveAfter = new MillisecondsTimePoint(
                dateFormat.parse("06/23/2011-15:28:34"));
        SpeedWithBearing speedWithBearingMainCurveBefore = new KnotSpeedWithBearingImpl(5, new DegreeBearingImpl(30));
        SpeedWithBearing speedWithBearingMainCurveAfter = new KnotSpeedWithBearingImpl(5.5, new DegreeBearingImpl(90));
        double directionChangeMainCurveInDegrees = 60;
        SpeedWithBearing lowestSpeed = new KnotSpeedWithBearingImpl(3, new DegreeBearingImpl(60.5));
        MillisecondsTimePoint lowestSpeedTimePoint = new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:28:29"));
        SpeedWithBearing highestSpeed = new KnotSpeedWithBearingImpl(6, new DegreeBearingImpl(65));
        MillisecondsTimePoint highestSpeedTimePoint = new MillisecondsTimePoint(
                dateFormat.parse("06/23/2011-15:28:32"));
        double maxTurningRateInDegreesPerSecond = 11.2043;
        double avgTurningRateInDegreesPerSecond = 9;
        Bearing courseAtMaxTurningRate = new DegreeBearingImpl(70.2244242);
        MillisecondsTimePoint maxTurningRateTimePoint = new MillisecondsTimePoint(
                dateFormat.parse("06/23/2011-15:28:33"));
        int gpsFixesCountMainCurve = 4;
        Duration longestIntervalBetweenTwoFixesMainCurve = new MillisecondsDurationImpl(4253);
        ManeuverMainCurveWithEstimationData mainCurve = new ManeuverMainCurveWithEstimationDataImpl(
                timePointMainCurveBefore, timePointMainCurveAfter, speedWithBearingMainCurveBefore,
                speedWithBearingMainCurveAfter, directionChangeMainCurveInDegrees, lowestSpeed, lowestSpeedTimePoint,
                highestSpeed, highestSpeedTimePoint, maxTurningRateTimePoint, maxTurningRateInDegreesPerSecond,
                courseAtMaxTurningRate, avgTurningRateInDegreesPerSecond, gpsFixesCountMainCurve,
                longestIntervalBetweenTwoFixesMainCurve);

        MillisecondsTimePoint timePointBefore = new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:28:10"));
        MillisecondsTimePoint timePointAfter = new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:28:55"));
        SpeedWithBearing speedWithBearingBefore = new KnotSpeedWithBearingImpl(6, new DegreeBearingImpl(20));
        SpeedWithBearing speedWithBearingAfter = new KnotSpeedWithBearingImpl(7.5, new DegreeBearingImpl(101.1));
        double directionChangeInDegrees = 81.1;
        Speed maneuverLowestSpeed = new KnotSpeedImpl(2.9);
        Speed maneuverHighestSpeed = new KnotSpeedImpl(5.4);
        SpeedWithBearing avgSpeedWithBearingBefore = new KnotSpeedWithBearingImpl(4.999, new DegreeBearingImpl(32.202));
        SpeedWithBearing avgSpeedWithBearingAfter = new KnotSpeedWithBearingImpl(5.3333, new DegreeBearingImpl(90.234));
        Duration durationFromPreviousManeuverEndToManeuverStart = new MillisecondsDurationImpl(10001);
        Duration durationFromManeuverEndToNextManeuverStart = new MillisecondsDurationImpl(20000);
        int gpsFixesCountFromPreviousManeuverEndToManeuverStart = 2;
        int gpsFixesCountFromManeuverEndToNextManeuverStart = 222;
        int gpsFixesCount = 133445;
        Duration longestIntervalBetweenTwoFixes = new MillisecondsDurationImpl(5643);
        Duration intervalBetweenLastFixOfCurveAndNextFix = new MillisecondsDurationImpl(1574);
        Duration intervalBetweenFirstFixOfCurveAndPreviousFix = new MillisecondsDurationImpl(78566);
        ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataImpl curve = new ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataImpl(
                timePointBefore, timePointAfter, speedWithBearingBefore, speedWithBearingAfter,
                directionChangeInDegrees, maneuverLowestSpeed, maneuverHighestSpeed, avgSpeedWithBearingBefore,
                durationFromPreviousManeuverEndToManeuverStart, gpsFixesCountFromPreviousManeuverEndToManeuverStart,
                avgSpeedWithBearingAfter, durationFromManeuverEndToNextManeuverStart,
                gpsFixesCountFromManeuverEndToNextManeuverStart, gpsFixesCount, longestIntervalBetweenTwoFixes,
                intervalBetweenLastFixOfCurveAndNextFix, intervalBetweenFirstFixOfCurveAndPreviousFix);

        SpeedWithBearing windSpeedWithBearing = new KnotSpeedWithBearingImpl(2, new DegreeBearingImpl(340));
        DegreePosition maneuverPosition = new DegreePosition(50.325246, 11.148556);
        Wind wind = new WindImpl(maneuverPosition, mainCurve.getTimePointOfMaxTurningRate(), windSpeedWithBearing);
        int jibingCount = 203;
        int tackingCount = 12345;
        boolean maneuverStartsByRunningAwayFromWind = true;
        Bearing relativeBearingToNextMarkBeforeManeuver = new DegreeBearingImpl(202.23);
        Bearing relativeBearingToNextMarkAfterManeuver = new DegreeBearingImpl(10.01);
        boolean markPassing = true;
        Distance closestDistanceToMark = new MeterDistance(3.0);
        Double targetTackAngle = 23.30;
        Double targetJibeAngle = 22.30;

        CompleteManeuverCurveWithEstimationData toSerialize = new CompleteManeuverCurveWithEstimationDataImpl(
                maneuverPosition, mainCurve, curve, wind, tackingCount, jibingCount,
                maneuverStartsByRunningAwayFromWind, relativeBearingToNextMarkBeforeManeuver,
                relativeBearingToNextMarkAfterManeuver, markPassing, closestDistanceToMark, targetTackAngle,
                targetJibeAngle);
        CompleteManeuverCurveWithEstimationDataJsonSerializer serializer = new CompleteManeuverCurveWithEstimationDataJsonSerializer(
                new ManeuverMainCurveWithEstimationDataJsonSerializer(),
                new ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer(),
                new ManeuverWindJsonSerializer(), new PositionJsonSerializer());
        JSONObject json = serializer.serialize(toSerialize);
        CompleteManeuverCurveWithEstimationDataJsonDeserializer deserializer = new CompleteManeuverCurveWithEstimationDataJsonDeserializer(
                new ManeuverMainCurveWithEstimationDataJsonDeserializer(),
                new ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonDeserializer(),
                new ManeuverWindJsonDeserializer(), new PositionJsonDeserializer());
        CompleteManeuverCurveWithEstimationData deserialized = deserializer.deserialize(json);

        assertEquals(targetJibeAngle, deserialized.getTargetJibeAngleInDegrees());
        assertEquals(targetTackAngle, deserialized.getTargetTackAngleInDegrees());
        assertEquals(closestDistanceToMark, deserialized.getDistanceToClosestMark());
        assertEquals(maneuverPosition, deserialized.getPosition());
        assertEquals(tackingCount, deserialized.getTackingCount());
        assertEquals(jibingCount, deserialized.getJibingCount());
        assertEquals(maneuverStartsByRunningAwayFromWind, deserialized.isManeuverStartsByRunningAwayFromWind());
        assertEquals(markPassing, deserialized.isMarkPassing());
        assertEquals(relativeBearingToNextMarkBeforeManeuver,
                deserialized.getRelativeBearingToNextMarkBeforeManeuver());
        assertEquals(relativeBearingToNextMarkAfterManeuver, deserialized.getRelativeBearingToNextMarkAfterManeuver());
        assertEquals(wind.getTimePoint(), deserialized.getWind().getTimePoint());
        assertEquals(wind.getPosition(), deserialized.getWind().getPosition());
        assertEquals(windSpeedWithBearing.getBearing(), deserialized.getWind().getBearing());
        assertEquals(windSpeedWithBearing.getMetersPerSecond(), deserialized.getWind().getMetersPerSecond(), DELTA);

        assertEquals(timePointBefore, deserialized.getCurveWithUnstableCourseAndSpeed().getTimePointBefore());
        assertEquals(timePointAfter, deserialized.getCurveWithUnstableCourseAndSpeed().getTimePointAfter());
        assertEquals(speedWithBearingBefore,
                deserialized.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingBefore());
        assertEquals(speedWithBearingAfter,
                deserialized.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter());
        assertEquals(directionChangeInDegrees,
                deserialized.getCurveWithUnstableCourseAndSpeed().getDirectionChangeInDegrees(), DELTA);
        assertEquals(maneuverLowestSpeed, deserialized.getCurveWithUnstableCourseAndSpeed().getLowestSpeed());
        assertEquals(avgSpeedWithBearingBefore,
                deserialized.getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingBefore());
        assertEquals(avgSpeedWithBearingAfter,
                deserialized.getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingAfter());
        assertEquals(durationFromPreviousManeuverEndToManeuverStart,
                deserialized.getCurveWithUnstableCourseAndSpeed().getDurationFromPreviousManeuverEndToManeuverStart());
        assertEquals(durationFromManeuverEndToNextManeuverStart,
                deserialized.getCurveWithUnstableCourseAndSpeed().getDurationFromManeuverEndToNextManeuverStart());
        assertEquals(gpsFixesCountFromPreviousManeuverEndToManeuverStart, deserialized
                .getCurveWithUnstableCourseAndSpeed().getGpsFixesCountFromPreviousManeuverEndToManeuverStart());
        assertEquals(gpsFixesCountFromManeuverEndToNextManeuverStart,
                deserialized.getCurveWithUnstableCourseAndSpeed().getGpsFixesCountFromManeuverEndToNextManeuverStart());
        assertEquals(gpsFixesCount, deserialized.getCurveWithUnstableCourseAndSpeed().getGpsFixesCount());
        assertEquals(longestIntervalBetweenTwoFixes,
                deserialized.getCurveWithUnstableCourseAndSpeed().getLongestIntervalBetweenTwoFixes());
        assertEquals(intervalBetweenLastFixOfCurveAndNextFix,
                deserialized.getCurveWithUnstableCourseAndSpeed().getIntervalBetweenLastFixOfCurveAndNextFix());
        assertEquals(intervalBetweenFirstFixOfCurveAndPreviousFix,
                deserialized.getCurveWithUnstableCourseAndSpeed().getIntervalBetweenFirstFixOfCurveAndPreviousFix());

        assertEquals(timePointMainCurveBefore, deserialized.getMainCurve().getTimePointBefore());
        assertEquals(timePointMainCurveAfter, deserialized.getMainCurve().getTimePointAfter());
        assertEquals(speedWithBearingMainCurveBefore, deserialized.getMainCurve().getSpeedWithBearingBefore());
        assertEquals(speedWithBearingMainCurveAfter, deserialized.getMainCurve().getSpeedWithBearingAfter());
        assertEquals(directionChangeMainCurveInDegrees, deserialized.getMainCurve().getDirectionChangeInDegrees(),
                DELTA);
        assertEquals(lowestSpeed, deserialized.getMainCurve().getLowestSpeed());
        assertEquals(lowestSpeedTimePoint, deserialized.getMainCurve().getLowestSpeedTimePoint());
        assertEquals(highestSpeed, deserialized.getMainCurve().getHighestSpeed());
        assertEquals(highestSpeedTimePoint, deserialized.getMainCurve().getHighestSpeedTimePoint());
        assertEquals(maxTurningRateInDegreesPerSecond,
                deserialized.getMainCurve().getMaxTurningRateInDegreesPerSecond(), DELTA);
        assertEquals(avgTurningRateInDegreesPerSecond,
                deserialized.getMainCurve().getAvgTurningRateInDegreesPerSecond(), DELTA);
        assertEquals(courseAtMaxTurningRate, deserialized.getMainCurve().getCourseAtMaxTurningRate());
        assertEquals(maxTurningRateTimePoint, deserialized.getMainCurve().getTimePointOfMaxTurningRate());
        assertEquals(gpsFixesCountMainCurve, deserialized.getMainCurve().getGpsFixesCount());
        assertEquals(longestIntervalBetweenTwoFixesMainCurve,
                deserialized.getMainCurve().getLongestIntervalBetweenTwoFixes());
    }

    @Test
    public void testCompleteManeuverCurveWithEstimationDataNullValues()
            throws ParseException, JsonDeserializationException {
        MillisecondsTimePoint timePointMainCurveBefore = new MillisecondsTimePoint(
                dateFormat.parse("06/23/2011-15:28:24"));
        MillisecondsTimePoint timePointMainCurveAfter = new MillisecondsTimePoint(
                dateFormat.parse("06/23/2011-15:28:34"));
        SpeedWithBearing speedWithBearingMainCurveBefore = new KnotSpeedWithBearingImpl(5, new DegreeBearingImpl(30));
        SpeedWithBearing speedWithBearingMainCurveAfter = new KnotSpeedWithBearingImpl(5.5, new DegreeBearingImpl(90));
        double directionChangeMainCurveInDegrees = 60;
        SpeedWithBearing lowestSpeed = new KnotSpeedWithBearingImpl(3, new DegreeBearingImpl(60.5));
        MillisecondsTimePoint lowestSpeedTimePoint = new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:28:29"));
        SpeedWithBearing highestSpeed = new KnotSpeedWithBearingImpl(6, new DegreeBearingImpl(65));
        MillisecondsTimePoint highestSpeedTimePoint = new MillisecondsTimePoint(
                dateFormat.parse("06/23/2011-15:28:32"));
        double maxTurningRateInDegreesPerSecond = 11.2043;
        double avgTurningRateInDegreesPerSecond = 9;
        Bearing courseAtMaxTurningRate = new DegreeBearingImpl(70.2244242);
        MillisecondsTimePoint maxTurningRateTimePoint = new MillisecondsTimePoint(
                dateFormat.parse("06/23/2011-15:28:33"));
        int gpsFixesCountMainCurve = 0;
        Duration longestIntervalBetweenTwoFixesMainCurve = Duration.NULL;
        ManeuverMainCurveWithEstimationData mainCurve = new ManeuverMainCurveWithEstimationDataImpl(
                timePointMainCurveBefore, timePointMainCurveAfter, speedWithBearingMainCurveBefore,
                speedWithBearingMainCurveAfter, directionChangeMainCurveInDegrees, lowestSpeed, lowestSpeedTimePoint,
                highestSpeed, highestSpeedTimePoint, maxTurningRateTimePoint, maxTurningRateInDegreesPerSecond,
                courseAtMaxTurningRate, avgTurningRateInDegreesPerSecond, gpsFixesCountMainCurve,
                longestIntervalBetweenTwoFixesMainCurve);

        MillisecondsTimePoint timePointBefore = new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:28:10"));
        MillisecondsTimePoint timePointAfter = new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:28:55"));
        SpeedWithBearing speedWithBearingBefore = new KnotSpeedWithBearingImpl(6, new DegreeBearingImpl(20));
        SpeedWithBearing speedWithBearingAfter = new KnotSpeedWithBearingImpl(7.5, new DegreeBearingImpl(101.1));
        double directionChangeInDegrees = 81.1;
        Speed maneuverLowestSpeed = new KnotSpeedImpl(2.9);
        Speed maneuverHighestSpeed = new KnotSpeedImpl(5.4);
        SpeedWithBearing avgSpeedWithBearingBefore = null;
        SpeedWithBearing avgSpeedWithBearingAfter = null;
        Duration durationFromPreviousManeuverEndToManeuverStart = null;
        Duration durationFromManeuverEndToNextManeuverStart = null;
        int gpsFixesCountFromPreviousManeuverEndToManeuverStart = 0;
        int gpsFixesCountFromManeuverEndToNextManeuverStart = 0;
        int gpsFixesCount = 133445;
        Duration longestIntervalBetweenTwoFixes = Duration.NULL;
        Duration intervalBetweenLastFixOfCurveAndNextFix = Duration.NULL;
        Duration intervalBetweenFirstFixOfCurveAndPreviousFix = Duration.NULL;
        ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataImpl curve = new ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataImpl(
                timePointBefore, timePointAfter, speedWithBearingBefore, speedWithBearingAfter,
                directionChangeInDegrees, maneuverLowestSpeed, maneuverHighestSpeed, avgSpeedWithBearingBefore,
                durationFromPreviousManeuverEndToManeuverStart, gpsFixesCountFromPreviousManeuverEndToManeuverStart,
                avgSpeedWithBearingAfter, durationFromManeuverEndToNextManeuverStart,
                gpsFixesCountFromManeuverEndToNextManeuverStart, gpsFixesCount, longestIntervalBetweenTwoFixes,
                intervalBetweenLastFixOfCurveAndNextFix, intervalBetweenFirstFixOfCurveAndPreviousFix);

        Wind wind = null;

        int jibingCount = 0;
        int tackingCount = 0;
        boolean maneuverStartsByRunningAwayFromWind = false;
        Bearing relativeBearingToNextMarkBeforeManeuver = null;
        Bearing relativeBearingToNextMarkAfterManeuver = null;
        boolean markPassing = false;
        DegreePosition maneuverPosition = new DegreePosition(50.325246, 11.148556);
        Distance closestDistanceToMark = null;
        Double targetTackAngle = null;
        Double targetJibeAngle = null;

        CompleteManeuverCurveWithEstimationData toSerialize = new CompleteManeuverCurveWithEstimationDataImpl(
                maneuverPosition, mainCurve, curve, wind, tackingCount, jibingCount,
                maneuverStartsByRunningAwayFromWind, relativeBearingToNextMarkBeforeManeuver,
                relativeBearingToNextMarkAfterManeuver, markPassing, closestDistanceToMark, targetTackAngle,
                targetJibeAngle);
        CompleteManeuverCurveWithEstimationDataJsonSerializer serializer = new CompleteManeuverCurveWithEstimationDataJsonSerializer(
                new ManeuverMainCurveWithEstimationDataJsonSerializer(),
                new ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer(),
                new ManeuverWindJsonSerializer(), new PositionJsonSerializer());
        JSONObject json = serializer.serialize(toSerialize);
        CompleteManeuverCurveWithEstimationDataJsonDeserializer deserializer = new CompleteManeuverCurveWithEstimationDataJsonDeserializer(
                new ManeuverMainCurveWithEstimationDataJsonDeserializer(),
                new ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonDeserializer(),
                new ManeuverWindJsonDeserializer(), new PositionJsonDeserializer());
        CompleteManeuverCurveWithEstimationData deserialized = deserializer.deserialize(json);

        assertEquals(targetJibeAngle, deserialized.getTargetJibeAngleInDegrees());
        assertEquals(targetTackAngle, deserialized.getTargetTackAngleInDegrees());
        assertEquals(closestDistanceToMark, deserialized.getDistanceToClosestMark());
        assertEquals(maneuverPosition, deserialized.getPosition());
        assertEquals(tackingCount, deserialized.getTackingCount());
        assertEquals(jibingCount, deserialized.getJibingCount());
        assertEquals(maneuverStartsByRunningAwayFromWind, deserialized.isManeuverStartsByRunningAwayFromWind());
        assertEquals(markPassing, deserialized.isMarkPassing());
        assertEquals(relativeBearingToNextMarkBeforeManeuver,
                deserialized.getRelativeBearingToNextMarkBeforeManeuver());
        assertEquals(relativeBearingToNextMarkAfterManeuver, deserialized.getRelativeBearingToNextMarkAfterManeuver());
        assertEquals(wind, deserialized.getWind());

        assertEquals(timePointBefore, deserialized.getCurveWithUnstableCourseAndSpeed().getTimePointBefore());
        assertEquals(timePointAfter, deserialized.getCurveWithUnstableCourseAndSpeed().getTimePointAfter());
        assertEquals(speedWithBearingBefore,
                deserialized.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingBefore());
        assertEquals(speedWithBearingAfter,
                deserialized.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter());
        assertEquals(directionChangeInDegrees,
                deserialized.getCurveWithUnstableCourseAndSpeed().getDirectionChangeInDegrees(), DELTA);
        assertEquals(maneuverLowestSpeed, deserialized.getCurveWithUnstableCourseAndSpeed().getLowestSpeed());
        assertEquals(avgSpeedWithBearingBefore,
                deserialized.getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingBefore());
        assertEquals(avgSpeedWithBearingAfter,
                deserialized.getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingAfter());
        assertEquals(durationFromPreviousManeuverEndToManeuverStart,
                deserialized.getCurveWithUnstableCourseAndSpeed().getDurationFromPreviousManeuverEndToManeuverStart());
        assertEquals(durationFromManeuverEndToNextManeuverStart,
                deserialized.getCurveWithUnstableCourseAndSpeed().getDurationFromManeuverEndToNextManeuverStart());
        assertEquals(gpsFixesCountFromPreviousManeuverEndToManeuverStart, deserialized
                .getCurveWithUnstableCourseAndSpeed().getGpsFixesCountFromPreviousManeuverEndToManeuverStart());
        assertEquals(gpsFixesCountFromManeuverEndToNextManeuverStart,
                deserialized.getCurveWithUnstableCourseAndSpeed().getGpsFixesCountFromManeuverEndToNextManeuverStart());
        assertEquals(gpsFixesCount, deserialized.getCurveWithUnstableCourseAndSpeed().getGpsFixesCount());
        assertEquals(longestIntervalBetweenTwoFixes,
                deserialized.getCurveWithUnstableCourseAndSpeed().getLongestIntervalBetweenTwoFixes());
        assertEquals(intervalBetweenLastFixOfCurveAndNextFix,
                deserialized.getCurveWithUnstableCourseAndSpeed().getIntervalBetweenLastFixOfCurveAndNextFix());
        assertEquals(intervalBetweenFirstFixOfCurveAndPreviousFix,
                deserialized.getCurveWithUnstableCourseAndSpeed().getIntervalBetweenFirstFixOfCurveAndPreviousFix());

        assertEquals(timePointMainCurveBefore, deserialized.getMainCurve().getTimePointBefore());
        assertEquals(timePointMainCurveAfter, deserialized.getMainCurve().getTimePointAfter());
        assertEquals(speedWithBearingMainCurveBefore, deserialized.getMainCurve().getSpeedWithBearingBefore());
        assertEquals(speedWithBearingMainCurveAfter, deserialized.getMainCurve().getSpeedWithBearingAfter());
        assertEquals(directionChangeMainCurveInDegrees, deserialized.getMainCurve().getDirectionChangeInDegrees(),
                DELTA);
        assertEquals(lowestSpeed, deserialized.getMainCurve().getLowestSpeed());
        assertEquals(lowestSpeedTimePoint, deserialized.getMainCurve().getLowestSpeedTimePoint());
        assertEquals(highestSpeed, deserialized.getMainCurve().getHighestSpeed());
        assertEquals(highestSpeedTimePoint, deserialized.getMainCurve().getHighestSpeedTimePoint());
        assertEquals(maxTurningRateInDegreesPerSecond,
                deserialized.getMainCurve().getMaxTurningRateInDegreesPerSecond(), DELTA);
        assertEquals(avgTurningRateInDegreesPerSecond,
                deserialized.getMainCurve().getAvgTurningRateInDegreesPerSecond(), DELTA);
        assertEquals(courseAtMaxTurningRate, deserialized.getMainCurve().getCourseAtMaxTurningRate());
        assertEquals(maxTurningRateTimePoint, deserialized.getMainCurve().getTimePointOfMaxTurningRate());
        assertEquals(gpsFixesCountMainCurve, deserialized.getMainCurve().getGpsFixesCount());
        assertEquals(longestIntervalBetweenTwoFixesMainCurve,
                deserialized.getMainCurve().getLongestIntervalBetweenTwoFixes());
    }

    @Test
    public void testDetailedBoatClass() throws JsonDeserializationException {
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true);
        String name = boatClass.getName();
        boolean typicallyStartsUpwind = boatClass.typicallyStartsUpwind();
        String displayName = boatClass.getName();
        Distance hullLength = boatClass.getHullLength();
        Distance hullBeam = boatClass.getHullBeam();
        BoatHullType hullType = boatClass.getHullType();
        DetailedBoatClassJsonSerializer serializer = new DetailedBoatClassJsonSerializer();
        JSONObject json = serializer.serialize(boatClass);
        BoatClassJsonDeserializer deserializer = new BoatClassJsonDeserializer(DomainFactory.INSTANCE);
        BoatClass deserialized = deserializer.deserialize(json);
        assertEquals(name, deserialized.getName());
        assertEquals(typicallyStartsUpwind, deserialized.typicallyStartsUpwind());
        assertEquals(displayName, deserialized.getName());
        assertEquals(hullLength, deserialized.getHullLength());
        assertEquals(hullBeam, deserialized.getHullBeam());
        assertEquals(hullType, deserialized.getHullType());
        assertEquals(boatClass, deserialized);
    }

}
