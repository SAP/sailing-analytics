package com.sap.sailing.server.security;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.RolePrototype;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

/**
 * Specifies a role that when associated to a user gives read access to the sailing domain parts needed for the home
 * page and basic analytical frontends.
 */
public class SailingViewerRole extends RolePrototype {
    private static final long serialVersionUID = 3291793984984443193L;

    private static final SailingViewerRole INSTANCE = new SailingViewerRole();

    SailingViewerRole() {
        super("sailing_viewer", "c42948df-517b-45cb-9fa9-d1e79f18e115",
                WildcardPermission.builder().withTypes(SecuredDomainType.EVENT, SecuredDomainType.LEADERBOARD_GROUP,
                        SecuredDomainType.LEADERBOARD, SecuredDomainType.REGATTA, SecuredDomainType.TRACKED_RACE)
                        .withActions(DefaultActions.READ).build(), WildcardPermission.builder().withTypes(SecuredDomainType.COMPETITOR, SecuredDomainType.BOAT)
                        .withActions(SecuredSecurityTypes.PublicReadableActions.READ_PUBLIC).build(),
                WildcardPermission.builder().withTypes(SecuredDomainType.COMPETITOR, SecuredDomainType.BOAT)
                        .withActions(DefaultActions.READ).build());
    }

    public static SailingViewerRole getInstance() {
        return INSTANCE;
    }
}
