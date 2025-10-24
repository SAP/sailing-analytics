package com.sap.sse.security.shared.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sap.sse.common.Named;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.SecurityUserImpl;

public class UserDTO extends
        SecurityUserImpl<StrippedRoleDefinitionDTO, RoleWithSecurityDTO, StrippedUserGroupDTO, WildcardPermissionWithSecurityDTO>
        implements Named, Serializable, SecuredDTO {

    private static final long serialVersionUID = 7556217539893146187L;

    private String email;
    private String fullName;
    private String company;
    private String locale;
    private List<AccountDTO> accounts;
    private boolean emailValidated;
    private List<StrippedUserGroupDTO> groups;
    private TimePoint lockedUntil;
    private SecurityInformationDTO securityInformation = new SecurityInformationDTO();
    private StrippedUserGroupDTO defaultTenantForCurrentServer;

    private Set<RoleWithSecurityDTO> roles;

    @Deprecated // gwt only
    UserDTO() {
        super(null);
    }

    /**
     * @param groups may be {@code null} which is equivalent to passing an empty groups collection
     */
    public UserDTO(String name, String email, String fullName, String company, String locale, boolean emailValidated,
            List<AccountDTO> accounts, Iterable<RoleWithSecurityDTO> roles, StrippedUserGroupDTO defaultTenant,
            Iterable<WildcardPermissionWithSecurityDTO> permissions,
            Iterable<StrippedUserGroupDTO> groups, TimePoint lockedUntil) {
        super(name, permissions);
        this.defaultTenantForCurrentServer = defaultTenant;
        this.email = email;
        this.fullName = fullName;
        this.company = company;
        this.locale = locale;
        this.emailValidated = emailValidated;
        this.accounts = accounts;
        this.groups = new ArrayList<>();
        Util.addAll(groups, this.groups);
        this.roles = new HashSet<>();
        Util.addAll(roles, this.getRolesInternal());
        this.lockedUntil = lockedUntil;
    }
    
    @Override
    protected Set<RoleWithSecurityDTO> getRolesInternal() {
        return roles;
    }

    /**
     * The tenant to use as {@link Ownership#getTenantOwner() tenant owner} of new objects created by this user
     */
    public StrippedUserGroupDTO getDefaultTenant() {
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
        for (RoleDTO role : getRoles()) {
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
     * Objects of this type have a copy of their user groups embedded and can respond to this call with the data
     * embedded. Note, however, that the response is not "live," so there is no round-trip to the server involved.
     */
    @Override
    public List<StrippedUserGroupDTO> getUserGroups() {
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
    public final AccessControlListDTO getAccessControlList() {
        return securityInformation.getAccessControlList();
    }

    @Override
    public final OwnershipDTO getOwnership() {
        return securityInformation.getOwnership();
    }

    @Override
    public final void setAccessControlList(final AccessControlListDTO accessControlList) {
        this.securityInformation.setAccessControlList(accessControlList);
    }

    @Override
    public final void setOwnership(final OwnershipDTO ownership) {
        this.securityInformation.setOwnership(ownership);
    }
    
    public void setDefaultTenantForCurrentServer(StrippedUserGroupDTO defaultTenant) {
        this.defaultTenantForCurrentServer = defaultTenant;
    }
    
    public StrippedUserDTO asStrippedUser() {
        return new StrippedUserDTO(getName());
    }

    @Override
    public TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return new TypeRelativeObjectIdentifier(getName());
    }

    /**
     * Erases the fields a user is not supposed to read with only the READ_PUBLIC and not the
     * READ permission.
     */
    public void clearNonPublicFields() {
        this.email = null;
    }
    
    public TimePoint getLockedUntil() {
        return lockedUntil;
    }
}
