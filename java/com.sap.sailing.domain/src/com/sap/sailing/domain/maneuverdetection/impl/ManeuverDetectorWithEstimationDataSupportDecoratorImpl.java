package com.sap.sailing.domain.maneuverdetection.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.ManeuverCurveWithUnstableCourseAndSpeedWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.ManeuverDetector;
import com.sap.sailing.domain.maneuverdetection.ManeuverDetectorWithEstimationDataSupport;
import com.sap.sailing.domain.maneuverdetection.ManeuverMainCurveWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.TrackTimeInfo;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.SpeedWithBearingStep;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.impl.CompleteManeuverCurveImpl;
import com.sap.sailing.domain.tracking.impl.ManeuverCurveBoundariesImpl;
import com.sap.sailing.domain.tracking.impl.NonCachingMarkPositionAtTimePointCache;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

/**
 * A decorator which adds support for management of estimation data for wind estimation to an existing maneuver detector
 * implementation.
 * 
 * @author Vladislav Chumak (D069712)
 * @see ManeuverDetector
 *
 */
public class ManeuverDetectorWithEstimationDataSupportDecoratorImpl
        implements ManeuverDetectorWithEstimationDataSupport {

    private final ManeuverDetectorImpl maneuverDetector;
    private final PolarDataService polarDataService;

    public ManeuverDetectorWithEstimationDataSupportDecoratorImpl(ManeuverDetectorImpl maneuverDetector,
            PolarDataService polarDataService) {
        this.maneuverDetector = maneuverDetector;
        this.polarDataService = polarDataService;
    }

    @Override
    public List<Maneuver> detectManeuvers() {
        return maneuverDetector.detectManeuvers();
    }

    @Override
    public TrackTimeInfo getTrackTimeInfo() {
        return maneuverDetector.getTrackTimeInfo();
    }

    @Override
    public List<Maneuver> detectManeuvers(Iterable<CompleteManeuverCurve> maneuverCurves) {
        List<Maneuver> maneuvers = new ArrayList<>();
        for (CompleteManeuverCurve maneuverCurve : maneuverCurves) {
            TimePoint maneuverTimePoint = maneuverCurve.getMainCurveBoundaries().getTimePoint();
            Position maneuverPosition = maneuverDetector.track.getEstimatedPosition(maneuverTimePoint,
                    /* extrapolate */false);
            Wind wind = maneuverDetector.trackedRace.getWind(maneuverPosition, maneuverTimePoint);
            maneuvers
                    .addAll(maneuverDetector.determineManeuversFromManeuverCurve(maneuverCurve.getMainCurveBoundaries(),
                            maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries(), wind,
                            maneuverCurve.getMarkPassing()));
        }
        return maneuvers;
    }

    @Override
    public List<CompleteManeuverCurve> detectCompleteManeuverCurves() {
        List<? extends ManeuverSpot> maneuverSpots = maneuverDetector.detectManeuverSpots();
        return maneuverSpots.stream().filter(maneuverSpot -> maneuverSpot.getManeuverCurve() != null)
                .map(maneuverSpot -> maneuverSpot.getManeuverCurve()).collect(Collectors.toList());
    }

    @Override
    public List<CompleteManeuverCurve> getCompleteManeuverCurves(Iterable<Maneuver> maneuvers) {
        List<CompleteManeuverCurve> result = new ArrayList<>();
        CompleteManeuverCurve curveToAdd = null;
        boolean previousManeuverCouldBelongToSameCurve = false;
        Maneuver previousManeuver = null;
        for (Maneuver maneuver : maneuvers) {
            boolean maneuverCouldBelongToSameCurve = maneuver.getType() == ManeuverType.PENALTY_CIRCLE
                    || maneuver.isMarkPassing()
                            && (maneuver.getType() == ManeuverType.TACK || maneuver.getType() == ManeuverType.JIBE);
            if (previousManeuverCouldBelongToSameCurve && maneuverCouldBelongToSameCurve
                    && previousManeuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter()
                            .equals(maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore())
                    && previousManeuver.getToSide() == maneuver.getToSide()) {
                curveToAdd = extendCompleteManeuverCurveWithManeuver(curveToAdd, maneuver);
            } else {
                if (curveToAdd != null) {
                    result.add(curveToAdd);
                }
                curveToAdd = convertManeuverToCompleteManeuverCurve(maneuver);
            }
            previousManeuver = maneuver;
            previousManeuverCouldBelongToSameCurve = maneuverCouldBelongToSameCurve;
        }
        if (curveToAdd != null) {
            result.add(curveToAdd);
        }
        return result;
    }

    /**
     * Converts the provided maneuver into {@link CompleteManeuverCurve}. The boundaries of provided maneuver are reused
     * for the resulting complete maneuver curve.
     * 
     * @see CompleteManeuverCurve
     * @see Maneuver
     */
    private CompleteManeuverCurve convertManeuverToCompleteManeuverCurve(Maneuver maneuver) {
        ManeuverMainCurveDetailsWithBearingSteps mainCurveBoundaries = new ManeuverMainCurveDetailsWithBearingSteps(
                maneuver.getMainCurveBoundaries().getTimePointBefore(),
                maneuver.getMainCurveBoundaries().getTimePointAfter(), maneuver.getTimePoint(),
                maneuver.getMainCurveBoundaries().getSpeedWithBearingBefore(),
                maneuver.getMainCurveBoundaries().getSpeedWithBearingAfter(),
                maneuver.getMainCurveBoundaries().getDirectionChangeInDegrees(),
                maneuver.getMaxTurningRateInDegreesPerSecond(), maneuver.getMainCurveBoundaries().getLowestSpeed(),
                maneuver.getMainCurveBoundaries().getHighestSpeed(),
                maneuverDetector.getSpeedWithBearingSteps(maneuver.getMainCurveBoundaries().getTimePointBefore(),
                        maneuver.getMainCurveBoundaries().getTimePointAfter()));
        return new CompleteManeuverCurveImpl(mainCurveBoundaries,
                maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries(), maneuver.getMarkPassing());
    }

    /**
     * Extends the end of provided maneuver curve with the end of provided maneuver. For this, the curve boundaries with
     * unstable course and speed are merged by appending, whereas the maneuver main curve gets recalculated completely
     * from scratch. The additional attributes such as, direction change and lowest speed get adjusted accordingly.
     */
    private CompleteManeuverCurve extendCompleteManeuverCurveWithManeuver(CompleteManeuverCurve maneuverCurve,
            Maneuver maneuver) {
        ManeuverMainCurveDetailsWithBearingSteps mainCurveDetails = maneuverDetector.computeManeuverMainCurveDetails(
                maneuverCurve.getMainCurveBoundaries().getTimePointBefore(),
                maneuver.getMainCurveBoundaries().getTimePointAfter(), maneuver.getToSide());
        if (mainCurveDetails == null) {
            return maneuverCurve;
        }
        Speed lowestSpeed = maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getLowestSpeed()
                .compareTo(maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getLowestSpeed()) > 0
                        ? maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getLowestSpeed()
                        : maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getLowestSpeed();
        Speed highestSpeed = maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getHighestSpeed()
                .compareTo(maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getHighestSpeed()) < 0
                        ? maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getHighestSpeed()
                        : maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getHighestSpeed();
        ManeuverCurveBoundaries maneuverCurveWithStableSpeedAndCourseBoundaries = new ManeuverCurveBoundariesImpl(
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore(),
                maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingBefore(),
                maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingAfter(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getDirectionChangeInDegrees()
                        + maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getDirectionChangeInDegrees(),
                lowestSpeed, highestSpeed);
        return new CompleteManeuverCurveImpl(mainCurveDetails, maneuverCurveWithStableSpeedAndCourseBoundaries,
                maneuverCurve.getMarkPassing() == null ? maneuver.getMarkPassing() : maneuverCurve.getMarkPassing());
    }

    @Override
    public List<CompleteManeuverCurveWithEstimationData> getCompleteManeuverCurvesWithEstimationData(
            Iterable<CompleteManeuverCurve> maneuverCurves) {
        List<CompleteManeuverCurveWithEstimationData> result = new ArrayList<>();

        CompleteManeuverCurve previousManeuverCurve = null;
        CompleteManeuverCurve currentManeuverCurve = null;
        for (CompleteManeuverCurve nextManeuverCurve : maneuverCurves) {
            if (currentManeuverCurve != null) {
                CompleteManeuverCurveWithEstimationData maneuverCurveWithEstimationData = calculateCompleteManeuverCurveWithEstimationData(
                        currentManeuverCurve, previousManeuverCurve, nextManeuverCurve);
                result.add(maneuverCurveWithEstimationData);
            }
            previousManeuverCurve = currentManeuverCurve;
            currentManeuverCurve = nextManeuverCurve;
        }
        if (currentManeuverCurve != null) {
            CompleteManeuverCurveWithEstimationData maneuverCurveWithEstimationData = calculateCompleteManeuverCurveWithEstimationData(
                    currentManeuverCurve, previousManeuverCurve, null);
            result.add(maneuverCurveWithEstimationData);
        }
        return result;
    }

    /**
     * Calculates a {@link CompleteManeuverCurveWithEstimationData}-instance for the provided {@code maneuverCurve}. The
     * computation of additional information required by {@link CompleteManeuverCurveWithEstimationData} is regarded as
     * computationally-intensive.
     */
    private CompleteManeuverCurveWithEstimationData calculateCompleteManeuverCurveWithEstimationData(
            CompleteManeuverCurve maneuverCurve, CompleteManeuverCurve previousManeuverCurve,
            CompleteManeuverCurve nextManeuverCurve) {
        Bearing courseAtMaxTurningRate = null;
        SpeedWithBearingStep stepWithLowestSpeed = null;
        SpeedWithBearingStep stepWithHighestSpeed = null;
        SpeedWithBearingStep stepWithMaxTurningRate = null;
        SpeedWithBearingStep previousStep = null;
        for (SpeedWithBearingStep step : maneuverCurve.getMainCurveBoundaries().getSpeedWithBearingSteps()) {
            if (stepWithLowestSpeed == null
                    || stepWithLowestSpeed.getSpeedWithBearing().compareTo(step.getSpeedWithBearing()) > 0) {
                stepWithLowestSpeed = step;
            }
            if (stepWithHighestSpeed == null
                    || stepWithHighestSpeed.getSpeedWithBearing().compareTo(step.getSpeedWithBearing()) < 0) {
                stepWithHighestSpeed = step;
            }
            if (previousStep != null && (stepWithMaxTurningRate == null || stepWithMaxTurningRate
                    .getTurningRateInDegreesPerSecond() < step.getTurningRateInDegreesPerSecond())) {
                stepWithMaxTurningRate = step;
                courseAtMaxTurningRate = previousStep.getSpeedWithBearing().getBearing()
                        .add(new DegreeBearingImpl(step.getCourseChangeInDegrees() / 2));
            }
            previousStep = step;
        }
        int gpsFixCountWithinMainCurve = 0;
        int gpsFixCountWithinWholeCurve = 0;
        int gpsFixesCountFromPreviousManeuver = 0;
        int gpsFixesCountToNextManeuver = 0;
        try {
            maneuverDetector.track.lockForRead();
            boolean considerPreviousManeuver = previousManeuverCurve != null && previousManeuverCurve
                    .getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter()
                    .before(maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore());
            boolean considerNextManeuver = nextManeuverCurve != null && nextManeuverCurve
                    .getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore()
                    .after(maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter());
            for (GPSFixMoving fix : maneuverDetector.track.getFixes(
                    considerPreviousManeuver
                            ? previousManeuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries()
                                    .getTimePointAfter()
                            : maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore(),
                    !considerPreviousManeuver,
                    considerNextManeuver
                            ? nextManeuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries()
                                    .getTimePointBefore()
                            : maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter(),
                    !considerNextManeuver)) {
                if (fix.getTimePoint().before(
                        maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore())) {
                    ++gpsFixesCountFromPreviousManeuver;
                } else if (fix.getTimePoint().after(
                        maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter())) {
                    ++gpsFixesCountToNextManeuver;
                } else {
                    if (!fix.getTimePoint().before(maneuverCurve.getMainCurveBoundaries().getTimePointBefore())
                            && !fix.getTimePoint().after(maneuverCurve.getMainCurveBoundaries().getTimePointAfter())) {
                        ++gpsFixCountWithinMainCurve;
                    }
                    ++gpsFixCountWithinWholeCurve;
                }
            }
        } finally {
            maneuverDetector.track.unlockAfterRead();
        }
        if (courseAtMaxTurningRate == null) {
            courseAtMaxTurningRate = stepWithLowestSpeed.getSpeedWithBearing().getBearing();
        }
        Duration longestGpsFixIntervalBetweenTwoFixes = maneuverDetector.track.getLongestIntervalBetweenTwoFixes(
                maneuverCurve.getMainCurveBoundaries().getTimePointBefore(),
                maneuverCurve.getMainCurveBoundaries().getTimePointAfter());
        ManeuverMainCurveWithEstimationData mainCurve = new ManeuverMainCurveWithEstimationDataImpl(
                maneuverCurve.getMainCurveBoundaries().getTimePointBefore(),
                maneuverCurve.getMainCurveBoundaries().getTimePointAfter(),
                maneuverCurve.getMainCurveBoundaries().getSpeedWithBearingBefore(),
                maneuverCurve.getMainCurveBoundaries().getSpeedWithBearingAfter(),
                maneuverCurve.getMainCurveBoundaries().getDirectionChangeInDegrees(),
                stepWithLowestSpeed.getSpeedWithBearing(), stepWithLowestSpeed.getTimePoint(),
                stepWithHighestSpeed.getSpeedWithBearing(), stepWithHighestSpeed.getTimePoint(),
                maneuverCurve.getMainCurveBoundaries().getTimePoint(),
                maneuverCurve.getMainCurveBoundaries().getMaxTurningRateInDegreesPerSecond(), courseAtMaxTurningRate,
                Math.abs(maneuverCurve.getMainCurveBoundaries().getDirectionChangeInDegrees())
                        / maneuverCurve.getMainCurveBoundaries().getDuration().asSeconds(),
                gpsFixCountWithinMainCurve, longestGpsFixIntervalBetweenTwoFixes);
        longestGpsFixIntervalBetweenTwoFixes = maneuverDetector.track.getLongestIntervalBetweenTwoFixes(
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter());
        TrackTimeInfo trackTimeInfo = previousManeuverCurve == null || nextManeuverCurve == null
                ? maneuverDetector.getTrackTimeInfo()
                : null;
        Pair<Duration, SpeedWithBearing> durationAndAvgSpeedWithBearingBefore = calculateDurationAndAvgSpeedWithBearingBetweenTimePoints(
                previousManeuverCurve == null ? trackTimeInfo.getTrackStartTimePoint()
                        : previousManeuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries()
                                .getTimePointAfter(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore());
        Pair<Duration, SpeedWithBearing> durationAndAvgSpeedWithBearingAfter = calculateDurationAndAvgSpeedWithBearingBetweenTimePoints(
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter(),
                nextManeuverCurve == null ? trackTimeInfo.getLatestRawFixTimePoint()
                        : nextManeuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore());
        Duration intervalBetweenLastFixOfCurveAndNextFix = Duration.NULL;
        GPSFixMoving lastManeuverFix = maneuverDetector.track.getLastFixAtOrBefore(
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter());
        if (lastManeuverFix != null) {
            GPSFixMoving firstFixAfterLastManeuverFix = maneuverDetector.track
                    .getFirstFixAfter(lastManeuverFix.getTimePoint());
            if (firstFixAfterLastManeuverFix != null) {
                intervalBetweenLastFixOfCurveAndNextFix = lastManeuverFix.getTimePoint()
                        .until(firstFixAfterLastManeuverFix.getTimePoint());
            }
        }
        Duration intervalBetweenFirstFixOfCurveAndPreviousFix = Duration.NULL;
        GPSFixMoving firstManeuverFix = maneuverDetector.track.getFirstFixAtOrAfter(
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore());
        if (firstManeuverFix != null) {
            GPSFixMoving lastFixBeforeFirstManeuverFix = maneuverDetector.track
                    .getLastFixBefore(firstManeuverFix.getTimePoint());
            if (lastFixBeforeFirstManeuverFix != null) {
                intervalBetweenFirstFixOfCurveAndPreviousFix = lastFixBeforeFirstManeuverFix.getTimePoint()
                        .until(firstManeuverFix.getTimePoint());
            }
        }
        ManeuverCurveWithUnstableCourseAndSpeedWithEstimationData curveWithUnstableCourseAndSpeed = new ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataImpl(
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingBefore(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingAfter(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getDirectionChangeInDegrees(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getLowestSpeed(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getHighestSpeed(),
                durationAndAvgSpeedWithBearingBefore.getB(), durationAndAvgSpeedWithBearingBefore.getA(),
                gpsFixesCountFromPreviousManeuver, durationAndAvgSpeedWithBearingAfter.getB(),
                durationAndAvgSpeedWithBearingAfter.getA(), gpsFixesCountToNextManeuver, gpsFixCountWithinWholeCurve,
                longestGpsFixIntervalBetweenTwoFixes, intervalBetweenLastFixOfCurveAndNextFix,
                intervalBetweenFirstFixOfCurveAndPreviousFix);
        TimePoint maneuverTimePoint = maneuverCurve.getMainCurveBoundaries().getTimePoint();
        Position maneuverPosition = maneuverDetector.track.getEstimatedPosition(maneuverTimePoint,
                /* extrapolate */false);
        Wind wind = maneuverDetector.trackedRace.getWind(maneuverPosition, maneuverTimePoint);
        int numberOfJibes = maneuverDetector.getNumberOfJibes(mainCurve, wind);
        int numberOfTacks = maneuverDetector.getNumberOfTacks(mainCurve, wind);
        boolean maneuverStartsByRunningAwayFromWind = (mainCurve.getSpeedWithBearingBefore().getBearing().getDegrees()
                - 180) * mainCurve.getDirectionChangeInDegrees() < 0;
        Bearing relativeBearingToNextMarkPassingBeforeManeuver = getRelativeBearingToNextMark(
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore(), maneuverCurve
                        .getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingBefore().getBearing());
        Bearing relativeBearingToNextMarkPassingAfterManeuver = getRelativeBearingToNextMark(
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter(), maneuverCurve
                        .getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingAfter().getBearing());
        BoatClass boatClass = maneuverDetector.trackedRace.getRace().getBoatOfCompetitor(maneuverDetector.competitor)
                .getBoatClass();
        Double targetTackAngle = null;
        Double targetJibeAngle = null;
        Speed boatSpeed = curveWithUnstableCourseAndSpeed.getSpeedWithBearingBefore()
                .compareTo(curveWithUnstableCourseAndSpeed.getSpeedWithBearingAfter()) < 0
                        ? curveWithUnstableCourseAndSpeed.getSpeedWithBearingBefore()
                        : curveWithUnstableCourseAndSpeed.getSpeedWithBearingAfter();
        if (polarDataService != null
                && polarDataService.getAllBoatClassesWithPolarSheetsAvailable().contains(boatClass)) {
            SpeedWithBearingWithConfidence<Void> closestTackTwa = polarDataService.getClosestTwaTws(ManeuverType.TACK,
                    boatSpeed, curveWithUnstableCourseAndSpeed.getDirectionChangeInDegrees(), boatClass);
            SpeedWithBearingWithConfidence<Void> closestJibeTwa = polarDataService.getClosestTwaTws(ManeuverType.JIBE,
                    boatSpeed, curveWithUnstableCourseAndSpeed.getDirectionChangeInDegrees(), boatClass);
            if (closestTackTwa != null) {
                targetTackAngle = polarDataService.getManeuverAngleInDegreesFromTwa(ManeuverType.TACK,
                        closestTackTwa.getObject().getBearing());
            }
            if (closestJibeTwa != null) {
                targetJibeAngle = polarDataService.getManeuverAngleInDegreesFromTwa(ManeuverType.JIBE,
                        closestJibeTwa.getObject().getBearing());
            }
        }
        Distance closestDistanceToMark = getClosestDistanceToMark(mainCurve.getTimePointOfMaxTurningRate());

        return new CompleteManeuverCurveWithEstimationDataImpl(maneuverPosition, mainCurve,
                curveWithUnstableCourseAndSpeed, wind, numberOfTacks, numberOfJibes,
                maneuverStartsByRunningAwayFromWind, relativeBearingToNextMarkPassingBeforeManeuver,
                relativeBearingToNextMarkPassingAfterManeuver, maneuverCurve.isMarkPassing(), closestDistanceToMark,
                targetTackAngle, targetJibeAngle);
    }

    public Distance getClosestDistanceToMark(TimePoint timePoint) {
        NonCachingMarkPositionAtTimePointCache markPositionAtTimePointCache = new NonCachingMarkPositionAtTimePointCache(
                maneuverDetector.trackedRace, timePoint);
        Distance result = null;
        TrackedLegOfCompetitor legAfter = maneuverDetector.trackedRace.getTrackedLeg(maneuverDetector.competitor,
                timePoint);
        if (legAfter != null) {
            Position maneuverPosition = maneuverDetector.track.getEstimatedPosition(timePoint, false);
            if (legAfter.getLeg().getTo() != null) {
                result = getClosestDistanceToMarkInternal(markPositionAtTimePointCache, legAfter.getLeg().getTo(),
                        maneuverPosition);
            }
            if (legAfter.getLeg().getFrom() != null) {
                Distance distance = getClosestDistanceToMarkInternal(markPositionAtTimePointCache,
                        legAfter.getLeg().getFrom(), maneuverPosition);
                if (result == null || distance != null && distance.compareTo(result) < 0) {
                    result = distance;
                }
            }
        }
        return result;
    }

    private Distance getClosestDistanceToMarkInternal(
            NonCachingMarkPositionAtTimePointCache markPositionAtTimePointCache, Waypoint waypoint,
            Position maneuverPosition) {
        Distance result = null;
        for (Mark mark : waypoint.getMarks()) {
            Position markPosition = markPositionAtTimePointCache.getEstimatedPosition(mark);
            if (markPosition != null) {
                Distance distance = markPosition.getDistance(maneuverPosition);
                if (result == null || distance.compareTo(result) < 0) {
                    result = distance;
                }
            }
        }
        return result;
    }

    /**
     * Calculates the duration and avg speed with avg course based on the competitor's track within the provided time
     * range.
     */
    private Pair<Duration, SpeedWithBearing> calculateDurationAndAvgSpeedWithBearingBetweenTimePoints(TimePoint from,
            TimePoint to) {
        Duration duration = from.until(to);
        Position fromPosition = maneuverDetector.track.getEstimatedPosition(from, false);
        Position toPosition = maneuverDetector.track.getEstimatedPosition(to, false);
        Distance distance = fromPosition.getDistance(toPosition);
        Bearing bearing = fromPosition.getBearingGreatCircle(toPosition);
        Speed speed = distance.inTime(Math.abs(duration.asMillis()));
        SpeedWithBearing avgSpeedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(), bearing);
        return new Pair<>(duration, avgSpeedWithBearing);
    }

    /**
     * Gets the relative bearing of the next mark from the boat's position and course at {@code timePoint}. The relative
     * bearing is calculated by absolute bearing of next mark from the boat's position minus the boat's course.
     */
    public Bearing getRelativeBearingToNextMark(TimePoint timePoint, Bearing boatCourse) {
        NonCachingMarkPositionAtTimePointCache markPositionAtTimePointCache = new NonCachingMarkPositionAtTimePointCache(
                maneuverDetector.trackedRace, timePoint);
        Bearing result = null;
        TrackedLegOfCompetitor legAfter = maneuverDetector.trackedRace.getTrackedLeg(maneuverDetector.competitor,
                timePoint);
        if (legAfter != null && legAfter.getLeg().getTo() != null) {
            Position maneuverEndPosition = maneuverDetector.track.getEstimatedPosition(timePoint, false);
            Waypoint nextWaypoint = legAfter.getLeg().getTo();
            for (Mark mark : nextWaypoint.getMarks()) {
                Position nextMarkPosition = markPositionAtTimePointCache.getEstimatedPosition(mark);
                Bearing absoluteBearing = maneuverEndPosition.getBearingGreatCircle(nextMarkPosition);
                Bearing resultCandidate = absoluteBearing.getDifferenceTo(boatCourse);
                if (result == null) {
                    result = resultCandidate;
                } else if (Math.signum(result.getDegrees()) != Math.signum(resultCandidate.getDegrees())) {
                    result = new DegreeBearingImpl(0);
                    break;
                } else if (Math.abs(result.getDegrees()) > Math.abs(resultCandidate.getDegrees())) {
                    result = resultCandidate;
                }

            }
        }
        return result;
    }

}
