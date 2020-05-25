package com.sap.sse.security.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.config.Ini;
import org.apache.shiro.config.Ini.Section;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.mgt.CachingSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.apache.shiro.web.config.WebIniSecurityManagerFactory;
import org.apache.shiro.web.env.IniWebEnvironment;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
import org.osgi.util.tracker.ServiceTracker;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FacebookApi;
import org.scribe.builder.api.FlickrApi;
import org.scribe.builder.api.Foursquare2Api;
import org.scribe.builder.api.GoogleApi;
import org.scribe.builder.api.ImgUrApi;
import org.scribe.builder.api.LinkedInApi;
import org.scribe.builder.api.LiveApi;
import org.scribe.builder.api.TumblrApi;
import org.scribe.builder.api.TwitterApi;
import org.scribe.builder.api.VimeoApi;
import org.scribe.builder.api.YahooApi;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import com.sap.sse.ServerInfo;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;
import com.sap.sse.mail.MailService;
import com.sap.sse.replication.OperationExecutionListener;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.replication.OperationWithResultWithIdWrapper;
import com.sap.sse.replication.OperationsToMasterSender;
import com.sap.sse.replication.OperationsToMasterSendingQueue;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.security.Action;
import com.sap.sse.security.ClientUtils;
import com.sap.sse.security.GithubApi;
import com.sap.sse.security.InstagramApi;
import com.sap.sse.security.OAuthRealm;
import com.sap.sse.security.SecurityInitializationCustomizer;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SessionCacheManager;
import com.sap.sse.security.SessionUtils;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.Credential;
import com.sap.sse.security.interfaces.OAuthToken;
import com.sap.sse.security.interfaces.SocialSettingsKeys;
import com.sap.sse.security.interfaces.UserImpl;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.operations.AclAddPermissionOperation;
import com.sap.sse.security.operations.AclPutPermissionsOperation;
import com.sap.sse.security.operations.AclRemovePermissionOperation;
import com.sap.sse.security.operations.AddPermissionForUserOperation;
import com.sap.sse.security.operations.AddRoleForUserOperation;
import com.sap.sse.security.operations.AddSettingOperation;
import com.sap.sse.security.operations.AddUserToUserGroupOperation;
import com.sap.sse.security.operations.CreateRoleDefinitionOperation;
import com.sap.sse.security.operations.CreateUserGroupOperation;
import com.sap.sse.security.operations.CreateUserOperation;
import com.sap.sse.security.operations.DeleteAclOperation;
import com.sap.sse.security.operations.DeleteOwnershipOperation;
import com.sap.sse.security.operations.DeleteRoleDefinitionOperation;
import com.sap.sse.security.operations.DeleteUserGroupOperation;
import com.sap.sse.security.operations.DeleteUserOperation;
import com.sap.sse.security.operations.PutRoleDefinitionToUserGroupOperation;
import com.sap.sse.security.operations.RemoveAccessTokenOperation;
import com.sap.sse.security.operations.RemovePermissionForUserOperation;
import com.sap.sse.security.operations.RemoveRoleDefinitionFromUserGroupOperation;
import com.sap.sse.security.operations.RemoveRoleFromUserOperation;
import com.sap.sse.security.operations.RemoveUserFromUserGroupOperation;
import com.sap.sse.security.operations.ResetPasswordOperation;
import com.sap.sse.security.operations.SetAccessTokenOperation;
import com.sap.sse.security.operations.SetDefaultTenantForServerForUserOperation;
import com.sap.sse.security.operations.SetEmptyAccessControlListOperation;
import com.sap.sse.security.operations.SetOwnershipOperation;
import com.sap.sse.security.operations.SetPreferenceOperation;
import com.sap.sse.security.operations.SetSettingOperation;
import com.sap.sse.security.operations.UnsetPreferenceOperation;
import com.sap.sse.security.operations.UpdateRoleDefinitionOperation;
import com.sap.sse.security.operations.UpdateSimpleUserEmailOperation;
import com.sap.sse.security.operations.UpdateSimpleUserPasswordOperation;
import com.sap.sse.security.operations.UpdateUserPropertiesOperation;
import com.sap.sse.security.operations.UpdateUserSubscriptionOperation;
import com.sap.sse.security.operations.ValidateEmailOperation;
import com.sap.sse.security.persistence.PersistenceFactory;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.Account.AccountType;
import com.sap.sse.security.shared.AdminRole;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.HasPermissionsProvider;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.PermissionChecker;
import com.sap.sse.security.shared.PredefinedRoles;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.RolePrototype;
import com.sap.sse.security.shared.SecurityAccessControlList;
import com.sap.sse.security.shared.Subscription;
import com.sap.sse.security.shared.SubscriptionPlan;
import com.sap.sse.security.shared.SubscriptionPlanHolder;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.UserRole;
import com.sap.sse.security.shared.UsernamePasswordAccount;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.PermissionAndRoleAssociation;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.util.ClearStateTestSupport;

public class SecurityServiceImpl implements ReplicableSecurityService, ClearStateTestSupport {

    private static final Logger logger = Logger.getLogger(SecurityServiceImpl.class.getName());

    private static final String ADMIN_DEFAULT_PASSWORD = "admin";
    
    // TODO remove, once we allow denied ACLs again
    private static final boolean supportDeniedActions = false;

    private final Set<String> migratedHasPermissionTypes = new ConcurrentSkipListSet<>();;

    private CachingSecurityManager securityManager;
    
    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/StringMessages";
    private static final ResourceBundleStringMessagesImpl messages = new ResourceBundleStringMessagesImpl(
            SecurityServiceImpl.STRING_MESSAGES_BASE_NAME, SecurityServiceImpl.class.getClassLoader(),
            StandardCharsets.UTF_8.name());

    /**
     * A cache manager that the {@link SessionCacheManager} delegates to. This way, multiple Shiro configurations can
     * share the cache manager provided as a singleton within this bundle instance. The cache manager is replicating,
     * forwarding changes to the caches to all replicas registered.
     */
    private final ReplicatingCacheManager cacheManager;
    
    private UserStore store;
    private AccessControlStore accessControlStore;
    
    private boolean isInitialOrMigration;
    private boolean isNewServer;
    
    private final ServiceTracker<MailService, MailService> mailServiceTracker;
    private final ConcurrentMap<OperationExecutionListener<ReplicableSecurityService>, OperationExecutionListener<ReplicableSecurityService>> operationExecutionListeners;

    /**
     * The master from which this replicable is currently replicating, or <code>null</code> if this replicable is not currently
     * replicated from any master.
     */
    private ReplicationMasterDescriptor replicatingFromMaster;
    
    private Set<OperationWithResultWithIdWrapper<?, ?>> operationsSentToMasterForReplication;
    
    private volatile boolean currentlyFillingFromInitialLoad;
    
    private ThreadLocal<Boolean> currentlyApplyingOperationReceivedFromMaster = ThreadLocal.withInitial(() -> false);

    private ThreadLocal<UserGroup> temporaryDefaultTenant = new InheritableThreadLocal<>();
    
    /**
     * This field is expected to be set by the {@link ReplicationService} once it has "adopted" this replicable.
     * The {@link ReplicationService} "injects" this service so it can be used here as a delegate for the
     * {@link OperationsToMasterSendingQueue#scheduleForSending(OperationWithResult, OperationsToMasterSender)}
     * method.
     */
    private OperationsToMasterSendingQueue unsentOperationsToMasterSender;

    private static Ini shiroConfiguration;

    private final HasPermissionsProvider hasPermissionsProvider;

    private String sharedAcrossSubdomainsOf;
    
    private String baseUrlForCrossDomainStorage;
    
    private final transient Set<SecurityInitializationCustomizer> customizers = ConcurrentHashMap.newKeySet();
    
    static {
        shiroConfiguration = new Ini();
        shiroConfiguration.loadFromPath("classpath:shiro.ini");
    }
    
    public SecurityServiceImpl(UserStore userStore, AccessControlStore accessControlStore) {
        this(null, userStore, accessControlStore);
    }

    /**
     * @param mailProperties
     *            must not be <code>null</code>
     */
    public SecurityServiceImpl(ServiceTracker<MailService, MailService> mailServiceTracker, UserStore userStore, AccessControlStore accessControlStore) {
        this(mailServiceTracker, userStore, accessControlStore, null);
    }
    
    /**
     * Creates a security service that is not shared across subdomains, therefore leading to the use of the full
     * domain through which its services are requested for {@code Document.domain} and hence for the browser local
     * storage, session storage and the Shiro {@code JSESSIONID} cookie's domain.
     * 
     * @param setAsActivatorSecurityService
     *            when <code>true</code>, the {@link Activator#setSecurityService(com.sap.sse.security.SecurityService)}
     *            will be called with this new instance as argument so that the cache manager can already be accessed
     *            when the security manager is created. {@link ReplicatingCacheManager#getCache(String)} fetches the
     *            activator's security service and passes it to the cache entries created. They need it, in turn, for
     *            replication.
     */
    public SecurityServiceImpl(ServiceTracker<MailService, MailService> mailServiceTracker, UserStore userStore,
            AccessControlStore accessControlStore, HasPermissionsProvider hasPermissionsProvider) {
        this(mailServiceTracker, userStore, accessControlStore, hasPermissionsProvider,
                /* sharedAcrossSubdomainsOf */ null, /* baseUrlForCrossDomainStorage */ null);
    }
    
    /**
     * Like {@link #SecurityServiceImpl(ServiceTracker, UserStore, AccessControlStore, HasPermissionsProvider)}, only that additionally
     * a parent domain can be specified in {@code isSharedAcrossSubdomains}, such as, e.g., {@code "sapsailing.com"}, across which
     * the browser local and session store shall be shared and for which sessions identified by the {@code JSESSIONID} cookie shall
     * be shared as well.
     */
    public SecurityServiceImpl(ServiceTracker<MailService, MailService> mailServiceTracker, UserStore userStore,
            AccessControlStore accessControlStore, HasPermissionsProvider hasPermissionsProvider,
            String sharedAcrossSubdomainsOf, String baseUrlForCrossDomainStorage) {
        logger.info("Initializing Security Service with user store " + userStore);
        this.currentlyFillingFromInitialLoad = false;
        this.currentlyFillingFromInitialLoad = false;
        operationsSentToMasterForReplication = new HashSet<>();
        this.sharedAcrossSubdomainsOf = sharedAcrossSubdomainsOf;
        this.baseUrlForCrossDomainStorage = baseUrlForCrossDomainStorage;
        this.operationExecutionListeners = new ConcurrentHashMap<>();
        this.store = userStore;
        this.accessControlStore = accessControlStore;
        this.mailServiceTracker = mailServiceTracker;
        this.hasPermissionsProvider = hasPermissionsProvider;
        cacheManager = loadReplicationCacheManagerContents();
        final Factory<SecurityManager> factory = new WebIniSecurityManagerFactory(shiroConfiguration);
        logger.info("Loaded shiro.ini file from: classpath:shiro.ini");
        final StringBuilder logMessage = new StringBuilder("[urls] section from Shiro configuration:");
        final Section urlsSection = shiroConfiguration.getSection("urls");
        if (urlsSection != null) {
            for (Entry<String, String> e : urlsSection.entrySet()) {
                logMessage.append("\n");
                logMessage.append(e.getKey());
                logMessage.append(": ");
                logMessage.append(e.getValue());
            }
        }
        logger.info(logMessage.toString());
        System.setProperty("java.net.useSystemProxies", "true");
        final CachingSecurityManager securityManager = (CachingSecurityManager) factory.getInstance();
        logger.info("Created: " + securityManager);
        SecurityUtils.setSecurityManager(securityManager);
        this.securityManager = securityManager;
    }

    private ReplicatingCacheManager loadReplicationCacheManagerContents() {
        logger.info("Loading session cache manager contents");
        int count = 0;
        final ReplicatingCacheManager result = new ReplicatingCacheManager();
        for (Entry<String, Set<Session>> cacheNameAndSessions : PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory().loadSessionsByCacheName().entrySet()) {
            final String cacheName = cacheNameAndSessions.getKey();
            final ReplicatingCache<Object, Object> cache = (ReplicatingCache<Object, Object>) result.getCache(cacheName, this);
            for (final Session session : cacheNameAndSessions.getValue()) {
                cache.put(session.getId(), session, /* store */ false);
                count++;
            }
        }
        logger.info("Loaded "+count+" sessions");
        return result;
    }

    @Override
    public boolean isCurrentlyFillingFromInitialLoad() {
        return currentlyFillingFromInitialLoad;
    }

    @Override
    public void setCurrentlyFillingFromInitialLoad(boolean currentlyFillingFromInitialLoad) {
        this.currentlyFillingFromInitialLoad = currentlyFillingFromInitialLoad;
    }

    @Override
    public boolean isCurrentlyApplyingOperationReceivedFromMaster() {
        return currentlyApplyingOperationReceivedFromMaster.get();
    }

    @Override
    public void setCurrentlyApplyingOperationReceivedFromMaster(boolean currentlyApplyingOperationReceivedFromMaster) {
        this.currentlyApplyingOperationReceivedFromMaster.set(currentlyApplyingOperationReceivedFromMaster);
    }

    @Override
    public void initialize() {
        initEmptyStore();
        initEmptyAccessControlStore();
    }

    /**
     * Creates a default "admin" user with initial password "admin" and initial role "admin" if the user <code>store</code>
     * is empty.
     */
    private void initEmptyStore() {
        final AdminRole adminRolePrototype = AdminRole.getInstance();
        RoleDefinition adminRoleDefinition = getRoleDefinition(adminRolePrototype.getId());
        assert adminRoleDefinition != null;
        try {
            isInitialOrMigration = false;
            if (!store.hasUsers()) {
                isInitialOrMigration = true;
                logger.info("No users found, creating default user \""+UserStore.ADMIN_USERNAME+"\" with password \""+ADMIN_DEFAULT_PASSWORD+"\"");
                final User adminUser = createSimpleUser(UserStore.ADMIN_USERNAME, "nobody@sapsailing.com",
                        ADMIN_DEFAULT_PASSWORD,
                        /* fullName */ null, /* company */ null, Locale.ENGLISH, /* validationBaseURL */ null,
                        null);
                setOwnership(adminUser.getIdentifier(), adminUser, null);
                Role adminRole = new Role(adminRoleDefinition);
                addRoleForUserAndSetUserAsOwner(adminUser, adminRole);
                // add new admin user to server group and make server group the default creation group for the admin user:
                final UserGroup defaultTenant = getServerGroup();
                addUserToUserGroup(defaultTenant, adminUser);
                setDefaultTenantForCurrentServerForUser(adminUser.getName(), defaultTenant.getId());
            }
            if (store.getUserByName(SecurityService.ALL_USERNAME) == null) {
                isInitialOrMigration = true;
                isNewServer = true;
                logger.info(SecurityService.ALL_USERNAME + " not found -> creating it now");
                User allUser = apply(new CreateUserOperation(SecurityService.ALL_USERNAME, null));
                // <all> user is explicitly not owned by itself because this would enable anybody to modify this user
                setOwnership(allUser.getIdentifier(), null, getServerGroup());
                // The permission to create new users is initially added but not recreated on server start if the admin removed it in the meanwhile.
                // This allows servers to be configured to not permit self-registration of new users but only users being managed by an admin user.
                WildcardPermission createUserPermission = SecuredSecurityTypes.USER.getPermission(DefaultActions.CREATE);
                addPermissionForUser(ALL_USERNAME, createUserPermission);
                QualifiedObjectIdentifier qualifiedTypeIdentifierForPermission = SecuredSecurityTypes.PERMISSION_ASSOCIATION
                        .getQualifiedObjectIdentifier(PermissionAndRoleAssociation.get(createUserPermission, allUser));
                // Permission association is owned by the server tenant.
                // This typically ensures that the server admin is able to remove the association.
                setOwnership(qualifiedTypeIdentifierForPermission, null, getServerGroup());
            }
            if (isInitialOrMigration) {
                // predefined roles are meant to be publicly readable
                for (UUID predefinedRoleId : getPredefinedRoleIds()) {
                    final RoleDefinition predefinedRole = getRoleDefinition(predefinedRoleId);
                    if (predefinedRole != null) {
                        addToAccessControlList(predefinedRole.getIdentifier(), null, DefaultActions.READ.name());
                    }
                }
            }
        } catch (UserManagementException | MailException | UserGroupManagementException e) {
            logger.log(Level.SEVERE,
                    "Exception while creating default " + UserStore.ADMIN_USERNAME + " and " + SecurityService.ALL_USERNAME + " user", e);
        }
    }
    
    private Iterable<UUID> getPredefinedRoleIds() {
        final Set<UUID> predefinedRoleIds = new HashSet<>();
        predefinedRoleIds.add(AdminRole.getInstance().getId());
        predefinedRoleIds.add(UserRole.getInstance().getId());
        for (final PredefinedRoles otherPredefinedRole : PredefinedRoles.values()) {
            predefinedRoleIds.add(otherPredefinedRole.getId());
        }
        return predefinedRoleIds;
    }
    
    private void initEmptyAccessControlStore() {
    }

    private MailService getMailService() {
        return mailServiceTracker == null ? null : mailServiceTracker.getService();
    }

    @Override
    public void sendMail(String username, String subject, String body) throws MailException {
        final User user = getUserByName(username);
        if (user != null) {
            final String toAddress = user.getEmail();
            if (toAddress != null) {
                MailService mailService = getMailService();
                if (mailService == null) {
                    logger.warning(String.format("Could not send mail to user %s: no MailService found", username));
                } else {
                    getMailService().sendMail(toAddress, subject, body);
                }
            }
        }
    }
    
    @Override
    public void resetPassword(final String username, String passwordResetBaseURL) throws UserManagementException, MailException {
        final User user = store.getUserByName(username);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        if (!user.isEmailValidated()) {
            throw new UserManagementException(UserManagementException.CANNOT_RESET_PASSWORD_WITHOUT_VALIDATED_EMAIL);
        }
        final String passwordResetSecret = user.createRandomSecret();
        apply(new ResetPasswordOperation(username, passwordResetSecret));
        Map<String, String> urlParameters = new HashMap<>();
        try {
            urlParameters.put("u", URLEncoder.encode(user.getName(), "UTF-8"));
            urlParameters.put("e", URLEncoder.encode(user.getEmail(), "UTF-8"));
            urlParameters.put("s", URLEncoder.encode(passwordResetSecret, "UTF-8"));
            final StringBuilder url = buildURL(passwordResetBaseURL, urlParameters);
            new Thread("sending password reset e-mail to user " + username) {
                @Override
                public void run() {
                    try {
                        sendMail(user.getName(), "Password Reset",
                                "Please click on the link below to reset your password for user " + user.getName()
                                        + ".\n   " + url.toString());
                    } catch (MailException e) {
                        logger.log(Level.SEVERE, "Error sending mail for password reset of user " + user.getName()
                                + " to address " + user.getEmail(), e);
                    }
                }
            }.start();
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE,
                    "Internal error: encoding UTF-8 not found. Couldn't send e-mail to user " + user.getName()
                            + " at e-mail address " + user.getEmail(), e);
        }
    }

    @Override
    public Void internalResetPassword(String username, String passwordResetSecret) {
        getUserByName(username).startPasswordReset(passwordResetSecret);
        return null;
    }

    @Override
    public CachingSecurityManager getSecurityManager() {
        return this.securityManager;
    }

    @Override
    public OwnershipAnnotation getOwnership(QualifiedObjectIdentifier idOfOwnedObjectAsString) {
        return accessControlStore.getOwnership(idOfOwnedObjectAsString);
    }
    
    public UserGroup getDefaultTenantForUser(User user) {
        UserGroup specificTenant = temporaryDefaultTenant.get();
        if (specificTenant == null) {
            specificTenant = user.getDefaultTenant(ServerInfo.getName());
            if (specificTenant == null) {
                String defaultTenantName = getDefaultTenantNameForUsername(user.getName());
                specificTenant = getUserGroupByName(defaultTenantName);
            }
        }
        return specificTenant;
    }

    @Override
    public UserGroup getDefaultTenantForCurrentUser() {
        if (SecurityUtils.getSecurityManager() != null && getCurrentUser() == null) {
            return null;
        }
        return getDefaultTenantForUser(getCurrentUser());
    }

    @Override
    public RoleDefinition getRoleDefinition(UUID idOfRoleDefinition) {
        return store.getRoleDefinition(idOfRoleDefinition);
    }

    /**
     * Returns a list of all existing access control lists. This is possibly not complete in the sense
     * that there is a access control list for every access controlled data object.
     */
    @Override
    public Iterable<AccessControlListAnnotation> getAccessControlLists() {
        return accessControlStore.getAccessControlLists();
    }

    @Override
    public AccessControlListAnnotation getAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObjectAsString) {
        return accessControlStore.getAccessControlList(idOfAccessControlledObjectAsString);
    }

    /**
     * @param id Has to be globally unique
     */
    private SecurityService setEmptyAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObjectAsString) {
        return setEmptyAccessControlList(idOfAccessControlledObjectAsString, /* display name of access-controlled object */ null);
    }

    /**
     * @param id Has to be globally unique
     */
    private SecurityService setEmptyAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObjectAsString, String displayNameOfAccessControlledObject) {
        apply(new SetEmptyAccessControlListOperation(idOfAccessControlledObjectAsString, displayNameOfAccessControlledObject));
        return this;
    }

    @Override
    public Void internalSetEmptyAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject, String displayNameOfAccessControlledObject) {
        accessControlStore.setEmptyAccessControlList(idOfAccessControlledObject, displayNameOfAccessControlledObject);
        return null;
    }

    @Override
    public AccessControlList overrideAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject,
            Map<UserGroup, Set<String>> permissionMap) {
        return overrideAccessControlList(idOfAccessControlledObject, permissionMap, /* displayNameOfAccessControlledObject */ null);
    }

    @Override
    public AccessControlList overrideAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject,
            Map<UserGroup, Set<String>> permissionMap, String displayNameOfAccessControlledObject) {
        // TODO remove, once we allow denied ACLs again
        if (!supportDeniedActions) {
            for (Set<String> actions : permissionMap.values()) {
                for (String action : actions) {
                    if (SecurityAccessControlList.isDeniedAction(action)) {
                        throw new IllegalArgumentException("Adding denied actions to an ACL is not allowed");
                    }
                }
            }
        }
        setEmptyAccessControlList(idOfAccessControlledObject, displayNameOfAccessControlledObject);
        
        for (Map.Entry<UserGroup, Set<String>> entry : permissionMap.entrySet()) {
            final UserGroup userGroup = entry.getKey();
            final Set<String> actionsToSet;
            if (userGroup == null) {
                // filter any denied action for anonymous user
                actionsToSet = entry.getValue().stream()
                        .filter(action -> !SecurityAccessControlList.isDeniedAction(action))
                        .collect(Collectors.toSet());
            } else {
                actionsToSet = entry.getValue();
            }
            
            final UUID userGroupId = userGroup == null ? null : userGroup.getId();
            // avoid the UserGroup object having to be serialized with the operation by using the ID
            apply(new AclPutPermissionsOperation(idOfAccessControlledObject, userGroupId, actionsToSet));
        }
        return accessControlStore.getAccessControlList(idOfAccessControlledObject).getAnnotation();
    }
    
    @Override
    public Void internalAclPutPermissions(QualifiedObjectIdentifier idOfAccessControlledObject, UUID groupId, Set<String> actions) {
        accessControlStore.setAclPermissions(idOfAccessControlledObject, getUserGroup(groupId), actions);
        return null;
    }

    /*
     * @param name The name of the user group to add
     */
    @Override
    public AccessControlList addToAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject,
            UserGroup group, String action) {
        // TODO remove, once we allow denied ACLs again
        if (!supportDeniedActions && SecurityAccessControlList.isDeniedAction(action)) {
            throw new IllegalArgumentException("Adding denied actions to an ACL is not allowed");
        }
        if (getAccessControlList(idOfAccessControlledObject) == null) {
            setEmptyAccessControlList(idOfAccessControlledObject);
        }
        final UUID groupId = group == null ? null : group.getId();
        apply(new AclAddPermissionOperation(idOfAccessControlledObject, groupId, action));
        return accessControlStore.getAccessControlList(idOfAccessControlledObject).getAnnotation();
    }

    @Override
    public Void internalAclAddPermission(QualifiedObjectIdentifier idOfAccessControlledObjectAsString, UUID groupId, String permission) {
        accessControlStore.addAclPermission(idOfAccessControlledObjectAsString, getUserGroup(groupId), permission);
        return null;
    }

    /*
     * @param name The name of the user group to remove
     */
    @Override
    public AccessControlList removeFromAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObjectAsString,
            UserGroup group, String permission) {
        final AccessControlList result;
        if (getAccessControlList(idOfAccessControlledObjectAsString) != null) {
            final UUID groupId = group == null ? null : group.getId();
            apply(new AclRemovePermissionOperation(idOfAccessControlledObjectAsString, groupId, permission));
            result = accessControlStore.getAccessControlList(idOfAccessControlledObjectAsString).getAnnotation();
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Void internalAclRemovePermission(QualifiedObjectIdentifier idOfAccessControlledObjectAsString, UUID groupId, String permission) {
        accessControlStore.removeAclPermission(idOfAccessControlledObjectAsString, getUserGroup(groupId), permission);
        return null;
    }

    @Override
    public void deleteAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObjectAsString) {
        if (getAccessControlList(idOfAccessControlledObjectAsString) != null) {
            apply(new DeleteAclOperation(idOfAccessControlledObjectAsString));
        }
    }

    @Override
    public Void internalDeleteAcl(QualifiedObjectIdentifier idOfAccessControlledObjectAsString) {
        accessControlStore.removeAccessControlList(idOfAccessControlledObjectAsString);
        return null;
    }

    @Override
    public Ownership setOwnership(QualifiedObjectIdentifier idOfOwnedObjectAsString, User userOwner,
            UserGroup tenantOwner) {
        return setOwnership(idOfOwnedObjectAsString, userOwner, tenantOwner, /* displayNameOfOwnedObject */ null);
    }

    @Override
    public Ownership setOwnership(QualifiedObjectIdentifier idOfOwnedObjectAsString, User userOwner,
            UserGroup tenantOwner, String displayNameOfOwnedObject) {
        if (userOwner == null && tenantOwner == null) {
            throw new IllegalArgumentException("No owner is not valid, would create non changeable object");
        }
        final UUID tenantId = tenantOwner == null ? null : tenantOwner.getId();
        final String userOwnerName = userOwner == null ? null : userOwner.getName();

        return apply(new SetOwnershipOperation(idOfOwnedObjectAsString, userOwnerName, tenantId,
                displayNameOfOwnedObject));
    }
    
    @Override
    public Ownership internalSetOwnership(QualifiedObjectIdentifier idAsString, String userOwnerName, UUID tenantOwnerId, String displayName) {
        return accessControlStore.setOwnership(idAsString, getUserByName(userOwnerName), getUserGroup(tenantOwnerId), displayName).getAnnotation();
    }

    @Override
    public void deleteOwnership(QualifiedObjectIdentifier idOfOwnedObjectAsString) {
        if (getOwnership(idOfOwnedObjectAsString) != null) {
            apply(new DeleteOwnershipOperation(idOfOwnedObjectAsString));
        }
    }

    @Override
    public Void internalDeleteOwnership(QualifiedObjectIdentifier idOfOwnedObjectAsString) {
        accessControlStore.removeOwnership(idOfOwnedObjectAsString);
        return null;
    }

    @Override
    public Iterable<UserGroup> getUserGroupList() {
        return store.getUserGroups();
    }

    @Override
    public UserGroup getUserGroup(UUID id) {
        return store.getUserGroup(id);
    }

    @Override
    public UserGroup getUserGroupByName(String name) {
        return store.getUserGroupByName(name);
    }

    @Override
    public Iterable<UserGroup> getUserGroupsOfUser(User user) {
        return store.getUserGroupsOfUser(user);
    }

    @Override
    public UserGroup createUserGroup(UUID id, String name) throws UserGroupManagementException {
        return createUserGroupWithInitialUser(id, name, getCurrentUser());
    }
    
    private UserGroup createUserGroupWithInitialUser(UUID id, String name, User initialUser) {
        logger.info("Creating user group " + name + " with ID " + id);
        apply(new CreateUserGroupOperation(id, name));
        final UserGroup userGroup = store.getUserGroup(id);
        if (initialUser != null) {
            logger.info("Adding initial user " + initialUser + " to group " + userGroup);
            addUserToUserGroup(userGroup, initialUser);
            addUserRoleForGroupToUser(userGroup, initialUser);
        }
        return userGroup;
    }

    @Override
    public Void internalCreateUserGroup(UUID id, String name) throws UserGroupManagementException {
        store.createUserGroup(id, name);
        return null;
    }

    @Override
    public void addUserToUserGroup(UserGroup userGroup, User user) {
        logger.info("Adding user "+user.getName()+" to group "+userGroup.getName());
        userGroup.add(user);
        final UUID groupId = userGroup.getId();
        final String username = user.getName();
        apply(new AddUserToUserGroupOperation(groupId, username));
    }

    @Override
    public Void internalAddUserToUserGroup(UUID groupId, String username) {
        final UserGroup userGroup = getUserGroup(groupId);
        userGroup.add(getUserByName(username));
        store.updateUserGroup(userGroup);
        return null;
    }
    
    @Override
    public Void internalRemoveUserFromUserGroup(UUID groupId, String username) {
        final UserGroup userGroup = getUserGroup(groupId);
        userGroup.remove(getUserByName(username));
        store.updateUserGroup(userGroup);
        return null;
    }
    
    @Override
    public void removeUserFromUserGroup(UserGroup userGroup, User user) {
        logger.info("Removing user "+user.getName()+" from group "+userGroup.getName());
        userGroup.remove(user);
        final UUID userGroupId = userGroup.getId();
        final String username = user.getName();
        apply(new RemoveUserFromUserGroupOperation(userGroupId, username));
    }

    @Override
    public void putRoleDefinitionToUserGroup(UserGroup userGroup, RoleDefinition roleDefinition, boolean forAll) {
        logger.info("Adding role definition " + roleDefinition.getName() + "(forAll = " + forAll + ") to group "
                + userGroup.getName());
        apply(new PutRoleDefinitionToUserGroupOperation(userGroup.getId(), roleDefinition.getId(), forAll));
    }

    @Override
    public Void internalPutRoleDefinitionToUserGroup(UUID groupId, UUID roleDefinitionId, boolean forAll)
            throws UserGroupManagementException {
        final UserGroup userGroup = getUserGroup(groupId);
        userGroup.put(getRoleDefinition(roleDefinitionId), forAll);
        store.updateUserGroup(userGroup);
        return null;
    }

    @Override
    public void removeRoleDefintionFromUserGroup(UserGroup userGroup, RoleDefinition roleDefinition) {
        logger.info("Removing role definition " + roleDefinition.getName() + " from group " + userGroup.getName());
        apply(new RemoveRoleDefinitionFromUserGroupOperation(userGroup.getId(), roleDefinition.getId()));
    }

    @Override
    public Void internalRemoveRoleDefinitionFromUserGroup(UUID groupId, UUID roleDefinitionId)
            throws UserGroupManagementException {
        final UserGroup userGroup = getUserGroup(groupId);
        userGroup.remove(getRoleDefinition(roleDefinitionId));
        store.updateUserGroup(userGroup);
        return null;
    }

    @Override
    public void deleteUserGroup(UserGroup userGroup) throws UserGroupManagementException {
        logger.info("Removing user group "+userGroup.getName());
        apply(new DeleteUserGroupOperation(userGroup.getId()));
    }
    
    @Override
    public Void internalDeleteUserGroup(UUID groupId) throws UserGroupManagementException {
        final UserGroup userGroup = getUserGroup(groupId);
        if (userGroup == null) {
            logger.warning("Strange: the user group with ID "+groupId+" which is about to be deleted couldn't be found");
        } else {
            accessControlStore.removeAllOwnershipsFor(userGroup);
            store.removeAllQualifiedRolesForUserGroup(userGroup);
            store.deleteUserGroup(userGroup);
        }
        return null;
    }

    @Override
    public Iterable<User> getUserList() {
        return store.getUsers();
    }

    @Override
    public String login(String username, String password) throws AuthenticationException {
        String redirectUrl;
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        logger.info("Trying to login: " + username);
        Subject subject = SecurityUtils.getSubject();
        subject.login(token);
        HttpServletRequest httpRequest = WebUtils.getHttpRequest(subject);
        SavedRequest savedRequest = WebUtils.getSavedRequest(httpRequest);
        if (savedRequest != null) {
            redirectUrl = savedRequest.getRequestUrl();
        } else {
            redirectUrl = "";
        }
        logger.info("Redirecturl: " + redirectUrl);
        return redirectUrl;
    }
    
    @Override
    public void logout() {
        Subject subject = SecurityUtils.getSubject();
        logger.info("Logging out");
        subject.logout();
    }

    @Override
    public User getUserByName(String name) {
        return store.getUserByName(name);
    }
    
    @Override
    public User getUserByAccessToken(String accessToken) {
        return store.getUserByAccessToken(accessToken);
    }

    @Override
    public User getUserByEmail(String email) {
        return store.getUserByEmail(email);
    }

    @Override
    public User createSimpleUser(final String username, final String email, String password, String fullName,
            String company, Locale locale, final String validationBaseURL, UserGroup groupOwningUser)
            throws UserManagementException, MailException, UserGroupManagementException {
        logger.info("Creating user "+username);
        if (store.getUserByName(username) != null) {
            logger.warning("User "+username+" already exists");
            throw new UserManagementException(UserManagementException.USER_ALREADY_EXISTS);
        }
        if (username == null || username.length() < 3) {
            throw new UserManagementException(UserManagementException.USERNAME_DOES_NOT_MEET_REQUIREMENTS);
        } else if (password == null || password.length() < 5) {
            throw new UserManagementException(UserManagementException.PASSWORD_DOES_NOT_MEET_REQUIREMENTS);
        }
        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        byte[] salt = rng.nextBytes().getBytes();
        String hashedPasswordBase64 = hashPassword(password, salt);
        UsernamePasswordAccount upa = new UsernamePasswordAccount(username, hashedPasswordBase64, salt);
        final User result = apply(new CreateUserOperation(username, email, upa)); // This also replicated the user creation
        addUserRoleToUser(result);
        final UserGroup tenant = getOrCreateTenantForUser(result);
        setDefaultTenantForCurrentServerForUser(username, tenant.getId());
        // the new user becomes its owner to ensure the user role is working correctly
        // the default tenant is the owning tenant to allow users having admin role for a specific server tenant to also be able to delete users
        apply(new SetOwnershipOperation(result.getIdentifier(), username, groupOwningUser==null?null:groupOwningUser.getId(), username));
        updateUserProperties(username, fullName, company, locale);
        // email has been set during creation already; the following call will trigger the e-mail validation process
        updateSimpleUserEmail(username, email, validationBaseURL);
        return result;
    }

    private void addUserRoleToUser(final User user) {
        addRoleForUserAndSetUserAsOwner(user, new Role(store.getRoleDefinitionByPrototype(UserRole.getInstance()),
                /* tenant qualifier */ null, /* user qualifier */ user));
    }
    
    private void addUserRoleForGroupToUser(final UserGroup group, final User user) {
        addRoleForUserAndSetUserAsOwner(user, new Role(store.getRoleDefinitionByPrototype(UserRole.getInstance()),
                /* tenant qualifier */ group, /* user qualifier */ null));
    }
    
    private UserGroup getOrCreateTenantForUser(User user) throws UserGroupManagementException {
        final String username = user.getName();
        final String defaultTenantNameForUsername = getDefaultTenantNameForUsername(username);
        UserGroup tenant = store.getUserGroupByName(defaultTenantNameForUsername);
        if (tenant != null) {
            logger.info("Found existing tenant "+defaultTenantNameForUsername+" to be used as default tenant for new user "+username);
        } else {
            logger.info("Creating user group "+defaultTenantNameForUsername+" as default tenant for new user "+username);
            tenant = createTenantForUser(defaultTenantNameForUsername, user);
        }
        return tenant;
    }
    
    private UserGroup createTenantForUser(String defaultTenantNameForUsername, User user) throws UserGroupManagementException {
        final UserGroup tenant = createUserGroupWithInitialUser(UUID.randomUUID(), defaultTenantNameForUsername, user);
        setOwnership(tenant.getIdentifier(), user, tenant);
        return tenant;
    }

    @Override
    public User internalCreateUser(String username, String email, Account... accounts) throws UserManagementException {
        final User result = store.createUser(username, email, accounts); // TODO: get the principal as owner
        return result;
    }

    private String getDefaultTenantNameForUsername(final String username) {
        return username + TENANT_SUFFIX;
    }

    @Override
    public void updateSimpleUserPassword(String username, String newPassword) throws UserManagementException {
        final User user = store.getUserByName(username);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        updateSimpleUserPassword(user, newPassword);
    }

    private void updateSimpleUserPassword(final User user, String newPassword) throws UserManagementException {
        if (newPassword == null || newPassword.length() < 5) {
            throw new UserManagementException(UserManagementException.PASSWORD_DOES_NOT_MEET_REQUIREMENTS);
        }
        // for non-admins, check that the old password is correct
        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        byte[] salt = rng.nextBytes().getBytes();
        String hashedPasswordBase64 = hashPassword(newPassword, salt);
        apply(new UpdateSimpleUserPasswordOperation(user.getName(), salt, hashedPasswordBase64));
    }
    
    @Override
    public Void internalUpdateSimpleUserPassword(String username, byte[] salt, String hashedPasswordBase64) {
        final User user = getUserByName(username);
        final UsernamePasswordAccount account = (UsernamePasswordAccount) user.getAccount(AccountType.USERNAME_PASSWORD);
        account.setSalt(salt);
        account.setSaltedPassword(hashedPasswordBase64);
        user.passwordWasReset();
        return null;
    }

    @Override
    public void updateUserProperties(String username, String fullName, String company, Locale locale) throws UserManagementException {
        final User user = store.getUserByName(username);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        apply(new UpdateUserPropertiesOperation(username, fullName, company, locale));
    }

    @Override
    public Void internalUpdateUserProperties(String username, String fullName, String company, Locale locale) {
        final User user = store.getUserByName(username);
        user.setFullName(fullName);
        user.setCompany(company);
        user.setLocale(locale);
        store.updateUser(user);
        return null;
    }

    @Override
    public boolean checkPassword(String username, String password) throws UserManagementException {
        final User user = store.getUserByName(username);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        final UsernamePasswordAccount account = (UsernamePasswordAccount) user.getAccount(AccountType.USERNAME_PASSWORD);
        String hashedOldPassword = hashPassword(password, account.getSalt());
        return Util.equalsWithNull(hashedOldPassword, account.getSaltedPassword());
    }
    
    @Override
    public boolean checkPasswordResetSecret(String username, String passwordResetSecret) throws UserManagementException {
        final User user = store.getUserByName(username);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        return Util.equalsWithNull(user.getPasswordResetSecret(), passwordResetSecret);
    }

    @Override
    public void updateSimpleUserEmail(final String username, final String newEmail, final String validationBaseURL) throws UserManagementException {
        final User user = store.getUserByName(username);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        logger.info("Changing e-mail address of user "+username+" to "+newEmail);
        final String validationSecret = user.createRandomSecret();
        apply(new UpdateSimpleUserEmailOperation(username, newEmail, validationSecret));
        if (validationBaseURL != null && newEmail != null && !newEmail.trim().isEmpty()) {
            new Thread("e-mail validation after changing e-mail of user " + username + " to " + newEmail) {
                @Override
                public void run() {
                    try {
                        startEmailValidation(user, validationSecret, validationBaseURL);
                    } catch (MailException e) {
                        logger.log(Level.SEVERE, "Error sending mail to validate e-mail address change for user "
                                + username + " to address " + newEmail, e);
                    }
                }
            }.start();
        }
    }

    @Override
    public Void internalUpdateSimpleUserEmail(final String username, final String newEmail, final String validationSecret) {
        final User user = getUserByName(username);
        user.setEmail(newEmail);
        user.startEmailValidation(validationSecret);
        return null;
    }

    @Override
    public boolean validateEmail(String username, String validationSecret) throws UserManagementException {
        final User user = store.getUserByName(username);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        return apply(new ValidateEmailOperation(username, validationSecret));
    }
    
    @Override
    public Boolean internalValidateEmail(String username, String validationSecret) {
        final User user = store.getUserByName(username);
        final boolean result = user.validate(validationSecret);
        if (result) {
            store.updateUser(user);
        }
        return result;
    }

    /**
     * {@link UserImpl#startEmailValidation(String) Triggers} e-mail validation for the <code>user</code> object and sends out a
     * URL to the user's e-mail that has the validation secret ready for validation by clicking.
     * 
     * @param validationSecret
     *            the result of either {@link UserImpl#startEmailValidation(String)} or {@link UserImpl#setEmail(String)}.
     * @param baseURL
     *            the URL under which the user can reach the e-mail validation service; this URL is required to assemble
     *            a validation URL that is sent by e-mail to the user, to make the user return the validation secret to
     *            the right server again.
     */
    private void startEmailValidation(User user, String validationSecret, String baseURL) throws MailException {
        try {
            Map<String, String> urlParameters = new HashMap<>();
            urlParameters.put("u", URLEncoder.encode(user.getName(), "UTF-8"));
            urlParameters.put("v", URLEncoder.encode(validationSecret, "UTF-8"));
            StringBuilder url = buildURL(baseURL, urlParameters);
            sendMail(user.getName(), messages.get(user.getLocaleOrDefault(), "emailValidationSubject"),
                    new StringBuilder()
                            .append(messages.get(user.getLocaleOrDefault(), "emailValidationMessage", user.getName()))
                            .append("\n").append("   ").append(url.toString()).toString());
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "Internal error: encoding UTF-8 not found. Couldn't send e-mail to user "
                    + user.getName() + " at e-mail address " + user.getEmail(), e);
        }
    }

    public StringBuilder buildURL(String baseURL, Map<String, String> urlParameters) {
        StringBuilder url = new StringBuilder(baseURL==null?"":baseURL);
        // Potentially contained hash is checked to support place-based mail verification
        boolean first = baseURL == null || !baseURL.contains("?") || baseURL.contains("#");
        for (Map.Entry<String, String> e : urlParameters.entrySet()) {
            if (first) {
                url.append('?');
                first = false;
            } else {
                url.append('&');
            }
            url.append(e.getKey());
            url.append('=');
            url.append(e.getValue());
        }
        return url;
    }

    protected String hashPassword(String password, Object salt) {
        return new Sha256Hash(password, salt, 1024).toBase64();
    }

    @Override
    public RoleDefinition createRoleDefinition(UUID roleId, String name) {
        return apply(new CreateRoleDefinitionOperation(roleId, name));
    }

    @Override
    public RoleDefinition internalCreateRoleDefinition(UUID roleId, String name) {
        return store.createRoleDefinition(roleId, name, Collections.emptySet());
    }
    
    @Override
    public void deleteRoleDefinition(RoleDefinition roleDefinition) {
        final UUID roleId = roleDefinition.getId();
        apply(new DeleteRoleDefinitionOperation(roleId));
    }

    @Override
    public Void internalDeleteRoleDefinition(UUID roleId) {
        final RoleDefinition role = store.getRoleDefinition(roleId);
        store.removeRoleDefinition(role);
        return null;
    }

    @Override
    public void updateRoleDefinition(RoleDefinition roleDefinitionWithNewProperties) {
        apply(new UpdateRoleDefinitionOperation(roleDefinitionWithNewProperties));
    }

    @Override
    public Void internalUpdateRoleDefinition(RoleDefinition roleWithNewProperties) {
        final RoleDefinition role = store.getRoleDefinition(roleWithNewProperties.getId());
        role.setName(roleWithNewProperties.getName());
        store.setRoleDefinitionDisplayName(roleWithNewProperties.getId(), role.getName());
        role.setPermissions(roleWithNewProperties.getPermissions());
        store.setRoleDefinitionPermissions(role.getId(), role.getPermissions());
        return null;
    }

    @Override
    public Iterable<RoleDefinition> getRoleDefinitions() {
        Collection<RoleDefinition> result = new ArrayList<>();
        filterObjectsWithPermissionForCurrentUser(DefaultActions.READ,
                store.getRoleDefinitions(), t -> result.add(t));
        return result;
    }
    
    private void addRoleForUserAndSetUserAsOwner(User user, Role role) {
        addRoleForUser(user.getName(), role);
        TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(role, user);
        QualifiedObjectIdentifier qualifiedTypeIdentifier = SecuredSecurityTypes.ROLE_ASSOCIATION
                .getQualifiedObjectIdentifier(associationTypeIdentifier);
        setOwnership(qualifiedTypeIdentifier, user, null);
    }

    @Override
    public void addRoleForUser(User user, Role role) {
        addRoleForUser(user.getName(), role);
    }

    @Override
    public void addRoleForUser(String username, Role role) {
        final UUID roleDefinitionId = role.getRoleDefinition().getId();
        final UUID idOfTenantQualifyingRole = role.getQualifiedForTenant() == null ? null : role.getQualifiedForTenant().getId();
        final String nameOfUserQualifyingRole = role.getQualifiedForUser() == null ? null : role.getQualifiedForUser().getName();
        apply(new AddRoleForUserOperation(username, roleDefinitionId, idOfTenantQualifyingRole, nameOfUserQualifyingRole));
    }

    @Override
    public Void internalAddRoleForUser(String username, UUID roleDefinitionId, UUID idOfTenantQualifyingRole,
            String nameOfUserQualifyingRole) throws UserManagementException {
        store.addRoleForUser(username, new Role(getRoleDefinition(roleDefinitionId),
                getUserGroup(idOfTenantQualifyingRole), getUserByName(nameOfUserQualifyingRole)));
        return null;
    }

    @Override
    public void removeRoleFromUser(User user, Role role) {
        removeRoleFromUser(user.getName(), role);
    }
    
    @Override
    public void removeRoleFromUser(String username, Role role) {
        final UUID roleDefinitionId = role.getRoleDefinition().getId();
        final UUID idOfTenantQualifyingRole = role.getQualifiedForTenant() == null ? null : role.getQualifiedForTenant().getId();
        final String nameOfUserQualifyingRole = role.getQualifiedForUser() == null ? null : role.getQualifiedForUser().getName();
        apply(new RemoveRoleFromUserOperation(username, roleDefinitionId, idOfTenantQualifyingRole, nameOfUserQualifyingRole));
    }

    @Override
    public Void internalRemoveRoleFromUser(String username, UUID roleDefinitionId, UUID idOfTenantQualifyingRole,
            String nameOfUserQualifyingRole) throws UserManagementException {
        store.removeRoleFromUser(username, new Role(getRoleDefinition(roleDefinitionId),
                getUserGroup(idOfTenantQualifyingRole), getUserByName(nameOfUserQualifyingRole)));
        return null;
    }

    @Override
    public void removePermissionFromUser(String username, WildcardPermission permissionToRemove) {
        apply(new RemovePermissionForUserOperation(username, permissionToRemove));
    }

    @Override
    public Void internalRemovePermissionForUser(String username, WildcardPermission permissionToRemove) throws UserManagementException {
        store.removePermissionFromUser(username, permissionToRemove);
        return null;
    }

    @Override
    public void addPermissionForUser(String username, WildcardPermission permissionToAdd) {
        apply(new AddPermissionForUserOperation(username, permissionToAdd));
    }

    @Override
    public Void internalAddPermissionForUser(String username, WildcardPermission permissionToAdd) throws UserManagementException {
        store.addPermissionForUser(username, permissionToAdd);
        return null;
    }

    @Override
    public void deleteUser(String username) throws UserManagementException {
        final User userToDelete = store.getUserByName(username);
        if (userToDelete == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        apply(new DeleteUserOperation(username));
    }

    @Override
    public Void internalDeleteUser(String username) throws UserManagementException {
        User userToDelete = store.getUserByName(username);
        if (userToDelete != null) {
            // remove all permissions the user has
            accessControlStore.removeAllOwnershipsFor(userToDelete);
            store.removeAllQualifiedRolesForUser(userToDelete);
            final String defaultTenantNameForUsername = getDefaultTenantNameForUsername(username);
            final UserGroup defaultTenantUserGroup = getUserGroupByName(defaultTenantNameForUsername);
            if (defaultTenantUserGroup != null) {
                List<User> usersInGroupList = Util.asList(defaultTenantUserGroup.getUsers());
                if (usersInGroupList.size() == 1 && usersInGroupList.contains(userToDelete)) {
                    // no other user is in group, delete it as well
                    try {
                        internalDeleteUserGroup(defaultTenantUserGroup.getId());
                    } catch (UserGroupManagementException e) {
                        logger.log(Level.SEVERE, "Could not delete default tenant for user", e);
                    }
                }
            }
            // also remove from all usergroups
            final Iterable<UserGroup> userGroups = userToDelete.getUserGroups();
            final Collection<UserGroup> userGroupsToLoopOver = new ArrayList<>();
            Util.addAll(userGroups, userGroupsToLoopOver); // avoid concurrent modification exception
            for (UserGroup userGroup : userGroupsToLoopOver) {
                if (userGroup != defaultTenantUserGroup) { // the defaultTenantUserGroup has already been deleted above
                    internalRemoveUserFromUserGroup(userGroup.getId(), userToDelete.getName());
                }
            }
            store.deleteUser(username);
        }
        return null;
    }

    @Override
    public boolean setSetting(String key, Object setting) {
        return apply(new SetSettingOperation(key, setting));
    }

    @Override
    public Boolean internalSetSetting(String key, Object setting) {
        return store.setSetting(key, setting);
    }

    @Override
    public Map<String, Object> getAllSettings() {
        return store.getAllSettings();
    }

    @Override
    public Map<String, Class<?>> getAllSettingTypes() {
        return store.getAllSettingTypes();
    }

    @Override
    public User verifySocialUser(Credential credential) throws UserManagementException {
        OAuthToken otoken = new OAuthToken(credential, credential.getVerifier());
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            try {
                subject.login(otoken);
                logger.info("User [" + subject.getPrincipal().toString() + "] logged in successfully.");
            } catch (UnknownAccountException uae) {
                logger.info("There is no user with username of " + subject.getPrincipal());
                throw new UserManagementException("Invalid credentials!");
            } catch (IncorrectCredentialsException ice) {
                logger.info("Password for account " + subject.getPrincipal() + " was incorrect!");
                throw new UserManagementException("Invalid credentials!");
            } catch (LockedAccountException lae) {
                logger.info("The account for username " + subject.getPrincipal() + " is locked.  "
                        + "Please contact your administrator to unlock it.");
                throw new UserManagementException("Invalid credentials!");
            } catch (AuthenticationException ae) {
                logger.log(Level.SEVERE, ae.getLocalizedMessage());
                throw new UserManagementException("An error occured while authenticating the user!");
            }
        }
        String username = subject.getPrincipal().toString();
        if (username == null) {
            logger.info("Something went wrong while authneticating, check doGetAuthenticationInfo() in "
                    + OAuthRealm.class.getName() + ".");
            throw new UserManagementException("An error occured while authenticating the user!");
        }
        User user = store.getUserByName(username);
        if (user == null) {
            logger.info("Could not find user " + username);
            throw new UserManagementException("An error occured while authenticating the user!");
        }
        return user;
    }

    @Override
    public User getCurrentUser() {
        final User result;
        Subject subject = SecurityUtils.getSubject();
        if (subject == null || !subject.isAuthenticated()) {
            result = null;
        } else {
            Object principal = subject.getPrincipal();
            if (principal == null) {
                result = null;
            } else {
                String username = principal.toString();
                if (username == null || username.length() <= 0) {
                    result = null;
                } else {
                    result = store.getUserByName(username);
                }
            }
        }
        return result;
    }

    @Override
    public String getAuthenticationUrl(Credential credential) throws UserManagementException {
        Token requestToken = null;
        String authorizationUrl = null;
        int authProvider = credential.getAuthProvider();
        OAuthService service = getOAuthService(authProvider);
        if (service == null) {
            throw new UserManagementException("Could not build OAuthService");
        }
        if (authProvider == ClientUtils.TWITTER || authProvider == ClientUtils.YAHOO
                || authProvider == ClientUtils.LINKEDIN || authProvider == ClientUtils.FLICKR
                || authProvider == ClientUtils.IMGUR || authProvider == ClientUtils.TUMBLR
                || authProvider == ClientUtils.VIMEO || authProvider == ClientUtils.GOOGLE) {
            String authProviderName = ClientUtils.getAuthProviderName(authProvider);
            logger.info(authProviderName + " requires Request token first.. obtaining..");
            try {
                requestToken = service.getRequestToken();
                logger.info("Got request token: " + requestToken);
                // we must save in the session. It will be required to
                // get the access token
                SessionUtils.saveRequestTokenToSession(requestToken);
            } catch (Exception e) {
                throw new UserManagementException("Could not get request token for " + authProvider + " "
                        + e.getMessage());
            }
        }
        logger.info("Getting Authorization url...");
        try {
            authorizationUrl = service.getAuthorizationUrl(requestToken);
            // Facebook has optional state var to protect against CSFR.
            // We'll use it
            if (authProvider == ClientUtils.FACEBOOK || authProvider == ClientUtils.GITHUB
                    || authProvider == ClientUtils.INSTAGRAM) {
                String state = UUID.randomUUID().toString();
                authorizationUrl += "&state=" + state;
                SessionUtils.saveStateToSession(state);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new UserManagementException("Could not get Authorization url: ");
        }

        if (authProvider == ClientUtils.FLICKR) {
            authorizationUrl += "&perms=read";
        }

        if (authProvider == ClientUtils.FACEBOOK) {
            authorizationUrl += "&scope=email";
        }

        logger.info("Authorization url: " + authorizationUrl);
        return authorizationUrl;
    }

    private OAuthService getOAuthService(int authProvider) {
        OAuthService service = null;
        switch (authProvider) {
        case ClientUtils.FACEBOOK: {
            service = new ServiceBuilder().provider(FacebookApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_FACEBOOK_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_FACEBOOK_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.GOOGLE: {
            service = new ServiceBuilder().provider(GoogleApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_GOOGLE_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_GOOGLE_APP_SECRET.name(), String.class))
                    .scope(store.getSetting(SocialSettingsKeys.OAUTH_GOOGLE_SCOPE.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();

            break;
        }

        case ClientUtils.TWITTER: {
            service = new ServiceBuilder().provider(TwitterApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_TWITTER_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_TWITTER_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;
        }
        case ClientUtils.YAHOO: {
            service = new ServiceBuilder().provider(YahooApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_YAHOO_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_YAHOO_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.LINKEDIN: {
            service = new ServiceBuilder().provider(LinkedInApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_LINKEDIN_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_LINKEDIN_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.INSTAGRAM: {
            service = new ServiceBuilder().provider(InstagramApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_INSTAGRAM_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_INSTAGRAM_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.GITHUB: {
            service = new ServiceBuilder().provider(GithubApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_GITHUB_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_GITHUB_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;

        }

        case ClientUtils.IMGUR: {
            service = new ServiceBuilder().provider(ImgUrApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_IMGUR_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_IMGUR_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.FLICKR: {
            service = new ServiceBuilder().provider(FlickrApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_FLICKR_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_FLICKR_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.VIMEO: {
            service = new ServiceBuilder().provider(VimeoApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_VIMEO_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_VIMEO_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.WINDOWS_LIVE: {
            // a Scope must be specified
            service = new ServiceBuilder().provider(LiveApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_WINDOWS_LIVE_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_WINDOWS_LIVE_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).scope("wl.basic").build();
            break;
        }

        case ClientUtils.TUMBLR: {
            service = new ServiceBuilder().provider(TumblrApi.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_TUMBLR_LIVE_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_TUMBLR_LIVE_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        case ClientUtils.FOURSQUARE: {
            service = new ServiceBuilder().provider(Foursquare2Api.class)
                    .apiKey(store.getSetting(SocialSettingsKeys.OAUTH_FOURSQUARE_APP_ID.name(), String.class))
                    .apiSecret(store.getSetting(SocialSettingsKeys.OAUTH_FOURSQUARE_APP_SECRET.name(), String.class))
                    .callback(ClientUtils.getCallbackUrl()).build();
            break;
        }

        default: {
            return null;
        }

        }
        return service;
    }

    @Override
    public void addSetting(String key, Class<?> clazz) throws UserManagementException {
        if (!isValidSettingsKey(key)) {
            throw new UserManagementException("Invalid key!");
        }
        apply(new AddSettingOperation(key, clazz));
    }

    @Override
    public Void internalAddSetting(String key, Class<?> clazz) {
        store.addSetting(key, clazz);
        return null;
    }

    public static boolean isValidSettingsKey(String key) {
        char[] characters = key.toCharArray();
        for (char c : characters) {
            if (!Character.isLetter(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void refreshSecurityConfig(ServletContext context) {
        logger.info("Refreshing security configuration!");
        IniWebEnvironment env = (IniWebEnvironment) WebUtils.getRequiredWebEnvironment(context);
        System.out.println("Env: " + env);
        FilterChainResolver resolver = env.getFilterChainResolver();
        System.out.println("Resolver: " + resolver);
        if (resolver instanceof PathMatchingFilterChainResolver) {
            PathMatchingFilterChainResolver pmfcr = (PathMatchingFilterChainResolver) resolver;
            FilterChainManager filterChainManager = pmfcr.getFilterChainManager();
            System.out.println(filterChainManager);

            Set<String> chainNames = filterChainManager.getChainNames();

            System.out.println("Chains:");
            for (String s : chainNames) {
                System.out.println(s + ": " + Arrays.toString(filterChainManager.getChain(s).toArray(new Filter[0])));
            }
        }
    }

    public static Ini getShiroConfiguration() {
        return shiroConfiguration;
    }

    @Override
    public ReplicatingCacheManager getCacheManager() {
        return cacheManager;
    }

    @Override
    public void setPreference(final String username, final String key, final String value) {
        apply(new SetPreferenceOperation(username, key, value));
    }

    @Override
    public void setPreferenceObject(final String username, final String key, final Object value) {
        final String preferenceObjectAsString = internalSetPreferenceObject(username, key, value);
        apply(new SetPreferenceOperation(username, key, preferenceObjectAsString));
    }

    @Override
    public Void internalSetPreference(final String username, final String key, final String value) {
        store.setPreference(username, key, value);
        return null;
    }
    
    @Override
    public String internalSetPreferenceObject(final String username, final String key, final Object value) {
        return store.setPreferenceObject(username, key, value);
    }
    
    @Override
    public void unsetPreference(String username, String key) {
        apply(new UnsetPreferenceOperation(username, key));
    }

    @Override
    public Void internalUnsetPreference(String username, String key) {
        store.unsetPreference(username, key);
        return null;
    }

    @Override
    public Void internalSetAccessToken(String username, String accessToken) {
        store.setAccessToken(username, accessToken);
        return null;
    }
    
    @Override
    public String getAccessToken(String username) {
        return store.getAccessToken(username);
    }

    @Override
    public String getOrCreateAccessToken(String username) {
        String result = store.getAccessToken(username);
        if (result == null) {
            result = createAccessToken(username);
        }
        return result;
    }

    @Override
    public Void internalRemoveAccessToken(String username) {
        store.removeAccessToken(username);
        return null;
    }

    @Override
    public String getPreference(String username, String key) {
        return store.getPreference(username, key);
    }

    @Override
    public Map<String, String> getAllPreferences(String username) {
        return store.getAllPreferences(username);
    }
    
    @Override
    public String createAccessToken(String username) {
        User user = getUserByName(username);
        final String token;
        if (user != null) {
            RandomNumberGenerator rng = new SecureRandomNumberGenerator();
            byte[] salt = rng.nextBytes().getBytes();
            token = hashPassword(new String(rng.nextBytes().getBytes()), salt);
            apply(new SetAccessTokenOperation(user.getName(), token));
        } else {
            token = null;
        }
        return token;
    }
    
    @Override
    public void removeAccessToken(String username) {
        Subject subject = SecurityUtils.getSubject();
        if (hasCurrentUserUpdatePermission(getUserByName(username))) {
            apply(new RemoveAccessTokenOperation(username));
        } else {
            throw new org.apache.shiro.authz.AuthorizationException("User " + subject.getPrincipal().toString()
                    + " does not have permission to remove access token of user " + username);
        }
    }

    @Override
    public UserGroup getServerGroup() {
        return store.getServerGroup();
    }

    @Override
    public <T> T setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
            HasPermissions type, TypeRelativeObjectIdentifier typeIdentifier, String securityDisplayName,
            Callable<T> actionWithResult) {
        return setOwnershipCheckPermissionForObjectCreationAndRevertOnError(type, typeIdentifier,
                securityDisplayName, actionWithResult, true);
    }

    /**
     * For the current session's {@link Subject} an ownership for an object of type {@code type} and with
     * type-relative object identifier {@code typeRelativeIdentifier} is created with the subject's
     * default creation group if no ownership for that object is found yet. Otherwise, the existing ownership
     * is left untouched.<p>
     * 
     * If {@code doServerCreateObjectCheck} is {@code true}, the {@link ServerActions#CREATE_OBJECT} permission
     * is checked for the executing server.<p>
     * 
     * Then, the {@link DefaultActions#CREATE} permission is checked for the {@code type}/{@code typeRelativeIdentifier}
     * object.<p>
     * 
     * If any of these permission checks fails and the ownership has not been found but created, the ownership
     * is removed again, and the authorization exception is thrown by this method. Otherwise, the subject is
     * considered to have the permission to create the object, the {@code actionWithResult} is invoked, and
     * the object returned by the action is the result of this method.
     */
    private <T> T setOwnershipCheckPermissionForObjectCreationAndRevertOnError(HasPermissions type,
            TypeRelativeObjectIdentifier typeRelativeIdentifier, String securityDisplayName, Callable<T> actionWithResult,
            boolean doServerCreateObjectCheck) {
        QualifiedObjectIdentifier identifier = type.getQualifiedObjectIdentifier(typeRelativeIdentifier);
        T result = null;
        boolean didSetOwnership = false;
        try {
            final OwnershipAnnotation preexistingOwnership = getOwnership(identifier);
            if (preexistingOwnership == null) {
                didSetOwnership = true;
                setDefaultOwnership(identifier, securityDisplayName);
            } else {
                logger.fine("Preexisting ownership found for " + identifier + ": " + preexistingOwnership);
            }
            if (doServerCreateObjectCheck) {
                checkCurrentUserServerPermission(ServerActions.CREATE_OBJECT);
            }
            try {
                SecurityUtils.getSubject().checkPermission(identifier.getStringPermission(DefaultActions.CREATE));
            } catch (AuthorizationException e) {
                if (didSetOwnership) {
                    throw e;
                } else {
                    // An ownership for this ID already exists and the user is not permitted
                    // -> a nicer error is produced to explain the user that a name clash is most probably the cause
                    throw new UnauthorizedException(MessageFormat.format(
                            "You are not permitted to create a \"{0}\" with name or identifier \"{1}\". "
                                    + "This is most probably caused by an already existing entry with the same name/identifier. "
                                    + "Please try to use a different name.",
                            identifier.getTypeIdentifier(), identifier.getTypeRelativeObjectIdentifier().toString()), e);
                }
            }
            result = actionWithResult.call();
        } catch (AuthorizationException e) {
            if (didSetOwnership) {
                deleteOwnership(identifier);
            }
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public boolean hasCurrentUserServerPermission(ServerActions action) {
        return SecurityUtils.getSubject().isPermitted(constructServerPermissionString(action));
    }
    
    @Override
    public void checkCurrentUserServerPermission(ServerActions action) {
        SecurityUtils.getSubject().checkPermission(constructServerPermissionString(action));
    }

    private String constructServerPermissionString(ServerActions action) {
        return SecuredSecurityTypes.SERVER.getStringPermissionForTypeRelativeIdentifier(
                action, new TypeRelativeObjectIdentifier(ServerInfo.getName()));
    }

    @Override
    public <T> T setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(HasPermissions type,
            TypeRelativeObjectIdentifier typeIdentifier, String securityDisplayName, Callable<T> actionWithResult) {
        return setOwnershipCheckPermissionForObjectCreationAndRevertOnError(type, typeIdentifier, securityDisplayName,
                actionWithResult, false);
    }

    @Override
    public void setDefaultOwnership(QualifiedObjectIdentifier identifier, String description) {
        setOwnership(identifier, getCurrentUser(), getDefaultTenantForCurrentUser(), description);
    }

    @Override
    public void setDefaultOwnershipIfNotSet(QualifiedObjectIdentifier identifier) {
        final OwnershipAnnotation preexistingOwnership = getOwnership(identifier);
        if (preexistingOwnership == null || (preexistingOwnership.getAnnotation() == null ||
                (preexistingOwnership.getAnnotation().getTenantOwner() == null && preexistingOwnership.getAnnotation().getUserOwner() == null))) {
            setDefaultOwnership(identifier, identifier.toString());
        }
    }

    @Override
    public void setOwnershipCheckPermissionForObjectCreationAndRevertOnError(HasPermissions type,
            TypeRelativeObjectIdentifier typeRelativeObjectIdentifier, String securityDisplayName,
            Action actionToCreateObject) {
        setOwnershipCheckPermissionForObjectCreationAndRevertOnError(type, typeRelativeObjectIdentifier,
                securityDisplayName, () -> {
                    actionToCreateObject.run();
                    return null;
                });
    }

    @Override
    public void setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(HasPermissions type,
            TypeRelativeObjectIdentifier typeRelativeObjectIdentifier, String securityDisplayName,
            Action actionToCreateObject) {
        setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(type, typeRelativeObjectIdentifier,
                securityDisplayName, () -> {
                    actionToCreateObject.run();
                    return null;
                });
    }

    @Override
    public void setOwnershipIfNotSet(QualifiedObjectIdentifier identifier, User user, UserGroup tenantOwner) {
        final OwnershipAnnotation preexistingOwnership = getOwnership(identifier);
        if (preexistingOwnership == null || (preexistingOwnership.getAnnotation() == null ||
                (preexistingOwnership.getAnnotation().getTenantOwner() == null && preexistingOwnership.getAnnotation().getUserOwner() == null))) {
            setOwnership(identifier, user, tenantOwner, identifier.toString());
        }
    }

    /**
     * Special case for user creation, as no currentUser might exist when registering anonymous, and since a user always
     * should own itself as userOwner
     * 
     * @return
     */
    @Override
    public User checkPermissionForObjectCreationAndRevertOnErrorForUserCreation(String username,
            Callable<User> createActionReturningCreatedObject) {
        QualifiedObjectIdentifier identifier = SecuredSecurityTypes.USER
                .getQualifiedObjectIdentifier(UserImpl.getTypeRelativeObjectIdentifier(username));
        User result = null;
        try {
            SecurityUtils.getSubject().checkPermission(identifier.getStringPermission(DefaultActions.CREATE));
            result = createActionReturningCreatedObject.call();
        } catch (AuthorizationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public void checkPermissionAndDeleteOwnershipForObjectRemoval(WithQualifiedObjectIdentifier object,
            Action actionToDeleteObject) {
        checkPermissionAndDeleteOwnershipForObjectRemoval(object, () -> {
            actionToDeleteObject.run();
            return null;
        });
    }

    @Override
    public <T> T checkPermissionAndDeleteOwnershipForObjectRemoval(WithQualifiedObjectIdentifier object,
            Callable<T> actionToDeleteObject) {
        QualifiedObjectIdentifier identifier = object.getIdentifier();
        return checkPermissionAndDeleteOwnershipForObjectRemoval(identifier, actionToDeleteObject);
    }

    @Override
    public <T> T checkPermissionAndDeleteOwnershipForObjectRemoval(QualifiedObjectIdentifier identifier,
            Callable<T> actionToDeleteObject) {
        try {
            SecurityUtils.getSubject().checkPermission(identifier.getStringPermission(DefaultActions.DELETE));
            final T result = actionToDeleteObject.call();
            deleteAllDataForRemovedObject(identifier);
            return result;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void checkPermissionAndDeleteOwnershipForObjectRemoval(QualifiedObjectIdentifier identifier,
            Action actionToDeleteObject) {
        checkPermissionAndDeleteOwnershipForObjectRemoval(identifier, () -> {
            actionToDeleteObject.run();
            return null;
        });
    }

    @Override
    public void deleteAllDataForRemovedObject(QualifiedObjectIdentifier identifier) {
        logger.info("Deleting ownerships for " + identifier);
        deleteOwnership(identifier);
        logger.info("Deleting acls for " + identifier);
        deleteAccessControlList(identifier);
    }

    @Override
    public <T extends WithQualifiedObjectIdentifier> void filterObjectsWithPermissionForCurrentUser(
            HasPermissions.Action action, Iterable<T> objectsToFilter,
            Consumer<T> filteredObjectsConsumer) {
        objectsToFilter.forEach(objectToCheck -> {
            if (SecurityUtils.getSubject().isPermitted(
                    objectToCheck.getIdentifier().getStringPermission(action))) {
                filteredObjectsConsumer.accept(objectToCheck);
            }
        });
    }

    /** Filters the objects with any of the given permissions for the current user */
    @Override
    public <T extends WithQualifiedObjectIdentifier> void filterObjectsWithAnyPermissionForCurrentUser(
            HasPermissions.Action[] actions, Iterable<T> objectsToFilter,
            Consumer<T> filteredObjectsConsumer) {
        objectsToFilter.forEach(objectToCheck -> {
            boolean isPermitted = false;
            for (int i = 0; i < actions.length; i++) {
                if (SecurityUtils.getSubject()
                        .isPermitted(objectToCheck.getIdentifier().getStringPermission(actions[i]))) {
                    isPermitted = true;
                    break;
                }
            }
            if (isPermitted) {
                filteredObjectsConsumer.accept(objectToCheck);
            }
        });
    }

    @Override
    public <T extends WithQualifiedObjectIdentifier, R> List<R> mapAndFilterByReadPermissionForCurrentUser(
            Iterable<T> objectsToFilter, Function<T, R> filteredObjectsMapper) {
        final List<R> result = new ArrayList<>();
        filterObjectsWithPermissionForCurrentUser(DefaultActions.READ, objectsToFilter,
                filteredObject -> result.add(filteredObjectsMapper.apply(filteredObject)));
        return result;
    }
    
    @Override
    public <T extends WithQualifiedObjectIdentifier, R> List<R> mapAndFilterByAnyExplicitPermissionForCurrentUser(
            HasPermissions permittedObject, HasPermissions.Action[] actions, Iterable<T> objectsToFilter,
            Function<T, R> filteredObjectsMapper) {
        final List<R> result = new ArrayList<>();
        filterObjectsWithAnyPermissionForCurrentUser(actions, objectsToFilter,
                filteredObject -> result.add(filteredObjectsMapper.apply(filteredObject)));
        return result;
    }

    @Override
    public User getAllUser() {
        return store.getUserByName(SecurityService.ALL_USERNAME);
    }
    
    @Override
    public boolean hasCurrentUserMetaPermission(WildcardPermission permissionToCheck, Ownership ownership) {
        if (hasPermissionsProvider == null) {
            logger.warning(
                    "Missing HasPermissionsProvider for meta permission check. Using basic permission check that will produce false negatives in some cases.");
            // In case we can not resolve all available HasPermissions instances, a meta permission check will not be
            // able to produce the expected results.
            // A basic permission check is done instead. This will potentially produce false negatives but never false
            // positives.
            return PermissionChecker.isPermitted(permissionToCheck, getCurrentUser(), getAllUser(), ownership, null);
        } else {
            return PermissionChecker.checkMetaPermission(permissionToCheck,
                    hasPermissionsProvider.getAllHasPermissions(), getCurrentUser(), getAllUser(), ownership);
        }
    }
    
    @Override
    public boolean hasCurrentUserMetaPermissionWithOwnershipLookup(WildcardPermission permissionToCheck) {
        if (hasPermissionsProvider == null) {
            logger.warning(
                    "Missing HasPermissionsProvider for meta permission check. Using basic permission check that will produce false negatives in some cases.");
            // In case we can not resolve all available HasPermissions instances, a meta permission check will not be
            // able to produce the expected results.
            // A basic permission check is done instead. This will potentially produce false negatives but never false
            // positives.
            return PermissionChecker.isPermitted(permissionToCheck, getCurrentUser(), getAllUser(), null, null);
        } else {
            return PermissionChecker.checkMetaPermissionWithOwnershipResolution(permissionToCheck,
                    hasPermissionsProvider.getAllHasPermissions(), getCurrentUser(), getAllUser(),
                    qualifiedObjectId -> {
                        OwnershipAnnotation ownershipAnnotation = accessControlStore.getOwnership(qualifiedObjectId);
                        return ownershipAnnotation == null ? null : ownershipAnnotation.getAnnotation();
                    });
        }
    }
    
    @Override
    public boolean hasCurrentUserAnyPermission(WildcardPermission permissionToCheck) {
        if (hasPermissionsProvider == null) {
            logger.warning(
                    "Missing HasPermissionsProvider for any permission check. Using basic permission check that will produce false negatives in some cases.");
            // In case we can not resolve all available HasPermissions instances, an any permission check will not be
            // able to produce the expected results.
            // A basic permission check is done instead. This will potentially produce false negatives but never false
            // positives.
            return PermissionChecker.isPermitted(permissionToCheck, getCurrentUser(), getAllUser(), null, null);
        } else {
            return PermissionChecker.hasUserAnyPermission(permissionToCheck, hasPermissionsProvider.getAllHasPermissions(), getCurrentUser(), getAllUser(), null);
        }
    }
    
    @Override
    public boolean hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(final RoleDefinition roleDefinition, final Ownership qualificationForGrantedPermissions) {
        boolean result = true;
        for (WildcardPermission permissionToCheck : roleDefinition.getPermissions()) {
            if (!hasCurrentUserMetaPermission(permissionToCheck, qualificationForGrantedPermissions)) {
                result = false;
                break;
            }
        }
        return result;
    }
    
    @Override
    public boolean hasCurrentUserMetaPermissionsOfRoleDefinitionsWithQualification(Set<RoleDefinition> roleDefinitions,
            Ownership qualificationForGrantedPermissions) {
        boolean result = true;
        for (final RoleDefinition roleDefinition : roleDefinitions) {
            result &= hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(roleDefinition,
                    qualificationForGrantedPermissions);
            if (!result) {
                break;
            }
        }
        return result;
    }

    @Override
    public RoleDefinition getOrCreateRoleDefinitionFromPrototype(final RolePrototype rolePrototype) {
        final RoleDefinition potentiallyExistingRoleDefinition = store.getRoleDefinition(rolePrototype.getId());
        final RoleDefinition result;
        if (potentiallyExistingRoleDefinition == null) {
            result = store.createRoleDefinition(rolePrototype.getId(), rolePrototype.getName(),
                    rolePrototype.getPermissions());
            setOwnership(result.getIdentifier(), null, getServerGroup());
        } else {
            result = potentiallyExistingRoleDefinition;
        }
        return result;
    }

    // ----------------- Replication -------------
    @Override
    public void clearReplicaState() throws MalformedURLException, IOException, InterruptedException {
        store.clear();
        accessControlStore.clear();
    }

    @Override
    public Serializable getId() {
        return getClass().getName();
    }
    
    @Override
    public ObjectInputStream createObjectInputStreamResolvingAgainstCache(InputStream is) throws IOException {
        return new ObjectInputStreamResolvingAgainstSecurityCache(is, store, null);
    }

    @Override
    public void initiallyFillFromInternal(ObjectInputStream is) throws IOException, ClassNotFoundException,
            InterruptedException {
        logger.info("Reading cache manager...");
        ReplicatingCacheManager newCacheManager = (ReplicatingCacheManager) is.readObject();
        cacheManager.replaceContentsFrom(newCacheManager);
        // overriding thread context class loader because the user store may be provided by a different bundle;
        // We're assuming here that the user store service is provided by the same bundle in the replica as on the master.
        ClassLoader oldCCL = Thread.currentThread().getContextClassLoader();
        if (store != null) {
            Thread.currentThread().setContextClassLoader(store.getClass().getClassLoader());
        }
        logger.info("Reading user store...");
        boolean createdServerGroup = false;
        try {
            final UserStore newUserStore = (UserStore) is.readObject();
            final UserGroup newServerGroup = newUserStore.getUserGroupByName(store.getServerGroupName());
            // before replacing the local UserStore's contents, capture the server group:
            final UserGroup oldServerGroup = store.getServerGroup();
            store.replaceContentsFrom(newUserStore);
            if (newServerGroup == null) {
                // create the server group in a replication-aware fashion, making sure it appears on the master
                final String serverGroupName = store.getServerGroupName();
                final UUID serverGroupUuid = UUID.randomUUID();
                createUserGroupWithInitialUser(serverGroupUuid, serverGroupName, /* initial user */ null);
                store.setServerGroup(store.getUserGroupByName(store.getServerGroupName()));
                createdServerGroup = true;
            } else if (newServerGroup != oldServerGroup) {
                store.setServerGroup(newServerGroup);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCCL);
        }
        if (accessControlStore != null) {
            Thread.currentThread().setContextClassLoader(accessControlStore.getClass().getClassLoader());
        }
        logger.info("Reading access control store...");
        try {
            AccessControlStore newAccessControlStore = (AccessControlStore) is.readObject();
            accessControlStore.replaceContentsFrom(newAccessControlStore);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCCL);
        }
        // now ensure that the SERVER object has a valid ownership; use the (potentially new) server group as the default:
        migrateServerObject();
        if (createdServerGroup) {
            final UserGroup serverGroup = store.getServerGroup();
            setOwnership(serverGroup.getIdentifier(), null, serverGroup, serverGroup.getName());
            final User adminUserOrNull = store.getUserByName(UserStore.ADMIN_USERNAME);
            if (adminUserOrNull == null) {
                logger.info("User 'admin' does not exist on replicated central SecurityService. "
                        + "'admin' will not be properly set up for this server.");
            } else {
                addUserToUserGroup(serverGroup, adminUserOrNull);
                setDefaultTenantForCurrentServerForUser(UserStore.ADMIN_USERNAME, serverGroup.getId());
            }
            isNewServer = true;
        }
        isInitialOrMigration = false;
        logger.info("Reading isSharedAcrossSubdomains...");
        sharedAcrossSubdomainsOf = (String) is.readObject();
        logger.info("...as "+sharedAcrossSubdomainsOf);
        logger.info("Reading baseUrlForCrossDomainStorage...");
        baseUrlForCrossDomainStorage = (String) is.readObject();
        logger.info("...as "+baseUrlForCrossDomainStorage);
        logger.info("Triggering SecurityInitializationCustomizers upon replication ...");
        customizers.forEach(c -> c.customizeSecurityService(this));
        logger.info("Done filling SecurityService");
    }

    @Override
    public void serializeForInitialReplicationInternal(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(cacheManager);
        objectOutputStream.writeObject(store);
        objectOutputStream.writeObject(accessControlStore);
        objectOutputStream.writeObject(sharedAcrossSubdomainsOf);
        objectOutputStream.writeObject(baseUrlForCrossDomainStorage);
    }

    @Override
    public Iterable<OperationExecutionListener<ReplicableSecurityService>> getOperationExecutionListeners() {
        return operationExecutionListeners.keySet();
    }

    @Override
    public void addOperationExecutionListener(OperationExecutionListener<ReplicableSecurityService> listener) {
        operationExecutionListeners.put(listener, listener);
    }

    @Override
    public void removeOperationExecutionListener(OperationExecutionListener<ReplicableSecurityService> listener) {
        operationExecutionListeners.remove(listener);
    }

    @Override
    public ReplicationMasterDescriptor getMasterDescriptor() {
        return replicatingFromMaster;
    }

    @Override
    public void startedReplicatingFrom(ReplicationMasterDescriptor master) {
        this.replicatingFromMaster = master;
    }

    @Override
    public void stoppedReplicatingFrom(ReplicationMasterDescriptor master) {
        this.replicatingFromMaster = null;
    }

    @Override
    public void addOperationSentToMasterForReplication(
            OperationWithResultWithIdWrapper<ReplicableSecurityService, ?> operationWithResultWithIdWrapper) {
        this.operationsSentToMasterForReplication.add(operationWithResultWithIdWrapper);
    }

    @Override
    public boolean hasSentOperationToMaster(OperationWithResult<ReplicableSecurityService, ?> operation) {
        return this.operationsSentToMasterForReplication.remove(operation);
    }

    @Override
    public boolean migrateOwnership(WithQualifiedObjectIdentifier identifier) {
        return migrateOwnership(identifier.getIdentifier(), identifier.getName());
    }

    @Override
    public boolean migrateOwnership(final QualifiedObjectIdentifier identifier, final String displayName) {
        return this.migrateOwnership(identifier, null, /* setServerGroupAsOwner */ true, displayName);
    }
    
    @Override
    public void migrateUser(final User user) {
        // If no ownership migration was necessary, this is not a migration
        if (migrateOwnership(user.getIdentifier(), user, /* setServerGroupAsOwner */ false, user.getName())) {
            final String tenantNameForUsername = getDefaultTenantNameForUsername(user.getName());
            // if there is already a default creation tenant set for the user, this is not a migration
            // If the user's tenant already exists, this is no migration
            if (user.getDefaultTenant(ServerInfo.getName()) == null
                    && getUserGroupByName(tenantNameForUsername) == null) {
                try {
                    final UserGroup tenantForUser = createTenantForUser(tenantNameForUsername, user);
                    setDefaultTenantForCurrentServerForUser(user.getName(), tenantForUser.getId());
                } catch (UserGroupManagementException e) {
                    logger.log(Level.SEVERE, "Error during migration while creating tenant for user: " + user, e);
                }
                // Only adding user role if it is most probably a migration case
                // In case an admin removes/changes the user role, it should not be recreated automatically
                addUserRoleToUser(user);
                final RoleDefinition adminRoleDefinition = getRoleDefinition(AdminRole.getInstance().getId());
                for (Role roleOfUser : user.getRoles()) {
                    if (roleOfUser.getRoleDefinition().equals(adminRoleDefinition)) {
                        final UserGroup serverGroup = getServerGroup();
                        if (roleOfUser.getQualifiedForTenant() == null
                                || roleOfUser.getQualifiedForTenant().equals(serverGroup)) {
                            // The user is a server admin -> Add it to the server group to allow setting the server
                            // group as default creation group
                            addUserToUserGroup(serverGroup, user);
                            if (UserStore.ADMIN_USERNAME.equals(user.getName())) {
                                // For the "admin" user the server group is initially set as
                                // default creation group. This is consistent to a newly created server
                                // and in most cases this is the group, the admin is meant to work with.
                                setDefaultTenantForCurrentServerForUser(user.getName(), getServerGroup().getId());
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void migratePermission(final User user, final WildcardPermission permissionToMigrate, final Function<WildcardPermission, WildcardPermission> permissionReplacement) {
        final WildcardPermission replacementPermissionOrNull = permissionReplacement.apply(permissionToMigrate);
        final WildcardPermission effectivePermission;
        if (replacementPermissionOrNull != null) {
            // replacing legacy permission with a replacement
            String username = user.getName();
            removePermissionFromUser(username, permissionToMigrate);
            addPermissionForUser(username, replacementPermissionOrNull);
            effectivePermission = replacementPermissionOrNull;
        } else {
            effectivePermission = permissionToMigrate;
        }
        final TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(effectivePermission, user);
        final QualifiedObjectIdentifier associationQualifiedIdentifier = SecuredSecurityTypes.PERMISSION_ASSOCIATION
                .getQualifiedObjectIdentifier(associationTypeIdentifier);
        migrateOwnership(associationQualifiedIdentifier, associationQualifiedIdentifier.toString());
    }
    
    private boolean migrateOwnership(final QualifiedObjectIdentifier identifier, User userOwnerToSet,
            boolean setServerGroupAsOwner, final String displayName) {
        boolean wasNecessaryToMigrate = false;
        final OwnershipAnnotation owner = this.getOwnership(identifier);
        // initialize ownerships on migration and fix objects that were orphaned by e.g. deleting the owning user/group
        if (owner == null
                || owner.getAnnotation().getTenantOwner() == null && owner.getAnnotation().getUserOwner() == null) {
            final UserGroup tenantOwnerToSet = setServerGroupAsOwner ? this.getServerGroup() : null;
            logger.info("missing Ownership fixed: Setting ownership for: " + identifier
                    + " to tenant: "
                    + tenantOwnerToSet + "; user: " + userOwnerToSet);
            this.setOwnership(identifier, userOwnerToSet, tenantOwnerToSet, displayName);
            wasNecessaryToMigrate = true;
        }
        migratedHasPermissionTypes.add(identifier.getTypeIdentifier());
        return wasNecessaryToMigrate;
    }

    @Override
    public void migrateServerObject() {
        final QualifiedObjectIdentifier serverIdentifier = SecuredSecurityTypes.SERVER
                .getQualifiedObjectIdentifier(
                        new TypeRelativeObjectIdentifier(ServerInfo.getName()));
        migrateOwnership(serverIdentifier, serverIdentifier.toString());
    }

    @Override
    public void checkMigration(Iterable<HasPermissions> allInstances) {
        Class<? extends HasPermissions> clazz = Util.first(allInstances).getClass();
        boolean allChecksSucessful = true;
        for (HasPermissions shouldBeMigrated : allInstances) {
            if (!migratedHasPermissionTypes.contains(shouldBeMigrated.getName())) {
                logger.severe("ensure Ownership failed: Did not check Ownerships for " + clazz.getName()
                        + " missing: " + shouldBeMigrated);
                allChecksSucessful = false;
            }
        }
        if (allChecksSucessful) {
            logger.info("Ownership checks finished: Sucessfully checked all types in " + clazz.getName());
        }
    }

    @Override
    public boolean hasCurrentUserReadPermission(WithQualifiedObjectIdentifier object) {
        return object == null ? true :
            SecurityUtils.getSubject().isPermitted(object.getPermissionType().getStringPermissionForObject(DefaultActions.READ, object));
    }

    @Override
    public boolean hasCurrentUserUpdatePermission(WithQualifiedObjectIdentifier object) {
        return object == null ? true :
            SecurityUtils.getSubject().isPermitted(object.getPermissionType().getStringPermissionForObject(DefaultActions.UPDATE, object));
    }

    @Override
    public boolean hasCurrentUserDeletePermission(WithQualifiedObjectIdentifier object) {
        return object == null ? true :
            SecurityUtils.getSubject().isPermitted(object.getPermissionType().getStringPermissionForObject(DefaultActions.DELETE, object));
    }

    public boolean hasCurrentUserExplicitPermissions(WithQualifiedObjectIdentifier object, HasPermissions.Action... actions) {
        boolean isPermitted = true;
        if (object != null) {
            for (int i = 0; i < actions.length; i++) {
                isPermitted &= SecurityUtils.getSubject().isPermitted(object.getPermissionType().getStringPermissionForObject(actions[i], object));
            }
        }
        return isPermitted;
    }

    public boolean hasCurrentUserOneOfExplicitPermissions(WithQualifiedObjectIdentifier object, HasPermissions.Action... actions) {
        boolean result = object == null;
        if (object != null) {
            for (com.sap.sse.security.shared.HasPermissions.Action action : actions) {
                if (hasCurrentUserExplicitPermissions(object, action)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void checkCurrentUserReadPermission(WithQualifiedObjectIdentifier object) {
        if (object != null) {
            SecurityUtils.getSubject().checkPermission(object.getPermissionType().getStringPermissionForObject(DefaultActions.READ, object));
        }
    }

    @Override
    public void checkCurrentUserUpdatePermission(WithQualifiedObjectIdentifier object) {
        if (object != null) {
            SecurityUtils.getSubject().checkPermission(object.getPermissionType().getStringPermissionForObject(DefaultActions.UPDATE, object));
        }
    }

    @Override
    public void checkCurrentUserDeletePermission(WithQualifiedObjectIdentifier object) {
        if (object != null) {
            SecurityUtils.getSubject().checkPermission(object.getPermissionType().getStringPermissionForObject(DefaultActions.DELETE, object));
        }
    }

    @Override
    public void checkCurrentUserDeletePermission(QualifiedObjectIdentifier identifier) {
        if (identifier != null) {
            SecurityUtils.getSubject().checkPermission(identifier.getStringPermission(DefaultActions.DELETE));
        }
    }

    @Override
    public void checkCurrentUserExplicitPermissions(WithQualifiedObjectIdentifier object, HasPermissions.Action... actions) {
        if (object != null) {
            for (int i = 0; i < actions.length; i++) {
                SecurityUtils.getSubject().checkPermission(object.getPermissionType().getStringPermissionForObject(actions[i], object));
            }
        }
    }

    @Override
    public void checkCurrentUserHasOneOfExplicitPermissions(WithQualifiedObjectIdentifier object, HasPermissions.Action... actions) {
        if (object != null) {
            boolean isPermitted = false;
            for (int i = 0; i < actions.length; i++) {
                if (SecurityUtils.getSubject()
                        .isPermitted(object.getPermissionType().getStringPermissionForObject(actions[i], object))) {
                    isPermitted = true;
                    break;
                }
            }
            if (!isPermitted) {
                throw new AuthorizationException();
            }
        }
    }

    @Override
    public void assumeOwnershipMigrated(String typeName) {
        migratedHasPermissionTypes.add(typeName);
    }
    
    @Override
    public boolean hasUserAllWildcardPermissionsForAlreadyRealizedQualifications(RoleDefinition role,
            Iterable<WildcardPermission> permissionsToCheck) {
        Pair<Boolean, Set<Ownership>> qualificationsToCheck = store.getExistingQualificationsForRoleDefinition(role);
        final Iterable<Ownership> effectiveQualificationsToCheck = Boolean.TRUE.equals(qualificationsToCheck.getA())
                ? Collections.singletonList(null)
                : qualificationsToCheck.getB();
        for (WildcardPermission permission : permissionsToCheck) {
            for (Ownership ownership : effectiveQualificationsToCheck) {
                if (!hasCurrentUserMetaPermission(permission, ownership)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public <T> T getPreferenceObject(String username, String key) {
        return store.getPreferenceObject(username, key);
    }

    @Override
    public void setDefaultTenantForCurrentServerForUser(String username, UUID defaultTenantId) {
        final String serverName = ServerInfo.getName();
        apply(new SetDefaultTenantForServerForUserOperation(username, defaultTenantId, serverName));
    }
    
    @Override
    public Void internalSetDefaultTenantForServerForUser(String username, UUID defaultTenantId, String serverName) {
        User user = getUserByName(username);
        UserGroup newDefaultTenant = getUserGroup(defaultTenantId);
        user.setDefaultTenant(newDefaultTenant, serverName);
        store.updateUser(user);
        return null;
    }
    
    @Override
    /**
     * This method does not handle RoleAssociationOwnerships! this must be done via the callback
     */
    public void copyUsersAndRoleAssociations(UserGroup source, UserGroup destination, RoleCopyListener callback) {
        for (User user : source.getUsers()) {
            addUserToUserGroup(destination, user);
        }

        for (Map.Entry<RoleDefinition, Boolean> entr : source.getRoleDefinitionMap().entrySet()) {
            putRoleDefinitionToUserGroup(destination, entr.getKey(), entr.getValue());
        }

        for (Pair<User, Role> userAndRole : store.getRolesQualifiedByUserGroup(source)) {
            final Role existingRole = userAndRole.getB();
            final Role copyRole = new Role(existingRole.getRoleDefinition(), destination,
                    existingRole.getQualifiedForUser());
            addRoleForUser(userAndRole.getA(),
                    copyRole);
            callback.onRoleCopy(userAndRole.getA(), existingRole, copyRole);
        }
    }
    
    @Override
    public <T> T doWithTemporaryDefaultTenant(UserGroup tenant, Callable<T> action) {
        final UserGroup previousValue = temporaryDefaultTenant.get();
        temporaryDefaultTenant.set(tenant);
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            temporaryDefaultTenant.set(previousValue);
        }
    }

    @Override
    // See com.sap.sse.security.impl.Activator.clearState(), moved due to required reinitialisation sequence for
    // permission-vertical
    public void clearState() throws Exception {
    }

    @Override
    public void setUnsentOperationToMasterSender(OperationsToMasterSendingQueue service) {
        this.unsentOperationsToMasterSender = service;
    }
    @Override
    public void storeSession(String cacheName, Session session) {
        PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().storeSession(cacheName, session);
    }

    @Override
    public <S, O extends OperationWithResult<S, ?>, T> void scheduleForSending(
            O operationWithResult, OperationsToMasterSender<S, O> sender) {
        if (unsentOperationsToMasterSender != null) {
            unsentOperationsToMasterSender.scheduleForSending(operationWithResult, sender);
        }
    }

    @Override
    public void removeSession(String cacheName, Session session) {
        PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().removeSession(cacheName, session);
    }

    @Override
    public void removeAllSessions(String cacheName) {
        PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().removeAllSessions(cacheName);
    }

    @Override
    public boolean isInitialOrMigration() {
        return isInitialOrMigration;
    }
    
    @Override
    public boolean isNewServer() {
        return isNewServer;
    }

    @Override
    public String getSharedAcrossSubdomainsOf() {
        return sharedAcrossSubdomainsOf;
    }

    @Override
    public String getBaseUrlForCrossDomainStorage() {
        return baseUrlForCrossDomainStorage;
    }
    
    @Override
    public void registerCustomizer(SecurityInitializationCustomizer customizer) {
        customizers.add(customizer);
        customizer.customizeSecurityService(this);
    }

    @Override
    public void updateUserSubscription(String username, Subscription subscription) throws UserManagementException {
        final User user = getUserByName(username);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        apply(new UpdateUserSubscriptionOperation(username, subscription));
    }

    @Override
    public Void internalUpdateSubscription(String username, Subscription subscription) throws UserManagementException {
        User user = getUserByName(username);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }

        Subscription currentSubscription = user.getSubscription();
        if (shouldUpdateUserRolesForSubscription(currentSubscription, subscription)) {
            updateUserRolesOnSubscriptionChange(username, currentSubscription, subscription);
        }

        user.setSubscription(subscription);
        store.updateUser(user);
        return null;
    }

    private void updateUserRolesOnSubscriptionChange(String username, Subscription currentSubscription,
            Subscription newSubscription) throws UserManagementException {
        logger.log(Level.INFO, "Update user subscription roles");
        if (currentSubscription != null && currentSubscription.getPlanId() != null
                && currentSubscription.isActiveSubscription()) {
            SubscriptionPlan currentPlan = SubscriptionPlanHolder.getInstance()
                    .getPlan(currentSubscription.getPlanId());
            if (currentPlan != null) {
                UUID[] roleDefinitionIds = currentPlan.getRoleDefinitionIds();
                for (UUID roleDefId : roleDefinitionIds) {
                    store.removeRoleFromUser(username, new Role(getRoleDefinition(roleDefId)));
                }
            }
        }

        if (newSubscription != null && newSubscription.getPlanId() != null && newSubscription.isActiveSubscription()) {
            SubscriptionPlan newPlan = SubscriptionPlanHolder.getInstance().getPlan(newSubscription.getPlanId());
            if (newPlan != null) {
                UUID[] roleDefinitionIds = newPlan.getRoleDefinitionIds();
                for (UUID roleDefId : roleDefinitionIds) {
                    store.addRoleForUser(username, new Role(getRoleDefinition(roleDefId)));
                }
            }
        }
    }

    private boolean shouldUpdateUserRolesForSubscription(Subscription currentSubscription,
            Subscription newSubscription) {
        if (isUserSubscriptionPlanChanged(currentSubscription, newSubscription)) {
            return true;
        }

        return newSubscription != null && currentSubscription != null
                && newSubscription.isActiveSubscription() != currentSubscription.isActiveSubscription();
    }

    /**
     * 
     * @param currentSubscription
     *            current user subscription
     * @param newSubscription
     *            new subscription
     * @return true if new user subscription plan is different with current subscription plan
     */
    private boolean isUserSubscriptionPlanChanged(Subscription currentSubscription, Subscription newSubscription) {
        if ((currentSubscription == null || currentSubscription.getPlanId() == null)
                && (newSubscription == null || newSubscription.getPlanId() == null)) {
            return false;
        }

        String currentPlan = null;
        String newPlan = null;

        if (currentSubscription != null) {
            currentPlan = currentSubscription.getPlanId();
        }
        if (newSubscription != null) {
            newPlan = newSubscription.getPlanId();
        }

        if (currentPlan == null && newPlan == null) {
            return false;
        } else if (currentPlan != null && newPlan != null) {
            return !currentPlan.equals(newPlan);
        } else {
            return true;
        }
    }
}
