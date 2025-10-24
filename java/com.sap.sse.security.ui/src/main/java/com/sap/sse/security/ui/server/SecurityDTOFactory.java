package com.sap.sse.security.ui.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.sap.sse.common.Util;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.interfaces.Social;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.Account.AccountType;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.SecurityUser;
import com.sap.sse.security.shared.SocialUserAccount;
import com.sap.sse.security.shared.UsernamePasswordAccount;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.AccessControlListAnnotationDTO;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.AccountDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.RoleDefinitionDTO;
import com.sap.sse.security.shared.dto.RoleWithSecurityDTO;
import com.sap.sse.security.shared.dto.StrippedRoleDefinitionDTO;
import com.sap.sse.security.shared.dto.StrippedUserDTO;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.shared.dto.WildcardPermissionWithSecurityDTO;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.PermissionAndRoleAssociation;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sap.sse.security.ui.oauth.client.SocialUserDTO;
import com.sap.sse.security.ui.shared.UsernamePasswordAccountDTO;

public class SecurityDTOFactory {
    private StrippedUserDTO createUserDTOFromUser(User user,
            Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        StrippedUserDTO result;
        if (user == null) {
            result = null;
        } else {
            result = fromOriginalToStrippedDownUser.get(user);
            if (result == null) {
                final StrippedUserDTO preResult = new StrippedUserDTO(user.getName());
                result = preResult;
                fromOriginalToStrippedDownUser.put(user, result);
            }
        }
        return result;
    }

    private UserDTO createUserDTOFromUser(User user, Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup,
            SecurityService securityService, Predicate<WildcardPermission> permissionFilter,
            Predicate<Role> roleFilter) {
        UserDTO userDTO;
        Map<AccountType, Account> accounts = user.getAllAccounts();
        List<AccountDTO> accountDTOs = new ArrayList<>();
        for (Account a : accounts.values()){
            switch (a.getAccountType()) {
            case SOCIAL_USER:
                accountDTOs.add(createSocialUserDTO((SocialUserAccount) a));
                break;
    
            default:
                UsernamePasswordAccount upa = (UsernamePasswordAccount) a;
                accountDTOs.add(new UsernamePasswordAccountDTO(upa.getName(), upa.getSaltedPassword(), upa.getSalt()));
                break;
            }
        }
        final List<WildcardPermission> filteredPermissions = StreamSupport
                .stream(user.getPermissions().spliterator(), false).filter(permissionFilter)
                .collect(Collectors.toList());
        final List<Role> filteredRoles = StreamSupport.stream(user.getRoles().spliterator(), false).filter(roleFilter)
                .collect(Collectors.toList());
        userDTO = new UserDTO(user.getName(), user.getEmail(), user.getFullName(), user.getCompany(),
                user.getLocale() != null ? user.getLocale().toLanguageTag() : null, user.isEmailValidated(),
                accountDTOs, createRolesDTOs(filteredRoles, fromOriginalToStrippedDownUser,
                        fromOriginalToStrippedDownUserGroup, securityService, user),
                /* default tenant filled in later */ null,
                getSecuredPermissions(filteredPermissions, user, securityService),
                createStrippedUserGroupDTOsFromUserGroups(securityService.getUserGroupsOfUser(user),
                        fromOriginalToStrippedDownUser, fromOriginalToStrippedDownUserGroup), user.getLockingAndBanning().getLockedUntil());
        userDTO.setDefaultTenantForCurrentServer(createStrippedUserGroupDTOFromUserGroup(
                securityService.getDefaultTenantForCurrentUser(),
                fromOriginalToStrippedDownUserGroup));
        SecurityDTOUtil.addSecurityInformation(this, securityService, userDTO, fromOriginalToStrippedDownUser,
                fromOriginalToStrippedDownUserGroup);
        return userDTO;
    }

    public Iterable<WildcardPermissionWithSecurityDTO> getSecuredPermissions(
            final Iterable<WildcardPermission> permissions, final User user, final SecurityService securityService) {
        Collection<WildcardPermissionWithSecurityDTO> securedPermissions = new ArrayList<>();
        for (WildcardPermission permission : permissions) {
            QualifiedObjectIdentifier identifier = SecuredSecurityTypes.PERMISSION_ASSOCIATION
                    .getQualifiedObjectIdentifier(PermissionAndRoleAssociation.get(permission, user));
            WildcardPermissionWithSecurityDTO securedPermission = new WildcardPermissionWithSecurityDTO(
                    permission.toString(), identifier);
            SecurityDTOUtil.addSecurityInformation(securityService, securedPermission);
            securedPermissions.add(securedPermission);
        }
        return securedPermissions;
    }


    private Iterable<RoleWithSecurityDTO> createRolesDTOs(Iterable<Role> roles,
            Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup,
            SecurityService securityService, User user) {
        return Util.map(roles, role->createRoleDTO(role,
                fromOriginalToStrippedDownUser, fromOriginalToStrippedDownUserGroup, securityService, user));
    }

    private RoleWithSecurityDTO createRoleDTO(Role role, Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup,
            SecurityService securityService, User user) {
        RoleDefinition rdef = role.getRoleDefinition();
        StrippedRoleDefinitionDTO rdefDTO = createStrippedRoleDefinitionDTO(rdef,
                fromOriginalToStrippedDownUserGroup);
        QualifiedObjectIdentifier identifier = SecuredSecurityTypes.ROLE_ASSOCIATION
                .getQualifiedObjectIdentifier(PermissionAndRoleAssociation.get(role, user));
        RoleWithSecurityDTO mappedRole = new RoleWithSecurityDTO(rdefDTO,
                createStrippedUserGroupDTOFromUserGroup(role.getQualifiedForTenant(),
                fromOriginalToStrippedDownUserGroup),
                createUserDTOFromUser(role.getQualifiedForUser(),
                        fromOriginalToStrippedDownUser, fromOriginalToStrippedDownUserGroup), role.isTransitive(),
                identifier);
        SecurityDTOUtil.addSecurityInformation(securityService, mappedRole);
        return mappedRole;
    }
    
    private StrippedRoleDefinitionDTO createStrippedRoleDefinitionDTO(final RoleDefinition roleDefinition,
            final Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        return new StrippedRoleDefinitionDTO(roleDefinition.getId(), roleDefinition.getName(),
                roleDefinition.getPermissions());
    }

    private RoleDefinitionDTO createRoleDefinitionDTO(final RoleDefinition roleDefinition,
            final SecurityService securityService, final Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            final Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        final RoleDefinitionDTO roleDefDTO = new RoleDefinitionDTO(roleDefinition.getId(), roleDefinition.getName(),
                roleDefinition.getPermissions());
        SecurityDTOUtil.addSecurityInformation(this, securityService, roleDefDTO, fromOriginalToStrippedDownUser,
                fromOriginalToStrippedDownUserGroup);
        return roleDefDTO;
    }

    public RoleDefinitionDTO createRoleDefinitionDTO(final RoleDefinition roleDefinition,
            final SecurityService securityService) {
        return createRoleDefinitionDTO(roleDefinition, securityService, new HashMap<>(), new HashMap<>());
    }

    public Iterable<RoleDefinitionDTO> createRoleDefinitionDTOs(final Iterable<RoleDefinition> roleDefinitions,
            final SecurityService securityService) {
        final Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser = new HashMap<>();
        final Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup = new HashMap<>();
        return Util.map(roleDefinitions, roleDefinition -> createRoleDefinitionDTO(roleDefinition, securityService,
                fromOriginalToStrippedDownUser, fromOriginalToStrippedDownUserGroup));
    }

    private Map<StrippedRoleDefinitionDTO, Boolean> createUserGroupRoleDefinitionDTOs(
            final Map<RoleDefinition, Boolean> roleDefinitions,
            final Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        final Map<StrippedRoleDefinitionDTO, Boolean> roleDefinitionDTOs = new HashMap<>();
        for (Entry<RoleDefinition, Boolean> entry : roleDefinitions.entrySet()) {
            roleDefinitionDTOs.put(createStrippedRoleDefinitionDTO(entry.getKey(),
                    fromOriginalToStrippedDownUserGroup), entry.getValue());
        }
        return roleDefinitionDTOs;
    }

    public SocialUserDTO createSocialUserDTO(SocialUserAccount socialUser) {
        SocialUserDTO socialUserDTO = new SocialUserDTO(socialUser.getProperty(Social.PROVIDER.name()));
        socialUserDTO.setSessionId(socialUser.getSessionId());
        for (Social s : Social.values()){
            socialUserDTO.setProperty(s.name(), socialUser.getProperty(s.name()));
        }

        return socialUserDTO;
    }

    public UserDTO createUserDTOFromUser(User user, SecurityService securityService,
            Predicate<WildcardPermission> permissionFilter, Predicate<Role> roleFilter) {
        return createUserDTOFromUser(user, new HashMap<>(), new HashMap<>(), securityService, permissionFilter,
                roleFilter);
    }
    public UserDTO createUserDTOFromUser(User user, SecurityService securityService) {
        return createUserDTOFromUser(user, new HashMap<>(), new HashMap<>(), securityService, v -> true, v -> true);
    }

    private Iterable<StrippedUserGroupDTO> createStrippedUserGroupDTOsFromUserGroups(
            Iterable<UserGroup> userGroups,
            Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        final List<StrippedUserGroupDTO> result;
        if (userGroups == null) {
            result = null;
        } else {
            result = new ArrayList<>();
            for (final UserGroup userGroup : userGroups) {
                result.add(createStrippedUserGroupDTOFromUserGroup(userGroup,
                        fromOriginalToStrippedDownUserGroup));
            }
        }
        return result;
    }

    /**
     * Produces a full {@link UserGroupImpl} object that has stripped-down {@link User} objects with their default
     * tenants stripped down and mapped by this same method recursively where for a single {@link User} object only a
     * single stripped-down user object will be created, as will for tenants.
     * 
     * @param securityService
     */
    UserGroupDTO createUserGroupDTOFromUserGroup(UserGroup userGroup,
            Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup, SecurityService securityService) {
        final UserGroupDTO result;
        if (userGroup == null) {
            result = null;
        } else {
            result = new UserGroupDTO(userGroup.getId(), userGroup.getName(),
                    createStrippedUsersFromUsers(userGroup.getUsers(), securityService, fromOriginalToStrippedDownUser,
                            fromOriginalToStrippedDownUserGroup),
                    createUserGroupRoleDefinitionDTOs(userGroup.getRoleDefinitionMap(),
                            fromOriginalToStrippedDownUserGroup));
            SecurityDTOUtil.addSecurityInformation(this, securityService, result, fromOriginalToStrippedDownUser,
                    fromOriginalToStrippedDownUserGroup);
        }
        return result;
    }
    
    public Iterable<StrippedUserGroupDTO> createStrippedUserGroupDTOFromUserGroups(Iterable<UserGroup> userGroups,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        Collection<StrippedUserGroupDTO> result = new ArrayList<>();
        for (UserGroup userGroup : userGroups) {
            result.add(createStrippedUserGroupDTOFromUserGroup(userGroup, fromOriginalToStrippedDownUserGroup));
        }
        return result;
    }

    public StrippedUserGroupDTO createStrippedUserGroupDTOFromUserGroup(UserGroup userGroup,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        final StrippedUserGroupDTO result;
        if (userGroup == null) {
            result = null;
        } else {
            if (fromOriginalToStrippedDownUserGroup.containsKey(userGroup)) {
                result = fromOriginalToStrippedDownUserGroup.get(userGroup);
            } else {
                result = new StrippedUserGroupDTO(userGroup.getId(), userGroup.getName(),
                        createUserGroupRoleDefinitionDTOs(userGroup.getRoleDefinitionMap(),
                                fromOriginalToStrippedDownUserGroup));
                fromOriginalToStrippedDownUserGroup.put(userGroup, result);
            }
        }
        return result;
    }

    public OwnershipDTO createOwnershipDTO(Ownership ownership,
            Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        final OwnershipDTO result;
        if (ownership == null) {
            result = null;
        } else {
            result = new OwnershipDTO(
                    createUserDTOFromUser(ownership.getUserOwner(), fromOriginalToStrippedDownUser,
                            fromOriginalToStrippedDownUserGroup),
                    createStrippedUserGroupDTOFromUserGroup(ownership.getTenantOwner(),
                            fromOriginalToStrippedDownUserGroup));
        }
        return result;
    }
    
    public AccessControlListAnnotationDTO createAccessControlListAnnotationDTO(
            AccessControlListAnnotation aclAnnotation) {
        return new AccessControlListAnnotationDTO(createAccessControlListDTO(aclAnnotation.getAnnotation()),
                aclAnnotation.getIdOfAnnotatedObject(), aclAnnotation.getDisplayNameOfAnnotatedObject());
    }
    
    public AccessControlListAnnotationDTO createAccessControlListAnnotationDTO(
            AccessControlListAnnotation aclAnnotation,
            Map<UserGroup, UserGroupDTO> fromOriginalToStrippedDownTenant,
            Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        return new AccessControlListAnnotationDTO(
                createAccessControlListDTO(aclAnnotation.getAnnotation(),
                fromOriginalToStrippedDownUser, fromOriginalToStrippedDownUserGroup),
                aclAnnotation.getIdOfAnnotatedObject(), aclAnnotation.getDisplayNameOfAnnotatedObject());
    }
        
    public AccessControlListDTO createAccessControlListDTO(AccessControlList acl) {
        return createAccessControlListDTO(acl, new HashMap<>(), new HashMap<>());
    }

    public AccessControlListDTO createAccessControlListDTO(AccessControlList acl,
            Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        final AccessControlListDTO result;
        if (acl == null) {
            result = null;
        } else {
            Map<StrippedUserGroupDTO, Set<String>> permissionMapDTO = new HashMap<>();
            for (final Entry<UserGroup, Set<String>> actionForGroup : acl.getActionsByUserGroup().entrySet()) {
                permissionMapDTO.put(
                        createStrippedUserGroupDTOFromUserGroup(actionForGroup.getKey(),
                                fromOriginalToStrippedDownUserGroup),
                        actionForGroup.getValue());
            }
            result = new AccessControlListDTO(permissionMapDTO);
        }
        return result;
    }

    /**
     * prunes the {@link AccessControlList} for the given {@link SecurityUser filterForUser} by removing all user groups
     * the user is not in from the resulting ACL. The {@code null} user group will remain because all users are assumed
     * to be in the pseudo-group of "anonymous" users.
     */
    public AccessControlListDTO pruneAccessControlListForUser(AccessControlListDTO acl, Iterable<StrippedUserGroupDTO> groupsOfUser,
            Iterable<StrippedUserGroupDTO> allUserGroups) {
        final AccessControlListDTO result;
        // add user groups of filterForUser user
        final Collection<StrippedUserGroupDTO> userGroups = new HashSet<>();
        Util.addAll(groupsOfUser, userGroups);
        // add user groups of alluser
        Util.addAll(allUserGroups, userGroups);
        if (acl != null) {
            final Map<StrippedUserGroupDTO, Set<String>> actionsByUserGroup = new HashMap<>();
            for (final Entry<StrippedUserGroupDTO, Set<String>> entry : acl.getActionsByUserGroup().entrySet()) {
                StrippedUserGroupDTO userGroup = entry.getKey();
                if (userGroup == null || userGroups.contains(userGroup)) {
                    actionsByUserGroup.put(userGroup, entry.getValue());
                }
            }
            result = new AccessControlListDTO(actionsByUserGroup);
        } else {
            result = null;
        }
        return result;
    }

    public static Ownership ownerFromDTO(OwnershipDTO ownershipDTO, SecurityService securityService) {
        User ownerUser = null;
        UserGroup ownerGroup = null;
        if (ownershipDTO.getUserOwner() != null) {
            ownerUser = securityService.getUserByName(ownershipDTO.getUserOwner().getName());
        }
        if (ownershipDTO.getTenantOwner() != null) {
            ownerGroup = securityService.getUserGroup(ownershipDTO.getTenantOwner().getId());
        }
        return new Ownership(ownerUser, ownerGroup);
    }

    public Set<StrippedUserDTO> createStrippedUsersFromUsers(Iterable<User> users, SecurityService securityService,
            Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        Set<StrippedUserDTO> result = new HashSet<>();
        users.forEach(user -> result.add(createStrippedUserFromUser(user, securityService,
                fromOriginalToStrippedDownUser, fromOriginalToStrippedDownUserGroup)));
        return result;
    }

    public StrippedUserDTO createStrippedUserFromUser(User user, SecurityService securityService,
            Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup) {
        StrippedUserDTO mappedUser = fromOriginalToStrippedDownUser.get(user);
        if (mappedUser == null) {
            mappedUser = new StrippedUserDTO(user.getName());
            fromOriginalToStrippedDownUser.put(user, mappedUser);
        }
        return mappedUser;
    }
}
