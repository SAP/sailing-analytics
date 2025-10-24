package com.sap.sse.security.ui.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.landscape.aws.common.shared.SecuredAwsLandscapeType;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.interfaces.Credential;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UnauthorizedException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.AccessControlListAnnotationDTO;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.RoleDefinitionDTO;
import com.sap.sse.security.shared.dto.RolesAndPermissionsForUserDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.StrippedUserDTO;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.shared.dto.WildcardPermissionWithSecurityDTO;
import com.sap.sse.security.shared.impl.PermissionAndRoleAssociation;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.ui.client.SerializationDummy;
import com.sap.sse.security.ui.client.UserManagementService;
import com.sap.sse.security.ui.oauth.client.CredentialDTO;
import com.sap.sse.security.ui.shared.SecurityServiceSharingDTO;
import com.sap.sse.security.ui.shared.SuccessInfo;

public class UserManagementServiceImpl extends RemoteServiceServlet implements UserManagementService {
    private static final long serialVersionUID = 4458564336368629101L;
    
    private static final Logger logger = Logger.getLogger(UserManagementServiceImpl.class.getName());

    private final BundleContext context;
    private final FutureTask<SecurityService> securityService;
    protected final SecurityDTOFactory securityDTOFactory;

    public UserManagementServiceImpl() {
        context = Activator.getContext();
        securityDTOFactory = new SecurityDTOFactory();
        final ServiceTracker<SecurityService, SecurityService> tracker = new ServiceTracker<>(context, SecurityService.class, /* customizer */ null);
        tracker.open();
        securityService = new FutureTask<SecurityService>(new Callable<SecurityService>() {
            @Override
            public SecurityService call() {
                SecurityService result = null;
                try {
                    logger.info("Waiting for SecurityService...");
                    result = tracker.waitForService(0);
                    logger.info("Obtained SecurityService "+result);
                    return result;
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Interrupted while waiting for UserStore service", e);
                }
                return result;
            }
        });
        new Thread("ServiceTracker in bundle com.sap.sse.security.ui waiting for SecurityService") {
            @Override
            public void run() {
                securityService.run();
                SecurityUtils.setSecurityManager(getSecurityService().getSecurityManager());
            }
        }.start();
    }

    protected UserDTO getAllUser() {
        final User allUser = getSecurityService().getAllUser();
        return allUser == null ? null
                : securityDTOFactory.createUserDTOFromUser(allUser, getSecurityService());
    }
    
    protected ServerInfoDTO getServerInfo() {
        ServerInfoDTO result = new ServerInfoDTO(ServerInfo.getName(), ServerInfo.getBuildVersion(), ServerInfo.getManageEventsBaseUrl());
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), result);
        return result;
    }
    
    @Override
    public ArrayList<HasPermissions> getAllHasPermissions() {
        final ArrayList<HasPermissions> result = new ArrayList<>();
        Util.addAll(getSecurityService().getAllHasPermissions(), result);
        return result;
    }
    
    @Override
    public ArrayList<RoleDefinitionDTO> getRoleDefinitions() {
        final ArrayList<RoleDefinitionDTO> result = new ArrayList<>();
        Util.addAll(securityDTOFactory.createRoleDefinitionDTOs(getSecurityService().getRoleDefinitions(),
                getSecurityService()), result);
        return result;
    }

    @Override
    public Collection<AccessControlListAnnotationDTO> getAccessControlLists() throws UnauthorizedException {
        // TODO decide whether a global getAccessControlList functionality is needed
        List<AccessControlListAnnotationDTO> acls = new ArrayList<>();
        for (AccessControlListAnnotation acl : getSecurityService().getAccessControlLists()) {
            if (SecurityUtils.getSubject()
                    .isPermitted(acl.getIdOfAnnotatedObject().getStringPermission(DefaultActions.CHANGE_ACL))) {
                acls.add(securityDTOFactory.createAccessControlListAnnotationDTO(acl));
            }
        }
        return acls;
    }

    @Override
    public Collection<UserGroupDTO> getUserGroups() {
        Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser = new HashMap<>();
        Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup = new HashMap<>();
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(getSecurityService().getUserGroupList(),
                ug -> securityDTOFactory.createUserGroupDTOFromUserGroup(ug, fromOriginalToStrippedDownUser,
                        fromOriginalToStrippedDownUserGroup, getSecurityService()));
    }

    @Override
    public UserGroupDTO getUserGroupByName(String userGroupName) throws UnauthorizedException {
        final UserGroup userGroup = getSecurityService().getUserGroupByName(userGroupName);
        if (userGroup == null || SecurityUtils.getSubject().isPermitted(
                SecuredSecurityTypes.USER_GROUP.getStringPermissionForObject(DefaultActions.READ, userGroup))) {
            Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser = new HashMap<>();
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup = new HashMap<>();
            return securityDTOFactory.createUserGroupDTOFromUserGroup(userGroup, fromOriginalToStrippedDownUser,
                    fromOriginalToStrippedDownUserGroup, getSecurityService());
        } else {
            throw new UnauthorizedException("Not permitted to read user group "+userGroupName);
        }
    }

    @Override
    public StrippedUserGroupDTO getStrippedUserGroupByName(String userGroupName) throws UnauthorizedException {
        final UserGroup userGroup = getSecurityService().getUserGroupByName(userGroupName);
        if (userGroup == null || SecurityUtils.getSubject().isPermitted(SecuredSecurityTypes.USER_GROUP
                .getStringPermissionForObject(DefaultActions.READ, userGroup))) {
            Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup = new HashMap<>();
            return securityDTOFactory.createStrippedUserGroupDTOFromUserGroup(userGroup,
                    fromOriginalToStrippedDownUserGroup);
        } else {
            throw new UnauthorizedException("Not permitted to read user group " + userGroupName);
        }
    }

    private UserDTO getUserByName(String username) throws UnauthorizedException {
        final User user = getSecurityService().getUserByName(username);
        if (user == null
                || SecurityUtils.getSubject()
                        .isPermitted(SecuredSecurityTypes.USER.getStringPermissionForObject(DefaultActions.READ, user))
                || SecurityUtils.getSubject().isPermitted(SecuredSecurityTypes.USER
                        .getStringPermissionForObject(SecuredSecurityTypes.PublicReadableActions.READ_PUBLIC, user))) {
            // TODO: pruning when current user only has READ_PUBLIC permission
            return user == null ? null : securityDTOFactory.createUserDTOFromUser(user, getSecurityService());
        } else {
            throw new UnauthorizedException("Not permitted to read user " + username);
        }
    }

    @Override
    public Collection<UserDTO> getUserList() throws UnauthorizedException {
        final HasPermissions.Action[] requiredActionsForRead = SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS;
        return getSecurityService().mapAndFilterByAnyExplicitPermissionForCurrentUser(SecuredSecurityTypes.USER,
                requiredActionsForRead, getSecurityService().getUserList(),
                this::getUserDTOWithFilteredRolesAndPermissions);
    }

    @Override
    public Triple<UserDTO, UserDTO, ServerInfoDTO> getCurrentUser() throws UnauthorizedException {
        logger.fine("Request: " + getThreadLocalRequest().getRequestURL());
        User user = getSecurityService().getCurrentUser();
        if (user == null) {
            return new Triple<>(null, getAllUser(), getServerInfo());
        }
        getSecurityService().checkCurrentUserReadPermission(user);
        return new Triple<>(securityDTOFactory.createUserDTOFromUser(user, getSecurityService()),
                getAllUser(), getServerInfo());
    }

    @Override
    public Map<String, String> getSettings() {
        Map<String, String> settings = new TreeMap<String, String>();
        for (Entry<String, Object> e : getSecurityService().getAllSettings().entrySet()){
            settings.put(e.getKey(), e.getValue().toString());
        }
        return settings;
    }

    @Override
    public Map<String, String> getSettingTypes() {
        Map<String, String> settingTypes = new TreeMap<String, String>();
        for (Entry<String, Class<?>> e : getSecurityService().getAllSettingTypes().entrySet()) {
            settingTypes.put(e.getKey(), e.getValue().getName());
        }
        return settingTypes;
    }
    
    protected SecurityService getSecurityService() {
        try {
            return securityService.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPreference(String username, String key) throws UserManagementException, UnauthorizedException {
        getSecurityService().checkCurrentUserReadPermission(getSecurityService().getUserByName(username));
        return getSecurityService().getPreference(username, key);
    }
    
    @Override
    public Map<String, String> getPreferences(String username, List<String> keys) throws UserManagementException, UnauthorizedException {
        getSecurityService().checkCurrentUserReadPermission(getSecurityService().getUserByName(username));
        Map<String, String> requestedPreferences = new HashMap<>();
        for (String key : keys) {
            requestedPreferences.put(key, getSecurityService().getPreference(username, key));
        }
        return requestedPreferences;
    }
    
    @Override
    public Map<String, String> getAllPreferences(String username)
            throws UserManagementException, UnauthorizedException {
        getSecurityService().checkCurrentUserReadPermission(getSecurityService().getUserByName(username));
        final Map<String, String> allPreferences = getSecurityService().getAllPreferences(username);
        final Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : allPreferences.entrySet()) {
            if (!entry.getKey().startsWith("_")) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    @Override
    public String getOrCreateAccessToken(String username) throws UnauthorizedException {
        getSecurityService().checkCurrentUserUpdatePermission(getUserByName(username));
        return getSecurityService().getOrCreateAccessToken(username);
    }

    @Override
    public AccessControlListDTO getAccessControlListWithoutPruning(QualifiedObjectIdentifier idOfAccessControlledObject) throws UnauthorizedException {
        if (SecurityUtils.getSubject()
                .isPermitted(idOfAccessControlledObject.getStringPermission(DefaultActions.CHANGE_ACL))) {
            AccessControlListAnnotation accessControlList = getSecurityService().getAccessControlList(idOfAccessControlledObject);
            if (accessControlList == null) {
                return null;
            }
            return securityDTOFactory.createAccessControlListDTO(
                    accessControlList.getAnnotation());
        } else {
            throw new UnauthorizedException("Not permitted to get the unpruned ACL for a user");
        }
    }

    @Override
    public SecuredDTO addSecurityInformation(SecuredDTO securedDTO) {
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), securedDTO);
        return securedDTO;
    }

    @Override
    public SerializationDummy serializationDummy(TypeRelativeObjectIdentifier typeRelativeObjectIdentifier, HasPermissions hasPermissions, SecuredAwsLandscapeType securedAwsLandscapeType) {
        return null;
    }

    @Override
    public Boolean userExists(String username) {
        return getSecurityService().getUserByName(username) != null;
    }

    @Override
    public Boolean userGroupExists(String userGroupName) {
        return getSecurityService().getUserGroupByName(userGroupName) != null;
    }

    @Override
    public RolesAndPermissionsForUserDTO getRolesAndPermissionsForUser(String username) throws UserManagementException {
        final User user = getSecurityService().getUserByName(username);
        if (user == null) {
            throw new UserManagementException("User '" + username + "'not found.");
        }
        final UserDTO userDTO = getUserDTOWithFilteredRolesAndPermissions(user);
        Collection<WildcardPermissionWithSecurityDTO> permissions = new ArrayList<>();
        for (WildcardPermission p : userDTO.getPermissions()) {
            if (p instanceof WildcardPermissionWithSecurityDTO) {
                permissions.add((WildcardPermissionWithSecurityDTO) p);
            }
        }
        return new RolesAndPermissionsForUserDTO(userDTO.getRoles(), permissions);
    }

    /**
     * @return The UserDTO for the given {@link User user} with his permissions and roles filtered to those the current
     *         user can actually see.
     */
    private UserDTO getUserDTOWithFilteredRolesAndPermissions(final User user) {
        final UserDTO result = securityDTOFactory.createUserDTOFromUser(user, getSecurityService(), permission -> {
            final TypeRelativeObjectIdentifier typeRelativeObjectIdentifier = PermissionAndRoleAssociation
                    .get(permission, user);
            return SecurityUtils.getSubject().isPermitted(SecuredSecurityTypes.PERMISSION_ASSOCIATION
                    .getStringPermissionForTypeRelativeIdentifier(DefaultActions.READ, typeRelativeObjectIdentifier));
        }, role -> {
            final TypeRelativeObjectIdentifier typeRelativeObjectIdentifier = PermissionAndRoleAssociation.get(role,
                    user);
            return SecurityUtils.getSubject().isPermitted(SecuredSecurityTypes.ROLE_ASSOCIATION
                    .getStringPermissionForTypeRelativeIdentifier(DefaultActions.READ, typeRelativeObjectIdentifier));
        });
        if (!getSecurityService().hasCurrentUserReadPermission(user)) {
            result.clearNonPublicFields();
        }
        return result;
    }

    @Override
    public SecurityServiceSharingDTO getSharingConfiguration() {
        return new SecurityServiceSharingDTO(getSecurityService().getSharedAcrossSubdomainsOf(),
                getSecurityService().getBaseUrlForCrossDomainStorage());
    }

    protected Credential createCredentialFromDTO(CredentialDTO credentialDTO) {
        Credential credential = new Credential();
        credential.setAuthProvider(credentialDTO.getAuthProvider());
        credential.setAuthProviderName(credentialDTO.getAuthProviderName());
        credential.setEmail(credentialDTO.getEmail());
        credential.setLoginName(credentialDTO.getLoginName());
        credential.setPassword(credentialDTO.getPassword());
        credential.setRedirectUrl(credentialDTO.getRedirectUrl());
        credential.setState(credentialDTO.getState());
        credential.setVerifier(credentialDTO.getVerifier());
        credential.setOauthToken(credentialDTO.getOauthToken());
        return credential;
    }

    @Override
    public Triple<UserDTO, UserDTO, ServerInfoDTO> verifySocialUser(CredentialDTO credentialDTO) {
        User user = null;
        try {
            user = getSecurityService().verifySocialUser(createCredentialFromDTO(credentialDTO));
        } catch (UserManagementException e) {
            e.printStackTrace();
        }
        final UserDTO userDTO = securityDTOFactory.createUserDTOFromUser(user, getSecurityService());
        return new Triple<>(userDTO, getAllUser(), getServerInfo());
    }

    @Override
    public SuccessInfo logout() {
        logger.info("Logging out user: " + SecurityUtils.getSubject());
        getSecurityService().logout();
        getHttpSession().invalidate();
        final Cookie cookie = new Cookie(UserManagementConstants.LOCALE_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setSecure(true);
        getThreadLocalResponse().addCookie(cookie);
        logger.info("Invalidated HTTP session");
        return new SuccessInfo(true, "Logged out.", /* redirectURL */ null, null);
    }

    @Override
    public SuccessInfo login(String username, String password) {
        try {
            String redirectURL = getSecurityService().login(username, password);
            UserDTO user = securityDTOFactory.createUserDTOFromUser(getSecurityService().getUserByName(username),
                    getSecurityService());
            return new SuccessInfo(true, "Success. Redirecting to " + redirectURL, redirectURL,
                    new Triple<>(user, getAllUser(), getServerInfo()));
        } catch (UserManagementException | AuthenticationException e) {
            return new SuccessInfo(false, SuccessInfo.FAILED_TO_LOGIN, /* redirectURL */ null, null);
        }
    }

    private HttpSession getHttpSession() {
        return getThreadLocalRequest().getSession();
    }

    @Override
    public Pair<Boolean, ArrayList<String>> getCORSFilterConfiguration() {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_CORS_FILTER);
        final Pair<Boolean, Set<String>> preResult = getSecurityService().getCORSFilterConfiguration(ServerInfo.getName());
        return preResult == null ? null : new Pair<>(preResult.getA(), new ArrayList<>(preResult.getB()));
    }
}
