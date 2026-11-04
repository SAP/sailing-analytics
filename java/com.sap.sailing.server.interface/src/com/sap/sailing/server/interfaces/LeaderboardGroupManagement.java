package com.sap.sailing.server.interfaces;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.sap.sailing.domain.common.ConflictObjectType;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardGroup;
import com.sap.sse.common.ObjectAlreadyExistsException;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

/**
 * Request-side permission and ownership handling shared by REST and GWT. Replicated domain operations
 * independently validate uniqueness under the registry locks, before making any changes.
 */
public final class LeaderboardGroupManagement {
    private LeaderboardGroupManagement() {
    }

    public static LeaderboardGroup createLeaderboardGroup(final RacingEventService service,
            final SecurityService securityService, final UUID id,
            final String name, final String description, final String displayName,
            final boolean displayGroupsInReverseOrder, final List<String> leaderboardNames,
            final int[] overallLeaderboardDiscardThresholds, final ScoringSchemeType overallLeaderboardScoringSchemeType) {
        final Runnable validateGroup = () -> {
            if (service.getLeaderboardGroupByID(id) != null) {
                throw new ObjectAlreadyExistsException(ConflictObjectType.LEADERBOARD_GROUP, id.toString());
            }
            checkGroupNameAvailable(service, name, /* existingGroup */ null);
        };
        final Callable<LeaderboardGroup> createGroup = () -> securityService
                .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD_GROUP,
                        LeaderboardGroupImpl.getTypeRelativeObjectIdentifier(id), name, validateGroup,
                        () -> service.apply(new CreateLeaderboardGroup(id, name, description, displayName,
                                displayGroupsInReverseOrder, leaderboardNames, overallLeaderboardDiscardThresholds,
                                overallLeaderboardScoringSchemeType)));
        final LeaderboardGroup result;
        if (overallLeaderboardScoringSchemeType != null) {
            final String overallName = LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(name);
            result = securityService.setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                    SecuredDomainType.LEADERBOARD, new TypeRelativeObjectIdentifier(overallName), overallName,
                    () -> {
                        validateGroup.run();
                        checkOverallNameAvailable(service, overallName);
                    }, createGroup);
        } else {
            result = call(createGroup);
        }
        return result;
    }

    public static void updateLeaderboardGroup(final RacingEventService service, final SecurityService securityService,
            final UUID id, final String newName,
            final String description, final String displayName, final List<String> leaderboardNames,
            final int[] overallLeaderboardDiscardThresholds, final ScoringSchemeType overallLeaderboardScoringSchemeType) {
        final LeaderboardGroup group = service.getLeaderboardGroupByID(id);
        if (group == null) {
            throw new IllegalArgumentException("Leaderboard group with ID " + id + " not found");
        }
        // Keep the permission decision and the corresponding update together for concurrent requests on this group.
        synchronized (group) {
            securityService.checkCurrentUserUpdatePermission(group);
            checkGroupNameAvailable(service, newName, group);
            final Callable<Void> update = () -> service.apply(new UpdateLeaderboardGroup(id, newName, description,
                    displayName, leaderboardNames, overallLeaderboardDiscardThresholds, overallLeaderboardScoringSchemeType));
            final Leaderboard overallLeaderboard = group.getOverallLeaderboard();
            if (overallLeaderboard == null && overallLeaderboardScoringSchemeType != null) {
                final String overallName = LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(newName);
                securityService.setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD,
                        new TypeRelativeObjectIdentifier(overallName), overallName,
                        () -> checkOverallNameAvailable(service, overallName), update);
            } else if (overallLeaderboard != null && overallLeaderboardScoringSchemeType == null) {
                securityService.checkPermissionAndDeleteOwnershipForObjectRemoval(overallLeaderboard, update);
            } else {
                if (overallLeaderboard != null) {
                    securityService.checkCurrentUserUpdatePermission(overallLeaderboard);
                }
                call(update);
            }
        }
    }

    private static void checkGroupNameAvailable(final RacingEventService service, final String name,
            final LeaderboardGroup existingGroup) {
        final LeaderboardGroup groupWithName = service.getLeaderboardGroupByName(name);
        if (groupWithName != null && (existingGroup == null || !existingGroup.getName().equals(name))) {
            throw new ObjectAlreadyExistsException(ConflictObjectType.LEADERBOARD_GROUP, name);
        }
    }

    private static void checkOverallNameAvailable(final RacingEventService service, final String name) {
        if (service.getLeaderboardByName(name) != null) {
            throw new ObjectAlreadyExistsException(ConflictObjectType.LEADERBOARD, name);
        }
    }

    private static <T> T call(final Callable<T> action) {
        final T result;
        try {
            result = action.call();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
