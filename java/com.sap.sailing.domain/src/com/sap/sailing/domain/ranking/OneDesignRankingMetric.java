package com.sap.sailing.domain.ranking;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tracking.impl.RaceRankComparator;
import com.sap.sailing.domain.tracking.impl.WindwardToGoComparator;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public class OneDesignRankingMetric extends NonPerformanceCurveRankingMetric {
    private static final long serialVersionUID = -8141113385324184349L;
    
    public OneDesignRankingMetric(TrackedRace trackedRace) {
        super(trackedRace);
    }

    @Override
    public RankingMetrics getType() {
        return RankingMetrics.ONE_DESIGN;
    }

    @Override
    public Comparator<Competitor> getRaceRankingComparator(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return new RaceRankComparator(getTrackedRace(), timePoint, cache);
    }

    @Override
    public Comparator<TrackedLegOfCompetitor> getLegRankingComparator(TrackedLeg trackedLeg,
            TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return new WindwardToGoComparator(trackedLeg, timePoint, cache);
    }

    @Override
    public Duration getGapToLeaderInOwnTime(RankingMetric.RankingInfo rankingInfo, Competitor competitor, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        // When the competitor is in the same leg as the leader (which in this ranking metric is always the boat
        // farthest ahead) then the competitor's duration to reach the current leader's position is estimated based on the
        // competitor's average VMG on the current leg.
        // If the leader has already finished the competitor's current leg, the time when the competitor will probably
        // finish the leg is compared to the time when the leader finished the leg.
        // When the competitor has already finished the current leg this can only mean the competitor finished the race
        // because otherwise the competitor would not be considered in this leg anymore. In this case, the mark passing
        // times for finishing the leg are compared between competitor and leader.
        final Duration result;
        final Competitor to = rankingInfo.getCompetitorFarthestAhead();
        final TrackedLegOfCompetitor currentLegTo = getCurrentLegOrLastLegIfAlreadyFinished(to, rankingInfo.getTimePoint());
        if (currentLegTo == null) {
            result = null;
        } else {
            final TimePoint tosLegFinishingTime = currentLegTo.getFinishTime();
            // If who is on the last leg, to will also be considered on the last leg, even if to already finished that leg.
            // If to finished the last leg and who is on the last leg, getEstimatedActualDurationFromTimePointToCompetitorFarthestAhead will
            // not work properly because it considers both on the same leg and therefore doesn't restrict the time to when to finished
            // the leg and looks at the excess time.
            final Waypoint lastWaypoint = getTrackedRace().getRace().getCourse().getLastWaypoint();
            
            final Duration howLongWhoNedsToReachTosPosition = rankingInfo.getCompetitorRankingInfo().apply(competitor).getEstimatedActualDurationFromTimePointToCompetitorFarthestAhead();
            if (howLongWhoNedsToReachTosPosition == null) {
                result = null;
            } else {
                if (tosLegFinishingTime != null && !tosLegFinishingTime.after(rankingInfo.getTimePoint()) &&
                        currentLegTo.getTrackedLeg().getLeg().getTo() == lastWaypoint) {
                    final Duration howLongAgoToPassedTheFinishLine = tosLegFinishingTime.until(rankingInfo.getTimePoint());
                    result = howLongWhoNedsToReachTosPosition.plus(howLongAgoToPassedTheFinishLine);
                } else {
                    result = howLongWhoNedsToReachTosPosition;
                }
            }
        }
        return result;
    }

    @Override
    public Duration getCorrectedTime(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getActualTimeSinceStartOfRace(competitor, timePoint);
    }

    /**
     * For one-design classes, the duration for <code>who</code> equals the duration it took <code>to</code> to
     * reach her current position since passing <code>fromWaypoint</code>.
     */
    @Override
    protected Duration getDurationToReachAtEqualPerformance(Competitor who, Competitor to, Waypoint fromWaypoint, TimePoint timePointOfTosPosition, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final MarkPassing whenToPassedFromWaypoint = getTrackedRace().getMarkPassing(to, fromWaypoint);
        validateGetDurationToReachAtEqualPerformanceParameters(to, fromWaypoint, timePointOfTosPosition, whenToPassedFromWaypoint);
        return whenToPassedFromWaypoint.getTimePoint().until(timePointOfTosPosition);
    }

    @Override
    protected Duration getCalculatedTime(Competitor who, Supplier<Leg> leg, Supplier<Position> estimatedPosition,
            Duration totalDurationSinceRaceStart, Distance totalWindwardDistanceTraveled) {
        return totalDurationSinceRaceStart;
    }

    @Override
    public Duration getLegGapToLegLeaderInOwnTime(TrackedLegOfCompetitor trackedLegOfCompetitor, TimePoint timePoint,
            final RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return trackedLegOfCompetitor.getGapToLeader(timePoint, WindPositionMode.LEG_MIDDLE, rankingInfo, cache);
    }

    @Override
    public RankingInfo getRankingInfo(final TimePoint timePoint, final WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return new AbstractRankingInfo(timePoint) {
            private static final long serialVersionUID = 25689357311324825L;

            @Override
            public Function<Competitor, CompetitorRankingInfo> getCompetitorRankingInfo() {
                return competitor -> getCompetitorRankingInfo(competitor);
            }

            private CompetitorRankingInfo getCompetitorRankingInfo(final Competitor competitor) {
                return new CompetitorRankingInfo() {
                    private static final long serialVersionUID = 1164789004900690406L;

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
                        return getWindwardDistanceTraveled(competitor, timePoint, cache);
                    }

                    @Override
                    public Duration getDurationSinceStartOfRaceUntilTimePoint() {
                        final TimePoint startOfRace = getTrackedRace().getStartOfRace();
                        return startOfRace==null?null:startOfRace.until(getTimePoint());
                    }

                    @Override
                    public Duration getTimeElapsed() {
                        final TimePoint startOfRace = getTrackedRace().getStartOfRace();
                        final Duration result;
                        if (startOfRace == null) {
                            result = null;
                        } else {
                            result = startOfRace.until(timePoint);
                        }
                        return result;
                    }

                    /**
                     * Corrected time is the same as actual time for one-design ranking
                     */
                    @Override
                    public Duration getCorrectedTime() {
                        return getTimeElapsed();
                    }

                    @Override
                    public Duration getEstimatedActualDurationFromTimePointToCompetitorFarthestAhead() {
                        return getPredictedDurationToReachWindwardPositionOf(competitor, getCompetitorFarthestAhead(), timePoint, cache);
                    }

                    /**
                     * Corrected time is the same as actual time for one-design ranking
                     */
                    @Override
                    public Duration getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead() {
                        return getEstimatedActualDurationFromRaceStartToCompetitorFarthestAhead();
                    }
                };
            }

            @Override
            public Competitor getCompetitorFarthestAhead() {
                return getTrackedRace().getOverallLeader(timePoint, cache);
            }

            @Override
            public Competitor getLeaderByCorrectedEstimatedTimeToCompetitorFarthestAhead() {
                return getCompetitorFarthestAhead();
            }
        };
    }
}
