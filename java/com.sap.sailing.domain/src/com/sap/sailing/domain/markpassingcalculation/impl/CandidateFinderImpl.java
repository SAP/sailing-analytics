package com.sap.sailing.domain.markpassingcalculation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.CPUMeteringType;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sailing.domain.markpassingcalculation.CandidateFinder;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.MarkPositionAtTimePointCacheImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.TimeRangeImpl;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * The standard implementation of {@link CandidateFinder}. There are two kinds of {@link Candidate}s. First of all,
 * every time a competitor passes the crossing-bearing of a waypoint, a candidate is created using linear interpolation
 * to estimate the exact time the bearing was crossed. Secondly, all local distance minima to a waypoint are candidates.
 * The probability of a candidate depends on its distance, whether it is on the right side and if it passes in the right
 * direction of its waypoint.
 * 
 * @author Nicolas Klose
 * @author Axel Uhl (d043530)
 * 
 */
public class CandidateFinderImpl implements CandidateFinder {
    /**
     * The higher this is, the closer the fixes have to be to waypoint to become a Candidate. The formula basically
     * works like this:
     * {@code probability := adjacentLegLength / fixDistanceFromWaypoint / STRICTNESS_OF_DISTANCE_BASED_PROBABILITY}. In
     * other words, the larger the distance, the less the probability; and this strictness factor is applied to the
     * denominator. Therefore, approximately, if the fix distance from the waypoint is {@code adjacentLegLength / }
     * {@link #STRICTNESS_OF_DISTANCE_BASED_PROBABILITY} then the probability based on that distance is 1. Twice
     * that distance brings down the probability to 1/2. Lesser distances can't increase the probability beyond 1.
     * <p>
     * 
     * (Effectively, the formula is a bit more complicated as tolerances for GPS HDOP are included.)
     */
    private final int STRICTNESS_OF_DISTANCE_BASED_PROBABILITY = 10;

    // All of the penalties are multiplied onto the probability of a Candidate. A value of 0 excludes Candidates that do
    // not fit, a value of 1 imposes no penalty on each criteria
    private static final double PENALTY_FOR_WRONG_SIDE = 0.8;
    
    /**
     * The penalty factor multiplied with a candidate's probability in case the competitor passes the line of an XTE
     * candidate with passing instructions other than {@link PassingInstruction#Line} in the wrong direction, e.g.,
     * passing a single mark that is to be passed to port in the wrong direction.
     */
    private static final double PENALTY_FOR_WRONG_DIRECTION = 0.7;
    
    /**
     * The penalty factor multiplied with a candidate's probability in case the competitor passes an XTE candidate for a
     * {@link PassingInstruction#Line line} in the wrong direction, e.g., passing a start line the wrong way.
     */
    private static final double PENALTY_FOR_LINE_PASSED_IN_WRONG_DIRECTION = 0.3;
    
    /**
     * Normally, for each distance candidate there should also be a proper XTE candidate that also tells about the
     * direction in which the boat crosses the line. XTE candidates are more precise and get a penalty in case the competitor
     * crosses the virtual line in the wrong direction: {@link #PENALTY_FOR_WRONG_DIRECTION}. The penalty a competitor should
     * get for not triggering a proper XTE candidate and only producing a candidate by getting near a mark and then
     * moving away again should be penalized at least as badly as {@link #PENALTY_FOR_WRONG_DIRECTION}.
     */
    private static final double PENALTY_FOR_DISTANCE_CANDIDATES = 0.9 * PENALTY_FOR_WRONG_DIRECTION;
    
    private static final double WORST_PENALTY_FOR_OTHER_COMPETITORS_BEING_FAR_FROM_START = 0.1;
    private static final double NUMBER_OF_HULL_LENGTHS_DISTANCE_FROM_START_AT_WHICH_WORST_PENALTY_APPLIES = 10;
    
    /**
     * If a {@link TrackedRace#getStartOfRace(boolean)) non-inferred start time} is known for the {@link #race},
     * candidates are not accepted if they are more than this duration earlier than the start time.
     */
    private static final Duration EARLIEST_START_MARK_PASSING_THIS_MUCH_BEFORE_START = Duration.ONE_MINUTE;

    /**
     * If a {@link TrackedRace#getFinishedTime()) finished time} ("Blue Flag Down") is known for the {@link #race},
     * candidates are not accepted if they are more than this duration later than the time when the race has officially
     * finished.
     */
    private static final Duration LATEST_FINISH_MARK_PASSING_THIS_MUCH_AFTER_RACE_FINISHED = Duration.ONE_MINUTE.times(5);

    private static final Logger logger = Logger.getLogger(CandidateFinderImpl.class.getName());

    
    private Map<Competitor, LinkedHashMap<GPSFix, Map<Waypoint, List<Distance>>>> distanceCache = new LinkedHashMap<>();
    private Map<Competitor, LinkedHashMap<GPSFix, Map<Waypoint, List<Distance>>>> xteCache = new LinkedHashMap<>();

    private Map<Competitor, Map<Waypoint, Map<List<GPSFix>, Candidate>>> xteCandidates = new HashMap<>();
    private Map<Competitor, Map<Waypoint, Map<GPSFix, Candidate>>> distanceCandidates = new HashMap<>();
    private final DynamicTrackedRace race;
    
    /**
     * Fixes are not considered eligible as candidates if this field is {@code null} (meaning a start time is not known
     * and won't be inferred from start mark passings) or a non-inferred start time is known for the race and the fix is
     * {@link #EARLIEST_START_MARK_PASSING_THIS_MUCH_BEFORE_START reasonably} earlier than this start time point. They
     * are also ignored if a non-inferred finished time for the race is known and the fix is
     * {@link #LATEST_FINISH_MARK_PASSING_THIS_MUCH_AFTER_RACE_FINISHED reasonably} after that time point. Always holds
     * a non-{@code null} object after the constructor has terminated.
     */
    private TimeRangeWithNullStartMeaningEmpty timeRangeForValidCandidates;
    
    private final double penaltyForSkipping = Edge.getPenaltyForSkipping();
    private final Map<Waypoint, PassingInstruction> passingInstructions = new LinkedHashMap<>();
    private final Comparator<Timed> comp = TimedComparator.INSTANCE;
    
    private final ExecutorService executor;
    
    /**
     * Like {@link #CandidateFinderImpl(DynamicTrackedRace, ExecutorService)} but creates a default executor service
     */
    public CandidateFinderImpl(DynamicTrackedRace race) {
        this(race, ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor());
    }

    public CandidateFinderImpl(DynamicTrackedRace race, ExecutorService executor) {
        this.race = race;
        this.executor = executor;
        this.timeRangeForValidCandidates = getTimeRangeOrNull(
                getTimePointWhenToStartConsideringCandidates(race.getStartOfRace(/* inferred */ false)),
                getTimePointWhenToFinishConsideringCandidates(race.getFinishedTime()));
        final RaceDefinition raceDefinition = race.getRace();
        for (Competitor c : raceDefinition.getCompetitors()) {
            xteCache.put(c, new LimitedLinkedHashMap<GPSFix, Map<Waypoint, List<Distance>>>(25));
            distanceCache.put(c, new LimitedLinkedHashMap<GPSFix, Map<Waypoint, List<Distance>>>(25));
            xteCandidates.put(c, new HashMap<Waypoint, Map<List<GPSFix>, Candidate>>());
            distanceCandidates.put(c, new HashMap<Waypoint, Map<GPSFix, Candidate>>());
        }
    }

    /**
     * For a given race start time {@code startOfRace} which is not inferred from any mark passings determines
     * the earliest start mark passing time point. No candidates earlier than this time point need to be considered.
     */
    private TimePoint getTimePointWhenToStartConsideringCandidates(TimePoint startOfRace) {
        return startOfRace == null ? null : startOfRace.minus(EARLIEST_START_MARK_PASSING_THIS_MUCH_BEFORE_START);
    }
    
    /**
     * For a given race finished time {@code finishedTime} which is not inferred from any mark passings determines
     * the latest finish mark passing time point. No candidates later than this time point need to be considered.
     */
    private TimePoint getTimePointWhenToFinishConsideringCandidates(TimePoint finishedTime) {
        return finishedTime == null ? null : finishedTime.plus(LATEST_FINISH_MARK_PASSING_THIS_MUCH_AFTER_RACE_FINISHED);
    }
    
    private Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> clearAllCandidates() {
        return updateCandiatesAfterRaceTimeRangeChanged(TimePoint.EndOfTime, TimePoint.BeginningOfTime);
    }
    
    /**
     * Adds or removes candidates for a time range. If the {@code startOfRangeToAdd} is at or before
     * {@code endOfRangeToAdd}, all fixes from this range for all competitors will be submitted for candidate analysis,
     * and new candidates may emerge from this. If {@code startOfRangeToAdd} is after {@code endOfRangeToAdd}, all
     * candidates within that (inverse) range are removed, and so are all edges attached to any of these candidates.
     * 
     * @param startOfRangeToAdd
     *            the start of a range of fixes valid for candidate analysis if {@link TimePoint#before(TimePoint)}
     *            {@code endOfRangeToAdd}; the end of the now invalid time range of fixes no longer valid for candidate
     *            analysis otherwise; may be {@code null}, meaning that no start of the valid time range is known and
     *            therefore all fixes from the beginning of the track up to {@code endOfRangeToAdd} are to be analyzed
     * @param endOfRangeToAdd
     *            the end of a range of fixes valid for candidate analysis if {@link TimePoint#after(TimePoint)}
     *            {@code startOfRangeToAdd}; the start of the now invalid time range of fixes no longer valid for
     *            candidate analysis otherwise; may be {@code null}, meaning that no end of the valid time range is
     *            known and therefore all fixes starting at {@code startOfRangeToAdd} until the end of the track are to
     *            be analyzed
     * @return the fixes added and removed, keyed by competitor; the value pair's {@link Pair#getA() first} component
     *         has the candidates added, the {@link Pair#getB() second} has the candidates removed
     */
    private Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> updateCandiatesAfterRaceTimeRangeChanged(
            final TimePoint startOfRangeToAdd, final TimePoint endOfRangeToAdd) {
        final Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> result = new HashMap<>();
        if (startOfRangeToAdd.after(endOfRangeToAdd)) {
            final TimeRange timeRangeToRemoveCandidatesFrom = new TimeRangeImpl(endOfRangeToAdd, startOfRangeToAdd);
            // remove candidates in the time range starting at endOfRangeToAdd and ending at startOfRangeToAdd:
            final Map<Competitor, Set<Candidate>> candidatesToRemovePerCompetitor = new HashMap<>();
            removeCandidatesInTimeRange(timeRangeToRemoveCandidatesFrom, candidatesToRemovePerCompetitor, distanceCandidates);
            removeCandidatesInTimeRange(timeRangeToRemoveCandidatesFrom, candidatesToRemovePerCompetitor, xteCandidates);
            for (final Entry<Competitor, Set<Candidate>> e : candidatesToRemovePerCompetitor.entrySet()) {
                result.put(e.getKey(), new Pair<>(Collections.emptySet(), e.getValue()));
            }
        } else {
            for (final Competitor competitor : race.getRace().getCompetitors()) {
                final List<GPSFixMoving> newFixesForCompetitor = new ArrayList<>();
                final DynamicGPSFixTrack<Competitor, GPSFixMoving> track = race.getTrack(competitor);
                if (track != null) {
                    track.lockForRead();
                    try {
                        for (final Iterator<GPSFixMoving> i=track.getFixesIterator(startOfRangeToAdd, /* inclusive */ true); i.hasNext(); ) {
                            final GPSFixMoving fix = i.next();
                            if (fix.getTimePoint().after(endOfRangeToAdd)) {
                                break;
                            }
                            newFixesForCompetitor.add(fix);
                        }
                    } finally {
                        track.unlockAfterRead();
                    }
                }
                final Pair<Iterable<Candidate>, Iterable<Candidate>> newAndRemovedCandidatesForCompetitor = getCandidateDeltas(competitor, newFixesForCompetitor);
                result.put(competitor, newAndRemovedCandidatesForCompetitor);
            }
        }
        return result;
    }

    /**
     * @param candidatesRemovedPerCompetitor
     *            updated by this method; candidates removed from {@code candidatePerWaypointPerCompetitor} are added to
     *            this map, keyed by the respective competitor
     */
    private <K> void removeCandidatesInTimeRange(final TimeRange timeRangeToRemoveCandidatesFrom,
            final Map<Competitor, Set<Candidate>> candidatesRemovedPerCompetitor,
            Map<Competitor, Map<Waypoint, Map<K, Candidate>>> candidatePerWaypointPerCompetitor) {
        for (final Entry<Competitor, Map<Waypoint, Map<K, Candidate>>> e : candidatePerWaypointPerCompetitor.entrySet()) {
            Set<Candidate> candidatesRemoved = candidatesRemovedPerCompetitor.get(e.getKey());
            if (candidatesRemoved == null) {
                candidatesRemoved = new HashSet<>();
                candidatesRemovedPerCompetitor.put(e.getKey(), candidatesRemoved);
            }
            for (final Map<K, Candidate> m : e.getValue().values()) {
                for (final Iterator<Candidate> candidateIter=m.values().iterator(); candidateIter.hasNext(); ) {
                    final Candidate candidate = candidateIter.next();
                    if (timeRangeToRemoveCandidatesFrom.includes(candidate.getTimePoint())) {
                        candidateIter.remove();
                        candidatesRemoved.add(candidate);
                    }
                }
            }
        }
    }

    private class LimitedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private int limit;

        public LimitedLinkedHashMap(int limit) {
            super();
            this.limit = limit;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return this.size() > limit;
        }
    }

    @Override
    public Util.Pair<Iterable<Candidate>, Iterable<Candidate>> getAllCandidates(Competitor c) {
        Iterable<GPSFixMoving> fixes = getAllFixes(c);
        distanceCache.get(c).clear();
        xteCache.get(c).clear();
        synchronized (xteCandidates) {
            xteCandidates.get(c).clear();
        }
        synchronized (distanceCandidates) {
            distanceCandidates.get(c).clear();
        }
        return getCandidateDeltas(c, fixes);
    }

    @Override
    public Map<Competitor, List<GPSFixMoving>> calculateFixesAffectedByNewMarkFixes(Map<Mark, List<GPSFix>> markFixes) {
        // TODO Right now creates on time stretch between the 2 outside markfixes
        Map<Competitor, List<GPSFixMoving>> affectedFixes = new HashMap<>();
        TimePoint start = null;
        TimePoint end = null;
        for (Entry<Mark, List<GPSFix>> fixes : markFixes.entrySet()) {
            for (GPSFix fix : fixes.getValue()) {
                TimeRange timePoints = race.getOrCreateTrack(fixes.getKey()).getEstimatedPositionTimePeriodAffectedBy(fix);
                TimePoint newStart = timePoints.from();
                TimePoint newEnd = timePoints.to();
                start = start == null || start.after(newStart) ? newStart : start;
                end = end == null || end.before(newEnd) ? newEnd : end;
            }
        }
        for (Competitor c : race.getRace().getCompetitors()) {
            List<GPSFixMoving> competitorFixes = new ArrayList<>();
            DynamicGPSFixTrack<Competitor, GPSFixMoving> track = race.getTrack(c);
            GPSFixMoving comFix = track.getFirstFixAtOrAfter(start);
            if (comFix != null) {
                if (end != null) {
                    while (comFix != null && !comFix.getTimePoint().after(end)) {
                        competitorFixes.add(comFix);
                        distanceCache.get(c).remove(comFix);
                        xteCache.get(c).remove(comFix);
                        comFix = track.getFirstFixAfter(comFix.getTimePoint());
                    }
                } else {
                    while (comFix != null) {
                        competitorFixes.add(comFix);
                        distanceCache.get(c).remove(comFix);
                        xteCache.get(c).remove(comFix);
                        comFix = track.getFirstFixAfter(comFix.getTimePoint());
                    }
                }
            }
            if (!competitorFixes.isEmpty()) {
                affectedFixes.put(c, competitorFixes);
            }
        }
        return affectedFixes;
    }

    @Override
    public Util.Pair<Iterable<Candidate>, Iterable<Candidate>> getCandidateDeltas(Competitor c, Iterable<GPSFixMoving> fixes) {
        List<Candidate> newCans = new ArrayList<>();
        List<Candidate> wrongCans = new ArrayList<>();
        Course course = race.getRace().getCourse();
        course.lockForRead();
        try {
            Util.Pair<List<Candidate>, List<Candidate>> distanceCandidates = checkForDistanceCandidateChanges(c, fixes,
                    race.getRace().getCourse().getWaypoints());
            Util.Pair<List<Candidate>, List<Candidate>> xteCandidates = checkForXTECandidatesChanges(c, fixes, race
                    .getRace().getCourse().getWaypoints());
            logger.finest(distanceCandidates.getA().size() + " new Distance Candidates, " + xteCandidates.getA().size()
                    + " new XTE Candidates, " + distanceCandidates.getB().size() + " removed distance Candidates and "
                    + xteCandidates.getB().size() + " removed XTE Candidates.");
            newCans.addAll(xteCandidates.getA());
            newCans.addAll(distanceCandidates.getA());
            wrongCans.addAll(xteCandidates.getB());
            wrongCans.addAll(distanceCandidates.getB());
        } finally {
            course.unlockAfterRead();
        }
        return new Util.Pair<Iterable<Candidate>, Iterable<Candidate>>(newCans, wrongCans);
    }

    /**
     * For all waypoints starting at {@code zeroBasedIndexOfWaypointChanged-1} in the current course removes all
     * candidates for all competitors and starts from scratch to determine the candidates for the waypoints starting at
     * {@code zeroBasedIndexOfWaypointChanged-1}. The current and the removed candidates are returned, keyed by the
     * competitors; the {@link Pair#getA() first} element of each value pair has the candidates that are new, the
     * {@link Pair#getB() second} element has those that have been removed.
     */
    private Map<Competitor, Util.Pair<List<Candidate>, List<Candidate>>> invalidateAfterCourseChange(int zeroBasedIndexOfWaypointChanged) {
        ConcurrentMap<Competitor, Util.Pair<List<Candidate>, List<Candidate>>> result = new ConcurrentHashMap<>();
        Course course = race.getRace().getCourse();
        for (Competitor c : race.getRace().getCompetitors()) {
            distanceCache.get(c).clear();
            xteCache.get(c).clear();
        }
        List<Waypoint> changedWaypoints = new ArrayList<>();
        course.lockForRead();
        try {
            for (Waypoint w : course.getWaypoints()) {
                // The candidates for the predecessor of the first waypoint added/removed are also affected
                // because their adjacent leg will have changed its direction, so roundings / passings have
                // to be decided differently
                // TODO but why are *all* subsequent waypoints "invalidated" this way? Wouldn't we by that same token only have to move to the successor of the last waypoint changed?
                if (course.getIndexOfWaypoint(w) > zeroBasedIndexOfWaypointChanged - 2) {
                    changedWaypoints.add(w);
                }
            }
            logger.finer(()->"Changed waypoints: "+changedWaypoints);
            final Set<Callable<Void>> tasks = new HashSet<>();
            final Thread executingThread = Thread.currentThread(); // most likely the MarkPassingCalculator.Listen thread
            for (Competitor c : race.getRace().getCompetitors()) {
                tasks.add((race.getTrackedRegatta().cpuMeterCallable(()->{
                    LockUtil.propagateLockSetFrom(executingThread); // "inherit" the course read lock from the "calling" thread into the thread pool executor thread
                    try {
                        List<Candidate> badCans = new ArrayList<>();
                        List<Candidate> newCans = new ArrayList<>();
                        for (Waypoint w : changedWaypoints) {
                            Map<List<GPSFix>, Candidate> xteCans = getXteCandidates(c, w);
                            badCans.addAll(xteCans.values());
                            xteCans.clear();
                            Map<GPSFix, Candidate> distanceCans = getDistanceCandidates(c, w);
                            badCans.addAll(distanceCans.values());
                            distanceCans.clear();
                        }
                        Set<GPSFixMoving> allFixes = getAllFixes(c);
                        newCans.addAll(checkForDistanceCandidateChanges(c, allFixes, changedWaypoints).getA());
                        newCans.addAll(checkForXTECandidatesChanges(c, allFixes, changedWaypoints).getA());
                        result.put(c, new Util.Pair<List<Candidate>, List<Candidate>>(newCans, badCans));
                        return null;
                    } finally {
                        LockUtil.unpropagateLockSetFrom(executingThread); // "return" the course read lock from the thread pool executor thread to the "calling" thread
                    }
                }, CPUMeteringType.MARK_PASSINGS.name())));
            }
            ThreadPoolUtil.INSTANCE.invokeAllAndLogExceptions(executor, Level.SEVERE,
                    "Problem trying to update competitor candidate sets after waypoints starting at zero-based index "+
                            zeroBasedIndexOfWaypointChanged+" have changed: %s", tasks);
        } finally {
            course.unlockAfterRead();
        }

        return result;
    }

    /**
     * After a course change, described by the waypoints added ({@code addedWaypoints}) and the waypoints removed
     * ({@code removedWaypoints}), determines the candidates that are new and those that have been removed, keyed by the
     * competitors; the {@link Pair#getA() first} element of each value pair has the candidates that are new, the
     * {@link Pair#getB() second} element has those that have been removed.
     */
    @Override
    public Map<Competitor, Util.Pair<List<Candidate>, List<Candidate>>> updateWaypoints(
            Iterable<Waypoint> addedWaypoints, Iterable<Waypoint> removedWaypoints, int smallestIndex) {
        Map<Competitor, List<Candidate>> removedWaypointCandidates = removeWaypoints(removedWaypoints);
        Map<Competitor, Util.Pair<List<Candidate>, List<Candidate>>> newAndUpdatedCandidates = invalidateAfterCourseChange(smallestIndex);
        for (Entry<Competitor, List<Candidate>> entry : removedWaypointCandidates.entrySet()) {
            final Pair<List<Candidate>, List<Candidate>> candidatesForCompetitor = newAndUpdatedCandidates.get(entry.getKey());
            if (candidatesForCompetitor != null) { // only possible if an exception struck in invalidateAfterCourseChange for the competitor
                candidatesForCompetitor.getB().addAll(entry.getValue());
            }
        }
        return newAndUpdatedCandidates;
    }

    /**
     * For all {@code waypoints} removed, removes the {@link #passingInstructions} entry and clears all
     * {@link #xteCandidates} and {@link #distanceCandidates} for all competitors. The candidates cleared this
     * way are returned, keyed by the {@link Competitor} to which they belonged.
     */
    private Map<Competitor, List<Candidate>> removeWaypoints(Iterable<Waypoint> waypoints) {
        Map<Competitor, List<Candidate>> result = new HashMap<>();
        for (Competitor c : race.getRace().getCompetitors()) {
            result.put(c, new ArrayList<Candidate>());
        }
        for (Waypoint w : waypoints) {
            passingInstructions.remove(w);
            for (Entry<Competitor, List<Candidate>> entry : result.entrySet()) {
                Competitor c = entry.getKey();
                List<Candidate> badCans = entry.getValue();
                badCans.addAll(getXteCandidates(c, w).values());
                synchronized (xteCandidates) {
                    xteCandidates.get(c).remove(w);
                }
                badCans.addAll(getDistanceCandidates(c, w).values());
                synchronized (distanceCandidates) {
                    distanceCandidates.get(c).remove(w);
                }
            }
        }
        return result;
    }

    /**
     * @return a live collection of {@link #distanceCandidates distance candidates} for competitor {@code c} passing
     *         waypoint {@code w}.
     */
    private Map<GPSFix, Candidate> getDistanceCandidates(Competitor c, Waypoint w) {
        synchronized (distanceCandidates) {
            Map<GPSFix, Candidate> result = distanceCandidates.get(c).get(w);
            if (result == null) {
                result = new HashMap<>();
                distanceCandidates.get(c).put(w, result);
            }
            return result;
        }
    }

    /**
     * @return a live collection of {@link #xteCandidates cross track error candidates} for competitor {@code c} passing
     *         waypoint {@code w}.
     */
    private Map<List<GPSFix>, Candidate> getXteCandidates(Competitor c, Waypoint w) {
        synchronized (xteCandidates) {
            Map<List<GPSFix>, Candidate> result = xteCandidates.get(c).get(w);
            if (result == null) {
                result = new HashMap<>();
                xteCandidates.get(c).put(w, result);
            }
            return result;
        }
    }

    private PassingInstruction determinePassingInstructions(Waypoint w) {
        final Waypoint firstWaypoint = race.getRace().getCourse().getFirstWaypoint();
        final Waypoint lastWaypoint = race.getRace().getCourse().getLastWaypoint();
        PassingInstruction instruction = w.getPassingInstructions();
        if ((w.equals(firstWaypoint) || w.equals(lastWaypoint)) && instruction == PassingInstruction.Gate) {
            instruction = PassingInstruction.Line;
        }
        if (instruction == PassingInstruction.None || instruction == null) {
            final int numberOfMarks = Util.size(w.getMarks());
            if (numberOfMarks == 2) {
                if (w.equals(firstWaypoint) || w.equals(lastWaypoint)) {
                    instruction = PassingInstruction.Line;
                } else {
                    instruction = PassingInstruction.Gate;
                }
            } else if (numberOfMarks == 1) {
                instruction = PassingInstruction.Single_Unknown;
            } else {
                instruction = PassingInstruction.None;
            }
        }
        return instruction;
    }

    private Set<GPSFixMoving> getAllFixes(Competitor c) {
        Set<GPSFixMoving> fixes = new TreeSet<>(comp);
        if (timeRangeForValidCandidates.getTimeRangeOrNull() != null) {
            final DynamicGPSFixTrack<Competitor, GPSFixMoving> track = race.getTrack(c);
            track.lockForRead();
            try {
                for (GPSFixMoving fix : track.getFixes(
                        timeRangeForValidCandidates.getTimeRangeOrNull().from(), /* fromInclusive */ true,
                        timeRangeForValidCandidates.getTimeRangeOrNull().to(),   /*  toInclusive  */ true)) {
                    fixes.add(fix);
                }
            } finally {
                track.unlockAfterRead();
            }
        }
        return fixes;
    }

    /**
     * For each fix the distance to each waypoint is calculated. Then the fix is checked for being a candidate by
     * checking the distance of the fix before and after. If the distance at the fix is a local minimum and the
     * probability exceeds {@link #penaltyForSkipping}, the fix is considered a candidate.
     */
    private Util.Pair<List<Candidate>, List<Candidate>> checkForDistanceCandidateChanges(Competitor c,
            Iterable<GPSFixMoving> fixes, Iterable<Waypoint> waypoints) {
        Util.Pair<List<Candidate>, List<Candidate>> result = new Util.Pair<List<Candidate>, List<Candidate>>(
                new ArrayList<Candidate>(), new ArrayList<Candidate>());
        TreeSet<GPSFixMoving> affectedFixes = new TreeSet<GPSFixMoving>(comp);
        final GPSFixTrack<Competitor, GPSFixMoving> track = race.getTrack(c);
        // remember last fixes to avoid expensive searches (bug4221)
        GPSFixMoving lastIterationFix = null;
        GPSFixMoving lastIterationAfterFix = null;
        Iterator<GPSFixMoving> firstFixAfterIterator = null;
        for (GPSFixMoving fix : fixes) {
            if (timeRangeForValidCandidates.getTimeRangeOrNull() != null && timeRangeForValidCandidates.getTimeRangeOrNull().includes(fix.getTimePoint())) {
                affectedFixes.add(fix);
                GPSFixMoving fixBefore = null;
                GPSFixMoving fixAfter = null;
                final boolean fixIsValid;
                track.lockForRead();
                try {
                    fixIsValid = track.isValid(fix);
                    if (fixIsValid) {
                        TimePoint t = fix.getTimePoint();
                        if (fix == lastIterationAfterFix) {
                            // bug4221 try to avoid this expensive search in case the fixes are already more or less contiguous
                            fixBefore = lastIterationFix;
                        } else {
                            fixBefore = track.getLastFixBefore(t);
                            // bug4221: searching with getFixesIterator is about as expensive, especially in large tracks,
                            // as searching with getFirstFixAfter; but with getFixesIterator we have an iterator at hand
                            // that we can use in case it happens to deliver as fixAfter the next fix from fixes. In this
                            // case we can quickly obtain the next "firstFixAfter" by simply calling next() on the iterator.
                            firstFixAfterIterator = track.getFixesIterator(t, /* inclusive */ false);
                        }
                        try {
                            fixAfter = Util.nextOrNull(firstFixAfterIterator);
                        } catch (ConcurrentModificationException e) {
                            // the iterator may have been obtained in a previous look execution, and another
                            // fix may have been added to the track; let's obtain the iterator again. We're
                            // under the track's read lock here:
                            firstFixAfterIterator = track.getFixesIterator(t, /* inclusive */ false);
                            fixAfter = Util.nextOrNull(firstFixAfterIterator);
                        }
                        lastIterationFix = fix;
                        lastIterationAfterFix = fixAfter;
                    }
                } finally {
                    track.unlockAfterRead();
                }
                if (fixBefore != null) {
                    affectedFixes.add(fixBefore);
                }
                if (fixAfter != null) {
                    affectedFixes.add(fixAfter);
                }
            }
        }
        // affectedFixes now contains all fixes whose "candidate status" may have changed due to the fix insertion
        lastIterationFix = null;
        lastIterationAfterFix = null;
        firstFixAfterIterator = null;
        // for all affected fixes check whether they are a distance candidate now; this requires checking left and right
        // of all affected fixes. Note the difference with the left/right-looking above: there, it was used to determine
        // all fixes whose candidate status may have changed. Here we look left/right for each affected fix to ultimately
        // determine whether or not they are a candidate.
        for (GPSFixMoving fix : affectedFixes) {
            Position p = null;
            GPSFixMoving fixBefore;
            GPSFixMoving fixAfter;
            try {
                track.lockForRead();
                TimePoint timePoint = fix.getTimePoint();
                if (fix == lastIterationAfterFix) {
                    // bug4221 try to avoid this expensive search in case the fixes are already more or less contiguous
                    fixBefore = lastIterationFix;
                } else {
                    fixBefore = track.getLastFixBefore(timePoint);
                    // bug4221: searching with getFixesIterator is about as expensive, especially in large tracks,
                    // as searching with getFirstFixAfter; but with getFixesIterator we have an iterator at hand
                    // that we can use in case it happens to deliver as fixAfter the next fix from fixes. In this
                    // case we can quickly obtain the next "firstFixAfter" by simply calling next() on the iterator.
                    firstFixAfterIterator = track.getFixesIterator(timePoint, /* inclusive */ false);
                }
                try {
                    fixAfter = Util.nextOrNull(firstFixAfterIterator);
                } catch (ConcurrentModificationException e) {
                    // the iterator may have been obtained in a previous look execution, and another
                    // fix may have been added to the track; let's obtain the iterator again. We're
                    // under the track's read lock here:
                    firstFixAfterIterator = track.getFixesIterator(timePoint, /* inclusive */ false);
                    fixAfter = Util.nextOrNull(firstFixAfterIterator);
                }
                lastIterationFix = fix;
                lastIterationAfterFix = fixAfter;
            } finally {
                track.unlockAfterRead();
            }
            if (fixBefore != null && fixAfter != null) {
                TimePoint t = null;
                Map<Waypoint, List<Distance>> fixDistances = getDistances(c, fix); // TODO bug4831 consider interpolating between fixBefore/fix/fixAfter to handle small sampling rates better
                Map<Waypoint, List<Distance>> fixDistancesBefore = getDistances(c, fixBefore);
                Map<Waypoint, List<Distance>> fixDistancesAfter = getDistances(c, fixAfter);
                for (Waypoint w : waypoints) {
                    Boolean wasCan = false;
                    Boolean isCan = false;
                    Candidate oldCan = null;
                    Double probability = null;
                    Distance distance = null;
                    Double startProbabilityBasedOnOtherCompetitors = null;
                    double onCorrectSideOfWaypoint = 0.8;
                    List<Distance> waypointDistances = fixDistances.get(w);
                    List<Distance> waypointDistancesBefore = fixDistancesBefore.get(w);
                    List<Distance> waypointDistancesAfter = fixDistancesAfter.get(w);
                    // due to course changes, waypoints that exist in the waypoints collection may not have a corresponding
                    // key in passingInstructions' key set which is the basis for the waypoints for which getDistances(...)
                    // computes results; so we have to check for null here:
                    if (waypointDistances != null && waypointDistancesBefore != null && waypointDistancesAfter != null) {
                        Iterator<Distance> disIter = waypointDistances.iterator();
                        Iterator<Distance> disBeforeIter = waypointDistancesBefore.iterator();
                        Iterator<Distance> disAfterIter = waypointDistancesAfter.iterator();
                        boolean portMark = true;
                        while (disIter.hasNext() && disBeforeIter.hasNext() && disAfterIter.hasNext()) {
                            Distance dis = disIter.next();
                            Distance disBefore = disBeforeIter.next();
                            Distance disAfter = disAfterIter.next();
                            if (dis != null && disBefore != null && disAfter != null) {
                                if (Math.abs(dis.getMeters()) < Math.abs(disBefore.getMeters())
                                        && Math.abs(dis.getMeters()) < Math.abs(disAfter.getMeters())) {
                                    t = fix.getTimePoint();
                                    final MarkPositionAtTimePointCache markPositionCache = new MarkPositionAtTimePointCacheImpl(race, t);
                                    p = fix.getPosition();
                                    Double newProbability = getDistanceBasedProbability(w, t, dis, markPositionCache, race.getBoatOfCompetitor(c).getBoatClass().getHullLength());
                                    if (newProbability != null) {
                                        // FIXME why not generate the candidate here where we have all information at hand?
                                        final double newOnCorrectSideOfWaypointPenalty = getSidePenalty(w, p, t, portMark, markPositionCache);
                                        newProbability *= newOnCorrectSideOfWaypointPenalty * PENALTY_FOR_DISTANCE_CANDIDATES;
                                        final Double newStartProbabilityBasedOnOtherCompetitors;
                                        if (isStartWaypoint(w)) {
                                            newStartProbabilityBasedOnOtherCompetitors = getProbabilityForStartCandidateBasedOnOtherCompetitorsBehavior(c, t, markPositionCache);
                                            if (newStartProbabilityBasedOnOtherCompetitors != null) {
                                                newProbability *= newStartProbabilityBasedOnOtherCompetitors;
                                            }
                                        } else {
                                            newStartProbabilityBasedOnOtherCompetitors = null;
                                        }
                                        if (newProbability > penaltyForSkipping
                                                && (probability == null || newProbability > probability)) {
                                            isCan = true;
                                            probability = newProbability;
                                            onCorrectSideOfWaypoint = newOnCorrectSideOfWaypointPenalty;
                                            distance = dis;
                                            startProbabilityBasedOnOtherCompetitors = newStartProbabilityBasedOnOtherCompetitors;
                                        }
                                    }
                                }
                            }
                            portMark = false;
                        }
                        oldCan = getDistanceCandidates(c, w).get(fix);
                        if (oldCan != null) {
                            wasCan = true;
                        }
                        if (!wasCan && isCan) {
                            Candidate newCan = new DistanceCandidateImpl(race.getRace().getCourse().getIndexOfWaypoint(w) + 1,
                                    t, probability, startProbabilityBasedOnOtherCompetitors, w, onCorrectSideOfWaypoint, distance);
                            getDistanceCandidates(c, w).put(fix, newCan);
                            result.getA().add(newCan);
                            logger.finest("Added distance candidate " + newCan.toString() + " for " + c);
                        } else if (wasCan && !isCan) {
                            getDistanceCandidates(c, w).remove(fix);
                            result.getB().add(oldCan);
                        } else if (wasCan && isCan && oldCan.getProbability() != probability) {
                            Candidate newCan = new DistanceCandidateImpl(race.getRace().getCourse().getIndexOfWaypoint(w) + 1,
                                    t, probability, startProbabilityBasedOnOtherCompetitors, w, onCorrectSideOfWaypoint, distance);
                            getDistanceCandidates(c, w).put(fix, newCan);
                            result.getA().add(newCan);
                            logger.finest("Added distance candidate " + newCan.toString() + " for " + c);
                            result.getB().add(oldCan);
                        }
                    }
                }
            }
        }
        return result;
    }

    private Map<Waypoint, List<Distance>> getDistances(Competitor c, GPSFix fix) {
        // TODO Possibly for specific waypoints
        Map<Waypoint, List<Distance>> result = distanceCache.get(c).get(fix);
        final MarkPositionAtTimePointCache markPositionCache = new MarkPositionAtTimePointCacheImpl(race, fix.getTimePoint());
        if (result == null) {
            // Else calculate distances and put them into the cache
            result = new LinkedHashMap<>();
            Course course = race.getRace().getCourse();
            course.lockForRead();
            try {
                for (Waypoint w : course.getWaypoints()) {
                    List<Distance> distances = calculateDistance(fix.getPosition(), w, fix.getTimePoint(), markPositionCache);
                    result.put(w, distances);
                }
            } finally {
                course.unlockAfterRead();
            }
            distanceCache.get(c).put(fix, result);
        }
        return result;
    }

    /**
     * For each fix the cross-track error(s) to each waypoint are calculated. Then all Util.Pairs of fixes are checked
     * for being a candidate.
     * 
     * @param waypointAsList
     */
    private Util.Pair<List<Candidate>, List<Candidate>> checkForXTECandidatesChanges(Competitor c,
            Iterable<GPSFixMoving> fixes, Iterable<Waypoint> waypoints) {
        Util.Pair<List<Candidate>, List<Candidate>> result = new Util.Pair<List<Candidate>, List<Candidate>>(
                new ArrayList<Candidate>(), new ArrayList<Candidate>());
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> track = race.getTrack(c);
        // remember last fixes to avoid expensive searches (bug4221)
        GPSFixMoving lastIterationFix = null;
        GPSFixMoving lastIterationAfterFix = null;
        Iterator<GPSFixMoving> firstFixAfterIterator = null;
        for (GPSFixMoving fix : fixes) {
            if (timeRangeForValidCandidates.getTimeRangeOrNull() != null && timeRangeForValidCandidates.getTimeRangeOrNull().includes(fix.getTimePoint())) {
                TimePoint t = fix.getTimePoint();
                GPSFixMoving fixBefore = null;
                GPSFixMoving fixAfter = null;
                final boolean fixIsValid;
                track.lockForRead();
                try {
                    fixIsValid = track.isValid(fix);
                    if (fixIsValid) {
                        if (fix == lastIterationAfterFix) {
                            // bug4221 try to avoid this expensive search in case the fixes are already more or less contiguous
                            fixBefore = lastIterationFix;
                        } else {
                            fixBefore = track.getLastFixBefore(t);
                            // bug4221: searching with getFixesIterator is about as expensive, especially in large tracks,
                            // as searching with getFirstFixAfter; but with getFixesIterator we have an iterator at hand
                            // that we can use in case it happens to deliver as fixAfter the next fix from fixes. In this
                            // case we can quickly obtain the next "firstFixAfter" by simply calling next() on the iterator.
                            firstFixAfterIterator = track.getFixesIterator(t, /* inclusive */ false);
                        }
                        try {
                            fixAfter = Util.nextOrNull(firstFixAfterIterator);
                        } catch (ConcurrentModificationException e) {
                            firstFixAfterIterator = track.getFixesIterator(t, /* inclusive */ false);
                            fixAfter = Util.nextOrNull(firstFixAfterIterator);
                        }
                        lastIterationFix = fix;
                        lastIterationAfterFix = fixAfter;
                    }
                } finally {
                    track.unlockAfterRead();
                }
                if (fixIsValid) {
                    Map<Waypoint, List<Distance>> xtesBefore = null;
                    Map<Waypoint, List<Distance>> xtesAfter = null;
                    TimePoint tBefore = null;
                    TimePoint tAfter = null;
                    if (fixBefore != null) {
                        xtesBefore = getXTE(c, fixBefore);
                        tBefore = fixBefore.getTimePoint();
                    }
                    if (fixAfter != null) {
                        xtesAfter = getXTE(c, fixAfter);
                        tAfter = fixAfter.getTimePoint();
                    }
                    Map<Waypoint, List<Distance>> xtes = getXTE(c, fix);
                    for (Waypoint w : waypoints) {
                        List<List<GPSFix>> oldCandidates = new ArrayList<>();
                        Map<List<GPSFix>, Candidate> newCandidates = new HashMap<List<GPSFix>, Candidate>();
                        Map<List<GPSFix>, Candidate> waypointCandidates = getXteCandidates(c, w);
                        for (List<GPSFix> fixPair : waypointCandidates.keySet()) {
                            if (fixPair.contains(fix)) {
                                oldCandidates.add(fixPair);
                            }
                        }
                        List<Distance> wayPointXTEs = xtes.get(w);
                        final int size = wayPointXTEs == null ? 0 : wayPointXTEs.size();
                        if (size > 0) {
                            Double xte = wayPointXTEs.get(0).getMeters();
                            if (xte == 0) {
                                newCandidates.put(Arrays.asList(fix, fix), createCandidate(c, 0, 0, t, t, w, true));
                            } else {
                                if (fixAfter != null && xtesAfter != null && xtesAfter.get(w) != null && !xtesAfter.get(w).isEmpty()) {
                                    Double xteAfter = xtesAfter.get(w).get(0).getMeters();
                                    if (xteAfter != null && xte < 0 != xteAfter <= 0) {
                                        newCandidates.put(Arrays.asList(fix, fixAfter),
                                                createCandidate(c, xte, xteAfter, t, tAfter, w, true));
                                    }
                                }
                                if (fixBefore != null && xtesBefore.get(w) != null && !xtesBefore.get(w).isEmpty()) {
                                    Double xteBefore = xtesBefore.get(w).get(0).getMeters();
                                    if (xte < 0 != xteBefore <= 0) {
                                        newCandidates.put(Arrays.asList(fixBefore, fix),
                                                createCandidate(c, xteBefore, xte, tBefore, t, w, true));
                                    }
                                }
                            }
                        }
                        if (size > 1) {
                            Double xte = wayPointXTEs.get(1).getMeters();
                            if (xte == 0) {
                                newCandidates.put(Arrays.asList(fix, fix), createCandidate(c, 0, 0, t, t, w, false));
                            } else {
                                if (fixAfter != null && xtesAfter != null && xtesAfter.get(w).size() >= 2) {
                                    Double xteAfter = xtesAfter.get(w).get(1).getMeters();
                                    if (xte < 0 != xteAfter <= 0) {
                                        newCandidates.put(Arrays.asList(fix, fixAfter),
                                                createCandidate(c, xte, xteAfter, t, tAfter, w,
                                                        /* still portMark in case of single-mark waypoint */ Util.size(w.getMarks()) < 2));
                                    }
                                }
                                if (fixBefore != null && xtesBefore.get(w).size() >= 2) {
                                    Double xteBefore = xtesBefore.get(w).get(1).getMeters();
                                    if (xte < 0 != xteBefore <= 0) {
                                        newCandidates.put(Arrays.asList(fixBefore, fix),
                                                createCandidate(c, xteBefore, xte, tBefore, t, w,
                                                        /* still portMark in case of single-mark waypoint */ Util.size(w.getMarks()) < 2));
                                    }
                                }
                            }
                        }
                        for (Entry<List<GPSFix>, Candidate> candidateWithFixes : newCandidates.entrySet()) {
                            Candidate newCan = candidateWithFixes.getValue();
                            List<GPSFix> canFixes = candidateWithFixes.getKey();
                            if (oldCandidates.contains(canFixes)) {
                                oldCandidates.remove(canFixes);
                                Candidate oldCan = waypointCandidates.get(canFixes);
                                if (newCan.compareTo(oldCan) != 0) {
                                    result.getB().add(oldCan);
                                    waypointCandidates.remove(canFixes);
                                    if (newCan.getProbability() > penaltyForSkipping) {
                                        result.getA().add(newCan);
                                        logger.finest("Added XTE " + newCan.toString() + " for " + c);
                                        waypointCandidates.put(canFixes, newCan);
                                    }
                                }
                            } else {
                                if (newCan.getProbability() > penaltyForSkipping) {
                                    result.getA().add(newCan);
                                    logger.finest("Added XTE " + newCan.toString() + " for " + c);
                                    waypointCandidates.put(canFixes, newCan);
                                }
                            }
                        }
                        for (List<GPSFix> badCanFixes : oldCandidates) {
                            result.getB().add(waypointCandidates.get(badCanFixes));
                            waypointCandidates.remove(badCanFixes);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns a non-{@code null} mapping from the waypoints currently in the course, telling the {@code fix}'s
     * cross-track error for each waypoint. There may be "excess" entries from older waypoints that have already
     * been removed from the course which may be reflected only by a later call to {@link #invalidateAfterCourseChange(int)}.
     * See also bug 5021.
     * 
     * @return if for a waypoint the mark positions are known, the resulting map will contain a non-empty list that for
     *         each way of passing the waypoint (e.g., for a gate the competitor can round the left or the right mark) the
     *         cross track error of the {@code fix} to the virtual line that must be crossed is contained; if the mark
     *         positions are not known, that waypoint's value will be an empty list.
     */
    private Map<Waypoint, List<Distance>> getXTE(Competitor c, GPSFix fix) {
        Map<Waypoint, List<Distance>> result = xteCache.get(c).get(fix);
        Course course = race.getRace().getCourse();
        course.lockForRead();
        try {
            if (result == null) {
                result = new HashMap<>();
                xteCache.get(c).put(fix, result);
            }
            final Position p = fix.getPosition();
            final TimePoint t = fix.getTimePoint();
            final MarkPositionAtTimePointCache markPositionCache = new MarkPositionAtTimePointCacheImpl(race, t);
            for (Waypoint w : course.getWaypoints()) {
                if (!result.containsKey(w)) { // bug5021: calculate in case waypoint was added since last call
                    List<Distance> distances = new ArrayList<>();
                    result.put(w, distances);
                    for (Util.Pair<Position, Bearing> crossingInfo : getCrossingInformation(w, t, markPositionCache)) {
                        if (crossingInfo.getA() != null && crossingInfo.getB() != null) {
                            distances.add(p.crossTrackError(crossingInfo.getA(), crossingInfo.getB()));
                        }
                    }
                }
            }
        } finally {
            course.unlockAfterRead();
        }
        return result;
    }

    /**
     * {@code xte1} and {@code xte2} are expected to have different signums or both be 0. The method
     * interpolates between the time points {@code t1} and {@code t2} according to where the XTE is
     * assumed to have crossed 0. The candidate is then generated for this interpolated time point.
     */
    private Candidate createCandidate(Competitor c, double xte1, double xte2, TimePoint t1, TimePoint t2, Waypoint w,
            Boolean portMark) {
        final long differenceInMillis = t2.asMillis() - t1.asMillis();
        final double ratio = (Math.abs(xte1) / (Math.abs(xte1) + Math.abs(xte2)));
        final TimePoint t = t1.plus((long) (differenceInMillis * ratio));
        final Position p = race.getTrack(c).getEstimatedPosition(t, false);
        final List<Distance> distances = calculateDistance(p, w, t, new MarkPositionAtTimePointCacheImpl(race, t));
        final Distance d = portMark ? distances.get(0) : distances.get(1);
        final MarkPositionAtTimePointCache markPositionCache = new MarkPositionAtTimePointCacheImpl(race, t);
        final double sidePenalty = getSidePenalty(w, p, t, portMark, markPositionCache);
        final Double distanceBasedProbability = getDistanceBasedProbability(w, t, d, markPositionCache, race.getBoatOfCompetitor(c).getBoatClass().getHullLength());
        double probability = distanceBasedProbability == null ? sidePenalty : distanceBasedProbability * sidePenalty;
        final Double passesInTheRightDirectionProbability = passesInTheRightDirection(w, xte1, xte2, portMark);
        // null would mean "unknown"; no penalty for those cases
        probability = passesInTheRightDirectionProbability == null ? probability : probability * passesInTheRightDirectionProbability;
        final Double startProbabilityBasedOnOtherCompetitors;
        if (isStartWaypoint(w)) {
            // add a penalty for a start candidate if we don't know the start time, it's not a gate start and
            // at time point t the other competitors are largely not even close to the start waypoint;
            // this will make start candidates much more likely if many other competitors are also very close to the
            // start, and it will help ruling out those candidates where someone is practicing a start just for themselves
            startProbabilityBasedOnOtherCompetitors = getProbabilityForStartCandidateBasedOnOtherCompetitorsBehavior(c, t, markPositionCache);
            if (startProbabilityBasedOnOtherCompetitors != null) {
                probability *= startProbabilityBasedOnOtherCompetitors;
            }
        } else {
            startProbabilityBasedOnOtherCompetitors = null;
        }
        return new XTECandidateImpl(race.getRace().getCourse().getIndexOfWaypoint(w) + 1, t, probability,
                startProbabilityBasedOnOtherCompetitors, w, sidePenalty, passesInTheRightDirectionProbability);
    }
    
    public static class AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine {
        private final Distance absoluteGeometricDistance;
        private final Distance signedProjectedDistance;
        public AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine(Distance absoluteGeometricDistance,
                Distance signedProjectedDistance) {
            super();
            this.absoluteGeometricDistance = absoluteGeometricDistance;
            this.signedProjectedDistance = signedProjectedDistance;
        }
        public Distance getAbsoluteGeometricDistance() {
            return absoluteGeometricDistance;
        }
        public Distance getSignedProjectedDistance() {
            return signedProjectedDistance;
        }
        @Override
        public String toString() {
            return "AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine [absoluteGeometricDistance="
                    + absoluteGeometricDistance + ", signedProjectedDistance=" + signedProjectedDistance + "]";
        }
    }

    /**
     * For a fixed line start it is very likely that all competitors pass the start waypoint at about the same time, or
     * that only a few competitors lag behind, e.g., because of start time penalties. This requires everyone to at least
     * be pretty close to the start waypoint at the start time, of if we're looking at a competitor starting late, the
     * field would at least have little variance and be consistently on the course side of the start line.
     * <p>
     * 
     * Therefore, a start mark passing candidate is very likely when a large share of competitors is very close to the
     * start line. If the other competitors are not very close to the start line, a start may still be likely if the
     * others are on the course side and have similar distance to the start line, whereas it becomes pretty unlikely
     * when everyone is sailing around anywhere on the course or behind the line but not being in close proximity to the
     * line and not having similar distances to the start line while being on course side.
     * 
     * <ul>
     * <li>check distance variance of other competitors; low variance could mean they started jointly</li>
     * <li>for start *line*, consider the side on which the other competitors are; don't rule out a late start if other
     * competitors have low distance variance and are on course side of the line</li>
     * <li>generally consider distance of other competitors to line</li>
     * <li>dampen "outliers" such as trackers forgotten in harbor or trackers not currently sending (80% of competitors
     * that look like they are starting seems like a good guess)</li>
     * </ul>
     * 
     * Approach: compute signed distances to start line (positive meaning on course side). Sort by absolute distance and
     * compute a {@link #weight(double) weighted} average where the boats farthest away are considered less relevant than
     * the ones closer, leading to a natural outlier removal.<p>
     * 
     * For the special case of a late starter, consider the weighted variance of the signed distance from the start line.
     * A low variance with a positive average (meaning on course side) indicates that everyone else may have started around the
     * same time, making this a probable late starter's candidate. Therefore, low variance with positive average shall increase
     * a probability that was low because of a large weighted average distance.
     * @param t
     *            the time point at which to consider the other competitors behavior relative to the start waypoint
     * 
     * @return {@code null} if the {@link #race} has a gate start or the start time is known; a probability between 0..1
     *         (inclusive) where 0 means that based on all but {@code c}'s relation to the start line it seems
     *         completely unlikely that a candidate for {@code c} at time {@code t} could have been a start candidate; 1
     *         meaning that based on the other competitors' relation to the start line at time point {@code t} is seems
     *         a fact that this must have been the start.
     * 
     */
    private Double getProbabilityForStartCandidateBasedOnOtherCompetitorsBehavior(Competitor c, TimePoint t, MarkPositionAtTimePointCache markPositionCache) {
        assert t.equals(markPositionCache.getTimePoint());
        assert race == markPositionCache.getTrackedRace();
        final Double result;
        if (race.getStartOfRace(/* inferred */ false) == null && race.isGateStart() != Boolean.TRUE) {
            final Waypoint start = race.getRace().getCourse().getFirstWaypoint();
            final Iterable<Pair<Position, Bearing>> crossingInformationForStart = getCrossingInformation(start, t, markPositionCache);
            if (start == null) {
                result = 1.0;
            } else {
                // if the start waypoint's passing instructions are unknown, assume it's a line
                final boolean startIsLine = getPassingInstructions(start) == PassingInstruction.Line;
                final List<AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine> distancesToStartLineOfOtherCompetitors = new ArrayList<>();
                for (final Competitor otherCompetitor : race.getRace().getCompetitors()) {
                    if (otherCompetitor != c) {
                        final DynamicGPSFixTrack<Competitor, GPSFixMoving> track = race.getTrack(otherCompetitor);
                        if (track != null) {
                            final Position estimatedPositionAtT = track.getEstimatedPosition(t, /* extrapolate */ true);
                            // consider that a position may not be available for that other competitor, although a track exists
                            if (estimatedPositionAtT != null) {
                                final Distance otherCompetitorsDistanceToStartAtT = getMinDistanceOrNull(calculateDistance(estimatedPositionAtT, start, t, markPositionCache));
                                if (otherCompetitorsDistanceToStartAtT != null) {
                                    final Distance crossTrackError;
                                    if (startIsLine) {
                                        Pair<Position, Bearing> crossingInformationForStartLine = crossingInformationForStart.iterator().next();
                                        crossTrackError = estimatedPositionAtT.crossTrackError(crossingInformationForStartLine.getA(),
                                                crossingInformationForStartLine.getB());
                                    } else {
                                        crossTrackError = null;
                                    }
                                    distancesToStartLineOfOtherCompetitors.add(new
                                            AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine(otherCompetitorsDistanceToStartAtT, crossTrackError));
                                }
                            }
                        }
                    }
                }
                result = getProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(distancesToStartLineOfOtherCompetitors, startIsLine);
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * The method computes two probabilities of the candidate being a start: one based on the other competitors largely
     * being close to the start line, meaning a regular start where the competitor starts roughly at the same time as
     * everybody else; the other probability computed is based on the other competitors largely being in a similar
     * distance to the start line on the course side, suggesting that the competitor is starting late, but mostly
     * everybody else did start earlier and around the same time. The probability returned is the probability of
     * the start being the one <em>or</em> the other scenario which mathematically is represented by using the inverted
     * probabilities of the two start options, multiplying them and inverting the resulting probability.<p>
     * 
     * Access needs to be protected to satisfy Maven-based test cases in com.sap.sailing.domain.test bundle which is not a fragment
     */
    protected Double getProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(
            final List<AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine> distancesToStartLineOfOtherCompetitors, boolean startIsLine) {
        final Double result;
        // sort by the absolute distance to start line
        // boats within one hull length of the start line are great indicators that we're at the start:
        final Distance hullLength = race.getRace().getBoatClass().getHullLength();
        final Distance weightedAverageAbsoluteDistance;
        if (!distancesToStartLineOfOtherCompetitors.isEmpty()) {
            Collections.sort(distancesToStartLineOfOtherCompetitors, (a, b)->a.getAbsoluteGeometricDistance().compareTo(b.getAbsoluteGeometricDistance()));
            Distance weightedAbsoluteDistanceSum = Distance.NULL;
            int i=0;
            double weightSum = 0;
            for (final AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine d : distancesToStartLineOfOtherCompetitors) {
                final double weight = weight(((double) i)/distancesToStartLineOfOtherCompetitors.size());
                final Distance weightedDistance = d.getAbsoluteGeometricDistance().scale(weight);
                weightedAbsoluteDistanceSum = weightedAbsoluteDistanceSum.add(weightedDistance);
                weightSum += weight;
                i++;
            }
            weightedAverageAbsoluteDistance = weightedAbsoluteDistanceSum.scale(1. / weightSum);
            final double probabilityOfStartWithOthers = Math.max(WORST_PENALTY_FOR_OTHER_COMPETITORS_BEING_FAR_FROM_START, Math.min(1.0,
                    (1.-(1.-WORST_PENALTY_FOR_OTHER_COMPETITORS_BEING_FAR_FROM_START)/NUMBER_OF_HULL_LENGTHS_DISTANCE_FROM_START_AT_WHICH_WORST_PENALTY_APPLIES
                            * (weightedAverageAbsoluteDistance.divide(hullLength)-1))));
            if (startIsLine) {
                // Now look at the variance, particularly on course side: low variance may mean we are looking at a late starter
                // sort such that the boats with the least value (greatest negative amount) comes first and the last ~20% are largely ignored
                Collections.sort(distancesToStartLineOfOtherCompetitors, (a, b)->a.getSignedProjectedDistance().compareTo(b.getSignedProjectedDistance()));
                Distance weightedSignedDistanceSum = Distance.NULL;
                weightSum = 0;
                i=0;
                for (final AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine d : distancesToStartLineOfOtherCompetitors) {
                    final double weight = weight(((double) i)/distancesToStartLineOfOtherCompetitors.size());
                    weightedSignedDistanceSum  = weightedSignedDistanceSum.add(d.getSignedProjectedDistance().scale(
                            weight));
                    weightSum += weight;
                    i++;
                }
                final Distance weightedAverageSignedDistance = weightedSignedDistanceSum.scale(1. / weightSum);
                i=0;
                double signedDistanceVarianceSumInSquareMeters = 0;
                weightSum = 0;
                for (final AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine d : distancesToStartLineOfOtherCompetitors) {
                    final double differenceFromWeightedAverageInMeters = d.getSignedProjectedDistance().getMeters() - weightedAverageSignedDistance.getMeters();
                    final double weight = weight(((double) i)/distancesToStartLineOfOtherCompetitors.size());
                    signedDistanceVarianceSumInSquareMeters += weight*differenceFromWeightedAverageInMeters*differenceFromWeightedAverageInMeters;
                    weightSum += weight;
                    i++;
                }
                final double weightedVarianceInSquareMeters = signedDistanceVarianceSumInSquareMeters / weightSum;
                final double weightedVarianceRatioToSquaredHullLength = weightedVarianceInSquareMeters / hullLength.getMeters() / hullLength.getMeters();
                final double probabilityOfLateStart = Math.max(WORST_PENALTY_FOR_OTHER_COMPETITORS_BEING_FAR_FROM_START, Math.min(1.0,
                        (1.-(1.-WORST_PENALTY_FOR_OTHER_COMPETITORS_BEING_FAR_FROM_START)/NUMBER_OF_HULL_LENGTHS_DISTANCE_FROM_START_AT_WHICH_WORST_PENALTY_APPLIES
                                * (weightedVarianceRatioToSquaredHullLength-1))));
                result = 1. - (1.-probabilityOfLateStart) * (1.-probabilityOfStartWithOthers);
            } else {
                result = probabilityOfStartWithOthers;
            }
        } else {
            // no distance for any other competitor from the start line was available
            result = 1.0;
        }
        return result;
    }

    /**
     * Idea: sort and cut off the worst 20%. We played with weight functions for averaging the distances such that after sorting them
     * in increasing order the smaller ones get greater weight and the "outlier" ones towards the end of the sorted set receive small
     * weights. Still, if even the "best" (least) distances are far away, the result will be a low probability.
     * A function that could be helpful is this: -atan(20*(x-0.8)) + Pi/2)/Pi. The 0.8 represents the 80% starting from which the
     * weight declines sharply. The factor of 20 represents how sharp the decline is. x is between 0 and 1, representing the start
     * and the end of the sorted distance collection, respectively. Outputs are between 0 and 1, representing the weight to assign
     * to a distance while averaging.
     */
    private double weight(double relativePositionInDistanceCollectionInIncreasingOrder) {
        final double SHARPNESS_OF_DECLINE = 100;
        final double CENTER_OF_DECLINE = 0.8;
        return (-Math.atan(SHARPNESS_OF_DECLINE*(relativePositionInDistanceCollectionInIncreasingOrder-CENTER_OF_DECLINE))+Math.PI/2) / Math.PI;
    }

    private Distance getMinDistanceOrNull(List<Distance> distances) {
        Distance result = null;
        for (Distance d : distances) {
            if (d != null && (result == null || result.compareTo(d) > 0)) {
                result = d;
            }
        }
        return result;
    }

    private boolean isStartWaypoint(Waypoint w) {
        return race.getRace().getCourse().getFirstWaypoint() == w;
    }

    /**
     * Determines whether a candidate is on the correct side of a waypoint. This is defined by the crossing information.
     * The cross-track error of <code>p</code> to the crossing Position and the crossing Bearing rotated by 90deg need
     * to be negative. If the passing instructions are {@link PassingInstruction#Line}, it checks whether the boat
     * passed between the two marks.
     */
    private double getSidePenalty(Waypoint w, Position p, TimePoint t, boolean portMark, MarkPositionAtTimePointCache markPositionCache) {
        assert t.equals(markPositionCache.getTimePoint());
        assert race == markPositionCache.getTrackedRace();
        boolean isOnRightSide = true;
        Distance onWrongSide = new MeterDistance(0);        
        PassingInstruction instruction = getPassingInstructions(w);
        if (instruction == PassingInstruction.Line) {
            List<Position> pos = new ArrayList<>();
            for (Mark m : w.getMarks()) {
                Position po = markPositionCache.getEstimatedPosition(m);
                if (po == null) {
                    isOnRightSide = true;
                    break;
                }
                pos.add(po);
            }
            if (pos.size() != 2) {
                isOnRightSide = true;
            }
            Position leftMarkPos = pos.get(0);
            Position rightMarkPos = pos.get(1);
            Bearing diff1 = leftMarkPos.getBearingGreatCircle(p)
                    .getDifferenceTo(leftMarkPos.getBearingGreatCircle(rightMarkPos));
            Bearing diff2 = rightMarkPos.getBearingGreatCircle(p)
                    .getDifferenceTo(rightMarkPos.getBearingGreatCircle(leftMarkPos));
            if (Math.abs(diff1.getDegrees()) > 90 || Math.abs(diff2.getDegrees()) > 90) {
                isOnRightSide =  false;
                Distance leftDistance = p.getDistance(leftMarkPos);
                Distance rightDistance = p.getDistance(rightMarkPos);
                onWrongSide = leftDistance.getMeters() < rightDistance.getMeters() ? leftDistance : rightDistance;
            }
        } else {
            Mark m = null;
            if (instruction == PassingInstruction.Single_Unknown || instruction == PassingInstruction.Port || instruction == PassingInstruction.Starboard
                    || instruction == PassingInstruction.FixedBearing || instruction == PassingInstruction.Offset) {
                m = w.getMarks().iterator().next();
            } else if (instruction == PassingInstruction.Gate) {
                Util.Pair<Mark, Mark> pair = getPortAndStarboardMarks(t, w, markPositionCache);
                m = portMark ? pair.getA() : pair.getB();
            }
            if (m != null) {
                Util.Pair<Position, Bearing> crossingInfo = Util.get(getCrossingInformation(w, t, markPositionCache), portMark ? 0 : 1);
                isOnRightSide = p.crossTrackError(crossingInfo.getA(), crossingInfo.getB().add(new DegreeBearingImpl(90))).getMeters() < 0;
                if (isOnRightSide == false) {
                    onWrongSide = p.getDistance(crossingInfo.getA());
                }
            }
        }
        final double result;
        if (isOnRightSide == true) {
            result = 1;
        } else {
            //TODO There should be a constant (either the 1.5 or the -0.2) which controls how strict the the curve  is.
            result = Math.min(1.0, (1-PENALTY_FOR_WRONG_SIDE) *
                            // consider the possibility that both, mark and boat could have been GPSFix.TYPICAL_HDOP off and
                            // only start penalizing beyond this distance
                            Math.pow(1.5, -0.2 * onWrongSide.add(GPSFix.TYPICAL_HDOP.scale(-2.0)).getMeters())
                            + PENALTY_FOR_WRONG_SIDE);
        }
        return result;
    }

    /**
     * Determines whether a candidate passes a waypoint in the right direction. A probability of 1 is assigned in case
     * it does. Otherwise, depending on the passing instructions, {@link #PENALTY_FOR_WRONG_DIRECTION} or
     * {@link #PENALTY_FOR_LINE_PASSED_IN_WRONG_DIRECTION} is returned as a "probability penalty" for passing the
     * waypoint in the wrong direction. The penalty is worse if {@link PassingInstruction#Line lines} are passed in the
     * wrong direction. Can only be applied to XTE-Candidates. For marks passed on port, the cross-track error should
     * switch from positive to negative and vice versa. Lines are also from positive to negative as the cross-track
     * error to a line is always positive when approaching it from the correct side.
     * 
     * @return {@code null} in case the passing instructions aren't defined and therefore the correct direction is not
     *         known
     */
    private Double passesInTheRightDirection(Waypoint w, double xte1, double xte2, boolean portMark) {
        final Double result;
        PassingInstruction instruction = getPassingInstructions(w);
        if (instruction == PassingInstruction.Single_Unknown) {
            result = null;
        } else if (instruction == PassingInstruction.Port
                || (instruction == PassingInstruction.Gate && portMark)) {
            result = xte1 > xte2 ? 1 : PENALTY_FOR_WRONG_DIRECTION;
        } else if (instruction == PassingInstruction.Starboard || (instruction == PassingInstruction.Gate && !portMark)) {
            result = xte1 < xte2 ? 1 : PENALTY_FOR_WRONG_DIRECTION;
        } else if (instruction == PassingInstruction.Line) {
            result = xte1 > xte2 ? 1 : PENALTY_FOR_LINE_PASSED_IN_WRONG_DIRECTION;
        } else {
            result = null;
        }
        return result;
    }

    /**
     * @return a probability based on the distance to <code>w</code>; for single marks the average leg lengths before
     *         and after the waypoint {@code w} is also taken into account; for two-mark waypoints such as gates and
     *         lines it seems fair to assume that the length of the adjacent legs should not play a role in how accurate
     *         the competitor needs to pass the waypoint. Here, the hull length is taken into account, assuming that the distance
     *         from the waypoint will be influenced by the size of the boat, specifically in "traffic."
     */
    private Double getDistanceBasedProbability(Waypoint w, TimePoint t, Distance distance, MarkPositionAtTimePointCache markPositionCache, Distance hullLength) {
        assert t.equals(markPositionCache.getTimePoint());
        assert race == markPositionCache.getTrackedRace();
        assert distance.getMeters() >= 0;
        final Double result;
        if (Util.size(w.getControlPoint().getMarks())>1) {
            result = 1 / (STRICTNESS_OF_DISTANCE_BASED_PROBABILITY/* Raising this will make it stricter */
                    // for a two-mark control point such as a gate or a line only consider the relation of
                    // the distance to 150x the boat length after taking off 2xHDOP and two boat lengths 
                    * Math.abs(Math.max(0.0, distance.add(GPSFix.TYPICAL_HDOP.add(hullLength).scale(-2)).
                            divide(hullLength.scale(150)))) + 1);
        } else {
            Distance legLength = getAverageLengthOfAdjacentLegs(t, w, markPositionCache);
            if (legLength != null) {
                result = 1 / (STRICTNESS_OF_DISTANCE_BASED_PROBABILITY/* Raising this will make it stricter */
                        // reduce distance by 2x the typical HDOP, accounting for the possibility that some distance from the mark
                        // may have been caused by inaccurate GPS tracking
                        * Math.abs(Math.max(0.0, distance.add(GPSFix.TYPICAL_HDOP.add(hullLength).scale(-2)).divide(legLength))) + 1);
            } else {
                result = null;
            }
        }
        return result;
    }

    /**
     * @return an average of the estimated legs before and after <code>w</code>.
     */
    private Distance getAverageLengthOfAdjacentLegs(TimePoint t, Waypoint w, MarkPositionAtTimePointCache markPositionCache) {
        assert t.equals(markPositionCache.getTimePoint());
        assert race == markPositionCache.getTrackedRace();
        final Distance result;
        final Course course = race.getRace().getCourse();
        if (course.getNumberOfWaypoints() < 2) {
            result = null;
        } else if (w == course.getFirstWaypoint()) {
            result = race.getTrackedLegStartingAt(w).getGreatCircleDistance(t, markPositionCache);
        } else if (w == course.getLastWaypoint()) {
            result = race.getTrackedLegFinishingAt(w).getGreatCircleDistance(t, markPositionCache);
        } else {
            Distance before = race.getTrackedLegStartingAt(w).getGreatCircleDistance(t, markPositionCache);
            Distance after = race.getTrackedLegFinishingAt(w).getGreatCircleDistance(t, markPositionCache);
            if (after != null && before != null) {
                result = new MeterDistance(before.add(after).getMeters() / 2);
            } else {
                result = null;
            }
        }
        return result;
    }

    /**
     * Calculates the distance from p to each mark of w at the timepoint t.
     * 
     * @param markPositionCache
     *            a mark position cache for time point {@code t} for this finder's {@link #race}
     */
    private List<Distance> calculateDistance(Position p, Waypoint w, TimePoint t, MarkPositionAtTimePointCache markPositionCache) {
        assert t.equals(markPositionCache.getTimePoint());
        assert race == markPositionCache.getTrackedRace();
        final List<Distance> distances = new ArrayList<>();
        PassingInstruction instruction = getPassingInstructions(w);
        boolean singleMark = false;
        switch (instruction) {
        case Port:
        case Single_Unknown:
        case Starboard:
        case FixedBearing:
            singleMark = true;
            break;
        case Gate:
            Util.Pair<Mark, Mark> posGate = getPortAndStarboardMarks(t, w, markPositionCache);
            if (posGate.getA() != null) {
                Position portGatePosition = markPositionCache.getEstimatedPosition(posGate.getA());
                distances.add(portGatePosition != null ? p.getDistance(portGatePosition) : null);
            } else {
                distances.add(null);
            }
            if (posGate.getB() != null) {
                Position starboardGatePosition = markPositionCache.getEstimatedPosition(posGate.getB());
                distances.add(starboardGatePosition != null ? p.getDistance(starboardGatePosition) : null);
            } else {
                distances.add(null);
            }
            break;
        case Line:
            Util.Pair<Mark, Mark> posLine = getPortAndStarboardMarks(t, w, markPositionCache);
            if (posLine.getA() != null && posLine.getB() != null) {
                Position portLinePosition = markPositionCache.getEstimatedPosition(posLine.getA());
                Position starboardLinePosition = markPositionCache.getEstimatedPosition(posLine.getB());
                distances.add((portLinePosition != null && starboardLinePosition != null) ? p.getDistanceToLine(
                        portLinePosition, starboardLinePosition).abs() : null);
            }
            break;
        case Offset:
            singleMark = true;
            // TODO Actually an Offset mark has two marks, only the first of which actually counts as being rounded. The
            // passing of the second mark is also of interest though.
            break;
        case None:
            break;
        default:
            break;
        }
        if (singleMark) {
            Position markPosition = markPositionCache.getEstimatedPosition(w.getMarks().iterator().next());
            distances.add(markPosition != null ? p.getDistance(markPosition) : null);
        }
        return distances;
    }

    private PassingInstruction getPassingInstructions(Waypoint w) {
        final PassingInstruction result;
        if (passingInstructions.containsKey(w)) {
            result = passingInstructions.get(w);
        } else {
            result = determinePassingInstructions(w);
            passingInstructions.put(w, result);
        }
        return result;
    }

    /**
     * @return all possible ways to pass a waypoint, described as a position and a bearing. The line out of those two
     *         must be crossed.
     */
    private Iterable<Util.Pair<Position, Bearing>> getCrossingInformation(Waypoint w, TimePoint t, MarkPositionAtTimePointCache markPositionCache) {
        assert t.equals(markPositionCache.getTimePoint());
        assert race == markPositionCache.getTrackedRace();
        List<Util.Pair<Position, Bearing>> result = new ArrayList<>();
        PassingInstruction instruction = getPassingInstructions(w);
        if (instruction == PassingInstruction.Line) {
            Util.Pair<Mark, Mark> marks = getPortAndStarboardMarks(t, w, markPositionCache);
            Bearing b = null;
            Mark portMark = marks.getA();
            Mark starBoardMark = marks.getB();
            if (portMark != null && starBoardMark != null) {
                Position portPosition = markPositionCache.getEstimatedPosition(portMark);
                Position starboardPosition = markPositionCache.getEstimatedPosition(starBoardMark);
                if (portPosition != null && starboardPosition != null) {
                    b = portPosition.getBearingGreatCircle(starboardPosition);
                    result.add(new Util.Pair<Position, Bearing>(portPosition, b));
                }
            }
        } else if (instruction == PassingInstruction.Gate) {
            Position before = markPositionCache.getApproximatePosition(race.getTrackedLegFinishingAt(w).getLeg().getFrom());
            Position after = markPositionCache.getApproximatePosition(race.getTrackedLegStartingAt(w).getLeg().getTo());
            Util.Pair<Mark, Mark> pos = getPortAndStarboardMarks(t, w, markPositionCache);
            Mark portMark = pos.getA();
            if (portMark != null) {
                Position portPosition = markPositionCache.getEstimatedPosition(portMark);
                Bearing crossingPort = before.getBearingGreatCircle(portPosition).middle(
                        after.getBearingGreatCircle(portPosition));
                result.add(new Util.Pair<Position, Bearing>(portPosition, crossingPort));
            }
            Mark starboardMark = pos.getB();
            if (starboardMark != null) {
                Position starboardPosition = markPositionCache.getEstimatedPosition(starboardMark);
                Bearing crossingStarboard = before.getBearingGreatCircle(starboardPosition).middle(
                        after.getBearingGreatCircle(starboardPosition));
                result.add(new Util.Pair<Position, Bearing>(starboardPosition, crossingStarboard));
            }
        } else { // single mark
            Bearing b = null;
            Position p = markPositionCache.getEstimatedPosition(w.getMarks().iterator().next());
            if (instruction == PassingInstruction.FixedBearing) {
                b = w.getFixedBearing();
            } else {
                // If the first or last waypoint is a single mark, check the passing instructions and create a "beam"
                // emerging orthogonal to the adjacent leg's direction from the mark to the side indicated by the
                // passing instructions. If no passing instructions are given, construct two lines, one to each direction
                // orthogonally to the adjacent leg:
                if (w == race.getRace().getCourse().getFirstWaypoint()) {
                    if (instruction == PassingInstruction.None || instruction == PassingInstruction.Single_Unknown) {
                        b = markPositionCache.getLegBearing(race.getTrackedLegStartingAt(w)).add(new DegreeBearingImpl(90));
                        result.add(new Pair<>(p, b));
                        b = markPositionCache.getLegBearing(race.getTrackedLegStartingAt(w)).add(new DegreeBearingImpl(270));
                    } else {
                        b = markPositionCache.getLegBearing(race.getTrackedLegStartingAt(w)).add(new DegreeBearingImpl(instruction == PassingInstruction.Port ? 90 : 270));
                    }
                } else if (w == race.getRace().getCourse().getLastWaypoint()) {
                    if (instruction == PassingInstruction.None || instruction == PassingInstruction.Single_Unknown) {
                        b = markPositionCache.getLegBearing(race.getTrackedLegFinishingAt(w)).add(new DegreeBearingImpl(90));
                        result.add(new Pair<>(p, b));
                        b = markPositionCache.getLegBearing(race.getTrackedLegFinishingAt(w)).add(new DegreeBearingImpl(270));
                    } else {
                        b = markPositionCache.getLegBearing(race.getTrackedLegFinishingAt(w)).add(new DegreeBearingImpl(instruction == PassingInstruction.Port ? 90 : 270));
                    }
                } else {
                    Bearing before = markPositionCache.getLegBearing(race.getTrackedLegFinishingAt(w));
                    Bearing after = markPositionCache.getLegBearing(race.getTrackedLegStartingAt(w));
                    if (before != null && after != null) {
                        b = before.middle(after.reverse());
                    }
                }
            }
            result.add(new Pair<>(p, b));
        }
        return result;
    }

    /**
     * @param markPositionCache
     *            a mark position cache for this finder's {@link #race} for time point {@code t}
     * @return the marks of a waypoint with two marks in the order port, starboard (when approaching the waypoint from
     *         the direction of the waypoint beforehand, or {@code (null, null)} in case the direction cannot be determined,
     *         e.g., because the waypoint with passing instructions "Line" is the only waypoint in the course.
     */
    private Util.Pair<Mark, Mark> getPortAndStarboardMarks(TimePoint t, Waypoint w, MarkPositionAtTimePointCache markPositionCache) {
        assert t.equals(markPositionCache.getTimePoint());
        assert race == markPositionCache.getTrackedRace();
        List<Position> markPositions = new ArrayList<Position>();
        for (Mark mark : w.getMarks()) {
            final Position estimatedMarkPosition = markPositionCache.getEstimatedPosition(mark);
            if (estimatedMarkPosition == null) {
                return new Util.Pair<Mark, Mark>(null, null);
            }
            markPositions.add(estimatedMarkPosition);
        }
        if (markPositions.size() != 2) {
            return new Util.Pair<Mark, Mark>(null, null);
        }
        final List<Leg> legs = race.getRace().getCourse().getLegs();
        final int indexOfWaypoint = race.getRace().getCourse().getIndexOfWaypoint(w);
        if (indexOfWaypoint < 0 || legs.isEmpty()) {
            return new Util.Pair<Mark, Mark>(null, null);
        }
        final boolean isStartLine = indexOfWaypoint == 0;
        final Bearing legDeterminingDirectionBearing = markPositionCache.getLegBearing(race.getTrackedLeg(
                legs.get(isStartLine ? 0 : indexOfWaypoint - 1)));
        if (legDeterminingDirectionBearing == null) {
            return new Util.Pair<Mark, Mark>(null, null);
        }
        Distance crossTrackErrorOfMark0OnLineFromMark1ToNextWaypoint = markPositions.get(0).crossTrackError(
                markPositions.get(1), legDeterminingDirectionBearing);
        final Mark starboardMarkWhileApproachingLine;
        final Mark portMarkWhileApproachingLine;
        if (crossTrackErrorOfMark0OnLineFromMark1ToNextWaypoint.getMeters() < 0) {
            portMarkWhileApproachingLine = Util.get(w.getMarks(), 0);
            starboardMarkWhileApproachingLine = Util.get(w.getMarks(), 1);
        } else {
            portMarkWhileApproachingLine = Util.get(w.getMarks(), 1);
            starboardMarkWhileApproachingLine = Util.get(w.getMarks(), 0);
        }
        return new Util.Pair<Mark, Mark>(portMarkWhileApproachingLine, starboardMarkWhileApproachingLine);
    }

    /**
     * If the {@link #race}'s regatta is configured to infer the start times from start mark passings then {@code null}
     * must be tolerated as a value for {@code from}, leading to an open interval starting at the
     * {@link TrackedRace#getStartOfTracking()} or, if not set, the {@link TimePoint#BeginningOfTime beginning of time}.
     * However, if the start time is expected to be set and not inferred, mark passings need to be detected only from
     * the start minus some tolerance interval. In this case, an interval that has {@code null} as its {@code from} time
     * point and thus is considered empty will be returned. It hence returns {@code null} from its
     * {@link TimeRangeWithNullStartMeaningEmpty#getTimeRangeOrNull()} method.
     */
    private TimeRangeWithNullStartMeaningEmpty getTimeRangeOrNull(TimePoint from, TimePoint to) {
        final TimePoint effectiveFrom = getEffectiveFrom(from);
        return new TimeRangeWithNullStartMeaningEmpty(effectiveFrom, to);
    }

    /**
     * If the {@link #race}'s regatta is configured to infer the start times from start mark passings then {@code null}
     * must be tolerated as a value for {@code from}, leading to an open interval starting at the
     * {@link TrackedRace#getStartOfTracking()} or, if not set, the {@link TimePoint#BeginningOfTime beginning of time}.
     * However, if the start time is expected to be set and not inferred, mark passings need to be detected only from
     * the start minus some tolerance interval. In this case, for an interval that has {@code null} as its {@code from} time
     * point and thus is considered empty, {@code null} will be returned.
     */
    private TimePoint getEffectiveFrom(TimePoint from) {
        final TimePoint effectiveFrom;
        if (from == null && race.getTrackedRegatta().getRegatta().useStartTimeInference()) {
            // need to check the whole track to be able to find start mark passings
            // Try to use current startOfTracking to acknowledge that it may have been moved after
            // earlier fixes had been recorded already; if not available, use BeginningOfTime
            effectiveFrom = race.getStartOfTracking() == null ? TimePoint.BeginningOfTime : race.getStartOfTracking();
        } else {
            effectiveFrom = from;
        }
        return effectiveFrom;
    }
    
    @Override
    public Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> getCandidateDeltasAfterRaceStartTimeChange() {
        final TimePoint newNonInferredStartTime = race.getStartOfRace(/* inferred */ false);
        final TimePoint newTimePointWhenToStartConsideringCandidates = getTimePointWhenToStartConsideringCandidates(newNonInferredStartTime);
        final TimePoint newEffectiveTimePointWhenToStartConsideringCandidates = getEffectiveFrom(newTimePointWhenToStartConsideringCandidates);
        final TimeRangeWithNullStartMeaningEmpty newTimeRange = timeRangeForValidCandidates.getWithNewFrom(newEffectiveTimePointWhenToStartConsideringCandidates);
        return getCandidateDeltasAfterTimingChange(newEffectiveTimePointWhenToStartConsideringCandidates, newTimeRange);
    }

    private Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> getCandidateDeltasAfterTimingChange(
            final TimePoint newTimePointWhenToStartConsideringCandidates,
            final TimeRangeWithNullStartMeaningEmpty newTimeRange) {
        final Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> result;
        if (!Util.equalsWithNull(newTimeRange, timeRangeForValidCandidates)) {
            if (newTimeRange.getTimeRangeOrNull() == null) {
                result = clearAllCandidates();
            } else if (timeRangeForValidCandidates.getTimeRangeOrNull() == null) {
                // so far no valid time range; now we have a valid one; use candidate
                // from new start or range to new end of range
                result = updateCandiatesAfterRaceTimeRangeChanged(
                        newTimeRange.getTimeRangeOrNull().from(), newTimeRange.getTimeRangeOrNull().to());
            } else {
                final TimePoint oldTimePointWhenToStartConsideringCandidates = timeRangeForValidCandidates.getTimeRangeOrNull().from();
                result = updateCandiatesAfterRaceTimeRangeChanged(newTimePointWhenToStartConsideringCandidates, oldTimePointWhenToStartConsideringCandidates);
            }
            timeRangeForValidCandidates = newTimeRange;
        } else {
            result = Collections.emptyMap();
        }
        return result;
    }

    @Override
    public Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> getCandidateDeltasAfterStartOfTrackingChange() {
        final TimePoint newNonInferredStartTime = race.getStartOfRace(/* inferred */ false);
        final TimePoint newTimePointWhenToStartConsideringCandidates = getTimePointWhenToStartConsideringCandidates(newNonInferredStartTime);
        final TimePoint newEffectiveTimePointWhenToStartConsideringCandidates = getEffectiveFrom(newTimePointWhenToStartConsideringCandidates);
        final TimeRangeWithNullStartMeaningEmpty newTimeRange = timeRangeForValidCandidates.getWithNewFrom(newEffectiveTimePointWhenToStartConsideringCandidates);
        return getCandidateDeltasAfterTimingChange(newEffectiveTimePointWhenToStartConsideringCandidates, newTimeRange);
    }

    @Override
    public Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> getCandidateDeltasAfterRaceFinishedTimeChange(
            TimePoint oldFinishedTime, TimePoint newFinishedTime) {
        final Map<Competitor, Pair<Iterable<Candidate>, Iterable<Candidate>>> result;
        final TimePoint newTimePointWhenToFinishConsideringCandidates = getTimePointWhenToFinishConsideringCandidates(newFinishedTime);
        final TimeRangeWithNullStartMeaningEmpty newTimeRange = timeRangeForValidCandidates.getWithNewTo(newTimePointWhenToFinishConsideringCandidates);
        if (!Util.equalsWithNull(newTimeRange, timeRangeForValidCandidates)) {
            if (newTimeRange.getTimeRangeOrNull() == null) {
                result = clearAllCandidates();
            } else if (timeRangeForValidCandidates.getTimeRangeOrNull() == null) {
                // so far no valid time range; now we have a valid one; use candidate
                // from new start or range to new end of range
                result = updateCandiatesAfterRaceTimeRangeChanged(
                        newTimeRange.getTimeRangeOrNull().from(),
                        newTimeRange.getTimeRangeOrNull().to());
            } else {
                final TimePoint oldTimePointWhenToFinishConsideringCandidates = timeRangeForValidCandidates.getTimeRangeOrNull().to();
                result = updateCandiatesAfterRaceTimeRangeChanged(oldTimePointWhenToFinishConsideringCandidates, newTimePointWhenToFinishConsideringCandidates);
            }
            timeRangeForValidCandidates = newTimeRange;
        } else {
            result = Collections.emptyMap();
        }
        return result;
    }

}
