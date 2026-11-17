package com.sap.sailing.domain.leaderboard.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.AdditionalScoringInformationFinder;
import com.sap.sailing.domain.abstractlog.race.scoring.AdditionalScoringInformationType;
import com.sap.sailing.domain.abstractlog.race.scoring.RaceLogAdditionalScoringInformationEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * {@link HighPointFirstGetsFixedScore} scheme that in most cases applies
 * 10 points for the winner. Iff for one race column a {@link RaceLogAdditionalScoringInformationEvent}
 * is found then 8 points are applied for the winner.
 * 
 * From Phil, Race Director, on 12.09.2014: "If there is a tie in the regatta score between two or more boats at any time, the tie
 * shall be broken in favour of the boat that has won the most races. If a tie still remains, it shall be broken in
 * favour of the boat that had the better place at the last race sailed."
 * 
 * @author Simon Marcel Pamies
 */
public abstract class AbstractHighPointFirstGetsFixedOr8AndLastBreaksTie extends HighPointFirstGetsFixedScore {
    private static final long serialVersionUID = 1L;
    
    private final double SCORE_FOR_WINNER_IF_OVERWRITTEN = 8.0;
    private static final int MIN_RACES_REQUIRED_TO_BE_SCORED = 3;

    public AbstractHighPointFirstGetsFixedOr8AndLastBreaksTie(double maxPoints) {
        super(maxPoints);
    }

    @Override
    public int compareByBetterScore(Competitor o1, List<Util.Pair<RaceColumn, Double>> o1Scores, Competitor o2, List<Util.Pair<RaceColumn, Double>> o2Scores, Iterable<RaceColumn> raceColumnsToConsider, boolean nullScoresAreBetter, TimePoint timePoint, Leaderboard leaderboard, Map<Competitor, Set<RaceColumn>> discardedRaceColumnsPerCompetitor, BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        Double o1Highest = getHighestScore(o1Scores);
        Double o2Highest = getHighestScore(o2Scores);
        return o2Highest.compareTo(o1Highest);
    }

    private double getHighestScore(List<com.sap.sse.common.Util.Pair<RaceColumn, Double>> scores) {
        double highestScore = 0;
        for (com.sap.sse.common.Util.Pair<RaceColumn, Double> score : scores) {
            final double scoreScaledByFactor = getScoreScaledByFactor(score.getA(), score.getB());
            if (scoreScaledByFactor > highestScore) {
                highestScore = scoreScaledByFactor;
            }
        }
        return highestScore;
    }
    
    /**
     * Generally, in the Extreme Sailing Series the number of races scored doesn't matter. However, if a competitor
     * scored fewer than five races, the competitor doesn't participate in the overall ranking and is therefore to be
     * sorted worse than all competitors completing five or more races. 
     */
    @Override
    public int compareByNumberOfRacesScored(int competitor1NumberOfRacesScored, int competitor2NumberOfRacesScored) {
        int result;
        if (competitor1NumberOfRacesScored >= MIN_RACES_REQUIRED_TO_BE_SCORED) {
            if (competitor2NumberOfRacesScored >= MIN_RACES_REQUIRED_TO_BE_SCORED) {
                result = 0;
            } else {
                result = -1; // competitor1 is better ("less") because it's the only one of the two scoring more than MIN_RACES_REQUIRED_TO_BE_SCORED
            }
        } else {
            if (competitor2NumberOfRacesScored >= MIN_RACES_REQUIRED_TO_BE_SCORED) {
                result = 1; // competitor2 is better ("less") because it's the only one of the two scoring more than MIN_RACES_REQUIRED_TO_BE_SCORED
            } else {
                result = 0;
            }
        }
        return result;
    }

    @Override
    public Double getScoreForRank(Leaderboard leaderboard, RaceColumn raceColumn, Competitor competitor, int rank, Callable<Integer> numberOfCompetitorsInRaceFetcher, NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher, TimePoint timePoint) {
        Double effectiveScore = super.getScoreForRank(leaderboard, raceColumn, competitor, rank, numberOfCompetitorsInRaceFetcher, numberOfCompetitorsInLeaderboardFetcher, timePoint);
        return checkForOverwrittenScore(raceColumn, rank, effectiveScore);
    }

    /** The race's maximum score, including the same race-log override used for finish scores. */
    protected double getScoreForRaceWinner(final RaceColumn raceColumn) {
        return checkForOverwrittenScore(raceColumn, /* rank */ 1, getScoreForRaceWinner());
    }
    
    private Double checkForOverwrittenScore(RaceColumn raceColumn, int rank, Double effectiveScore) {
        Double result = effectiveScore;
        for (Fleet fleet : raceColumn.getFleets()) {
            RaceLog raceLog = raceColumn.getRaceLog(fleet);
            AdditionalScoringInformationFinder finder = new AdditionalScoringInformationFinder(raceLog);
            RaceLogAdditionalScoringInformationEvent event = finder.analyze(AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE);
            if (event != null) {
                if (rank == 0) {
                    result = null;
                } else {
                    result = Math.max(getMinimumScoreFromRank(), (double) (SCORE_FOR_WINNER_IF_OVERWRITTEN - rank + 1));
                }
            }
        }
        return result;
    }
}
