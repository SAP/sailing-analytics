package com.sap.sse.security.storemerging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import com.mongodb.ConnectionString;
import com.mongodb.client.ClientSession;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.PermissionAndRoleAssociation;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.PersistenceFactory;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

public class SecurityStoreMerger {
    private static final Logger logger = Logger.getLogger(SecurityStoreMerger.class.getName());
    
    private static final String TARGET_DEFAULT_TENANT_NAME_SYSTEM_PROPERTY_NAME = "default.tenant.name";

    private final UserStore targetUserStore;
    private final AccessControlStore targetAccessControlStore;

    public SecurityStoreMerger(MongoDBConfiguration cfgForTarget, String targetDefaultCreationGroupName) throws UserStoreManagementException {
        this(cfgForTarget, cfgForTarget.getService(), targetDefaultCreationGroupName);
    }
    
    public SecurityStoreMerger(MongoDBConfiguration cfgForTarget, MongoDBService serviceForTarget, String targetDefaultCreationGroupName) throws UserStoreManagementException {
        this(PersistenceFactory.create(serviceForTarget), cfgForTarget, targetDefaultCreationGroupName);
    }

    public SecurityStoreMerger(ClientSession clientSession, MongoDBConfiguration cfgForTarget, String targetDefaultCreationGroupName) throws UserStoreManagementException {
        this(clientSession, cfgForTarget, cfgForTarget.getService(), targetDefaultCreationGroupName);
    }

    public SecurityStoreMerger(ClientSession clientSession, MongoDBConfiguration cfgForTarget, MongoDBService serviceForTarget, String targetDefaultCreationGroupName) throws UserStoreManagementException {
        this(PersistenceFactory.create(clientSession, serviceForTarget), cfgForTarget, targetDefaultCreationGroupName);
    }

    public SecurityStoreMerger(PersistenceFactory targetPf, MongoDBConfiguration cfgForTarget, String targetDefaultCreationGroupName) throws UserStoreManagementException {
        logger.info("Loading target user store from "+cfgForTarget);
        this.targetUserStore = loadUserStore(targetPf, targetDefaultCreationGroupName);
        logger.info("Loading target access control store from "+cfgForTarget);
        this.targetAccessControlStore = loadAccessControlStore(targetPf, targetUserStore);
    }

    private AccessControlStore loadAccessControlStore(final PersistenceFactory targetPf, UserStore userStore) {
        final AccessControlStore accessControlStore = new AccessControlStoreImpl(
                targetPf.getDefaultMajorityDomainObjectFactory(),
                targetPf.getDefaultMajorityMongoObjectFactory(), userStore);
        accessControlStore.loadACLsAndOwnerships();
        return accessControlStore;
    }

    private UserStoreImpl loadUserStore(final PersistenceFactory targetPf, String defaultTenantName)
            throws UserStoreManagementException {
        final UserStoreImpl userStore = new UserStoreImpl(targetPf.getDefaultMajorityDomainObjectFactory(),
                targetPf.getDefaultMajorityMongoObjectFactory(), defaultTenantName);
        userStore.ensureDefaultRolesExist();
        // actually load the users and migrate them if required
        userStore.loadAndMigrateUsers();
        return userStore;
    }

    public UserStore getTargetUserStore() {
        return targetUserStore;
    }

    public AccessControlStore getTargetAccessControlStore() {
        return targetAccessControlStore;
    }

    /**
     * The target MongoDB into which to merge is provided by the system properties {@code mongo.uri} or alternatively
     * {@code mongo.port}, {@code mongo.host} and {@code mongo.dbName}. See also {@link MongoDBConfiguration}. The
     * source databases from which to import, as well as their default creation group names are provided as pairs of
     * command line arguments to this main method where the first element of each pair is the MongoDB URI indicating the
     * database from which from read, and the second element being the name of the store's corresponding default
     * creation group name (we don't expect this to be used, still it needs to be provided for consistency).<p>
     * 
     * Order matters. The import will happen source by source, as if you executed those imports one after the other.
     * Clash / conflict resolution then works as specified for a single import.<p>
     * 
     * The importing environment's default creation group has to be provided in the system property named as defined
     * by {@link #TARGET_DEFAULT_TENANT_NAME_SYSTEM_PROPERTY_NAME}.<p>
     * 
     * Example:<br>
     *  {@code java -Dmongo.uri="mongodb://localhost/winddb" -Ddefault.tenant.name=my-group} {@link SecurityStoreMerger} {@code "mongodb://localhost:27017/winddb2" my-default-group "mongodb://otherhost:12345/winddb3?replicaSet=rs0" another-group}
     * 
     * @param args
     *            pairs of MongoDB URI and default creation group names for the stores from which to import
     */
    public static void main(String[] args) throws UserStoreManagementException {
        final MongoDBConfiguration cfgForTarget = MongoDBConfiguration.getDefaultConfiguration();
        final SecurityStoreMerger instance = new SecurityStoreMerger(cfgForTarget, System.getProperty(TARGET_DEFAULT_TENANT_NAME_SYSTEM_PROPERTY_NAME));
        for (int i=0; i<args.length/2; i++) {
            final MongoDBConfiguration cfgForSource = new MongoDBConfiguration(new ConnectionString(args[2*i]));
            final MongoDBService sourceService = cfgForSource.getService();
            instance.importStores(sourceService.startCausallyConsistentSession(), cfgForSource, sourceService, args[2*i+1]);
        }
    }

    public Pair<UserStore, AccessControlStore> importStores(ClientSession causallyConsistentSessionForSource, MongoDBConfiguration cfgForSource, MongoDBService sourceService, String defaultCreationGroupNameForSource) throws UserStoreManagementException {
        final Pair<UserStore, AccessControlStore> sourceStores = readStores(causallyConsistentSessionForSource, cfgForSource, sourceService, defaultCreationGroupNameForSource);
        logger.info("Importing user store and access control store read from "+cfgForSource);
        importStores(sourceStores.getA(), sourceStores.getB());
        return sourceStores;
    }
    
    Pair<UserStore, AccessControlStore> readStores(ClientSession causallyConsistentSessionForSource, MongoDBConfiguration cfgForSource, MongoDBService sourceService, String defaultCreationGroupNameForSource) throws UserStoreManagementException {
        logger.info("Reading user store and access control store from "+cfgForSource);
        final PersistenceFactory sourcePf = PersistenceFactory.create(causallyConsistentSessionForSource, sourceService);
        final UserStore sourceUserStore = loadUserStore(sourcePf, defaultCreationGroupNameForSource);
        final AccessControlStore sourceAccessControlStore = loadAccessControlStore(sourcePf, sourceUserStore);
        return new Pair<>(sourceUserStore, sourceAccessControlStore);
    }
    
    void importStores(final UserStore sourceUserStore, final AccessControlStore sourceAccessControlStore) throws UserStoreManagementException {
        logger.info("Importing user store and access control store");
        // the following maps work like this: The keys are source objects to be imported.
        // If the key object is to be added to the target, it is its own value;
        // if it is to be dropped, the key is not part of the map. If it is to be merged with an object in the target,
        // the corresponding target object is the value.
        final Map<User, User> userMap = markUsersForAddMergeOrDrop(sourceUserStore, sourceAccessControlStore);
        final Map<UserGroup, UserGroup> userGroupMap = markUserGroupsForAddMergeOrDrop(sourceUserStore, sourceAccessControlStore, userMap);
        replaceSourceUserReferencesToUsersAndGroups(userMap, userGroupMap, sourceAccessControlStore);
        replaceSourceUserGroupReferencesToUsers(userMap, userGroupMap);
        replaceSourceAccessControlListReferencesToGroups(sourceAccessControlStore, userGroupMap);
        mergeUsersAndGroups(sourceUserStore, userMap, userGroupMap, sourceAccessControlStore);
        // analyze ownerships after mergeUsersAndGroups because mergeUsersAndGroups may remove
        // role and permission association ownerships from sourceAccessControlStore
        final Set<OwnershipAnnotation> ownershipsToTryToImport =
                replaceSourceOwnershipReferencesToUsersAndGroups(sourceAccessControlStore, userMap, userGroupMap);
        mergePreferences(sourceUserStore, userMap);
        mergeOwnerships(sourceAccessControlStore, ownershipsToTryToImport);
        mergeAccessControlLists(sourceAccessControlStore);
    }

    private Map<User, User> markUsersForAddMergeOrDrop(UserStore sourceUserStore, AccessControlStore sourceAccessControlStore) {
        final Map<User, User> userMap = new HashMap<>();
        for (final User user : sourceUserStore.getUsers()) {
            final User targetUserWithEqualName = targetUserStore.getUserByName(user.getName());
            if (targetUserWithEqualName != null) {
                if (Util.equalsWithNull(user.getEmail(), targetUserWithEqualName.getEmail())) {
                    logger.info("Found user "+user.getName()+" in target having equal e-mail address "+user.getEmail()+
                            ". Marking for merge.");
                    userMap.put(user, targetUserWithEqualName);
                } else {
                    logger.info("Found user " + user.getName() + " in target, but e-mail addresses " + user.getEmail()
                            + " and " + targetUserWithEqualName.getEmail() + " don't match. Dropping.");
                    // remove all permission and role association's ownerships and ACLs:
                    for (final Role role : user.getRoles()) {
                        removeRoleAssociationOwnershipAndACL(role, user, sourceAccessControlStore);
                    }
                    for (final WildcardPermission permission : user.getPermissions()) {
                        removePermissionAssociationOwnershipAndACL(permission, user, sourceAccessControlStore);
                    }
                    // remove ownership and ACL for the user object itself so nothing of that is imported into the target
                    sourceAccessControlStore.removeOwnership(user.getIdentifier());
                    sourceAccessControlStore.removeAccessControlList(user.getIdentifier());
                }
            } else {
                logger.info("User "+user.getName()+" not found in target. Marking for adding.");
                userMap.put(user, user);
            }
        }
        return userMap;
    }

    /**
     * Operates on the yet unmodified groups where user references have not yet been replaced. The modifications that
     * will later be applied to the source groups are described by the {@code userMap} which tells whether source users
     * will be added to the target, merged with a target user, or dropped. For groups dropped, ownership and ACL
     * information will be removed from the {@code sourceAccessControlStore} so it doesn't get imported into the target
     * 
     * @return the mapping of source user groups to target user groups; no mapping for source groups to be dropped;
     *         mapping key to itself means "add," mapping source group to target group means "merge."
     */
    private Map<UserGroup, UserGroup> markUserGroupsForAddMergeOrDrop(UserStore sourceUserStore,
            AccessControlStore sourceAccessControlStore, Map<User, User> userMap) {
        final Map<UserGroup, UserGroup> userGroupMap = new HashMap<>();
        for (final UserGroup sourceGroup : sourceUserStore.getUserGroups()) {
            final UserGroup targetGroupWithSameID = targetUserStore.getUserGroup(sourceGroup.getId());
            if (targetGroupWithSameID != null) {
                logger.info("Identical target group found: "+targetGroupWithSameID+". Marking for merge.");
                userGroupMap.put(sourceGroup, targetGroupWithSameID);
            } else {
                final UserGroup targetGroupWithEqualName = targetUserStore.getUserGroupByName(sourceGroup.getName());
                if (targetGroupWithEqualName != null) {
                    if (considerGroupsIdentical(targetGroupWithEqualName, sourceGroup, userMap)) {
                        logger.info("Identical target group (though different ID) found: "+targetGroupWithEqualName+". Merging...");
                        userGroupMap.put(sourceGroup, targetGroupWithEqualName);
                    } else {
                        logger.warning("Found existing target user group "+targetGroupWithEqualName+" but source group "+
                                sourceGroup+" is not considered identical. Dropping.");
                        // remove ownership and ACL information for dropped group so it doesn't get imported into target:
                        sourceAccessControlStore.removeOwnership(sourceGroup.getIdentifier());
                        sourceAccessControlStore.removeAccessControlList(sourceGroup.getIdentifier());
                    }
                } else {
                    logger.info("No target user group found for source group "+sourceGroup+". Marking for adding");
                    userGroupMap.put(sourceGroup, sourceGroup);
                }
            }
        }
        return userGroupMap;
    }

    private void replaceSourceUserReferencesToUsersAndGroups(Map<User, User> userMap,
            Map<UserGroup, UserGroup> userGroupMap, AccessControlStore sourceAccessControlStore) {
        for (final User sourceUser : userMap.keySet()) {
            final Set<Role> rolesToRemoveBecauseOfLostOrMissingQualifier = new HashSet<>();
            final Map<Role, Role> rolesToReplaceDueToChangingQualifierObject = new HashMap<>();
            for (final Role role : sourceUser.getRoles()) {
                final User qualifiedForUser = role.getQualifiedForUser();
                final UserGroup qualifiedForGroup = role.getQualifiedForTenant();
                final RoleDefinition targetRoleDefinition = targetUserStore.getRoleDefinition(role.getRoleDefinition().getId());
                if (qualifiedForUser == null && qualifiedForGroup == null) {
                    logger.severe("Dropping unqualified role "+role+" from user "+sourceUser.getName());
                    rolesToRemoveBecauseOfLostOrMissingQualifier.add(role);
                } else if (targetRoleDefinition == null) {
                    logger.severe("Dropping role "+role+" from user "+sourceUser.getName()+
                            " because the role definition with ID "+
                            role.getRoleDefinition().getIdAsString()+" was not found in target user store");
                } else {
                    final User userQualifierInTarget;
                    final UserGroup groupQualifierInTarget;
                    if (qualifiedForUser != null) {
                        userQualifierInTarget = userMap.get(qualifiedForUser);
                        if (userQualifierInTarget == null) {
                            logger.severe("User qualifying role "+role+" for user "+sourceUser.getName()+" will be dropped. Dropping role.");
                            rolesToRemoveBecauseOfLostOrMissingQualifier.add(role);
                        }
                    } else {
                        userQualifierInTarget = null;
                    }
                    if (qualifiedForGroup != null) {
                        groupQualifierInTarget = userGroupMap.get(qualifiedForGroup);
                        if (groupQualifierInTarget == null) {
                            logger.severe("Group qualifying role "+role+" for user "+sourceUser.getName()+" will be dropped. Dropping role.");
                            rolesToRemoveBecauseOfLostOrMissingQualifier.add(role);
                        }
                    } else {
                        groupQualifierInTarget = null;
                    }
                    if (!rolesToRemoveBecauseOfLostOrMissingQualifier.contains(role) &&
                            (qualifiedForUser != userQualifierInTarget || qualifiedForGroup != groupQualifierInTarget)) {
                        logger.info("Qualifying user/group for role "+role+" on user "+sourceUser.getName()+
                                " merged to target. Updating role");
                        rolesToReplaceDueToChangingQualifierObject.put(role, new Role(targetRoleDefinition,
                                groupQualifierInTarget, userQualifierInTarget, role.isTransitive()));
                    }
                }
            }
            for (final Role roleToRemove : rolesToRemoveBecauseOfLostOrMissingQualifier) {
                sourceUser.removeRole(roleToRemove);
                // also remove ownership/ACL information for the corresponding role association:
                removeRoleAssociationOwnershipAndACL(roleToRemove, sourceUser, sourceAccessControlStore);
            }
            for (final Entry<Role, Role> e : rolesToReplaceDueToChangingQualifierObject.entrySet()) {
                // the role may change its security ID by replacing the group ID; therefore, we need to
                // move the ownership / ACL information from old to new; redundant if only the user was
                // replaced because the username would remain unchanged.
                final QualifiedObjectIdentifier idOfOldRoleAssociation = SecuredSecurityTypes.ROLE_ASSOCIATION
                        .getQualifiedObjectIdentifier(PermissionAndRoleAssociation.get(e.getKey(), sourceUser));
                final OwnershipAnnotation oldRoleAssociationOwnership = sourceAccessControlStore.getOwnership(idOfOldRoleAssociation);
                final AccessControlListAnnotation oldRoleAssociationACL = sourceAccessControlStore.getAccessControlList(idOfOldRoleAssociation);
                sourceUser.removeRole(e.getKey());
                sourceAccessControlStore.removeOwnership(idOfOldRoleAssociation);
                sourceAccessControlStore.removeAccessControlList(idOfOldRoleAssociation);
                sourceUser.addRole(e.getValue());
                // now apply the copied ownership / ACL information to the new role association:
                final QualifiedObjectIdentifier idOfNewRoleAssociation = SecuredSecurityTypes.ROLE_ASSOCIATION
                    .getQualifiedObjectIdentifier(PermissionAndRoleAssociation.get(e.getValue(), sourceUser));
                if (oldRoleAssociationOwnership != null) {
                    sourceAccessControlStore.setOwnership(idOfNewRoleAssociation,
                            oldRoleAssociationOwnership.getAnnotation().getUserOwner(),
                            oldRoleAssociationOwnership.getAnnotation().getTenantOwner(),
                            oldRoleAssociationOwnership.getDisplayNameOfAnnotatedObject());
                }
                if (oldRoleAssociationACL != null) {
                    for (final Entry<UserGroup, Set<String>> permissionMap : oldRoleAssociationACL.getAnnotation().getActionsByUserGroup().entrySet()) {
                        sourceAccessControlStore.setAclPermissions(idOfNewRoleAssociation, permissionMap.getKey(), permissionMap.getValue());
                    }
                }
            }
        }
    }
    
    private void removeRoleAssociationOwnershipAndACL(Role roleToRemove, User sourceUser, AccessControlStore sourceAccessControlStore) {
        final QualifiedObjectIdentifier idOfRoleAssociation = SecuredSecurityTypes.ROLE_ASSOCIATION
                .getQualifiedObjectIdentifier(PermissionAndRoleAssociation.get(roleToRemove, sourceUser));
        sourceAccessControlStore.removeOwnership(idOfRoleAssociation);
        sourceAccessControlStore.removeAccessControlList(idOfRoleAssociation);
    }

    private void replaceSourceUserGroupReferencesToUsers(Map<User, User> userMap,
            Map<UserGroup, UserGroup> userGroupMap) {
        // two passes to avoid ConcurrentModificationException
        for (final UserGroup sourceUserGroup : userGroupMap.keySet()) {
            final Map<User, User> mapping = new HashMap<>();
            for (final User user : sourceUserGroup.getUsers()) {
                mapping.put(user, userMap.get(user));
            }
            for (final Entry<User, User> e : mapping.entrySet()) {
                if (e.getKey() != e.getValue()) {
                    sourceUserGroup.remove(e.getKey());
                }
                if (e.getValue() == null) {
                    logger.severe("User "+e.getKey().getName()+" from group "+sourceUserGroup.getName()+" dropped. Removing from group.");
                } else if (e.getValue() != e.getKey()) {
                    logger.info("User " + e.getKey().getName() + " from group " + sourceUserGroup.getName()
                            + " merging into target's user " + e.getValue().getName() + ". Updating group.");
                    sourceUserGroup.add(e.getValue());
                }
            }
        }
    }
    
    private Set<OwnershipAnnotation> replaceSourceOwnershipReferencesToUsersAndGroups(AccessControlStore sourceAccessControlStore,
            Map<User, User> userMap, Map<UserGroup, UserGroup> userGroupMap) {
        final Set<OwnershipAnnotation> result = new HashSet<>();
        for (final OwnershipAnnotation sourceOwnership : sourceAccessControlStore.getOwnerships()) {
            final UserGroup groupOwnership = sourceOwnership.getAnnotation().getTenantOwner();
            final User userOwnership = sourceOwnership.getAnnotation().getUserOwner();
            final UserGroup targetGroupOwnership = userGroupMap.get(groupOwnership);
            final User targetUserOwnership = userMap.get(userOwnership);
            if (targetGroupOwnership != null || targetUserOwnership != null) {
                if (targetGroupOwnership != groupOwnership || targetUserOwnership != userOwnership) {
                    // something changed, and at least one ownership component is not null; create new annotation:
                    logger.info("User/group of ownership for object "+sourceOwnership.getIdOfAnnotatedObject()+" changed. Ownership updated");
                    result.add(new OwnershipAnnotation(new Ownership(targetUserOwnership, targetGroupOwnership),
                            sourceOwnership.getIdOfAnnotatedObject(), sourceOwnership.getDisplayNameOfAnnotatedObject()));
                } else {
                    result.add(sourceOwnership);
                }
            } else {
                logger.info("Ownership's group or user dropped. Not importing ownership for object with ID "
                        + sourceOwnership.getIdOfAnnotatedObject());
            }
        }
        return result;
    }
    
    private void replaceSourceAccessControlListReferencesToGroups(AccessControlStore sourceAccessControlStore,
            Map<UserGroup, UserGroup> userGroupMap) {
        for (final AccessControlListAnnotation aclAnnotation : sourceAccessControlStore.getAccessControlLists()) {
            final Map<UserGroup, Set<String>> actionsByUserGroup = aclAnnotation.getAnnotation().getActionsByUserGroup();
            final Set<UserGroup> groupsInAcl = new HashSet<>(actionsByUserGroup.keySet());
            for (final UserGroup sourceGroup : groupsInAcl) {
                final Set<String> actionsForGroup = new HashSet<>(actionsByUserGroup.get(sourceGroup));
                final UserGroup targetGroup = userGroupMap.get(sourceGroup);
                if (sourceGroup != null && targetGroup == null) {
                    // drop; check that we don't grow permissions:
                    for (final String action : actionsForGroup) {
                        if (action.startsWith("!")) {
                            throw new IllegalStateException("Denying ACL permission "+action
                                    +" would be dropped because group "+sourceGroup.getName()
                                    +" to which it applies will be dropped. Therefore, users who belonged to this group"
                                    +" could accidentally receive this permission in the target. Aborting!");
                        }
                        aclAnnotation.getAnnotation().removePermission(sourceGroup, action);
                    }
                } else if (targetGroup != sourceGroup) {
                    // replace source group by target group:
                    logger.info("Replacing group "+sourceGroup.getName()+" by merged target group for ACL on "+
                            aclAnnotation.getDisplayNameOfAnnotatedObject()+" with ID "+aclAnnotation.getIdOfAnnotatedObject());
                    for (final String action : actionsForGroup) {
                        aclAnnotation.getAnnotation().removePermission(sourceGroup, action);
                        aclAnnotation.getAnnotation().addPermission(targetGroup, action);
                    }
                }
            }
        }
    }

    private void mergeUsersAndGroups(UserStore sourceUserStore, Map<User, User> userMap,
            Map<UserGroup, UserGroup> userGroupMap, AccessControlStore sourceAccessControlStore) throws UserGroupManagementException, UserManagementException {
        for (final UserGroup sourceGroup : sourceUserStore.getUserGroups()) {
            final UserGroup targetGroup = userGroupMap.get(sourceGroup);
            if (targetGroup != null) {
                if (targetGroup == sourceGroup) {
                    // places the existing source group into the target user store
                    targetUserStore.addUserGroup(targetGroup);
                } else {
                    mergeSecondUserGroupIntoFirst(targetGroup, sourceGroup);
                }
            } // else  drop
        }
        for (final User sourceUser : sourceUserStore.getUsers()) {
            final User targetUser = userMap.get(sourceUser);
            if (targetUser != null) {
                if (targetUser == sourceUser) {
                    // places the existing user into the target user store
                    targetUserStore.addUser(targetUser);
                } else {
                    mergeSecondUserIntoFirst(targetUser, sourceUser, userGroupMap, sourceAccessControlStore);
                }
            } // else drop
        }
    }
    
    private void mergeSecondUserGroupIntoFirst(UserGroup targetGroup, UserGroup sourceGroup) {
        for (final User sourceUser : sourceGroup.getUsers()) {
            boolean updated = false;
            if (!targetGroup.contains(sourceUser)) {
                logger.info("Adding user "+sourceUser.getName()+" to merged group "+targetGroup.getName());
                targetGroup.add(sourceUser);
                updated = true;
            }
            for (final Entry<RoleDefinition, Boolean> e : sourceGroup.getRoleDefinitionMap().entrySet()) {
                final Boolean roleAssociationInTargetGroup = targetGroup.getRoleAssociation(e.getKey());
                updated = 
                    !Util.equalsWithNull(targetGroup.put(e.getKey(),
                            (roleAssociationInTargetGroup==null?false:roleAssociationInTargetGroup) || e.getValue()),
                            roleAssociationInTargetGroup)
                    || updated;
            }
            if (updated) {
                targetUserStore.updateUserGroup(targetGroup);
            }
        }
    }

    private void mergeSecondUserIntoFirst(User targetUser, User sourceUser, Map<UserGroup, UserGroup> userGroupMap, AccessControlStore sourceAccessControlStore) throws UserManagementException {
        assert Util.equalsWithNull(targetUser.getEmail(), sourceUser.getEmail());
        for (final Role role : sourceUser.getRoles()) {
            if (role.getQualifiedForTenant() == null && role.getQualifiedForUser() == null) {
                // such roles should have been dropped from the source user already during "phase 2"
                throw new InternalError("Adding an unqualified role "+role+" to user "+targetUser.getName()+" forbidden.");
            } else {
                if (targetUserStore.getRoleDefinition(role.getRoleDefinition().getId()) == null) {
                    // such roles should have been dropped from the source user already during "phase 2"
                    throw new InternalError("Role definition for role "+role+" not found in target user store. "+
                            "Dropping role from user "+sourceUser.getName());
                } else {
                    logger.info("Adding role "+role+" to target user "+targetUser.getName());
                    targetUserStore.addRoleForUser(targetUser.getName(), role);
                }
            }
        }
        for (final WildcardPermission permission : sourceUser.getPermissions()) {
            if (Util.isEmpty(permission.getQualifiedObjectIdentifiers())) {
                logger.severe("Dropping unqualified permission "+permission+" for user "+sourceUser.getName());
                // make sure that the permission association's ownership/ACL are not copied to target:
                removePermissionAssociationOwnershipAndACL(permission, sourceUser, sourceAccessControlStore);
            } else {
                logger.info("Adding qualified permission "+permission+" to target user "+targetUser.getName());
                targetUserStore.addPermissionForUser(targetUser.getName(), permission);
            }
        }
        boolean updated = false;
        if (!targetUser.isEmailValidated() && sourceUser.isEmailValidated()) {
            final String validationSecret;
            if (targetUser.getValidationSecret() == null) {
                validationSecret = targetUser.createRandomSecret();
                targetUser.startEmailValidation(validationSecret);
            } else {
                validationSecret = targetUser.getValidationSecret();
            }
            logger.info("Validating e-mail address "+targetUser.getEmail()+" of target user "+targetUser.getName()+
                    " because it was validated successfully on the source side");
            targetUser.validate(validationSecret);
            updated = true;
        }
        updated = copyNonNullValue(sourceUser.getCompany(), targetUser.getCompany(), targetUser::setCompany);
        updated = copyNonNullValue(sourceUser.getFullName(), targetUser.getFullName(), targetUser::setFullName);
        updated = copyNonNullValue(sourceUser.getLocale(), targetUser.getLocale(), targetUser::setLocale);
        updated = mergeDefaultCreationGroups(targetUser, sourceUser, userGroupMap) || updated;
        if (updated) {
            targetUserStore.updateUser(targetUser);
        }
    }

    private void removePermissionAssociationOwnershipAndACL(final WildcardPermission permission,
            User sourceUser, AccessControlStore sourceAccessControlStore) {
        final QualifiedObjectIdentifier idOfOldPermissionAssociation = SecuredSecurityTypes.PERMISSION_ASSOCIATION
                .getQualifiedObjectIdentifier(PermissionAndRoleAssociation.get(permission, sourceUser));
        sourceAccessControlStore.removeOwnership(idOfOldPermissionAssociation);
        sourceAccessControlStore.removeAccessControlList(idOfOldPermissionAssociation);
    }

    private boolean mergeDefaultCreationGroups(User targetUser, User sourceUser, Map<UserGroup, UserGroup> userGroupMap) {
        boolean updated = false;
        for (final Entry<String, UserGroup> e : sourceUser.getDefaultTenantMap().entrySet()) {
            if (targetUser.getDefaultTenant(e.getKey()) == null) {
                final UserGroup mappedDefaultCreationGroup = userGroupMap.get(e.getValue());
                if (mappedDefaultCreationGroup != null) {
                    targetUser.setDefaultTenant(mappedDefaultCreationGroup, e.getKey());
                    updated = true;
                } else {
                    logger.warning("Default creation group "+e.getValue().getName()+" for user "+targetUser.getName()+
                            " on server "+e.getKey()+" not merged because that groups was dropped.");
                }
            }
        }
        return updated;
    }

    /**
     * If the {@code sourceValue} is a non-{@code null} value, and {@code targetValue} is a {@code null}
     * value, the {@code setterOnTargetUser} is used to copy the {@code sourceValue} to the target user.
     * In this case, {@code true} is returned.
     * 
     * @return {@code true} if and only if the {@code setter} was called to update the {@code targetUser}
     */
    private <T> boolean copyNonNullValue(T sourceValue, T targetValue, Consumer<T> setterOnTargetUser) {
        final boolean updated;
        if (sourceValue != null && targetValue == null) {
            setterOnTargetUser.accept(sourceValue);
            updated = true;
        } else {
            updated = false;
        }
        return updated;
    }

    private void mergePreferences(UserStore sourceUserStore, Map<User, User> userMap) {
        for (final Entry<User, User> e : userMap.entrySet()) {
            for (final Entry<String, String> preference : sourceUserStore.getAllPreferences(e.getKey().getName()).entrySet()) {
                if (targetUserStore.getPreference(e.getValue().getName(), preference.getKey()) == null) {
                    logger.info("Copying preference for key "+preference.getKey()+" for user "+e.getKey().getName());
                    targetUserStore.setPreference(e.getValue().getName(), preference.getKey(), preference.getValue());
                } else {
                    logger.info("Not copying preference for key "+preference.getKey()+" for user "+e.getKey().getName()+
                            " because the key for that user was already present in target");
                }
            }
        }
    }
    
    private void mergeOwnerships(AccessControlStore sourceAccessControlStore,
            Set<OwnershipAnnotation> ownershipsToTryToImport) {
        for (final OwnershipAnnotation o : ownershipsToTryToImport) {
            if (targetAccessControlStore.getOwnership(o.getIdOfAnnotatedObject()) == null) {
                targetAccessControlStore.setOwnership(o.getIdOfAnnotatedObject(), o.getAnnotation().getUserOwner(),
                        o.getAnnotation().getTenantOwner(), o.getDisplayNameOfAnnotatedObject());
            }
        }
    }

    /**
     * The source access control lists are expected to have their groups already updated to point to their corresponding
     * groups in the target store. See also {@link #replaceSourceAccessControlListReferencesToGroups(AccessControlStore, Map)}.
     */
    private void mergeAccessControlLists(AccessControlStore sourceAccessControlStore) {
        logger.info("Applying all source ACLs to target");
        for (final AccessControlListAnnotation sourceACL : sourceAccessControlStore.getAccessControlLists()) {
            for (final Entry<UserGroup, Set<String>> permissionsPerGroup : sourceACL.getAnnotation().getActionsByUserGroup().entrySet()) {
                for (final String action : permissionsPerGroup.getValue()) {
                    targetAccessControlStore.addAclPermission(sourceACL.getIdOfAnnotatedObject(), permissionsPerGroup.getKey(), action);
                }
            }
        }
    }
    
    /**
     * If the groups have equal {@link UserGroup#getId() IDs} then they are considered identical. If both groups have
     * different IDs but equal names and the names match the pattern {@code <username>-tenant} and both contain a user
     * named {@code <username>} and the source user will not be dropped (see {@code userMap}) then they will be
     * considered identical, too. In all other cases they are considered distinct.
     * 
     * @param userMap
     *            tells what happens with the users from the imported source; if not in the keys, the user will be
     *            dropped. If the value is identical to the key, the user is added. Otherwise, the value tells the
     *            equal-named user in the target with which they key source user will be merged.
     */
    static boolean considerGroupsIdentical(final UserGroup targetGroup, final UserGroup sourceGroup, Map<User, User> userMap) {
        final String targetTenantGroupUserName, sourceTenantGroupUserName;
        return targetGroup.getId().equals(sourceGroup.getId()) ||
                (targetTenantGroupUserName=getTenantGroupUserName(targetGroup)) != null &&
                (sourceTenantGroupUserName=getTenantGroupUserName(sourceGroup)) != null &&
                targetTenantGroupUserName.equals(sourceTenantGroupUserName) &&
                hasUserNamed(targetGroup, targetTenantGroupUserName) &&
                hasUserNamed(sourceGroup, sourceTenantGroupUserName) &&
                userMap.get(StreamSupport.stream(sourceGroup.getUsers().spliterator(), /* parallel */ false).
                        filter(u->u.getName().equals(sourceTenantGroupUserName)).findAny().get()) != null;
    }

    private static boolean hasUserNamed(UserGroup group, String username) {
        return Util.contains(Util.map(group.getUsers(), u->u.getName()), username);
    }

    private static final Pattern tenantUserGroupNamePattern = Pattern.compile("(.*)"+SecurityService.TENANT_SUFFIX);
    private static String getTenantGroupUserName(UserGroup g) {
        final String result;
        final Matcher matcher = tenantUserGroupNamePattern.matcher(g.getName());
        if (matcher.matches()) {
            result = matcher.group(1);
        } else {
            result = null;
        }
        return result;
    }
}
