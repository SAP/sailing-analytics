package com.sap.sailing.domain.leaderboard.impl;

import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.ScoringSchemeType;

/**
 * The default score of all columns, including medal race columns, is defined as 1.0 for this
 * scoring scheme. This way, the medal series races for quarter-final, semi-final and grand
 * final do not need explicit column factors of 1.0 when (as is usual for this format) no
 * doubling of these race scores is desired.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class LowPointWithEliminatingMedalSeriesPromotingTwoToFinalAndTwoToSemifinal
        extends LowPointWithEliminatingMedalSeriesWithPromotions {
    private static final long serialVersionUID = -6753499035563730886L;

    public LowPointWithEliminatingMedalSeriesPromotingTwoToFinalAndTwoToSemifinal() {
        super(new int[] { 2, 2 });
    }

    @Override
    public ScoringSchemeType getType() {
        return ScoringSchemeType.LOW_POINT_WITH_ELIMINATING_MEDAL_SERIES_PROMOTING_TWO_TO_FINAL_AND_TWO_TO_SEMIFINAL;
    }
    
    
    @Override
    public double getScoreFactor(RaceColumn raceColumn) {
        Double factor = raceColumn.getExplicitFactor();
        if (factor == null) {
            factor = 1.0;
        }
        return factor;
    }
}
