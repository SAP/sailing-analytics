package com.sap.sse.security.ui.server;

import java.util.HashMap;
import java.util.Map;

import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.SecurityUser;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.NamedSecuredObjectDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.StrippedUserDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

public abstract class SecurityDTOUtil {

    private SecurityDTOUtil() {
    }

    /**
     * Adds {@link AccessControlList access control list} and {@link Ownership ownership} information for the given
     * {@link QualifiedObjectIdentifier qualified object identifier} to the provided {@link NamedSecuredObjectDTO
     * secured object DTO}.
     * 
     * @param securityService
     *            the {@link SecurityService} to determine access control list and ownership
     * @param securedObject
     *            the {@link NamedSecuredObjectDTO} to add security information to
     * @param objectId
     *            the {@link QualifiedObjectIdentifier} to get security information for
     */
    public static void addSecurityInformation(final SecurityService securityService, final SecuredDTO securedObject,
            final QualifiedObjectIdentifier objectId) {
        addSecurityInformation(new SecurityDTOFactory(), securityService, securedObject, objectId);
    }

    /**
     * Adds {@link AccessControlList access control list} and {@link Ownership ownership} information for the given
     * {@link QualifiedObjectIdentifier qualified object identifier} to the provided {@link NamedSecuredObjectDTO
     * secured object DTO}.
     * 
     * @param securityService
     *            the {@link SecurityService} to determine access control list and ownership
     * @param securityDTOFactory
     *            the {@link SecurityDTOFactory} to use for DTO creation
     * @param securedObject
     *            the {@link NamedSecuredObjectDTO} to add security information to
     * @param objectId
     *            the {@link QualifiedObjectIdentifier} to get security information for
     */
    public static void addSecurityInformation(final SecurityDTOFactory securityDTOFactory,
            final SecurityService securityService, final SecuredDTO securedObject,
            final QualifiedObjectIdentifier objectId) {
        final Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser = new HashMap<>();
        final Map<UserGroup, UserGroupDTO> fromOriginalToStrippedDownUserGroup = new HashMap<>();
        addSecurityInformation(securityDTOFactory, securityService, securedObject, objectId,
                fromOriginalToStrippedDownUser, fromOriginalToStrippedDownUserGroup, false);
    }

    /**
     * Adds {@link AccessControlList access control list} and {@link Ownership ownership} information for the given
     * {@link QualifiedObjectIdentifier qualified object identifier} to the provided {@link NamedSecuredObjectDTO
     * secured object DTO} by using the provided mappings of {@link SecurityUser users} and {@link UserGroup user
     * groups}.
     * <p>
     * <b>NOTE:</b> This method can be used to reuse already stripped down {@link SecurityUser users} or
     * {@link UserGroup user groups} in order to avoid multiple mappings of the the same instances. However, it must be
     * used with caution, especially in the context of one or more {@link UserDTO} instances, which themselves contain
     * ownership information. Reusing an {@link UserDTO} as user owner object might cause infinite relation paths.
     * </p>
     * 
     * @param securityService
     *            the {@link SecurityService} to determine access control list and ownership
     * @param securityDTOFactory
     *            the {@link SecurityDTOFactory} to use for DTO creation
     * @param securedObject
     *            the {@link NamedSecuredObjectDTO} to add security information to
     * @param objectId
     *            the {@link QualifiedObjectIdentifier} to get security information for
     * @param fromOriginalToStrippedDownUser
     *            the {@link Map} to stripped down {@link SecurityUser user}s to use
     * @param fromOriginalToStrippedDownUserGroup
     *            the {@link Map} to stripped down {@link UserGroup user group}s to use
     */
    public static void addSecurityInformation(final SecurityDTOFactory securityDTOFactory,
            final SecurityService securityService, final SecuredDTO securedObject,
            final QualifiedObjectIdentifier objectId,
            final Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser,
            final Map<UserGroup, UserGroupDTO> fromOriginalToStrippedDownUserGroup,
            final boolean disablePruningForCurrentUser) {
        final AccessControlListAnnotation accessControlList = securityService.getAccessControlList(objectId);
        AccessControlListDTO accessControlListDTO = securityDTOFactory.createAccessControlListDTO(
                accessControlList == null ? null : accessControlList.getAnnotation(), fromOriginalToStrippedDownUser,
                fromOriginalToStrippedDownUserGroup);
        if (disablePruningForCurrentUser) {
            securedObject.setAccessControlList(accessControlListDTO);
        } else {
            User user = securityService.getCurrentUser();
            if (user != null) {
                // TODO also prune for all user
                StrippedUserDTO userDTO = new SecurityDTOFactory().createStrippedUserFromUser(user, securityService,
                        fromOriginalToStrippedDownUser, fromOriginalToStrippedDownUserGroup);
                securedObject.setAccessControlList(
                        securityDTOFactory.pruneAccessControlListForUser(accessControlListDTO, userDTO));
            }
        }
        final OwnershipAnnotation ownership = securityService.getOwnership(objectId);
        securedObject.setOwnership(
                securityDTOFactory.createOwnershipDTO(ownership == null ? null : ownership.getAnnotation(),
                        fromOriginalToStrippedDownUser, fromOriginalToStrippedDownUserGroup));
    }
}
