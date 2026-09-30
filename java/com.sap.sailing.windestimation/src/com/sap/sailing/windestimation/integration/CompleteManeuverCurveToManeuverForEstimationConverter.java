package com.sap.sailing.windestimation.integration;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.maneuverdetection.TrackTimeInfo;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.transformer.ManeuverForEstimationTransformer;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;

/**
 * Converts {@link CompleteManeuverCurve} instances to {@link ManeuverForEstimation} instances.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class CompleteManeuverCurveToManeuverForEstimationConverter {

    private static final Duration MIN_DURATION_TO_OTHER_MANEUVER = new MillisecondsDurationImpl(
            ManeuverForEstimationTransformer.MIN_SECONDS_TO_OTHER_MANEUVER * 1000L);

    private final TrackedRace trackedRace;
    private final ManeuverForEstimationTransformer maneuverForEstimationTransformer = new ManeuverForEstimationTransformer();
    private final PolarDataService polarService;

    public CompleteManeuverCurveToManeuverForEstimationConverter(TrackedRace trackedRace,
            PolarDataService polarService) {
        this.trackedRace = trackedRace;
        this.polarService = polarService;
    }

    public ManeuverForEstimation convertCleanManeuverSpotToManeuverForEstimation(CompleteManeuverCurve maneuver,
            CompleteManeuverCurve previousManeuver, CompleteManeuverCurve nextManeuver, Competitor competitor,
            TrackTimeInfo trackTimeInfo) {
        if (!maneuverForEstimationTransformer.isManeuverEligibleForAnalysis(
                maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getDirectionChangeInDegrees(),
                maneuver.getMainCurveBoundaries().getDirectionChangeInDegrees())) {
            // skip further computation in order to improve performance
            return null;
        }
        BoatClass boatClass = trackedRace.getBoatOfCompetitor(competitor).getBoatClass();
        ManeuverCurveBoundaries maneuverCurveBoundaries = maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries();
        final GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
        Position maneuverPosition = track.getEstimatedPosition(maneuver.getTimePoint(), false);
        Double courseChangeInDegreesWithinTurningSectionOfPreviousManeuver = previousManeuver == null ? null
                : previousManeuver.getMainCurveBoundaries().getDirectionChangeInDegrees();
        Double courseChangeInDegreesWithinTurningSectionOfNextManeuver = nextManeuver == null ? null
                : nextManeuver.getMainCurveBoundaries().getDirectionChangeInDegrees();
        Double targetTackAngleInDegrees = getTargetTackAngleInDegrees(maneuverCurveBoundaries, boatClass);
        Double targetJibeAngleInDegrees = getTargetJibeAngleInDegrees(maneuverCurveBoundaries, boatClass);
        boolean markPassingDataAvailable = maneuver.isMarkPassing()
                || hasNextWaypoint(competitor, maneuverCurveBoundaries.getTimePointAfter());
        Duration durationFromPreviousManeuverEndToManeuverStart = previousManeuver == null
                ? trackTimeInfo.getTrackStartTimePoint().until(maneuverCurveBoundaries.getTimePointBefore())
                : previousManeuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter()
                        .until(maneuverCurveBoundaries.getTimePointBefore());
        // enforce last temporary maneuver to be clean in terms interval to next maneuver
        Duration durationFromManeuverEndToNextManeuverStart = nextManeuver == null ? MIN_DURATION_TO_OTHER_MANEUVER
                : maneuverCurveBoundaries.getTimePointAfter()
                        .until(nextManeuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore());
        Duration longestIntervalBetweenTwoFixes = null;
        Duration intervalBetweenFirstFixOfCurveAndPreviousFix = Duration.ONE_YEAR;
        GPSFixMoving lastFixBeforeManeuver = track.getLastFixBefore(maneuverCurveBoundaries.getTimePointBefore());
        if (lastFixBeforeManeuver != null) {
            intervalBetweenFirstFixOfCurveAndPreviousFix = lastFixBeforeManeuver.getTimePoint()
                    .until(maneuverCurveBoundaries.getTimePointBefore());
        }
        Duration intervalBetweenLastFixOfCurveAndNextFix = Duration.ONE_YEAR;
        GPSFixMoving firstFixAfterManeuver = track.getFirstFixAfter(maneuverCurveBoundaries.getTimePointAfter());
        if (firstFixAfterManeuver != null) {
            intervalBetweenLastFixOfCurveAndNextFix = maneuverCurveBoundaries.getTimePointAfter()
                    .until(firstFixAfterManeuver.getTimePoint());
        }
        GPSFixMoving previousFix = null;
        long longestIntervalBetweenTwoFixesInMillis = 0;
        track.lockForRead();
        try {
            for (GPSFixMoving fix : track.getFixes(maneuverCurveBoundaries.getTimePointBefore(), true,
                    maneuverCurveBoundaries.getTimePointAfter(), true)) {
                if (previousFix != null) {
                    long intervalBetweenPreviousAndCurrentFixInMillis = previousFix.getTimePoint()
                            .until(fix.getTimePoint()).asMillis();
                    if (longestIntervalBetweenTwoFixesInMillis < intervalBetweenPreviousAndCurrentFixInMillis) {
                        longestIntervalBetweenTwoFixesInMillis = intervalBetweenPreviousAndCurrentFixInMillis;
                    }
                }
                previousFix = fix;
            }
        } finally {
            track.unlockAfterRead();
        }
        longestIntervalBetweenTwoFixes = new MillisecondsDurationImpl(longestIntervalBetweenTwoFixesInMillis);
        ConvertableManeuverForEstimationAdapterForCompleteManeuverCurve convertableManeuver = new ConvertableManeuverForEstimationAdapterForCompleteManeuverCurve(
                maneuver, maneuverPosition, markPassingDataAvailable, longestIntervalBetweenTwoFixes,
                courseChangeInDegreesWithinTurningSectionOfPreviousManeuver,
                courseChangeInDegreesWithinTurningSectionOfNextManeuver, intervalBetweenFirstFixOfCurveAndPreviousFix,
                intervalBetweenLastFixOfCurveAndNextFix, durationFromPreviousManeuverEndToManeuverStart,
                durationFromManeuverEndToNextManeuverStart, targetTackAngleInDegrees, targetJibeAngleInDegrees);
        // TODO compute scaledSpeedDivisor, recompute maneuverForEstimation, reclassify all maneuver instances if
        // scaledSpeedDivisor has significantly changed?
        ManeuverForEstimation maneuverForEstimation = maneuverForEstimationTransformer
                .getManeuverForEstimation(convertableManeuver, 1.0, boatClass, competitor.getName());
        return maneuverForEstimation;
    }

    private Double getTargetTackAngleInDegrees(ManeuverCurveBoundaries curveWithUnstableCourseAndSpeed,
            BoatClass boatClass) {
        Double targetTackAngle = null;
        Speed boatSpeed = curveWithUnstableCourseAndSpeed.getSpeedWithBearingBefore()
                .compareTo(curveWithUnstableCourseAndSpeed.getSpeedWithBearingAfter()) < 0
                        ? curveWithUnstableCourseAndSpeed.getSpeedWithBearingBefore()
                        : curveWithUnstableCourseAndSpeed.getSpeedWithBearingAfter();
        if (polarService != null && polarService.getAllBoatClassesWithPolarSheetsAvailable().contains(boatClass)) {
            SpeedWithBearingWithConfidence<Void> closestTackTwa = polarService.getClosestTwaTws(ManeuverType.TACK,
                    boatSpeed, curveWithUnstableCourseAndSpeed.getDirectionChangeInDegrees(), boatClass);
            if (closestTackTwa != null) {
                targetTackAngle = polarService.getManeuverAngleInDegreesFromTwa(ManeuverType.TACK,
                        closestTackTwa.getObject().getBearing());
            }
        }
        return targetTackAngle;
    }

    private boolean hasNextWaypoint(Competitor competitor, TimePoint timePoint) {
        TrackedLegOfCompetitor legAfter = trackedRace.getTrackedLeg(competitor, timePoint);
        if (legAfter != null && legAfter.getLeg().getTo() != null) {
            return true;
        }
        return false;
    }

    private Double getTargetJibeAngleInDegrees(ManeuverCurveBoundaries curveWithUnstableCourseAndSpeed,
            BoatClass boatClass) {
        Double targetJibeAngle = null;
        Speed boatSpeed = curveWithUnstableCourseAndSpeed.getSpeedWithBearingBefore()
                .compareTo(curveWithUnstableCourseAndSpeed.getSpeedWithBearingAfter()) < 0
                        ? curveWithUnstableCourseAndSpeed.getSpeedWithBearingBefore()
                        : curveWithUnstableCourseAndSpeed.getSpeedWithBearingAfter();
        if (polarService != null && polarService.getAllBoatClassesWithPolarSheetsAvailable().contains(boatClass)) {
            SpeedWithBearingWithConfidence<Void> closestJibeTwa = polarService.getClosestTwaTws(ManeuverType.JIBE,
                    boatSpeed, curveWithUnstableCourseAndSpeed.getDirectionChangeInDegrees(), boatClass);
            if (closestJibeTwa != null) {
                targetJibeAngle = polarService.getManeuverAngleInDegreesFromTwa(ManeuverType.JIBE,
                        closestJibeTwa.getObject().getBearing());
            }
        }
        return targetJibeAngle;
    }

}
