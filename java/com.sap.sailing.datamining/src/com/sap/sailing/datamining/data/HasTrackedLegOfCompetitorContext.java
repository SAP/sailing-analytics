package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;

public interface HasTrackedLegOfCompetitorContext extends HasWindOnTrackedLeg {
    @Connector(scanForStatistics=false)
    public HasTrackedLegContext getTrackedLegContext();
    
    public TrackedLegOfCompetitor getTrackedLegOfCompetitor();
    
    @Dimension(messageKey="CompetitorSearchTag", ordinal=11) // TODO Clean me: Move Dimension to Competitor when possible
    public String getCompetitorSearchTag();
    
    @Dimension(messageKey="RelativeScoreInRaceInPercent", ordinal=12)
    public ClusterDTO getPercentageClusterForRelativeScoreInRace();

    @Connector(messageKey="Competitor")
    public Competitor getCompetitor();
    
    @Statistic(messageKey="DistanceTraveled", resultDecimals=0, ordinal=0)
    public Distance getDistanceTraveled();
    
    @Statistic(messageKey="RankGainsOrLosses", resultDecimals=2, ordinal=1)
    public Double getRankGainsOrLosses();
    
    @Statistic(messageKey="RelativeScore", resultDecimals=2, ordinal=2)
    public Double getRelativeRank();
    
    @Statistic(messageKey="AbsoluteRank", resultDecimals=2, ordinal=3)
    public Double getAbsoluteRank();
    
    @Statistic(messageKey="TimeSpentInSeconds", resultDecimals=0, ordinal=4)
    public Long getTimeTakenInSeconds();

}