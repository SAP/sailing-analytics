package com.sap.sailing.domain.markpassingcalculation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sailing.domain.markpassingcalculation.CandidateChooser;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;

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
 * @author Axel Uhl (d043530)
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
     * See {@link #getDistanceEstimationBasedProbability(Competitor, Candidate, Candidate, Distance)}.
     */
    private static final double MAX_REASONABLE_RATIO_BETWEEN_DISTANCE_TRAVELED_AND_LEG_LENGTH = 2.0;

    /**
     * Start mark passings will be considered this much before the actual race start. The race start
     * as identified by {@link TrackedRace#getStartOfRace()} is therefore {@link #EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME}
     * after {@link #raceStartTime}.
     */
    private static final Duration EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME = Duration.ONE_SECOND.times(30);
    
    /**
     * The duration after which a start mark passing's probability is considered only 50%. A perfect start mark
     * passing happening exactly at the race start time has time-wise probability of 1.0. Another delay of this much
     * lets the probability drop to 1/3, and so on.
     */
    private static final Duration DELAY_AFTER_WHICH_PROBABILITY_OF_START_HALVES = Duration.ONE_MINUTE.times(5);
    
    /**
     * In order to save the expensive distance calculation we may try to rank down an edge based on an
     * outrageously low speed the competitor would have been sailing at, based on the known distance between
     * the waypoints and the time taken between the waypoints as provided by the candidates. Anything above
     * this constant is assumed to be reasonable and gets a probability of 1 (no "penalty"). As the speed
     * drops below this value, the ratio of the actual speed estimate and this constant defined the probability
     * which then is less than 1 and therefore constitutes a penalty. Should the result be below the
     * {@link Edge#getPenaltyForSkipping() skip limit} then it is not necessary to calculate the distance
     * actually sailed, saving a lot of computational effort.<p>
     * 
     * With the current selection of 1kt and a skip probability of 0.1 any speed estimated below 0.1kt will
     * lead to the edge being discarded.
     */
    private static final Speed MINIMUM_REASONABLE_SPEED = new KnotSpeedImpl(3);
    
    private static final Speed MAXIMUM_REASONABLE_SPEED = GPSFixTrack.DEFAULT_MAX_SPEED_FOR_SMOOTHING;

    private static final double MINIMUM_PROBABILITY = Edge.getPenaltyForSkipping();

    private static final Logger logger = Logger.getLogger(CandidateChooserImpl.class.getName());

    private Map<Competitor, Map<Waypoint, MarkPassing>> currentMarkPasses = new HashMap<>();
    
    /**
     * The graph of edges connecting the candidates in time-wise ascending order. The {@link Edge#getStart()} candidate
     * always happens before the {@link Edge#getEnd()} candidate. Edges are keyed by the {@link Competitor} to whose
     * track they belong. The value maps key the edges by their {@link Edge#getStart() start candidate}.
     * <p>
     *
     * Methods operating on this collection and the collections embedded in it must be {@code synchronized} in order to
     * avoid overlapping operations. This will generally not limit concurrency further than usual, except for the
     * start-up phase where a background thread may be spawned by the constructor in case the
     * {@link MarkPassingCalculator#MarkPassingCalculator(DynamicTrackedRace, boolean, boolean, MarkPassingRaceFingerprintRegistry)} constructor is invoked
     * with the {@code waitForInitialMarkPassingCalculation} parameter set to {@code false}. In this case, mark passing
     * calculation will be launched in the background and will not be waited for. This then needs to be synchronized
     * with the dynamic (re-)calculations triggered by fixes and other data popping in.
     */
    private Map<Competitor, Map<Candidate, Set<Edge>>> allEdges = new HashMap<>();
    
    /**
     * The candidates found, keyed by the {@link Competitor} to whose track they belong.
     * The value sets contain all candidates found, but not for all of them do we want to
     * construct {@link Edge}s in the {@link #allEdges graph}. Whenever a competitor's candidate set
     * changes, filter rules are applied to eliminate redundant and "stray" candidates. For example,
     * if a tracker has been left on the race committee boat forming one end of the start line, and
     * the tracker keeps tracking, it will swerve back and forth slightly, producing many distance
     * candidates for various waypoints. By identifying that the tracker hasn't left a small bounding
     * box over significant amounts of time, many of those candidates can be removed and represented by,
     * say, the first and the last candidate in that small bounding box.<p>
     * 
     * Furthermore, specifically for {@link DistanceCandidateImpl distance candidates} we often see
     * candidates for different marks created for approximately (sometimes even precisely) the same
     * time point. For example, if a tracker approaches a start line from the leeward side and then
     * moves away again, a candidate for the start line but also for the gate situated on the windward
     * side of the start line will be created, roughly at the same time. Of those fixes only the one or
     * two most likely ones should have edges constructed for them. This should help eliminate, e.g.,
     * candidates for the windward mark if the tracker was near the start line at that time. This augments
     * the filter rule suppressing candidates with very low probability; it takes relative probability
     * of candidates with similar time points into account.<p>
     * 
     * Note, however, that we cannot know for which lap a candidate has to be created. If the same mark
     * is referred to by more than one {@link Waypoint} in the {@link Course}, if one candidate survives
     * the filtering then the respective candidates for all occurrences of that mark have to survive, too.
     * Example: if the fix reaches maximum proximity to the windward mark, candidates for all occurrences
     * of the windward mark in the course's waypoints will result. Around that time, other candidates may
     * have been produced due to the course change also for the leeward gate or even the start line or an
     * offset mark. Comparing the probabilities of the candidates in such a narrow time window will usually
     * show as many equal-rated candidates for the windward mark as there are occurrences of the windward
     * mark in the course. All of them need to pass the filter, and not only a single one. Other candidates
     * from the time window whose probability is discernably less should be filtered.
     */
    private final Map<Competitor, NavigableSet<Candidate>> candidates;
    
    /**
     * Those candidates from {@link #candidates} that have passed the filter logic of stage 1 (of candidates in close
     * chronological sequence pick only those that have the highest probability). They are fed into the filter stage 2
     * before they may become part of the {@link #allEdges graph}.
     */
    private final Map<Competitor, MostProbableCandidatesInSmallTimeRangeFilter> mostProbableCandidatesInSmallTimeRangeFilters;
    
    /**
     * Those candidates from {@link #candidates} that have passed the filter logic of stage 2 (of candidates between
     * which the track does not leave a small bounding box pick only those at start and end of that sequence) and are
     * added to the {@link #allEdges graph}. This data structure is redundant to {@link #allEdges} and intended to speed
     * up the algorithm used to find out which candidates must be added to and removed from the graph after changes have
     * been applied to {@link #candidates} and the filter rules have been re-applied.
     */
    private final Map<Competitor, StationarySequenceBasedFilter> stationarySequenceBasedFilters;
    
    private final Map<Competitor, NavigableSet<Candidate>> fixedPassings = new HashMap<>();
    private final ConcurrentHashMap<Competitor, Integer> suppressedPassings = new ConcurrentHashMap<>();
    
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
    
    /**
     * Most data structures in this candidate chooser are keyed by {@link Competitor} objects. When a method iterates
     * over or manipulates such a per-competitor structure it must obtain the corresponding lock from here and
     * acquire the read/write lock respectively. See {@link #getCompetitorLock}. The map is initialized in the
     * constructor and populated for all the race's competitors already. From there on it is used in read-only
     * mode, so no synchronization is necessary.
     */
    private final HashMap<Competitor, NamedReentrantReadWriteLock> perCompetitorLocks;
    
    /**
     * The {@link #start} and {@link #end} proxy candidates may have {@code null} time points and hence
     * would cause trouble with a regular {@link TimedComparator}. Therefore, this specialized comparator
     * considers the {@link #start} candidate less than all others, the {@link #end} candidate greater
     * than all others, and all other candidates are compared using a regular {@link TimedComparator},
     * but if time points are equal, disambiguation follows this order of precedence: waypoint index,
     * probability, candidate class (XTE before distance).
     * 
     * @author Axel Uhl (D043530)
     *
     */
    private class StartAndEndAwareTimeBasedCandidateComparator implements Comparator<Candidate> {
        private final Comparator<Timed> timedComparator = TimedComparator.INSTANCE;
        
        @Override
        public int compare(Candidate o1, Candidate o2) {
            int result;
            if (o1 == o2) {
                result = 0;
            } else if (o1 == start || o2 == end) {
                result = -1;
            } else if (o1 == end || o2 == start) {
                result = 1;
            } else {
                result = timedComparator.compare(o1, o2);
                if (result == 0) {
                    result = Integer.compare(o1.getOneBasedIndexOfWaypoint(), o2.getOneBasedIndexOfWaypoint());
                    if (result == 0) {
                        result = Double.compare(o1.getProbability(), o2.getProbability());
                        if (result == 0) {
                            result = o2.getClass().getSimpleName().compareTo(o1.getClass().getSimpleName());
                            if (result == 0) {
                                result = Util.compareToWithNull(o1.getWaypoint()==null?null:o1.getWaypoint().getId().toString(),
                                        o2.getWaypoint()==null?null:o2.getWaypoint().getId().toString(), /* nullIsLess */ true);
                                if (result == 0) {
                                    // last resort: try object identity hash code which is not guaranteed to be
                                    // different for distinct objects, but it's highly likely
                                    result = Integer.compare(System.identityHashCode(o1), System.identityHashCode(o2));
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }
    }
    
    private final StartAndEndAwareTimeBasedCandidateComparator CANDIDATE_COMPARATOR = new StartAndEndAwareTimeBasedCandidateComparator();

    /**
     * Captures statistics about this candidate chooser.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    public class Stats {
        public class CompetitorStats {
            private final int candidates;
            private final int candidatesAfterHighestProbabilityInShortTimeFilter;
            private final int candidatesAfterBoundingBoxFilter;
            private final int edges;
            public CompetitorStats(int candidates, int candidatesAfterHighestProbabilityInShortTimeFilter,
                    int candidatesAfterBoundingBoxFilter, int edges) {
                super();
                this.candidates = candidates;
                this.candidatesAfterHighestProbabilityInShortTimeFilter = candidatesAfterHighestProbabilityInShortTimeFilter;
                this.candidatesAfterBoundingBoxFilter = candidatesAfterBoundingBoxFilter;
                this.edges = edges;
            }
            public int getCandidates() {
                return candidates;
            }
            public int getCandidatesAfterHighestProbabilityInShortTimeFilter() {
                return candidatesAfterHighestProbabilityInShortTimeFilter;
            }
            public int getCandidatesAfterBoundingBoxFilter() {
                return candidatesAfterBoundingBoxFilter;
            }
            public int getEdges() {
                return edges;
            }
        }
        
        public int getTotalNumberOfCandidates() {
            int result = 0;
            for (final Set<Candidate> competitorCandidates : candidates.values()) {
                result += competitorCandidates.size();
            }
            return result;
        }
        
        public int getTotalNumberOfCandidatesAfterHighestProbabilityInShortTimeFilter() {
            int result = 0;
            for (final MostProbableCandidatesInSmallTimeRangeFilter competitorCandidates : mostProbableCandidatesInSmallTimeRangeFilters.values()) {
                result += Util.size(competitorCandidates.getFilteredCandidates());
            }
            return result;
        }
        
        public int getTotalNumberOfCandidatesAfterBoundingBoxFilter() {
            int result = 0;
            for (final StationarySequenceBasedFilter competitorCandidates : stationarySequenceBasedFilters.values()) {
                result += Util.size(competitorCandidates.getFilteredCandidates());
            }
            return result;
        }
        
        public int getTotalNumberOfEdges() {
            int result = 0;
            for (final Map<Candidate, Set<Edge>> edgesPerCandidate : allEdges.values()) {
                for (final Set<Edge> edges : edgesPerCandidate.values()) {
                    result += edges.size();
                }
            }
            return result;
        }
        
        public Map<Competitor, CompetitorStats> getPerCompetitorStats() {
            final Map<Competitor, CompetitorStats> result = new HashMap<>();
            for (final Entry<Competitor, NavigableSet<Candidate>> competitorAndCandidates : candidates.entrySet()) {
                result.put(competitorAndCandidates.getKey(), new CompetitorStats(
                        competitorAndCandidates.getValue().size(),
                        Util.size(mostProbableCandidatesInSmallTimeRangeFilters.get(competitorAndCandidates.getKey()).getFilteredCandidates()),
                        Util.size(stationarySequenceBasedFilters.get(competitorAndCandidates.getKey()).getFilteredCandidates()),
                        getNumberOfEdges(competitorAndCandidates.getKey())));
            }
            return result;
        }

        private int getNumberOfEdges(Competitor competitor) {
            int result = 0;
            for (final Set<Edge> edgeSet : allEdges.get(competitor).values()) {
                result += edgeSet.size();
            }
            return result;
        }
    }

    public CandidateChooserImpl(DynamicTrackedRace race) {
        this.perCompetitorLocks = new HashMap<>();
        this.race = race;
        waypointPositionAndDistanceCache = new WaypointPositionAndDistanceCache(race, Duration.ONE_MINUTE);
        final TimePoint startOfRaceWithoutInference = race.getStartOfRace(/* inferred */ false);
        raceStartTime = startOfRaceWithoutInference != null ? startOfRaceWithoutInference.
                minus(EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME) : null;
        start = new CandidateWithSettableTime(/* Index */0, raceStartTime, /* Probability */1, /* Waypoint */null);
        end = new CandidateWithSettableWaypointIndex(race.getRace().getCourse().getNumberOfWaypoints() + 1, /* TimePoint */null,
                /* Probability */1, /* Waypoint */null);
        candidates = new HashMap<>();
        mostProbableCandidatesInSmallTimeRangeFilters = new HashMap<>();
        stationarySequenceBasedFilters = new HashMap<>();
        List<Candidate> startAndEnd = Arrays.asList(start, end);
        for (Competitor c : race.getRace().getCompetitors()) {
            perCompetitorLocks.put(c, createCompetitorLock(c));
            candidates.put(c, Collections.synchronizedNavigableSet(new TreeSet<Candidate>(CANDIDATE_COMPARATOR)));
            mostProbableCandidatesInSmallTimeRangeFilters.put(c, new MostProbableCandidatesInSmallTimeRangeFilter(CANDIDATE_COMPARATOR, start, end));
            stationarySequenceBasedFilters.put(c, new StationarySequenceBasedFilter(CANDIDATE_COMPARATOR, race.getTrack(c), start, end));
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

    public Stats getStats() {
        return new Stats();
    }
    
    private NamedReentrantReadWriteLock createCompetitorLock(Competitor c) {
        return new NamedReentrantReadWriteLock("Competitor lock for "+c+" in candidate chooser "+this, /* fair */ false);
    }

    @Override
    public void calculateMarkPassDeltas(Competitor c, Iterable<Candidate> newCans, Iterable<Candidate> oldCans) {
        calculateMarkPassDeltas(c, /* new fixes */ Collections.emptySet(), /* fixesReplacingExistingOnes */ Collections.emptySet(),
                newCans, oldCans);
    }

    @Override
    public void calculateMarkPassDeltas(Competitor c, Iterable<GPSFixMoving> newFixes,
            Iterable<GPSFixMoving> fixesReplacingExistingOnes, Iterable<Candidate> newCans, Iterable<Candidate> oldCans) {
        updateStationarySequences(c, newFixes, fixesReplacingExistingOnes);
        final TimePoint startOfRace = race.getStartOfRace(/* inference */ false);
        if (startOfRace != null) {
            final boolean startTimeUpdated;
            synchronized (this) { // protect raceStartTime check and update
                if (raceStartTime == null || !startOfRace.minus(EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME).equals(raceStartTime)) {
                    raceStartTime = startOfRace.minus(EARLY_STARTS_CONSIDERED_THIS_MUCH_BEFORE_STARTTIME);
                    startTimeUpdated = true;
                } else {
                    startTimeUpdated = false;
                }
            }
            if (startTimeUpdated) {
                Collection<Candidate> startList = Collections.singleton(start);
                for (Competitor competitor : candidates.keySet()) {
                    removeCandidates(competitor, startList);
                }
                start.setTimePoint(raceStartTime);
                for (Competitor competitor : allEdges.keySet()) {
                    addCandidates(competitor, startList);
                }
            }
        }
        removeCandidates(c, oldCans);
        addCandidates(c, newCans);
        findShortestPath(c);
    }

    /**
     * The {@link StationarySequenceBasedFilter} object for competitor {@code c} is updated using the new
     * and replacing fixes. The resulting changes in candidates passing that filter stage are
     * {@link #adjustGraph(Competitor, Pair) propagated} into the {@link #allEdges graph}, but
     * no {@link #findShortestPath(Competitor) recalculation of shortest path} is done yet.
     */
    private void updateStationarySequences(Competitor c, Iterable<GPSFixMoving> newFixes,
            Iterable<GPSFixMoving> fixesReplacingExistingOnes) {
        LockUtil.lockForWrite(perCompetitorLocks.get(c));
        try {
            adjustGraph(c, stationarySequenceBasedFilters.get(c).updateFixes(newFixes, fixesReplacingExistingOnes));
        } finally {
            LockUtil.unlockAfterWrite(perCompetitorLocks.get(c));
        }
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
        // remove "end" from the fixed passings which is ordered by waypoint number; after the change,
        // "end" may need to be at a different position in the navigable set.
        for (final NavigableSet<Candidate> fixedPassingsForCompetitor : fixedPassings.values()) {
            fixedPassingsForCompetitor.remove(end);
        }
        end.setOneBasedWaypointIndex(race.getRace().getCourse().getNumberOfWaypoints()+1);
        for (final NavigableSet<Candidate> fixedPassingsForCompetitor : fixedPassings.values()) {
            fixedPassingsForCompetitor.add(end);
        }
    }

    @Override
    public void setFixedPassing(Competitor c, Integer zeroBasedIndexOfWaypoint, TimePoint t) {
        final NamedReentrantReadWriteLock lock = perCompetitorLocks.get(c);
        if (lock != null) { // otherwise this is a phony competitor and we aren't interested in fixed mark passings for it
            if (zeroBasedIndexOfWaypoint >= 0 && zeroBasedIndexOfWaypoint < race.getRace().getCourse().getNumberOfWaypoints()) {
                LockUtil.lockForWrite(lock);
                try {
                    Candidate fixedCan = new CandidateForFixedMarkPassingImpl(zeroBasedIndexOfWaypoint + 1, t, 1, Util.get(race.getRace().getCourse().getWaypoints(), zeroBasedIndexOfWaypoint));
                    NavigableSet<Candidate> fixed = fixedPassings.get(c);
                    if (fixed != null) { // can only set the mark passing if the competitor is still part of this race
                        if (!fixed.add(fixedCan)) {
                            Candidate old = fixed.ceiling(fixedCan);
                            fixed.remove(old);
                            removeCandidates(c, Collections.singleton(old));
                            fixed.add(fixedCan);
                        }
                        addCandidates(c, Collections.singleton(fixedCan));
                        findShortestPath(c);
                    }
                } finally {
                    LockUtil.unlockAfterWrite(lock);
                }
            } else {
                logger.warning("Competitor "+c+" has fixed mark passing for non-existing waypoint #"+(zeroBasedIndexOfWaypoint+1));
            }
        }
    }

    @Override
    public void removeFixedPassing(Competitor c, Integer zeroBasedIndexOfWaypoint) {
        final NamedReentrantReadWriteLock lock = perCompetitorLocks.get(c);
        if (lock != null) { // otherwise this is a phony competitor and we aren't interested in fixed mark passings for it
            LockUtil.lockForWrite(lock);
            try {
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
            } finally {
                LockUtil.unlockAfterWrite(lock);
            }
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
        assert perCompetitorLocks.get(c).isWriteLocked();
        final Boolean isGateStart = race.isGateStart();
        Map<Candidate, Set<Edge>> edgesForCompetitor = allEdges.get(c);
        final Iterable<Candidate> competitorCandidates = getFilteredCandidates(c);
        for (Candidate newCan : newCandidates) {
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
    
                    final Supplier<Double> estimatedDistanceProbabilitySupplier;
                    final double estimatedDistanceProbability;
                    final double startTimingProbability;
                    // when null, don't create an edge; when a valid distance, use this as the totalGreatCircleDistance value
                    final Distance ignoreEdgeDueToProbabilityLowerThanMinimumForSkipping;
                    if (early == start) {
                        // An edge starting at the start proxy node. If the late candidate is for a start mark passing,
                        // determine a probability not based on distance traveled but based on the
                        // time difference between scheduled start time and candidate's time point. If the "late" candidate
                        // is not for the start mark/line, meaning that mark passings including the actual start are
                        // skipped, as usual use getDistanceEstimationBasedProbability assuming a start mark passing at
                        // the race's start time.
                        if (isGateStart == Boolean.TRUE || start.getTimePoint() == null) { // TODO for gate start read gate timing and scale probability accordingly
                            startTimingProbability = 1; // no start time point known; all candidate time points equally likely
                            estimatedDistanceProbability = 1.0;
                            ignoreEdgeDueToProbabilityLowerThanMinimumForSkipping = Distance.NULL;
                            estimatedDistanceProbabilitySupplier = null; // can't tell distance sailed either because we don't know the start time
                        } else {
                            // no gate start and we know the race start time
                            if (late.getWaypoint() != null && late.getWaypoint() == race.getRace().getCourse().getFirstWaypoint()) {
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
                                estimatedDistanceProbability = 1.0;
                                ignoreEdgeDueToProbabilityLowerThanMinimumForSkipping = Distance.NULL;
                                estimatedDistanceProbabilitySupplier = null;
                            } else {
                                startTimingProbability = 1; // can't really tell how well the start time was matched when
                                                            // we don't have a start candidate
                                if (late == end) {
                                    estimatedDistanceProbability = 1.0;
                                    ignoreEdgeDueToProbabilityLowerThanMinimumForSkipping = Distance.NULL;
                                    estimatedDistanceProbabilitySupplier = null;
                                } else {
                                    estimatedDistanceProbability = 0.0;
                                    ignoreEdgeDueToProbabilityLowerThanMinimumForSkipping = getIgnoreDueToTimingInducedEstimatedSpeeds(c, early, late);
                                    estimatedDistanceProbabilitySupplier = ()->getDistanceEstimationBasedProbability(c, early, late, ignoreEdgeDueToProbabilityLowerThanMinimumForSkipping);
                                }
                            }
                        }
                    } else {
                        startTimingProbability = 1; // no penalty for any start time difference because this edge doesn't cover a start
                        if (late == end) {
                            // final edge; we don't know anything about distances for the end proxy node
                            estimatedDistanceProbability = 1.0;
                            ignoreEdgeDueToProbabilityLowerThanMinimumForSkipping = Distance.NULL;
                            estimatedDistanceProbabilitySupplier = null;
                        } else {
                            estimatedDistanceProbability = 0.0;
                            ignoreEdgeDueToProbabilityLowerThanMinimumForSkipping = getIgnoreDueToTimingInducedEstimatedSpeeds(c, early, late);
                            estimatedDistanceProbabilitySupplier = ()->getDistanceEstimationBasedProbability(c, early, late, ignoreEdgeDueToProbabilityLowerThanMinimumForSkipping);
                        }
                    }
                    // If one of the candidates is fixed, the edge is always created unless they travel backwards in time.
                    // Otherwise the edge is only created if the distance estimation, which can be calculated as long as the
                    // candidates are not the proxy and or start is close enough to the actual distance sailed.
                    final NavigableSet<Candidate> fixed = fixedPassings.get(c);
                    // TODO this comparison does not exactly implement the condition "if distance is more likely than skipping"
                    if (travelingForwardInTimeOrUnknown(early, late) &&
                            (fixed.contains(early) || fixed.contains(late) || ignoreEdgeDueToProbabilityLowerThanMinimumForSkipping != null)) {
                        final Edge edge;
                        if (estimatedDistanceProbabilitySupplier != null) {
                            edge = new Edge(early, late,
                                        ()->startTimingProbability * estimatedDistanceProbabilitySupplier.get(), race.getRace().getCourse().getNumberOfWaypoints());
                        } else {
                            edge = new Edge(early, late,
                                    startTimingProbability * estimatedDistanceProbability, race.getRace().getCourse().getNumberOfWaypoints());
                        }
                        addEdge(edgesForCompetitor, edge);
                    }
                }
            }
        }
    }

    /**
     * @return the candidates that passed all stages of candidate filtering; see also
     *         {@link #updateFilteredCandidates(Competitor, Iterable, Iterable)}
     */
    private Iterable<Candidate> getFilteredCandidates(Competitor c) {
        return stationarySequenceBasedFilters.get(c).getFilteredCandidates();
    }

    private boolean travelingForwardInTimeOrUnknown(Candidate early, Candidate late) {
        return early.getTimePoint() == null || late.getTimePoint() == null || early.getTimePoint().before(late.getTimePoint());
    }

    private void addEdge(Map<Candidate, Set<Edge>> edgesForCompetitor, Edge e) {
        logger.finest(()->"Adding "+ e.toString());
        Set<Edge> edgeSet = edgesForCompetitor.get(e.getStart());
        if (edgeSet == null) {
            edgeSet = new HashSet<>();
            edgesForCompetitor.put(e.getStart(), edgeSet);
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
        LockUtil.lockForWrite(perCompetitorLocks.get(c));
        try {
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
                currentEdgesMoreLikelyFirst.add(new Util.Pair<Edge, Double>(new Edge(
                        new CandidateImpl(-1, null, /* estimated distance probability */ 1, null), startOfFixedInterval,
                        ()->1.0, race.getRace().getCourse().getNumberOfWaypoints()), 1.0));
                // find the shortest path from startOfFixedInterval to endOfFixedInterval:
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
                            logger.finest(()->"Added "+ currentMostLikelyEdge + " as most likely edge for " + c);
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
                if (endOfFixedInterval != null && endOfFixedInterval.getOneBasedIndexOfWaypoint() > end.getOneBasedIndexOfWaypoint()) {
                    // we have fixed candidates for waypoints whose index exceeds that of the proxy "end" node;
                    // this can happen, e.g., if fixed passings are being processed already before a course
                    // definition has been received yet.  In this case we call off the search; the fixedPassings
                    // contain the "end" node not at the end; an inconsistency which the next call to
                    // updateEndProxyNodeWaypointIndex() is expected to fix during the next round of changes.
                    logger.warning("In "+this+" the proxy end node's waypoint index "+end.getOneBasedIndexOfWaypoint()+
                            " was exceeded by that of the fixed mark passing "+endOfFixedInterval+
                            ". Stopping at "+end+" in this round; the end node will be updated soon and another round "+
                            "of calculations will start, then with an up-to-date waypoint index for the end proxy node.");
                    endOfFixedInterval = null;
                }
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
                logger.fine(()->"Updating MarkPasses for " + c + " in case "+race.getRace().getName());
                race.updateMarkPassings(c, newMarkPassings);
            }
        } finally {
            LockUtil.unlockAfterWrite(perCompetitorLocks.get(c));
        }
    }

    /**
     * The timing of the candidates is put in relation to the distance between the waypoints they connect. This implies
     * a rough speed estimate. If this is completely "out of whack" (way too low or way too high), we can assume that if
     * ultimately carrying out the potentially expensive (for long-distance races) calculation of the distance sailed,
     * we would only find that the distance-based probability would be below the {@link #MINIMUM_PROBABILITY} threshold
     * such that that edge would be ignored.
     * 
     * @return {@code null} if the edge between {@code c1} and {@code c2} shall be ignored; the
     *         {@link #getMinimumTotalGreatCircleDistanceBetweenWaypoints(Waypoint, Waypoint, TimePoint) straight-line
     *         distance between the corresponding waypoints} otherwise. This distance can be used for a subsequent call
     *         to {@link #getDistanceEstimationBasedProbability(Competitor, Candidate, Candidate, Distance)} so it doesn't have
     *         to be re-calculated there.
     */
    private Distance getIgnoreDueToTimingInducedEstimatedSpeeds(Competitor c, Candidate c1, Candidate c2) {
        final boolean ignore;
        final Distance totalGreatCircleDistance;
        assert c1.getOneBasedIndexOfWaypoint() < c2.getOneBasedIndexOfWaypoint();
        assert c2 != end;
        if (c1.getTimePoint() == null || c2.getTimePoint() == null) {
            // cannot compute a distance in case of unknown timings; ignore the edge
            ignore = true;
            totalGreatCircleDistance = null;
        } else {
            final TimePoint middleOfc1Andc2 = new MillisecondsTimePoint(c1.getTimePoint().plus(c2.getTimePoint().asMillis()).asMillis() / 2);
            Waypoint first = getFirstWaypoint(c1);
            final Waypoint second = c2.getWaypoint();
            totalGreatCircleDistance = getMinimumTotalGreatCircleDistanceBetweenWaypoints(first, second, middleOfc1Andc2);
            if (totalGreatCircleDistance == null) {
                ignore = true; // no distance known; cannot tell, so ignore the edge
            } else {
                // Computing the distance traveled can be quite expensive, especially for candidates very far apart.
                // As a quick approximation let's look at how long the time between the candidates was and relate that to the minimum distance
                // between the waypoints. This leads to a speed estimation; if we take the minimum distance times two, we
                // get an upper bound for a reasonable distance sailed between the waypoints and therefore an estimation
                // for the maximum speed at which the competitor would have had to sail:
                Speed estimatedMaxSpeed = totalGreatCircleDistance.scale(2).inTime(c1.getTimePoint().until(c2.getTimePoint()));
                final double estimatedMinSpeedBasedProbability = Math.max(0, estimatedMaxSpeed.divide(MINIMUM_REASONABLE_SPEED));
                final double estimatedMaxSpeedBasedProbability = Math.max(0, MAXIMUM_REASONABLE_SPEED.divide(estimatedMaxSpeed));
                final double estimatedSpeedBasedProbabilityMinimum = Math.min(estimatedMaxSpeedBasedProbability, estimatedMinSpeedBasedProbability);
                ignore = estimatedSpeedBasedProbabilityMinimum < MINIMUM_PROBABILITY;
            }
        }
        return ignore ? null : totalGreatCircleDistance;
    }

    /**
     * If the candidate has no waypoint associated, return the course's first waypoint; otherwise, return the candidates
     * {@link Candidate#getWaypoint() waypoint}.
     */
    private Waypoint getFirstWaypoint(Candidate candidate) {
        Waypoint first;
        if (candidate.getOneBasedIndexOfWaypoint() == 0) {
            first = race.getRace().getCourse().getFirstWaypoint();
        } else {
            first = candidate.getWaypoint();
        }
        return first;
    }

    /**
     * The distance between waypoints is used to estimate the distance that should be covered between these two
     * candidates. This estimation is then compared to the distance actually sailed. A distance smaller than the
     * estimation is (aside from a small tolerance) impossible, a distance larger get increasingly unlikely.
     * 
     * @param totalGreatCircleDistance
     *            the result of a previous
     *            {@link #getMinimumTotalGreatCircleDistanceBetweenWaypoints(Waypoint, Waypoint, TimePoint)} call for
     *            the two waypoints of {@code c1} and {@code c2}
     */
    private double getDistanceEstimationBasedProbability(Competitor c, Candidate c1, Candidate c2, Distance totalGreatCircleDistance) {
        final double result;
        assert c1.getOneBasedIndexOfWaypoint() < c2.getOneBasedIndexOfWaypoint();
        assert c2 != end;
        if (totalGreatCircleDistance == null) {
            result = 0.0; // no distance known; cannot tell
        } else {
            // Computing the distance traveled can be quite expensive, especially for candidates very far apart.
            // Let's first look at how long the time between the candidates was and relate that to the minimum distance
            // between the waypoints. This leads to a speed estimation; if we take the minimum distance times two, we
            // get an upper bound for a reasonable distance sailed between the waypoints and therefore an estimation
            // for the maximum speed at which the competitor would have had to sail:
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

    /**
     * New candidates will be added to {@link #candidates}. The filtering rules will be applied to update
     * {@link #mostProbableCandidatesInSmallTimeRangeFilters} and {@link #stationarySequenceBasedFilters} so that the
     * sets of filtered candidates are again consistent with the contents of {@link #candidates} and the filter rules.
     * Afterwards, the {@link #allEdges graph} will be updated by removing edges for candidates that are no longer part
     * of {@link #stationarySequenceBasedFilters} (the last filter stage) and by adding edges for new candidates in
     * {@link #stationarySequenceBasedFilters}. Note that the set of {@link #stationarySequenceBasedFilters} may
     * actually shrink by adding a new candidate, simply because that candidate closes a gap between other candidates in
     * the {@link #mostProbableCandidatesInSmallTimeRangeFilters} filter or by extending a stationary sequence such that
     * a candidate in the middle of that sequence no longer is close enough to the sequence's border.
     */
    private void addCandidates(Competitor c, Iterable<Candidate> newCandidates) {
        LockUtil.lockForWrite(perCompetitorLocks.get(c));
        try {
            for (Candidate can : newCandidates) {
                candidates.get(c).add(can);
            }
            updateFilteredCandidatesAndAdjustGraph(c, newCandidates, /* removedCandidates */ Collections.emptySet());
        } finally {
            LockUtil.unlockAfterWrite(perCompetitorLocks.get(c));
        }
    }

    /**
     * Precondition: the {@link #candidates candidates(c)} for the competitor {@code c} have been updated.
     * <p>
     * 
     * Then this method applies the filter rules to update {@link #mostProbableCandidatesInSmallTimeRangeFilters} of filter stage 1 (based on
     * highest probability in short time ranges) and subsequently the second filter stage's
     * {@link #stationarySequenceBasedFilters}. Based on the difference in filter results, the {@link #allEdges graph} is
     * adjusted by removing edges for those candidates no longer passing the filter or no longer existing, and by adding
     * edges for candidates that have now become available as filtered candidates and that weren't before. No shortest
     * path analysis is triggered yet.
     */
    private void updateFilteredCandidatesAndAdjustGraph(Competitor c, Iterable<Candidate> newCandidates, Iterable<Candidate> removedCandidates) {
        Pair<Iterable<Candidate>, Iterable<Candidate>> filteredCandidatesAddedAndRemoved = updateFilteredCandidates(c, newCandidates, removedCandidates);
        adjustGraph(c, filteredCandidatesAddedAndRemoved);
    }

    /**
     * Adjusts the {@link #allEdges graph} based on the nodes added and removed. No {@link #findShortestPath(Competitor)
     * path analysis} is performed yet.
     */
    private void adjustGraph(Competitor c,
            Pair<Iterable<Candidate>, Iterable<Candidate>> filteredCandidatesAddedAndRemoved) {
        final Map<Candidate, Set<Edge>> competitorEdges = allEdges.get(c);
        for (final Candidate candidateRemoved : filteredCandidatesAddedAndRemoved.getB()) {
            logger.finest(()->"Removing all edges containing " + candidateRemoved + "of "+ c);
            removeEdgesForCandidate(candidateRemoved, competitorEdges);
        }
        createNewEdges(c, filteredCandidatesAddedAndRemoved.getA());
    }
    
    /**
     * Based on {@link #candidates candidates(c)}, computes the set of candidates that pass all filter rules and hence
     * are expected to be represented in the {@link #allEdges graph}. When this method returns, it has updated the
     * {@link #mostProbableCandidatesInSmallTimeRangeFilters} accordingly.
     * <p>
     * 
     * A two-pass algorithm. In the first pass, clusters of {@link DistanceCandidateImpl distance candidates} in close
     * time-wise proximity are sorted by their probability (see {@link MostProbableCandidatesInSmallTimeRangeFilter}).
     * Only the group with the highest probability is selected, assuming that multiple occurrences of the same mark in
     * multiple waypoints leads to very similar if not equal probabilities. This way, candidates for marks further away
     * don't depend on the overall minimum probability, but the relative ranking leads to a quick elimination of
     * unlikely candidates.
     * <p>
     * 
     * During the second pass, clusters of candidates are considered where the track between the first and the last
     * candidate of the cluster does fit into a small bounding box (see {@link StationarySequenceBasedFilter}). This
     * suggests that the tracker was not actively sailing during this period, and only the first and the last candidate
     * of the cluster will pass the filter.
     * <p>
     * 
     * @param competitor
     *            the competitor whose candidates to filter
     * @param newCandidates
     *            the candidates that were added to the {@link #candidates candidates(c)} collection; some of these may
     *            already have been in the collection, so the parameter actually describes a superset of the candidates
     *            added
     * @param removedCandidates
     *            the candidates that were removed from the {@link #candidates candidates(c)} collection; some of these
     *            may already have been missing from the collection, so the parameter actually describes a superset of
     *            the candidates removed
     * @return a pair whose first element holds the set of candidates that now pass the filter and didn't before, and
     *         whose second element holds the set of candidates that did pass the filter before but don't anymore
     */
    private Pair<Iterable<Candidate>, Iterable<Candidate>> updateFilteredCandidates(Competitor competitor, Iterable<Candidate> newCandidates, Iterable<Candidate> removedCandidates) {
        final NavigableSet<Candidate> competitorCandidates = candidates.get(competitor);
        // pass 1
        final Pair<Set<Candidate>, Set<Candidate>> filteredCandidatesAddedAndRemovedBasedOnMostProbableCandidatesPerSequence =
                mostProbableCandidatesInSmallTimeRangeFilters.get(competitor).updateCandidates(competitorCandidates, newCandidates, removedCandidates);
        // pass 2
        final Pair<Iterable<Candidate>, Iterable<Candidate>> candidatesOnTheMove = stationarySequenceBasedFilters.get(competitor).updateCandidates(
                filteredCandidatesAddedAndRemovedBasedOnMostProbableCandidatesPerSequence.getA(),
                filteredCandidatesAddedAndRemovedBasedOnMostProbableCandidatesPerSequence.getB());
        return candidatesOnTheMove;
    }

    /**
     * Removes the {@code wrongCandidates} from the competitor's {@link #candidates} and updates the
     * {@link #mostProbableCandidatesInSmallTimeRangeFilters} and {@link #stationarySequenceBasedFilters} filters
     * accordingly. If filtered candidates are removed, their adjacent edges are removed from the {@link #allEdges
     * graph}. If candidates now pass the filter which previously didn't, edges are inserted for them. This can happen,
     * e.g., if a candidate is removed which previously connected other candidates into a cluster which in its entirety
     * became subject to filtering and which now after the removal of the candidate is split up such that the parts of
     * the former cluster now may pass the filter.
     */
    private void removeCandidates(Competitor c, Iterable<Candidate> wrongCandidates) {
        LockUtil.lockForWrite(perCompetitorLocks.get(c));
        try {
            for (Candidate can : wrongCandidates) {
                candidates.get(c).remove(can);
            }
            updateFilteredCandidatesAndAdjustGraph(c, /* newCandidates */ Collections.emptySet(), wrongCandidates);
        } finally {
            LockUtil.unlockAfterWrite(perCompetitorLocks.get(c));
        }
    }

    private void removeEdgesForCandidate(Candidate can, Map<Candidate, Set<Edge>> edges) {
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
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(getClass().getSimpleName());
        result.append(" for race ");
        result.append(race.getRace().getName()+" in regatta "+race.getTrackedRegatta().getRegatta().getName());
        result.append(". Filtered vs. original candidate ratio: ");
        long original = 0;
        long filtered = 0;
        for (final Entry<Competitor, NavigableSet<Candidate>> competitorAndCandidate : candidates.entrySet()) {
            original += competitorAndCandidate.getValue().size();
            filtered += Util.size(mostProbableCandidatesInSmallTimeRangeFilters.get(competitorAndCandidate.getKey()).getFilteredCandidates());
        }
        result.append(filtered);
        result.append("/");
        result.append(original);
        result.append("=");
        result.append((double) filtered/(double) original);
        return result.toString();
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
