package com.sap.sailing.domain.maneuverdetection.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.maneuverdetection.ApproximatedFixesCalculator;
import com.sap.sailing.domain.maneuverdetection.ManeuverDetector;
import com.sap.sailing.domain.maneuverdetection.TrackTimeInfo;
import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.ManeuverLoss;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.SpeedWithBearingStep;
import com.sap.sailing.domain.tracking.SpeedWithBearingStepsIterable;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.CompleteManeuverCurveImpl;
import com.sap.sailing.domain.tracking.impl.ManeuverCurveBoundariesImpl;
import com.sap.sailing.domain.tracking.impl.ManeuverWithMainCurveBoundariesImpl;
import com.sap.sailing.domain.tracking.impl.ManeuverWithStableSpeedAndCourseBoundariesImpl;
import com.sap.sailing.domain.tracking.impl.SpeedWithBearingStepImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.BearingChangeAnalyzer;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * @author Vladislav Chumak (D069712)
 * @see ManeuverDetector
 *
 */
public class ManeuverDetectorImpl extends AbstractManeuverDetectorImpl {
    
    /**
     * This {@link #DETECTOR_VERSION} variable indicates the version of the {@link ManeuverDetector} and must be changed
     * manually when changing the calculator. It should be changed by adding +1;
     */
    public static final int DETECTOR_VERSION = 1;


    private static final Logger logger = Logger.getLogger(ManeuverDetectorImpl.class.getName());

    /**
     * Defines the maximal absolute course change velocity in degrees per second that shall be regarded as a stable
     * course.
     */
    private static final double MAX_TURNING_RATE_IN_DEG_PER_SECOND_FOR_STABLE_COURSE_ANALYSIS = 1;

    /**
     * Defines the absolute course change in degrees between bearing steps to ignore in order to shorten the
     * approximated span between start and end time of maneuver main curve.
     */
    private static final double MIN_ANGULAR_VELOCITY_FOR_MAIN_CURVE_BOUNDARIES_IN_DEGREES_PER_SECOND = 0.2;

    /**
     * Constructor for unit tests only.
     */
    public ManeuverDetectorImpl() {
        super(null, null);
    }

    /**
     * Constructs maneuver detector which is supposed to be used for maneuver detection within the provided tracked race
     * for provided competitor.
     * 
     * @param trackedRace
     *            The tracked race whose maneuvers are supposed to be detected
     * @param competitor
     *            The competitor, whose maneuvers shall be discovered
     */
    public ManeuverDetectorImpl(TrackedRace trackedRace, Competitor competitor) {
        super(trackedRace, competitor);
    }

    @Override
    public List<Maneuver> detectManeuvers() {
        return getAllManeuversFromManeuverSpots(detectManeuverSpots());
    }

    /**
     * Detects maneuver spots performed within a GPS-track of the competitor associated with this
     * {@link ManeuverDetector}-instance. See {@link ManeuverDetector} description for more info regarding the detection
     * strategy.
     * 
     * @return an empty list if no maneuver spots were detected, otherwise the list with detected maneuver spots.
     * @see ManeuverSpot
     */
    protected List<? extends ManeuverSpot> detectManeuverSpots() {
        TrackTimeInfo startAndEndTimePoints = getTrackTimeInfo();
        if (startAndEndTimePoints != null) {
            List<ManeuverSpot> maneuverSpots = detectManeuverSpots(startAndEndTimePoints.getTrackStartTimePoint(),
                    startAndEndTimePoints.getTrackEndTimePoint());
            return maneuverSpots;
        }
        return Collections.emptyList();
    }

    /**
     * Detects the maneuver spots with corresponding maneuvers within provided time frame. See step 1ff. in
     * {@link ManeuverDetector} description.
     * 
     * @param earliestManeuverStart
     *            maneuver start will not be before this time point; if a maneuver is found whose time point is at or
     *            after this time point, no matter how close it is, its start regarding speed and course into the
     *            maneuver and the leg before the maneuver is not taken from an earlier time point, even if half the
     *            maneuver duration before the maneuver time point were before this time point.
     * @param latestManeuverEnd
     *            maneuver end will not be after this time point; if a maneuver is found whose time point is at or
     *            before this time point, no matter how close it is, its end regarding speed and course out of the
     *            maneuver and the leg after the maneuver is not taken from a later time point, even if half the
     *            maneuver duration after the maneuver time point were after this time point.
     * 
     * @return an empty list if no maneuver spots are detected for <code>competitor</code> between <code>from</code> and
     *         <code>to</code>, or else the list of maneuver spots with corresponding maneuvers detected.
     */
    public List<ManeuverSpot> detectManeuverSpots(TimePoint earliestManeuverStart, TimePoint latestManeuverEnd) {
        ApproximatedFixesCalculator approximatedFixesCalculator = new ApproximatedFixesCalculatorImpl(trackedRace,
                competitor);
        Iterable<GPSFixMoving> approximatedFixes = approximatedFixesCalculator.approximate(earliestManeuverStart,
                latestManeuverEnd);
        return detectManeuverSpots(approximatedFixes, earliestManeuverStart, latestManeuverEnd);
    }

    /**
     * Uses the provided {@code approximatingFixesToAnalyze} as douglas peucker fixes to detect maneuver spots with
     * corresponding maneuvers. See step 2ff. in {@link ManeuverDetector} description. Maneuvers can only be expected to
     * be detected if at least three fixes are provided in <code>approximatedFixesToAnalyze</code>. The first and the
     * last DP-fix get never associated with a maneuver spot.
     */
    protected List<ManeuverSpot> detectManeuverSpots(Iterable<GPSFixMoving> approximatingFixesToAnalyze,
            TimePoint earliestManeuverStart, TimePoint latestManeuverEnd) {
        List<ManeuverSpot> result = new ArrayList<>();
        if (Util.size(approximatingFixesToAnalyze) > 2) {
            List<GPSFixMoving> fixesGroupForManeuverSpotAnalysis = new ArrayList<GPSFixMoving>();
            Iterator<GPSFixMoving> approximationPointsIter = approximatingFixesToAnalyze.iterator();
            GPSFixMoving previous = approximationPointsIter.next();
            GPSFixMoving current = approximationPointsIter.next();
            NauticalSide lastCourseChangeDirection = null;
            do {
                GPSFixMoving next = approximationPointsIter.next();
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
                    result.add(maneuverSpot);
                    fixesGroupForManeuverSpotAnalysis.clear();
                }
                fixesGroupForManeuverSpotAnalysis.add(current);
                previous = current;
                current = next;
                lastCourseChangeDirection = courseChangeDirectionOnOriginalFixes;
            } while (approximationPointsIter.hasNext());
            if (!fixesGroupForManeuverSpotAnalysis.isEmpty()) {
                ManeuverSpot maneuverSpot = createManeuverSpotWithManeuversFromFixesGroup(
                        fixesGroupForManeuverSpotAnalysis, lastCourseChangeDirection, earliestManeuverStart,
                        latestManeuverEnd);
                result.add(maneuverSpot);
            }
        }
        return result;

    }

    /**
     * Checks whether {@code currentFix} can be grouped together with the previous fixes in order to be regarded as a
     * single maneuver spot. For this, the {@code newCourseChangeDirection must match the direction of provided {@code
     * lastCourseChangeDirection}. Additionally, the distance from {@code previousFix} to {@code currentFix} must be <=
     * 4 hull lengths, or the time difference <= getApproximatedManeuverDuration().
     * 
     * @param lastCourseChangeDirection The last course within previous three fixes counting from {@code currentFix}
     * 
     * @param mewCourseChangeDirection
     *            The current course within {@code previousFix}, {@code currentFix} and the fix which is following after
     *            {@code currentFix}
     * @param previousFix
     *            The fix before {@code currentFix}
     * @param currentFix
     *            The fix which is checked for grouping
     * @return {@code false} if fixes must not be grouped together, otherwise {@code true}
     */
    protected boolean checkDouglasPeuckerFixesGroupable(NauticalSide lastCourseChangeDirection,
            NauticalSide newCourseChangeDirection, GPSFixMoving previousFix, GPSFixMoving currentFix) {
        final boolean result;
        if (lastCourseChangeDirection != newCourseChangeDirection) {
            result = false;
        } else {
            Distance fourHullLengths = trackedRace.getRace().getBoatOfCompetitor(competitor).getBoatClass().getHullLength().scale(4);
            if (currentFix.getTimePoint().asMillis()
                    - previousFix.getTimePoint().asMillis() > getApproximateManeuverDuration().asMillis()
                    && currentFix.getPosition().getDistance(previousFix.getPosition()).compareTo(fourHullLengths) > 0) {
                result = false;
            } else {
                result = true;
            }
        }
        return result;
    }

    private List<Maneuver> getAllManeuversFromManeuverSpots(List<? extends ManeuverSpot> maneuverSpots) {
        List<Maneuver> maneuvers = new ArrayList<>();
        for (ManeuverSpot maneuverSpot : maneuverSpots) {
            ManeuverSpotWithTypedManeuvers maneuverSpotWithTypedManeuvers = createManeuverSpotWithTypedManeuversFromManeuverCurve(
                    maneuverSpot.getDouglasPeuckerFixes(), maneuverSpot.getManeuverSpotDirection(),
                    maneuverSpot.getManeuverCurve());
            for (Maneuver maneuver : maneuverSpotWithTypedManeuvers.getManeuvers()) {
                maneuvers.add(maneuver);
            }
        }
        return maneuvers;
    }

    /**
     * Determines course change direction around the provided {@code fix} by means of
     * {@link #getSpeedWithBearingSteps(TimePoint, TimePoint)}. The course change analysis considers fixes within
     * duration of maximally {@code getApproximateManeuverDuration()} before and after maneuver. More precisely, the
     * duration between analysis start and time point of the provided {@code fix}, as well as between the time point of
     * the provided {@code fix} and analysis end time point can be maximally
     * {@code getApproximateManeuverDuration().divide(2.0)} and minimally
     * {@code earliestCourseChangeAnalysisStart.until(fix.getTimePoint())} and
     * {@code latestCourseChangeAnalysisEnd.until(fix.getTimePoint())}
     */
    protected NauticalSide getCourseChangeDirectionAroundFix(TimePoint earliestCourseChangeAnalysisStart,
            GPSFixMoving fix, TimePoint latestCourseChangeAnalysisEnd) {
        TimePoint fromTimePointForCourseChangeAnalysis = earliestCourseChangeAnalysisStart;
        Duration durationFromEarliestStartToFix = fromTimePointForCourseChangeAnalysis.until(fix.getTimePoint());
        Duration maxDurationForOriginalFixesCourseChangeInvestigation = getApproximateManeuverDuration().divide(2.0);
        if (durationFromEarliestStartToFix.compareTo(maxDurationForOriginalFixesCourseChangeInvestigation) > 0) {
            fromTimePointForCourseChangeAnalysis = fix.getTimePoint()
                    .minus(maxDurationForOriginalFixesCourseChangeInvestigation);
        }
        TimePoint toTimePointForCourseChangeAnalysis = latestCourseChangeAnalysisEnd;
        Duration durationFromFixToLatestEnd = fix.getTimePoint().until(toTimePointForCourseChangeAnalysis);
        if (durationFromFixToLatestEnd.compareTo(maxDurationForOriginalFixesCourseChangeInvestigation) > 0) {
            toTimePointForCourseChangeAnalysis = fix.getTimePoint()
                    .plus(maxDurationForOriginalFixesCourseChangeInvestigation);
        }
        Bearing courseChangeOnOriginalFixes = getCourseChange(fromTimePointForCourseChangeAnalysis,
                toTimePointForCourseChangeAnalysis);
        return getDirectionOfCourseChange(courseChangeOnOriginalFixes.getDegrees());
    }

    /**
     * On <code>competitor</code>'s track iterates the fixes starting after <code>startExclusive</code> until
     * <code>endExclusive</code> or any later fix has been reached and sums up the direction change as a "bearing." A
     * negative sign means a direction change to port, a positive sign means a direction change to starboard.
     */
    private Bearing getCourseChange(TimePoint startInclusive, TimePoint endInclusive) {
        SpeedWithBearingStepsIterable speedWithBearingSteps = getSpeedWithBearingSteps(startInclusive, endInclusive);
        double totalCourseChangeInDegrees = 0;
        for (SpeedWithBearingStep step : speedWithBearingSteps) {
            totalCourseChangeInDegrees += step.getCourseChangeInDegrees();
        }
        return new DegreeBearingImpl(totalCourseChangeInDegrees);
    }

    /**
     * Creates maneuvers from the provided {@code group} of douglas peucker fixes considering the provided time range
     * limit. This method might return zero or more maneuvers. The maneuvers are determined by the following work-flow:
     * <ol>
     * <li>Main curve of maneuver within the time range of douglas peucker fixes +-
     * ({@link BoatClass#getApproximateManeuverDuration() maneuver duration}{@code  / 2}) is determined. The main curve
     * is defined as the section of maneuver with the highest absolute course change towards the provided
     * {@code maneuverDirection} ({@link Maneuver more info}).</li>
     * <li>Maneuver start and end with stable speed and course are determined by analysis of speed maxima starting
     * before and after the main curve, followed by extension to the points with stable course
     * ({@link #computeManeuverDetails(Competitor, ManeuverMainCurveDetailsWithBearingSteps, TimePoint, TimePoint) more
     * info})</li>
     * <li>The maneuver type is determined considering the maneuver's main curve, marks and wind direction</li>
     * </ol>
     * 
     * @param douglasPeuckerFixesGroup
     *            The douglas peucker fixes which may represent the maneuver basis
     * @param maneuverDirection
     *            The course change direction within douglas peucker fixes.
     * @param earliestManeuverStart
     *            Maneuver start will not be before this time point; if a maneuver is found whose time point is at or
     *            after this time point, no matter how close it is, its start regarding speed and course into the
     *            maneuver and the leg before the maneuver is not taken from an earlier time point, even if half the
     *            maneuver duration before the maneuver time point were before this time point.
     * @param latestManeuverEnd
     *            Maneuver end will not be after this time point; if a maneuver is found whose time point is at or
     *            before this time point, no matter how close it is, its end regarding speed and course out of the
     *            maneuver and the leg after the maneuver is not taken from a later time point, even if half the
     *            maneuver duration after the maneuver time point were after this time point.
     * @return The derived list maneuver spots with corresponding maneuvers. The maneuver spot count {@code >= 0}.
     */
    protected ManeuverSpot createManeuverSpotWithManeuversFromFixesGroup(List<GPSFixMoving> douglasPeuckerFixesGroup,
            NauticalSide maneuverDirection, TimePoint earliestManeuverStart, TimePoint latestManeuverEnd) {
        CompleteManeuverCurve maneuverCurve = createCompleteManeuverCurveFromFixesGroup(douglasPeuckerFixesGroup,
                maneuverDirection, earliestManeuverStart, latestManeuverEnd);
        return new ManeuverSpot(douglasPeuckerFixesGroup, maneuverDirection, maneuverCurve);
    }

    protected ManeuverSpotWithTypedManeuvers createManeuverSpotWithTypedManeuversFromManeuverCurve(
            List<GPSFixMoving> douglasPeuckerFixesGroup, NauticalSide maneuverDirection,
            CompleteManeuverCurve maneuverCurve) {
        if (maneuverCurve == null) {
            return new ManeuverSpotWithTypedManeuvers(douglasPeuckerFixesGroup, maneuverDirection, null,
                    new ArrayList<>(), null);
        }
        TimePoint maneuverTimePoint = maneuverCurve.getMainCurveBoundaries().getTimePoint();
        Position maneuverPosition = track.getEstimatedPosition(maneuverTimePoint, /* extrapolate */false);
        final Wind wind = trackedRace.getWind(maneuverPosition, maneuverTimePoint);
        List<Maneuver> maneuvers = determineManeuversFromManeuverCurve(maneuverCurve.getMainCurveBoundaries(),
                maneuverCurve.getManeuverCurveWithStableSpeedAndCourseBoundaries(), wind,
                maneuverCurve.getMarkPassing());
        return new ManeuverSpotWithTypedManeuvers(douglasPeuckerFixesGroup, maneuverDirection, maneuverCurve, maneuvers,
                new WindMeasurement(maneuverTimePoint, maneuverPosition, wind == null ? null : wind.getBearing()));
    }

    /**
     * Determines the complete maneuver curve for the provided DP-fixes group.
     * 
     * @param douglasPeuckerFixesGroup
     *            The douglas peucker fixes which represents the basis points of the complete maneuver curve.
     * @param maneuverDirection
     *            The course change direction within douglas peucker fixes.
     * @param earliestManeuverStart
     *            Maneuver curve start will not be before this time point; if a maneuver is found whose time point is at
     *            or after this time point, no matter how close it is, its start regarding speed and course into the
     *            maneuver and the leg before the maneuver is not taken from an earlier time point, even if half the
     *            maneuver duration before the maneuver time point were before this time point.
     * @param latestManeuverEnd
     *            Maneuver curve end will not be after this time point; if a maneuver is found whose time point is at or
     *            before this time point, no matter how close it is, its end regarding speed and course out of the
     *            maneuver and the leg after the maneuver is not taken from a later time point, even if half the
     *            maneuver duration after the maneuver time point were after this time point.
     * @return The derived list maneuver spots with corresponding maneuvers. The maneuver spot count {@code >= 0}.
     */
    private CompleteManeuverCurve createCompleteManeuverCurveFromFixesGroup(List<GPSFixMoving> douglasPeuckerFixesGroup,
            NauticalSide maneuverDirection, TimePoint earliestManeuverStart, TimePoint latestManeuverEnd) {
        long durationForDouglasPeuckerExtensionForMainCurveAnalysisInMillis = getDurationForDouglasPeuckerExtensionForMainCurveAnalysis(
                getApproximateManeuverDuration()).asMillis();
        TimePoint earliestTimePointBeforeManeuver = Collections
                .max(Arrays.asList(
                        new MillisecondsTimePoint(douglasPeuckerFixesGroup.get(0).getTimePoint().asMillis()
                                - durationForDouglasPeuckerExtensionForMainCurveAnalysisInMillis),
                        earliestManeuverStart));
        TimePoint latestTimePointAfterManeuver = Collections
                .min(Arrays.asList(
                        new MillisecondsTimePoint(
                                douglasPeuckerFixesGroup.get(douglasPeuckerFixesGroup.size() - 1).getTimePoint()
                                        .asMillis() + durationForDouglasPeuckerExtensionForMainCurveAnalysisInMillis),
                        latestManeuverEnd));
        ManeuverMainCurveDetailsWithBearingSteps maneuverMainCurveDetails = computeManeuverMainCurveDetails(
                earliestTimePointBeforeManeuver, latestTimePointAfterManeuver, maneuverDirection);
        if (maneuverMainCurveDetails == null) {
            return null;
        }
        ManeuverCurveBoundaries maneuverUnstableCourseAndSpeedBoundaries = computeManeuverUnstableCourseAndSpeedBoundaries(
                maneuverMainCurveDetails, earliestManeuverStart, latestManeuverEnd);
        MarkPassing markPassing = getMarkPassingIfPresent(maneuverMainCurveDetails);
        CompleteManeuverCurve maneuverCurve = new CompleteManeuverCurveImpl(maneuverMainCurveDetails,
                maneuverUnstableCourseAndSpeedBoundaries, markPassing);
        return maneuverCurve;
    }

    /**
     * Gets the mark passing performed within the provided maneuver curve boundaries. {@code null} is returned when no
     * mark passing is present.
     */
    private MarkPassing getMarkPassingIfPresent(ManeuverCurveBoundaries maneuverCurveBoundaries) {
        // the TrackedLegOfCompetitor variables may be null, e.g., in case the time points are before or after the race
        TrackedLegOfCompetitor legBeforeManeuver = trackedRace.getTrackedLeg(competitor,
                maneuverCurveBoundaries.getTimePointBefore());
        TrackedLegOfCompetitor legAfterManeuver = trackedRace.getTrackedLeg(competitor,
                maneuverCurveBoundaries.getTimePointAfter());
        MarkPassing markPassing = null; // will remain null if no mark passing has been recorded within maneuver
                                        // boundaries
        // check whether a waypoint has been passed within maneuver
        if (legBeforeManeuver != legAfterManeuver
                // a maneuver at the start line is not to be considered a MARK_PASSING maneuver; show a tack as a tack
                && legAfterManeuver != null
                && legAfterManeuver.getLeg().getFrom() != trackedRace.getRace().getCourse().getFirstWaypoint()) {
            Waypoint waypointPassed = legAfterManeuver.getLeg().getFrom();
            markPassing = trackedRace.getMarkPassing(competitor, waypointPassed);
        }
        return markPassing;
    }

    /**
     * Derives maneuvers from the provided maneuver main curve and maneuver curve with stable speed and course before
     * and after. This method gets called recursively if the splitting of main curve and maneuver curve boundaries is
     * performed in order to extract multiple maneuvers for one complete maneuver curve.
     */
    protected List<Maneuver> determineManeuversFromManeuverCurve(
            ManeuverMainCurveDetailsWithBearingSteps maneuverMainCurveDetails,
            ManeuverCurveBoundaries maneuverUnstableCourseAndSpeedBoundaries, Wind wind, MarkPassing markPassing) {
        boolean maneuversAlreadyAdded = false;
        int numberOfJibes = getNumberOfJibes(maneuverMainCurveDetails, wind);
        int numberOfTacks = getNumberOfTacks(maneuverMainCurveDetails, wind);
        List<Maneuver> maneuvers = new ArrayList<>();
        if (numberOfTacks > 0 && numberOfJibes > 0) {
            if (markPassing != null && markPassing.getTimePoint().after(maneuverMainCurveDetails.getTimePointBefore())
                    && markPassing.getTimePoint().before(maneuverMainCurveDetails.getTimePointAfter())) {
                // In case of a mark passing we need to split the maneuver analysis into the phase before and after
                // the mark passing to catch kiwi drops. First of all, this is important to identify the correct
                // maneuver time point for
                // each tack and jibe, second it is essential to call a penalty which is only the case if the tack and
                // the jibe are on the same side of the mark passing; otherwise this may have been a
                // kiwi drop.
                // Therefore, we recursively detect the maneuvers for the segment before and the segment after the
                // mark passing and add the results to our result.
                Pair<ManeuverMainCurveDetailsWithBearingSteps, ManeuverMainCurveDetailsWithBearingSteps> mainCurves = splitManeuverMainCurveByTimePoint(
                        maneuverMainCurveDetails, markPassing.getTimePoint());
                if (mainCurves.getA() != null && mainCurves.getB() != null) {
                    int numberOfJibesBeforeMarkPassing = getNumberOfJibes(mainCurves.getA(), wind);
                    int numberOfTacksBeforeMarkPassing = getNumberOfTacks(mainCurves.getA(), wind);
                    int numberOfJibesAfterMarkPassing = getNumberOfJibes(mainCurves.getB(), wind);
                    int numberOfTacksAfterMarkPassing = getNumberOfTacks(mainCurves.getB(), wind);
                    if (numberOfJibesBeforeMarkPassing + numberOfTacksBeforeMarkPassing > 0
                            && numberOfJibesAfterMarkPassing + numberOfTacksAfterMarkPassing > 0) {
                        Pair<ManeuverCurveBoundaries, ManeuverCurveBoundaries> maneuverUnstableCourseAndSpeedBoundariesPair = splitManeuverCurveWithStableSpeedAndCourseByTimePoint(
                                maneuverUnstableCourseAndSpeedBoundaries, mainCurves.getA(), mainCurves.getB(),
                                markPassing.getTimePoint());
                        if (maneuverUnstableCourseAndSpeedBoundariesPair != null) {
                            maneuversAlreadyAdded = true;
                            maneuvers.addAll(determineManeuversFromManeuverCurve(mainCurves.getA(),
                                    maneuverUnstableCourseAndSpeedBoundariesPair.getA(), wind, markPassing));
                            maneuvers.addAll(determineManeuversFromManeuverCurve(mainCurves.getB(),
                                    maneuverUnstableCourseAndSpeedBoundariesPair.getB(), wind, markPassing));
                        }
                    }
                }
            }
            if (!maneuversAlreadyAdded) {
                // Either there was no mark passing, or the mark passing was not accompanied by a tack or a jibe.
                // For the first tack/jibe combination (they must alternate because the course changes in the same
                // direction
                // and
                // the wind is considered sufficiently stable to not allow for two successive tacks or two successive
                // jibes)
                // we create a PENALTY_CIRCLE maneuver and recurse for the time interval after the first penalty circle
                // has
                // completed.
                if (numberOfTacks > 1 || numberOfJibes > 1) {
                    TimePoint firstPenaltyCircleCompletedAt = getTimePointOfCompletionOfFirstPenaltyCircle(
                            maneuverMainCurveDetails.getTimePointBefore(),
                            maneuverMainCurveDetails.getSpeedWithBearingBefore().getBearing(),
                            maneuverMainCurveDetails.getSpeedWithBearingSteps(), wind);
                    if (firstPenaltyCircleCompletedAt == null) {
                        // This should really not happen!
                        logger.warning(
                                "Maneuver detection has failed to process penalty circle maneuver correctly, because getTimePointOfCompletionOfFirstPenaltyCircle() returned null. Race-Id: "
                                        + trackedRace.getRace().getId() + ", Competitor: " + competitor.getName()
                                        + ", Time point before maneuver: "
                                        + maneuverUnstableCourseAndSpeedBoundaries.getTimePointBefore());
                    } else {
                        Pair<ManeuverMainCurveDetailsWithBearingSteps, ManeuverMainCurveDetailsWithBearingSteps> mainCurves = splitManeuverMainCurveByTimePoint(
                                maneuverMainCurveDetails, firstPenaltyCircleCompletedAt);
                        if (mainCurves.getA() != null && mainCurves.getB() != null) {
                            maneuversAlreadyAdded = true;
                            Pair<ManeuverCurveBoundaries, ManeuverCurveBoundaries> maneuverUnstableCourseAndSpeedBoundariesPair = splitManeuverCurveWithStableSpeedAndCourseByTimePoint(
                                    maneuverUnstableCourseAndSpeedBoundaries, mainCurves.getA(), mainCurves.getB(),
                                    firstPenaltyCircleCompletedAt);
                            maneuvers.add(createManeuverFromManeuverCurveAndWind(mainCurves.getA(),
                                    maneuverUnstableCourseAndSpeedBoundariesPair.getA(), wind, markPassing));
                            // after we've "consumed" one tack and one jibe, recursively find more maneuvers if tacks
                            // and/or jibes remain
                            maneuvers.addAll(determineManeuversFromManeuverCurve(mainCurves.getB(),
                                    maneuverUnstableCourseAndSpeedBoundariesPair.getB(), wind, markPassing));
                        }
                    }
                }
            }
        }
        if (!maneuversAlreadyAdded) {
            maneuvers.add(createManeuverFromManeuverCurveAndWind(maneuverMainCurveDetails,
                    maneuverUnstableCourseAndSpeedBoundaries, wind, markPassing));
        }
        return maneuvers;
    }

    private Pair<ManeuverCurveBoundaries, ManeuverCurveBoundaries> splitManeuverCurveWithStableSpeedAndCourseByTimePoint(
            ManeuverCurveBoundaries maneuverUnstableCourseAndSpeedBoundaries,
            ManeuverMainCurveDetailsWithBearingSteps firstManeuverMainCurveHalf,
            ManeuverMainCurveDetailsWithBearingSteps lastManeuverMainCurveHalf, TimePoint timePoint) {
        SpeedWithBearingStepsIterable speedWithBearingSteps = getSpeedWithBearingSteps(
                maneuverUnstableCourseAndSpeedBoundaries.getTimePointBefore(),
                maneuverUnstableCourseAndSpeedBoundaries.getTimePointAfter());
        Pair<SpeedWithBearingStepsIterable, SpeedWithBearingStepsIterable> splitSteps = splitSpeedWithBearingStepsByTimePoint(
                speedWithBearingSteps, timePoint);
        double courseChangeInDegreesBefore = 0;
        Speed lowestSpeedBefore = null;
        Speed highestSpeedBefore = null;
        SpeedWithBearingStep lastStepBefore = null;
        for (SpeedWithBearingStep step : splitSteps.getA()) {
            courseChangeInDegreesBefore += step.getCourseChangeInDegrees();
            if (lastStepBefore == null) {
                lowestSpeedBefore = step.getSpeedWithBearing();
                highestSpeedBefore = lowestSpeedBefore;
            }
            if (lowestSpeedBefore.compareTo(step.getSpeedWithBearing()) > 0) {
                lowestSpeedBefore = step.getSpeedWithBearing();
            }
            if (highestSpeedBefore.compareTo(step.getSpeedWithBearing()) < 0) {
                highestSpeedBefore = step.getSpeedWithBearing();
            }
            lastStepBefore = step;
        }
        double courseChangeInDegreesAfter = 0;
        Speed lowestSpeedAfter = null;
        Speed highestSpeedAfter = null;
        SpeedWithBearingStep firstStepAfter = null;
        if (splitSteps != null) {
            for (SpeedWithBearingStep step : splitSteps.getB()) {
                if (firstStepAfter == null) {
                    firstStepAfter = step;
                    lowestSpeedAfter = step.getSpeedWithBearing();
                    highestSpeedAfter = lowestSpeedAfter;
                }
                courseChangeInDegreesAfter += step.getCourseChangeInDegrees();
                if (lowestSpeedAfter.compareTo(step.getSpeedWithBearing()) > 0) {
                    lowestSpeedAfter = step.getSpeedWithBearing();
                }
                if (highestSpeedAfter.compareTo(step.getSpeedWithBearing()) < 0) {
                    highestSpeedAfter = step.getSpeedWithBearing();
                }
            }
        }
        if (lastStepBefore == null || firstStepAfter == null) {
            return null;
        }
        ManeuverCurveBoundaries firstManeuverDetails = new ManeuverCurveBoundariesImpl(
                maneuverUnstableCourseAndSpeedBoundaries.getTimePointBefore(), lastStepBefore.getTimePoint(),
                maneuverUnstableCourseAndSpeedBoundaries.getSpeedWithBearingBefore(),
                lastStepBefore.getSpeedWithBearing(), courseChangeInDegreesBefore, lowestSpeedBefore,
                highestSpeedBefore);
        ManeuverCurveBoundaries lastManeuverDetails = new ManeuverCurveBoundariesImpl(firstStepAfter.getTimePoint(),
                maneuverUnstableCourseAndSpeedBoundaries.getTimePointAfter(), firstStepAfter.getSpeedWithBearing(),
                maneuverUnstableCourseAndSpeedBoundaries.getSpeedWithBearingAfter(), courseChangeInDegreesAfter,
                lowestSpeedAfter, highestSpeedAfter);
        return new Pair<>(firstManeuverDetails, lastManeuverDetails);
    }

    private Pair<ManeuverMainCurveDetailsWithBearingSteps, ManeuverMainCurveDetailsWithBearingSteps> splitManeuverMainCurveByTimePoint(
            ManeuverMainCurveDetailsWithBearingSteps maneuverMainCurveDetails, TimePoint timePoint) {
        Pair<SpeedWithBearingStepsIterable, SpeedWithBearingStepsIterable> splitSteps = splitSpeedWithBearingStepsByTimePoint(
                maneuverMainCurveDetails.getSpeedWithBearingSteps(), timePoint);
        NauticalSide maneuverDirection = getDirectionOfCourseChange(
                maneuverMainCurveDetails.getDirectionChangeInDegrees());
        SpeedWithBearingStepsIterable stepsBeforeIterable = splitSteps.getA();
        ManeuverMainCurveDetailsWithBearingSteps mainCurveBefore = stepsBeforeIterable == null ? null
                : computeManeuverMainCurve(stepsBeforeIterable, maneuverDirection);
        SpeedWithBearingStepsIterable stepsAfterIterable = splitSteps.getB();
        ManeuverMainCurveDetailsWithBearingSteps mainCurveAfter = stepsAfterIterable == null ? null
                : computeManeuverMainCurve(stepsAfterIterable, maneuverDirection);
        return new Pair<ManeuverMainCurveDetailsWithBearingSteps, ManeuverMainCurveDetailsWithBearingSteps>(
                mainCurveBefore, mainCurveAfter);
    }

    private Pair<SpeedWithBearingStepsIterable, SpeedWithBearingStepsIterable> splitSpeedWithBearingStepsByTimePoint(
            SpeedWithBearingStepsIterable speedWithBearingSteps, TimePoint timePoint) {
        List<SpeedWithBearingStep> stepsBefore = new ArrayList<>();
        List<SpeedWithBearingStep> stepsAfter = new ArrayList<>();
        SpeedWithBearingStep lastEntry = null;
        for (SpeedWithBearingStep entry : speedWithBearingSteps) {
            if (!entry.getTimePoint().after(timePoint)) {
                stepsBefore.add(entry);
            }
            if (!entry.getTimePoint().before(timePoint)) {
                if (stepsAfter.isEmpty()) {
                    // First step supposed to have 0 as course change as it does not have any previous steps to compute
                    // bearing difference. If the condition is not met, the existing code which uses
                    // SpeedWithBearingStepsIterable class will break.
                    if (lastEntry != null && lastEntry.getTimePoint().before(timePoint)) {
                        // If there is not any step located at the splitting time point, we need to retrieve the
                        // interpolated speed with bearing at time point in order to produce boundary step for both step
                        // sets at time point. If we will not do it, the course change between the steps split by
                        // splitting time point will be lost.
                        SpeedWithBearing speedWithBearing = track.getEstimatedSpeed(timePoint);
                        if (speedWithBearing != null) {
                            double courseChangeAngleInDegrees = lastEntry.getSpeedWithBearing().getBearing()
                                    .getDifferenceTo(speedWithBearing.getBearing(),
                                            new DegreeBearingImpl(lastEntry.getCourseChangeInDegrees()))
                                    .getDegrees();
                            double turningRateInDegreesPerSecond = Math.abs(
                                    courseChangeAngleInDegrees / lastEntry.getTimePoint().until(timePoint).asSeconds());
                            SpeedWithBearingStep lastStepBefore = new SpeedWithBearingStepImpl(timePoint,
                                    speedWithBearing, courseChangeAngleInDegrees, turningRateInDegreesPerSecond);
                            stepsBefore.add(lastStepBefore);
                            SpeedWithBearingStep firstStepAfter = new SpeedWithBearingStepImpl(timePoint,
                                    speedWithBearing, 0.0, 0.0);
                            stepsAfter.add(firstStepAfter);
                            courseChangeAngleInDegrees = firstStepAfter.getSpeedWithBearing().getBearing()
                                    .getDifferenceTo(speedWithBearing.getBearing(),
                                            new DegreeBearingImpl(firstStepAfter.getCourseChangeInDegrees()))
                                    .getDegrees();
                            turningRateInDegreesPerSecond = Math.abs(
                                    courseChangeAngleInDegrees / timePoint.until(entry.getTimePoint()).asSeconds());
                            entry = new SpeedWithBearingStepImpl(entry.getTimePoint(), entry.getSpeedWithBearing(),
                                    courseChangeAngleInDegrees, turningRateInDegreesPerSecond);
                        }

                    }
                    if (stepsAfter.isEmpty()) {
                        entry = new SpeedWithBearingStepImpl(entry.getTimePoint(), entry.getSpeedWithBearing(), 0.0,
                                0.0);
                    }
                }
                stepsAfter.add(entry);
            }
        }
        return new Pair<>(stepsBefore.isEmpty() ? null : new SpeedWithBearingStepsIterable(stepsBefore),
                stepsAfter.isEmpty() ? null : new SpeedWithBearingStepsIterable(stepsAfter));
    }

    /**
     * Creates maneuvers from the provided {@code group} of douglas peucker fixes considering the provided time range
     * limit. This method might return zero or more maneuvers. The maneuvers are determined by the following work-flow:
     * <ol>
     * <li>Main curve of maneuver within the time range of douglas peucker fixes +-
     * ({@link BoatClass#getApproximateManeuverDuration() maneuver duration}{@code  / 2}) is determined. The main curve
     * is defined as the section of maneuver with the highest absolute course change towards the provided
     * {@code maneuverDirection} ({@link Maneuver more info}).</li>
     * <li>Maneuver start and end with stable speed and course are determined by analysis of speed maxima starting
     * before and after the main curve, followed by extension to the points with stable course
     * ({@link #computeManeuverDetails(Competitor, ManeuverMainCurveDetailsWithBearingSteps, TimePoint, TimePoint) more
     * info})</li>
     * <li>The maneuver type is determined considering the maneuver's main curve, marks and wind direction</li>
     * </ol>
     * 
     * @param douglasPeuckerFixesGroup
     *            The douglas peucker fixes which may represent the maneuver basis
     * @param maneuverDirection
     *            The course change direction within douglas peucker fixes.
     * @param earliestManeuverStart
     *            Maneuver start will not be before this time point; if a maneuver is found whose time point is at or
     *            after this time point, no matter how close it is, its start regarding speed and course into the
     *            maneuver and the leg before the maneuver is not taken from an earlier time point, even if half the
     *            maneuver duration before the maneuver time point were before this time point.
     * @param latestManeuverEnd
     *            Maneuver end will not be after this time point; if a maneuver is found whose time point is at or
     *            before this time point, no matter how close it is, its end regarding speed and course out of the
     *            maneuver and the leg after the maneuver is not taken from a later time point, even if half the
     *            maneuver duration after the maneuver time point were after this time point.
     * @return The derived list maneuver spots with corresponding maneuvers. The maneuver spot count {@code >= 0}.
     * @throws NoWindException
     *             When no wind information during maneuver performance is available
     * @see #groupChangesInSameDirectionIntoManeuvers(Competitor, List, boolean, TimePoint, TimePoint)
     * @see #computeManeuverDetails(Competitor, ManeuverMainCurveDetailsWithBearingSteps, TimePoint, TimePoint)
     */
    private Maneuver createManeuverFromManeuverCurveAndWind(
            ManeuverMainCurveDetailsWithBearingSteps maneuverMainCurveDetails,
            ManeuverCurveBoundaries maneuverUnstableCourseAndSpeedBoundaries, Wind wind, MarkPassing markPassing) {
        final Maneuver maneuver;
        final ManeuverType maneuverType;
        Tack tackAfterManeuver = null;
        ManeuverLoss maneuverLoss = null;
        final Position maneuverPosition = track.getEstimatedPosition(maneuverMainCurveDetails.getTimePoint(),
                /* extrapolate */false);
        int numberOfJibes = getNumberOfJibes(maneuverMainCurveDetails, wind);
        int numberOfTacks = getNumberOfTacks(maneuverMainCurveDetails, wind);
        if (numberOfTacks > 0 && numberOfJibes > 0) {
            maneuverType = ManeuverType.PENALTY_CIRCLE;
        } else if (numberOfTacks > 0 || numberOfJibes > 0) {
            maneuverType = numberOfTacks > 0 ? ManeuverType.TACK : ManeuverType.JIBE;
        } else if (wind != null) {
            // heading up or bearing away
            Bearing windBearing = wind.getBearing();
            Bearing toWindBeforeManeuver = windBearing
                    .getDifferenceTo(maneuverMainCurveDetails.getSpeedWithBearingBefore().getBearing());
            Bearing toWindAfterManeuver = windBearing
                    .getDifferenceTo(maneuverMainCurveDetails.getSpeedWithBearingAfter().getBearing());
            maneuverType = Math.abs(toWindBeforeManeuver.getDegrees()) < Math.abs(toWindAfterManeuver.getDegrees())
                    ? ManeuverType.HEAD_UP
                    : ManeuverType.BEAR_AWAY;
        } else {
            // no wind information; marking as UNKNOWN
            maneuverType = ManeuverType.UNKNOWN;
        }

        if (numberOfTacks + numberOfJibes > 0 || wind == null) {
            if (wind != null) {
                try {
                    Position positionForNewTack = track
                            .getEstimatedPosition(maneuverUnstableCourseAndSpeedBoundaries.getTimePointAfter(), false);
                    if (positionForNewTack != null) {
                        tackAfterManeuver = trackedRace.getTack(positionForNewTack,
                                maneuverUnstableCourseAndSpeedBoundaries.getTimePointAfter(),
                                maneuverUnstableCourseAndSpeedBoundaries.getSpeedWithBearingAfter().getBearing());
                    }
                } catch (NoWindException e) {
                    tackAfterManeuver = null;
                }
            } else {
                tackAfterManeuver = null;
            }
            maneuverLoss = getManeuverLoss(maneuverUnstableCourseAndSpeedBoundaries);
            maneuver = new ManeuverWithStableSpeedAndCourseBoundariesImpl(maneuverType, tackAfterManeuver,
                    maneuverPosition, maneuverMainCurveDetails.getTimePoint(),
                    maneuverMainCurveDetails.extractCurveBoundariesOnly(), maneuverUnstableCourseAndSpeedBoundaries,
                    maneuverMainCurveDetails.getMaxTurningRateInDegreesPerSecond(), markPassing, maneuverLoss);
        } else {
            // Logic for head-up and bear-away
            try {
                Position positionForNewTack = track.getEstimatedPosition(maneuverMainCurveDetails.getTimePointAfter(),
                        false);
                if (positionForNewTack != null) {
                    tackAfterManeuver = trackedRace.getTack(positionForNewTack,
                            maneuverMainCurveDetails.getTimePointAfter(),
                            maneuverMainCurveDetails.getSpeedWithBearingAfter().getBearing());
                }
            } catch (NoWindException e) {
                tackAfterManeuver = null;
            }
            maneuver = new ManeuverWithMainCurveBoundariesImpl(maneuverType, tackAfterManeuver, maneuverPosition,
                    maneuverMainCurveDetails.getTimePoint(), maneuverMainCurveDetails.extractCurveBoundariesOnly(),
                    maneuverUnstableCourseAndSpeedBoundaries,
                    maneuverMainCurveDetails.getMaxTurningRateInDegreesPerSecond(), markPassing, maneuverLoss);
        }
        return maneuver;
    }

    /**
     * Computes the maneuver loss as the distance projected onto the average course between entering and exiting the
     * maneuver that the boat lost compared to not having maneuvered. With this distance measure, the competitors speed
     * and bearing before the maneuver, as defined by <code>maneuverBoundaries.timePointBefore()</code> is extrapolated
     * until <code>maneuverBoundaries.timePointAfter()</code>, and the resulting extrapolated position's "windward
     * distance" is compared to the competitor's actual position at that time. This distance is returned as the result
     * of this method.
     */
    protected ManeuverLoss getManeuverLoss(ManeuverCurveBoundaries maneuverBoundaries) {
        final GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
        SpeedWithBearing speedWithBearingWhenSpeedStartedToDrop = maneuverBoundaries.getSpeedWithBearingBefore();
        SpeedWithBearing speedWithBearingAfterManeuver = maneuverBoundaries.getSpeedWithBearingAfter();
        TimePoint timePointWhenSpeedStartedToDrop = maneuverBoundaries.getTimePointBefore();
        TimePoint timePointWhenSpeedLevelledOffAfterManeuver = maneuverBoundaries.getTimePointAfter();
        Duration maneuverDuration = timePointWhenSpeedStartedToDrop.until(timePointWhenSpeedLevelledOffAfterManeuver);
        // For upwind/downwind legs, find the mean course between inbound and outbound course and project actual
        // and
        // extrapolated positions onto it:
        Bearing middleManeuverAngle = speedWithBearingWhenSpeedStartedToDrop.getBearing()
                .middle(speedWithBearingAfterManeuver.getBearing());
        // extrapolate maximum speed before maneuver to time point of maximum speed after maneuver and project
        // resulting position
        // onto the average maneuver course; compare to the projected position actually reached at the time
        // point of maximum speed after
        // maneuver:
        Position positionWhenSpeedStartedToDrop = track.getEstimatedPosition(timePointWhenSpeedStartedToDrop,
                /* extrapolate */ false);
        Position extrapolatedPositionAtTimePointOfMaxSpeedAfterManeuver = speedWithBearingWhenSpeedStartedToDrop
                .travelTo(positionWhenSpeedStartedToDrop, timePointWhenSpeedStartedToDrop,
                        timePointWhenSpeedLevelledOffAfterManeuver);
        Position actualPositionAtTimePointOfMaxSpeedAfterManeuver = track
                .getEstimatedPosition(timePointWhenSpeedLevelledOffAfterManeuver, /* extrapolate */ false);
        Position projectedExtrapolatedPositionAtTimePointOfMaxSpeedAfterManeuver = extrapolatedPositionAtTimePointOfMaxSpeedAfterManeuver
                .projectToLineThrough(positionWhenSpeedStartedToDrop, middleManeuverAngle);
        Position projectedActualPositionAtTimePointOfMaxSpeedAfterManeuver = actualPositionAtTimePointOfMaxSpeedAfterManeuver
                .projectToLineThrough(positionWhenSpeedStartedToDrop, middleManeuverAngle);
        Distance projectedDistanceSailed = positionWhenSpeedStartedToDrop
                .getDistance(projectedActualPositionAtTimePointOfMaxSpeedAfterManeuver);
        Distance projectedDistanceSailedIfNotManeuvering = positionWhenSpeedStartedToDrop
                .getDistance(projectedExtrapolatedPositionAtTimePointOfMaxSpeedAfterManeuver);
        return new ManeuverLoss(projectedDistanceSailed, projectedDistanceSailedIfNotManeuvering,
                positionWhenSpeedStartedToDrop, actualPositionAtTimePointOfMaxSpeedAfterManeuver, maneuverDuration,
                speedWithBearingWhenSpeedStartedToDrop, middleManeuverAngle);
    }

    protected Duration getDurationForDouglasPeuckerExtensionForMainCurveAnalysis(Duration approximateManeuverDuration) {
        return approximateManeuverDuration.divide(2);
    }

    /**
     * Starting at <code>timePointBeforeManeuver</code>, and assuming that the group of
     * <code>approximatedFixesAndCourseChanges</code> contains at least a tack and a jibe, finds the approximated fix's
     * time point at which one tack and one jibe have been completed and for which the total course change is as close
     * as possible to 360 degrees.
     */
    private TimePoint getTimePointOfCompletionOfFirstPenaltyCircle(TimePoint timePointBeforeManeuver,
            Bearing courseBeforeManeuver, SpeedWithBearingStepsIterable maneuverBearingSteps, Wind wind) {
        double totalCourseChangeInDegrees = 0;
        double bestTotalCourseChangeInDegrees = 0; // this should be as close as possible to 360 degrees after one tack
                                                   // and one jibe
        BearingChangeAnalyzer bearingChangeAnalyzer = BearingChangeAnalyzer.INSTANCE;
        Bearing newCourse = courseBeforeManeuver;
        TimePoint result = null;
        boolean firstEntry = true;
        for (SpeedWithBearingStep fixAndCourseChange : maneuverBearingSteps) {
            if (firstEntry) {
                firstEntry = false;
                continue;
            }
            totalCourseChangeInDegrees += fixAndCourseChange.getCourseChangeInDegrees();
            newCourse = newCourse.add(new DegreeBearingImpl(fixAndCourseChange.getCourseChangeInDegrees()));
            int numberOfJibes = bearingChangeAnalyzer.didPass(courseBeforeManeuver, totalCourseChangeInDegrees,
                    newCourse, wind.getBearing());
            int numberOfTacks = bearingChangeAnalyzer.didPass(courseBeforeManeuver, totalCourseChangeInDegrees,
                    newCourse, wind.getFrom());
            if (numberOfJibes > 0 && numberOfTacks > 0) {
                if (numberOfJibes > 1 || numberOfTacks > 1) {
                    // It could be that one or both numbers increased to greater than 1 from 0. In this case
                    // we want to find the point between the completion of the penalty and the next maneuver
                    // which increases one of the counters to 2 and use that time point as the result:
                    if (result == null) {
                        // It could be that both numbers increased, and one was 1 before, so now we have 1 and 2. But
                        // we can't split it up finer than two fixes, so we'll use the time point between the last two
                        // fixes
                        // instead:
                        result = fixAndCourseChange.getTimePoint();
                    }
                    break; // don't continue into a subsequent tack/jibe sailed in conjunction with the penalty or
                           // starting the next circle
                }
                if (Math.abs(360 - Math.abs(totalCourseChangeInDegrees)) < (Math
                        .abs(360 - Math.abs(bestTotalCourseChangeInDegrees)))) {
                    bestTotalCourseChangeInDegrees = totalCourseChangeInDegrees;
                    result = fixAndCourseChange.getTimePoint();
                } else {
                    break; // not getting closer but further away from 360 degrees
                }
            }
        }
        return result;
    }

    /**
     * Computes details of the {@link Maneuver main curve of maneuver}, such as maneuver entering and exiting time point
     * with speed and bearing, time point with the highest turning rate (maneuver climax), total course change, and
     * speed with bearing steps of main curve. The maneuver section with the highest sum of absolute course change
     * angles between bearing steps is defined as the main curve section ({@link Maneuver more info})s.
     * 
     * @param competitor
     *            The competitor whose maneuvers are being determined
     * @param timePointBeforeManeuver
     * @param timePointAfterManeuver
     *            The time range which will be gradually decreased in order to locate the section with the highest
     *            course change towards the target course change direction
     * @param maneuverDirection
     *            The target course change direction for the main curve to determine
     * @return The details of the maneuver main curve
     */
    protected ManeuverMainCurveDetailsWithBearingSteps computeManeuverMainCurveDetails(
            TimePoint timePointBeforeManeuver, TimePoint timePointAfterManeuver, NauticalSide maneuverDirection) {
        SpeedWithBearingStepsIterable stepsToAnalyze = getSpeedWithBearingSteps(timePointBeforeManeuver,
                timePointAfterManeuver);
        ManeuverMainCurveDetailsWithBearingSteps maneuverMainCurveDetails = computeManeuverMainCurve(stepsToAnalyze,
                maneuverDirection);
        return maneuverMainCurveDetails;
    }

    /**
     * Gets a new list with bearing steps which are lying between provided time range (including the boundaries). To get
     * the steps, performance costy call to {@link GPSFixTrack#getSpeedWithBearingSteps(TimePoint, TimePoint, Duration)}
     * is made.
     */
    protected SpeedWithBearingStepsIterable getSpeedWithBearingSteps(TimePoint timePointBeforeManeuver,
            TimePoint timePointAfterManeuver) {
        SpeedWithBearingStepsIterable stepsToAnalyze = track.getSpeedWithBearingSteps(timePointBeforeManeuver,
                timePointAfterManeuver);
        return stepsToAnalyze;
    }

    /**
     * Computes the details of maneuver such as maneuver entering and exiting time point with speed and bearing, time
     * point with the highest turning rate (maneuver climax) and total course change. The provided details of maneuver
     * main curve are used as minimal maneuver section which gets expanded by analyzing the speed and bearing trend
     * regarding stability before and after the main curve of maneuver. The goal is to determine the maneuver entering
     * and exiting time points such that the speed and course values ideally represent stable segments leading into and
     * out of the maneuver. It is assumed that before maneuver the speed starts to slow down. Thus, in order to
     * approximate the beginning time point of the maneuver, the speed maximum is determined throughout forward in time
     * iteration of speed steps starting from time point of main curve beginning. From the determined speed maximum, the
     * iteration continues until the point, when the bearing changes occur only with a maximum of
     * {@value #MAX_TURNING_RATE_IN_DEG_PER_SECOND_FOR_STABLE_COURSE_ANALYSIS} degrees per second, which is regarded as
     * a stable course. The exiting time point of maneuver is approximated analogously by speed maximum determination
     * throughout backward in time iteration of speed steps starting from time of main curve end, followed by a search
     * for a point with stable course.
     * 
     * @param maneuverMainCurveDetails
     *            The details of the main curve, ideally computed by
     *            {@link #computeManeuverMainCurveDetails(Competitor, TimePoint, TimePoint, NauticalSide)}
     * @param earliestManeuverStart
     *            Maneuver start will not be before this time point
     * @param latestManeuverEnd
     *            Maneuver end will not be after this time point
     * @return The details of the maneuver
     */
    private ManeuverCurveBoundaries computeManeuverUnstableCourseAndSpeedBoundaries(
            ManeuverMainCurveDetailsWithBearingSteps maneuverMainCurveDetails, TimePoint earliestManeuverStart,
            TimePoint latestManeuverEnd) {
        ManeuverCurveBoundaryExtension beforeManeuverSectionExtension = expandBeforeManeuverSectionBySpeedAndBearingTrendAnalysis(
                maneuverMainCurveDetails, earliestManeuverStart);
        ManeuverCurveBoundaryExtension afterManeuverSectionExtension = expandAfterManeuverSectionBySpeedAndBearingTrendAnalysis(
                maneuverMainCurveDetails, latestManeuverEnd);
        double totalCourseChangeInDegrees = beforeManeuverSectionExtension.getCourseChangeInDegreesWithinExtensionArea()
                + maneuverMainCurveDetails.getDirectionChangeInDegrees()
                + afterManeuverSectionExtension.getCourseChangeInDegreesWithinExtensionArea();
        Speed lowestSpeed = maneuverMainCurveDetails.getLowestSpeed();
        if (lowestSpeed == null || beforeManeuverSectionExtension.getLowestSpeedWithinExtensionArea() != null
                && lowestSpeed.compareTo(beforeManeuverSectionExtension.getLowestSpeedWithinExtensionArea()) > 0) {
            lowestSpeed = beforeManeuverSectionExtension.getLowestSpeedWithinExtensionArea();
        }
        if (lowestSpeed == null || afterManeuverSectionExtension.getLowestSpeedWithinExtensionArea() != null
                && lowestSpeed.compareTo(afterManeuverSectionExtension.getLowestSpeedWithinExtensionArea()) > 0) {
            lowestSpeed = afterManeuverSectionExtension.getLowestSpeedWithinExtensionArea();
        }
        Speed highestSpeed = maneuverMainCurveDetails.getHighestSpeed();
        if (highestSpeed == null || beforeManeuverSectionExtension.getHighestSpeedWithinExtensionArea() != null
                && highestSpeed.compareTo(beforeManeuverSectionExtension.getHighestSpeedWithinExtensionArea()) < 0) {
            highestSpeed = beforeManeuverSectionExtension.getHighestSpeedWithinExtensionArea();
        }
        if (highestSpeed == null || afterManeuverSectionExtension.getHighestSpeedWithinExtensionArea() != null
                && highestSpeed.compareTo(afterManeuverSectionExtension.getHighestSpeedWithinExtensionArea()) < 0) {
            highestSpeed = afterManeuverSectionExtension.getHighestSpeedWithinExtensionArea();
        }
        return new ManeuverCurveBoundariesImpl(beforeManeuverSectionExtension.getExtensionTimePoint(),
                afterManeuverSectionExtension.getExtensionTimePoint(),
                beforeManeuverSectionExtension.getSpeedWithBearingAtExtensionTimePoint(),
                afterManeuverSectionExtension.getSpeedWithBearingAtExtensionTimePoint(), totalCourseChangeInDegrees,
                lowestSpeed, highestSpeed);
    }

    /**
     * Determines the start of maneuver by analysis of speed and bearing trend starting from the start of provided
     * maneuver main curve. Firstly, speed maximum is located by iterating through the speed with bearings steps
     * backward in time, starting from the time point of main curve start {@code t}. In interval {@code [t -}
     * {@link BoatClass#getApproximateManeuverDurationInMilliseconds() approx. maneuver duration} {@code / 8; t]} global
     * speed maximum is considered, whereas in interval {@code [t -}
     * {@link BoatClass#getApproximateManeuverDurationInMilliseconds() approx. maneuver duration} {@code ; t - }
     * {@link BoatClass#getApproximateManeuverDurationInMilliseconds() approx. maneuver duration} {@code / 8)} the
     * search continues only if the speed keeps rising. After the time point with speed maximum {@code t'} is
     * determined, the course changes get analyzed starting from {@code t'} until {@code (t -}
     * {@link BoatClass#getApproximateManeuverDurationInMilliseconds() approx. maneuver duration}{@code )} in order to
     * locate the point where the bearing starts to change with a rate of maximal
     * {@value #MAX_TURNING_RATE_IN_DEG_PER_SECOND_FOR_STABLE_COURSE_ANALYSIS} degrees per second, which is regarded as
     * a stable course.
     * 
     * @param maneuverMainCurveDetails
     *            The details of the main curve, ideally computed by
     *            {@link #computeManeuverMainCurveDetails(Competitor, TimePoint, TimePoint, NauticalSide)}
     * @param earliestManeuverStart
     *            Maneuver start will not be before this time point
     * @return The time point and speed at located step with speed maximum, as well as the total course change from the
     *         step iteration started until the step with the speed maximum
     * @see #computeManeuverDetails(Competitor, ManeuverMainCurveDetailsWithBearingSteps, TimePoint, TimePoint)
     */
    private ManeuverCurveBoundaryExtension expandBeforeManeuverSectionBySpeedAndBearingTrendAnalysis(
            ManeuverMainCurveDetailsWithBearingSteps maneuverMainCurveDetails, TimePoint earliestManeuverStart) {
        Duration approximateManeuverDuration = getApproximateManeuverDuration();
        Duration minDurationForSpeedTrendAnalysis = approximateManeuverDuration.divide(8.0);
        Duration maxDurationForSpeedTrendAnalysis = approximateManeuverDuration;
        TimePoint latestTimePointForSpeedTrendAnalysis = maneuverMainCurveDetails.getTimePointBefore();
        TimePoint earliestTimePointForSpeedTrendAnalysis = latestTimePointForSpeedTrendAnalysis
                .minus(maxDurationForSpeedTrendAnalysis);
        if (earliestTimePointForSpeedTrendAnalysis.before(earliestManeuverStart)) {
            earliestTimePointForSpeedTrendAnalysis = earliestManeuverStart;
        }
        TimePoint timePointSinceGlobalMaximumSearch = latestTimePointForSpeedTrendAnalysis
                .minus(minDurationForSpeedTrendAnalysis);
        SpeedWithBearingStepsIterable stepsToAnalyze = getSpeedWithBearingSteps(earliestTimePointForSpeedTrendAnalysis,
                latestTimePointForSpeedTrendAnalysis);
        ManeuverCurveBoundaryExtension maneuverStart = findSpeedMaximum(stepsToAnalyze, true,
                timePointSinceGlobalMaximumSearch);
        if (isCourseChangeLimitExceededForCurveExtension(maneuverMainCurveDetails, maneuverStart)) {
            maneuverStart = null;
        }
        TimePoint stableBearingAnalysisUntil = maneuverStart == null ? maneuverMainCurveDetails.getTimePointBefore()
                : maneuverStart.getExtensionTimePoint();
        stepsToAnalyze = getSpeedWithBearingStepsWithinTimeRange(stepsToAnalyze, earliestTimePointForSpeedTrendAnalysis,
                stableBearingAnalysisUntil);
        ManeuverCurveBoundaryExtension stableBearingExtension = findStableBearingWithMaxAbsCourseChangeSpeed(
                stepsToAnalyze, true, MAX_TURNING_RATE_IN_DEG_PER_SECOND_FOR_STABLE_COURSE_ANALYSIS);
        ManeuverCurveBoundaryExtension mergedExtension = extendManeuverCurveBoundaryExtension(maneuverStart, stableBearingExtension);
        if (!isCourseChangeLimitExceededForCurveExtension(maneuverMainCurveDetails, mergedExtension)) {
            maneuverStart = mergedExtension;
        }
        return maneuverStart != null
                ? maneuverStart
                : new ManeuverCurveBoundaryExtension(maneuverMainCurveDetails.getTimePointBefore(),
                        maneuverMainCurveDetails.getSpeedWithBearingBefore(), 0, null, null);
    }
    
    private ManeuverCurveBoundaryExtension extendManeuverCurveBoundaryExtension(ManeuverCurveBoundaryExtension previousExtension, ManeuverCurveBoundaryExtension newExtension) {
        if (previousExtension == null) {
            return newExtension;
        }
        if (newExtension == null) {
            return previousExtension;
        }
        Speed lowestSpeed = previousExtension.getLowestSpeedWithinExtensionArea().compareTo(newExtension.getLowestSpeedWithinExtensionArea()) > 0 ? newExtension.getLowestSpeedWithinExtensionArea() : previousExtension.getLowestSpeedWithinExtensionArea();
        Speed highestSpeed = previousExtension.getHighestSpeedWithinExtensionArea().compareTo(newExtension.getHighestSpeedWithinExtensionArea()) < 0 ? newExtension.getHighestSpeedWithinExtensionArea() : previousExtension.getHighestSpeedWithinExtensionArea();
        double courseChangeInDegrees = previousExtension.getCourseChangeInDegreesWithinExtensionArea() + newExtension.getCourseChangeInDegreesWithinExtensionArea();
        ManeuverCurveBoundaryExtension mergedExtension = new ManeuverCurveBoundaryExtension(newExtension.getExtensionTimePoint(), newExtension.getSpeedWithBearingAtExtensionTimePoint(), courseChangeInDegrees, lowestSpeed, highestSpeed);
        return mergedExtension;
    }

    private boolean isCourseChangeLimitExceededForCurveExtension(
            ManeuverMainCurveDetailsWithBearingSteps maneuverMainCurveDetails,
            ManeuverCurveBoundaryExtension curveBoundaryExtension) {
        if (curveBoundaryExtension == null) {
            return false;
        }
        return Math.abs(maneuverMainCurveDetails.getDirectionChangeInDegrees()) / 2.0 < Math
                .abs(curveBoundaryExtension.getCourseChangeInDegreesWithinExtensionArea());
    }

    /**
     * Determines the end of maneuver by analysis of speed and bearing trend starting from the end of provided maneuver
     * main curve. Firstly, speed maximum is located by iterating through the speed with bearings steps forward in time,
     * starting from the time point of main curve end {@code t}. In interval {@code [t; t +}
     * {@link BoatClass#getApproximateManeuverDurationInMilliseconds() approx. maneuver duration} {@code ]} global speed
     * maximum is considered, whereas in interval {@code (t +}
     * {@link BoatClass#getApproximateManeuverDurationInMilliseconds() approx. maneuver duration} {@code ; t + }
     * {@link BoatClass#getApproximateManeuverDurationInMilliseconds() approx. maneuver duration} {@code * 3)} the
     * search continues only if the speed keeps rising. After the time point with speed maximum {@code t'} is
     * determined, the course changes get analyzed starting from {@code t'} until {@code (t +}
     * {@link BoatClass#getApproximateManeuverDurationInMilliseconds() approx. maneuver duration} {@code * 3)} in order
     * to locate the point where the bearing starts to change with a rate of maximal
     * {@value #MAX_TURNING_RATE_IN_DEG_PER_SECOND_FOR_STABLE_COURSE_ANALYSIS} degrees per second, which is regarded as
     * a stable course.
     * 
     * @param maneuverMainCurveDetails
     *            The details of the main curve, ideally computed by
     *            {@link #computeManeuverMainCurveDetails(Competitor, TimePoint, TimePoint, NauticalSide)}
     * @param latestManeuverEnd
     *            Maneuver end will not be after this time point
     * @return The time point and speed at located step with speed maximum, as well as the total course change from the
     *         step iteration started until the step with the speed maximum
     * @see #computeManeuverDetails(Competitor, ManeuverMainCurveDetailsWithBearingSteps, TimePoint, TimePoint)
     */
    private ManeuverCurveBoundaryExtension expandAfterManeuverSectionBySpeedAndBearingTrendAnalysis(
            ManeuverMainCurveDetailsWithBearingSteps maneuverMainCurveDetails, TimePoint latestManeuverEnd) {
        Duration approximateManeuverDuration = getApproximateManeuverDuration();
        Duration minDurationForSpeedTrendAnalysis = approximateManeuverDuration;
        Duration maxDurationForSpeedTrendAnalysis = getMaxDurationForAfterManeuverSectionExtension(
                approximateManeuverDuration);
        TimePoint earliestTimePointForSpeedTrendAnalysis = maneuverMainCurveDetails.getTimePointAfter();
        TimePoint latestTimePointForSpeedTrendAnalysis = earliestTimePointForSpeedTrendAnalysis
                .plus(maxDurationForSpeedTrendAnalysis);
        if (latestTimePointForSpeedTrendAnalysis.after(latestManeuverEnd)) {
            latestTimePointForSpeedTrendAnalysis = latestManeuverEnd;
        }
        TimePoint timePointBeforeLocalMaximumSearch = earliestTimePointForSpeedTrendAnalysis
                .plus(minDurationForSpeedTrendAnalysis);
        SpeedWithBearingStepsIterable stepsToAnalyze = getSpeedWithBearingSteps(earliestTimePointForSpeedTrendAnalysis,
                latestTimePointForSpeedTrendAnalysis);
        ManeuverCurveBoundaryExtension maneuverEnd = findSpeedMaximum(stepsToAnalyze, false,
                timePointBeforeLocalMaximumSearch);
        if (isCourseChangeLimitExceededForCurveExtension(maneuverMainCurveDetails, maneuverEnd)) {
            maneuverEnd = null;
        }
        TimePoint stableBearingAnalysisFrom = maneuverEnd == null ? maneuverMainCurveDetails.getTimePointAfter()
                : maneuverEnd.getExtensionTimePoint();
        stepsToAnalyze = getSpeedWithBearingStepsWithinTimeRange(stepsToAnalyze, stableBearingAnalysisFrom,
                latestTimePointForSpeedTrendAnalysis);
        ManeuverCurveBoundaryExtension stableBearingExtension = findStableBearingWithMaxAbsCourseChangeSpeed(
                stepsToAnalyze, false, MAX_TURNING_RATE_IN_DEG_PER_SECOND_FOR_STABLE_COURSE_ANALYSIS);
        ManeuverCurveBoundaryExtension mergedExtension = extendManeuverCurveBoundaryExtension(maneuverEnd, stableBearingExtension);
        if (!isCourseChangeLimitExceededForCurveExtension(maneuverMainCurveDetails, mergedExtension)) {
            maneuverEnd = mergedExtension;
        }
        return maneuverEnd != null
                ? maneuverEnd
                : new ManeuverCurveBoundaryExtension(maneuverMainCurveDetails.getTimePointAfter(),
                        maneuverMainCurveDetails.getSpeedWithBearingAfter(), 0, null, null);
    }

    protected Duration getMaxDurationForAfterManeuverSectionExtension(Duration approximateManeuverDuration) {
        return approximateManeuverDuration.times(3.0);
    }

    /**
     * Finds speed maximum considering the provided {@code stepsToAnalyze}. In order to limit the time range for the
     * speed maximum search, the caller must cut off the appropriate steps from the provided {@code stepsToAnalyze}.
     * Additionally, the method supports specification of {@code globalMaximumSearchUntilTimePoint} which defines the
     * time point since which the search is supposed to continue only if the speed continues to rise.
     * 
     * @param stepsToAnalyze
     *            Steps which are used for speed maximum search. Must be in chronological order (forward in time).
     * @param timeBackwardSearch
     *            {@code true} if the search should be performed backwards in time, {@code false} for forward in time.
     *            When the search is performed backwards in time, then the provided {@code stepsToAnalyze} are going to
     *            be iterated in the reverse order.
     * @param globalMaximumSearchUntilTimePoint
     *            The time point after which the search iteration is going to continue only if the speed continues to
     *            rise. {@code null} will deactivate this feature.
     * @return The time point and speed at located step with speed maximum, as well as the total course change from the
     *         step iteration started until the step with the speed maximum
     */
    public ManeuverCurveBoundaryExtension findSpeedMaximum(SpeedWithBearingStepsIterable stepsToAnalyze,
            boolean timeBackwardSearch, TimePoint globalMaximumSearchUntilTimePoint) {
        final Iterable<SpeedWithBearingStep> finalStepsToAnalyze;
        final Predicate<SpeedWithBearingStep> localMaximumSearch;
        if (timeBackwardSearch) {
            // reverse the steps to iterate through
            finalStepsToAnalyze = cloneAndReverseIterable(stepsToAnalyze);
            localMaximumSearch = step -> globalMaximumSearchUntilTimePoint == null ? false
                    : step.getTimePoint().before(globalMaximumSearchUntilTimePoint);
        } else {
            finalStepsToAnalyze = stepsToAnalyze;
            localMaximumSearch = step -> globalMaximumSearchUntilTimePoint == null ? false
                    : step.getTimePoint().after(globalMaximumSearchUntilTimePoint);
        }
        double previousSpeedInKnots = 0;
        double maxSpeedInKnots = 0;
        SpeedWithBearingStep stepWithMaxSpeed = null;
        double courseChangeSinceMainCurveBeforeSpeedMaximumInDegrees = 0;
        double courseChangeAfterStepWithSpeedMaximum = 0;
        Speed lowestSpeed = null;
        Speed highestSpeed = null;
        for (SpeedWithBearingStep speedWithBearingStep : finalStepsToAnalyze) {
            courseChangeAfterStepWithSpeedMaximum += speedWithBearingStep.getCourseChangeInDegrees();
            double speedInKnots = speedWithBearingStep.getSpeedWithBearing().getKnots();
            if (localMaximumSearch.test(speedWithBearingStep) && previousSpeedInKnots > speedInKnots) {
                // We are in the interval where the search for speed maximum is supposed to be only continued, if the
                // speed continues to grow. The speed starts to drop => abort further search
                break;
            } else {
                // Otherwise find the step with the highest speed
                if (maxSpeedInKnots < speedInKnots) {
                    maxSpeedInKnots = speedInKnots;
                    stepWithMaxSpeed = speedWithBearingStep;
                    courseChangeSinceMainCurveBeforeSpeedMaximumInDegrees += courseChangeAfterStepWithSpeedMaximum;
                    courseChangeAfterStepWithSpeedMaximum = 0;
                }
                if (lowestSpeed == null || lowestSpeed.compareTo(speedWithBearingStep.getSpeedWithBearing()) > 0) {
                    lowestSpeed = speedWithBearingStep.getSpeedWithBearing();
                }
                if (highestSpeed == null || highestSpeed.compareTo(speedWithBearingStep.getSpeedWithBearing()) < 0) {
                    highestSpeed = speedWithBearingStep.getSpeedWithBearing();
                }
            }
            previousSpeedInKnots = speedInKnots;
        }
        // The course change contained in a speed with bearing step references the bearing difference with its preceding
        // step back in time. We need to remove the added course change from the last step in order to not go further
        // time backward.
        if (timeBackwardSearch && stepWithMaxSpeed != null) {
            courseChangeSinceMainCurveBeforeSpeedMaximumInDegrees -= stepWithMaxSpeed.getCourseChangeInDegrees();
        }
        return stepWithMaxSpeed == null ? null
                : new ManeuverCurveBoundaryExtension(stepWithMaxSpeed.getTimePoint(),
                        stepWithMaxSpeed.getSpeedWithBearing(), courseChangeSinceMainCurveBeforeSpeedMaximumInDegrees,
                        lowestSpeed, highestSpeed);
    }

    private Iterable<SpeedWithBearingStep> cloneAndReverseIterable(SpeedWithBearingStepsIterable stepsToAnalyze) {
        ArrayList<SpeedWithBearingStep> tempSteps = new ArrayList<>();
        for (SpeedWithBearingStep step : stepsToAnalyze) {
            tempSteps.add(step);
        }
        Collections.reverse(tempSteps);
        return tempSteps;
    }

    /**
     * Finds a first section within the provided {@code stepsToAnalyze} where the bearing starts to change with a
     * maximal rate of {@code maxCourseChangeInDegreesPerSecond}.
     * 
     * @param stepsToAnalyze
     *            Steps which are used for stable bearing search. Must be in chronological order (forward in time).
     * @param timeBackwardSearch
     *            {@code true} if the search should be performed backward in time, {@code false} for forward in time.
     *            When the search is performed backward in time, then the provided {@code stepsToAnalyze} are going to
     *            be iterated in the reverse order.
     * @param maxCourseChangeInDegreesPerSecond
     *            Defines the course change rate which is regarded as a stable course
     * @return The time point and speed at located step with the first stable course, as well as the total course change
     *         from the step iteration started until the located step
     */
    public ManeuverCurveBoundaryExtension findStableBearingWithMaxAbsCourseChangeSpeed(
            SpeedWithBearingStepsIterable stepsToAnalyze, boolean timeBackwardSearch,
            double maxCourseChangeInDegreesPerSecond) {
        final Iterable<SpeedWithBearingStep> finalStepsToAnalyze;
        if (timeBackwardSearch) {
            finalStepsToAnalyze = cloneAndReverseIterable(stepsToAnalyze);
        } else {
            finalStepsToAnalyze = stepsToAnalyze;
        }
        SpeedWithBearingStep previousStep = null;
        SpeedWithBearingStep stepUntilStableBearing = null;
        double courseChangeUntilStepWithStableBearingInDegrees = 0;
        Speed lowestSpeed = null;
        Speed highestSpeed = null;
        for (SpeedWithBearingStep currentStep : finalStepsToAnalyze) {
            if (previousStep != null) {
                double courseChangePerSecondInDegrees = Math.abs(currentStep.getCourseChangeInDegrees()
                        / previousStep.getTimePoint().until(currentStep.getTimePoint()).asSeconds());
                if (courseChangePerSecondInDegrees <= maxCourseChangeInDegreesPerSecond) {
                    stepUntilStableBearing = timeBackwardSearch ? currentStep : previousStep;
                    break;
                }
            }
            if (lowestSpeed == null || lowestSpeed.compareTo(currentStep.getSpeedWithBearing()) > 0) {
                lowestSpeed = currentStep.getSpeedWithBearing();
            }
            if (highestSpeed == null || highestSpeed.compareTo(currentStep.getSpeedWithBearing()) < 0) {
                highestSpeed = currentStep.getSpeedWithBearing();
            }
            courseChangeUntilStepWithStableBearingInDegrees += currentStep.getCourseChangeInDegrees();
            previousStep = currentStep;
        }
        if (stepUntilStableBearing == null) {
            stepUntilStableBearing = previousStep;
        }
        return stepUntilStableBearing == null ? null
                : new ManeuverCurveBoundaryExtension(stepUntilStableBearing.getTimePoint(),
                        stepUntilStableBearing.getSpeedWithBearing(), courseChangeUntilStepWithStableBearingInDegrees,
                        lowestSpeed, highestSpeed);
    }

    /**
     * Computes maneuver main curve details, such as entering and exiting time point with speed and bearing, time point
     * with the highest turning rate (maneuver climax) and total course change. The strategy here is to cut away bearing
     * steps from the left and right in order to reach a maximal course change corresponding to the target maneuver
     * direction. Furthermore, the main curve boundaries get additionally shortened if the turning rate of the
     * corresponding steps appears lower than
     * {@value #MIN_ANGULAR_VELOCITY_FOR_MAIN_CURVE_BOUNDARIES_IN_DEGREES_PER_SECOND} degrees per second.
     * 
     * @param maneuverTimePoint
     *            The computed time point of maneuver
     * @param bearingStepsToAnalyze
     *            The bearing steps contained within maneuver
     * @param maneuverDirection
     *            The nautical direction of the maneuver
     * @return The computed entering and exiting time point with its speeds with bearings, time point of maneuver climax
     *         and total course change for the main curve
     */
    public ManeuverMainCurveDetailsWithBearingSteps computeManeuverMainCurve(
            SpeedWithBearingStepsIterable bearingStepsToAnalyze, NauticalSide maneuverDirection) {
        double totalCourseChangeSignum = maneuverDirection == NauticalSide.PORT ? -1 : 1;
        double maxCourseChangeInDegrees = 0;
        double currentCourseChangeInDegrees = 0;
        double maxTurningRateInDegreesPerSecond = 0;
        double maxTurningRateInDegreesPerSecondCandidate = 0;
        Speed lowestSpeed = null;
        Speed lowestSpeedCandidate = null;
        Speed highestSpeed = null;
        Speed highestSpeedCandidate = null;
        TimePoint maneuverTimePoint = null;
        TimePoint maneuverTimePointCandidate = null;
        TimePoint previousTimePoint = null;
        // Refine the time point before and after maneuver by checking whether the total course changed before maneuver
        // time point may be increased or kept unchanged if we cut off bearing steps one by one from the left and right.
        TimePoint refinedTimePointBeforeManeuver = null;
        SpeedWithBearing refinedSpeedWithBearingBeforeManeuver = null;
        TimePoint refinedTimePointAfterManeuver = null;
        SpeedWithBearing refinedSpeedWithBearingAfterManeuver = null;
        boolean turningRateMinimumReachedAtMainCurveBeginning = false;
        ManeuverMainCurveDetailsWithBearingSteps bestBoundariesBeforeReset = null;
        for (SpeedWithBearingStep entry : bearingStepsToAnalyze) {
            currentCourseChangeInDegrees += entry.getCourseChangeInDegrees();
            TimePoint timePoint = entry.getTimePoint();
            // If the direction sign does not match, or the turning rate at the beginning of the curve is nearly
            // zero => cut the bearing step from the left
            if (0 >= currentCourseChangeInDegrees * totalCourseChangeSignum
                    || !turningRateMinimumReachedAtMainCurveBeginning && entry
                            .getTurningRateInDegreesPerSecond() < MIN_ANGULAR_VELOCITY_FOR_MAIN_CURVE_BOUNDARIES_IN_DEGREES_PER_SECOND) {
                // remember the section before timePointBefore reset, because it might be better than the following
                // section
                if (maxCourseChangeInDegrees
                        * totalCourseChangeSignum >= MIN_ANGULAR_VELOCITY_FOR_MAIN_CURVE_BOUNDARIES_IN_DEGREES_PER_SECOND
                        && (bestBoundariesBeforeReset == null || bestBoundariesBeforeReset.getDirectionChangeInDegrees()
                                * totalCourseChangeSignum < maxCourseChangeInDegrees)) {
                    bestBoundariesBeforeReset = new ManeuverMainCurveDetailsWithBearingSteps(
                            refinedTimePointBeforeManeuver, refinedTimePointAfterManeuver, maneuverTimePoint,
                            refinedSpeedWithBearingBeforeManeuver, refinedSpeedWithBearingAfterManeuver,
                            maxCourseChangeInDegrees, maxTurningRateInDegreesPerSecond, lowestSpeed, highestSpeed,
                            null);
                }
                currentCourseChangeInDegrees = 0;
                maxCourseChangeInDegrees = 0;
                refinedTimePointBeforeManeuver = timePoint;
                refinedSpeedWithBearingBeforeManeuver = entry.getSpeedWithBearing();
                refinedTimePointAfterManeuver = null;
                refinedSpeedWithBearingAfterManeuver = null;
                turningRateMinimumReachedAtMainCurveBeginning = false;
                maneuverTimePoint = maneuverTimePointCandidate = null;
                maxTurningRateInDegreesPerSecond = maxTurningRateInDegreesPerSecondCandidate = 0;
                highestSpeed = highestSpeedCandidate = lowestSpeed = lowestSpeedCandidate = entry.getSpeedWithBearing();
            } else {
                turningRateMinimumReachedAtMainCurveBeginning = true;
            }
            if (lowestSpeedCandidate == null || lowestSpeedCandidate.compareTo(entry.getSpeedWithBearing()) > 0) {
                lowestSpeedCandidate = entry.getSpeedWithBearing();
            }
            if (highestSpeedCandidate == null || highestSpeedCandidate.compareTo(entry.getSpeedWithBearing()) < 0) {
                highestSpeedCandidate = entry.getSpeedWithBearing();
            }
            // Check whether the course change is performed in the target direction of maneuver. If yes, consider
            // the step to locate the maneuver time point with the highest turning rate within main curve.
            if (0 < currentCourseChangeInDegrees * totalCourseChangeSignum) {
                if (maxTurningRateInDegreesPerSecondCandidate < entry.getTurningRateInDegreesPerSecond()) {
                    maxTurningRateInDegreesPerSecondCandidate = entry.getTurningRateInDegreesPerSecond();
                    Duration durationFromPreviousStep = previousTimePoint.until(timePoint);
                    maneuverTimePointCandidate = previousTimePoint.plus(durationFromPreviousStep.divide(2.0));
                }
            }
            // Check whether the totalCourseChange gets notably better with the added course change of current bearing
            // step, considering the target sign of the course change
            if (maxCourseChangeInDegrees * totalCourseChangeSignum < currentCourseChangeInDegrees
                    * totalCourseChangeSignum
                    && entry.getTurningRateInDegreesPerSecond() >= MIN_ANGULAR_VELOCITY_FOR_MAIN_CURVE_BOUNDARIES_IN_DEGREES_PER_SECOND) {
                maxCourseChangeInDegrees = currentCourseChangeInDegrees;
                refinedTimePointAfterManeuver = timePoint;
                refinedSpeedWithBearingAfterManeuver = entry.getSpeedWithBearing();
                maxTurningRateInDegreesPerSecond = maxTurningRateInDegreesPerSecondCandidate;
                maneuverTimePoint = maneuverTimePointCandidate;
                lowestSpeed = lowestSpeedCandidate;
                highestSpeed = highestSpeedCandidate;
            }
            previousTimePoint = timePoint;
        }
        // check if the course change of the current section is higher than of the best section which has been
        // previously reset
        if (bestBoundariesBeforeReset != null && bestBoundariesBeforeReset.getDirectionChangeInDegrees()
                * totalCourseChangeSignum >= maxCourseChangeInDegrees * totalCourseChangeSignum) {
            refinedTimePointBeforeManeuver = bestBoundariesBeforeReset.getTimePointBefore();
            refinedTimePointAfterManeuver = bestBoundariesBeforeReset.getTimePointAfter();
            refinedSpeedWithBearingBeforeManeuver = bestBoundariesBeforeReset.getSpeedWithBearingBefore();
            refinedSpeedWithBearingAfterManeuver = bestBoundariesBeforeReset.getSpeedWithBearingAfter();
            maneuverTimePoint = bestBoundariesBeforeReset.getTimePoint();
            maxCourseChangeInDegrees = bestBoundariesBeforeReset.getDirectionChangeInDegrees();
            maxTurningRateInDegreesPerSecond = bestBoundariesBeforeReset.getMaxTurningRateInDegreesPerSecond();
            lowestSpeed = bestBoundariesBeforeReset.getLowestSpeed();
            highestSpeed = bestBoundariesBeforeReset.getHighestSpeed();
        }
        if (refinedTimePointBeforeManeuver == null) {
            // Should not occur, if bearingStepsToAnalyze.size() > 0 and first BearingStep.getCourseChangeInDegrees() == 0
            return null;
        }
        if (refinedSpeedWithBearingAfterManeuver == null) {
            // Can only occur, when after maneuver time point different direction compared to the analyzed maneuver is
            // sailed. Thus, the resulting time point until the cut operation should be performed is the maneuver time
            // point itself.
            return null;
        }
        SpeedWithBearingStepsIterable maneuverMainCurveSpeedWithBearingSteps = getSpeedWithBearingStepsWithinTimeRange(
                bearingStepsToAnalyze, refinedTimePointBeforeManeuver, refinedTimePointAfterManeuver);
        ManeuverMainCurveDetailsWithBearingSteps mainCurveDetails = new ManeuverMainCurveDetailsWithBearingSteps(
                refinedTimePointBeforeManeuver, refinedTimePointAfterManeuver, maneuverTimePoint,
                refinedSpeedWithBearingBeforeManeuver, refinedSpeedWithBearingAfterManeuver, maxCourseChangeInDegrees,
                maxTurningRateInDegreesPerSecond, lowestSpeed, highestSpeed, maneuverMainCurveSpeedWithBearingSteps);
        return mainCurveDetails;
    }

    /**
     * Gets a new list with bearing steps which are lying between provided time range (including the boundaries). To get
     * the steps, only the provided {@code bearingStepsToAnalyze} is processed and filtered accordingly. No calls to
     * {@link GPSFixTrack#getSpeedWithBearingSteps(TimePoint, TimePoint, Duration)} are made.
     */
    public SpeedWithBearingStepsIterable getSpeedWithBearingStepsWithinTimeRange(
            SpeedWithBearingStepsIterable bearingStepsToAnalyze, TimePoint timePointBefore, TimePoint timePointAfter) {
        List<SpeedWithBearingStep> maneuverBearingSteps = new ArrayList<>();
        for (SpeedWithBearingStep entry : bearingStepsToAnalyze) {
            if (entry.getTimePoint().after(timePointAfter)) {
                break;
            }
            if (!entry.getTimePoint().before(timePointBefore)) {
                if (maneuverBearingSteps.isEmpty()) {
                    // First bearing step supposed to have 0 as course change as
                    // it does not have any previous steps with bearings to compute bearing difference.
                    // If the condition is not met, the existing code which uses ManeuverBearingStep class will break.
                    entry = new SpeedWithBearingStepImpl(entry.getTimePoint(), entry.getSpeedWithBearing(), 0.0, 0.0);
                }
                maneuverBearingSteps.add(entry);
            }
        }
        return new SpeedWithBearingStepsIterable(maneuverBearingSteps);
    }

}
