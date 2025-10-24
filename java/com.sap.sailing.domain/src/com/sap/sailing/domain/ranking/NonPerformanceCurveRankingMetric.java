package com.sap.sailing.domain.ranking;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

public abstract class NonPerformanceCurveRankingMetric extends AbstractRankingMetric {
    private static final long serialVersionUID = 2647817114244817444L;

    public interface NonPerformanceCurveRankingInfo extends RankingInfoWithLegLeader {
        Duration getActualTimeFromRaceStartToReachFarthestAheadInLeg(Competitor competitor, Leg leg, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);
    }
    
    public class NonPerformanceCurveRankingInfoImpl extends AbstractRankingInfoWithCompetitorRankingInfoCache implements NonPerformanceCurveRankingInfo {
        private static final long serialVersionUID = 7525315823563332681L;

        public NonPerformanceCurveRankingInfoImpl(TimePoint timePoint, Map<Competitor, RankingMetric.CompetitorRankingInfo> competitorRankingInfo, Competitor competitorFarthestAhead) {
            super(timePoint, competitorRankingInfo, competitorFarthestAhead);
        }

        @Override
        public Duration getActualTimeFromRaceStartToReachFarthestAheadInLeg(Competitor competitor, Leg leg, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
            final Duration result;
            final TrackedLegOfCompetitor tloc = getTrackedRace().getTrackedLeg(competitor, leg);
            final Duration raceDurationAtTimePoint = getTrackedRace().getStartOfRace().until(getTimePoint());
            if (tloc != null && tloc.hasStartedLeg(getTimePoint())) {
                final Competitor competitorFarthestAheadInLeg = getCompetitorFarthestAheadInLeg(leg, getTimePoint(), cache);
                final TrackedLegOfCompetitor tlocOfCompetitorFarthestAheadInLeg = tloc.getTrackedLeg().getTrackedLeg(competitorFarthestAheadInLeg);
                final Duration predictedDurationFromTimePointToReachFarthestAheadInLeg = getPredictedDurationToReachWindwardPositionOf(
                        tloc, tlocOfCompetitorFarthestAheadInLeg, getTimePoint(), cache);
                if (predictedDurationFromTimePointToReachFarthestAheadInLeg == null) {
                    result = null;
                } else {
                    final Duration cDurationFromRaceStartToReachFarthestInLeg = raceDurationAtTimePoint.plus(predictedDurationFromTimePointToReachFarthestAheadInLeg);
                    result = cDurationFromRaceStartToReachFarthestInLeg;
                }
            } else {
                result = null;
            }
            return result;
        }
    }

    protected NonPerformanceCurveRankingMetric(TrackedRace trackedRace) {
        super(trackedRace);
    }
    
    /**
     * Not all implementations may need the leg and the estimated position; therefore, to avoid unnecessary
     * calculations, {@link Supplier}s are expected instead of the values themselves, allowing for lazy on-demand
     * calculation.
     * 
     * @param estimatedPosition
     *            the position where the competitor <code>who</code> is when calculating the corrected time; some
     *            ranking metrics may require this information to determine quickly how far within the current leg the
     *            competitor has sailed. As others may not need it at all, the parameter is declared as a
     *            {@link Supplier} which delays evaluation until it is needed or avoids it altogether.
     */
    protected abstract Duration getCalculatedTime(Competitor who, Supplier<Leg> leg,
            Supplier<Position> estimatedPosition, Duration totalDurationSinceRaceStart,
            Distance totalWindwardDistanceTraveled);

    public RankingMetric.RankingInfo getRankingInfo(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        Map<Competitor, RankingMetric.CompetitorRankingInfo> result = new HashMap<>();
        Competitor competitorFarthestAhead = getCompetitorFarthestAhead(timePoint, cache);
        if (competitorFarthestAhead != null) {
            final Distance totalWindwardDistanceTraveled = getWindwardDistanceTraveled(competitorFarthestAhead, timePoint, cache);
            final TimePoint startOfRace = getTrackedRace().getStartOfRace();
            if (startOfRace != null) {
                final Duration durationSinceStartOfRaceUntilTimePoint = startOfRace.until(timePoint);
                final Waypoint finish = getTrackedRace().getRace().getCourse().getLastWaypoint();
                for (Competitor competitor : getCompetitors()) {
                    // accommodate also for the possibility of an empty course (finish==null)
                    final MarkPassing finishMarkPassing =  finish == null ? null : getTrackedRace().getMarkPassing(competitor, finish);
                    final Duration timeElapsed;
                    if (finishMarkPassing != null && finishMarkPassing.getTimePoint().before(timePoint)) {
                        // competitor has already finished the race at timePoint; so the time elapsed for that competitor stops counting
                        // at the finish mark passing:
                        timeElapsed = startOfRace.until(finishMarkPassing.getTimePoint());
                    } else {
                        timeElapsed = durationSinceStartOfRaceUntilTimePoint;
                    }
                    // TODO bug5110: we cannot compute the following if at timePoint the position of either of the two competitors involved is unknown; we can, however, do this if timePoint is after the two finish mark passings, or if the competitorFarthestAhead has already finished at timePoint and the position of "competitor" is known.
                    final Duration predictedDurationToReachWindwardPositionOfCompetitorFarthestAhead = getPredictedDurationToReachWindwardPositionOf(
                            competitor, competitorFarthestAhead, timePoint, cache);
                    final Duration totalEstimatedDurationSinceRaceStartToCompetitorFarthestAhead = predictedDurationToReachWindwardPositionOfCompetitorFarthestAhead == null ? null
                            : durationSinceStartOfRaceUntilTimePoint.plus(predictedDurationToReachWindwardPositionOfCompetitorFarthestAhead);
                    final Duration calculatedEstimatedTimeWhenReachingCompetitorFarthestAhead = totalEstimatedDurationSinceRaceStartToCompetitorFarthestAhead == null ? null
                            : getCalculatedTime(
                                    competitor,
                                    () -> getTrackedRace().getTrackedLeg(competitorFarthestAhead, timePoint).getLeg(),
                                    () -> getTrackedRace().getTrack(competitorFarthestAhead).getEstimatedPosition(
                                            timePoint, /* extrapolate */true),
                                    totalEstimatedDurationSinceRaceStartToCompetitorFarthestAhead,
                                    totalWindwardDistanceTraveled);
                    final Duration calculatedTime = getCalculatedTime(competitor,
                            () -> getTrackedRace().getCurrentLeg(competitor, timePoint).getLeg(), () -> getTrackedRace()
                                    .getTrack(competitor).getEstimatedPosition(timePoint, /* extrapolated */true),
                            getTrackedRace().getTimeSailedSinceRaceStart(competitor, timePoint), totalWindwardDistanceTraveled);
                    RankingMetric.CompetitorRankingInfo rankingInfo = new CompetitorRankingInfoImpl(timePoint,
                            competitor, getWindwardDistanceTraveled(competitor, timePoint, cache),
                            durationSinceStartOfRaceUntilTimePoint, timeElapsed, calculatedTime,
                            predictedDurationToReachWindwardPositionOfCompetitorFarthestAhead,
                            calculatedEstimatedTimeWhenReachingCompetitorFarthestAhead);
                    result.put(competitor, rankingInfo);
                }
            }
        }
        return new NonPerformanceCurveRankingInfoImpl(timePoint, result, competitorFarthestAhead);
    }

    /**
     * Predicts how long <code>who</code> will take to reach competitor <code>to</code>'s position at
     * <code>timePoint</code>, starting at <code>who</code>'s position at <code>timePoint</code>, assuming a continued
     * performance for <code>who</code> that matches her average VMG on her current leg so far, and equal performance
     * with <code>to</code> on any subsequent leg that <code>who</code> needs to travel to reach <code>to</code>'s
     * position at <code>timePoint</code>. If <code>to</code> has already finished the race, the finish line position is
     * where <code>who</code> needs to arrive.
     * <p>
     * If <code>to</code> is already in a later leg, <code>who</code>'s remaining duration to reach the end of her
     * current leg is estimated using
     * {@link TrackedLegOfCompetitor#getEstimatedTimeToNextMark(TimePoint, com.sap.sailing.domain.tracking.WindPositionMode)}
     * ; then from the waypoint reached this way the
     * {@link #getAbsoluteWindwardDistanceTraveled(Competitor, Waypoint, TimePoint) windward distance to competitor
     * <code>to</code>} is determined and from this, using the handicaps for both competitors, <code>who</code> and
     * <code>to</code>, their performance between the waypoint and <code>to</code>'s position at <code>timePoint</code>
     * is equated, hence assuming that considering their handicaps, both competitors are doing equally well on this part
     * of the course, meaning that <code>who</code> will not gain any (corrected) time on <code>to</code> during this
     * period. From the equations, the duration it will take <code>who</code> to reach this position starting at the
     * upcoming waypoint can be determined which is then added to the duration estimated to reach that upcoming waypoint
     * (based on <code>who</code>'s average VMG in her current leg).
     * <p>
     * 
     * If <code>who</code> and <code>to</code> are in the same leg, the windward distance is calculated, and
     * <code>who</code>'s average VMG during the current leg is used to estimate the time until she reaches the position
     * <code>to</code> had at <code>timePoint</code>.
     * 
     * Precondition: <code>who</code>'s windward / along-course position is behind that of <code>to</code>, or an
     * {@link IllegalArgumentException} will be thrown.
     * <p>
     * 
     * @return <code>null</code>, if either of the two competitors' current legs is <code>null</code>
     */
    protected Duration getPredictedDurationToReachWindwardPositionOf(Competitor who, Competitor to, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final TrackedLegOfCompetitor currentLegWho = getCurrentLegOrLastLegIfAlreadyFinished(who, timePoint);
        final TrackedLegOfCompetitor currentLegTo = getCurrentLegOrLastLegIfAlreadyFinished(to, timePoint);
        final Duration result = getPredictedDurationToReachWindwardPositionOf(currentLegWho, currentLegTo, timePoint, cache);
        return result;
    }

    /**
     * Similar to
     * {@link #getPredictedDurationToReachWindwardPositionOf(Competitor, Competitor, TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache)},
     * allowing the caller to specify the legs to consider for the two competitors. This way, the "to" competitor's
     * leg may be set to one that is not necessarily the current leg at <code>timePoint</code>, enabling a comparison
     * for a specific leg.
     * <p>
     * 
     * The resulting duration may be negative if <code>legWho</code>'s competitor has reached the position in question
     * before <code>timePoint</code>.
     * <p>
     * 
     * Precondition: <code>legWho</code>'s leg is the same as or an earlier leg than <code>legTo</code>'s leg.
     */
    protected Duration getPredictedDurationToReachWindwardPositionOf(final TrackedLegOfCompetitor legWho, final TrackedLegOfCompetitor legTo,
            TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        assert legWho == null || legTo == null ||
                getTrackedRace().getRace().getCourse().getIndexOfWaypoint(legWho.getTrackedLeg().getLeg().getFrom()) <=
                getTrackedRace().getRace().getCourse().getIndexOfWaypoint(legTo.getTrackedLeg().getLeg().getFrom());
        final Duration result;
        if (legWho == null || legTo == null ||
                !(isAssumedToHaveStartedLeg(timePoint, legWho) || isAssumedToHaveFinishedLeg(timePoint, legWho)) ||
                !(isAssumedToHaveStartedLeg(timePoint, legTo) || isAssumedToHaveFinishedLeg(timePoint, legTo))) {
            result = null;
        } else {
            final Competitor who = legWho.getCompetitor();
            final Competitor to = legTo.getCompetitor();
            if (who == to) {
                if (isAssumedToHaveFinishedLeg(timePoint, legWho) && legWho.getFinishTime() != null) {
                    result = timePoint.until(legWho.getFinishTime()); // negative time; reached end of leg some time ago
                } else {
                    result = Duration.NULL; // still racing in the leg; reaches its own position in no time
                }
            } else {
                assert getTrackedRace().getRace().getCourse().getIndexOfWaypoint(legWho.getLeg().getFrom()) <= getTrackedRace()
                        .getRace().getCourse().getIndexOfWaypoint(legTo.getLeg().getFrom());
                // bug5316: note that the following may result in a negative duration; this is particularly the case
                // when "who" has sailed (or is extrapolated) beyond the end of the leg without having received a mark passing.
                // In this case we want to assume that it will take "who" approximately that long again to reach back to
                // the waypoint to actually pass it; so we use the absolute duration instead.
                final Duration toEndOfLegOrTo = getPredictedDurationToEndOfLegOrTo(timePoint, legWho, legWho.getTrackedLeg().getTrackedLeg(to), cache);
                // toEndOfLegOrTo may be a negative Duration in case both have finished the leg before timePoint; it will then represent the duration
                // between "who"'s leg finish mark passing and timePoint.
                if (toEndOfLegOrTo == null) {
                    result = null;
                } else {
                    final Duration durationForSubsequentLegsToReachAtEqualPerformance;
                    if (legWho.getLeg() == legTo.getLeg()) {
                        durationForSubsequentLegsToReachAtEqualPerformance = Duration.NULL;
                    } else {
                        // FIXME bug5316: isn't it strange to assume that "to" reached its current position at timePoint? It could have been way earlier than that...
                        durationForSubsequentLegsToReachAtEqualPerformance = getDurationToReachAtEqualPerformance(who, to,
                                legWho.getLeg().getTo(), timePoint, cache);
                    }
                    result = durationForSubsequentLegsToReachAtEqualPerformance == null ? null : toEndOfLegOrTo.plus(durationForSubsequentLegsToReachAtEqualPerformance);
                }
            }
        }
        return result;
    }

    /**
     * Computes the duration that <code>who</code> would take to reach <code>to</code>'s windward / along-track position
     * at <code>timePoint</code>, starting at <code>fromWaypoint</code>, assuming the same corrected performance at
     * which <code>to</code> sailed starting at <code>fromWaypoint</code> up to her current position.
     * <p>
     * 
     * Precondition: competitor <code>to</code> has already passed <code>fromWaypoint</code>. If not, an
     * {@link IllegalArgumentException} will be thrown.
     * <p>
     * 
     * Implementations can validate this precondition using
     * {@link #validateGetDurationToReachAtEqualPerformanceParameters(Competitor, Waypoint, TimePoint, MarkPassing)}.
     */
    protected abstract Duration getDurationToReachAtEqualPerformance(Competitor who, Competitor to, Waypoint fromWaypoint,
            TimePoint timePointOfTosPosition, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);
    
    protected Comparator<Competitor> getComparatorByEstimatedCorrectedTimeWhenReachingCompetitorFarthestAhead(
            final Function<Competitor, RankingMetric.CompetitorRankingInfo> rankingInfos) {
        return (c1, c2) -> rankingInfos.apply(c1).getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead()
                .compareTo(rankingInfos.apply(c2).getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead());
    }

    @Override
    public Speed getReferenceImpliedWind(TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final SpeedWithConfidence<TimePoint> averageWindWithConfidence = getTrackedRace().getAverageWindSpeedWithConfidenceWithNumberOfSamples(/* number of samples */ 10);
        return averageWindWithConfidence == null ? null : averageWindWithConfidence.getObject();
    }
}
