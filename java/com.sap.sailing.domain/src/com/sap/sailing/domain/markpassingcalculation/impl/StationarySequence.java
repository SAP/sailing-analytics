package com.sap.sailing.domain.markpassingcalculation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sse.common.Bounds;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.ValueCollectionConstructor;
import com.sap.sse.common.impl.BoundsImpl;
import com.sap.sse.common.impl.MeterDistance;

/**
 * A sequence of {@link Candidate}s such that the track of smoothed GPS fixes between them fits into a bounding box that
 * has a maximum {@link Bounds#getDiameter() diameter} of {@link #CANDIDATE_FILTER_DISTANCE}. The first, the last and
 * the most probable {@link Candidate} for each {@link Candidate#getWaypoint() waypoint} for which this sequence has
 * candidates are considered valid and are returned from {@link #getValidCandidates()}. Note, that first, last and most
 * probably may fully or partly overlap, especially in cases with fewer than three candidates for a single waypoint.
 * <p>
 * 
 * A sequence can be updated by adding ({@link #addWithin(Candidate, Set, Set)},
 * {@link #tryToExtendAfterLast(Candidate, Set, Set)}, and
 * {@link #tryToExtendBeforeFirst(Candidate, Set, Set, NavigableSet)}) and by removing
 * ({@link #remove(Candidate, Set, Set, NavigableSet)}) candidates, as well as by
 * {@link #tryToAddFix(GPSFixMoving, Set, Set, NavigableSet, boolean) announcing GPS fixes} that were added to or
 * replaced in the underlying {@link #track}. If candidates are added/removed after changes to the track were made but
 * before those changes were {@link #tryToAddFix(GPSFixMoving, Set, Set, NavigableSet, boolean) announced},
 * inconsistencies regarding the bounding box may be observed. Fixes may have appeared that cause the bounding box
 * around the track between the first and the last candidate in this sequence to exceed the maximum "diameter" allowed
 * for a {@link StationarySequence} (see also bug 5087). The implementation reacts tolerant to such observations and
 * assumes that the call to {@link #tryToAddFix(GPSFixMoving, Set, Set, NavigableSet, boolean)} that announces the
 * offending change will be made at a later point in time, then making the necessary adjustments again. In particular,
 * the implementation will tolerate bounding box sizes in violation of the diameter rule if this violation is unexpected
 * (e.g., upon removing a candidate
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class StationarySequence {
    /**
     * If we identify several consecutive candidates that all lie in a bounding box with a {@link Bounds#getDiameter()
     * diameter} less than or equal to this distance, only the first and the last of those candidates pass the filter.
     */
    public static final Distance CANDIDATE_FILTER_DISTANCE = new MeterDistance(30);

    private final ValueCollectionConstructor<Candidate, NavigableSet<Candidate>> valueSetConstructorForCandidatesByTime;

    private final ValueCollectionConstructor<Candidate, NavigableSet<Candidate>> valueSetConstructorForCandidatesByAscendingProbability;

    private final NavigableSet<Candidate> candidates;
    
    private final Map<Waypoint, NavigableSet<Candidate>> candidatesByWaypoint;

    private final Map<Waypoint, NavigableSet<Candidate>> candidatesByWaypointSortedByAscendingProbability;
    
    private final GPSFixTrack<Competitor, GPSFixMoving> track;
    
    private Bounds boundingBoxOfTrackSpanningCandidates;

    private final Comparator<Candidate> candidateComparator;

    /**
     * Constructs a new stationary sequence with a single {@code seed} candidate in it which acts as {@link #getFirst()
     * first} and {@link #getLast() last} element at the same time. Therefore, the track segment spanned by the
     * resulting sequence is empty which trivially fulfills the bounding box criterion.
     */
    public StationarySequence(Candidate seed, Comparator<Candidate> candidateComparator, GPSFixTrack<Competitor, GPSFixMoving> track) {
        this.candidates = new TreeSet<>(candidateComparator);
        this.candidatesByWaypoint = new HashMap<>();
        this.candidatesByWaypointSortedByAscendingProbability = new HashMap<>();
        this.candidateComparator = candidateComparator;
        this.valueSetConstructorForCandidatesByTime = ()->new TreeSet<Candidate>(candidateComparator);
        this.valueSetConstructorForCandidatesByAscendingProbability = ()-> new TreeSet<Candidate>((c1, c2)->{
            final int resultBasedOnProbability = Double.compare(c1.getProbability(), c2.getProbability());
            final int result;
            if (resultBasedOnProbability == 0) {
                // disambiguate based on regular candidateComparator
                result = candidateComparator.compare(c1, c2);
            } else {
                result = resultBasedOnProbability;
            }
            return result;
        });
        this.track = track;
        this.candidates.add(seed);
        Util.addToValueSet(candidatesByWaypoint, seed.getWaypoint(), seed, valueSetConstructorForCandidatesByTime);
        Util.addToValueSet(candidatesByWaypointSortedByAscendingProbability, seed.getWaypoint(), seed, valueSetConstructorForCandidatesByAscendingProbability);
        boundingBoxOfTrackSpanningCandidates = createNewBounds(seed);
        assert isCandidatesConsistent(seed);
    }
    
    /**
     * Tries to extend this stationary sequence at the end by traversing fixes, adding them to the
     * {@link #boundingBoxOfTrackSpanningCandidates} and seeing if the bounding box grows larger than
     * {@link #CANDIDATE_FILTER_DISTANCE} in {@link Bounds#getDiameter() diameter}. If it does, nothing changes, and
     * {@code false} is returned. But if the track leading to the {@code candidateAfterSequence} keeps the bounding box
     * sufficiently small, the candidate is added, the {@link #boundingBoxOfTrackSpanningCandidates} is adjusted,
     * the {@code candidateAfterSequence} will be added to {@code candidatesEffectivelyAdded} because it is the
     * new {@link #getLast() last} element of this sequence, and all other candidates in this sequence
     * that no longer are sufficiently close (time-wise) to the new last candidate are added to
     * {@code candidatesEffectivelyRemoved}; {@code true} is returned in this case, indicating that the sequence
     * has been extended successfully.
     */
    public boolean tryToExtendAfterLast(Candidate candidateAfterSequence,
            Set<Candidate> candidatesEffectivelyAdded, Set<Candidate> candidatesEffectivelyRemoved) {
        Bounds bounds = computeExtendedBoundsForFixesBetweenCandidates(getLast(), candidateAfterSequence,
                boundingBoxOfTrackSpanningCandidates, /* tolerateTemporaryBoundingBoxExcess */ false);
        if (bounds != null) {
            boundingBoxOfTrackSpanningCandidates = bounds;
            addCandidateAndUpdateFilterResultDelta(candidateAfterSequence, candidatesEffectivelyAdded, candidatesEffectivelyRemoved);
        }
        return bounds != null;
    }

    /**
     * Has no side effects on this object. In particular, the {@link #boundingBoxOfTrackSpanningCandidates} field is not
     * yet updated to the result of this method. Use the method to test whether extending the
     * {@cod eboundingBoxToStartWith} would be possible.
     * 
     * @param tolerateTemporaryBoundingBoxExcess
     *            if {@code true}, fixes will be added to the bounding box returned no matter its
     *            {@link Bounds#getDiameter() diameter}
     * 
     * @return {@code null} if {code #boundingBoxToStartWith} was {@code null} already or the
     *         {@link #boundingBoxToStartWith} would grow too large if the fixes between {@code start} and {@code end}
     *         were inserted; the extended bounds otherwise.
     */
    private Bounds computeExtendedBoundsForFixesBetweenCandidates(Candidate start, Candidate end,
            Bounds boundingBoxToStartWith, boolean tolerateTemporaryBoundingBoxExcess) {
        Bounds bounds = boundingBoxToStartWith;
        if (bounds != null) {
            track.lockForRead();
            try {
                final Iterator<GPSFixMoving> iter = track.getRawFixesIterator(start.getTimePoint(), /* inclusive */ true,
                        end.getTimePoint(), /* inclusive */ true);
                while (iter.hasNext()) {
                    final GPSFixMoving fix = iter.next();
                    bounds = bounds.extend(fix.getPosition());
                    if (!tolerateTemporaryBoundingBoxExcess && bounds.getDiameter().compareTo(CANDIDATE_FILTER_DISTANCE) > 0) {
                        bounds = null;
                        break;
                    }
                }
            } finally {
                track.unlockAfterRead();
            }
        }
        return bounds;
    }

    /**
     * Tries to extend this stationary sequence before the start by traversing fixes, adding them to the
     * {@link #boundingBoxOfTrackSpanningCandidates} and seeing if the bounding box grows larger than
     * {@link #CANDIDATE_FILTER_DISTANCE} in {@link Bounds#getDiameter() diameter}. If it does, nothing changes, and
     * {@code false} is returned. But if the track leading to the {@code candidateBeforeSequence} keeps the bounding box
     * sufficiently small, the candidate is added, the {@link #boundingBoxOfTrackSpanningCandidates} is adjusted, the
     * {@code candidateBeforeSequence} will be added to {@code candidatesEffectivelyAdded} because it is the new
     * {@link #getFirst() first} element of this sequence, and all other candidates in this sequence that no longer are
     * sufficiently close (time-wise) to the new first candidate are added to {@code candidatesEffectivelyRemoved};
     * {@code true} is returned in this case, indicating that the sequence has been extended successfully.
     * 
     * @param stationarySequenceSetToUpdate
     *            when this method causes a change in what {@link #getFirst()} returns before and after the call, this
     *            method maintains the set referenced by this parameter accordingly, assuming that the position in the
     *            set may change, or, if this sequence runs empty, it has to be removed from the set altogether.
     */
    public boolean tryToExtendBeforeFirst(Candidate candidateBeforeSequence,
            Set<Candidate> candidatesEffectivelyAdded, Set<Candidate> candidatesEffectivelyRemoved,
            NavigableSet<StationarySequence> stationarySequenceSetToUpdate) {
        Bounds bounds = computeExtendedBoundsForFixesBetweenCandidates(candidateBeforeSequence, getFirst(),
                boundingBoxOfTrackSpanningCandidates, /* tolerateTemporaryBoundingBoxExcess */ false);
        if (bounds != null) {
            boundingBoxOfTrackSpanningCandidates = bounds;
            addCandidateAndUpdateFilterResultDelta(candidateBeforeSequence, candidatesEffectivelyAdded, candidatesEffectivelyRemoved);
        }
        return bounds != null;
    }

    public Iterable<Candidate> getValidCandidates() {
        final NavigableSet<Candidate> result = new TreeSet<>(candidateComparator);
        for (final NavigableSet<Candidate> candidateForWaypoint : candidatesByWaypoint.values()) {
            result.add(candidateForWaypoint.first());
            result.add(candidateForWaypoint.last());
        }
        for (final NavigableSet<Candidate> candidateForWaypoint : candidatesByWaypointSortedByAscendingProbability.values()) {
            result.add(candidateForWaypoint.last());
        }
        return result;
    }
    
    /**
     * A candidate that is part of this stationary sequence is considered valid if it is time-wise close enough to one
     * of the sequence's borders (see {@link #CANDIDATE_FILTER_TIME_WINDOW}) or if it is a candidate for a
     * {@link Candidate#isFixed() fixed mark passing}.
     * <p>
     * 
     * It is much faster on average to call this method than to call {@link #getValidCandidates()} and then probing for
     * contains because this method only needs to check the data structures related to {@code c}'s
     * {@link Candidate#getWaypoint() waypoint} whereas {@link #getValidCandidates()} enumerates all valid candidates
     * for all waypoints contained in this sequence.
     */
    private boolean isValidCandidate(Candidate c) {
        final NavigableSet<Candidate> candidatesForSameWaypoint;
        final NavigableSet<Candidate> candidatesForSameWaypointSortedByAscendingProbability;
        return c.isFixed() ||
                ((candidatesForSameWaypoint=candidatesByWaypoint.get(c.getWaypoint())) != null
                        && !candidatesForSameWaypoint.isEmpty()
                        && (candidatesForSameWaypoint.first() == c || candidatesForSameWaypoint.last() == c)) ||
                ((candidatesForSameWaypointSortedByAscendingProbability=candidatesByWaypointSortedByAscendingProbability.get(c.getWaypoint())) != null
                        && !candidatesForSameWaypointSortedByAscendingProbability.isEmpty()
                        && candidatesForSameWaypointSortedByAscendingProbability.last() == c);
    }
    
    public int size() {
        return candidates.size();
    }
    
    public boolean isEmpty() {
        return candidates.isEmpty();
    }
    
    /**
     * Adds a candidate whose time point is within (including the first/last candidate's time points) the
     * time range of this sequence. This will not change this sequence's set of fixes spanned. Therefore,
     * also the {@link #boundingBoxOfTrackSpanningCandidates bounding box} remains unchanged. If the candidate
     * is the first or the last for its waypoint in this sequence it passes the filter and is therefore added to
     * {@code candidatesEffectivelyAdded} and removed from {@code candidatesEffectivelyRemoved}. and there are now more than two candidates for
     * that waypoint in this sequence, the previously first or last, respectively, will be removed from the filter
     * results because it is no longer valid.
     */
    void addWithin(Candidate candidate, Set<Candidate> candidatesEffectivelyAdded, Set<Candidate> candidatesEffectivelyRemoved) {
        assert !candidates.contains(candidate) && !getFirst().getTimePoint().after(candidate.getTimePoint()) &&
                !getLast().getTimePoint().before(candidate.getTimePoint());
        addCandidateAndUpdateFilterResultDelta(candidate, candidatesEffectivelyAdded, candidatesEffectivelyRemoved);
    }

    /**
     * Adds a candidate to this sequence. The caller is responsible for ensuring that the
     * {@link #boundingBoxOfTrackSpanningCandidates bounding box} is updated consistently. If the candidate is the first
     * or the last for its waypoint in this sequence it passes the filter and is therefore added to
     * {@code candidatesEffectivelyAdded} and removed from {@code candidatesEffectivelyRemoved}. If there are now more
     * than two candidates for that waypoint in this sequence and the new one passes the filter because it's the first
     * or last, the previously first or last, respectively, will be removed from the filter results because it is no
     * longer valid, unless it's a {@link Candidate#isFixed() fixed} candidate.
     */
    private void addCandidateAndUpdateFilterResultDelta(Candidate candidate, Set<Candidate> candidatesEffectivelyAdded,
            Set<Candidate> candidatesEffectivelyRemoved) {
        assert isCandidatesConsistent(candidate);
        candidates.add(candidate);
        final NavigableSet<Candidate> candidatesForSameWaypointSortedByAscendingProbability = candidatesByWaypointSortedByAscendingProbability.get(candidate.getWaypoint());
        assert candidatesForSameWaypointSortedByAscendingProbability == null || !candidatesForSameWaypointSortedByAscendingProbability.contains(candidate);
        final Candidate mostProbableCandidateForWaypointSoFar;
        if (candidatesForSameWaypointSortedByAscendingProbability != null && !candidatesForSameWaypointSortedByAscendingProbability.isEmpty()) {
            mostProbableCandidateForWaypointSoFar = candidatesForSameWaypointSortedByAscendingProbability.last();
        } else {
            mostProbableCandidateForWaypointSoFar = null;
        }
        Util.addToValueSet(candidatesByWaypoint, candidate.getWaypoint(), candidate, valueSetConstructorForCandidatesByTime);
        Util.addToValueSet(candidatesByWaypointSortedByAscendingProbability, candidate.getWaypoint(), candidate, valueSetConstructorForCandidatesByAscendingProbability);
        assert isCandidatesConsistent(candidate);
        if (isValidCandidate(candidate)) {
            candidatesEffectivelyAdded.add(candidate);
            candidatesEffectivelyRemoved.remove(candidate);
            // check if candidate became first or last for its waypoint
            final NavigableSet<Candidate> candidatesForWaypoint = candidatesByWaypoint.get(candidate.getWaypoint());
            if (candidatesForWaypoint.size() > 2) {
                final Iterator<Candidate> iterator;
                // the new candidate "shadows" a previously valid candidate which now no longer is the first/last
                // if the new candidate is now first/last, respectively:
                if (candidatesForWaypoint.first() == candidate) {
                    iterator = candidatesForWaypoint.iterator();
                } else if (candidatesForWaypoint.last() == candidate) {
                    iterator = candidatesForWaypoint.descendingIterator();
                } else {
                    iterator = null;
                }
                if (iterator != null) {
                    // remove the candidate previously valid that is now shadowed by the new candidate,
                    // but only if it isn't or didn't become valid for other reasons, such as for being
                    // the most probable candidate
                    final Candidate borderCandidate = iterator.next();
                    assert borderCandidate == candidate;
                    final Candidate candidateShadowed = iterator.next(); // this is the one shadowed...
                    if (!candidateShadowed.isFixed() && !isValidCandidate(candidateShadowed)) {
                        candidatesEffectivelyAdded.remove(candidateShadowed);
                        candidatesEffectivelyRemoved.add(candidateShadowed);
                    }
                }
            }
            if (mostProbableCandidateForWaypointSoFar != null && !isValidCandidate(mostProbableCandidateForWaypointSoFar)) {
                // the candidate for the same waypoint that previously had the highest probability no longer passes
                // the filter; it needs to be removed from candidatesEffectivelyAdded and added to candidatesEffectivelyRemoved.
                candidatesEffectivelyAdded.remove(mostProbableCandidateForWaypointSoFar);
                candidatesEffectivelyRemoved.add(mostProbableCandidateForWaypointSoFar);
            }
        }
    }
    
    /**
     * Removes the candidate from {@link candidates}. If the candidate was a valid candidate because it was the first or
     * the last in this sequence for its waypoint, it is added to {@code candidatesEffectivelyRemoved} and removed from
     * {@code candidatesEffectivelyAdded}. If it was the first or the last candidate of its waypoint, the candidate for
     * the same waypoint that is now the first or the last, respectively, is added to the filter result.
     * 
     * @param stationarySequenceSetToUpdate
     *            when this method causes a change in what {@link #getFirst()} returns before and after the call, this
     *            method maintains the set referenced by this parameter accordingly, assuming that the position in the
     *            set may change, or, if this sequence runs empty, it has to be removed from the set altogether.
     *            Furthermore, should removing the {@code candidate} reduce this sequence's size to {@code 1}, this
     *            sequence is removed from the {@code stationarySequenceSetToUpdate}.
     */
    void remove(Candidate candidate, Set<Candidate> candidatesEffectivelyAdded, Set<Candidate> candidatesEffectivelyRemoved,
            NavigableSet<StationarySequence> stationarySequenceSetToUpdate) {
        assert candidates.contains(candidate);
        assert isCandidatesConsistent(candidate);
        final NavigableSet<Candidate> candidatesWithSameWaypoint = candidatesByWaypoint.get(candidate.getWaypoint());
        assert candidatesWithSameWaypoint.contains(candidate);
        final NavigableSet<Candidate> candidatesWithSameWaypointSortedByAscendingProbability = candidatesByWaypointSortedByAscendingProbability.get(candidate.getWaypoint());
        assert candidatesWithSameWaypointSortedByAscendingProbability.contains(candidate);
        final boolean wasValidCandidate = isValidCandidate(candidate);
        final boolean wasFirst = candidate == getFirst();
        final boolean wasLast = candidate == getLast();
        final boolean wasFirstOfItsWaypoint = candidatesWithSameWaypoint.first() == candidate;
        final boolean wasLastOfItsWaypoint = candidatesWithSameWaypoint.last() == candidate;
        final boolean secondOfItsWaypointWasValid = candidatesWithSameWaypoint.size() > 1 &&
                isValidCandidate(candidatesWithSameWaypoint.higher(candidatesWithSameWaypoint.first()));
        final boolean secondToLastOfItsWaypointWasValid = candidatesWithSameWaypoint.size() > 1 &&
                isValidCandidate(candidatesWithSameWaypoint.lower(candidatesWithSameWaypoint.last()));
        final boolean wasMostProbableOfItsWaypoint = candidatesWithSameWaypointSortedByAscendingProbability.last() == candidate;
        final boolean secondMostProbableOfItsWaypointWasValid = candidatesWithSameWaypointSortedByAscendingProbability.size() > 1 &&
                isValidCandidate(candidatesWithSameWaypointSortedByAscendingProbability.lower(candidatesWithSameWaypointSortedByAscendingProbability.last()));
        if (wasFirst || size() == 2) { // if size() == 2 then it will shrink to 1 and this sequence shall be removed
            stationarySequenceSetToUpdate.remove(this);
        }
        candidates.remove(candidate);
        Util.removeFromValueSet(candidatesByWaypoint, candidate.getWaypoint(), candidate);
        Util.removeFromValueSet(candidatesByWaypointSortedByAscendingProbability, candidate.getWaypoint(), candidate);
        assert !candidatesWithSameWaypointSortedByAscendingProbability.contains(candidate);
        assert isCandidatesConsistent(candidate);
        if (wasFirst && candidates.size() > 1) {
            stationarySequenceSetToUpdate.add(this);
        }
        // recalculate the bounding box if the candidate removal causes the track spanned from first
        // to last candidate to actually shrink
        if ((wasFirst && candidate.getTimePoint().until(getFirst().getTimePoint()).compareTo(Duration.NULL) > 0) ||
            (wasLast  && getLast().getTimePoint().until(candidate.getTimePoint()).compareTo(Duration.NULL) > 0)) {
            refreshBoundingBox();
        }
        if (wasValidCandidate) {
            candidatesEffectivelyRemoved.add(candidate);
            candidatesEffectivelyAdded.remove(candidate);
            // incremental update of delta structures:
            // check if another candidate of the same waypoint has become valid by removing "candidate"
            if (wasFirstOfItsWaypoint) {
                if (!secondOfItsWaypointWasValid && !candidatesWithSameWaypoint.isEmpty()) {
                    // add only if the now first (which used to be the second before removing candidate)
                    // wasn't already valid before, e.g., because it used to be the most probable or
                    // the last; see bug 5094
                    candidatesEffectivelyAdded.add(candidatesWithSameWaypoint.first());
                    candidatesEffectivelyRemoved.remove(candidatesWithSameWaypoint.first());
                }
            } else if (wasLastOfItsWaypoint) {
                if (!secondToLastOfItsWaypointWasValid && !candidatesWithSameWaypoint.isEmpty()) {
                    // add only if the now last (which used to be the second to last before removing candidate)
                    // wasn't already valid before, e.g., because it used to be the most probable or
                    // the first; see bug 5094
                    candidatesEffectivelyAdded.add(candidatesWithSameWaypoint.last());
                    candidatesEffectivelyRemoved.remove(candidatesWithSameWaypoint.last());
                }
            }
            // check if another candidate of the same waypoint has become the most probable
            if (wasMostProbableOfItsWaypoint) {
                if (!secondMostProbableOfItsWaypointWasValid && !candidatesWithSameWaypointSortedByAscendingProbability.isEmpty()) {
                    // add only if the now most probable (which used to be the second most probable before removing candidate)
                    // wasn't already valid before, e.g., because it used to be the first or the last; see bug 5094
                    candidatesEffectivelyAdded.add(candidatesWithSameWaypointSortedByAscendingProbability.last());
                    candidatesEffectivelyRemoved.remove(candidatesWithSameWaypointSortedByAscendingProbability.last());
                }
            }
        }
    }
    
    /**
     * For internal assertions, validates that regarding {@code candidate} the three collections
     * {@link #candidates}, {@link #candidatesByWaypoint} and {@link #candidatesByWaypointSortedByAscendingProbability}
     * consistently either do or do not contain {@code candidate}.
     */
    private boolean isCandidatesConsistent(Candidate candidate) {
        final boolean inCandidates = candidates.contains(candidate);
        final boolean inCandidatesByWaypoint = candidatesByWaypoint.get(candidate.getWaypoint()) != null && candidatesByWaypoint.get(candidate.getWaypoint()).contains(candidate);
        final boolean inCandidatesByWaypointSortedByAscendingProbability = candidatesByWaypointSortedByAscendingProbability.get(candidate.getWaypoint()) != null &&
                candidatesByWaypointSortedByAscendingProbability.get(candidate.getWaypoint()).contains(candidate);
        return inCandidates == inCandidatesByWaypoint && inCandidates == inCandidatesByWaypointSortedByAscendingProbability;
    }

    /**
     * {@link #recomputeBounds(boolean) Computes} and sets this sequence's bounding box from scratch. It is assumed that
     * this method is called <em>after</em> having made sure that the bounding box does not exceed limits.
     * <p>
     * 
     * The method allows for the {@link #boundingBoxOfTrackSpanningCandidates bounding box} to grow larger than
     * {@link #CANDIDATE_FILTER_DISTANCE} in {@link Bounds#getDiameter() diameter}, assuming this is only temporary and
     * caused by fixes added to or replaced in the track while candidate updates were going on and assuming that this
     * will be fixed by calls to {@link #tryToAddFix(GPSFixMoving, Set, Set, NavigableSet, boolean)} later.
     */
    private void refreshBoundingBox() {
        Bounds newBounds;
        newBounds = recomputeBounds(/* tolerateTemporaryBoundingBoxExcess */ true);
        assert isEmpty() || newBounds != null;
        boundingBoxOfTrackSpanningCandidates = newBounds;
    }

    /**
     * @param tolerateTemporaryBoundingBoxExcess
     *            if {@code true}, fixes will be added to the bounding box returned no matter its
     *            {@link Bounds#getDiameter() diameter}
     * @return {@code null} if empty or the bounds would exceed a {@link Bounds#getDiameter() diameter} of
     *         {@link StationarySequence#CANDIDATE_FILTER_DISTANCE}; otherwise the {@link Bounds} containing all fixed
     *         starting with the first and ending at the last {@link #candidates} in this sequence.
     */
    private Bounds recomputeBounds(boolean tolerateTemporaryBoundingBoxExcess) {
        Bounds newBounds;
        if (isEmpty()) {
            newBounds = null;
        } else {
            newBounds = createNewBounds(getFirst());
            newBounds = computeExtendedBoundsForFixesBetweenCandidates(getFirst(), getLast(), newBounds, tolerateTemporaryBoundingBoxExcess);
        }
        return newBounds;
    }

    /**
     * Creates new {@link Bounds} based on the estimated position on the {@link #track} at the {@code candidate}'s time point.
     * If no estimated position can be obtained, {@code null} is returned.
     */
    private Bounds createNewBounds(Candidate candidate) {
        final Position estimatedPosition = track.getEstimatedPosition(candidate.getTimePoint(), /* extrapolate */ true);
        return estimatedPosition == null ? null : new BoundsImpl(estimatedPosition);
    }

    Candidate getFirst() {
        return candidates.first();
    }
    
    Candidate getLast() {
        return candidates.last();
    }
    
    /**
     * A fix was added to the track in the time range spanned by this stationary sequence. One of the following cases
     * applied:
     * <ul>
     * <li>Adding the fix to the {@link #boundingBoxOfTrackSpanningCandidates} keeps the bounding box's
     * {@link Bounds#getDiameter() diameter} within {@link #CANDIDATE_FILTER_DISTANCE thresholds}. Only the bounding box
     * update is performed, and no change to the candidates that pass this filter is applied.</li>
     * <li>Adding the fix enlarged the bounding box beyond thresholds. This stationary sequence needs splitting. This
     * object keeps its {@link #getFirst() first} candidate and all further candidates up to but excluding the time
     * point of the {@code newFix}. (Note: the first candidate may still be exactly at the fix, but a resulting sequence
     * with only one candidate will be removed anyway.) A second, new stationary sequence is created for all remaining
     * candidates if there are at least two of them. The changes to the filter results are announced by updating
     * {@code candidatesEffectivelyAdded} and {@code candidatesEffectivelyRemoved}.</li>
     * </ul>
     * 
     * @param stationarySequenceSetToUpdate
     *            when this method causes a change in what {@link #getFirst()} returns before and after the call,
     *            this method maintains the set referenced by this parameter accordingly, assuming that the position
     *            in the set may change, or, if this sequence runs empty, it has to be removed from the set altogether.
     *            Note, however, that in case a {@link StationarySequence} is returned by this method then it is not
     *            added by this method to the {@code stationarySequenceSetToUpdate}. This is the caller's responsibility.
     * 
     * @return {@code null} if no new {@link StationarySequence} resulted from any splitting activity (could be because
     *         no split took place, or the split didn't leave more than one candidate for a second sequence}; the new
     *         sequence created by a split otherwise.
     */
    public StationarySequence tryToAddFix(GPSFixMoving newFix, Set<Candidate> candidatesEffectivelyAdded,
            Set<Candidate> candidatesEffectivelyRemoved, NavigableSet<StationarySequence> stationarySequenceSetToUpdate,
            boolean isReplacement) {
        assert !newFix.getTimePoint().before(getFirst().getTimePoint());
        assert !newFix.getTimePoint().after(getLast().getTimePoint());
        final StationarySequence tailSequence;
        final Bounds newBounds;
        if (isReplacement) {
            // the fix could have replaced another one that was "far out," and the bounding box may shrink now;
            // later additions may unnecessarily split, so we need to refresh the bounding box in this case:
            newBounds = recomputeBounds(/* tolerateTemporaryBoundingBoxExcess */ false); // can be null only if bounds' diameter exceeds threshold
        } else {
            newBounds = boundingBoxOfTrackSpanningCandidates.extend(newFix.getPosition());
        }
        if (newBounds != null && newBounds.getDiameter().compareTo(CANDIDATE_FILTER_DISTANCE) < 0) {
            boundingBoxOfTrackSpanningCandidates = newBounds; // ...and we're done
            tailSequence = null;
        } else {
            // split:
            final Set<Candidate> oldValidCandidates = new TreeSet<>(candidateComparator);
            Util.addAll(getValidCandidates(), oldValidCandidates);
            final Candidate candidateForFixTimePoint = getCandidateMatchingTimePointOrCreateDummy(newFix.getTimePoint());
            SortedSet<Candidate> tailSet = candidates.tailSet(candidateForFixTimePoint); // will contain a candidate for newFix.getTimePoint() if such a candidate exists
            boolean tryToAddCandidateAtFixLater = false;
            if (!tailSet.isEmpty() && tailSet.first().getTimePoint().equals(candidateForFixTimePoint.getTimePoint())) {
                // new fix TimePoint matches exactly with that of a candidate in this stationary sequence;
                // construct the tailing stationary sequence
                // without this candidate to start with, then try whether it can be extended to the left:
                tryToAddCandidateAtFixLater = true;
                tailSet = candidates.tailSet(candidateForFixTimePoint, /* inclusive */ false);
                assert !tailSet.contains(candidateForFixTimePoint);
            }
            tailSequence = tailSet.isEmpty() ? null : createStationarySequence(tailSet);
            if (tailSequence != null && tryToAddCandidateAtFixLater) {
                tailSequence.tryToExtendBeforeFirst(candidates.floor(candidateForFixTimePoint),
                        new TreeSet<>(candidateComparator), new TreeSet<>(candidateComparator), stationarySequenceSetToUpdate);
            }
            // now remove the tail set candidates from this stationary sequence:
            final ArrayList<Candidate> fullTailSet = new ArrayList<>(candidates.tailSet(candidateForFixTimePoint));
            if (!fullTailSet.isEmpty() && fullTailSet.get(0) == getFirst()) {
                // all candidates will be removed from this sequence; the sequence must be removed from its containing set before removing the first candidate...
                stationarySequenceSetToUpdate.remove(this);
            }
            candidates.removeAll(fullTailSet);
            for (final Candidate candidateFromFullTailSet : fullTailSet) {
                Util.removeFromValueSet(candidatesByWaypoint, candidateFromFullTailSet.getWaypoint(), candidateFromFullTailSet);
                Util.removeFromValueSet(candidatesByWaypointSortedByAscendingProbability, candidateFromFullTailSet.getWaypoint(), candidateFromFullTailSet);
                assert isCandidatesConsistent(candidateFromFullTailSet);
            }
            if (size() == 1) {
                stationarySequenceSetToUpdate.remove(this);
            }
            // ...and it doesn't need adding because if it was removed, it's empty now.
            refreshBoundingBox();
            final Set<Candidate> newValidCandidates = new TreeSet<>(candidateComparator);
            Util.addAll(getValidCandidates(), newValidCandidates);
            if (tailSequence != null) { // this includes the possibility of a single candidate being added to the tail set
                Util.addAll(tailSequence.getValidCandidates(), newValidCandidates);
            }
            final Set<Candidate> candidatesAdded = new TreeSet<>(candidateComparator);
            candidatesAdded.addAll(newValidCandidates);
            candidatesAdded.removeAll(oldValidCandidates);
            final Set<Candidate> candidatesRemoved = new TreeSet<>(candidateComparator);
            candidatesRemoved.addAll(oldValidCandidates);
            candidatesRemoved.removeAll(newValidCandidates);
            candidatesEffectivelyAdded.addAll(candidatesAdded);
            candidatesEffectivelyRemoved.removeAll(candidatesAdded);
            candidatesEffectivelyRemoved.addAll(candidatesRemoved);
            candidatesEffectivelyAdded.removeAll(candidatesRemoved);
        }
        final StationarySequence result = tailSequence != null && tailSequence.size() >= 2 ? tailSequence : null;
        assert result == null || !this.getLast().getTimePoint().equals(result.getFirst().getTimePoint());
        return result;
    }

    /**
     * Precondition: the track between the {@code candidates} is valid for a stationary sequence, not leaving a bounding
     * box of diameter {@link #CANDIDATE_FILTER_DISTANCE}, and {@code candidates} contains at least one element.<p>
     * 
     * Constructs a new sequence from the candidates. No updates to {@link #candidates} are performed here.
     * 
     * @return a sequence with the candidate(s)
     */
    private StationarySequence createStationarySequence(SortedSet<Candidate> candidates) {
        final Set<Candidate> candidatesEffectivelyAdded = new TreeSet<>(candidateComparator);
        final Set<Candidate> candidatesEffectivelyRemoved = new TreeSet<>(candidateComparator);
        final Iterator<Candidate> candidateIterator = candidates.iterator();
        assert candidateIterator.hasNext();
        final StationarySequence result = new StationarySequence(candidateIterator.next(), candidateComparator, track);
        while (candidateIterator.hasNext()) {
            final Candidate nextCandidate = candidateIterator.next();
            boolean extensionOk = result.tryToExtendAfterLast(nextCandidate, candidatesEffectivelyAdded, candidatesEffectivelyRemoved);
            assert extensionOk;
            assert isCandidatesConsistent(nextCandidate);
        }
        return result;
    }

    /**
     * If this sequence contains a {@link Candidate} that matches the {@code timePoint} exactly, that candidate is
     * returned. Otherwise, a dummy candidate is created that has the {@code timePoint} provided.
     * <p>
     * 
     * Note that depending on the {@link #candidateComparator} used, two candidates are not necessarily considered equal
     * only because they have equal time points.
     */
    private Candidate getCandidateMatchingTimePointOrCreateDummy(TimePoint timePoint) {
        final Candidate dummy = new CandidateImpl(/* one-based index of waypoint */ 1, timePoint, /* probability */ 0, /* waypoint */ null);
        final Candidate floorCandidate = candidates.floor(dummy);
        final Candidate ceilingCandidate = candidates.ceiling(dummy);
        final Candidate result;
        if (floorCandidate != null && floorCandidate.getTimePoint().equals(timePoint)) {
            result = floorCandidate;
        } else if (ceilingCandidate != null && ceilingCandidate.getTimePoint().equals(timePoint)) {
            result = ceilingCandidate;
        } else {
            result = dummy;
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "Stationary Sequence "+candidates+", bounds diameter "+boundingBoxOfTrackSpanningCandidates.getDiameter();
    }
    
    /**
     * Creates a dummy time point that can be used for searching in {@link #candidates} by time point. It
     * uses {@code 1} for the one-based waypoint index, {@code null} for the waypoint and {@code 0.0} for its
     * probability.
     */
    static Candidate createDummyCandidate(TimePoint timePoint) {
        return new CandidateImpl(/* one-based index of waypoint */ 1, timePoint, /* probability */ 0, /* waypoint */ null);
    }

    public Iterable<Candidate> getAllCandidates() {
        return Collections.unmodifiableCollection(candidates);
    }

    /**
     * Tells whether the {@link #candidates} set contains {@code candidate}. Containment is decided based on
     * the {@link #candidateComparator}'s notion of equality. 
     */
    public boolean contains(Candidate candidate) {
        return candidates.contains(candidate);
    }
}

