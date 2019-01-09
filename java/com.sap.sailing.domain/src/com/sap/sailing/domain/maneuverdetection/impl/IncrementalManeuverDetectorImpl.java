package com.sap.sailing.domain.maneuverdetection.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.maneuverdetection.ApproximatedFixesCalculator;
import com.sap.sailing.domain.maneuverdetection.IncrementalApproximatedFixesCalculator;
import com.sap.sailing.domain.maneuverdetection.IncrementalManeuverDetector;
import com.sap.sailing.domain.maneuverdetection.impl.WindEstimationInteraction.PossibleChangeOfManeuverTypesInfo;
import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * Incremental maneuver detector, which is capable of detecting maneuvers by a {@link #detectManeuvers()} call in an
 * incremental way. This detector is using {@link ApproximatedFixesCalculatorImpl} for calculation of Douglas Peucker
 * fixes. When the Douglas Peucker fixes are calculated, it tries to match the resulting fixes with already calculated
 * Douglas Peucker fixes groups for already calculated maneuvers (represented by {@link ManeuverSpot}) from previous
 * {@link #detectManeuvers()} calls. The existing maneuvers of matched existing Douglas Peucker fixes groups are reused,
 * when the following conditions are met:
 * <ul>
 * <li>The next determined Douglas Peucker fix following after the last fix of the matched existing Douglas Peucker
 * fixes group gets matched with the beginning fix of an another existing Douglas Peucker fixes group</li>
 * <li>The currently measured wind within the Douglas Peucker fixes group is nearly the same as previouly measured when
 * the existing Douglas Peucker fixes group had been determined.</li>
 * <li>The reused Douglas Peucker fixes group is far enough from the last fix of the track, so that its maneuvers cannot
 * be extended by new incoming fixes. The "far enough" is defined in
 * {@link #checkManeuverSpotFarEnoughFromLatestRawFix(TimePoint, long, TimePoint, ManeuverSpot)}.
 * </ul>
 * With exception: the first and last calculated Douglas Peucker points are ignored during matching process, because
 * they get never added to Douglas Peucker fixes sets which represent maneuver section.
 * {@link #checkDouglasPeuckerFixesNearlySame(GPSFixMoving, GPSFixMoving)} defines whether two Douglas Peucker match and
 * are nearly same. The nearly the same measured wind is defined by
 * {@link #checkManeuverSpotWindNearlySame(ManeuverSpot)}.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class IncrementalManeuverDetectorImpl extends ManeuverDetectorImpl implements IncrementalManeuverDetector {

    /**
     * The wind course tolerance in degrees which defines the maximal acceptable deviation of wind measurement between
     * previously measured wind and the current recorded wind at the same maneuver spot. If the tolerance limit is
     * exceeded, the maneuvers for the analysed maneuver spot get recalculated.
     */
    private static final double WIND_COURSE_TOLERANCE_IN_DEGREES_TO_IGNORE_FOR_MANEUVER_REUSE = 5.0;

    /**
     * Defines the tolerance in seconds between last time points of douglas peucker fixes within a maneuver spot and the
     * time points of currently analysed douglas peucker fixes. If the tolerance limit is exceeded, the existing
     * maneuver spot gets discarded.
     */
    private static final double DOUGLAS_PEUCKER_FIXES_TIME_POINT_TOLERANCE_IN_SECONDS_TO_IGNORE_FOR_MANEUVER_REUSE = 1;

    /**
     * The result of previous maneuver detection, or {@code null} if not performed until yet
     */
    private volatile ManeuverDetectionResult lastManeuverDetectionResult = null;

    /**
     * Calculator for douglas peucker fixes
     */
    private final ApproximatedFixesCalculator approximatedFixesCalculator;

    private final WindEstimationInteraction windEstimationInteraction;

    /**
     * Constructor for unit tests only.
     */
    public IncrementalManeuverDetectorImpl() {
        super();
        approximatedFixesCalculator = null;
        windEstimationInteraction = null;
    }

    /**
     * Constructs incremental maneuver detector which is supposed to be used for maneuver detection within the provided
     * tracked race for provided competitor.
     * 
     * @param trackedRace
     *            The tracked race whose maneuvers are supposed to be detected
     * @param competitor
     *            The competitor, whose maneuvers shall be discovered
     */
    public IncrementalManeuverDetectorImpl(TrackedRace trackedRace, Competitor competitor,
            WindEstimationInteraction windEstimationInteraction) {
        super(trackedRace, competitor);
        this.windEstimationInteraction = windEstimationInteraction;
        // TODO Use IncrementalApproximatedFixesCalculatorImpl when its deprecation status gets fixed
        this.approximatedFixesCalculator = new ApproximatedFixesCalculatorImpl(trackedRace, competitor);
    }

    @Override
    public List<Maneuver> getAlreadyDetectedManeuvers() {
        ManeuverDetectionResult lastManeuverDetectionResult = this.lastManeuverDetectionResult;
        if (lastManeuverDetectionResult != null) {
            return getAllManeuversFromManeuverSpots(lastManeuverDetectionResult.getManeuverSpots());
        }
        return Collections.emptyList();
    }

    @Override
    public List<CompleteManeuverCurve> getAlreadyDetectedManeuverCurves() {
        ManeuverDetectionResult lastManeuverDetectionResult = this.lastManeuverDetectionResult;
        if (lastManeuverDetectionResult != null) {
            return lastManeuverDetectionResult.getManeuverSpots().stream()
                    .filter(maneuverSpot -> maneuverSpot.getManeuverCurve() != null)
                    .map(maneuverSpot -> maneuverSpot.getManeuverCurve()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void clearState() {
        lastManeuverDetectionResult = null;
        if (approximatedFixesCalculator instanceof IncrementalApproximatedFixesCalculator) {
            ((IncrementalApproximatedFixesCalculator) approximatedFixesCalculator).clearState();
        }
    }

    @Override
    public List<Maneuver> detectManeuvers() {
        List<ManeuverSpotWithTypedManeuvers> maneuverSpots = detectManeuverSpots();
        List<Maneuver> maneuvers = getAllManeuversFromManeuverSpots(maneuverSpots);
        return maneuvers;
    }

    private List<Maneuver> getAllManeuversFromManeuverSpots(List<ManeuverSpotWithTypedManeuvers> maneuverSpots) {
        List<Maneuver> maneuvers = new ArrayList<>(maneuverSpots.size());
        for (ManeuverSpotWithTypedManeuvers maneuverSpot : maneuverSpots) {
            for (Maneuver maneuver : maneuverSpot.getManeuvers()) {
                maneuvers.add(maneuver);
            }
        }
        return maneuvers;
    }

    @Override
    protected List<ManeuverSpotWithTypedManeuvers> detectManeuverSpots() {
        TrackTimeInfo trackTimeInfo = getTrackTimeInfo();
        if (trackTimeInfo != null) {
            TimePoint earliestManeuverStart = trackTimeInfo.getTrackStartTimePoint();
            TimePoint latestManeuverEnd = trackTimeInfo.getTrackEndTimePoint();
            TimePoint latestRawFixTimePoint = trackTimeInfo.getLatestRawFixTimePoint();
            Iterable<GPSFixMoving> douglasPeuckerFixes = approximatedFixesCalculator.approximate(earliestManeuverStart,
                    latestManeuverEnd);
            ManeuverDetectionResult lastManeuverDetectionResult = this.lastManeuverDetectionResult;
            List<ManeuverSpotWithTypedManeuvers> maneuverSpotsWithTypedManeuvers;
            if (lastManeuverDetectionResult == null) {
                List<ManeuverSpot> maneuverSpots = detectManeuverSpots(douglasPeuckerFixes, earliestManeuverStart,
                        latestManeuverEnd);
                if (windEstimationInteraction != null) {
                    windEstimationInteraction.newManeuverSpotsDetected(competitor, maneuverSpots);
                }
                maneuverSpotsWithTypedManeuvers = new ArrayList<>();
                for (ManeuverSpot maneuverSpot : maneuverSpots) {
                    ManeuverSpotWithTypedManeuvers maneuverSpotWithTypedManeuvers = createManeuverSpotWithTypedManeuversFromManeuverCurve(
                            (List<GPSFixMoving>) maneuverSpot.getDouglasPeuckerFixes(),
                            maneuverSpot.getManeuverSpotDirection(), maneuverSpot.getManeuverCurve());
                    maneuverSpotsWithTypedManeuvers.add(maneuverSpotWithTypedManeuvers);
                }
            } else if (latestRawFixTimePoint.equals(lastManeuverDetectionResult.getLatestRawFixTimePoint())) {
                maneuverSpotsWithTypedManeuvers = new ArrayList<>();
                for (ManeuverSpotWithTypedManeuvers existingManeuverSpot : lastManeuverDetectionResult
                        .getManeuverSpots()) {
                    ManeuverSpotWithTypedManeuvers newManeuverSpot = getManeuverSpotWithTypedManeuversFromExistingManeuverSpotConsideringPossibleWindChange(
                            existingManeuverSpot);
                    maneuverSpotsWithTypedManeuvers.add(newManeuverSpot);
                }
            } else {
                IncrementalManeuverSpotDetectionResult detectionResult = detectManeuverSpotsIncrementally(trackTimeInfo,
                        douglasPeuckerFixes, lastManeuverDetectionResult);
                PossibleChangeOfManeuverTypesInfo possibleChangeOfManeuverTypesInfo = null;
                if (windEstimationInteraction != null) {
                    possibleChangeOfManeuverTypesInfo = windEstimationInteraction.newManeuverSpotsDetected(competitor,
                            detectionResult.getNewManeuverSpots());
                }
                maneuverSpotsWithTypedManeuvers = getAllManeuverSpotsWithTypedManeuversFromDetectionResultSortedByTimePoint(
                        detectionResult);
                if (possibleChangeOfManeuverTypesInfo.isWindChanged()) {
                    for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
                        if (possibleChangeOfManeuverTypesInfo.isWindChangedForCompetitor(competitor)) {
                            if (this.competitor.equals(competitor)) {
                                List<ManeuverSpotWithTypedManeuvers> maneuverSpotsWithChangedWind = possibleChangeOfManeuverTypesInfo
                                        .getManeuverSpotsWithChangedWindForCompetitor(competitor);
                                maneuverSpotsWithTypedManeuvers = readjustTypedManeuverSpotsAfterWindChange(
                                        maneuverSpotsWithTypedManeuvers, maneuverSpotsWithChangedWind);
                            } else {
                                List<Maneuver> maneuversWithPossibleWindChanges = new ArrayList<>();
                                for (ManeuverSpotWithTypedManeuvers maneuverSpot : possibleChangeOfManeuverTypesInfo
                                        .getManeuverSpotsWithChangedWindForCompetitor(competitor)) {
                                    for (Maneuver maneuver : maneuverSpot.getManeuvers()) {
                                        maneuversWithPossibleWindChanges.add(maneuver);
                                    }
                                }
                                trackedRace.recalculateManeuverTypesAfterPossibleWindChanges(competitor,
                                        maneuversWithPossibleWindChanges);
                            }
                        }
                    }
                }
            }
            int incrementalRunsCount = lastManeuverDetectionResult == null ? 1
                    : (lastManeuverDetectionResult.getIncrementalRunsCount() < Integer.MAX_VALUE
                            ? lastManeuverDetectionResult.getIncrementalRunsCount() + 1
                            : Integer.MAX_VALUE);
            this.lastManeuverDetectionResult = new ManeuverDetectionResult(latestRawFixTimePoint,
                    maneuverSpotsWithTypedManeuvers, incrementalRunsCount);
            return maneuverSpotsWithTypedManeuvers;
        }
        return Collections.emptyList();
    }

    // TODO Would be nice if trackedRace.recalculateManeuverTypesAfterPossibleWindChanges(competitor,
    // maneuversWithPossibleWindChanges) will call this method instead of triggerManeuverCacheRecalculation(competitor)
    @Override
    public List<Maneuver> recalculateManeuverTypesAfterPossibleWindChangesAndGetAllManeuvers(
            Iterable<Maneuver> maneuversWithPossibleWindChanges) {
        ManeuverDetectionResult lastManeuverDetectionResult = this.lastManeuverDetectionResult;
        if (lastManeuverDetectionResult == null) {
            return detectManeuvers();
        } else {
            List<ManeuverSpotWithTypedManeuvers> existingManeuverSpots = lastManeuverDetectionResult.getManeuverSpots();
            List<ManeuverSpotWithTypedManeuvers> reviewedManeuverSpots = new ArrayList<>();
            for (ManeuverSpotWithTypedManeuvers existingManeuverSpot : existingManeuverSpots) {
                if (Util.containsAll(maneuversWithPossibleWindChanges, existingManeuverSpot.getManeuvers())) {
                    ManeuverSpotWithTypedManeuvers newManeuverSpot = getManeuverSpotWithTypedManeuversFromExistingManeuverSpotConsideringPossibleWindChange(
                            existingManeuverSpot);
                    reviewedManeuverSpots.add(newManeuverSpot);
                } else {
                    reviewedManeuverSpots.add(existingManeuverSpot);
                }
            }
            int incrementalRunsCount = lastManeuverDetectionResult.getIncrementalRunsCount() < Integer.MAX_VALUE
                    ? lastManeuverDetectionResult.getIncrementalRunsCount() + 1
                    : Integer.MAX_VALUE;
            this.lastManeuverDetectionResult = new ManeuverDetectionResult(
                    lastManeuverDetectionResult.getLatestRawFixTimePoint(), reviewedManeuverSpots,
                    incrementalRunsCount);
            List<Maneuver> maneuvers = getAllManeuversFromManeuverSpots(reviewedManeuverSpots);
            return maneuvers;
        }
    }

    private List<ManeuverSpotWithTypedManeuvers> readjustTypedManeuverSpotsAfterWindChange(
            List<ManeuverSpotWithTypedManeuvers> existingManeuverSpots,
            List<ManeuverSpotWithTypedManeuvers> maneuverSpotsWithWindChange) {
        List<ManeuverSpotWithTypedManeuvers> result = new ArrayList<>(existingManeuverSpots.size());
        for (ManeuverSpotWithTypedManeuvers existingManeuverSpot : existingManeuverSpots) {
            if (maneuverSpotsWithWindChange.contains(existingManeuverSpot)) {
                ManeuverSpotWithTypedManeuvers newManeuverSpot = getManeuverSpotWithTypedManeuversFromExistingManeuverSpotConsideringPossibleWindChange(
                        existingManeuverSpot);
                result.add(newManeuverSpot);
            } else {
                result.add(existingManeuverSpot);
            }
        }
        return result;
    }

    protected List<ManeuverSpotWithTypedManeuvers> getAllManeuverSpotsWithTypedManeuversFromDetectionResultSortedByTimePoint(
            IncrementalManeuverSpotDetectionResult detectionResult) {
        List<ManeuverSpotWithTypedManeuvers> result = new ArrayList<>();
        Iterator<ManeuverSpotWithTypedManeuvers> exitingManeuverSpotsIterator = detectionResult
                .getManeuverSpotsToReuse().iterator();
        Iterator<ManeuverSpot> newManeuverSpotsIterator = detectionResult.getNewManeuverSpots().iterator();
        ManeuverSpot currentNewManeuverSpot = newManeuverSpotsIterator.hasNext() ? newManeuverSpotsIterator.next()
                : null;
        ManeuverSpotWithTypedManeuvers currentExistingManeuverSpot = exitingManeuverSpotsIterator.hasNext()
                ? exitingManeuverSpotsIterator.next()
                : null;
        while (true) {
            if (currentExistingManeuverSpot != null && (currentNewManeuverSpot == null
                    || !currentExistingManeuverSpot.getManeuverCurve().getMainCurveBoundaries().getTimePoint().after(
                            currentNewManeuverSpot.getManeuverCurve().getMainCurveBoundaries().getTimePoint()))) {
                ManeuverSpotWithTypedManeuvers maneuverSpotWithTypedManeuvers = getManeuverSpotWithTypedManeuversFromExistingManeuverSpotConsideringPossibleWindChange(
                        currentExistingManeuverSpot);
                result.add(maneuverSpotWithTypedManeuvers);
                currentExistingManeuverSpot = exitingManeuverSpotsIterator.hasNext()
                        ? exitingManeuverSpotsIterator.next()
                        : null;
            } else if (currentNewManeuverSpot != null) {
                ManeuverSpotWithTypedManeuvers maneuverSpotWithTypedManeuvers = createManeuverSpotWithTypedManeuversFromManeuverCurve(
                        (List<GPSFixMoving>) currentNewManeuverSpot.getDouglasPeuckerFixes(),
                        currentNewManeuverSpot.getManeuverSpotDirection(), currentNewManeuverSpot.getManeuverCurve());
                result.add(maneuverSpotWithTypedManeuvers);
                currentNewManeuverSpot = newManeuverSpotsIterator.hasNext() ? newManeuverSpotsIterator.next() : null;
            } else {
                break;
            }
        }
        return result;
    }

    private ManeuverSpotWithTypedManeuvers getManeuverSpotWithTypedManeuversFromExistingManeuverSpotConsideringPossibleWindChange(
            ManeuverSpotWithTypedManeuvers currentExistingManeuverSpot) {
        ManeuverSpotWithTypedManeuvers maneuverSpotWithTypedManeuvers;
        if (checkManeuverSpotWindNearlySame(currentExistingManeuverSpot)
                || currentExistingManeuverSpot.getManeuverCurve() == null) {
            // We found an existing maneuver spot with similar fixes and estimated winds => reuse
            // existing maneuver spot
            maneuverSpotWithTypedManeuvers = currentExistingManeuverSpot;
        } else {
            // New wind information has been received which considerably differs from previous
            // maneuver spot calculation => recalculate maneuvers of existing maneuver curve
            CompleteManeuverCurve maneuverCurve = currentExistingManeuverSpot.getManeuverCurve();
            WindMeasurement windMeasurement = currentExistingManeuverSpot.getWindMeasurement();
            Wind wind = trackedRace.getWind(windMeasurement.getPosition(), windMeasurement.getTimePoint());
            List<Maneuver> maneuvers = determineManeuversFromManeuverCurve(maneuverCurve.getMainCurveBoundaries(),
                    maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries(), wind,
                    maneuverCurve.getMarkPassing());
            maneuverSpotWithTypedManeuvers = new ManeuverSpotWithTypedManeuvers(
                    currentExistingManeuverSpot.getDouglasPeuckerFixes(),
                    currentExistingManeuverSpot.getManeuverSpotDirection(), maneuverCurve, maneuvers,
                    new WindMeasurement(windMeasurement.getTimePoint(), windMeasurement.getPosition(),
                            wind.getBearing()));
        }
        return maneuverSpotWithTypedManeuvers;
    }

    // protected for unit tests
    protected IncrementalManeuverSpotDetectionResult detectManeuverSpotsIncrementally(TrackTimeInfo trackTimeInfo,
            Iterable<GPSFixMoving> approximatingFixesToAnalyze, ManeuverDetectionResult lastManeuverDetectionResult) {
        List<ManeuverSpotWithTypedManeuvers> existingManeuverSpots = new ArrayList<>();
        List<ManeuverSpot> newManeuverSpots = new ArrayList<>();
        TimePoint earliestManeuverStart = trackTimeInfo.getTrackStartTimePoint();
        TimePoint latestManeuverEnd = trackTimeInfo.getTrackEndTimePoint();
        TimePoint latestRawFixTimePoint = trackTimeInfo.getLatestRawFixTimePoint();
        long maxDurationForDouglasPeuckerFixExtensionInManeuverAnalysisInMillis = getMaxDurationForDouglasPeuckerFixExtensionInManeuverAnalysis()
                .asMillis();
        TimePoint latestRawFixTimePointOfPreviousManeuverDetectionIteration = lastManeuverDetectionResult
                .getLatestRawFixTimePoint();
        if (Util.size(approximatingFixesToAnalyze) > 2) {
            List<GPSFixMoving> fixesGroupForManeuverSpotAnalysis = new ArrayList<GPSFixMoving>();
            Iterator<GPSFixMoving> approximationPointsIter = approximatingFixesToAnalyze.iterator();
            GPSFixMoving previous = approximationPointsIter.next();
            GPSFixMoving current = approximationPointsIter.next();
            NauticalSide lastCourseChangeDirection = null;
            ManeuverSpotWithTypedManeuvers matchingManeuverSpotFromState = null;
            Iterator<GPSFixMoving> matchingFixesGroupFromStateIterator = null;
            ListIterator<ManeuverSpotWithTypedManeuvers> lastManeuverSpotIteratorUsed = getExistingManeuverSpotByFirstDouglasPeuckerFix(
                    lastManeuverDetectionResult, null, current);
            ManeuverSpotWithTypedManeuvers nextExistingSpot = lastManeuverSpotIteratorUsed != null
                    ? lastManeuverSpotIteratorUsed.next()
                    : null;
            do {
                GPSFixMoving next = approximationPointsIter.next();
                // check if we have previously found a similar fixes group from state
                if (matchingManeuverSpotFromState != null) {
                    if (matchingFixesGroupFromStateIterator.hasNext()) {
                        GPSFixMoving existingDouglasPeuckerFix = matchingFixesGroupFromStateIterator.next();
                        if (!checkDouglasPeuckerFixesNearlySame(existingDouglasPeuckerFix, current)) {
                            // existing maneuver spot does not match with the fixes sequence in this run => discard
                            // existing maneuver spot and process fixesGroupForManeuverSpotAnalysis normally like in
                            // ManeuverDetectorImpl
                            matchingManeuverSpotFromState = null;
                        }
                    } else {
                        // check if the existing group is followed by an existing group, otherwise discard the existing
                        // maneuver spot, because it can possibly be extended by the next fix.
                        ListIterator<ManeuverSpotWithTypedManeuvers> maneuverSpotIterator = getExistingManeuverSpotByFirstDouglasPeuckerFix(
                                lastManeuverDetectionResult, lastManeuverSpotIteratorUsed, current);
                        if (maneuverSpotIterator != null) {
                            nextExistingSpot = maneuverSpotIterator.next();
                            lastManeuverSpotIteratorUsed = maneuverSpotIterator;
                            existingManeuverSpots.add(matchingManeuverSpotFromState);
                            fixesGroupForManeuverSpotAnalysis.clear();
                        }
                        matchingManeuverSpotFromState = null;
                    }
                }
                // If we are not matching the fixes with existing fixes group, analyze fixes grouping normally like
                // ManeuverDetectorImpl does
                if (matchingManeuverSpotFromState == null && nextExistingSpot == null) {
                    // Split douglas peucker fixes groups to identify maneuver spots
                    NauticalSide courseChangeDirectionOnOriginalFixes = getCourseChangeDirectionAroundFix(
                            previous.getTimePoint(), current, next.getTimePoint());
                    if (!fixesGroupForManeuverSpotAnalysis.isEmpty()
                            && !checkDouglasPeuckerFixesGroupable(lastCourseChangeDirection,
                                    courseChangeDirectionOnOriginalFixes, previous, current)) {
                        // current fix does not belong to the existing fixes group; determine maneuvers of recent fixes
                        // group, then start a new list
                        ManeuverSpot maneuverSpot = createManeuverSpotWithManeuversFromFixesGroup(
                                fixesGroupForManeuverSpotAnalysis, lastCourseChangeDirection, earliestManeuverStart,
                                latestManeuverEnd);
                        newManeuverSpots.add(maneuverSpot);
                        fixesGroupForManeuverSpotAnalysis.clear();
                    }
                    lastCourseChangeDirection = courseChangeDirectionOnOriginalFixes;
                }
                fixesGroupForManeuverSpotAnalysis.add(current);
                // check if we have a new fixes group.
                if (fixesGroupForManeuverSpotAnalysis.size() == 1) {
                    // Check if we got an existing maneuver spot with similar fix at beginning
                    ManeuverSpotWithTypedManeuvers maneuverSpot;
                    if (nextExistingSpot != null) {
                        maneuverSpot = nextExistingSpot;
                        nextExistingSpot = null;
                    } else {
                        ListIterator<ManeuverSpotWithTypedManeuvers> maneuverSpotIterator = getExistingManeuverSpotByFirstDouglasPeuckerFix(
                                lastManeuverDetectionResult, lastManeuverSpotIteratorUsed, current);
                        if (maneuverSpotIterator != null) {
                            maneuverSpot = maneuverSpotIterator.next();
                            lastManeuverSpotIteratorUsed = maneuverSpotIterator;
                        } else {
                            maneuverSpot = null;
                        }
                    }
                    if (maneuverSpot != null) {
                        // if the maneuver
                        // spot is lying within time range of latestRawFix.getTimePoint() - (longest maneuver
                        // duration)
                        // and
                        // latestRawFixTimePoint.after(latestRawFixTimePointOfPreviousManeuverDetectionIteration),
                        // then we need to recalculate the maneuver spot, because the boundaries of maneuver may get
                        // extended by new incoming fixes
                        boolean maneuverSpotIsFarEnoughFromLatestRawFix = checkManeuverSpotFarEnoughFromLatestRawFix(
                                latestRawFixTimePoint,
                                maxDurationForDouglasPeuckerFixExtensionInManeuverAnalysisInMillis,
                                latestRawFixTimePointOfPreviousManeuverDetectionIteration, maneuverSpot);

                        if (maneuverSpotIsFarEnoughFromLatestRawFix) {
                            matchingManeuverSpotFromState = maneuverSpot;
                            matchingFixesGroupFromStateIterator = maneuverSpot.getDouglasPeuckerFixes().iterator();
                            // first fix already matched with getExistingManeuverSpotByFirstDouglasPeuckerFix(current)
                            // call => move iteration cursor to next
                            matchingFixesGroupFromStateIterator.next();
                        }
                        lastCourseChangeDirection = maneuverSpot.getManeuverSpotDirection();
                    }
                }
                previous = current;
                current = next;
            } while (approximationPointsIter.hasNext());
            if (!fixesGroupForManeuverSpotAnalysis.isEmpty()) {
                ManeuverSpot maneuverSpot = createManeuverSpotWithManeuversFromFixesGroup(
                        fixesGroupForManeuverSpotAnalysis, lastCourseChangeDirection, earliestManeuverStart,
                        latestManeuverEnd);
                newManeuverSpots.add(maneuverSpot);
            }
        }
        return new IncrementalManeuverSpotDetectionResult(existingManeuverSpots, newManeuverSpots);
    }

    /**
     * Checks whether the provided maneuver spot is too close to {@code latestRawFixTimePoint}, so that recalculation of
     * the maneuver boundaries is needed, because the boundaries may get extended by new incoming fixes.
     */
    private boolean checkManeuverSpotFarEnoughFromLatestRawFix(TimePoint latestRawFixTimePoint,
            long maxDurationForDouglasPeuckerFixExtensionInManeuverAnalysisInMillis,
            TimePoint latestRawFixTimePointOfPreviousManeuverDetectionIteration,
            ManeuverSpot previouslyDetectedManeuverSpotWithSameDouglasPeuckerPoints) {
        if (latestRawFixTimePoint.after(latestRawFixTimePointOfPreviousManeuverDetectionIteration)) {
            GPSFixMoving latestDouglasPeuckerFix = null;
            for (GPSFixMoving fix : previouslyDetectedManeuverSpotWithSameDouglasPeuckerPoints
                    .getDouglasPeuckerFixes()) {
                latestDouglasPeuckerFix = fix;
            }
            if (latestDouglasPeuckerFix.getTimePoint().until(latestRawFixTimePoint)
                    .asMillis() < maxDurationForDouglasPeuckerFixExtensionInManeuverAnalysisInMillis) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the max duration by which the maneuver boundary can be extended after the latest douglas peucker fix of
     * provided maneuver spot.
     */
    private Duration getMaxDurationForDouglasPeuckerFixExtensionInManeuverAnalysis() {
        Duration approximateManeuverDuration = getApproximateManeuverDuration();
        return getDurationForDouglasPeuckerExtensionForMainCurveAnalysis(approximateManeuverDuration)
                .plus(getMaxDurationForAfterManeuverSectionExtension(approximateManeuverDuration));
    }

    private boolean checkManeuverSpotWindNearlySame(ManeuverSpotWithTypedManeuvers maneuverSpot) {
        if (maneuverSpot.getWindMeasurement() != null) {
            WindMeasurement windMeasurement = maneuverSpot.getWindMeasurement();
            Bearing lastWindCourse = windMeasurement.getWindCourse();
            Wind currentWind = trackedRace.getWind(windMeasurement.getPosition(), windMeasurement.getTimePoint());
            if (lastWindCourse == null && currentWind == null) {
                return true;
            }
            if (lastWindCourse == null || currentWind == null) {
                return false;
            }
            double bearingInDegrees = lastWindCourse.getDifferenceTo(currentWind.getBearing()).getDegrees();
            if (bearingInDegrees > WIND_COURSE_TOLERANCE_IN_DEGREES_TO_IGNORE_FOR_MANEUVER_REUSE) {
                return false;
            }
        }
        // maneuver spot has no maneuvers, or previously measured wind and current wind are within tolerance level
        return true;
    }

    private boolean checkDouglasPeuckerFixesNearlySame(GPSFixMoving existingDouglasPeuckerFix,
            GPSFixMoving newDouglasPeuckerFix) {
        double secondsDifference = existingDouglasPeuckerFix.getTimePoint().until(newDouglasPeuckerFix.getTimePoint())
                .asSeconds();
        if (Math.abs(
                secondsDifference) > DOUGLAS_PEUCKER_FIXES_TIME_POINT_TOLERANCE_IN_SECONDS_TO_IGNORE_FOR_MANEUVER_REUSE) {
            return false;
        }
        return true;
    }

    // for unit tests only
    protected void setLastManeuverDetectionResult(ManeuverDetectionResult lastManeuverDetectionResult) {
        this.lastManeuverDetectionResult = lastManeuverDetectionResult;
    }

    @Override
    public int getIncrementalRunsCount() {
        ManeuverDetectionResult lastManeuverDetectionResult = this.lastManeuverDetectionResult;
        return lastManeuverDetectionResult != null ? lastManeuverDetectionResult.getIncrementalRunsCount() : 0;
    }

    /**
     * Tries to get an already processed maneuver spot from previous calls of {@link #detectManeuvers()} which starts
     * with a douglas peucker fix similar to the provided {@code newDouglasPeuckerFix}. This method was designed to run
     * within loops of {@link #detectManeuverSpotsIncrementally(TrackTimeInfo, Iterable, ManeuverDetectionResult)}. In
     * order to prevent squared iteration complexity, the method returns a {@code ListIterator} which is supposed to be
     * used to retrieve the located existing maneuver spot, as well as to provide the same iterator for the following
     * call of this method within following iterations, in order to resume the search iteration from the position of the
     * previously retrieved maneuver spot.
     * 
     * @param lastManeuverDetectionResult
     *            The result of previously performed maneuver detection, which contains the maneuver spots to look up
     * @param lastIteratorUsed
     *            {@code ListIterator}, which was returned by previous call of this method, or {@code null} which causes
     *            iteration of existing maneuver spots start from scratch
     * @param newDouglasPeuckerFix
     *            The beginning douglas peucker fix of maneuver spot to find
     * @return {@code null} if no corresponding maneuver spot could be found, otherwise a {@code ListIterator} which
     *         points to the matched maneuver spot in its first {@code next()} call.
     */
    private ListIterator<ManeuverSpotWithTypedManeuvers> getExistingManeuverSpotByFirstDouglasPeuckerFix(
            ManeuverDetectionResult lastManeuverDetectionResult,
            ListIterator<ManeuverSpotWithTypedManeuvers> lastIteratorUsed, GPSFixMoving newDouglasPeuckerFix) {
        ManeuverSpotWithTypedManeuvers firstManeuverSpotIterated = null;
        if (lastIteratorUsed != null) {
            while (lastIteratorUsed.hasNext()) {
                ManeuverSpotWithTypedManeuvers maneuverSpot = lastIteratorUsed.next();
                if (firstManeuverSpotIterated == null) {
                    firstManeuverSpotIterated = maneuverSpot;
                }
                GPSFixMoving firstFix = maneuverSpot.getDouglasPeuckerFixes().iterator().next();
                if (checkDouglasPeuckerFixesNearlySame(newDouglasPeuckerFix, firstFix)) {
                    lastIteratorUsed.previous();
                    return lastIteratorUsed;
                }
            }
        }
        ListIterator<ManeuverSpotWithTypedManeuvers> newIterator = lastManeuverDetectionResult.getManeuverSpots()
                .listIterator();
        // no maneuver spot detected with lastIteratorUsed. Try from beginning until firstManeuverSpotIterated
        while (newIterator.hasNext()) {
            ManeuverSpotWithTypedManeuvers maneuverSpot = newIterator.next();
            if (maneuverSpot == firstManeuverSpotIterated) {
                break;
            }
            GPSFixMoving firstFix = maneuverSpot.getDouglasPeuckerFixes().iterator().next();
            if (checkDouglasPeuckerFixesNearlySame(newDouglasPeuckerFix, firstFix)) {
                newIterator.previous();
                return newIterator;
            }
        }

        return null;
    }

    // protected for unit tests
    /**
     * Represents a result of already performed maneuver analysis. The result is used by
     * {@link IncrementalManeuverDetectorImpl} to determine maneuvers incrementally.
     * 
     * @author Vladislav Chumak (D069712)
     *
     */
    protected static class ManeuverDetectionResult {

        private final TimePoint latestFixTimePoint;
        private final List<ManeuverSpotWithTypedManeuvers> maneuverSpots;
        private final int incrementalRunsCount;

        public ManeuverDetectionResult(TimePoint latestFixTimePoint, List<ManeuverSpotWithTypedManeuvers> maneuverSpots,
                int incrementalRunsCount) {
            this.latestFixTimePoint = latestFixTimePoint;
            this.maneuverSpots = maneuverSpots;
            this.incrementalRunsCount = incrementalRunsCount;
        }

        public TimePoint getLatestRawFixTimePoint() {
            return latestFixTimePoint;
        }

        public List<ManeuverSpotWithTypedManeuvers> getManeuverSpots() {
            return maneuverSpots;
        }

        public int getIncrementalRunsCount() {
            return incrementalRunsCount;
        }

    }

    // protected for unit tests
    protected static class IncrementalManeuverSpotDetectionResult {
        private final List<ManeuverSpotWithTypedManeuvers> maneuverSpotsToReuse;
        private final List<ManeuverSpot> newManeuverSpots;

        public IncrementalManeuverSpotDetectionResult(List<ManeuverSpotWithTypedManeuvers> maneuverSpotsToReuse,
                List<ManeuverSpot> newManeuverSpots) {
            this.maneuverSpotsToReuse = maneuverSpotsToReuse;
            this.newManeuverSpots = newManeuverSpots;
        }

        public List<ManeuverSpotWithTypedManeuvers> getManeuverSpotsToReuse() {
            return maneuverSpotsToReuse;
        }

        public List<ManeuverSpot> getNewManeuverSpots() {
            return newManeuverSpots;
        }

    }

}
