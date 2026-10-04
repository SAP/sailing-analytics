package com.sap.sailing.server.hierarchy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.SecurityUtils;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SecurityService.RoleCopyListener;
import com.sap.sse.security.ShiroWildcardPermissionFromParts;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.PermissionAndRoleAssociation;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;

/**
 * Encapsulates the logic to ensure consistency of group owners in the sailing domain object hierarchy.
 */
public class SailingHierarchyOwnershipUpdater {
    public static SailingHierarchyOwnershipUpdater createOwnershipUpdater(boolean createNewGroup,
            UUID existingGroupIdOrNull, String newGroupName, boolean migrateCompetitors, boolean migrateBoats,
            boolean copyMembersAndRoles,
            RacingEventService service) {
        SecurityService securityService = service.getSecurityService();
        final UserGroup sourceGroup = securityService.getUserGroup(existingGroupIdOrNull);
        final GroupOwnerUpdateStrategy updateStrategy;
        if (!createNewGroup) {
            updateStrategy = createExitingGroupModifyingUpdate(sourceGroup);
        } else {
            if (copyMembersAndRoles) {
                updateStrategy = createNewGroupUsingUpdate(newGroupName, securityService, sourceGroup, service);
            } else {
                updateStrategy = createNewGroupWithoutCopying(newGroupName, securityService);
            }
        }
        return new SailingHierarchyOwnershipUpdater(service, securityService, updateStrategy, migrateCompetitors,
                migrateBoats);
    }

    public interface GroupOwnerUpdateStrategy {
        boolean needsUpdate(QualifiedObjectIdentifier identifier, OwnershipAnnotation currentOwnership);
        UserGroup getNewGroupOwner();
    }

    private final RacingEventService service;
    private final SecurityService securityService;
    private final GroupOwnerUpdateStrategy updateStrategy;
    private final boolean updateCompetitors;
    private final boolean updateBoats;
    private final Set<QualifiedObjectIdentifier> objectsToUpdateOwnershipsFor;
    private final Set<Event> visitedEvents;
    private final Set<LeaderboardGroup> visitedLeaderboardGroups;
    private final Set<Leaderboard> visitedLeaderboards;

    private SailingHierarchyOwnershipUpdater(final RacingEventService service, SecurityService securityService,
            final GroupOwnerUpdateStrategy updateStrategy, final boolean updateCompetitors, final boolean updateBoats) {
        this.service = service;
        this.securityService = securityService;
        this.updateStrategy = updateStrategy;
        this.updateCompetitors = updateCompetitors;
        this.updateBoats = updateBoats;
        objectsToUpdateOwnershipsFor = new HashSet<>();
        visitedEvents = new HashSet<>();
        visitedLeaderboardGroups = new HashSet<>();
        visitedLeaderboards = new HashSet<>();
    }

    public void updateGroupOwnershipForEventHierarchy(Event event) {
        updateGroupOwnershipForEventHierarchyInternal(event);
        commitChanges();
    }

    private void updateGroupOwnershipForEventHierarchyInternal(Event event) {
        if (visitedEvents.add(event)) {
            updateGroupOwner(event.getIdentifier());
            SailingHierarchyWalker.walkFromEvent(event, /* includeLeaderboardGroupsWithOverallLeaderboard */ false,
                    new EventHierarchyVisitor() {
                @Override
                public void visit(Leaderboard leaderboard, Set<LeaderboardGroup> leaderboardGroups) {
                    updateGroupOwnershipForLeaderboardHierarchyInternal(leaderboard);
                }
    
                @Override
                public void visit(LeaderboardGroup leaderboardGroup) {
                    // leaderboard groups with overall leaderboard may be visited if all their leaderboards belong
                    // to the "event", but the process won't recurse back into "event" as we pass it explicitly as
                    // an event not to visit
                    updateGroupOwnershipForLeaderboardGroupHierarchyInternal(leaderboardGroup);
                }
            });
        }
    }

    public void updateGroupOwnershipForLeaderboardGroupHierarchy(LeaderboardGroup leaderboardGroup) {
        updateGroupOwnershipForLeaderboardGroupHierarchyInternal(leaderboardGroup);
        commitChanges();
    }

    private void updateGroupOwnershipForLeaderboardGroupHierarchyInternal(LeaderboardGroup leaderboardGroup) {
        if (visitedLeaderboardGroups.add(leaderboardGroup)) {
            updateGroupOwner(leaderboardGroup.getIdentifier());
            SailingHierarchyWalker.walkFromLeaderboardGroup(service, leaderboardGroup,
                    /* includeEventsIfLeaderboardGroupHasOverallLeaderboard */ true,
                    new LeaderboardGroupHierarchyVisitor() {
                        @Override
                        public void visit(Leaderboard leaderboard) {
                            updateGroupOwnershipForLeaderboardHierarchyInternal(leaderboard);
                        }
    
                        @Override
                        public void visit(Event event) {
                            updateGroupOwnershipForEventHierarchyInternal(event);
                        }
                    });
        }
    }

    public void updateGroupOwnershipForLeaderboardHierarchy(Leaderboard leaderboard) {
        updateGroupOwnershipForLeaderboardHierarchyInternal(leaderboard);
        commitChanges();
    }
    
    private void updateGroupOwnershipForLeaderboardHierarchyInternal(Leaderboard leaderboard) {
        if (visitedLeaderboards.add(leaderboard)) {
            updateGroupOwner(leaderboard.getIdentifier());
            if (leaderboard instanceof RegattaLeaderboard) {
                RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                updateGroupOwner(regattaLeaderboard.getRegatta().getIdentifier());
            }
            SailingHierarchyWalker.walkFromLeaderboard(leaderboard, new LeaderboardHierarchyVisitor() {
                @Override
                public void visit(TrackedRace race) {
                    updateGroupOwner(race.getIdentifier());
                }
    
                @Override
                public void visit(Boat boat) {
                    if (updateBoats) {
                        updateGroupOwner(boat.getIdentifier());
                    }
                }
    
                @Override
                public void visit(Competitor competitor) {
                    if (updateCompetitors) {
                        updateGroupOwner(competitor.getIdentifier());
                    }
                }
            });
        }
    }

    private void updateGroupOwner(QualifiedObjectIdentifier id) {
        final OwnershipAnnotation ownership = securityService.getOwnership(id);
        if (updateStrategy.needsUpdate(id, ownership)) {
            final WildcardPermission permission = id.getPermission(DefaultActions.CHANGE_OWNERSHIP);
            SecurityUtils.getSubject().checkPermission(new ShiroWildcardPermissionFromParts(permission));
            objectsToUpdateOwnershipsFor.add(id);
        }
    }

    private void commitChanges() {
        final UserGroup groupOwnerToSet = updateStrategy.getNewGroupOwner();
        for (QualifiedObjectIdentifier id : objectsToUpdateOwnershipsFor) {
            final OwnershipAnnotation ownership = securityService.getOwnership(id);
            securityService.setOwnership(id, ownership == null ? null : (User) ownership.getAnnotation().getUserOwner(),
                    groupOwnerToSet);
        }
    }



    private static GroupOwnerUpdateStrategy createExitingGroupModifyingUpdate(final UserGroup sourceGroup) {
        if (sourceGroup == null) {
            throw new RuntimeException("User group does not exist");
        }
        final GroupOwnerUpdateStrategy updateStrategy;
        updateStrategy = new GroupOwnerUpdateStrategy() {
            @Override
            public boolean needsUpdate(QualifiedObjectIdentifier identifier, OwnershipAnnotation currentOwnership) {
                return currentOwnership == null
                        || !sourceGroup.equals(currentOwnership.getAnnotation().getTenantOwner());
            }

            @Override
            public UserGroup getNewGroupOwner() {
                return sourceGroup;
            }
        };
        return updateStrategy;
    }

    /**
     * Creates a new group without copying the members and roles from an old group but add the current user to the new
     * group.
     */
    private static GroupOwnerUpdateStrategy createNewGroupWithoutCopying(final String newGroupName,
            final SecurityService securityService) {
        if (newGroupName == null || newGroupName.isEmpty()) {
            throw new RuntimeException("No name for new Group given");
        }

        final GroupOwnerUpdateStrategy updateStrategy;
        updateStrategy = new GroupOwnerUpdateStrategy() {

            private UserGroup groupOwnerToSet;

            @Override
            public boolean needsUpdate(QualifiedObjectIdentifier identifier, OwnershipAnnotation currentOwnership) {
                return true;
            }

            @Override
            public UserGroup getNewGroupOwner() {
                if (groupOwnerToSet == null) {
                    try {
                        groupOwnerToSet = securityService.createUserGroup(UUID.randomUUID(), newGroupName);
                        final QualifiedObjectIdentifier identifier = groupOwnerToSet.getIdentifier();
                        securityService.setDefaultOwnership(identifier, identifier.toString());
                        securityService.addUserToUserGroup(groupOwnerToSet, securityService.getCurrentUser());
                    } catch (UserGroupManagementException e) {
                        throw new RuntimeException("Could not create user group");
                    }
                }
                return groupOwnerToSet;
            }
        };
        return updateStrategy;
    }

    private static GroupOwnerUpdateStrategy createNewGroupUsingUpdate(String newGroupName,
            SecurityService securityService, final UserGroup sourceGroup, RacingEventService service) {
        if (newGroupName == null || newGroupName.isEmpty()) {
            throw new RuntimeException("No name for new Group given");
        }
        final GroupOwnerUpdateStrategy updateStrategy;
        updateStrategy = new GroupOwnerUpdateStrategy() {
            private UserGroup groupOwnerToSet;

            @Override
            public boolean needsUpdate(QualifiedObjectIdentifier identifier, OwnershipAnnotation currentOwnership) {
                return true;
            }

            @Override
            public UserGroup getNewGroupOwner() {
                if (groupOwnerToSet == null) {
                    try {
                        if (sourceGroup != null) {
                            // When migrating from an existing user group -> copy as much as possible from the
                            // existing group to make the migrated objects to be visible for most people as before
                            groupOwnerToSet = copyUserGroup(sourceGroup, newGroupName, securityService, service);
                        } else {
                            // The migration may start at an object that currently has no group owner (e.g. in case
                            // this owner was just deleted) -> in this case we just create a new group
                            groupOwnerToSet = securityService.createUserGroup(UUID.randomUUID(), newGroupName);
                            final QualifiedObjectIdentifier identifier = groupOwnerToSet.getIdentifier();
                            securityService.setDefaultOwnership(identifier, identifier.toString());
                        }
                    } catch (UserGroupManagementException e) {
                        throw new RuntimeException("Could not create user group");
                    }
                }
                return groupOwnerToSet;
            }
        };
        return updateStrategy;
    }

    private static UserGroup copyUserGroup(UserGroup userGroupToCopy, String name, SecurityService securitySerice,
            RacingEventService service) throws UserGroupManagementException {
        // explicitly loading the current version of the group in case the given instance e.g. originates from the UI
        // and is possibly out of date.
        final UUID newGroupId = UUID.randomUUID();
        return securitySerice.setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredSecurityTypes.USER_GROUP, UserGroupImpl.getTypeRelativeObjectIdentifier(newGroupId), name, () -> {
                    final UserGroup createdUserGroup = securitySerice.createUserGroup(newGroupId, name);
                    securitySerice.copyUsersAndRoleAssociations(userGroupToCopy, createdUserGroup,
                            new RoleCopyListener() {
                                @Override
                                public void onRoleCopy(User user, Role existingRole, Role copyRole) {
                                    TypeRelativeObjectIdentifier existingAssociationTypeIdentifier = PermissionAndRoleAssociation
                                            .get(existingRole, user);
                                    TypeRelativeObjectIdentifier copyAssociationTypeIdentifier = PermissionAndRoleAssociation
                                            .get(copyRole,
                                            user);
                                    QualifiedObjectIdentifier existingQualifiedTypeIdentifier = SecuredSecurityTypes.ROLE_ASSOCIATION
                                            .getQualifiedObjectIdentifier(existingAssociationTypeIdentifier);
                                    QualifiedObjectIdentifier copyQualifiedTypeIdentifier = SecuredSecurityTypes.ROLE_ASSOCIATION
                                            .getQualifiedObjectIdentifier(copyAssociationTypeIdentifier);
                                    OwnershipAnnotation existingOwner = securitySerice
                                            .getOwnership(existingQualifiedTypeIdentifier);
                                    securitySerice.setOwnership(copyQualifiedTypeIdentifier,
                                            existingOwner.getAnnotation().getUserOwner(),
                                            existingOwner.getAnnotation().getTenantOwner(),
                                            existingOwner.getDisplayNameOfAnnotatedObject());
                                }
                            });

                    return createdUserGroup;
                });
    }
}
