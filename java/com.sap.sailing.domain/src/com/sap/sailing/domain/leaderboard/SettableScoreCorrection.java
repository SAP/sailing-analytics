package com.sap.sailing.domain.leaderboard;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.MaxPointsReason;

public interface SettableScoreCorrection extends ScoreCorrection {

    /**
     * @param reason
     *            if <code>null</code>, any existing max-points reason is removed; while
     *            {@link #getMaxPointsReason(Competitor, RaceColumn)} will return {@link MaxPointsReason#NONE}, a
     *            call to {@link #isScoreCorrected(Competitor, RaceColumn)} will return <code>false</code> if no
     *            other explicit score correction was made for the <code>competitor</code>.
     */
    void setMaxPointsReason(Competitor competitor, RaceColumn raceColumn, MaxPointsReason reason);
    
    MaxPointsReason getMaxPointsReason(Competitor competitor, RaceColumn raceColumn);

    void correctScore(Competitor competitor, RaceColumn raceColumn, int points);

    /**
     * Removes a score correction which makes the competitor's score for <code>raceColumn</code> to fall back to the score
     * determined by the tracking data.
     */
    void uncorrectScore(Competitor competitor, RaceColumn raceColumn);
    
    /**
     * @return <code>null</code> if not set for the competitor, e.g., because no correction was made or an explicit
     *         {@link MaxPointsReason} was provided for the competitor.
     */
    Integer getExplicitScoreCorrection(Competitor competitor, RaceColumn raceColumn);

    boolean hasCorrectionFor(RaceColumn raceInLeaderboard);

}
