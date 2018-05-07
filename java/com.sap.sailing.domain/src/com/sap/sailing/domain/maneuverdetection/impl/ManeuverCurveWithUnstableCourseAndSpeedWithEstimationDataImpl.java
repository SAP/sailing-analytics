package com.sap.sailing.domain.maneuverdetection.impl;

import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.maneuverdetection.ManeuverCurveWithUnstableCourseAndSpeedWithEstimationData;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;

public class ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataImpl
        extends ManeuverCurveBoundariesWithDetailedManeuverLossImpl
        implements ManeuverCurveWithUnstableCourseAndSpeedWithEstimationData {

    private final SpeedWithBearing averageSpeedWithBearingBefore;
    private final Duration durationFromPreviousManeuverEndToManeuverStart;
    private final SpeedWithBearing averageSpeedWithBearingAfter;
    private final Duration durationFromManeuverEndToNextManeuverStart;
    private final int gpsFixesCount;
    private final int gpsFixesCountFromPreviousManeuverEndToManeuverStart;
    private final int gpsFixesCountFromManeuverEndToNextManeuverStart;
    private final Duration longestIntervalBetweenTwoFixes;
    private final Duration intervalBetweenLastFixOfCurveAndNextFix;
    private final Duration intervalBetweenFirstFixOfCurveAndPreviousFix;

    public ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataImpl(TimePoint timePointBefore,
            TimePoint timePointAfter, SpeedWithBearing speedWithBearingBefore, SpeedWithBearing speedWithBearingAfter,
            double directionChangeInDegrees, Speed lowestSpeed, SpeedWithBearing averageSpeedWithBearingBefore,
            Duration durationFromPreviousManeuverEndToManeuverStart,
            int gpsFixesCountFromPreviousManeuverEndToManeuverStart, SpeedWithBearing averageSpeedWithBearingAfter,
            Duration durationFromManeuverEndToNextManeuverStart, int gpsFixesCountFromManeuverEndToNextManeuverStart,
            Distance distanceSailedWithinManeuver, Distance distanceSailedWithinManeuverTowardMiddleAngleProjection,
            Distance distanceSailedIfNotManeuvering, Distance distanceSailedTowardMiddleAngleProjectionIfNotManeuvering,
            int gpsFixesCount, Duration longestIntervalBetweenTwoFixes,
            Duration intervalBetweenLastFixOfCurveAndNextFix, Duration intervalBetweenFirstFixOfCurveAndPreviousFix) {
        super(timePointBefore, timePointAfter, speedWithBearingBefore, speedWithBearingAfter, directionChangeInDegrees,
                lowestSpeed, distanceSailedWithinManeuver, distanceSailedWithinManeuverTowardMiddleAngleProjection,
                distanceSailedIfNotManeuvering, distanceSailedTowardMiddleAngleProjectionIfNotManeuvering);
        this.averageSpeedWithBearingBefore = averageSpeedWithBearingBefore;
        this.durationFromPreviousManeuverEndToManeuverStart = durationFromPreviousManeuverEndToManeuverStart;
        this.gpsFixesCountFromPreviousManeuverEndToManeuverStart = gpsFixesCountFromPreviousManeuverEndToManeuverStart;
        this.averageSpeedWithBearingAfter = averageSpeedWithBearingAfter;
        this.durationFromManeuverEndToNextManeuverStart = durationFromManeuverEndToNextManeuverStart;
        this.gpsFixesCountFromManeuverEndToNextManeuverStart = gpsFixesCountFromManeuverEndToNextManeuverStart;
        this.gpsFixesCount = gpsFixesCount;
        this.longestIntervalBetweenTwoFixes = longestIntervalBetweenTwoFixes;
        this.intervalBetweenLastFixOfCurveAndNextFix = intervalBetweenLastFixOfCurveAndNextFix;
        this.intervalBetweenFirstFixOfCurveAndPreviousFix = intervalBetweenFirstFixOfCurveAndPreviousFix;
    }

    @Override
    public SpeedWithBearing getAverageSpeedWithBearingBefore() {
        return averageSpeedWithBearingBefore;
    }

    @Override
    public Duration getDurationFromPreviousManeuverEndToManeuverStart() {
        return durationFromPreviousManeuverEndToManeuverStart;
    }

    @Override
    public SpeedWithBearing getAverageSpeedWithBearingAfter() {
        return averageSpeedWithBearingAfter;
    }

    @Override
    public Duration getDurationFromManeuverEndToNextManeuverStart() {
        return durationFromManeuverEndToNextManeuverStart;
    }

    @Override
    public int getGpsFixesCount() {
        return gpsFixesCount;
    }

    @Override
    public int getGpsFixesCountFromPreviousManeuverEndToManeuverStart() {
        return gpsFixesCountFromPreviousManeuverEndToManeuverStart;
    }

    @Override
    public int getGpsFixesCountFromManeuverEndToNextManeuverStart() {
        return gpsFixesCountFromManeuverEndToNextManeuverStart;
    }

    @Override
    public Duration getLongestIntervalBetweenTwoFixes() {
        return longestIntervalBetweenTwoFixes;
    }

    @Override
    public Duration getIntervalBetweenLastFixOfCurveAndNextFix() {
        return intervalBetweenLastFixOfCurveAndNextFix;
    }

    @Override
    public Duration getIntervalBetweenFirstFixOfCurveAndPreviousFix() {
        return intervalBetweenFirstFixOfCurveAndPreviousFix;
    }

}
