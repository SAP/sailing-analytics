package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.security.SecurityService;

public interface HasLeaderboardGroupContext {
    @Connector(messageKey="LeaderboardGroup", ordinal=0)
    LeaderboardGroup getLeaderboardGroup();

    PolarDataService getPolarDataService();

    DomainFactory getBaseDomainFactory();
    
    SecurityService getSecurityService();
    
    SailorProfiles getSailorProfiles();
}
