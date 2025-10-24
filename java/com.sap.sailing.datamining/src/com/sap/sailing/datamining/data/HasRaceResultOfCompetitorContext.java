package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;

public interface HasRaceResultOfCompetitorContext extends HasSomethingOfCompetitorContextWithTrackedRaceContext {
    @Connector(scanForStatistics=false)
    HasLeaderboardContext getLeaderboardContext();

    @Override
    @Connector(scanForStatistics=false)
    HasTrackedRaceContext getTrackedRaceContext();

    @Connector(messageKey="Boat")
    Boat getBoat();

    @Dimension(messageKey="Regatta", ordinal=4)
    String getRegattaName();
    
    @Dimension(messageKey="RelativeScoreInPercent", ordinal=6)
    ClusterDTO getPercentageClusterForRelativeScore();

    @Dimension(messageKey="WindSpeedInBeaufort", ordinal=7)
    int getAverageWindSpeedInRoundedBeaufort();
    
    /**
     * 0 means the competitor won the race, 1 means the competitor ranked last
     */
    @Statistic(messageKey="RelativeScore", ordinal=1, resultDecimals=2)
    Double getRelativeRank();
    
    @Statistic(messageKey="AbsoluteRank", ordinal=2, resultDecimals=2)
    Double getAbsoluteRank();
    
    @Dimension(messageKey="IRM")
    MaxPointsReason getMaxPointsReason();
    
    @Dimension(messageKey="Discarded")
    boolean isDiscarded();
    
    @Statistic(messageKey="NumberOfPodiumFinish", ordinal=3)
    Boolean isPodiumFinish();
    
    @Statistic(messageKey="NumberOfWins", ordinal=4)
    Boolean isWin();
}
