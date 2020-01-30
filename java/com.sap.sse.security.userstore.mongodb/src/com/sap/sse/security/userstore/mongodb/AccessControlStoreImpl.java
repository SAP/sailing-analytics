package com.sap.sse.security.userstore.mongodb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.LockUtil.RunnableWithResult;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
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

    // private static final Logger logger = Logger.getLogger(AccessControlStoreImpl.class.getName());

    private String name = "Access control store";

    /**
     * maps from object ID string representations to the access control lists for the respective key object
     */
    private final ConcurrentHashMap<QualifiedObjectIdentifier, AccessControlListAnnotation> accessControlLists;

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
    private final ConcurrentHashMap<UserGroup, Set<OwnershipAnnotation>> userGroupToOwnership;
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
                AccessControlListAnnotation acl = getOrCreateAcl(idOfAccessControlledObject);
                acl.getAnnotation().setPermissions(userGroup, actions);
                mongoObjectFactory.storeAccessControlList(acl);
                internalMapUserGroupToACL(userGroup, acl);
            }
        });
    }

    private AccessControlListAnnotation getOrCreateAcl(QualifiedObjectIdentifier idOfAccessControlledObject) {
        return accessControlLists.computeIfAbsent(idOfAccessControlledObject,
                id->new AccessControlListAnnotation(new AccessControlList(), id, /* display name */ null));
    }

    @Override
    public void addAclPermission(final QualifiedObjectIdentifier idOfAccessControlledObject, final UserGroup userGroup,
            final String action) {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                AccessControlListAnnotation acl = getOrCreateAcl(idOfAccessControlledObject);
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
                AccessControlListAnnotation acl = getOrCreateAcl(idOfAccessControlledObjectAsString);
                if (acl.getAnnotation().removePermission(userGroup, action)) {
                    internalRemoveUserGroupToACLMapping(userGroup, acl);
                    mongoObjectFactory.storeAccessControlList(acl);
                }
            }
        });
    }

    @Override
    public void denyAclPermission(final QualifiedObjectIdentifier idOfAccessControlledObjectAsString,
            final UserGroup userGroup, final String action) {
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                final AccessControlListAnnotation acl = getOrCreateAcl(idOfAccessControlledObjectAsString);
                if (acl.getAnnotation().denyPermission(userGroup, action)) {
                    internalMapUserGroupToACL(userGroup, acl);
                    mongoObjectFactory.storeAccessControlList(acl);
                }
            }
        });
    }

    private void internalMapUserGroupToACL(final UserGroup userGroup2, final AccessControlListAnnotation acl) {
        if (!lockForManagementMappings.isWriteLockedByCurrentThread()) {
            throw new IllegalStateException("Current thread has no write lock!");
        }

        final UserGroup userGroup = userGroup2 == null ? NULL_GROUP : userGroup2;
        Set<AccessControlListAnnotation> currentACLsContainingGroup = userGroupToAccessControlListAnnotation
                .get(userGroup);
        if (currentACLsContainingGroup == null) {
            currentACLsContainingGroup = Collections
                    .newSetFromMap(new ConcurrentHashMap<AccessControlListAnnotation, Boolean>());
            userGroupToAccessControlListAnnotation.put(userGroup, currentACLsContainingGroup);
        }
        currentACLsContainingGroup.add(acl);
    }

    private void internalRemoveUserGroupToACLMapping(final UserGroup userGroup2,
            final AccessControlListAnnotation acl) {
        if (!lockForManagementMappings.isWriteLockedByCurrentThread()) {
            throw new IllegalStateException("Current thread has no write lock!");
        }
        final UserGroup userGroup = userGroup2 == null ? NULL_GROUP : userGroup2;
        Set<AccessControlListAnnotation> currentACLsContainingGroup = userGroupToAccessControlListAnnotation
                .get(userGroup);
        if (currentACLsContainingGroup != null) {
            currentACLsContainingGroup.remove(acl);
            if (currentACLsContainingGroup.isEmpty()) {
                userGroupToAccessControlListAnnotation.remove(userGroup);
            }
        }
    }

    @Override
    public void removeAclDenial(final QualifiedObjectIdentifier idOfAccessControlledObject, final UserGroup userGroup2,
            final String action) {
        final UserGroup userGroup = userGroup2 == null ? NULL_GROUP : userGroup2;
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                final AccessControlListAnnotation acl = getOrCreateAcl(idOfAccessControlledObject);
                if (acl.getAnnotation().removeDenial(userGroup, action)) {
                    internalRemoveUserGroupToACLMapping(userGroup, acl);
                    mongoObjectFactory.storeAccessControlList(acl);
                }
            }
        });
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
                accessControlLists.clear();
                ownerships.clear();
                userGroupToAccessControlListAnnotation.clear();
                userGroupToOwnership.clear();
                userToOwnership.clear();
            }
        });
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
    public void removeAllOwnershipsFor(final UserGroup userGroup2) {
        final UserGroup userGroup = userGroup2 == null ? NULL_GROUP : userGroup2;
        LockUtil.executeWithWriteLock(lockForManagementMappings, new Runnable() {
            @Override
            public void run() {
                Set<OwnershipAnnotation> knownOwnerships = userGroupToOwnership.get(userGroup);
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
                    userGroupToOwnership.remove(userGroup);
                }

                Set<AccessControlListAnnotation> knownACLEntries = userGroupToAccessControlListAnnotation
                        .get(userGroup);
                if (knownACLEntries != null) {
                    for (AccessControlListAnnotation acl : knownACLEntries) {
                        internalRemoveUserGroupToACLMapping(userGroup, acl);
                        mongoObjectFactory.storeAccessControlList(acl);
                    }
                    userGroupToAccessControlListAnnotation.remove(userGroup);
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
