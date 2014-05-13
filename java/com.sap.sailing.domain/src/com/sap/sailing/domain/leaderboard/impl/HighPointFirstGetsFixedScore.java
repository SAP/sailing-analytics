package com.sap.sailing.domain.leaderboard.impl;

import java.util.concurrent.Callable;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher;


/**
 * A variant of the {@link HighPoint} scoring system where the winner gets a fixed number of points, and
 * all subsequent ranks get one point less. No negative scores are returned. 1.0 is the worst score a
 * competitor can score.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public abstract class HighPointFirstGetsFixedScore extends HighPoint {
    private static final long serialVersionUID = -2767385186133743330L;

    private final double scoreForRaceWinner;
    
    public HighPointFirstGetsFixedScore(double scoreForRaceWinner) {
        this.scoreForRaceWinner = scoreForRaceWinner;
    }
    
    protected double getScoreForRaceWinner() {
        return scoreForRaceWinner;
    }

    @Override
    public Double getScoreForRank(RaceColumn raceColumn, Competitor competitor, int rank, Callable<Integer> numberOfCompetitorsInRaceFetcher, NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher) {
        final int effectiveRank = getEffectiveRank(raceColumn, competitor, rank);
        final Double result;
        if (effectiveRank == 0) {
            result = null;
        } else {
            result = Math.max(1.0, (double) (getScoreForRaceWinner() - effectiveRank + 1));
        }
        return result;
    }

    protected int getEffectiveRank(RaceColumn raceColumn, Competitor competitor, int rank) {
        final int effectiveRank;
        int competitorFleetOrdering;
        if (rank == 0) {
            effectiveRank = 0;
        } else {
            final Fleet fleetOfCompetitor = raceColumn.getFleetOfCompetitor(competitor);
            if (fleetOfCompetitor != null && raceColumn.hasSplitFleetContiguousScoring() && (competitorFleetOrdering=fleetOfCompetitor.getOrdering()) != 0) {
                int numberOfCompetitorsInBetterFleets = getNumberOfCompetitorsInBetterFleets(raceColumn, competitorFleetOrdering);
                effectiveRank = rank + numberOfCompetitorsInBetterFleets;
            } else {
                effectiveRank = rank;
            }
        }
        return effectiveRank;
    }
}
