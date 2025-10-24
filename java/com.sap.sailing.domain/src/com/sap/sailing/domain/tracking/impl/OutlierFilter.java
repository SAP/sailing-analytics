package com.sap.sailing.domain.tracking.impl;

import java.util.Iterator;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Can filter {@link DynamicGPSFixTrack} contents for fixes that seem inconsistent in their timing. The underlying
 * hypothesis is that some fix sources, such as the "Sail Insight" app, may have issues that every now and then
 * produce a fix that has an incorrect time stamp but a valid and plausible position. While one may think that
 * in such cases a constant offset should be observed between the implausible and probable fixes, reality suggests
 * that the offsets are sometimes random.<p>
 * 
 * The filter looks for several indicators for inconsistencies, such as unusual sampling times that don't seem to fit
 * the overall frequency at which fixes usually arrive; also, inconsistencies between COG/SOG reported and inferred
 * from neighbor fix positions are a good indication for fixes with incorrect timing; lastly, a timing inconsistency
 * is considered more probable if a time offset exists that brings the fix fairly close to the segments of the remaining
 * track.<p>
 * 
 * Use {@link #findAndRemoveInconsistenciesOnRawFixes(DynamicGPSFixTrack)} to obtain a replaced {@link DynamicGPSFixTrack}
 * together with the number of inconsistencies found. The original track will not be changed by this operation.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class OutlierFilter {
    /**
     * On {@link #track} looks at adjacent fixes and compares the COG/SOG values reported by those fixes with the
     * COG/SOG value inferred from their position and time delta.
     * <p>
     * 
     * Hypothesis: we have a fix sequence that describes the trajectory of a sailing boat where some of the fixes have
     * an incorrect time point. The offset of these incorrect time points varies. In the particular case observed, all
     * regular fixes have a time point that is at a full second (UTC) with zero milliseconds, whereas all outliers have
     * a non-zero millisecond part that does not fit the otherwise very regular sampling rate.
     * <p>
     * 
     * Due to the irregularity of the offsets there is no point in trying to "learn" this offset. Instead, it's more
     * about recognizing the outliers which so far always seem to come as a single fix in a longer series of regular
     * fixes, and then finding a good time point adjustment so it matches the sequence.
     * <p>
     * 
     * With this in mind we would always have to look "both ways," trying to find out whether the fix originally had an
     * earlier or a later time point that would bring it closely in line with the other fixes. The fix does contain
     * valuable information despite its incorrect time point because it could indicate a deviation from the straight
     * line otherwise connecting the two adjacent fixes.
     * <p>
     * 
     * To approximate the correct time point we look for the track segment closest to the fix's position, then project
     * the fix onto it and split the segment's duration proportionately.
     * <p>
     * 
     * @return the number of inconsistencies found on the {@code track} passed, as well as a replacement track that has
     *         the outliers found adjusted
     */
    public Pair<Integer, DynamicGPSFixTrack<Competitor, GPSFixMoving>> findAndRemoveInconsistenciesOnRawFixes(DynamicGPSFixTrack<Competitor, GPSFixMoving> track) {
        int numberOfInconsistencies = 0;
        final DynamicGPSFixMovingTrackImpl<Competitor> replacedTrack = new DynamicGPSFixMovingTrackImpl<Competitor>(track.getTrackedItem(),
                /* millisecondsOverWhichToAverage */ 5000, /* losslessCompaction */ true);
        replacedTrack.suspendValidityAndMaxSpeedCaching();
        GPSFixMoving previous = null, fix = null;
        track.lockForRead();
        try {
            for (final GPSFixMoving next : track.getRawFixes()) { // raw fixes with ascending reported time
                if (previous != null && fix != null) {
                    final Pair<GPSFixMoving, Double> adjusted = isLikelyOutlierWithCorrectableTimepoint(track, previous, fix, next);
                    if (adjusted != null) {
                        // TODO remember (previous, fix, next) as an outlier to move and do not insert into replacedTrack
                        // TODO then run the adjustment process (see method adjust(track, previous, fix, next)) with the reduced track
                        // TODO this way, contiguous outliers will less probably have a negative impact on adjusting the outliers
                        numberOfInconsistencies++;
                        final GPSFixMoving replacementFix = adjusted.getA();
                        replacedTrack.add(replacementFix);
                    } else {
                        replacedTrack.add(fix);
                    }
                }
                previous = fix;
                fix = next;
            }
        } finally {
            track.unlockAfterRead();
        }
        return new Pair<>(numberOfInconsistencies, replacedTrack);
    }
    
    /**
     * For outlier identification, we use multiple hints:
     * <ul>
     * <li>a non-zero millisecond time point</li>
     * 
     * <li>the time point representing an inconsistency in an otherwise very regular sampling rate</li>
     * 
     * <li>a noticeable mismatch either in SOG (in case the fix has a time stamp too early and actually was recorded
     * later, so SOG is reported higher) with mostly consistent COG, or an approximately reverse COG (in case the fix
     * was actually recorded earlier) with a more or less random SOG</li>
     *
     * <li>the fix position being very close to the remaining trajectory, such that a segment between two non-outlier
     * fixes can be found to which the incorrectly-timed fix has a very small distance</li>
     * </ul>
     * 
     * @return {@code null} if at less than three of these four criteria are fulfilled for the {@code fix}; otherwise
     *         the adjusted fix with the new time point and the ratio between its distance from the closest track
     *         segment and that segment's length
     */
    private Pair<GPSFixMoving, Double> isLikelyOutlierWithCorrectableTimepoint(DynamicGPSFixTrack<Competitor, GPSFixMoving> track,
            GPSFixMoving previous, GPSFixMoving fix, GPSFixMoving next) {
        final int HOW_MANY_CRITERIA_TO_FULFILL = 2;
        final double DISTANCE_RATIO_TOLERANCE = 0.5; // ratio between cross-track distance and length of closest segment
        final Pair<GPSFixMoving, Double> adjustedFixAndDistance;
        int criteriaFulfilled = 0;
        if (hasNonZeroMilliseconds(fix.getTimePoint())) {
            criteriaFulfilled++;
        }
        if (isInconsistentWithSamplingRate(track, previous, fix, next)) {
            criteriaFulfilled++;
        }
        if (hasInconsistentCogSog(previous, fix, next, /* speed ratio tolerance */ 0.1, /* course degree tolerance */ 10)) {
            criteriaFulfilled++;
        }
        if (criteriaFulfilled >= HOW_MANY_CRITERIA_TO_FULFILL) {
            final Pair<GPSFixMoving, Double> adjusted = adjust(previous, fix, track);
            if (adjusted.getB() > DISTANCE_RATIO_TOLERANCE) {
                adjustedFixAndDistance = null;
            } else {
                adjustedFixAndDistance = adjusted;
                criteriaFulfilled++;
            }
        } else {
            adjustedFixAndDistance = null;
        }
        assert criteriaFulfilled >= HOW_MANY_CRITERIA_TO_FULFILL || adjustedFixAndDistance == null;
        return adjustedFixAndDistance;
    }
    
    public static boolean hasInconsistentCogSog(GPSFixMoving previous, GPSFixMoving fix, GPSFixMoving next, double SPEED_RATIO_TOLERANCE, double COURSE_DEGREE_TOLERANCE) {
        final SpeedWithBearing inferredBetweenPreviousAndFix = previous.getSpeedAndBearingRequiredToReach(fix);
        final SpeedWithBearing inferredBetweenFixAndNext = fix.getSpeedAndBearingRequiredToReach(next);
        final SpeedWithBearing reportedByPrevious = previous.getSpeed();
        final SpeedWithBearing reportedByFix = fix.getSpeed();
        final SpeedWithBearing reportedByNext = next.getSpeed();
        return isConsistent(reportedByPrevious, reportedByNext, SPEED_RATIO_TOLERANCE, COURSE_DEGREE_TOLERANCE)
                && !isConsistent(reportedByPrevious, inferredBetweenPreviousAndFix, SPEED_RATIO_TOLERANCE, COURSE_DEGREE_TOLERANCE)
                && !isConsistent(inferredBetweenFixAndNext, reportedByFix, SPEED_RATIO_TOLERANCE, COURSE_DEGREE_TOLERANCE)
                && !isConsistent(inferredBetweenFixAndNext, reportedByNext, SPEED_RATIO_TOLERANCE, COURSE_DEGREE_TOLERANCE);
    }
    
    public static boolean isConsistent(double ratio, double tolerance) {
        return ratio < 1+tolerance && ratio > 1-tolerance; 
    }

    public static boolean isConsistent(SpeedWithBearing a, SpeedWithBearing b, double SPEED_RATIO_TOLERANCE, double COURSE_DEGREE_TOLERANCE) {
        return isConsistent(a.getKnots()/b.getKnots(), SPEED_RATIO_TOLERANCE) &&
               a.getBearing().getDifferenceTo(b.getBearing()).abs().getDegrees() < COURSE_DEGREE_TOLERANCE;
    }

    private boolean isInconsistentWithSamplingRate(DynamicGPSFixTrack<Competitor, GPSFixMoving> track,
            GPSFixMoving previous, GPSFixMoving fix, GPSFixMoving next) {
        final double RATIO_TOLERANCE = 0.05;
        final Duration averageIntervalBetweenFixes = track.getAverageIntervalBetweenFixes();
        final double ratioPreviousToFix = previous.getTimePoint().until(fix.getTimePoint()).divide(averageIntervalBetweenFixes);
        final double ratioFixToNext = fix.getTimePoint().until(next.getTimePoint()).divide(averageIntervalBetweenFixes);
        return !isConsistent(ratioPreviousToFix, RATIO_TOLERANCE) || !isConsistent(ratioFixToNext, RATIO_TOLERANCE);
    }

    private boolean hasNonZeroMilliseconds(TimePoint timePoint) {
        return timePoint.asMillis() % 1000 != 0;
    }

    /**
     * @return the adjusted fix, and the ratio between the fix's cross-track distance from the nearest track segment and
     *         that segment's length
     */
    private Pair<GPSFixMoving, Double> adjust(GPSFixMoving previous, GPSFixMoving fix, DynamicGPSFixTrack<Competitor, GPSFixMoving> track) {
        final Iterator<GPSFixMoving> ascendingIterator = track.getFixesIterator(previous.getTimePoint(), /* inclusive */ true);
        final Pair<GPSFixMoving, Double> ascendingBestMatch = findBestMatch(fix, ascendingIterator);
        final Iterator<GPSFixMoving> descendingIterator = track.getFixesDescendingIterator(fix.getTimePoint(), /* inclusive */ false);
        final Pair<GPSFixMoving, Double> descendingBestMatch = findBestMatch(fix, descendingIterator);
        // Use the greater of the two offsets; the lesser will link it to its own sub-sequence neighbor
        return ascendingBestMatch != null && (descendingBestMatch == null || ascendingBestMatch.getB().compareTo(descendingBestMatch.getB()) < 0) ?
                ascendingBestMatch : descendingBestMatch;
    }
    
    /**
     * Starting with the first pair of fixes returned by the {@code iterator} looks for the minimal distance of fix's position
     * to the line connecting the pair of fixes.<p>
     * 
     * Should {@code fix} be consistent with the fixes from {@code iterator} then
     * the minimum distance is expected to be found right for the first pair of fixes, and that distance would then be the
     * typical distance traveled between two fixes at the COG/SOG reported. The offset computed should then be pretty close
     * to zero.<p>
     * 
     * Otherwise, a minimum would be found some number of fixes away. The distance of {@code fix}'s position to the two
     * other fixes will then be determined, and the duration between those fixes will be split proportionately based on the
     * respective distances of {@code fix}'s position to each of them to obtain a good estimate of its actual time point.
     * The difference between this inferred time point and the time point that {@code fix} reports is then used as the
     * offset.
     */
    private Pair<GPSFixMoving, Double> findBestMatch(final GPSFixMoving fix, final Iterator<GPSFixMoving> iterator) {
        final Position fixPosition = fix.getPosition();
        GPSFixMoving lastFix = null;
        GPSFixMoving result = null;
        Distance minimum = new MeterDistance(Double.MAX_VALUE);
        boolean foundMinimum = false;
        Double distanceRatio = null;
        while (!foundMinimum && iterator.hasNext()) {
            final GPSFixMoving currentFix = iterator.next();
            if (currentFix != fix) { // skip the outlier fix itself
                if (lastFix != null) {
                    final Distance distanceFromSegment = fixPosition.getDistanceToLine(lastFix.getPosition(), currentFix.getPosition()).abs();
                    if (distanceFromSegment.compareTo(minimum) < 0) {
                        minimum = distanceFromSegment;
                        final Bearing bearingFromLastToCurrent = lastFix.getPosition().getBearingGreatCircle(currentFix.getPosition());
                        final Distance alongTrackDistanceFromLastFix = fixPosition.alongTrackDistance(lastFix.getPosition(), bearingFromLastToCurrent);
                        // interpolate the time between the adjacent fixes to whose connection "fix" is closest, splitting the duration
                        // between the adjacent fixes proportionately based on "fix"'s distances to each of the two adjacent fixes:
                        final Distance distanceFromLastFixToCurrentFix = lastFix.getPosition().getDistance(currentFix.getPosition());
                        final TimePoint inferredTimePointForFix = lastFix.getTimePoint().plus(lastFix.getTimePoint().until(currentFix.getTimePoint()).times(
                                distanceFromLastFixToCurrentFix.equals(Distance.NULL) ? 0.5 : alongTrackDistanceFromLastFix.divide(distanceFromLastFixToCurrentFix)));
                        result = new GPSFixMovingImpl(fixPosition, inferredTimePointForFix, fix.getSpeed(), fix.getOptionalTrueHeading());
                        distanceRatio = distanceFromSegment.divide(distanceFromLastFixToCurrentFix);
                    } else { // we found a minimum after fix:
                        foundMinimum = true;
                    }
                }
                lastFix = currentFix;
            }
        }
        return foundMinimum ? new Pair<>(result, distanceRatio) : null;
    }
}
