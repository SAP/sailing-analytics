package com.sap.sse.security.shared.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sap.sse.security.shared.AbstractRole;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.SecurityUser;
import com.sap.sse.security.shared.SecurityUserGroup;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WildcardPermission;

public abstract class SecurityUserImpl<RD extends RoleDefinition, R extends AbstractRole<RD, G, ?>, G extends SecurityUserGroup<?>, P extends WildcardPermission>
        extends AbstractUserReference implements SecurityUser<RD, R, G> {
    private static final long serialVersionUID = -3639860207453072248L;

    private Set<WildcardPermission> permissions;
    
    /**
     * Creates a user with empty permission set and empty role set
     */
    public SecurityUserImpl(String name) {
        this(name, new HashSet<P>());
    }
    
    public SecurityUserImpl(String name, Iterable<P> permissions) {
        super(name);
        this.permissions = new HashSet<>();
        for (P permission : permissions) {
            this.permissions.add(permission);
        }
    }
    
    protected abstract Set<R> getRolesInternal();

    @Override
    public Iterable<R> getRoles() {
        return getRolesInternal();
    }

    @Override
    public boolean hasRole(R role) {
        return getRolesInternal().contains(role);
    }
    
    @Override
    public Iterable<WildcardPermission> getPermissions() {
        return permissions;
    }
    
    public void addRole(R role) {
        getRolesInternal().add(role);
    }

    public void removeRole(R role) {
        getRolesInternal().remove(role);
    }

    public void addPermission(P permission) {
        permissions.add(permission);
    }
    
    public void removePermission(P permission) {
        permissions.remove(permission);
    }

    @Override
    public String toString() {
        return getName() + " (roles: " + getRoles() + ")";
    }

    /**
     * This default implementation does not know where to obtain this user's groups from. It
     * therefore returns an empty collection.
     */
    @Override
    public Iterable<G> getUserGroups() {
        return Collections.emptyList();
    }


    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier()); 
    }

    public TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return getTypeRelativeObjectIdentifier(getName());
    }

    @Override
    public HasPermissions getPermissionType() {
        return SecuredSecurityTypes.USER;
    }

    public static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(String userName) {
        return new TypeRelativeObjectIdentifier(userName);
    }
}
