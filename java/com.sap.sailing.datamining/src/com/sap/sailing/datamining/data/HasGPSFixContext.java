package com.sap.sailing.datamining.data;

import java.util.Locale;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public interface HasGPSFixContext {
    
    @Connector(scanForStatistics=false)
    public HasTrackedLegOfCompetitorContext getTrackedLegOfCompetitorContext();
    
    @Connector(ordinal=1)
    public GPSFixMoving getGPSFix();
    
    @Dimension(messageKey="WindStrengthInBeaufort", ordinal=11)
    public ClusterDTO getWindStrengthAsBeaufortCluster(Locale locale, ResourceBundleStringMessages stringMessages);

}