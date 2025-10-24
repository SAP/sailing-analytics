package com.sap.sailing.gwt.ui.client;

import com.sap.sailing.gwt.common.communication.routing.ProvidesLeaderboardRouting;
import com.sap.sse.gwt.client.ServiceRoutingProvider;

public class AbstractSailingWriteEntryPoint extends AbstractSailingEntryPoint<SailingServiceWriteAsync> {
    
    /**
     * Lazily initialize sailing service. Concrete entry point subclasses may influence sailing service creation by
     * implementing a given routing information providing interface {@link ProvidesLeaderboardRouting}. The routing
     * information provided via {@link ProvidesLeaderboardRouting#getLeaderboardName()} needs to be available when this
     * method is called for the first time. Repetitive calls will not cause a new leaderboard name to take effect.
     */
    @Override
    protected SailingServiceWriteAsync getSailingService() {
        if (sailingService == null) {
            if (this instanceof ServiceRoutingProvider) {
                sailingService = SailingServiceHelper.createSailingServiceWriteInstance((ServiceRoutingProvider) this);
            } else {
                sailingService = SailingServiceHelper.createSailingServiceWriteInstance();
            }
        }
        return sailingService;
    }
}
