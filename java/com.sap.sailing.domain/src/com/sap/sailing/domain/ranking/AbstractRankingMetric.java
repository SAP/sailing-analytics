package com.sap.sailing.domain.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tracking.impl.AbstractRaceRankComparator;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MeterDistance;

public abstract class AbstractRankingMetric implements RankingMetric {
    private static final long serialVersionUID = -3671039530564696392L;
    private final TrackedRace trackedRace;
    
    public class CompetitorRankingInfoImpl implements RankingMetric.CompetitorRankingInfo {
        private static final long serialVersionUID = 2699792976562460961L;

        /**
         * For which time point in the race was this ranking information computed?
         */
        private final TimePoint timePoint;
        
        /**
         * Whose ranking does this object describe?
         */
        private final Competitor competitor;
        
        /**
         * How far did {@link #competitor} actually sail windward / along track from the start of the race until
         * {@link #timePoint}? <code>null</code> before the race start; {@link Distance#NULL} after the race start
         * until {@link #competitor} has actually started.
         */
        private final Distance windwardDistanceSailed;
        
        private final Duration durationSinceStartOfRaceUntilTimePoint;
        
        /**
         * Usually the difference between {@link #timePoint} and the start of the race
         * 
         * FIXME bug5316: there is an important difference to be made here in case {@link #timePoint} is after the {@link #competitor} has finished the race;
         * then, timeElapsed should probably stop counting when the race is finished, but the difference betten {@link #timePoint} and the start of the race keeps counting...
         */
        private final Duration timeElapsed;
        
        /**
         * The corrected time for the {@link #competitor}, assuming the race ended at {@link #timePoint}. This
         * is applying the handicaps proportionately to the time and distance the competitor sailed so far.
         * 
         * FIXME bug5316: we probably want this to reflect the {@link #timeElapsed}, so stop counting when the {@link #competitor} finished the race
         */
        private final Duration correctedTime;
        
        /**
         * Based on the {@link #competitor}'s average VMG in the current leg and the windward position
         * of the competitor that is farthest ahead in the race, how long would it take {@link #competitor}
         * to reach the competitor farthest ahead if that competitor stopped at {@link #timePoint}? Can be
         * a negative number if {@link #competitor} finished the race before {@link #timePoint}.
         */
        private final Duration estimatedActualDurationToCompetitorFarthestAhead;
        
        /**
         * The corrections applied to the time and distance sailed when the {@link #competitor} would have reached the
         * competitor farthest ahead (which would be the case {@link #estimatedActualDurationToCompetitorFarthestAhead} after
         * {@link #timePoint}).
         */
        private final Duration correctedTimeAtEstimatedArrivalAtCompetitorFarthestAhead;

        public CompetitorRankingInfoImpl(TimePoint timePoint, Competitor competitor, Distance windwardDistanceSailed,
                Duration durationSinceStartOfRaceUntilTimePoint, Duration timeElapsed, Duration correctedTime,
                Duration estimatedActualDurationToCompetitorFarthestAhead, Duration correctedTimeAtEstimatedArrivalAtCompetitorFarthestAhead) {
            super();
            this.timePoint = timePoint;
            this.competitor = competitor;
            this.windwardDistanceSailed = windwardDistanceSailed;
            this.durationSinceStartOfRaceUntilTimePoint = durationSinceStartOfRaceUntilTimePoint;
            this.timeElapsed = timeElapsed;
            this.correctedTime = correctedTime;
            this.estimatedActualDurationToCompetitorFarthestAhead = estimatedActualDurationToCompetitorFarthestAhead;
            this.correctedTimeAtEstimatedArrivalAtCompetitorFarthestAhead = correctedTimeAtEstimatedArrivalAtCompetitorFarthestAhead;
        }

        @Override
        public TimePoint getTimePoint() {
            return timePoint;
        }

        @Override
        public Competitor getCompetitor() {
            return competitor;
        }

        @Override
        public Distance getWindwardDistanceSailed() {
            return windwardDistanceSailed;
        }

        @Override
        public Duration getDurationSinceStartOfRaceUntilTimePoint() {
            return durationSinceStartOfRaceUntilTimePoint;
        }

        @Override
        public Duration getTimeElapsed() {
            return timeElapsed;
        }

        @Override
        public Duration getCorrectedTime() {
            return correctedTime;
        }

        @Override
        public Duration getEstimatedActualDurationFromTimePointToCompetitorFarthestAhead() {
            return estimatedActualDurationToCompetitorFarthestAhead;
        }
        
        @Override
        public Duration getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead() {
            return correctedTimeAtEstimatedArrivalAtCompetitorFarthestAhead;
        }
    }

    /**
     * Helper instance used to encode <code>null</code> values in {@link ConcurrentHashMap} instances which do not accept
     * <code>null</code> as key nor value.
     */
    private final static Competitor NULL_COMPETITOR = new CompetitorImpl(null, null, null, null, null, null, null, null, null, null);

    public abstract class AbstractRankingInfo implements RankingInfo {
        private static final long serialVersionUID = -1714363056412906424L;

        /**
         * The time point for which this ranking information is valid
         */
        private final TimePoint timePoint;
        
        /**
         * Caches, on demand, the results of calls to {@link #getCompetitorFarthestAheadInLeg(Leg, TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache)}.
         * <code>null</code> values are encoded as the {@link #NULL_COMPETITOR} and must be translated back to <code>null</code> before returning
         * to a caller outside of this class.
         */
        private final ConcurrentMap<Leg, Competitor> competitorFarthestAheadInLeg;
        
        public AbstractRankingInfo(final TimePoint timePoint) {
            this.timePoint = timePoint;
            this.competitorFarthestAheadInLeg = new ConcurrentHashMap<>();
        }

        @Override
        public TimePoint getTimePoint() {
            return timePoint;
        }
        
        @Override
        public Competitor getCompetitorFarthestAheadInLeg(Leg leg, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
            Competitor result = competitorFarthestAheadInLeg.get(leg);
            if (result == NULL_COMPETITOR) {
                result = null;
            } else if (result == null) {
                result = AbstractRankingMetric.this.getCompetitorFarthestAheadInLeg(getTrackedRace().getTrackedLeg(leg), timePoint, cache);
                competitorFarthestAheadInLeg.put(leg, result == null ? NULL_COMPETITOR : result);
            }
            return result;
        }
    }
    
    public abstract class AbstractRankingInfoWithCompetitorRankingInfoCache extends AbstractRankingInfo implements RankingInfoWithLegLeader {
        private static final long serialVersionUID = 4476276127292347825L;

        /**
         * The basic information for each competitor, telling about actual and corrected times as well as information
         * about actual and corrected times needed to reach the position of the competitor farthest ahead at
         * {@link #timePoint}.
         */
        private final Function<Competitor, CompetitorRankingInfo> competitorRankingInfo;

        /**
         * The competitor with the least corrected time for her arrival at {@link #competitorFarthestAhead}'s windward
         * position at {@link #timePoint}.
         */
        private final Competitor leaderByCorrectedEstimatedTimeToCompetitorFarthestAhead;
        
        private final Competitor competitorFarthestAhead;
        
        public AbstractRankingInfoWithCompetitorRankingInfoCache(TimePoint timePoint, Map<Competitor, CompetitorRankingInfo> competitorRankingInfo, Competitor competitorFarthestAhead) {
            super(timePoint);
            this.competitorFarthestAhead = competitorFarthestAhead;
            this.competitorRankingInfo = c->competitorRankingInfo.get(c); 
            final Comparator<Duration> durationComparatorNullsLast = Comparator.nullsLast(Comparator.naturalOrder());
            leaderByCorrectedEstimatedTimeToCompetitorFarthestAhead = competitorRankingInfo.keySet().stream().sorted(
                    (c1, c2) -> durationComparatorNullsLast.compare(
                            competitorRankingInfo.get(c1).getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead(),
                            competitorRankingInfo.get(c2).getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead())).
                      findFirst().orElse(null);
        }

        @Override
        public Function<Competitor, CompetitorRankingInfo> getCompetitorRankingInfo() {
            return competitorRankingInfo;
        }

        @Override
        public Competitor getLeaderByCorrectedEstimatedTimeToCompetitorFarthestAhead() {
            return leaderByCorrectedEstimatedTimeToCompetitorFarthestAhead;
        }
        
        @Override
        public Competitor getCompetitorFarthestAhead() {
            return competitorFarthestAhead;
        }

        @Override
        public Competitor getLeaderInLegByCalculatedTime(Leg leg, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
            final TrackedLeg trackedLeg = getTrackedRace().getTrackedLeg(leg);
            return trackedLeg.getLeader(getTimePoint(), cache);
        }
    }
    
    protected AbstractRankingMetric(TrackedRace trackedRace) {
        super();
        this.trackedRace = trackedRace;
    }

    @Override
    public TrackedRace getTrackedRace() {
        return trackedRace;
    }

    /**
     * Uses a sequential spliterator across the competitors.
     */
    protected Competitor getCompetitorFarthestAhead(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        Comparator<Competitor> oneDesignComparator = getWindwardDistanceTraveledComparator(timePoint, cache);
        Optional<Competitor> competitorFarthestAhead = StreamSupport
                .stream(getCompetitors().spliterator(),
                        /* parallel */ /* bug 5720/5756: deadlock to be avoided by not using ForkJoinPool */ false)
                .sorted(oneDesignComparator).findFirst();
        return competitorFarthestAhead.orElse(null);
    }
    
    /**
     * The time from the {@link TrackedRace#getStartOfRace() race start} until <code>timePoint</code> or until
     * the point in time when <code>competitor</code> passed the finish mark, whichever comes first. If there is
     * no mark passing for {@code competitor} for the last waypoint or no {@link TrackedRace#getStartOfRace()} is
     * known, {@code null} is returned.
     */
    @Override
    public Duration getActualTimeSinceStartOfRace(Competitor competitor, TimePoint timePoint) {
        final Duration result;
        final TimePoint startOfRace = getTrackedRace().getStartOfRace();
        if (startOfRace == null || timePoint.before(startOfRace)) {
            result = null;
        } else {
            final Waypoint finish = getTrackedRace().getRace().getCourse().getLastWaypoint();
            if (finish == null) {
                result = null;
            } else {
                final MarkPassing finishingMarkPassing = getTrackedRace().getMarkPassing(competitor, finish);
                if (finishingMarkPassing != null) {
                    if (finishingMarkPassing.getTimePoint().before(timePoint)) {
                        result = startOfRace.until(finishingMarkPassing.getTimePoint());
                    } else {
                        result = startOfRace.until(timePoint);
                    }
                } else {
                    if (trackedRace.getEndOfTracking() != null && timePoint.after(trackedRace.getEndOfTracking())) {
                        result = null; // race not finished until end of tracking; no reasonable value can be computed for competitor
                    } else {
                        result = startOfRace.until(timePoint);
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Fetches the competitors to consider for this ranking
     */
    protected Iterable<Competitor> getCompetitors() {
        return getTrackedRace().getRace().getCompetitors();
    }
    
    /**
     * Get's <code>who</code>'s current tracked leg at <code>timePoint</code>, or <code>null</code> if <code>who</code> hasn't
     * started at <code>timePoint</code> yet, or <code>who</code>'s tracked leg for the last leg if <code>who</code> has
     * already finished the race at <code>timePoint</code>.
     */
    protected TrackedLegOfCompetitor getCurrentLegOrLastLegIfAlreadyFinished(Competitor who, TimePoint timePoint) {
        TrackedLegOfCompetitor currentLegWho = getTrackedRace().getCurrentLeg(who, timePoint);
        if (currentLegWho == null) { // already finished or not yet started; if already finished, use last leg
            final Waypoint lastWaypoint = getTrackedRace().getRace().getCourse().getLastWaypoint();
            if (lastWaypoint != null && getTrackedRace().getRace().getCourse().getNumberOfWaypoints() > 1) { // could be an empty course
                final TrackedLeg lastTrackedLeg = getTrackedRace().getTrackedLegFinishingAt(lastWaypoint);
                TrackedLegOfCompetitor whosLastTrackedLeg = lastTrackedLeg.getTrackedLeg(who);
                if (isAssumedToHaveFinishedLeg(timePoint, whosLastTrackedLeg)) {
                    currentLegWho = whosLastTrackedLeg;
                }
            }
        }
        return currentLegWho;
    }

    /**
     * For the situation at <code>timePoint</code>, determines how long in real, uncorrected time <code>who</code> lags
     * behind <code>to</code> in the leg identified by <code>legWho</code>. If both are still sailing in the leg at
     * <code>timePoint</code>, this is the time <code>who</code> needs with constant average VMG to reach
     * <code>to</code>'s "windward" (or along course for reaching legs) position at <code>timePoint</code>. If only
     * <code>to</code> has already finished the leg at {@code timePoint} then <code>who</code>'s projected duration to the end
     * of the leg using her average VMG on the leg is used. Note that in this latter case it doesn't matter whether
     * {@code who} already has a mark passing for the end of the leg or not; the idea is to not "rewrite history" by
     * letting the mark passing have an impact on the rankings prior to the mark passing.
     * <p>
     * 
     * The result may be a negative duration in case <code>who</code> reached the position in question before
     * <code>timePoint</code>. If <code>who</code> sailed past the waypoint without receiving a mark passing, we assume
     * <code>who</code> has to spend as much time to get back to the waypoint and will return a positive duration.
     * <p>
     * 
     * Precondition: <code>who</code> and <code>to</code> have both started sailing the leg at <code>timePoint</code>
     * and <code>to</code> has sailed a greater or equal windward distance compared to <code>who</code>. If not, the
     * result is undefined.
     */
    protected Duration getPredictedDurationToEndOfLegOrTo(TimePoint timePoint, final TrackedLegOfCompetitor legWho, final TrackedLegOfCompetitor legTo,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        assert isAssumedToHaveStartedLeg(timePoint, legWho) || isAssumedToHaveFinishedLeg(timePoint, legWho);
        assert isAssumedToHaveStartedLeg(timePoint, legTo) || isAssumedToHaveFinishedLeg(timePoint, legTo);
        final Duration toEndOfLegOrTo;
        if (isAssumedToHaveFinishedLeg(timePoint, legTo)) {
            // calculate actual time it takes who to reach the end of the leg starting at timePoint:
            final TimePoint whosLegFinishTime = legWho.getFinishTime();
            if (whosLegFinishTime != null && !whosLegFinishTime.after(timePoint)) {
                // who's leg finishing time is known and is already reached at timePoint; we don't need to extrapolate;
                // "who" needs no more time at timePoint to reach the end of the leg
                toEndOfLegOrTo = timePoint.until(whosLegFinishTime);
            } else {
                // estimate who's leg finishing time by extrapolating with the average VMG (if available) or the current VMG
                // (if no average VMG can currently be computed, e.g., because the time point is exactly at the leg start)
                final Position positionOfEndOfLeg = getTrackedRace().getApproximatePosition(legWho.getLeg().getTo(), timePoint);
                // Turn into a positive duration because a negative duration can only occur if "who" sailed past the waypoint without
                // receiving a mark passing and hence now has to sail back, taking a positive duration to get there.
                final Duration durationToReach = getDurationToReach(positionOfEndOfLeg, timePoint, legWho, cache);
                toEndOfLegOrTo = durationToReach==null?null:durationToReach.abs();
            }
        } else {
            // competitor "to" is still in same leg; project "who" to "to"'s position using VMG
            final Position positionOfTo = getTrackedRace().getTrack(legTo.getCompetitor()).getEstimatedPosition(timePoint, /* extrapolate */ true);
            toEndOfLegOrTo = positionOfTo == null ? null : getDurationToReach(positionOfTo, timePoint, legWho, cache);
        }
        return toEndOfLegOrTo;
    }

    private Duration getDurationToReach(final Position windwardPositionToReachInWhosCurrentLeg, TimePoint timePoint,
            final TrackedLegOfCompetitor whosLeg, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Duration toEndOfLegOrTo;
        final Speed averageVMG = whosLeg.getAverageVelocityMadeGood(timePoint, cache);
        final Speed vmg = averageVMG == null || Double.isNaN(averageVMG.getKnots()) ?
                /* default to current VMG */ whosLeg.getVelocityMadeGood(timePoint, WindPositionMode.EXACT, cache) : averageVMG;
        toEndOfLegOrTo = vmg == null || vmg.getKnots() == 0.0 ? null : vmg.getDuration(
                whosLeg.getTrackedLeg().getWindwardDistance(
                        getTrackedRace().getTrack(whosLeg.getCompetitor()).getEstimatedPosition(timePoint, /* extrapolate */true),
                        windwardPositionToReachInWhosCurrentLeg, timePoint, WindPositionMode.LEG_MIDDLE));
        return toEndOfLegOrTo;
    }
    
    protected void validateGetDurationToReachAtEqualPerformanceParameters(Competitor to, Waypoint fromWaypoint,
            TimePoint timePointOfTosPosition, final MarkPassing whenToPassedFromWaypoint) {
        if (whenToPassedFromWaypoint == null) {
            throw new IllegalArgumentException("Competitor "+to+" is expected to have passed "+fromWaypoint+" but hasn't");
        }
        if (whenToPassedFromWaypoint.getTimePoint().after(timePointOfTosPosition)) {
            throw new IllegalArgumentException("Competitor "+to+" was expected to have passed "+fromWaypoint+" before "+timePointOfTosPosition+
                    " but did pass it at "+whenToPassedFromWaypoint.getTimePoint());
        }
    }

    /**
     * How far did <code>competitor</code> sail windwards/along-course since the start of the race?
     * For each leg the total windward distance sailed is limited to the leg's windward distance at its
     * {@link TrackedLeg#getReferenceTimePoint() reference time point}. This ensures that significantly "overstaying" the lay lines
     * doesn't let a competitor rank better than one who already passed the mark but traveled little windward distance in
     * the next leg.
     * 
     * @param timePoint needed to determine <code>competitor</code>'s position at that time point; note that the
     * time point for wind approximation is taken to be a reference time point selected based on the mark passings
     * for the respective leg's from/to waypoints.
     */
    protected Distance getWindwardDistanceTraveled(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getWindwardDistanceTraveled(competitor, getTrackedRace().getRace().getCourse().getFirstWaypoint(), timePoint, cache);
    }

    /**
     * Constructs a comparator based on the results of
     * {@link #getWindwardDistanceTraveled(Competitor, TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache)} where competitors are
     * "less" than other competitors ("better") if they are in a later leg or, if in the same leg, have a greater
     * windward distance traveled. If both competitors have already finished the race, the finishing time is compared.
     */
    private Comparator<Competitor> getWindwardDistanceTraveledComparator(final TimePoint timePoint, final WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Map<Competitor, Distance> windwardDistanceTraveledPerCompetitor = new HashMap<>();
        for (final Competitor competitor : getCompetitors()) {
            windwardDistanceTraveledPerCompetitor.put(competitor, getWindwardDistanceTraveled(competitor, timePoint, cache));
        }
        return new AbstractRaceRankComparator<Distance>(getTrackedRace(), timePoint, /* lessIsBetter */ false) {
            @Override
            protected Distance getComparisonValueForSameLeg(Competitor competitor) {
                return windwardDistanceTraveledPerCompetitor.get(competitor);
            }
        };
    }

    /**
     * How far did <code>competitor</code> sail windwards/along-course since passing the <code>from</code> waypoint?
     * For each leg the total windward distance sailed is limited to the leg's windward distance at its
     * {@link TrackedLeg#getReferenceTimePoint() reference time point}. This ensures that significantly "overstaying" the lay lines
     * doesn't let a competitor rank better than one who already passed the mark but traveled little windward distance in
     * the next leg.<p>
     * 
     * If mark positions along the way are not known, the windward distance of those legs will be counted as 0.
     * 
     * @param timePoint needed to determine <code>competitor</code>'s position at that time point; note that the
     * time point for wind approximation is taken to be a reference time point selected based on the mark passings
     * for the respective leg's from/to waypoints.
     */
    protected Distance getWindwardDistanceTraveled(Competitor competitor, Waypoint from, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Distance result;
        if (from == null) {
            result = null;
        } else {
            Distance d = Distance.NULL;
            boolean count = false; // start counting only once the "from" waypoint has been found
            final Course course = getTrackedRace().getRace().getCourse();
            course.lockForRead();
            try {
                for (final TrackedLeg trackedLeg : getTrackedRace().getTrackedLegs()) {
                    count = count || trackedLeg.getLeg().getFrom() == from;
                    if (count) {
                        final LegType legTypeForRanking = getLegTypeForRanking(trackedLeg);
                        final TrackedLegOfCompetitor trackedLegOfCompetitor = trackedLeg.getTrackedLeg(competitor);
                        if (isAssumedToHaveStartedLeg(timePoint, trackedLegOfCompetitor)) {
                            if (!isAssumedToHaveFinishedLeg(timePoint, trackedLegOfCompetitor)) {
                                // partial distance sailed:
                                final Position estimatedPosition = getTrackedRace().getTrack(competitor).getEstimatedPosition(timePoint, /* extrapolate */ true);
                                if (estimatedPosition != null) {
                                    final Distance windwardDistanceFromLegStart = trackedLeg.getWindwardDistanceFromLegStart(legTypeForRanking,
                                            estimatedPosition, cache);
                                    if (windwardDistanceFromLegStart == null) {
                                        // probably the leg start position is not known; therefore, distance cannot be determined; return null:
                                        d = null;
                                        break;
                                    }
                                    final Distance legWindwardDistance = trackedLeg.getWindwardDistance(legTypeForRanking, cache);
                                    if (legWindwardDistance != null && legWindwardDistance.compareTo(windwardDistanceFromLegStart) < 0) {
                                        d = d.add(legWindwardDistance);
                                    } else {
                                        // if the competitor is currently at the mark rounding, the windward distance within the leg may
                                        // be negative; don't reduce the distance in this case
                                        if (windwardDistanceFromLegStart.getMeters() > 0) {
                                            d = d.add(windwardDistanceFromLegStart);
                                        }
                                    }
                                }
                                break;
                            } else {
                                final Distance legWindwardDistance = trackedLeg.getWindwardDistance(legTypeForRanking, cache);
                                if (legWindwardDistance != null) {
                                    d = d.add(legWindwardDistance);
                                }
                            }
                        }
                    }
                }
            } finally {
                course.unlockAfterRead();
            }
            result = d;
        }
        return result;
    }

    /**
     * Defines the leg type to use for ranking competitors in {@code trackedLeg}. This default implementation returns
     * {@code null}, meaning to infer the leg type based on the wind direction on that leg. Specializations may override
     * this, e.g., to use a leg type matching any ranking specifications made with the ranking metric for that leg, such
     * as projecting to the rhumb line instead of to the wind in certain circumstances, even if the leg may be classified
     * as an upwind or downwind leg based on the wind detected on it.
     */
    protected LegType getLegTypeForRanking(TrackedLeg trackedLeg) {
        return null;
    }

    /**
     * The {@link Competitor} of the {@code trackedLegOfCompetitor} is assumed to have started the leg specified by
     * {@code trackedLegOfCompetitor} if the competitor has a mark passing for the leg's end waypoint or any waypoint
     * of the course thereafter that is at or before {@code timePoint}. This assumes the possibility that mark passings
     * may be missing for in-between waypoints. In particular, it is possible that a finish mark passing has been
     * derived from an official finishing time even though mark passings in between or even the entire track are
     * missing, so asking whether a leg has been finished can be answered with {@code true} if the {@code timePoint}
     * is at of after that finish mark passing.
     */
    protected boolean isAssumedToHaveFinishedLeg(TimePoint timePoint, final TrackedLegOfCompetitor trackedLegOfCompetitor) {
        final Waypoint legEndWaypoint = trackedLegOfCompetitor.getLeg().getTo();
        final MarkPassing markPassing = findMarkPassingForWaypointOrSuccessorAtOrBeforeTimePoint(timePoint,
                trackedLegOfCompetitor, legEndWaypoint);
        return markPassing != null;
    }

    /**
     * The {@link Competitor} of the {@code trackedLegOfCompetitor} is assumed to have started the leg specified by
     * {@code trackedLegOfCompetitor} if the competitor has a mark passing for the leg's start waypoint or any waypoint
     * of the course thereafter that is at or before {@code timePoint}. This assumes the possibility that mark passings
     * may be missing for in-between waypoints. In particular, it is possible that a finish mark passing has been
     * derived from an official finishing time even though mark passings in between or even the entire track are
     * missing, so asking whether a leg has been started can be answered with {@code true} if the {@code timePoint}
     * is at of after that finish mark passing.
     */
    protected boolean isAssumedToHaveStartedLeg(TimePoint timePoint, final TrackedLegOfCompetitor trackedLegOfCompetitor) {
        final Waypoint legStartWaypoint = trackedLegOfCompetitor.getLeg().getFrom();
        final MarkPassing markPassing = findMarkPassingForWaypointOrSuccessorAtOrBeforeTimePoint(timePoint,
                trackedLegOfCompetitor, legStartWaypoint);
        return markPassing != null;
    }

    private MarkPassing findMarkPassingForWaypointOrSuccessorAtOrBeforeTimePoint(TimePoint timePoint,
            final TrackedLegOfCompetitor trackedLegOfCompetitor, final Waypoint legStartWaypoint) {
        final Iterable<Waypoint> waypoints = getTrackedRace().getRace().getCourse().getWaypoints();
        boolean checkForMarkPassing = false;
        MarkPassing markPassing = null;
        for (final Waypoint waypoint : waypoints) {
            if (waypoint == legStartWaypoint) {
                checkForMarkPassing = true; // from here on, check for mark passings for the waypoint
            }
            if (checkForMarkPassing) {
                final MarkPassing markPassingCandidate = getTrackedRace().getMarkPassing(trackedLegOfCompetitor.getCompetitor(), waypoint);
                if (markPassingCandidate != null && !markPassingCandidate.getTimePoint().after(timePoint)) {
                    markPassing = markPassingCandidate;
                    break;
                }
            }
        }
        return markPassing;
    }

    /**
     * @return <code>null</code> if no competitor has started the leg yet; the first competitor to finish the leg if any
     *         has already finished the leg at <code>timePoint</code>; or the competitor with the greatest windward
     *         distance traveled in the leg at <code>timePoint</code> otherwise
     */
    protected Competitor getCompetitorFarthestAheadInLeg(TrackedLeg trackedLeg, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        Competitor firstAroundMark = getFirstLegFinisherBefore(trackedLeg, timePoint);
        final Competitor result;
        if (firstAroundMark != null) {
            result = firstAroundMark;
        } else {
            List<MarkPassing> copyOfMarkPassingsForLegStart = new ArrayList<>();
            {   // scope the markPassingsForLegStart, so it is no longer used outside this block
                final Iterable<MarkPassing> markPassingsForLegStart = getTrackedRace().getMarkPassingsInOrder(trackedLeg.getLeg().getFrom());
                // See bug 3728: obtaining lock for mark passings in order for a waypoint before code potentially called from here,
                // e.g., through getWindwardDistanceTraveled(...), tries to obtain the read lock for mark passings for a competitor can
                // result in a deadlock. Therefore, we copy the mark passings for the leg start under the lock, then release it again
                // before calling into a deep stack with getWindwardDistanceTraveled(...) which may well obtain a read lock on
                // the mark passings for a competitor.
                getTrackedRace().lockForRead(markPassingsForLegStart);
                try {
                    Util.addAll(markPassingsForLegStart, copyOfMarkPassingsForLegStart);
                } finally {
                    getTrackedRace().unlockAfterRead(markPassingsForLegStart);
                }
            }
            Distance maxWindwardDistanceTraveled = new MeterDistance(Double.MIN_VALUE);
            Competitor competitorFarthestAlong = null;
            for (MarkPassing mp : copyOfMarkPassingsForLegStart) {
                if (mp.getTimePoint().after(timePoint)) {
                    break;
                }
                final Distance windwardDistanceTraveled = getWindwardDistanceTraveled(mp.getCompetitor(), mp.getWaypoint(), timePoint, cache);
                if (windwardDistanceTraveled != null && windwardDistanceTraveled.compareTo(maxWindwardDistanceTraveled) > 0) {
                    maxWindwardDistanceTraveled = windwardDistanceTraveled;
                    competitorFarthestAlong = mp.getCompetitor();
                }
            }
            result = competitorFarthestAlong;
        }
        return result;
    }

    /**
     * Determines the first competitor finishing the leg identified by <code>trackedLeg</code> at or before <code>timePoint</code>. If
     * no such competitor exists, <code>null</code> is returned.
     */
    private Competitor getFirstLegFinisherBefore(TrackedLeg trackedLeg, TimePoint timePoint) {
        Iterable<MarkPassing> markPassingsForLegEnd = getTrackedRace().getMarkPassingsInOrder(trackedLeg.getLeg().getTo());
        Competitor firstAroundMark = null;
        getTrackedRace().lockForRead(markPassingsForLegEnd);
        try {
            final Iterator<MarkPassing> i = markPassingsForLegEnd.iterator();
            if (i.hasNext()) {
                MarkPassing markPassing = i.next();
                if (!markPassing.getTimePoint().after(timePoint)) {
                    firstAroundMark = markPassing.getCompetitor();
                }
            }
        } finally {
            getTrackedRace().unlockAfterRead(markPassingsForLegEnd);
        }
        return firstAroundMark;
    }
}
