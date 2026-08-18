package com.sap.sailing.domain.tracking.impl;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.GPSTrackListener;
import com.sap.sse.common.Duration;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.TimeRangeImpl;
import com.sap.sse.common.scalablevalue.KadaneExtremeSubsequenceFinder;
import com.sap.sse.common.scalablevalue.KadaneExtremeSubsequenceFinderLinkedNodesImpl;
import com.sap.sse.common.scalablevalue.ScalableDouble;
import com.sap.sse.common.scalablevalue.ScalableValueWithDistance;

/**
 * Given a {@link GPSFixTrack} containing {@link GPSFixMoving}, an instance of this class finds areas on the track where
 * the course changes sufficiently quickly and far to indicate a relevant maneuver. The basis for the duration to
 * consider is the {@link BoatClass}'s {@link BoatClass#getApproximateManeuverDuration() maneuver duration}. The basis
 * for the angular threshold is {@link BoatClass#getManeuverDegreeAngleThreshold()}.
 * <p>
 * 
 * Analyzing the track works with a {@link FixWindow analysis window} that contains up to a certain duration worth of
 * GPS fixes, keeping track of their respective {@link SpeedWithBearing} COG/SOG vectors as well as the cumulative
 * course change counted from the beginning of the window up to each of the fixes stored in the window. Course changes
 * within the window are all in the same {@link NauticalSide direction} (so either all to {@link NauticalSide#PORT PORT}
 * or all to {@link NauticalSide#STARBOARD STARBOARD}). When a fix addition leads to a maximum cumulative course change
 * exceeding the threshold, a maneuver candidate is added to {@link #maneuverCandidates}, and the fixes from the start
 * of the window up to the fix where the
 * <p>
 * 
 * When a fix is added to the window that at the insertion point leads to a change in COG change direction, all fixes
 * from the beginning of the window up to the fix preceding the one added are removed, so the window now starts with a
 * course change in the other direction. If the fix insert position was not at the end of the window, the course change
 * direction from the fix added to the next fix is also checked for consistency with the first course change direction
 * of the window.
 * <p>
 * 
 * Upon construction, all maneuver candidates for all fixes already contained by the {@link GPSFixTrack} are determined.
 * They can be requested in total or as a subset using {@link #approximate(TimePoint, TimePoint)}.
 * <p>
 * 
 * This object {@link GPSFixTrack#addListener(GPSTrackListener) observes} the {@link GPSFixTrack} for new fixes. If a
 * new fix arrives and it has a time point newer than the last fix in the current analysis window, it is added to the
 * window, updating the {@link #maneuverCandidates maneuver candidates} accordingly. If the fix was delivered "out of
 * order" outside of the current analysis window, a temporary {@link FixWindow} is constructed, and a time range around
 * the new fix is re-scanned, again updating the set of maneuver candidates accordingly. If the out-of-order fix fell in
 * between the first and the last fix in the current analysis window, it is inserted into the window accordingly, and
 * the current window is assessed for maneuvers as usual. Being in between fixes, this case does not influence the
 * current analysis window's duration.
 * <p>
 * 
 * For concurrency control, an approximation of this type uses {@code synchronized} methods where mutations may occur
 * (particularly in the {@link #gpsFixReceived(GPSFixMoving, Competitor, boolean, AddResult)} method), as well as for
 * the {@link #approximate(TimePoint, TimePoint)} method which is the "reading" accessor. Furthermore, object
 * serialization obtains this object's monitor by making the {@link #writeObject} method {@code synchronized}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class CourseChangeBasedTrackApproximation implements Serializable, GPSTrackListener<Competitor, GPSFixMoving> {
    private static final long serialVersionUID = -258129016229111573L;
    private static final Logger logger = Logger.getLogger(CourseChangeBasedTrackApproximation.class.getName());

    private final GPSFixTrack<Competitor, GPSFixMoving> track;
    private final BoatClass boatClass;
    private final FixWindow fixWindow;
    
    /**
     * The set needs no special synchronization; all methods on this {@link CourseChangeBasedTrackApproximation} object
     * that may read or mutate it are {@code synchronized} methods.
     */
    private final NavigableSet<GPSFixMoving> maneuverCandidates;
    private int numberOfFixesAdded;
    
    /**
     * The fix window consists of the list of fixes, a corresponding list with the course changes at each fix within the
     * window, as well as the aggregated total course change from the beginning of the window up to the respective fix;
     * furthermore, the window duration is maintained to compare against the maximum window length.
     * <p>
     * 
     * @author Axel Uhl (D043530)
     *
     */
    private class FixWindow implements Serializable {
        private static final long serialVersionUID = -6386675214043449110L;

        private final LinkedList<GPSFixMoving> window;
        
        /**
         * New fixes shall be inserted into the {@link FixWindow} only when it is unlikely that newer fixes will still
         * influence the calculation of its {@link GPSFixMoving#getCachedEstimatedSpeed() estimated speed}. This way,
         * the differences between seeing and not seeing newer fixes is reduced, and so it isn't so relevant anymore
         * whether the approximation is updated incrementally as fixes arrive, or after a race has been fully loaded.
         * <p>
         * 
         * See also bug 6209.
         */
        private final LinkedList<GPSFixMoving> queueOfNewFixes;
        
        /**
         * We need to remember the speed / bearing as we saw them when we inserted the fixes into the {@link #window}
         * collection. Based on more fixes getting added to the track, things may change. In particular, fixes that may have
         * had a valid speed when inserted may later have their cached speed/bearing invalidated, and computing it again
         * from the track may then yield {@code null}.<p>
         */
        private final LinkedList<SpeedWithBearing> speedForFixesInWindow;
        
        /**
         * one shorter than "window"; {@link #totalCourseChangeFromBeginningOfWindow}{@code [i]} is from
         * {@link #window}{@code [0]} to {@link #window}{@code [i+1]}
         */
        private final KadaneExtremeSubsequenceFinder<Double, Double, ScalableDouble> courseChangeBetweenFixesInWindow;

        private final double maneuverAngleInDegreesThreshold;
        private Duration windowDuration;
        
        private final boolean logFixes;
        
        FixWindow(boolean logFixes) {
            this.logFixes = logFixes;
            this.window = new LinkedList<>();
            this.queueOfNewFixes = new LinkedList<>();
            this.speedForFixesInWindow = new LinkedList<>();
            this.windowDuration = Duration.NULL;
            // use twice the maneuver duration to also catch slowly-executed gybes
            this.maneuverAngleInDegreesThreshold = boatClass.getManeuverDegreeAngleThreshold();
            this.courseChangeBetweenFixesInWindow = new KadaneExtremeSubsequenceFinderLinkedNodesImpl<>();
        }
        
        /**
         * The duration of the fix window to analyze is the maximum of twice the
         * {@link BoatClass#getApproximateManeuverDuration() approximate maneuver duration} of this track's boat class,
         * and three times the {@link GPSFixTrack#getAverageIntervalBetweenRawFixes() average duration between two raw
         * fixes} on the track.
         */
        private Duration getMaximumWindowLength() {
            final Duration twiceTheApproximateManeuverDuration = boatClass.getApproximateManeuverDuration().times(2);
            final Duration averageIntervalBetweenRawFixes = track.getAverageIntervalBetweenRawFixes();
            final Duration threeTimesTheDurationBetweenRawFixes = averageIntervalBetweenRawFixes == null ? null : averageIntervalBetweenRawFixes.times(3);
            return Comparator.<Duration>nullsFirst(Comparator.naturalOrder()).compare(
                    twiceTheApproximateManeuverDuration, threeTimesTheDurationBetweenRawFixes) > 0 ?
                    twiceTheApproximateManeuverDuration : threeTimesTheDurationBetweenRawFixes;
        }
        
        /**
         * Adds a fix to this fix window, sorted by time point. If this produces an interesting candidate within this
         * window, the candidate is returned, and the window is reset in such a way that the same candidate will not be
         * returned a second time.
         * <p>
         * 
         * The window will have established its invariants when this method returns.
         * <p>
         * 
         * This method queues the new fix and only adds it to the window when it is "old enough" to not be influenced
         * anymore by newer fixes that may still arrive. The influence is measured by the
         * {@link GPSFixTrack#getMillisecondsOverWhichToAverageSpeed()} interval. This way, approximation results
         * are more stable but have a delay with regards to the newest fixes known by the track.<p>
         * 
         * Fixes may arrive out of order. However, the method assumes that most fixes will have to get added to the
         * end of the queue. Adding out of order fixes may therefore be a bit slower than simply adding to the end.
         * 
         * @param next
         *            a fix that, in case this window is not empty, is not before the first fix in this window
         * @return a maneuver candidate from the {@link #window} if one became available by adding the {@code next} fix,
         *         or {@code null} if no maneuver candidate became available
         */
        GPSFixMoving add(GPSFixMoving next) {
            insertIntoQueueSortedByTime(next);
            final GPSFixMoving first = queueOfNewFixes.getFirst();
            // getMillisecondsOverWhichToAverageSpeed() is the duration in both directions that fixes are considered for validity  / outlier decisions.
            final GPSFixMoving result = first.getTimePoint().until(next.getTimePoint()).asMillis() > track.getMillisecondsOverWhichToAverageSpeed()
                    ? addOldEnoughFix(queueOfNewFixes.removeFirst())
                    : null;
            return result;
        }
        
        /**
         * Inserts {@code fix} into {@link #queueOfNewFixes}, maintaining sorted order by time point.
         * If a fix with an equal time point already exists, it is replaced.
         */
        private void insertIntoQueueSortedByTime(GPSFixMoving fix) {
            if (queueOfNewFixes.isEmpty() || queueOfNewFixes.getLast().getTimePoint().before(fix.getTimePoint())) {
                queueOfNewFixes.add(fix);
            } else {
                final ListIterator<GPSFixMoving> iter = queueOfNewFixes.listIterator(queueOfNewFixes.size());
                boolean added = false;
                while (!added && iter.hasPrevious()) {
                    final GPSFixMoving previous = iter.previous();
                    if (previous.getTimePoint().equals(fix.getTimePoint())) {
                        logger.fine(()->{
                            return
                                "Replacing fix " + previous
                                + " in queue of new fixes; previous fix was " + previous
                                + ", new fix is " + fix;
                        });
                        iter.set(fix); // replace existing fix
                        added = true;
                    } else if (previous.getTimePoint().before(fix.getTimePoint())) {
                        iter.next(); // move back to the position after previous
                        iter.add(fix);
                        added = true;
                    }
                }
                if (!added) {
                    queueOfNewFixes.addFirst(fix);
                }
            }
            assert inIncreasingTimePointOrder(queueOfNewFixes);
        }
        
        private boolean inIncreasingTimePointOrder(LinkedList<GPSFixMoving> fixes) {
            boolean result = true;
            TimePoint previousTimePoint = null;
            for (final GPSFixMoving fix : fixes) {
                if (previousTimePoint != null && !fix.getTimePoint().after(previousTimePoint)) {
                    result = false;
                    break;
                }
                previousTimePoint = fix.getTimePoint();
            }
            return result;
        }
        
        private GPSFixMoving addOldEnoughFix(GPSFixMoving next) {
            assert window.isEmpty() || !next.getTimePoint().before(window.peekFirst().getTimePoint());
            final GPSFixMoving result;
            final SpeedWithBearing nextSpeed = next.isEstimatedSpeedCached() ? next.getCachedEstimatedSpeed() : track.getEstimatedSpeed(next.getTimePoint());
            if (logFixes) {
                logFix(next, nextSpeed);
            }
            if (nextSpeed != null) {
                numberOfFixesAdded++;
                int insertPosition = window.size();
                GPSFixMoving previous;
                SpeedWithBearing previousSpeed;
                if (window.isEmpty()) {
                    previous = null;
                    previousSpeed = null;
                } else {
                    final ListIterator<GPSFixMoving> previousFixIter = window.listIterator(window.size());
                    final ListIterator<SpeedWithBearing> previousSpeedIter = speedForFixesInWindow.listIterator(speedForFixesInWindow.size());
                    previousSpeed = previousSpeedIter.previous();
                    while ((previous = previousFixIter.previous()).getTimePoint().after(next.getTimePoint())) {
                        insertPosition--;
                        previousSpeed = previousSpeedIter.previous();
                    } // previous shall be the last fix in this window before next
                }
                this.window.add(insertPosition, next);
                this.speedForFixesInWindow.add(insertPosition, nextSpeed);
                if (previous != null) {
                    // shortcut estimated COG calculation by checking the fix's cache; if cached, this avoids a
                    // rather expensive ceil/floor search on the track; resort to track.getEstimatedSpeed if not cached
                    assert previousSpeed != null; // we wouldn't have added the fix in the previous run if it hadn't had a valid speed
                    final double courseChangeBetweenPreviousAndNextInDegrees = previousSpeed.getBearing().getDifferenceTo(nextSpeed.getBearing()).getDegrees();
                    if (insertPosition == window.size()-1) { // if appended to the end, the window duration changes
                        windowDuration = windowDuration.plus(previous.getTimePoint().until(next.getTimePoint()));
                    }
                    if (courseChangeBetweenFixesInWindow.isEmpty()) {
                        courseChangeBetweenFixesInWindow.add(new ScalableDouble(courseChangeBetweenPreviousAndNextInDegrees));
                    } else {
                        courseChangeBetweenFixesInWindow.add(insertPosition-1, new ScalableDouble(courseChangeBetweenPreviousAndNextInDegrees));
                        if (courseChangeBetweenFixesInWindow.size() > insertPosition) { // the node just added isn't the last one; update the next node!
                            courseChangeBetweenFixesInWindow.remove(insertPosition);
                            final SpeedWithBearing nextNextSpeed = this.speedForFixesInWindow.get(insertPosition+1);
                            final double courseChangeBetweenNextAndNextNextInDegrees = nextSpeed.getBearing().getDifferenceTo(nextNextSpeed.getBearing()).getDegrees();
                            courseChangeBetweenFixesInWindow.add(insertPosition, new ScalableDouble(courseChangeBetweenNextAndNextNextInDegrees));
                        }
                    }
                    if (windowDuration.compareTo(getMaximumWindowLength()) > 0) {
                        result = tryToExtractManeuverCandidate();
                        if (result == null) {
                            // if a result was found, fixes up to and including the result have already been removed;
                            // otherwise, keep removing fixes from the beginning of the window until the window duration
                            // is again at or below the maximum allowed:
                            while (windowDuration.compareTo(getMaximumWindowLength()) > 0) {
                                removeFirst(1);
                            }
                        }
                    } else {
                        result = null;
                    }
                    assert window.isEmpty() && courseChangeBetweenFixesInWindow.isEmpty() || window.size() == courseChangeBetweenFixesInWindow.size()+1;
                } else { // the window was empty so far; we added the next fix, but no maneuver can yet be identified in lack of a course change
                    result = null;
                }
            } else {
                // nextSpeed == null; unable to determine COG for next fix, so fix was not added, therefore no new maneuver
                result = null;
            }
            return result; 
        }

        private void logFix(GPSFixMoving next, final SpeedWithBearing nextSpeed) {
            final boolean validityCached = next.isValidityCached();
            final boolean validity;
            if (validityCached) {
                validity = next.isValidCached();
            } else {
                track.lockForRead();
                try {
                    validity = track.isValid(next);
                } finally {
                    track.unlockAfterRead();
                }
            }
            // CSV logging: approxId, fixIndex, fixTimeMillis, validityCached, speedCached, COG, SOG
            System.out.println(System.identityHashCode(this) + "," + next.getTimePoint().asMillis() + ","
                    + next.isValidityCached() + "," + next.isEstimatedSpeedCached() + ","
                    + (nextSpeed == null ? "null" : nextSpeed.getBearing().getDegrees()) + ","
                    + (nextSpeed == null ? "null" : nextSpeed.getKnots()) + ","
                    + validityCached + "," + validity);
        }

        /**
         * Tries to extract a maneuver candidate from the current {@link #window}. See {@link #getManeuverCandidate()}.
         * Basically, the maximum course change in the window has to be equal to or exceed the maneuver threshold. If
         * such a candidate is found, all fixes before the candidate as well as the candidate itself are
         * {@link #removeFirst(int) removed} from the {@link #window}, and all invariants are re-established.
         * <p>
         * 
         * Usually, this method will be called by {@link #add(GPSFixMoving)}, but especially after having added the last
         * fix of a set of fixes the client may want to know if the current window already contains a maneuver
         * candidate, regardless of whether the typical maneuver duration has been exceeded by the window contents. In
         * such a case, clients may want to call this method which will return a maneuver candidate if available and
         * clean up the window accordingly.
         * 
         * @return the maneuver candidate if one was found in the {@link #window}, or {@code null} if no such candidate
         *         was found.
         */
        GPSFixMoving tryToExtractManeuverCandidate() {
            // analysis window has exceeded the typical maneuver duration for the boat class;
            final Pair<GPSFixMoving, Integer> candidateAndItsIndex = getManeuverCandidate();
            if (candidateAndItsIndex != null) {
                removeFirst(candidateAndItsIndex.getB()+1); // remove all including the maneuver fix
            }
            return candidateAndItsIndex == null ? null : candidateAndItsIndex.getA();
        }

        /**
         * Removes the first fix from the {@link #window} and adjusts all structures to re-establish all invariants. In
         * particular, the {@link #totalCourseChangeInWindow}, the {@link #totalCourseChangeFromBeginningOfWindow} and
         * the {@link #windowDuration}, as well as {@link #courseChangeInDegreesForFixesInWindow} are adjusted.
         * 
         * @return the fix removed from the beginning of this window
         */
        private void removeFirst(int howManyElementsToRemove) {
            assert !window.isEmpty();
            final int oldSize = window.size();
            for (int i=0; i<howManyElementsToRemove; i++) {
                window.removeFirst();
                speedForFixesInWindow.removeFirst();
            }
            trimQueue();
            windowDuration = window.isEmpty() ? Duration.NULL : window.getFirst().getTimePoint().until(window.getLast().getTimePoint());
            // adjust totalCourseChangeFromBeginningOfWindow by subtracting the first course change from all others
            // and shifting all by one position to the "left"
            if (!courseChangeBetweenFixesInWindow.isEmpty()) {
                // when removing the last element from the window, we have no "between fixes" anymore;
                // courseChangeBetweenFixesInWindow contains one element fewer than window, unless window is
                // empty in which case courseChangeBetweenFixesInWindow is also empty.
                courseChangeBetweenFixesInWindow.removeFirst(howManyElementsToRemove==oldSize?
                        howManyElementsToRemove-1:howManyElementsToRemove);
            }
        }

        /**
         * After having {@link #removeFirst(int) removed} items from the {@link #window}, the {@link #queueOfNewFixes}
         * may now contain fixes older than the oldest element in {@link #window}. This would violate our assumption that
         * only fixes at or after the start of the window are added, so we need to "trim" the queue accordingly.
         */
        private void trimQueue() {
            if (!window.isEmpty()) {
                final TimePoint timePointOfOldestFixInWindow = window.getFirst().getTimePoint();
                while (!queueOfNewFixes.isEmpty() && queueOfNewFixes.peekFirst().getTimePoint().before(timePointOfOldestFixInWindow)) {
                    queueOfNewFixes.removeFirst();
                }
            }
        }

        /**
         * If the {@link #absoluteMaximumTotalCourseChangeFromBeginningOfWindowInDegrees} is greater than or equal to
         * the {@link #maneuverAngleInDegreesThreshold}, the fix representing the maneuver candidate best is returned.
         * For this, this method selects the fix that has the greatest turn rate from its predecessor towards the
         * direction represented by the signum of the
         * {@link #absoluteMaximumTotalCourseChangeFromBeginningOfWindowInDegrees}. Turn rate is defined as angle change
         * per time.
         * <p>
         * The {@link #window} is left unchanged.
         * 
         * @return a pair holding the maneuver candidate fix if one was found, or {@code null} if no candidate was
         *         found, as well as the index of that fix within the {@link #window}
         */
        private Pair<GPSFixMoving, Integer> getManeuverCandidate() {
            final GPSFixMoving result;
            final ScalableValueWithDistance<Double, Double> maxSum = courseChangeBetweenFixesInWindow.getMaxSum();
            final double maximumCourseChangeToStarboard = maxSum == null ? Double.NEGATIVE_INFINITY : maxSum.divide(1);
            final ScalableValueWithDistance<Double, Double> minSum = courseChangeBetweenFixesInWindow.getMinSum();
            final double maximumCourseChangeToPort = minSum == null ? Double.NEGATIVE_INFINITY : -minSum.divide(1);
            final double absoluteMaximumTotalCourseChangeFromBeginningOfWindowInDegrees = Math.max(maximumCourseChangeToStarboard, maximumCourseChangeToPort);
            int indexOfMaximumAbsoluteCourseChangeInCorrectDirection = -1;
            if (absoluteMaximumTotalCourseChangeFromBeginningOfWindowInDegrees >= maneuverAngleInDegreesThreshold) {
                final int indexOfMaximumTotalCourseChangeStart = maximumCourseChangeToStarboard >= maximumCourseChangeToPort ?
                        courseChangeBetweenFixesInWindow.getStartIndexOfMaxSumSequence():
                        courseChangeBetweenFixesInWindow.getStartIndexOfMinSumSequence();
                final Iterable<ScalableDouble> maximumCourseChangeSequence = ()->maximumCourseChangeToStarboard >= maximumCourseChangeToPort ?
                        courseChangeBetweenFixesInWindow.getSubSequenceWithMaxSum() :
                        courseChangeBetweenFixesInWindow.getSubSequenceWithMinSum();
                int i=indexOfMaximumTotalCourseChangeStart;
                double maximumAbsoluteCourseChangeInCorrectDirection = Double.NEGATIVE_INFINITY;
                for (final ScalableDouble courseChange : maximumCourseChangeSequence) {
                    final double absoluteCourseChangeInDegrees = courseChange.divide(maximumCourseChangeToStarboard >= maximumCourseChangeToPort ? 1 : -1); 
                    if (absoluteCourseChangeInDegrees > maximumAbsoluteCourseChangeInCorrectDirection) {
                        maximumAbsoluteCourseChangeInCorrectDirection = absoluteCourseChangeInDegrees;
                        indexOfMaximumAbsoluteCourseChangeInCorrectDirection = i+1; // the course change is to the next fix in window
                    }
                    i++;
                }
                result = window.get(indexOfMaximumAbsoluteCourseChangeInCorrectDirection); // pick the fix introducing, not finishing, the highest turn rate
            } else {
                result = null;
            }
            assert indexOfMaximumAbsoluteCourseChangeInCorrectDirection < window.size();
            return result == null ? null : new Pair<>(result, indexOfMaximumAbsoluteCourseChangeInCorrectDirection);
        }

        /**
         * @return {@code true} if and only if this window is empty or the {@code fix} is not earlier than the first fix
         *         in this window
         */
        boolean isAtOrAfterFirst(TimePoint fix) {
            return window.isEmpty() || !fix.before(window.peekFirst().getTimePoint());
        }
    }

    public CourseChangeBasedTrackApproximation(GPSFixTrack<Competitor, GPSFixMoving> track, BoatClass boatClass, boolean logFixes) {
        this.track = track;
        this.boatClass = boatClass;
        this.fixWindow = new FixWindow(logFixes);
        this.maneuverCandidates = new TreeSet<>(TimedComparator.INSTANCE);
        track.addListener(this);
        addAllFixesOfTrack();
    }

    /**
     * Defined only in order to make it {@code synchronized} so that data will be written to the output stream
     * consistently.
     */
    private synchronized void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }
    
    private synchronized void addAllFixesOfTrack() {
        track.lockForRead();
        try {
            for (final GPSFixMoving fix : track.getRawFixes()) {
                addFix(fix);
            }
        } finally {
            track.unlockAfterRead();
        }
    }

    private void addFix(final GPSFixMoving fix) {
        addFix(fix, fixWindow);
    }
    
    /**
     * Adds {@code fix} to the {@code fixWindowToAddTo}, and if a maneuver candidate results, adds
     * that candidate to {@link #maneuverCandidates}.
     */
    private void addFix(final GPSFixMoving fix, FixWindow fixWindowToAddTo) {
        final GPSFixMoving maneuverCandidate = fixWindowToAddTo.add(fix);
        if (maneuverCandidate != null) {
            maneuverCandidates.add(maneuverCandidate);
        }
    }
    
    public int getNumberOfFixesAdded() {
        return numberOfFixesAdded;
    }
    
    /**
     * We want the listener relationship with the track to be serialized, e.g., during initial load / replication
     */
    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public synchronized void gpsFixReceived(GPSFixMoving fix, Competitor competitor, boolean firstFixInTrack, AddResult addedOrReplaced) {
        assert competitor == track.getTrackedItem();
        if (fixWindow.isAtOrAfterFirst(fix.getTimePoint())) {
            addFix(fix);
        } else {
            final FixWindow outOfOrderWindow = new FixWindow(/* logFixes */ false);
            final Duration maximumWindowLength = outOfOrderWindow.getMaximumWindowLength();
            // fix is an out-of-order delivery; construct a new FixWindow and analyze the track around the new fix.
            // A time range around the fix is constructed that will be re-scanned. The time range covers at least
            // maximumWindowLength before and after the fix. If any existing maneuver candidate is found in this
            // time range, the range is extended to cover at least maximumWindowLength before and after that
            // existing candidate, and the candidate is removed for now (in most cases it will be resurrected
            // by the re-scan).
            TimeRange leftPartOfTimeRangeToReScan = new TimeRangeImpl(fix.getTimePoint().minus(maximumWindowLength), fix.getTimePoint());
            TimeRange rightPartOfTimeRangeToReScan = new TimeRangeImpl(fix.getTimePoint(), fix.getTimePoint().plus(maximumWindowLength));
            GPSFixMoving candidateInRange;
            while ((candidateInRange = getExistingManeuverCandidateInRange(leftPartOfTimeRangeToReScan)) != null) {
                maneuverCandidates.remove(candidateInRange);
                leftPartOfTimeRangeToReScan = new TimeRangeImpl(candidateInRange.getTimePoint().minus(maximumWindowLength), candidateInRange.getTimePoint());
            }
            while ((candidateInRange = getExistingManeuverCandidateInRange(rightPartOfTimeRangeToReScan)) != null) {
                maneuverCandidates.remove(candidateInRange);
                rightPartOfTimeRangeToReScan = new TimeRangeImpl(candidateInRange.getTimePoint(), candidateInRange.getTimePoint().plus(maximumWindowLength));
            }
            final TimeRange totalRange = new TimeRangeImpl(leftPartOfTimeRangeToReScan.from(), rightPartOfTimeRangeToReScan.to());
            final List<GPSFixMoving> fixesToAdd = new ArrayList<>();
            track.lockForRead();
            try {
                for (final GPSFixMoving reAnalysisFix : track.getFixes(totalRange.from(), /* fromInclusive */ true, totalRange.to(), /* toInclusive */ true)) {
                    fixesToAdd.add(reAnalysisFix);
                }
            } finally {
                track.unlockAfterRead();
            }
            for (final GPSFixMoving reAnalysisFix : fixesToAdd) {
                addFix(reAnalysisFix, outOfOrderWindow);
            }
            GPSFixMoving remainingCandidate = outOfOrderWindow.tryToExtractManeuverCandidate(); // even if window isn't full now, look for a candidate
            if (remainingCandidate != null) {
                maneuverCandidates.add(remainingCandidate);
            }
        }
    }

    /**
     * Precondition: the caller owns this object's monitor ({@code synchronized})
     */
    private GPSFixMoving getExistingManeuverCandidateInRange(TimeRange leftPartOfTimeRangeToReScan) {
        final GPSFixMoving firstCandidateAtOrAfterStartOfTimeRange = maneuverCandidates.ceiling(
                /* dummy fix */ createDummyFix(leftPartOfTimeRangeToReScan.from()));
        final GPSFixMoving result;
        if (leftPartOfTimeRangeToReScan.includes(firstCandidateAtOrAfterStartOfTimeRange)) {
            result = firstCandidateAtOrAfterStartOfTimeRange;
        } else {
            result = null;
        }
        return result;
    }

    private GPSFixMovingImpl createDummyFix(TimePoint timePoint) {
        return new GPSFixMovingImpl(/* position */ null, timePoint, /* speed */ null, /* optionalTrueHeading */ null);
    }

    @Override
    public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        // we're not interested in this sort of change here
    }

    public synchronized Iterable<GPSFixMoving> approximate(TimePoint from, TimePoint to) {
        final Iterable<GPSFixMoving> result;
        if (fixWindow.isAtOrAfterFirst(to)) {
            // there may be an overlap of the time range requested and the current window;
            // could be that the non-full window contains a candidate; check:
            GPSFixMoving remainingCandidate = fixWindow.tryToExtractManeuverCandidate();
            if (remainingCandidate != null) {
                maneuverCandidates.add(remainingCandidate);
            }
        }
        if (from.before(to)) {
            synchronized (maneuverCandidates) {
                result = new ArrayList<>(maneuverCandidates.subSet(createDummyFix(from), /* fromInclusive */ true, createDummyFix(to), /* toInclusive */ true));
            }
        } else {
            result = Collections.emptySet();
        }
        return result;
    }
}
