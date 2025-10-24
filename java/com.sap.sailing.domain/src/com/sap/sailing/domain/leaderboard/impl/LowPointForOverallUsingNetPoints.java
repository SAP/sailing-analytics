package com.sap.sailing.domain.leaderboard.impl;

import java.util.concurrent.Callable;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.MetaLeaderboard;
import com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher;
import com.sap.sailing.domain.leaderboard.meta.MetaLeaderboardColumn;
import com.sap.sse.common.TimePoint;


/**
 * For {@link MetaLeaderboard}s only; instead of using the ranks from the underlying leaderboards in each column,
 * this scoring scheme uses the net points from the underlying leaderboards to determine the points scored in the
 * respective column of the overall (meta) leaderboard.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class LowPointForOverallUsingNetPoints extends LowPoint {
    private static final long serialVersionUID = -2767385186133743330L;

    public LowPointForOverallUsingNetPoints() {
        super();
    }

    @Override
    public ScoringSchemeType getType() {
        return ScoringSchemeType.LOW_POINT_OVERALL_USING_NET_POINTS;
    }

    @Override
    public Double getScoreForRank(Leaderboard leaderboard, RaceColumn raceColumn, Competitor competitor, int rank,
            Callable<Integer> numberOfCompetitorsInRaceFetcher,
            NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher, TimePoint timePoint) {
        assert raceColumn instanceof MetaLeaderboardColumn;
        final Leaderboard underlyingLeaderboard = ((MetaLeaderboardColumn) raceColumn).getLeaderboard();
        return underlyingLeaderboard.getNetPoints(competitor, timePoint);
    }
}
