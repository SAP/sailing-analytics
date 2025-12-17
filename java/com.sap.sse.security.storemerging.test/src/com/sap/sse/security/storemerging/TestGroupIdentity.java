package com.sap.sse.security.storemerging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.impl.TimedLockImpl;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.interfaces.UserImpl;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;

public class TestGroupIdentity {
    @Test
    public void testGroupIdentityWithEqualUuid() {
        final UUID commonUuid1 = UUID.randomUUID();
        final UUID commonUuid2 = UUID.fromString(commonUuid1.toString());
        final UserGroup g1 = new UserGroupImpl(commonUuid1, "SomeName");
        final UserGroup g2 = new UserGroupImpl(commonUuid2, "SomeName");
        assertTrue(SecurityStoreMerger.considerGroupsIdentical(g1, g2, Collections.emptyMap()));
    }
    
    @Test
    public void testGroupIdentityWithDifferentUuidAndDifferentName() {
        final UUID uuid1 = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();
        final UserGroup g1 = new UserGroupImpl(uuid1, "SomeName1");
        final UserGroup g2 = new UserGroupImpl(uuid2, "SomeName2");
        assertFalse(SecurityStoreMerger.considerGroupsIdentical(g1, g2, Collections.emptyMap()));
    }
    
    @Test
    public void testGroupIdentityWithDifferentUuidEqualNonTenantName() {
        final UUID uuid1 = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();
        final UserGroup g1 = new UserGroupImpl(uuid1, "SomeName");
        final UserGroup g2 = new UserGroupImpl(uuid2, "SomeName");
        assertFalse(SecurityStoreMerger.considerGroupsIdentical(g1, g2, Collections.emptyMap()));
    }
    
    @Test
    public void testGroupIdentityWithDifferentUuidEqualTenantNameButNoCorrespondingUserInOne() {
        final UUID uuid1 = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();
        final String username = "user";
        final UserGroup g1 = new UserGroupImpl(uuid1, username+SecurityService.TENANT_SUFFIX);
        final User user = new UserImpl(username, /* email */ null, (Map<String, UserGroup>) /* defaultTenantForServer */ null, /* userGroupProvider */ null, new TimedLockImpl());
        g1.add(user);
        final UserGroup g2 = new UserGroupImpl(uuid2, username+SecurityService.TENANT_SUFFIX);
        assertFalse(SecurityStoreMerger.considerGroupsIdentical(g1, g2, Collections.emptyMap()));
    }
    
    @Test
    public void testGroupIdentityWithDifferentUuidEqualTenantNameAndCorrespondingUserInBoth() {
        final UUID uuid1 = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();
        final String username = "user";
        final UserGroup g1 = new UserGroupImpl(uuid1, username+SecurityService.TENANT_SUFFIX);
        final User user1 = new UserImpl(username, /* email */ null, (Map<String, UserGroup>) /* defaultTenantForServer */ null, /* userGroupProvider */ null, new TimedLockImpl());
        g1.add(user1);
        final UserGroup g2 = new UserGroupImpl(uuid2, username+SecurityService.TENANT_SUFFIX);
        final User user2 = new UserImpl(username, /* email */ null, (Map<String, UserGroup>) /* defaultTenantForServer */ null, /* userGroupProvider */ null, new TimedLockImpl());
        g2.add(user2);
        final Map<User, User> userMap = new HashMap<>();
        userMap.put(user2, user1); // user2 assumed to get merged with user1
        assertTrue(SecurityStoreMerger.considerGroupsIdentical(g1, g2, userMap));
    }
}
