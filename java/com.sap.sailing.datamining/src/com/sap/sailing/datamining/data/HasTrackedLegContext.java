package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sse.common.Distance;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;

public interface HasTrackedLegContext {
    
    @Connector(scanForStatistics=false)
    public HasTrackedRaceContext getTrackedRaceContext();

    public TrackedLeg getTrackedLeg();
    
    @Dimension(messageKey="LegType", ordinal=6)
    public LegType getLegType();
    @Dimension(messageKey="LegNumber", ordinal=7)
    public int getLegNumber();
    @Statistic(messageKey = "LengthoftheLeg", resultDecimals = 2)
    Distance getLegLength();
}