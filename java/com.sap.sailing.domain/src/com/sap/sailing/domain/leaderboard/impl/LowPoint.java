package com.sap.sailing.domain.leaderboard.impl;

import java.util.concurrent.Callable;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher;


/**
 * The scoring system as used by the ISAF standard scoring scheme, also known as the "Low Point Scoring System."
 * Scores are primarily attributed according to rank, so a race's winner gets score 1.00 and so on. Lower scores are
 * therefore better.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class LowPoint extends AbstractScoringSchemeImpl {
    private static final long serialVersionUID = -2767385186133743330L;

    public LowPoint() {
        super(/* higherIsBetter */ false);
    }

    @Override
    public Double getScoreForRank(RaceColumn raceColumn, Competitor competitor, int rank, Callable<Integer> numberOfCompetitorsInRaceFetcher) {
        return rank == 0 ? null : (double) rank;
    }

    @Override
    public Double getPenaltyScore(RaceColumn raceColumn, Competitor competitor, MaxPointsReason maxPointsReason,
            Integer numberOfCompetitorsInRace, NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher) {
        Double result;
        if (numberOfCompetitorsInRace == null) {
            result = (double) (numberOfCompetitorsInLeaderboardFetcher.getNumberOfCompetitorsInLeaderboard()+1);
        } else {
            result = (double) (numberOfCompetitorsInRace+1);
        }
        return result;
    }

    @Override
    public ScoringSchemeType getType() {
        return ScoringSchemeType.LOW_POINT;
    }

    @Override
    public boolean isValidInTotalScore(Leaderboard leaderboard, RaceColumn raceColumn, TimePoint at) {
        return true;
    }
}
