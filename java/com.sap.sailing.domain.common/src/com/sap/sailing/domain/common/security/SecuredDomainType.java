package com.sap.sailing.domain.common.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.impl.HasPermissionsImpl;

/**
 * Logical domain types in the "sailing" domain that require the user to have certain permissions
 * in order to use their actions. These types are defined here in the "common" bundle so that
 * the server as well as the client can check them.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class SecuredDomainType extends HasPermissionsImpl {
    private static final long serialVersionUID = -7072719056136061490L;
    private static final Set<HasPermissions> allInstances = new HashSet<>();
    
    public SecuredDomainType(String logicalTypeName, Action... availableActions) {
        super(logicalTypeName, availableActions);
        allInstances.add(this);
    }
    
    public SecuredDomainType(String logicalTypeName) {
        super(logicalTypeName);
        allInstances.add(this);
    }
    
    public static Iterable<HasPermissions> getAllInstances() {
        return Collections.unmodifiableSet(allInstances);
    }
    
    // AdminConsole permissions
    public static final HasPermissions MANAGE_MARK_PASSINGS = new SecuredDomainType("MANAGE_MARK_PASSINGS");
    public static final HasPermissions MANAGE_MARK_POSITIONS = new SecuredDomainType("MANAGE_MARK_POSITIONS");
    public static final HasPermissions DETAIL_TIMER = new SecuredDomainType("DETAIL_TIMER"); // TODO this is not a valid "HasPermission" instance; it's more an operation the user may be granted on objects of the TimePanel type
    
    public static final HasPermissions EVENT = new SecuredDomainType("EVENT");

    public static final HasPermissions REGATTA = new SecuredDomainType("REGATTA");

    public static final HasPermissions LEADERBOARD = new SecuredDomainType("LEADERBOARD");

    public static final HasPermissions LEADERBOARD_GROUP = new SecuredDomainType("LEADERBOARD_GROUP");

    public static final HasPermissions TRACKED_RACE = new SecuredDomainType("TRACKED_RACE",
            TrackedRaceActions.ALL_ACTIONS);
    
    public static enum TrackedRaceActions implements Action {
        CAN_REPLAY_DURING_LIVE_RACES;

        private static final Action[] ALL_ACTIONS = new Action[] { CAN_REPLAY_DURING_LIVE_RACES, DefaultActions.READ,
                DefaultActions.CREATE, DefaultActions.UPDATE, DefaultActions.CHANGE_OWNERSHIP,
                DefaultActions.CHANGE_ACL };

        public static final Action[] MUTATION_ACTIONS = new Action[] { DefaultActions.CREATE, DefaultActions.UPDATE,
                DefaultActions.CHANGE_OWNERSHIP, DefaultActions.CHANGE_ACL };
    }

    public static enum CompetitorAndBoatActions implements Action {
        READ_PUBLIC;
        
        public static final Action[] READ_AND_READ_PUBLIC_ACTIONS = new Action[] { READ_PUBLIC, DefaultActions.READ };

        private static final Action[] ALL_ACTIONS = new Action[] { READ_PUBLIC, DefaultActions.READ,
                DefaultActions.CREATE, DefaultActions.UPDATE, DefaultActions.CHANGE_OWNERSHIP,
                DefaultActions.CHANGE_ACL };

        public static final Action[] MUTATION_ACTIONS = new Action[] { DefaultActions.CREATE, DefaultActions.UPDATE,
                DefaultActions.CHANGE_OWNERSHIP, DefaultActions.CHANGE_ACL };
    };

    public static final HasPermissions COMPETITOR = new SecuredDomainType("COMPETITOR", CompetitorAndBoatActions.ALL_ACTIONS);

    public static final HasPermissions BOAT = new SecuredDomainType("BOAT", CompetitorAndBoatActions.ALL_ACTIONS);

    public static final HasPermissions MEDIA_TRACK = new SecuredDomainType("MEDIA_TRACK");

    public static final HasPermissions RESULT_IMPORT_URL = new SecuredDomainType("RESULT_IMPORT_URL");

    public static enum ReplicatorActions implements Action {
        START, STOP, DROP_CONNECTION
    };

    public static final HasPermissions REPLICATOR = new SecuredDomainType("REPLICATOR", ReplicatorActions.values());

    public static final HasPermissions RACE_MANAGER_APP_DEVICE_CONFIGURATION = new SecuredDomainType(
            "RACE_MANAGER_APP_DEVICE_CONFIGURATION");

    public static final HasPermissions EXPEDITION_DEVICE_CONFIGURATION = new SecuredDomainType(
            "EXPEDITION_DEVICE_CONFIGURATION");
    public static final HasPermissions IGTIMI_ACCOUNT = new SecuredDomainType("IGTIMI_ACCOUNT");
    public static final HasPermissions SWISS_TIMING_ACCOUNT = new SecuredDomainType("SWISS_TIMING_ACCOUNT");
    public static final HasPermissions SWISS_TIMING_ARCHIVE_ACCOUNT = new SecuredDomainType(
            "SWISS_TIMING_ARCHIVE_ACCOUNT");
    public static final HasPermissions TRACTRAC_ACCOUNT = new SecuredDomainType("TRACTRAC_ACCOUNT");
}
