package com.sap.sse.security.userstore.mongodb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sap.sse.common.Util;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.concurrent.RunnableWithResult;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;

public class AccessControlStoreImpl implements AccessControlStore {
    private static final long serialVersionUID = 2165649781000936074L;

    private String name = "Access control store";

    /**
     * maps from object ID string representations to the access control lists for the respective key object
     */
    private final ConcurrentHashMap<QualifiedObjectIdentifier, AccessControlListAnnotation> accessControlLists;
    
    /**
     * For quick lookup of denying ACLs during meta-permission checks (permission to grant a permission) this map
     * contains a subset of the {@link #accessControlList} map, keyed by the
     * {@link QualifiedObjectIdentifier#getTypeIdentifier() type identifier} (which is a {@link String}) of the objects
     * to which those ACLs pertain, and as a nested map, by the {@link UserGroup} that is the key in the ACL that denies
     * permission to an action (could be the {@code null} group, in that case meaning the anonymous users). Access to
     * this map, like access to {@link #accessControlLists}, has to be synchronized under the
     * {@link #lockForManagementMappings}.<p>
     * 
     * As the inner maps are implemented as non-concurrent maps, {@code null} keys are permissible, so the anonymous
     * group is actually represented as a {@code null} key.
     */
    private final ConcurrentHashMap<String, Map<UserGroup, Set<QualifiedObjectIdentifier>>> accessControlListsWithDenials;

    /**
     * maps from object ID string representations to the ownership information for the respective key object
     */
    private final ConcurrentHashMap<QualifiedObjectIdentifier, OwnershipAnnotation> ownerships;

    /**
     * These are a set of helper mappings plus the corresponding lock, they are used to allow fast lockups of
     * ownerships, without the need to iterate over all objects of a type and without issues with concurrent updates
     * (prevented by the lock)
     */
    private final ConcurrentHashMap<User, Set<OwnershipAnnotation>> userToOwnership;
    
    /**
     * The anonymous {@code null} group is represented by {@link #NULL_GROUP} instead as a {@link ConcurrentHashMap}
     * cannot handle {@code null} keys/values.
     */
    private final ConcurrentHashMap<UserGroup, Set<OwnershipAnnotation>> userGroupToOwnership;

    /**
     * The anonymous {@code null} group is represented by {@link #NULL_GROUP} instead as a {@link ConcurrentHashMap}
     * cannot handle {@code null} keys/values.
     */
    private final ConcurrentHashMap<UserGroup, Set<AccessControlListAnnotation>> userGroupToAccessControlListAnnotation;

    private static final UserGroupImpl NULL_GROUP = new UserGroupImpl(null, "<null group>");
    /**
     * Currently this lock is somewhat coarse, it could be optimized a little, by being split for ACL and Ownership if
     * profiling deems this necessary
     */
    private final NamedReentrantReadWriteLock lockForManagementMappings;

    /**
     * Won't be serialized and remains <code>null</code> on the de-serializing end.
     */
    private final transient MongoObjectFactory mongoObjectFactory;

    /**
     * Won't be serialized and remains <code>null</code> on the de-serializing end.
     */
    private final transient DomainObjectFactory domainObjectFactory;

    /**
     * Won't be serialized and remains <code>null</code> on the de-serializing end.
     */
    private final transient UserStore userStore;

    public AccessControlStoreImpl(UserStore userStore) {
        this(PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), userStore);
    }

    public AccessControlStoreImpl(final DomainObjectFactory domainObjectFactory,
            final MongoObjectFactory mongoObjectFactory, final UserStore userStore) {
        accessControlLists = new ConcurrentHashMap<>();
        accessControlListsWithDenials = new ConcurrentHashMap<>();
        ownerships = new ConcurrentHashMap<>();
        userToOwnership = new ConcurrentHashMap<>();
        userGroupToOwnership = new ConcurrentHashMap<>();
        userGroupToAccessControlListAnnotation = new ConcurrentHashMap<>();
        lockForManagementMappings = new NamedReentrantReadWriteLock("ownershipLock", true);
        this.mongoObjectFactory = mongoObjectFactory;
        this.domainObjectFactory = domainObjectFactory;
        this.userStore = userStore;
    }
    
    @Override
    public void loadACLsAndOwnerships() {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                if (domainObjectFactory != null) {
                    for (AccessControlListAnnotation acl : domainObjectFactory.loadAllAccessControlLists(userStore)) {
                        internalAddACL(acl);
                    }
                    for (OwnershipAnnotation ownership : domainObjectFactory.loadAllOwnerships(userStore)) {
                        internalSetOwnershipAndMapUserAndUserGroupToOwnership(ownership);
                    }
                }
            }
        });
    }

    private void internalAddACL(AccessControlListAnnotation acl) {
        assert lockForManagementMappings.isWriteLockedByCurrentThread();
        accessControlLists.put(acl.getIdOfAnnotatedObject(), acl);
        for (UserGroup owner : acl.getAnnotation().getActionsByUserGroup().keySet()) {
            internalMapUserGroupToACL(owner, acl);
        }
    }

    @Override
    public Iterable<AccessControlListAnnotation> getAccessControlLists() {
        return LockUtil.executeWithReadLockAndResult(lockForManagementMappings,
                new RunnableWithResult<Iterable<AccessControlListAnnotation>>() {
                    @Override
                    public Iterable<AccessControlListAnnotation> run() {
                        return new ArrayList<>(accessControlLists.values());
                    }
                });
    }

    @Override
    public AccessControlListAnnotation getAccessControlList(
            final QualifiedObjectIdentifier idOfAccessControlledObjectAsString) {
        return LockUtil.executeWithReadLockAndResult(lockForManagementMappings,
                new RunnableWithResult<AccessControlListAnnotation>() {
                    @Override
                    public AccessControlListAnnotation run() {
                        return accessControlLists.get(idOfAccessControlledObjectAsString);
                    }
                });
    }

    @Override
    public AccessControlListAnnotation setEmptyAccessControlList(
            final QualifiedObjectIdentifier idOfAccessControlledObject,
            final String displayNameOfAccessControlledObject) {
        return LockUtil.executeWithWriteLockAndResult(lockForManagementMappings,
                new RunnableWithResult<AccessControlListAnnotation>() {
                    @Override
                    public AccessControlListAnnotation run() {
                        removeAccessControlList(idOfAccessControlledObject);
                        AccessControlListAnnotation acl = new AccessControlListAnnotation(new AccessControlList(),
                                idOfAccessControlledObject, displayNameOfAccessControlledObject);
                        accessControlLists.put(idOfAccessControlledObject, acl);
                        mongoObjectFactory.storeAccessControlList(acl);
                        return acl;
                    }
                });
    }

    @Override
    public void setAclPermissions(final QualifiedObjectIdentifier idOfAccessControlledObject, final UserGroup userGroup,
            final Set<String> actions) {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                AccessControlListAnnotation acl = getOrCreateAclInternal(idOfAccessControlledObject);
                acl.getAnnotation().setPermissions(userGroup, actions);
                mongoObjectFactory.storeAccessControlList(acl);
                internalMapUserGroupToACL(userGroup, acl);
            }
        });
    }

    private AccessControlListAnnotation getOrCreateAclInternal(QualifiedObjectIdentifier idOfAccessControlledObject) {
        assert lockForManagementMappings.isWriteLockedByCurrentThread();
        return accessControlLists.computeIfAbsent(idOfAccessControlledObject,
                id->new AccessControlListAnnotation(new AccessControlList(), id, /* display name */ null));
    }
    
    @Override
    public AccessControlListAnnotation getOrCreateAcl(QualifiedObjectIdentifier idOfAccessControlledObject) {
        return LockUtil.executeWithWriteLockAndResult(lockForManagementMappings, () -> {
            return getOrCreateAclInternal(idOfAccessControlledObject);
        });
    }
    

    @Override
    public void addAclPermission(final QualifiedObjectIdentifier idOfAccessControlledObject, final UserGroup userGroup,
            final String action) {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                AccessControlListAnnotation acl = getOrCreateAclInternal(idOfAccessControlledObject);
                acl.getAnnotation().addPermission(userGroup, action);
                internalMapUserGroupToACL(userGroup, acl);
                mongoObjectFactory.storeAccessControlList(acl);
            }
        });
    }

    @Override
    public void removeAclPermission(final QualifiedObjectIdentifier idOfAccessControlledObjectAsString,
            final UserGroup userGroup, final String action) {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                AccessControlListAnnotation acl = getOrCreateAclInternal(idOfAccessControlledObjectAsString);
                if (acl.getAnnotation().removePermission(userGroup, action)) {
                    internalRemoveUserGroupToACLMapping(userGroup, acl);
                    mongoObjectFactory.storeAccessControlList(acl);
                }
            }
        });
    }

    private void internalMapUserGroupToACL(final UserGroup userGroup, final AccessControlListAnnotation acl) {
        if (!lockForManagementMappings.isWriteLockedByCurrentThread()) {
            throw new IllegalStateException("Current thread has no write lock!");
        }
        final UserGroup effectiveUserGroup = userGroup == null ? NULL_GROUP : userGroup;
        Set<AccessControlListAnnotation> currentACLsContainingGroup = userGroupToAccessControlListAnnotation
                .get(effectiveUserGroup);
        if (currentACLsContainingGroup == null) {
            currentACLsContainingGroup = Collections
                    .newSetFromMap(new ConcurrentHashMap<AccessControlListAnnotation, Boolean>());
            userGroupToAccessControlListAnnotation.put(effectiveUserGroup, currentACLsContainingGroup);
        }
        currentACLsContainingGroup.add(acl);
        final String type = acl.getIdOfAnnotatedObject().getTypeIdentifier();
        // FIXME bug5239: add if an action is denied; remove if no action denied for group
        Map<UserGroup, Set<QualifiedObjectIdentifier>> aclsByGroupForType = accessControlListsWithDenials.get(type);
        final Set<String> deniedActions = acl.getAnnotation().getDeniedActions(userGroup);
        if (deniedActions == null || deniedActions.isEmpty()) {
            if (aclsByGroupForType != null) {
                Util.removeFromValueSet(aclsByGroupForType, userGroup, acl.getIdOfAnnotatedObject());
            }
        } else {
            if (aclsByGroupForType == null) {
                aclsByGroupForType = new HashMap<>();
                accessControlListsWithDenials.put(type, aclsByGroupForType);
                Util.addToValueSet(aclsByGroupForType, userGroup, acl.getIdOfAnnotatedObject());
            }
        }
    }

    private void internalRemoveUserGroupToACLMapping(final UserGroup userGroup,
            final AccessControlListAnnotation acl) {
        if (!lockForManagementMappings.isWriteLockedByCurrentThread()) {
            throw new IllegalStateException("Current thread has no write lock!");
        }
        final UserGroup effectiveUserGroup = userGroup == null ? NULL_GROUP : userGroup;
        Set<AccessControlListAnnotation> currentACLsContainingGroup = userGroupToAccessControlListAnnotation
                .get(effectiveUserGroup);
        if (currentACLsContainingGroup != null) {
            currentACLsContainingGroup.remove(acl);
            if (currentACLsContainingGroup.isEmpty()) {
                userGroupToAccessControlListAnnotation.remove(effectiveUserGroup);
            }
        }
        final String typeIdentifier = acl.getIdOfAnnotatedObject().getTypeIdentifier();
        final Map<UserGroup, Set<QualifiedObjectIdentifier>> aclsByGroupForEvent = accessControlListsWithDenials.get(typeIdentifier);
        if (aclsByGroupForEvent != null) {
            aclsByGroupForEvent.remove(userGroup);
            if (aclsByGroupForEvent.isEmpty()) {
                accessControlListsWithDenials.remove(typeIdentifier);
            }
        }
    }

    @Override
    public void removeAccessControlList(final QualifiedObjectIdentifier idOfAccessControlledObject) {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                final AccessControlListAnnotation acl = accessControlLists.remove(idOfAccessControlledObject);
                if (acl != null) {
                    for (UserGroup userGroup : acl.getAnnotation().getActionsByUserGroup().keySet()) {
                        internalRemoveUserGroupToACLMapping(userGroup, acl);
                    }
                    mongoObjectFactory.deleteAccessControlList(idOfAccessControlledObject, acl.getAnnotation());
                }
            }
        });
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public OwnershipAnnotation setOwnership(final QualifiedObjectIdentifier id, final User userOwnerName,
            final UserGroup tenantOwner, String displayNameOfOwnedObject) {
        final OwnershipAnnotation ownership = new OwnershipAnnotation(new Ownership(userOwnerName, tenantOwner), id,
                displayNameOfOwnedObject);
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                // first removing it, prevents the necessity to do a delta update
                removeUserAndUserGroupToOwnershipMapping(ownership);
                // and add it to the new ownership
                internalSetOwnershipAndMapUserAndUserGroupToOwnership(ownership);
            }
        });
        // and that it is finally written
        mongoObjectFactory.storeOwnership(ownership);
        return ownership;
    }

    private void internalSetOwnershipAndMapUserAndUserGroupToOwnership(final OwnershipAnnotation ownership) {
        ownerships.put(ownership.getIdOfAnnotatedObject(), ownership);
        UserGroup tenantOwner = ownership.getAnnotation().getTenantOwner();
        if (tenantOwner != null) {
            Set<OwnershipAnnotation> currentGroupOwnerships = userGroupToOwnership.get(tenantOwner);
            if (currentGroupOwnerships == null) {
                currentGroupOwnerships = Collections
                        .newSetFromMap(new ConcurrentHashMap<OwnershipAnnotation, Boolean>());
                userGroupToOwnership.put(tenantOwner, currentGroupOwnerships);
            }
            currentGroupOwnerships.add(ownership);
        }
        User userOwnerName = ownership.getAnnotation().getUserOwner();
        if (userOwnerName != null) {
            Set<OwnershipAnnotation> currentUserOwnerships = userToOwnership.get(userOwnerName);
            if (currentUserOwnerships == null) {
                currentUserOwnerships = Collections
                        .newSetFromMap(new ConcurrentHashMap<OwnershipAnnotation, Boolean>());
                userToOwnership.put(userOwnerName, currentUserOwnerships);
            }
            currentUserOwnerships.add(ownership);
        }
    }

    @Override
    public void removeOwnership(final QualifiedObjectIdentifier id) {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                OwnershipAnnotation ownership = ownerships.remove(id);
                if (ownership != null) {
                    removeUserAndUserGroupToOwnershipMapping(ownership);
                    mongoObjectFactory.deleteOwnership(id, ownership.getAnnotation());
                }
            }
        });
    }

    private void removeUserAndUserGroupToOwnershipMapping(OwnershipAnnotation ownership) {
        UserGroup userGroupKey = ownership.getAnnotation().getTenantOwner();
        if (userGroupKey != null) {
            Set<OwnershipAnnotation> currentGroupOwnerships = userGroupToOwnership.get(userGroupKey);
            if (currentGroupOwnerships != null) {
                currentGroupOwnerships.remove(ownership);
                if (currentGroupOwnerships.isEmpty()) {
                    // no more entries, remove entry
                    userGroupToOwnership.remove(ownership.getAnnotation().getTenantOwner());
                }
            }
        }
        User userKey = ownership.getAnnotation().getUserOwner();
        if (userKey != null) {
            Set<OwnershipAnnotation> currentUserOwnerships = userToOwnership
                    .get(ownership.getAnnotation().getUserOwner());
            if (currentUserOwnerships != null) {
                currentUserOwnerships.remove(ownership);
                if (currentUserOwnerships.isEmpty()) {
                    // no more entries, remove entry
                    userToOwnership.remove(ownership.getAnnotation().getUserOwner());
                }
            }
        }
    }
    
    @Override
    public Iterable<OwnershipAnnotation> getOwnerhipsWithGroupOwner(UserGroup owningUserGroup) {
        final Set<OwnershipAnnotation> ownerships = userGroupToOwnership.get(owningUserGroup);
        return ownerships==null ? Collections.emptySet() : Collections.unmodifiableCollection(ownerships);
    }

    @Override
    public OwnershipAnnotation getOwnership(final QualifiedObjectIdentifier idOfOwnedObjectAsString) {
        return LockUtil.executeWithReadLockAndResult(lockForManagementMappings,
                new RunnableWithResult<OwnershipAnnotation>() {
                    @Override
                    public OwnershipAnnotation run() {
                        return ownerships.get(idOfOwnedObjectAsString);
                    }
                });
    }

    @Override
    public Iterable<OwnershipAnnotation> getOwnerships() {
        return LockUtil.executeWithReadLockAndResult(lockForManagementMappings,
                new RunnableWithResult<Iterable<OwnershipAnnotation>>() {
                    @Override
                    public Iterable<OwnershipAnnotation> run() {
                        return new ArrayList<>(ownerships.values());
                    }
                });
    }

    @Override
    public void clear() {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                mongoObjectFactory.deleteAllOwnerships();
                mongoObjectFactory.deleteAllAccessControlLists();
                removeAll();
            }
        });
    }

    private void removeAll() {
        accessControlLists.clear();
        accessControlListsWithDenials.clear();
        ownerships.clear();
        userGroupToAccessControlListAnnotation.clear();
        userGroupToOwnership.clear();
        userToOwnership.clear();
    }

    @Override
    public void replaceContentsFrom(final AccessControlStore newAccessControlStore) {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                clear();
                for (AccessControlListAnnotation acl : newAccessControlStore.getAccessControlLists()) {
                    internalAddACL(acl);
                }
                for (OwnershipAnnotation ownership : newAccessControlStore.getOwnerships()) {
                    internalSetOwnershipAndMapUserAndUserGroupToOwnership(ownership);
                }
            }
        });
    }
    
    @Override
    public Set<AccessControlListAnnotation> getAccessControlListsForGroup(UserGroup group) {
        final Set<AccessControlListAnnotation> aclsForGroup = userGroupToAccessControlListAnnotation.get(group);
        return aclsForGroup == null ? null : Collections.unmodifiableSet(aclsForGroup);
    }

    @Override
    public Map<UserGroup, Set<QualifiedObjectIdentifier>> getAccessControlListsWithDenials(String typeIdentifier) {
        final Map<UserGroup, Set<QualifiedObjectIdentifier>> aclsForType = accessControlListsWithDenials.get(typeIdentifier);
        return aclsForType == null ? null : Collections.unmodifiableMap(aclsForType);
    }

    @Override
    public void removeAllOwnershipsFor(final UserGroup userGroup) {
        final UserGroup effectiveUserGroup = userGroup == null ? NULL_GROUP : userGroup;
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                Set<OwnershipAnnotation> knownOwnerships = userGroupToOwnership.get(effectiveUserGroup);
                if (knownOwnerships != null) {
                    // do not use setOwnership, we know the user will not change, and we can use the more effective
                    // remove
                    // for userGroupToOwnership after deleting
                    for (OwnershipAnnotation ownership : knownOwnerships) {
                        final OwnershipAnnotation groupLessOwnership = new OwnershipAnnotation(
                                new Ownership(ownership.getAnnotation().getUserOwner(), null),
                                ownership.getIdOfAnnotatedObject(), ownership.getDisplayNameOfAnnotatedObject());
                        ownerships.put(ownership.getIdOfAnnotatedObject(), groupLessOwnership);
                        mongoObjectFactory.storeOwnership(groupLessOwnership);
                    }
                    userGroupToOwnership.remove(effectiveUserGroup);
                }
                Set<AccessControlListAnnotation> knownACLEntries = userGroupToAccessControlListAnnotation
                        .get(effectiveUserGroup);
                if (knownACLEntries != null) {
                    for (AccessControlListAnnotation acl : knownACLEntries) {
                        acl.getAnnotation().setPermissions(effectiveUserGroup, Collections.emptySet());
                        internalRemoveUserGroupToACLMapping(effectiveUserGroup, acl);
                        mongoObjectFactory.storeAccessControlList(acl);
                    }
                    userGroupToAccessControlListAnnotation.remove(effectiveUserGroup);
                    for (final Entry<String, Map<UserGroup, Set<QualifiedObjectIdentifier>>> e : accessControlListsWithDenials.entrySet()) {
                        e.getValue().remove(effectiveUserGroup);
                    }
                }
            }
        });
    }

    @Override
    public void removeAllOwnershipsFor(final User user) {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                Set<OwnershipAnnotation> knownOwnerships = userToOwnership.get(user);
                if (knownOwnerships != null) {
                    // do not use setOwnership, we know the group will not change, and we can use the more effective
                    // remove
                    // for userToOwnership after deleting
                    for (OwnershipAnnotation ownership : knownOwnerships) {
                        final OwnershipAnnotation userLessOwnership = new OwnershipAnnotation(
                                new Ownership(null, ownership.getAnnotation().getTenantOwner()),
                                ownership.getIdOfAnnotatedObject(), ownership.getDisplayNameOfAnnotatedObject());
                        ownerships.put(ownership.getIdOfAnnotatedObject(), userLessOwnership);
                        mongoObjectFactory.storeOwnership(userLessOwnership);
                    }
                    userToOwnership.remove(user);
                }
            }
        });
    }
}
