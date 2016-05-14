package com.sap.sailing.domain.markpassingcalculation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sailing.domain.markpassingcalculation.CandidateChooser;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * The standard implementation of {@link CandidateChooser}. A graph is created, with each {@link Candidate} as a
 * vertices, all between two proxy Candidates, <code>start</code> and <code>end</code> using {@link Edge}s. These are
 * only created if the both the waypoints and the the timepoints are in chronological order and the distance-based
 * estimation is good enough. They are saved in <code>allEdges</code>, a map in which every candidate is the key to a
 * list of all the edges that start at this candidate. The shortest path between the proxy-Candidates is the most likely
 * sequence of {@link MarkPassing}s. Every time new candidates arrive, the start time of the race is checked. If it has
 * changed, the proxy start and all edges containing it are updated.
 * 
 * @author Nicolas Klose
 * 
 */
public class CandidateChooserImpl implements CandidateChooser {
    /**
     * Earlier finish mark passings are to be preferred over later ones if they otherwise seem equally likely. While the
     * {@link #getProbabilityOfActualDistanceGivenGreatCircleDistance(Distance, Distance, double)} method should usually
     * assign an equal probability of 1.0 for edges whose distance is in the range of 1.0 and
     * {@link #MAX_REASONABLE_RATIO_BETWEEN_DISTANCE_TRAVELED_AND_LEG_LENGTH} times the leg length, for the finishing
     * leg the candidates that require more distance than the minimum distance required receive an increasing penalty.
     * The maximum penalty for finishing candidates that have required
     * {@link #MAX_REASONABLE_RATIO_BETWEEN_DISTANCE_TRAVELED_AND_LEG_LENGTH} times the leg distance is expressed by
     * this constant.
     */
    private static final double PENALTY_FOR_LATEST_FINISH_PASSING = 0.95;

    /**
     * Distance ratios of actual distance traveled and leg length above this threshold will receive
     * penalties on their probability. Ratios below 1.0 receive the ratio as the penalty.
     * See {@link #getDistanceEstimationBasedProbability(Competitor, Candidate, Candidate)}.
     */
    private static final double MAX_REASONABLE_RATIO_BETWEEN_DISTANCE_TRAVELED_AND_LEG_LENGTH = 2.0;

    /**
     * Start mark passings will be considered this much before the actual race start. The race start
     * as identified by {@link TrackedRace#getStartOfRace()} is therefore {@link #EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME}
     * after {@link #raceStartTime}.
     */
    private static final Duration EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME = new MillisecondsDurationImpl(5000);
    
    /**
     * The duration after which a start mark passing's probability is considered only 50%. A perfect start mark
     * passing happening exactly at the race start time has time-wise probability of 1.0. Another delay of this much
     * lets the probability drop to 1/3, and so on.
     */
    private static final Duration DELAY_AFTER_WHICH_PROBABILITY_OF_START_HALVES = Duration.ONE_MINUTE;
    
    private static final double MINIMUM_PROBABILITY = Edge.getPenaltyForSkipping();

    private static final Logger logger = Logger.getLogger(CandidateChooserImpl.class.getName());

    private Map<Competitor, Map<Waypoint, MarkPassing>> currentMarkPasses = new HashMap<>();
    private Map<Competitor, Map<Candidate, Set<Edge>>> allEdges = new HashMap<>();
    private Map<Competitor, Set<Candidate>> candidates = new HashMap<>();
    private Map<Competitor, NavigableSet<Candidate>> fixedPassings = new HashMap<>();
    private Map<Competitor, Integer> suppressedPassings = new HashMap<>();
    
    /**
     * Set to {@link #EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME} milliseconds before the actual race start,
     * in case the actual start of race is known; {@code null} otherwise.
     */
    private TimePoint raceStartTime;
    private final WaypointPositionAndDistanceCache waypointPositionAndDistanceCache;
    
    /**
     * An artificial proxy candidate that comes before the start mark passing. Its time point is set to
     * {@link #EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME} milliseconds before the race start time or <code>null</code>
     * in case the race start time is not known.
     */
    private final CandidateWithSettableTime start;
    private final CandidateWithSettableWaypointIndex end;
    private final DynamicTrackedRace race;
    
    public CandidateChooserImpl(DynamicTrackedRace race) {
        this.race = race;
        waypointPositionAndDistanceCache = new WaypointPositionAndDistanceCache(race, Duration.ONE_MINUTE);
        final TimePoint startOfRaceWithoutInference = race.getStartOfRace(/* inferred */ false);
        raceStartTime = startOfRaceWithoutInference != null ? startOfRaceWithoutInference.
                minus(EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME) : null;
        start = new CandidateWithSettableTime(/* Index */0, raceStartTime, /* Probability */1, /* Waypoint */null);
        end = new CandidateWithSettableWaypointIndex(race.getRace().getCourse().getNumberOfWaypoints() + 1, /* TimePoint */null,
                /* Probability */1, /* Waypoint */null);
        candidates = new HashMap<>();
        List<Candidate> startAndEnd = Arrays.asList(start, end);
        for (Competitor c : race.getRace().getCompetitors()) {
            candidates.put(c, Collections.synchronizedSet(new TreeSet<Candidate>()));
            final HashMap<Waypoint, MarkPassing> currentMarkPassesForCompetitor = new HashMap<Waypoint, MarkPassing>();
            currentMarkPasses.put(c, currentMarkPassesForCompetitor);
            // in case the tracked race already has mark passings, e.g., from another mark passing calculator,
            // ensure consistency of the currentMarkPasses map with the TrackedRace:
            for (final Waypoint w : race.getRace().getCourse().getWaypoints()) {
                final MarkPassing mp = race.getMarkPassing(c, w);
                if (mp != null) {
                    currentMarkPassesForCompetitor.put(w, mp);
                }
            }
            TreeSet<Candidate> fixedPasses = new TreeSet<Candidate>(new Comparator<Candidate>() {
                @Override
                public int compare(Candidate o1, Candidate o2) {
                    final int result;
                    if (o1 == null) {
                        if (o2 == null) {
                            result = 0;
                        } else {
                            result = -1;
                        }
                    } else if (o2 == null) {
                        result = 1;
                    } else {
                        result = o1.getOneBasedIndexOfWaypoint() - o2.getOneBasedIndexOfWaypoint();
                    }
                    return result;
                }
            });
            fixedPassings.put(c, fixedPasses);
            allEdges.put(c, new HashMap<Candidate, Set<Edge>>());
            fixedPasses.addAll(startAndEnd);
            addCandidates(c, startAndEnd);
        }
    }

    @Override
    public void calculateMarkPassDeltas(Competitor c, Iterable<Candidate> newCans, Iterable<Candidate> oldCans) {
       final TimePoint startOfRace = race.getStartOfRace(/* inference */ false);
        if (startOfRace != null) {
            if (raceStartTime == null || !startOfRace.minus(EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME).equals(raceStartTime)) {
                raceStartTime = startOfRace.minus(EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME);
                List<Candidate> startList = new ArrayList<>();
                startList.add(start);
                for (Competitor com : candidates.keySet()) {
                    removeCandidates(com, startList);
                }
                start.setTimePoint(raceStartTime);
                for (Competitor com : allEdges.keySet()) {
                    addCandidates(com, startList);
                }
            }
        }
        removeCandidates(c, oldCans);
        addCandidates(c, newCans);
        findShortestPath(c);
    }

    @Override
    public void removeWaypoints(Iterable<Waypoint> waypoints) {
        for (Competitor c : currentMarkPasses.keySet()) {
            for (Waypoint w : waypoints) {
                currentMarkPasses.get(c).remove(w);
            }
        }
    }
    
    @Override
    public void updateEndProxyNodeWaypointIndex() {
        end.setOneBasedWaypointIndex(race.getRace().getCourse().getNumberOfWaypoints()+1);
    }

    @Override
    public void setFixedPassing(Competitor c, Integer zeroBasedIndexOfWaypoint, TimePoint t) {
        Candidate fixedCan = new CandidateImpl(zeroBasedIndexOfWaypoint + 1, t, 1, Util.get(race.getRace().getCourse().getWaypoints(), zeroBasedIndexOfWaypoint));
        NavigableSet<Candidate> fixed = fixedPassings.get(c);
        if (fixed != null) { // can only set the mark passing if the competitor is still part of this race
            if (!fixed.add(fixedCan)) {
                Candidate old = fixed.ceiling(fixedCan);
                fixed.remove(old);
                removeCandidates(c, Arrays.asList(old));
                fixed.add(fixedCan);
            }
            addCandidates(c, Arrays.asList(fixedCan));
            findShortestPath(c);
        }
    }

    @Override
    public void removeFixedPassing(Competitor c, Integer zeroBasedIndexOfWaypoint) {
        Candidate toRemove = null;
        for (Candidate can : fixedPassings.get(c)) {
            if (can.getOneBasedIndexOfWaypoint() - 1 == zeroBasedIndexOfWaypoint) {
                toRemove = can;
                break;
            }
        }
        if (toRemove != null) {
            fixedPassings.get(c).remove(toRemove);
            removeCandidates(c, Arrays.asList(toRemove));
            findShortestPath(c);
        }
    }

    @Override
    public void suppressMarkPassings(Competitor c, Integer zeroBasedIndexOfWaypoint) {
        suppressedPassings.put(c, zeroBasedIndexOfWaypoint);
        findShortestPath(c);
    }

    @Override
    public void stopSuppressingMarkPassings(Competitor c) {
        suppressedPassings.remove(c);
        findShortestPath(c);
    }

    private void createNewEdges(Competitor c, Iterable<Candidate> newCandidates) {
        final Boolean isGateStart = race.isGateStart();
        Map<Candidate, Set<Edge>> edges = allEdges.get(c);
        for (Candidate newCan : newCandidates) {
            final Set<Candidate> competitorCandidates = candidates.get(c);
            synchronized (competitorCandidates) {
                for (Candidate oldCan : competitorCandidates) {
                    final Candidate early;
                    final Candidate late;
                    if (oldCan.getOneBasedIndexOfWaypoint() < newCan.getOneBasedIndexOfWaypoint()) {
                        early = oldCan;
                        late = newCan;
                    } else if (oldCan.getOneBasedIndexOfWaypoint() > newCan.getOneBasedIndexOfWaypoint()) {
                        late = oldCan;
                        early = newCan;
                    } else {
                        continue; // don't create edge from/to same waypoint
                    }
    
                    final double estimatedDistanceProbability;
                    final double startTimingProbability;
                    if (early == start) {
                        // An edge starting at the start proxy node. If the late candidate is for a start mark passing,
                        // determine a probability not based on distance traveled but based on the
                        // time difference between scheduled start time and candidate's time point. If the "late" candidate
                        // is not for the start mark/line, meaning that mark passings including the actual start are
                        // skipped, as usual use getDistanceEstimationBasedProbability assuming a start mark passing at
                        // the race's start time.
                        if (isGateStart == Boolean.TRUE || start.getTimePoint() == null) { // TODO for gate start read gate timing and scale probability accordingly
                            startTimingProbability = 1; // no start time point known; all candidate time points equally likely
                            estimatedDistanceProbability = 1; // can't tell distance sailed either because we don't know the start time
                        } else {
                            // no gate start and we know the race start time
                            if (late.getWaypoint() == race.getRace().getCourse().getFirstWaypoint()) {
                                // no skips; going from the start proxy node to a candidate for the start mark passing;
                                // calculate the probability for the start being the start given its timing and multiply
                                // with the estimation for the distance-based probability:
                                final Duration timeGapBetweenStartOfRaceAndCandidateTimePoint = early.getTimePoint()
                                        .plus(EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME).until(late.getTimePoint()).abs();
                                // Being DELAY_AFTER_WHICH_PROBABILITY_OF_START_HALVES off means a probability of 1/2; being twice this time
                                // off means 1/3, and so on
                                startTimingProbability = DELAY_AFTER_WHICH_PROBABILITY_OF_START_HALVES.divide(
                                        DELAY_AFTER_WHICH_PROBABILITY_OF_START_HALVES.plus(
                                                timeGapBetweenStartOfRaceAndCandidateTimePoint));
                                estimatedDistanceProbability = 1;
                            } else {
                                startTimingProbability = 1; // can't really tell how well the start time was matched when
                                                              // we don't have a start candidate
                                estimatedDistanceProbability = late == end ? 1 : getDistanceEstimationBasedProbability(c, early, late);
                            }
                        }
                    } else {
                        startTimingProbability = 1; // no penalty for any start time difference because this edge doesn't cover a start
                        if (late == end) {
                            // final edge; we don't know anything about distances for the end proxy node
                            estimatedDistanceProbability = 1;
                        } else {
                            estimatedDistanceProbability = getDistanceEstimationBasedProbability(c, early, late);
                        }
                    }
                    // If one of the candidates is fixed, the edge is always created unless they travel backwards in time.
                    // Otherwise the edge is only created if the distance estimation, which can be calculated as long as the
                    // candidates are not the proxy and or start is close enough to the actual distance sailed.
                    final NavigableSet<Candidate> fixed = fixedPassings.get(c);
                    // TODO this comparison does not exactly implement the condition "if distance is more likely than skipping"
                    if (travelingForwardInTimeOrUnknown(early, late) &&
                            (fixed.contains(early) || fixed.contains(late) || estimatedDistanceProbability > MINIMUM_PROBABILITY)) {
                        addEdge(edges, new Edge(early, late, startTimingProbability * estimatedDistanceProbability, race.getRace().getCourse().getNumberOfWaypoints()));
                    }
                }
            }
        }
    }

    private boolean travelingForwardInTimeOrUnknown(Candidate early, Candidate late) {
        return early.getTimePoint() == null || late.getTimePoint() == null || early.getTimePoint().before(late.getTimePoint());
    }

    private void addEdge(Map<Candidate, Set<Edge>> edges, Edge e) {
        logger.finest("Adding "+ e.toString());
        Set<Edge> edgeSet = edges.get(e.getStart());
        if (edgeSet == null) {
            edgeSet = new HashSet<>();
            edges.put(e.getStart(), edgeSet);
        }
        edgeSet.add(e); // FIXME what about edges that should replace an edge between the same two candidates? Will those edges somehow be removed?
    }

    /**
     * Calculates the most likely series of {@link MarkPassings} using the edges in {@link allEdges}. These are saved in
     * {@link #currentMarkPasses} and the {@link DynamicTrackedRace} is
     * {@link DynamicTrackedRace#updateMarkPassings(Competitor, Iterable) notified}.<p>
     * 
     * The algorithm works out optimal solutions between fixed mark passings. By default, start and end proxy
     * candidates are the only fixed elements. If more fixed elements are provided, the algorithm solves the
     * optimization problem separately for each segment and concatenates the solutions.
     */
    private void findShortestPath(Competitor c) {
        Map<Candidate, Set<Edge>> allCompetitorEdges = allEdges.get(c);
        SortedSet<Candidate> mostLikelyCandidates = new TreeSet<>();
        NavigableSet<Candidate> fixedPasses = fixedPassings.get(c);
        Candidate startOfFixedInterval = fixedPasses.first();
        Candidate endOfFixedInterval = fixedPasses.higher(startOfFixedInterval);
        Integer zeroBasedIndexOfWaypoint = suppressedPassings.get(c);
        Integer oneBasedIndexOfSuppressedWaypoint = zeroBasedIndexOfWaypoint != null ? zeroBasedIndexOfWaypoint + 1 : end
                .getOneBasedIndexOfWaypoint();
        while (endOfFixedInterval != null) {
            if (oneBasedIndexOfSuppressedWaypoint <= endOfFixedInterval.getOneBasedIndexOfWaypoint()) {
                endOfFixedInterval = end;
            }
            NavigableSet<Util.Pair<Edge, Double>> currentEdgesMoreLikelyFirst = new TreeSet<>(new Comparator<Util.Pair<Edge, Double>>() {
                @Override
                public int compare(Util.Pair<Edge, Double> o1, Util.Pair<Edge, Double> o2) {
                    int result = o2.getB().compareTo(o1.getB());
                    return result != 0 ? result : o1.getA().compareTo(o2.getA());
                }
            });
            Map<Candidate, Util.Pair<Candidate, Double>> candidateWithParentAndHighestTotalProbability = new HashMap<>();
            int indexOfEndOfFixedInterval = endOfFixedInterval.getOneBasedIndexOfWaypoint();

            boolean endFound = false;
            currentEdgesMoreLikelyFirst.add(new Util.Pair<Edge, Double>(new Edge(new CandidateImpl(-1, null, /* estimated distance probability */ 1, null), startOfFixedInterval,
                    1, race.getRace().getCourse().getNumberOfWaypoints()), 1.0));
            while (!endFound) {
                Util.Pair<Edge, Double> mostLikelyEdgeWithProbability = currentEdgesMoreLikelyFirst.pollFirst();
                if (mostLikelyEdgeWithProbability == null) {
                    endFound = true;
                } else {
                    Edge currentMostLikelyEdge = mostLikelyEdgeWithProbability.getA();
                    Double currentHighestProbability = mostLikelyEdgeWithProbability.getB();
                    // If the shortest path to this candidate is already known the new edge is not added.
                    if (!candidateWithParentAndHighestTotalProbability.containsKey(currentMostLikelyEdge.getEnd())) {
                        // The most likely edge taking us to currentMostLikelyEdge.getEnd() is found. Remember it.
                        candidateWithParentAndHighestTotalProbability.put(currentMostLikelyEdge.getEnd(), new Util.Pair<Candidate, Double>(
                                currentMostLikelyEdge.getStart(), currentHighestProbability));
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.finest("Added "+ currentMostLikelyEdge + " as most likely edge for " + c);
                        }
                        endFound = currentMostLikelyEdge.getEnd() == endOfFixedInterval;
                        if (!endFound) {
                            // the end of the segment was not yet found; add edges leading away from
                            // currentMostLikelyEdge.getEnd(), multiplying up their probabilities with the probability
                            // of reaching currentMostLikelyEdge.getEnd()
                            Set<Edge> edgesForNewCandidate = allCompetitorEdges.get(currentMostLikelyEdge.getEnd());
                            if (edgesForNewCandidate != null) {
                                for (Edge e : edgesForNewCandidate) {
                                    int oneBasedIndexOfEndOfEdge = e.getEnd().getOneBasedIndexOfWaypoint();
                                    // only add edge if it stays within the current segment, not exceeding
                                    // the next fixed mark passing
                                    if (oneBasedIndexOfEndOfEdge <= indexOfEndOfFixedInterval
                                            && (oneBasedIndexOfEndOfEdge < oneBasedIndexOfSuppressedWaypoint || e.getEnd() == end)) {
                                        currentEdgesMoreLikelyFirst.add(new Util.Pair<Edge, Double>(e, currentHighestProbability * e.getProbability()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            final Pair<Candidate, Double> bestCandidateAndProbabilityForEndOfFixedInterval = candidateWithParentAndHighestTotalProbability.get(endOfFixedInterval);
            Candidate marker = bestCandidateAndProbabilityForEndOfFixedInterval == null ? null : bestCandidateAndProbabilityForEndOfFixedInterval.getA();
            while (marker != null && marker.getOneBasedIndexOfWaypoint() > 0) {
                mostLikelyCandidates.add(marker);
                marker = candidateWithParentAndHighestTotalProbability.get(marker).getA();
            }
            startOfFixedInterval = endOfFixedInterval;
            endOfFixedInterval = fixedPasses.higher(endOfFixedInterval);
        }
        boolean changed = false;
        Map<Waypoint, MarkPassing> currentPasses = currentMarkPasses.get(c);
        if (currentPasses.size() != mostLikelyCandidates.size()) {
            changed = true;
        } else {
            for (Candidate can : mostLikelyCandidates) {
                MarkPassing currentPassing = currentPasses.get(can.getWaypoint());
                if (currentPassing == null || currentPassing.getTimePoint().compareTo(can.getTimePoint()) != 0) {
                    changed = true;
                    break;
                }
            }
        }
        if (changed) {
            currentPasses.clear();
            List<MarkPassing> newMarkPassings = new ArrayList<>();
            for (Candidate can : mostLikelyCandidates) {
                if (can != start && can != end) {
                    MarkPassingImpl newMarkPassing = new MarkPassingImpl(can.getTimePoint(), can.getWaypoint(), c);
                    currentPasses.put(newMarkPassing.getWaypoint(), newMarkPassing);
                    newMarkPassings.add(newMarkPassing);
                }
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Updating MarkPasses for " + c + " in case "+race.getRace().getName());
            }
            race.updateMarkPassings(c, newMarkPassings);
        }
    }

    /**
     * The distance between waypoints is used to estimate the distance that should be covered between these two
     * candidates. This estimation is then compared to the distance actually sailed. A distance smaller than the
     * estimation is (aside from a small tolerance) impossible, a distance larger get increasingly unlikely.
     */
    private double getDistanceEstimationBasedProbability(Competitor c, Candidate c1, Candidate c2) {
        final double result;
        assert c1.getOneBasedIndexOfWaypoint() < c2.getOneBasedIndexOfWaypoint();
        assert c2 != end;
        Waypoint first;
        final TimePoint middleOfc1Andc2 = new MillisecondsTimePoint(c1.getTimePoint().plus(c2.getTimePoint().asMillis()).asMillis() / 2);
        if (c1.getOneBasedIndexOfWaypoint() == 0) {
            first = race.getRace().getCourse().getFirstWaypoint();
        } else {
            first = c1.getWaypoint();
        }
        final Waypoint second = c2.getWaypoint();
        final Distance totalGreatCircleDistance = getMinimumTotalGreatCircleDistanceBetweenWaypoints(first, second, middleOfc1Andc2);
        if (totalGreatCircleDistance == null) {
            result = 0; // no distance known; cannot tell; low probability
        } else {
            final Distance actualDistanceTraveled = race.getTrack(c).getDistanceTraveled(c1.getTimePoint(), c2.getTimePoint());
            final double probabilityForMaxReasonableRatioBetweenDistanceTraveledAndLegLength =
                    c2.getWaypoint() == race.getRace().getCourse().getLastWaypoint() ? PENALTY_FOR_LATEST_FINISH_PASSING : 1.0;
            result = getProbabilityOfActualDistanceGivenGreatCircleDistance(totalGreatCircleDistance, actualDistanceTraveled,
                    probabilityForMaxReasonableRatioBetweenDistanceTraveledAndLegLength);
        }
        return result;
    }

    /**
     * Based on a direct great-circle distance between waypoints and an actual distance sailed, determines how likely it
     * is that this distance sailed could have happened between those waypoints. For a reaching leg, this would be based
     * on a straight comparison of the numbers. However, with upwind and downwind legs and boats not going from mark to
     * mark on a great circle segment, distances sailed will exceed the great line circle distances.
     * <p>
     * 
     * A smaller distance than great circle from mark to mark is getting the more unlikely the shorter the distance is,
     * somewhere between the distance estimated and twice that is likely, and anything greater than that gradually
     * becomes unlikely.
     * <p>
     * 
     * Finishing legs are a special case. Here, we'd like to prefer an earlier candidate over a later one as long as the
     * earlier one still leads to a "reasonable" distance sailed, particularly if two such candidates are otherwise
     * equally highly likely. Therefore, this method accepts a parameter
     * {@code probabilityForMaxReasonableRatioBetweenDistanceTraveledAndLegLength} that configures a slight "slope" in
     * the interval that for non-finishing legs receives a constant probability of 1.0. This slope will give 1.0 for the
     * shortest possible distance and slightly less for the longest distance that for non-finishing legs would still
     * result in 1.0. The probabilities of even greater distances then starts contiguously at the end value of that
     * slope.
     * 
     * @return a number between 0 and 1 with 1 representing a "fair chance" that the actual distance sailed could have
     *         been sailed for the given great circle distance; 1 is returned for actual distances being in the range of
     *         1..2 times the great circle distance. Actual distances outside this interval reduce probability linearly
     *         for smaller distances (gradient 0.5) and varies with the one over the ratio for distances that exceed
     *         twice the great circle distance.
     */
    private double getProbabilityOfActualDistanceGivenGreatCircleDistance(Distance totalGreatCircleDistance, Distance actualDistanceTraveled,
            double probabilityForMaxReasonableRatioBetweenDistanceTraveledAndLegLength) {
        final double result;
        final double ratio = actualDistanceTraveled.getMeters() / totalGreatCircleDistance.getMeters();
        // A smaller distance than great circle from mark to mark is very unlikely, somewhere between the distance
        // estimated and double that is likely and anything greater than that gradually becomes unlikely
        if (ratio <= 1) {
            result = ratio;
        } else if (ratio <= MAX_REASONABLE_RATIO_BETWEEN_DISTANCE_TRAVELED_AND_LEG_LENGTH) {
            result = 1 - (1-probabilityForMaxReasonableRatioBetweenDistanceTraveledAndLegLength)*(ratio-1)/(MAX_REASONABLE_RATIO_BETWEEN_DISTANCE_TRAVELED_AND_LEG_LENGTH-1);
        } else {
            // start at probability probabilityForMaxReasonableRatioBetweenDistanceTraveledAndLegLength for ratio==MAX_REASONABLE_RATIO_BETWEEN_DISTANCE_TRAVELED_AND_LEG_LENGTH
            result = probabilityForMaxReasonableRatioBetweenDistanceTraveledAndLegLength/(ratio-MAX_REASONABLE_RATIO_BETWEEN_DISTANCE_TRAVELED_AND_LEG_LENGTH + 1.);
        }
        return result;
    }

    private Distance getMinimumTotalGreatCircleDistanceBetweenWaypoints(Waypoint first, final Waypoint second, final TimePoint timePoint) {
        Distance totalGreatCircleDistance = new MeterDistance(0);
        boolean legsAreBetweenCandidates = false;
        for (TrackedLeg leg : race.getTrackedLegs()) {
            Waypoint from = leg.getLeg().getFrom();
            if (from == second) {
                break;
            }
            if (from == first) {
                legsAreBetweenCandidates = true;
            }
            if (legsAreBetweenCandidates) {
                final Distance minimumDistanceToNextWaypoint = waypointPositionAndDistanceCache.getMinimumDistance(from, leg.getLeg().getTo(), timePoint);
                if (minimumDistanceToNextWaypoint == null) {
                    totalGreatCircleDistance = null;
                    break;
                } else {
                    // subtract twice the typical error margin of the position fixes of the marks, assuming that the leg could have been
                    // a little shorter in fact:
                    totalGreatCircleDistance = totalGreatCircleDistance.add(minimumDistanceToNextWaypoint).add(GPSFix.TYPICAL_HDOP.scale(-2));
                }
            }
        }
        return totalGreatCircleDistance;
    }

    private void addCandidates(Competitor c, Iterable<Candidate> newCandidates) {
        for (Candidate can : newCandidates) {
            candidates.get(c).add(can);
        }
        createNewEdges(c, newCandidates);
    }

    private void removeCandidates(Competitor c, Iterable<Candidate> wrongCandidates) {
        for (Candidate can : wrongCandidates) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Removing all edges containing " + can + "of "+ c);
            }
            candidates.get(c).remove(can);
            Map<Candidate, Set<Edge>> edges = allEdges.get(c);
            edges.remove(can);
            for (Set<Edge> set : edges.values()) {
                for (Iterator<Edge> i = set.iterator(); i.hasNext();) {
                    final Edge e = i.next();
                    if (e.getStart() == can || e.getEnd() == can) {
                        i.remove();
                    }
                }
            }
        }
    }

    private static class CandidateWithSettableTime extends CandidateImpl {
        private static final long serialVersionUID = -1792983349299883266L;
        private TimePoint variableTimePoint;
        
        public CandidateWithSettableTime(int oneBasedIndexOfWaypoint, TimePoint p, double distanceProbability, Waypoint w) {
            super(oneBasedIndexOfWaypoint, /* time point */ null, distanceProbability, w);
            this.variableTimePoint = p;
        }

        public void setTimePoint(TimePoint t) {
            variableTimePoint = t;
        }
        
        @Override
        public TimePoint getTimePoint() {
            return variableTimePoint;
        }
    }

    private static class CandidateWithSettableWaypointIndex extends CandidateImpl {
        private static final long serialVersionUID = 5868551535609781722L;
        private int variableOneBasedWaypointIndex;
        
        public CandidateWithSettableWaypointIndex(int oneBasedIndexOfWaypoint, TimePoint p, double distanceProbability, Waypoint w) {
            super(/* oneBasedIndexOfWaypoint */ -1, p, distanceProbability, w);
            this.variableOneBasedWaypointIndex = oneBasedIndexOfWaypoint;
        }

        public void setOneBasedWaypointIndex(int oneBasedWaypointIndex) {
            this.variableOneBasedWaypointIndex = oneBasedWaypointIndex;
        }
        
        @Override
        public int getOneBasedIndexOfWaypoint() {
            return variableOneBasedWaypointIndex;
        }
    }
}
