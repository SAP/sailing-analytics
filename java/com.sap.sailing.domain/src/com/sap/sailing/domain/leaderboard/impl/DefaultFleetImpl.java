package com.sap.sailing.domain.leaderboard.impl;

import com.sap.sailing.domain.base.IsManagedBySharedDomainFactory;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.common.LeaderboardNameConstants;

public class DefaultFleetImpl extends FleetImpl implements IsManagedBySharedDomainFactory {

    private static final long serialVersionUID = 6233489616955255401L;

    public DefaultFleetImpl() {
        super(LeaderboardNameConstants.DEFAULT_FLEET_NAME);
    }

    @Override
    public IsManagedBySharedDomainFactory resolve(SharedDomainFactory domainFactory) {
        return FlexibleLeaderboardImpl.defaultFleet;
    }

}
