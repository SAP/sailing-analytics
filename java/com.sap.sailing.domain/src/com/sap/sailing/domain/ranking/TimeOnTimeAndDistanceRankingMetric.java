package com.sap.sailing.domain.ranking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Mile;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.shared.util.WeakReferenceWithCleanerCallback;
import com.sap.sse.util.SerializableRunnable;

/**
 * The basic concept of this ranking metric is to compare corrected reciproke VMG/VMC (measured in seconds per nautical
 * mile) which we define here to be the corrected time divided by the windward or along course distance traveled, minus
 * an optional time-on-distance allowance which basically tells the expected VMG/VMC by providing the seconds to the
 * mile expected for that competitor according to its rating.
 * <p>
 * 
 * In these calculations, a leg distance shall always be the same for all competitors, meaning that for upwind/downwind
 * legs an average wind direction needs to be agreed upon.
 * <p>
 * 
 * The corrected time is determined by applying the time-on-time factor to the actual time since the start of race.
 * <p>
 * 
 * The reciproke VMG/VMC for a competitor i is determined as follows:
 * 
 * <pre>vmgc_i := t_i * f_i / d_i - g_i</pre>
 * 
 * with sailed windward/along-course distance <code>d_i</code> since the start, <code>f_i</code> being competitor i's
 * time-on-time factor, and with time-on-distance allowance <code>g_i</code> measured in seconds per nautical mile and
 * <code>t_i</code> being the time since the start of the race.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class TimeOnTimeAndDistanceRankingMetric extends NonPerformanceCurveRankingMetric {
    private static final long serialVersionUID = 2827013130741242548L;

    private final TimeOnTimeFactorMapping timeOnTimeFactor;

    private final TimeOnDistanceAllowancePerNauticalMileMap timeOnDistanceFactorNauticalMile;
    
    private ConcurrentHashMap<Competitor, Double> timeOnTimeFactorCache;

    /**
     * For each value obtained and stored in {@link #timeOnTimeFactorCache}, an update callback is registered
     * that is triggered when the time-on-time factor for that competitor may have changed. It is important to
     * strongly reference these callbacks here to tie their object life cycle to that of this ranking metric.
     * When this ranking metric becomes eligible for garbage collection then so do these callback objects.
     * When used together with a {@link WeakReferenceWithCleanerCallback}, any listener / observer relations
     * can be terminated at that point. 
     */
    private ConcurrentHashMap<Competitor, SerializableRunnable> timeOnTimeFactorCacheUpdateCallbacks;
    
    private ConcurrentHashMap<Competitor, Duration> timeOnDistanceFactorInSecondsPerNauticalMileCache;
    
    /**
     * For each value obtained and stored in {@link #timeOnTimeFactorCache}, an update callback is registered
     * that is triggered when the time-on-time factor for that competitor may have changed. It is important to
     * strongly reference these callbacks here to tie their object life cycle to that of this ranking metric.
     * When this ranking metric becomes eligible for garbage collection then so do these callback objects.
     * When used together with a {@link WeakReferenceWithCleanerCallback}, any listener / observer relations
     * can be terminated at that point. 
     */
    private ConcurrentHashMap<Competitor, SerializableRunnable> timeOnDistanceFactorInSecondsPerNauticalMileCacheUpdateCallbacks;
    
    /**
     * The regular constructor that can also be used as <code>TimeOnTimeAndDistanceRankingMetric::new</code>
     * to obtain a {@link RankingMetricConstructor} implementation. It uses the {@link TrackedRace}'s regatta
     * and its handicap figures to obtain each competitor's handicaps.
     */
    public TimeOnTimeAndDistanceRankingMetric(final TrackedRace trackedRace) {
        this(trackedRace,
                (TimeOnTimeAndDistanceRankingMetric totadrm)->(Competitor c) -> trackedRace.getTrackedRegatta().getRegatta().getTimeOnTimeFactor(c, Optional.of(totadrm.getTimeOnTimeFactorCacheUpdateCallback(c))),
                (TimeOnTimeAndDistanceRankingMetric totadrm)->(Competitor c) -> trackedRace.getTrackedRegatta().getRegatta().getTimeOnDistanceAllowancePerNauticalMile(c, Optional.of(totadrm.getTimeOnDistanceAllowanceCacheUpdateCallback(c))));
    }
    
    /**
     * Mostly to simplify testing; instead of obtaining the handicap numbers through the {@link TrackedRace} and
     * its regatta, handicap mappings can be passed directly and overrule anything defined for the competitor or
     * on the regatta.
     */
    public TimeOnTimeAndDistanceRankingMetric(final TrackedRace trackedRace, TimeOnTimeFactorMapping timeOnTimeFactor,
            TimeOnDistanceAllowancePerNauticalMileMap timeOnDistanceFactorInSecondsPerNauticalMile) {
        this(trackedRace,
                // don't need the "this" pointer here; simply ignoring and passing through the functions provided as parameters
             (TimeOnTimeAndDistanceRankingMetric totadrm)->timeOnTimeFactor,
             (TimeOnTimeAndDistanceRankingMetric totadrm)->timeOnDistanceFactorInSecondsPerNauticalMile);
    }
    
    private TimeOnTimeAndDistanceRankingMetric(final TrackedRace trackedRace,
            Function<TimeOnTimeAndDistanceRankingMetric, TimeOnTimeFactorMapping> timeOnTimeFactorMappingFunction,
            Function<TimeOnTimeAndDistanceRankingMetric, TimeOnDistanceAllowancePerNauticalMileMap> timeOnDistanceFactorInSecondsPerNauticalMileFunction) {
        super(trackedRace);
        this.timeOnTimeFactor = timeOnTimeFactorMappingFunction.apply(this);
        this.timeOnDistanceFactorNauticalMile = timeOnDistanceFactorInSecondsPerNauticalMileFunction.apply(this);
        timeOnTimeFactorCache = new ConcurrentHashMap<>();
        timeOnDistanceFactorInSecondsPerNauticalMileCache = new ConcurrentHashMap<>();
        timeOnTimeFactorCacheUpdateCallbacks = new ConcurrentHashMap<>();
        timeOnDistanceFactorInSecondsPerNauticalMileCacheUpdateCallbacks = new ConcurrentHashMap<>();
    }
    
    /**
     * Re-establishes empty caches for the time-on-time factors (TMF) and the time-on-distance allowances as well as the
     * callbacks connected to those cache entries.
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        timeOnTimeFactorCache = new ConcurrentHashMap<>();
        timeOnDistanceFactorInSecondsPerNauticalMileCache = new ConcurrentHashMap<>();
        timeOnTimeFactorCacheUpdateCallbacks = new ConcurrentHashMap<>();
        timeOnDistanceFactorInSecondsPerNauticalMileCacheUpdateCallbacks = new ConcurrentHashMap<>();
    }
    
    private Runnable getTimeOnTimeFactorCacheUpdateCallback(Competitor competitor) {
        final SerializableRunnable result = ()->timeOnTimeFactorCache.remove(competitor);
        timeOnTimeFactorCacheUpdateCallbacks.put(competitor, result);
        return result;
    }
    
    private Runnable getTimeOnDistanceAllowanceCacheUpdateCallback(Competitor competitor) {
        final SerializableRunnable result = ()->timeOnDistanceFactorInSecondsPerNauticalMileCache.remove(competitor);
        timeOnDistanceFactorInSecondsPerNauticalMileCacheUpdateCallbacks.put(competitor, result);
        return result;
    }
    
    @Override
    public RankingMetrics getType() {
        return RankingMetrics.TIME_ON_TIME_AND_DISTANCE;
    }

    /**
     * Ranks the competitors by their average corrected velocity made good, determined by the following formula:
     * 
     * <pre>
     * t_i * f_i / d_i - g_i
     * </pre>
     * 
     * where <code>t_i</code> is the time sailed by competitor i, <code>f_i</code> is the time-on-time factor for
     * competitor i, <code>d_i</code> is the (windward/along-course) distance traveled by competitor i, <code>g_i</code>
     * is the time-on-distance allowance (provided as time per distance) and <code>d</code> is the total windward /
     * along-course distance of the {@link #getTrackedRace() race's} course.
     */
    @Override
    public Comparator<Competitor> getRaceRankingComparator(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final RankingMetric.RankingInfo rankingInfo = getRankingInfo(timePoint, cache);
        final Comparator<Duration> durationComparatorNullsLast = Comparator.nullsLast(Comparator.naturalOrder());
        return (c1, c2) -> {
            final CompetitorRankingInfo c1CompetitorRankingInfo = rankingInfo.getCompetitorRankingInfo().apply(c1);
            final CompetitorRankingInfo c2CompetitorRankingInfo = rankingInfo.getCompetitorRankingInfo().apply(c2);
            return
                    durationComparatorNullsLast.compare(
                            c1CompetitorRankingInfo == null ? null : c1CompetitorRankingInfo.getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead(),
                            c2CompetitorRankingInfo == null ? null : c2CompetitorRankingInfo.getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead());
        };
    }

    @Override
    public Comparator<TrackedLegOfCompetitor> getLegRankingComparator(TrackedLeg trackedLeg, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        // competitors that have not yet started the leg will get a duration based on Long.MAX_VALUE
        final Map<Competitor, Duration> correctedTimesToReachFastestBoatsPositionAtTimePointOrEndOfLegMeasuredFromStartOfRace = new HashMap<>();
        final Competitor fastestCompetitorInLeg = getCompetitorFarthestAheadInLeg(trackedLeg, timePoint, cache);
        final boolean fastestCompetitorHasStartedLeg;
        if (fastestCompetitorInLeg != null) {
            final TrackedLegOfCompetitor trackedLegOfFastestCompetitorInLeg = trackedLeg.getTrackedLeg(fastestCompetitorInLeg);
            // May be false if there is a gap in the logging. Example: We get a finishing time for a competitor whose tracker has failed. 
            // The competitor now can be the fastest competitor in the finishing leg but there is no start for the given leg. 
            // In this case it is not possible to compare a competitor that has not started the finishing leg against the fastest competitor -> so they are ranked equal.
            fastestCompetitorHasStartedLeg = isAssumedToHaveStartedLeg(timePoint, trackedLegOfFastestCompetitorInLeg);
            final Distance totalWindwardDistanceLegLeaderTraveledUpToTimePointOrLegEnd;
            final TimePoint startOfRace = getTrackedRace().getStartOfRace();
            final Position positionOfFastestBoatInLegAtTimePointOrLegEnd;
            if (trackedLegOfFastestCompetitorInLeg.hasFinishedLeg(timePoint)) {
                // fastest boat has already finished leg at timePoint; sum up windward distances of all legs up to and including trackedLeg
                positionOfFastestBoatInLegAtTimePointOrLegEnd = getTrackedRace().getApproximatePosition(
                        trackedLeg.getLeg().getTo(), timePoint);
                Distance totalWindwardDistanceIncludingCompleteLeg = Distance.NULL;
                final Course course = getTrackedRace().getRace().getCourse();
                course.lockForRead();
                try {
                    for (TrackedLeg tl : getTrackedRace().getTrackedLegs()) {
                        totalWindwardDistanceIncludingCompleteLeg = totalWindwardDistanceIncludingCompleteLeg.add(tl
                                .getWindwardDistance(cache));
                        if (tl == trackedLeg || totalWindwardDistanceIncludingCompleteLeg == null) {
                            break;
                        }
                    }
                } finally {
                    course.unlockAfterRead();
                }
                totalWindwardDistanceLegLeaderTraveledUpToTimePointOrLegEnd = totalWindwardDistanceIncludingCompleteLeg;
            } else {
                // fastest boat still in same leg
                positionOfFastestBoatInLegAtTimePointOrLegEnd = getTrackedRace().getTrack(fastestCompetitorInLeg)
                        .getEstimatedPosition(timePoint, /* extrapolate */true);
                totalWindwardDistanceLegLeaderTraveledUpToTimePointOrLegEnd = getWindwardDistanceTraveled(
                        fastestCompetitorInLeg, timePoint, cache);
            }
            for (Competitor competitor : getCompetitors()) {
                final TrackedLegOfCompetitor competitorLeg = trackedLeg.getTrackedLeg(competitor);
                final Duration correctedTime;
                if (competitorLeg != null && competitorLeg.hasStartedLeg(timePoint)) {
                    final Duration timeToReachFastest = getPredictedDurationToEndOfLegOrTo(timePoint,
                            competitorLeg, trackedLegOfFastestCompetitorInLeg, cache);
                    final Duration totalDurationSinceRaceStart = timeToReachFastest == null ? null : startOfRace == null ? null :
                        startOfRace.until(timePoint).plus(timeToReachFastest);
                    correctedTime = getCalculatedTime(competitor, () -> trackedLeg.getLeg(),
                            () -> positionOfFastestBoatInLegAtTimePointOrLegEnd, totalDurationSinceRaceStart,
                            totalWindwardDistanceLegLeaderTraveledUpToTimePointOrLegEnd);
                } else { // competitor hasn't started the leg yet; they all get MAX_VALUE as the corrected duration,
                         // hence comparing equal to each other
                    // and greater than all competitors who have already started the leg
                    correctedTime = new MillisecondsDurationImpl(Long.MAX_VALUE);
                }
                correctedTimesToReachFastestBoatsPositionAtTimePointOrEndOfLegMeasuredFromStartOfRace.put(competitor, correctedTime);
            }
        } else {
            fastestCompetitorHasStartedLeg = false;
        }
        final Comparator<Duration> durationComparatorNullsLast = Comparator.nullsLast(Comparator.naturalOrder());
        return (tloc1, tloc2) -> fastestCompetitorHasStartedLeg ?
                durationComparatorNullsLast.compare(
                        correctedTimesToReachFastestBoatsPositionAtTimePointOrEndOfLegMeasuredFromStartOfRace.get(tloc1.getCompetitor()),
                        correctedTimesToReachFastestBoatsPositionAtTimePointOrEndOfLegMeasuredFromStartOfRace.get(tloc2.getCompetitor()))
                        : 0;
    }

    @Override
    public Duration getCorrectedTime(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Duration timeActuallySpent = getActualTimeSinceStartOfRace(competitor, timePoint);
        final Distance windwardDistanceSailed = getWindwardDistanceTraveled(competitor, timePoint, cache);
        return getCalculatedTime(competitor, ()->getTrackedRace().getCurrentLeg(competitor, timePoint).getLeg(),
                ()->getTrackedRace().getTrack(competitor).getEstimatedPosition(timePoint, /* extrapolate */true),
                timeActuallySpent, windwardDistanceSailed);
    }

    double getTimeOnTimeFactor(Competitor competitor) {
        return timeOnTimeFactorCache.computeIfAbsent(competitor, timeOnTimeFactor);
    }

    Duration getTimeOnDistanceFactorInSecondsPerNauticalMile(Competitor competitor) {
        return timeOnDistanceFactorInSecondsPerNauticalMileCache.computeIfAbsent(competitor, timeOnDistanceFactorNauticalMile);
    }

    @Override
    protected Duration getCalculatedTime(Competitor who, Supplier<Leg> leg, Supplier<Position> estimatedPosition,
            Duration totalDurationSinceRaceStart, Distance totalWindwardDistanceTraveled) {
        final Duration timeOnDistanceFactorInSecondsPerNauticalMile = getTimeOnDistanceFactorInSecondsPerNauticalMile(who);
        return totalDurationSinceRaceStart == null ? null :
            totalDurationSinceRaceStart.times(getTimeOnTimeFactor(who)).minus(
                totalWindwardDistanceTraveled == null || timeOnDistanceFactorInSecondsPerNauticalMile == null ? Duration.NULL :
                    timeOnDistanceFactorInSecondsPerNauticalMile.times(totalWindwardDistanceTraveled.getNauticalMiles()));
    }

    /**
     * Equal performance is defined here to mean equal reciproke VMG as determined by the formula
     * <pre>vmgc_i := t_i * f_i / d_i - g_i</pre> that, when equating it for <code>who</code> and <code>to</code> yields:
     * <pre>t_who * f_who / d_who - g_who = t_to * f_to / d_to - g_to</pre> Resolving for <code>t_who</code> gives:
     * <pre>t_who = (t_to * f_to / d_to - g_to + g_who) * d_who / f_who</pre> Furthermore, we have <code>d_who==d_to</code>
     * because we want to know how long <code>who</code> would take for that same distance under performance equal to
     * that of <code>to</code>.
     */
    @Override
    protected Duration getDurationToReachAtEqualPerformance(Competitor who, Competitor to, Waypoint fromWaypoint, TimePoint timePointOfTosPosition, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final MarkPassing whenToPassedFromWaypoint = getTrackedRace().getMarkPassing(to, fromWaypoint);
        final Duration t_who;
        if (whenToPassedFromWaypoint == null) {
            t_who = null;
        } else {
            validateGetDurationToReachAtEqualPerformanceParameters(to, fromWaypoint, timePointOfTosPosition, whenToPassedFromWaypoint);
            final Duration t_to = whenToPassedFromWaypoint.getTimePoint().until(timePointOfTosPosition);
            final Distance d_to = getWindwardDistanceTraveled(to, fromWaypoint, timePointOfTosPosition, cache);
            final double   f_to = getTimeOnTimeFactor(to);
            final Duration timeOnDistanceFactorInSecondsPerNauticalMileTo = getTimeOnDistanceFactorInSecondsPerNauticalMile(to);
            final double   g_to = timeOnDistanceFactorInSecondsPerNauticalMileTo==null?0:timeOnDistanceFactorInSecondsPerNauticalMileTo.asSeconds();
            final Distance d_who = d_to;
            final double   f_who = getTimeOnTimeFactor(who);
            final Duration timeOnDistanceFactorInSecondsPerNauticalMileWho = getTimeOnDistanceFactorInSecondsPerNauticalMile(who);
            final double   g_who = timeOnDistanceFactorInSecondsPerNauticalMileWho==null?0:timeOnDistanceFactorInSecondsPerNauticalMileWho.asSeconds();
            t_who = d_to == null ? null : new MillisecondsDurationImpl(Double.valueOf(
                    // first compute the seconds "to" would need with time-on-time correction for one nautical mile, then
                    // subtract "to"'s ToD allowance and add "who"'s ToD allowance, then scale by "who"'s distance in NM and
                    // apply who's ToT factor:
                    (1./(d_to.inTime(t_to.times(f_to)).getMetersPerSecond() / Mile.METERS_PER_NAUTICAL_MILE) - g_to + g_who)
                                  * d_who.getNauticalMiles() / f_who * 1000.).longValue());
        }
        return t_who;
    }

    /**
     * For the {@link RankingInfo#getTimePoint() ranking info's time point} compares <code>competitor</code> to
     * {@link RankingInfo#getLeaderByCorrectedEstimatedTimeToCompetitorFarthestAhead()}. Based on the definition of
     * "leader" the corrected estimated time for <code>competitor</code> when reaching the fastest boat's position at
     * {@link #timePoint} is expected to be greater than that of the leader. We're equating <code>competitor</code>'s
     * and the leader's corrected time when reaching the fastest boat's position at {@link #timePoint}, assuming a
     * summand in <code>competitor</code>'s actual time required to reach the fastest boat's position. This equation can
     * then be resolved for this additional summand (which is a negative duration), telling in <code>competitor</code>'s
     * own time how much time she would have to make good to rank equal to the leader.
     * <p>
     * 
     * The math behind this works as follows. Let <code>i</code> represent the <code>competitor</code>, <code>k</code>
     * the leader, <code>d</code> the total windward distance from the start to the fastest competitor's position at
     * {@link RankingInfo#getTimePoint() time point provided by the ranking info}. Then we have for the corrected
     * reciproke average corrected VMGs:
     * 
     * <pre>
     * t_i * f_i - d * g_i - diff_corr_t_i = t_k * f_k - d * g_k
     * </pre>
     * 
     * where <code>t_i / t_k</code> is the actual duration from the race start until competitor <code>i / k</code> (or
     * <code>competitor</code> and the leader, respectively) reaches the fastest competitor's position at
     * {@link #timePoint}. The <code>diff_corr_t_i</code> is the sorting criterion for ranking because corrected time is
     * (also for Polar Curve after mapping implied wind to corrected times through the use of a scratch boat) the
     * basis for ranking. But we would additionally like to understand what this difference means in <code>i</code>'s
     * own time, so we introduce <code>diff_t_i</code> as follows:
     * 
     * <pre>
     * (t_i - diff_t_i) * f_i - d * g_i = t_k * f_k - d * g_k
     * </pre>
     * 
     * which resolves to
     * 
     * <pre>
     * <b>diff_t_i</b> = t_i - (t_k * f_k + d * (g_i - g_k)) / f_i
     * </pre>
     */
    @Override
    public Duration getGapToLeaderInOwnTime(RankingMetric.RankingInfo rankingInfo, Competitor competitor, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Duration result;
        // actual times and common distance:
        final Competitor leaderByCorrectedEstimatedTimeToCompetitorFarthestAhead = rankingInfo.getLeaderByCorrectedEstimatedTimeToCompetitorFarthestAhead();
        if (leaderByCorrectedEstimatedTimeToCompetitorFarthestAhead == null) {
            result = null;
        } else {
            final Duration t_k = rankingInfo.getCompetitorRankingInfo().apply(leaderByCorrectedEstimatedTimeToCompetitorFarthestAhead).
                                                 getEstimatedActualDurationFromRaceStartToCompetitorFarthestAhead();
            final Duration t_i = rankingInfo.getCompetitorRankingInfo().apply(competitor).getEstimatedActualDurationFromRaceStartToCompetitorFarthestAhead();
            final Distance d   = rankingInfo.getCompetitorRankingInfo().apply(rankingInfo.getCompetitorFarthestAhead()).getWindwardDistanceSailed();
            result = getGapToCompetitorInOwnTime(competitor, leaderByCorrectedEstimatedTimeToCompetitorFarthestAhead, t_i, t_k, d);
        }
        return result;
    }

    /**
     * For the {@link RankingInfo#getTimePoint() ranking info's time point} compares <code>competitor</code> to
     * {@link RankingInfo#getLeaderByCorrectedEstimatedTimeToCompetitorFarthestAhead()}. Based on the definition of
     * "leader" the corrected estimated time for <code>competitor</code> when reaching the fastest boat's position at
     * {@link #timePoint} is expected to be greater than that of the leader. We're equating <code>competitor</code>'s
     * and the leader's corrected time when reaching the fastest boat's position at {@link #timePoint}, assuming a
     * summand in <code>competitor</code>'s actual time required to reach the fastest boat's position. This equation can
     * then be resolved for this additional summand (which is a negative duration), telling in <code>competitor</code>'s
     * own time how much time she would have to make good to rank equal to the leader.
     * <p>
     * 
     * The math behind this works as follows. Let <code>i</code> represent the <code>competitor</code>, <code>k</code>
     * the leader, <code>d</code> the total windward distance from the start to the fastest competitor's position at
     * {@link RankingInfo#getTimePoint() time point provided by the ranking info}. Then we have for the corrected
     * reciproke average corrected VMGs:
     * 
     * <pre>
     * t_i * f_i - d * g_i - diff_corr_t_i = t_k * f_k - d * g_k
     * </pre>
     * 
     * where <code>t_i / t_k</code> is the actual duration from the race start until competitor <code>i / k</code> (or
     * <code>competitor</code> and the leader, respectively) reaches the fastest competitor's position at
     * {@link #timePoint}. The <code>diff_corr_t_i</code> is the sorting criterion for ranking because corrected time is
     * (also for Polar Curve after mapping implied wind to corrected times through the use of a scratch boat) the
     * basis for ranking. But we would additionally like to understand what this difference means in <code>i</code>'s
     * own time, so we introduce <code>diff_t_i</code> as follows:
     * 
     * <pre>
     * (t_i - diff_t_i) * f_i - d * g_i = t_k * f_k - d * g_k
     * </pre>
     * 
     * which resolves to
     * 
     * <pre>
     * <b>diff_t_i</b> = t_i - (t_k * f_k + d * (g_i - g_k)) / f_i
     * </pre>
     * 
     * @param t_i the actual time that competitor <code>i</code> took since race start to travel windward distance <code>d</code>
     * @param t_k the actual time that competitor <code>k</code> took since race start to travel windward distance <code>d</code>
     */
    private Duration getGapToCompetitorInOwnTime(Competitor i, final Competitor k, final Duration t_i, final Duration t_k, final Distance d) {
        // handicap factors / allowances:
        final double   f_i = getTimeOnTimeFactor(i);
        final Duration g_i = getTimeOnDistanceFactorInSecondsPerNauticalMile(i);
        final double   f_k = getTimeOnTimeFactor(k);
        final Duration g_k = getTimeOnDistanceFactorInSecondsPerNauticalMile(k);
        final Duration diff_t_i;
        if (t_i == null || t_k == null || d == null) {
            diff_t_i = null;
        } else {
            diff_t_i = t_i.minus(t_k.times(f_k).plus(((g_i==null?Duration.NULL:g_i).minus((g_k==null?Duration.NULL:g_k))).times(d.getNauticalMiles())).divide(f_i));
        }
        return diff_t_i;
    }

    @Override
    public Duration getLegGapToLegLeaderInOwnTime(TrackedLegOfCompetitor trackedLegOfCompetitor, TimePoint timePoint,
            RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        assert rankingInfo instanceof NonPerformanceCurveRankingInfo;
        final NonPerformanceCurveRankingInfo npcRankingInfo = (NonPerformanceCurveRankingInfo) rankingInfo;
        final Duration result;
        final Leg leg = trackedLegOfCompetitor.getLeg();
        if (getTrackedRace().getStartOfRace() == null || !trackedLegOfCompetitor.hasStartedLeg(timePoint)) {
            result = null;
        } else {
            final Competitor farthestAheadOrEarliestLegFinisher = getCompetitorFarthestAheadInLeg(trackedLegOfCompetitor.getTrackedLeg(), timePoint, cache);
            if (farthestAheadOrEarliestLegFinisher == null) {
                result = null;
            } else {
                final TrackedLegOfCompetitor tlocOfFarthestAhead = trackedLegOfCompetitor.getTrackedLeg().getTrackedLeg(farthestAheadOrEarliestLegFinisher);
                final boolean farthestAheadAlreadyFinishedLeg = tlocOfFarthestAhead.hasFinishedLeg(timePoint);
                final Distance windwardDistanceFarthestTraveledUntilFinishingLeg = getWindwardDistanceTraveled(farthestAheadOrEarliestLegFinisher,
                            farthestAheadAlreadyFinishedLeg ? tlocOfFarthestAhead.getFinishTime() : timePoint, cache);
                // Consider all competitors, regardless of whether they have started the leg or not; a competitor in a previous
                // leg may well be the race leader. However, if a competitor has finished the leg already, use the finishing time
                // point for that competitor.
                // leader in the leg is now the competitor with the least corrected time to reach the competitor farthest ahead in the leg
                final Competitor legLeader = npcRankingInfo.getLeaderInLegByCalculatedTime(trackedLegOfCompetitor.getTrackedLeg().getLeg(), cache);
                result = getGapToCompetitorInOwnTime(trackedLegOfCompetitor.getCompetitor(), legLeader,
                        npcRankingInfo.getActualTimeFromRaceStartToReachFarthestAheadInLeg(trackedLegOfCompetitor.getCompetitor(), leg, cache),
                        npcRankingInfo.getActualTimeFromRaceStartToReachFarthestAheadInLeg(legLeader, leg, cache),
                        windwardDistanceFarthestTraveledUntilFinishingLeg);
            }
        }
        return result;
    }

}
