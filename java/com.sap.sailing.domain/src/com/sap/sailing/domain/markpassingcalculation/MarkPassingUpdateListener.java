package com.sap.sailing.domain.markpassingcalculation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.util.IntHolder;

/**
 * Listens for changes that might affect the MarkPassingCalculator: new Fixes of a Competitor or a Mark, updated
 * Markpassings or Waypoints and the end of the race. New information is put in to a queue to be evaluated by the
 * {@link MarkPassingCalculator}. To have only one queue each fix is stored in a StorePositionUpdateStrategy, which than
 * sorts itself in the MPC.
 * 
 * @author Nicolas Klose
 * 
 */
public class MarkPassingUpdateListener extends AbstractRaceChangeListener {
    private final MarkPassingCalculator markPassingCalculator;

    /**
     * Adds itself automatically as a Listener on the <code>race</code> and its course.
     * 
     * @param markPassingCalculator
     *            the mark passing calculator to send updates to for enqueuing
     */
    public MarkPassingUpdateListener(DynamicTrackedRace race, MarkPassingCalculator markPassingCalculator) {
        this.markPassingCalculator = markPassingCalculator;
        race.addListener(this);
    }

    @Override
    public void competitorPositionChanged(final GPSFixMoving fix, final Competitor competitor, AddResult addedOrReplaced) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                List<GPSFixMoving> list = competitorFixes.get(competitor);
                if (list == null) {
                    list = new ArrayList<>();
                    competitorFixes.put(competitor, list);
                }
                list.add(fix);
                if (addedOrReplaced == AddResult.REPLACED) {
                    List<GPSFixMoving> listOfReplacingFixes = competitorFixesThatReplacedExistingOnes.get(competitor);
                    if (listOfReplacingFixes == null) {
                        listOfReplacingFixes = new ArrayList<>();
                        competitorFixesThatReplacedExistingOnes.put(competitor, listOfReplacingFixes);
                    }
                    listOfReplacingFixes.add(fix);
                }
            }
        });
    }

    @Override
    public void markPositionChanged(final GPSFix fix, final Mark mark, boolean firstInTrack, AddResult addedOrReplaced) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                List<GPSFix> list = markFixes.get(mark);
                if (list == null) {
                    list = new ArrayList<>();
                    markFixes.put(mark, list);
                }
                list.add(fix);
            }
        });
    }

    @Override
    public void waypointAdded(final int zeroBasedIndex, final Waypoint waypointThatGotAdded) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes,List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                addedWaypoints.add(waypointThatGotAdded);
                if (smallestChangedWaypointIndex.value == -1 || smallestChangedWaypointIndex.value > zeroBasedIndex) {
                    smallestChangedWaypointIndex.value = zeroBasedIndex;
                }
            }
        });
    }

    @Override
    public void waypointRemoved(final int zeroBasedIndex, final Waypoint waypointThatGotRemoved) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                removedWaypoints.add(waypointThatGotRemoved);
                if (smallestChangedWaypointIndex.value == -1 || smallestChangedWaypointIndex.value > zeroBasedIndex) {
                    smallestChangedWaypointIndex.value = zeroBasedIndex;
                }
            }
        });
    }

    public void addFixedPassing(final Competitor c, final Integer zeroBasedIndexOfWaypoint,
            final TimePoint timePointOfFixedPassing) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                fixedMarkPassings.add(new Triple<Competitor, Integer, TimePoint>(c, zeroBasedIndexOfWaypoint,
                        timePointOfFixedPassing));
            }
        });
    }

    public void removeFixedPassing(final Competitor c, final Integer zeroBasedIndexOfWaypoint) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                removedMarkPassings.add(new Pair<Competitor, Integer>(c, zeroBasedIndexOfWaypoint));
            }

        });
    }

    public void addSuppressedPassing(final Competitor c, final Integer zeroBasedIndexOfWaypoint) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                suppressedMarkPassings.add(new Pair<Competitor, Integer>(c, zeroBasedIndexOfWaypoint));

            }
        });
    }

    public void removeSuppressedPassing(final Competitor c) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                unSuppressedMarkPassings.add(c);
            }
        });
    }

    @Override
    public void startOfRaceChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                final Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> newAndRemovedCandidatesPerCompetitor =
                        candidateFinder.getCandidateDeltasAfterRaceStartTimeChange();
                for (final Entry<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> i : newAndRemovedCandidatesPerCompetitor.entrySet()) {
                    candidateChooser.calculateMarkPassDeltas(i.getKey(), i.getValue().getA(), i.getValue().getB());
                }
            }
        });
    }

    @Override
    public void startOfTrackingChanged(TimePoint oldStartOfTracking, TimePoint newStartOfTracking) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                final Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> newAndRemovedCandidatesPerCompetitor =
                        candidateFinder.getCandidateDeltasAfterStartOfTrackingChange();
                for (final Entry<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> i : newAndRemovedCandidatesPerCompetitor.entrySet()) {
                    candidateChooser.calculateMarkPassDeltas(i.getKey(), i.getValue().getA(), i.getValue().getB());
                }
            }
        });
    }

    @Override
    public void finishedTimeChanged(TimePoint oldFinishedTime, TimePoint newFinishedTime) {
        markPassingCalculator.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes,
                    Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints,
                    List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings,
                    List<Pair<Competitor, Integer>> removedMarkPassings, List<Pair<Competitor, Integer>> suppressedMarkPassings,
                    List<Competitor> unSuppressedMarkPassings, CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                final Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> newAndRemovedCandidatesPerCompetitor =
                        candidateFinder.getCandidateDeltasAfterRaceFinishedTimeChange(oldFinishedTime, newFinishedTime);
                for (final Entry<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> i : newAndRemovedCandidatesPerCompetitor.entrySet()) {
                    candidateChooser.calculateMarkPassDeltas(i.getKey(), i.getValue().getA(), i.getValue().getB());
                }
            }
        });
    }
    
    @Override
    public String toString() {
        return getClass().getName()+" for "+markPassingCalculator;
    }
}
