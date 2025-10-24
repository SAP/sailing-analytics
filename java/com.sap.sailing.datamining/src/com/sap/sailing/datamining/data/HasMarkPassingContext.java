package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;

public interface HasMarkPassingContext extends HasManeuver, HasWindOnTrackedLegOfCompetitor {
    @Connector(scanForStatistics=false)
    HasTrackedLegOfCompetitorContext getTrackedLegOfCompetitorContext();
    
    @Connector(messageKey="Waypoint", ordinal=14)
    Waypoint getWaypoint();

    @Dimension(messageKey="PassingSide", ordinal=15)
    NauticalSide getPassingSide();

    @Statistic(messageKey="RelativeScore", ordinal=3, resultDecimals=2)
    Double getRelativeRank();
    
    @Statistic(messageKey="AbsoluteRank", ordinal=4, resultDecimals=2)
    Double getAbsoluteRank();
    
}