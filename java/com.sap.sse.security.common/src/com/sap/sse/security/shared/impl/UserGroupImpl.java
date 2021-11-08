package com.sap.sse.security.shared.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sap.sse.security.shared.AbstractUserGroupImpl;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

public class UserGroupImpl extends AbstractUserGroupImpl<User, RoleDefinition> implements UserGroup {

    private static final long serialVersionUID = -1290667868080992763L;

    public UserGroupImpl(UUID id, String name) {
        this(id, name, new HashSet<>(), new HashMap<>());
    }

    public UserGroupImpl(UUID id, String name, Set<User> users, Map<RoleDefinition, Boolean> roleDefinitionMap) {
        super(id, name, users, roleDefinitionMap);
    }

    static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(UserGroup userGroup) {
        return getTypeRelativeObjectIdentifier(userGroup.getId());
    }

    public static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(UUID uuid) {
        return new TypeRelativeObjectIdentifier(uuid.toString());
    }

    public static String getTypeRelativeObjectIdentifierAsString(UserGroup userGroup) {
        return userGroup.getId().toString();
    }

    @Override
    public boolean isRoleAssociated(RoleDefinition roleDefinition) {
        return roleDefinitionMap.containsKey(roleDefinition);
    }

    @Override
    public Boolean getRoleAssociation(RoleDefinition roleDefinition) {
        return roleDefinitionMap.get(roleDefinition);
    }
}
