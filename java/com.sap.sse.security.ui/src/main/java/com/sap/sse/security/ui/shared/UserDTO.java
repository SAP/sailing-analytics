package com.sap.sse.security.ui.shared;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.AccessControlList;
import com.sap.sse.security.shared.Ownership;
import com.sap.sse.security.shared.Role;
import com.sap.sse.security.shared.SecuredObject;
import com.sap.sse.security.shared.UserGroup;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.SecurityUserImpl;

public class UserDTO extends SecurityUserImpl implements IsSerializable, SecuredObject {
    private static final long serialVersionUID = -4807678211983511872L;
    
    private String email;
    private String fullName;
    private String company;
    private String locale;
    private List<AccountDTO> accounts;
    private boolean emailValidated;
    private List<UserGroup> groups;
    private AccessControlList accessControlList;
    private Ownership ownership;
    private UserGroup defaultTenantForCurrentServer;

    /**
     * @param groups may be {@code null} which is equivalent to passing an empty groups collection
     */
    public UserDTO(String name, String email, String fullName, String company, String locale, boolean emailValidated,
            List<AccountDTO> accounts, Iterable<Role> roles, UserGroup defaultTenant, Iterable<WildcardPermission> permissions,
            Iterable<UserGroup> groups) {
        super(name, roles, permissions);
        this.defaultTenantForCurrentServer = defaultTenant;
        this.email = email;
        this.fullName = fullName;
        this.company = company;
        this.locale = locale;
        this.emailValidated = emailValidated;
        this.accounts = accounts;
        this.groups = new ArrayList<>();
        Util.addAll(groups, this.groups);
    }

    /**
     * The tenant to use as {@link Ownership#getTenantOwner() tenant owner} of new objects created by this user
     */
    public UserGroup getDefaultTenant() {
        return defaultTenantForCurrentServer;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCompany() {
        return company;
    }
    
    public String getLocale() {
        return locale;
    }

    public Iterable<String> getStringRoles() {
        ArrayList<String> result = new ArrayList<>();
        for (Role role : getRoles()) {
            result.add(role.toString());
        }
        return result;
    }
    
    /**
     * Same as {@link #getPermissions()}, but returning the permissions in their string representation,
     * as specified by {@link WildcardPermission#toString()}.
     */
    public Iterable<String> getStringPermissions() {
        List<String> result = new ArrayList<>();
        for (WildcardPermission wp : getPermissions()) {
            result.add(wp.toString());
        }
        return result;
    }
    
    /**
     * Objects of this type have a copy of their user groups embedded and can respond to this
     * call with the data embedded. Note, however, that the response is not "live," so there is
     * no round-trip to the server involved.
     */
    @Override
    public Iterable<UserGroup> getUserGroups() {
        return groups;
    }

    public List<AccountDTO> getAccounts() {
        return accounts;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailValidated() {
        return emailValidated;
    }

    @Override
    public final AccessControlList getAccessControlList() {
        return accessControlList;
    }

    @Override
    public final Ownership getOwnership() {
        return ownership;
    }

    @Override
    public final void setAccessControlList(final AccessControlList accessControlList) {
        this.accessControlList = accessControlList;
    }

    @Override
    public final void setOwnership(final Ownership ownership) {
        this.ownership = ownership;
    }
    
    public void setDefaultTenantForCurrentServer(UserGroup defaultTenant) {
        this.defaultTenantForCurrentServer = defaultTenant;
    }
}
