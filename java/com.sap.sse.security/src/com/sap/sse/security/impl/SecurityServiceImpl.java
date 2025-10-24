package com.sap.sse.security.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.apache.shiro.env.BasicIniEnvironment;
import org.apache.shiro.env.Environment;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
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

import com.nulabinc.zxcvbn.StandardDictionaries;
import com.nulabinc.zxcvbn.StandardKeyboards;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import com.nulabinc.zxcvbn.ZxcvbnBuilder;
import com.nulabinc.zxcvbn.io.ClasspathResource;
import com.nulabinc.zxcvbn.matchers.SlantedKeyboardLoader;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.http.HttpHeaderUtil;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.common.media.TakedownNoticeRequestContext;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;
import com.sap.sse.mail.MailService;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.replication.interfaces.impl.AbstractReplicableWithObjectInputStream;
import com.sap.sse.rest.CORSFilterConfiguration;
import com.sap.sse.security.Action;
import com.sap.sse.security.ClientUtils;
import com.sap.sse.security.GithubApi;
import com.sap.sse.security.InstagramApi;
import com.sap.sse.security.OAuthRealm;
import com.sap.sse.security.PermissionChangeListener;
import com.sap.sse.security.SecurityInitializationCustomizer;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SessionCacheManager;
import com.sap.sse.security.SessionUtils;
import com.sap.sse.security.ShiroWildcardPermissionFromParts;
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
import com.sap.sse.security.operations.SecurityOperation;
import com.sap.sse.security.operations.SetAccessTokenOperation;
import com.sap.sse.security.operations.SetDefaultTenantForServerForUserOperation;
import com.sap.sse.security.operations.SetEmptyAccessControlListOperation;
import com.sap.sse.security.operations.SetOwnershipOperation;
import com.sap.sse.security.operations.SetPreferenceOperation;
import com.sap.sse.security.operations.SetSettingOperation;
import com.sap.sse.security.operations.UnsetPreferenceOperation;
import com.sap.sse.security.operations.UpdateItemPriceOperation;
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
import com.sap.sse.security.shared.PermissionChecker.AclResolver;
import com.sap.sse.security.shared.PredefinedRoles;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.RolePrototype;
import com.sap.sse.security.shared.SecurityAccessControlList;
import com.sap.sse.security.shared.SubscriptionPlanProvider;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.UserRole;
import com.sap.sse.security.shared.UsernamePasswordAccount;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.LockingAndBanning;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.PermissionAndRoleAssociation;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.subscription.Subscription;
import com.sap.sse.security.shared.subscription.SubscriptionPlan;
import com.sap.sse.security.shared.subscription.SubscriptionPlanRole;
import com.sap.sse.security.shared.subscription.SubscriptionPrice;
import com.sap.sse.security.util.RemoteServerUtil;
import com.sap.sse.shared.classloading.ClassLoaderRegistry;
import com.sap.sse.shared.util.impl.ApproximateTime;
import com.sap.sse.util.ClearStateTestSupport;
import com.sap.sse.util.ThreadPoolUtil;

public class SecurityServiceImpl
extends AbstractReplicableWithObjectInputStream<ReplicableSecurityService, SecurityOperation<?>>
implements ReplicableSecurityService, ClearStateTestSupport {

    private static final Logger logger = Logger.getLogger(SecurityServiceImpl.class.getName());

    private static final String ADMIN_DEFAULT_PASSWORD = "admin";

    private final Set<String> migratedHasPermissionTypes = new ConcurrentSkipListSet<>();

    private SecurityManager securityManager;
    
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
    
    /**
     * Keys are the replica set's server names. Values are {@link Pair}s whose {@link Pair#getA() first} component is a
     * boolean telling whether the CORS filter for the replica set identified by the key uses the "wildcard" (*) to
     * allow REST requests from all possible origins, and the {@link Pair#getB() second} component lists the allowed
     * origins in case it's not a wildcard configuration. For wildcard configurations, the second component is ignored.
     */
    private final ConcurrentMap<String, Pair<Boolean, Set<String>>> corsFilterConfigurationsByReplicaSetName;
    
    private final UserStore store;
    private final AccessControlStore accessControlStore;
    
    private boolean isInitialOrMigration;
    private boolean isNewServer;
    
    private final ServiceTracker<MailService, MailService> mailServiceTracker;
    
    private final ServiceTracker<CORSFilterConfiguration, CORSFilterConfiguration> corsFilterConfigurationTracker;
    
    private final ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker;

    private ThreadLocal<UserGroup> temporaryDefaultTenant = new InheritableThreadLocal<>();
    
    private final static Ini shiroConfiguration;
    private final static Environment shiroEnvironment;

    private final HasPermissionsProvider hasPermissionsProvider;
    private final SubscriptionPlanProvider subscriptionPlanProvider;

    private String sharedAcrossSubdomainsOf;
    
    private String baseUrlForCrossDomainStorage;
    
    private final transient Set<SecurityInitializationCustomizer> customizers = ConcurrentHashMap.newKeySet();
    
    private final AclResolver<AccessControlList, Ownership> aclResolver;
    
    private final PermissionChangeListeners permissionChangeListeners;
    
    private final ClassLoaderRegistry initialLoadClassLoaderRegistry = ClassLoaderRegistry.createInstance();
    
    /**
     * Contains locking objects keyed by client IP addresses that describe which client IPs are currently locked
     * from bearer token-based authentication due to previously failing requests. Requests for which a client
     * IP address could not be determined are keyed with the {@link #CLIENT_IP_NULL_ESCAPE} key.<p>
     * 
     * When entering values into this map, the method entering it is responsible for also scheduling a background
     * task that a while after lock expiry the record is expunged again from the map to avoid garbage piling up.
     * 
     * @see #failedBearerTokenAuthentication(String)
     * @see #successfulBearerTokenAuthentication(String)
     * @see #isClientIPLockedForBearerTokenAuthentication(String)
     */
    private final ConcurrentMap<String, LockingAndBanning> clientIPBasedLockingAndBanningForBearerTokenAuthentication;
    private final static String CLIENT_IP_NULL_ESCAPE = UUID.randomUUID().toString();

    /**
     * Contains locking objects keyed by client IP addresses ({@code null} not allowed) that describe which client IPs
     * are locked for {@link User} creation, e.g., through the
     * {@link #createSimpleUser(String, String, String, String, String, Locale, String, UserGroup, String, boolean)
     * createSimpleUser} method.<p>
     * 
     * When entering values into this map, the method entering it is responsible for also scheduling a background
     * task that a while after lock expiry the record is expunged again from the map to avoid garbage piling up.
     */
    private final ConcurrentMap<String, LockingAndBanning> clientIPBasedLockingAndBanningForUserCreation;

    private final Zxcvbn passwordValidator;
    
    /**
     * When working with a user's subscriptions, such as first reading, then changing and updating a user's subscription
     * based on what was read, a user-specific write lock must be obtained to ensure that no writes can cut in between.
     * See also {@link #lockSubscriptionsForUser} and {@link #unlockSubscriptionsForUser}.
     */
    private final static ConcurrentMap<User, NamedReentrantReadWriteLock> subscriptionLocksForUsers;

    static {
        shiroConfiguration = new Ini();
        shiroConfiguration.loadFromPath("classpath:shiro.ini");
        shiroEnvironment = new BasicIniEnvironment("classpath:shiro.ini");
        subscriptionLocksForUsers = new ConcurrentHashMap<>();
    }
    
    /**
     * Creates a security service that is not shared across subdomains, therefore leading to the use of the full
     * domain through which its services are requested for {@code Document.domain} and hence for the browser local
     * storage, session storage and the Shiro {@code JSESSIONID} cookie's domain.
     * @param setAsActivatorSecurityService
     *            when <code>true</code>, the {@link Activator#setSecurityService(com.sap.sse.security.SecurityService)}
     *            will be called with this new instance as argument so that the cache manager can already be accessed
     *            when the security manager is created. {@link ReplicatingCacheManager#getCache(String)} fetches the
     *            activator's security service and passes it to the cache entries created. They need it, in turn, for
     *            replication.
     */
    public SecurityServiceImpl(ServiceTracker<MailService, MailService> mailServiceTracker,
            ServiceTracker<CORSFilterConfiguration, CORSFilterConfiguration> corsFilterConfigurationTracker,
            UserStore userStore, AccessControlStore accessControlStore, HasPermissionsProvider hasPermissionsProvider,
            SubscriptionPlanProvider subscriptionPlanProvider) {
        this(mailServiceTracker, corsFilterConfigurationTracker, /* replicationServiceTracker */ null, userStore, accessControlStore,
                hasPermissionsProvider, subscriptionPlanProvider, /* sharedAcrossSubdomainsOf */ null, /* baseUrlForCrossDomainStorage */ null);
    }
    
    /**
     * Like {@link #SecurityServiceImpl(ServiceTracker, UserStore, AccessControlStore, HasPermissionsProvider)}, only that additionally
     * a parent domain can be specified in {@code isSharedAcrossSubdomains}, such as, e.g., {@code "sapsailing.com"}, across which
     * the browser local and session store shall be shared and for which sessions identified by the {@code JSESSIONID} cookie shall
     * be shared as well.
     */
    public SecurityServiceImpl(ServiceTracker<MailService, MailService> mailServiceTracker, ServiceTracker<CORSFilterConfiguration, CORSFilterConfiguration> corsFilterConfigurationTracker,
            ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker, UserStore userStore, AccessControlStore accessControlStore,
            HasPermissionsProvider hasPermissionsProvider, SubscriptionPlanProvider subscriptionPlanProvider, String sharedAcrossSubdomainsOf, String baseUrlForCrossDomainStorage) {
        initialLoadClassLoaderRegistry.addClassLoader(getClass().getClassLoader());
        if (hasPermissionsProvider == null) {
            throw new IllegalArgumentException("No HasPermissionsProvider defined");
        }
        logger.info("Initializing Security Service with user store " + userStore);
        this.clientIPBasedLockingAndBanningForBearerTokenAuthentication = new ConcurrentHashMap<>();
        this.clientIPBasedLockingAndBanningForUserCreation = new ConcurrentHashMap<>();
        this.permissionChangeListeners = new PermissionChangeListeners(this);
        this.sharedAcrossSubdomainsOf = sharedAcrossSubdomainsOf;
        this.subscriptionPlanProvider = subscriptionPlanProvider;
        this.baseUrlForCrossDomainStorage = baseUrlForCrossDomainStorage;
        this.store = userStore;
        this.accessControlStore = accessControlStore;
        this.mailServiceTracker = mailServiceTracker;
        this.corsFilterConfigurationTracker = corsFilterConfigurationTracker;
        this.replicationServiceTracker = replicationServiceTracker;
        this.hasPermissionsProvider = hasPermissionsProvider;
        this.cacheManager = loadReplicationCacheManagerContents();
        this.corsFilterConfigurationsByReplicaSetName = loadCORSFilterConfigurations();
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
        final SecurityManager securityManager = shiroEnvironment.getSecurityManager();
        logger.info("Created: " + securityManager);
        SecurityUtils.setSecurityManager(securityManager);
        this.securityManager = securityManager;
        aclResolver = new SecurityServiceAclResolver(accessControlStore);
        try {
            passwordValidator = createPasswordValidator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private Zxcvbn createPasswordValidator() throws IOException {
        final ZxcvbnBuilder builder = new ZxcvbnBuilder();
        builder.dictionaries(StandardDictionaries.loadAllDictionaries());
        builder.keyboards(StandardKeyboards.loadAllKeyboards());
        return builder
                .keyboard(new SlantedKeyboardLoader("qwertz", new ClasspathResource("qwertz-keyboard.txt")).load())
                .build();
    }
    
    @Override
    public ClassLoader getDeserializationClassLoader() {
        return initialLoadClassLoaderRegistry.getCombinedMasterDataClassLoader();
    }

    @Override
    public ClassLoaderRegistry getInitialLoadClassLoaderRegistry() {
        return initialLoadClassLoaderRegistry;
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
    
    private ConcurrentMap<String, Pair<Boolean, Set<String>>> loadCORSFilterConfigurations() {
        logger.info("Loading CORS filter configurations");
        final ConcurrentMap<String, Pair<Boolean, Set<String>>> result = new ConcurrentHashMap<>();
        result.putAll(PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory().loadCORSFilterConfigurationsForReplicaSetNames());
        if (result.containsKey(ServerInfo.getName())) {
            final Pair<Boolean, Set<String>> thisServersCORSFilterConfig = result.get(ServerInfo.getName());
            if (thisServersCORSFilterConfig.getA()) {
                getCORSFilterConfiguration().setWildcard();
            } else {
                getCORSFilterConfiguration().setOrigins(thisServersCORSFilterConfig.getB());
            }
        }
        return result;
    }
    
    @Override
    public Iterable<? extends HasPermissions> getAllHasPermissions() {
        return hasPermissionsProvider.getAllHasPermissions();
    }
    
    @Override
    public Map<Serializable, SubscriptionPlan> getAllSubscriptionPlans() {
        return subscriptionPlanProvider.getAllSubscriptionPlans();
    }
    
    @Override
    public SubscriptionPlan getSubscriptionPlanById(String planId) {
        return subscriptionPlanProvider.getAllSubscriptionPlans().get(planId);
    }
    
    @Override
    public SubscriptionPlan getSubscriptionPlanByItemPriceId(String itemPriceId) {
        SubscriptionPlan result = null;
        final Map<Serializable, SubscriptionPlan> allSubscriptionPlans = getAllSubscriptionPlans();
        for (SubscriptionPlan plan : allSubscriptionPlans.values()) {
            for (SubscriptionPrice price : plan.getPrices()) {
                if (itemPriceId.equals(price.getPriceId())) {
                    result = plan;
                }
            }
        }
        return result;
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
                        null, /* clientIP */ null, /* enforce strong password */ false);
                setOwnership(adminUser.getIdentifier(), adminUser, null);
                Role adminRole = new Role(adminRoleDefinition, /* transitive */ true);
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

    private CORSFilterConfiguration getCORSFilterConfiguration() {
        return corsFilterConfigurationTracker == null ? null : corsFilterConfigurationTracker.getService();
    }
    
    private ReplicationService getReplicationService() {
        return replicationServiceTracker == null ? null : replicationServiceTracker.getService();
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
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is starting password reset for user "+username);
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
        logger.info("Password reset for user "+username+" requested");
        getUserByName(username).startPasswordReset(passwordResetSecret);
        return null;
    }

    @Override
    public SecurityManager getSecurityManager() {
        return this.securityManager;
    }

    @Override
    public OwnershipAnnotation getOwnership(QualifiedObjectIdentifier idOfOwnedObjectAsString) {
        return accessControlStore.getOwnership(idOfOwnedObjectAsString);
    }
    
    private UserGroup getDefaultTenantForUser(User user) {
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
    public void setTemporaryDefaultTenant(final UUID tenantGroupId) {
        if (tenantGroupId != null || getCurrentUser() == null) {
            final UserGroup tenantGroup = getUserGroup(tenantGroupId);
            if (tenantGroup == null) {
                temporaryDefaultTenant.remove();
            } else {
                if (Util.contains(getUserGroupsOfUser(getCurrentUser()), tenantGroup)) {
                    temporaryDefaultTenant.set(tenantGroup);
                } else {
                    logger.warning("User " + getCurrentUser().getName()
                            + " tried to set foreign temporary default tenant group " + tenantGroupId.toString());
                }
            }
        } else {
            temporaryDefaultTenant.remove();
        }
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
    
    public AccessControlListAnnotation getOrCreateAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObjectAsString) {
        return accessControlStore.getOrCreateAcl(idOfAccessControlledObjectAsString);
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
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is clearing ACL of object with ID "+idOfAccessControlledObjectAsString);
        apply(new SetEmptyAccessControlListOperation(idOfAccessControlledObjectAsString, displayNameOfAccessControlledObject));
        return this;
    }

    @Override
    public Void internalSetEmptyAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject, String displayNameOfAccessControlledObject) {
        permissionChangeListeners.aclChanged(idOfAccessControlledObject);
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
            logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is setting the ACL for "+idOfAccessControlledObject+" with actions "+actionsToSet);
            apply(new AclPutPermissionsOperation(idOfAccessControlledObject, userGroupId, actionsToSet));
        }
        return accessControlStore.getAccessControlList(idOfAccessControlledObject).getAnnotation();
    }
    
    @Override
    public Void internalAclPutPermissions(QualifiedObjectIdentifier idOfAccessControlledObject, UUID groupId, Set<String> actions) {
        permissionChangeListeners.aclChanged(idOfAccessControlledObject);
        accessControlStore.setAclPermissions(idOfAccessControlledObject, getUserGroup(groupId), actions);
        return null;
    }

    /*
     * @param name The name of the user group to add
     */
    @Override
    public AccessControlList addToAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject,
            UserGroup group, String action) {
        if (getAccessControlList(idOfAccessControlledObject) == null) {
            setEmptyAccessControlList(idOfAccessControlledObject);
        }
        final UUID groupId = group == null ? null : group.getId();
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is adding permission "+action+" for group with ID "+groupId+" to the ACL for "+idOfAccessControlledObject);
        apply(new AclAddPermissionOperation(idOfAccessControlledObject, groupId, action));
        return accessControlStore.getAccessControlList(idOfAccessControlledObject).getAnnotation();
    }

    @Override
    public Void internalAclAddPermission(QualifiedObjectIdentifier idOfAccessControlledObject, UUID groupId, String action) {
        permissionChangeListeners.aclChanged(idOfAccessControlledObject);
        accessControlStore.addAclPermission(idOfAccessControlledObject, getUserGroup(groupId), action);
        return null;
    }

    /*
     * @param name The name of the user group to remove
     */
    @Override
    public AccessControlList removeFromAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject,
            UserGroup group, String permission) {
        final AccessControlList result;
        if (getAccessControlList(idOfAccessControlledObject) != null) {
            final UUID groupId = group == null ? null : group.getId();
            logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is removing permission "+permission+" for group with ID "+groupId+" from the ACL for "+idOfAccessControlledObject);
            apply(new AclRemovePermissionOperation(idOfAccessControlledObject, groupId, permission));
            result = accessControlStore.getAccessControlList(idOfAccessControlledObject).getAnnotation();
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Void internalAclRemovePermission(QualifiedObjectIdentifier idOfAccessControlledObject, UUID groupId, String permission) {
        permissionChangeListeners.aclChanged(idOfAccessControlledObject);
        accessControlStore.removeAclPermission(idOfAccessControlledObject, getUserGroup(groupId), permission);
        return null;
    }

    @Override
    public void deleteAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject) {
        if (getAccessControlList(idOfAccessControlledObject) != null) {
            logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is deleting the ACL of object "+idOfAccessControlledObject);
            apply(new DeleteAclOperation(idOfAccessControlledObject));
        }
    }

    @Override
    public Void internalDeleteAcl(QualifiedObjectIdentifier idOfAccessControlledObject) {
        permissionChangeListeners.aclChanged(idOfAccessControlledObject);
        accessControlStore.removeAccessControlList(idOfAccessControlledObject);
        return null;
    }

    @Override
    public Ownership setOwnership(QualifiedObjectIdentifier objectId, User userOwner,
            UserGroup tenantOwner) {
        return setOwnership(objectId, userOwner, tenantOwner, /* displayNameOfOwnedObject */ null);
    }

    @Override
    public Ownership setOwnership(QualifiedObjectIdentifier objectId, User userOwner,
            UserGroup tenantOwner, String displayNameOfOwnedObject) {
        if (userOwner == null && tenantOwner == null) {
            throw new IllegalArgumentException("No owner is not valid, would create non changeable object");
        }
        final UUID tenantId = tenantOwner == null ? null : tenantOwner.getId();
        final String userOwnerName = userOwner == null ? null : userOwner.getName();
        final OwnershipAnnotation existingOwnership = getOwnership(objectId);
        final User existingUserOwner;
        final UserGroup existingTenantOwner;
        final String existingDisplayNameOfOwnedObject;
        final Ownership result;
        if (existingOwnership == null || existingOwnership.getAnnotation() == null) {
            existingUserOwner = null;
            existingTenantOwner = null;
        } else {
            existingUserOwner = existingOwnership.getAnnotation().getUserOwner();
            existingTenantOwner = existingOwnership.getAnnotation().getTenantOwner();
        }
        if (existingOwnership == null) {
            existingDisplayNameOfOwnedObject = null;
        } else {
            existingDisplayNameOfOwnedObject = existingOwnership.getDisplayNameOfAnnotatedObject();
        }
        if (Util.equalsWithNull(existingDisplayNameOfOwnedObject, displayNameOfOwnedObject)
            && existingUserOwner == userOwner
            && existingTenantOwner == tenantOwner) {
            result = existingOwnership.getAnnotation();
        } else {
            logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is setting ownership of object "+objectId+" to group with ID "+tenantId+" and user "+userOwnerName);
            result = apply(new SetOwnershipOperation(objectId, userOwnerName, tenantId,
                    displayNameOfOwnedObject));
        }
        return result;
    }
    
    @Override
    public Ownership internalSetOwnership(QualifiedObjectIdentifier objectId, String userOwnerName, UUID tenantOwnerId, String displayName) {
        final Ownership result = accessControlStore.setOwnership(objectId, getUserByName(userOwnerName), getUserGroup(tenantOwnerId), displayName).getAnnotation();
        permissionChangeListeners.ownershipChanged(objectId);
        return result;
    }

    @Override
    public void deleteOwnership(QualifiedObjectIdentifier objectId) {
        if (getOwnership(objectId) != null) {
            logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is deleting ownership information from object with ID "+objectId);
            apply(new DeleteOwnershipOperation(objectId));
        }
    }

    @Override
    public Void internalDeleteOwnership(QualifiedObjectIdentifier objectId) {
        accessControlStore.removeOwnership(objectId);
        permissionChangeListeners.ownershipChanged(objectId);
        return null;
    }

    @Override
    public Iterable<UserGroup> getUserGroupList() {
        return store.getUserGroups();
    }

    @Override
    public Iterable<UserGroup> getUserGroupsWithRoleDefinition(RoleDefinition roleDefinition) {
        return store.getUserGroupsWithRoleDefinition(roleDefinition);
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
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is creating user group " + name + " with ID " + id);
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
        final User user = getUserByName(username);
        userGroup.add(user);
        permissionChangeListeners.userAddedToOrRemovedFromGroup(user, userGroup);
        store.updateUserGroup(userGroup);
        return null;
    }
    
    @Override
    public Void internalRemoveUserFromUserGroup(UUID groupId, String username) {
        final UserGroup userGroup = getUserGroup(groupId);
        final User user = getUserByName(username);
        permissionChangeListeners.userAddedToOrRemovedFromGroup(user, userGroup);
        userGroup.remove(user);
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
        final RoleDefinition roleDefinition = getRoleDefinition(roleDefinitionId);
        permissionChangeListeners.roleAddedToOrRemovedFromGroup(userGroup, roleDefinition);
        userGroup.put(roleDefinition, forAll);
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
        final RoleDefinition roleDefinition = getRoleDefinition(roleDefinitionId);
        permissionChangeListeners.roleAddedToOrRemovedFromGroup(userGroup, roleDefinition);
        userGroup.remove(roleDefinition);
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
            final Iterable<OwnershipAnnotation> ownerhipsWithGroupOwner = accessControlStore.getOwnerhipsWithGroupOwner(userGroup);
            accessControlStore.removeAllOwnershipsFor(userGroup);
            store.deleteUserGroup(userGroup);
            for (final OwnershipAnnotation ownershipWithGroupAsOwner : ownerhipsWithGroupOwner) {
                permissionChangeListeners.ownershipChanged(ownershipWithGroupAsOwner.getIdOfAnnotatedObject());
            }
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
    public Iterable<User> getUsersWithPermissions(WildcardPermission permission) {
        if (Util.size(permission.getQualifiedObjectIdentifiers()) != 1) {
            throw new IllegalArgumentException("Permission needs to specify exactly one object identifier");
        }
        final ScheduledExecutorService foregroundExecutor = ThreadPoolUtil.INSTANCE.getDefaultForegroundTaskThreadPoolExecutor();
        final int numberOfJobs = ThreadPoolUtil.INSTANCE.getReasonableThreadPoolSize();
        final ConcurrentMap<User, Boolean> result = new ConcurrentHashMap<>();
        final User allUser = getAllUser();
        final ConcurrentLinkedDeque<User> userList = new ConcurrentLinkedDeque<>();
        Util.addAll(getUserList(), userList);
        final Set<Future<?>> futures = new HashSet<>();
        final QualifiedObjectIdentifier objectIdentifier = permission.getQualifiedObjectIdentifiers().iterator().next();
        final OwnershipAnnotation ownership = accessControlStore.getOwnership(objectIdentifier);
        final AccessControlListAnnotation acl = accessControlStore.getAccessControlList(objectIdentifier);
        // create as many jobs as we expect the thread pool size to be and have each of them
        // keep polling User objects and check whether that user has the permission sought; if so,
        // add the User to the result.
        // result and userList are thread-safe data structures
        for (int i=0; i<numberOfJobs; i++) {
            futures.add(foregroundExecutor.submit(()->{
                User user;
                int usersHandled = 0;
                while ((user=userList.poll()) != null) {
                    usersHandled++;
                    if (PermissionChecker.isPermitted(permission, user, allUser,
                            ownership == null ? null : ownership.getAnnotation(), acl == null ? null : acl.getAnnotation())) {
                        result.put(user, true);
                    }
                }
                final int finalUsersHandled = usersHandled;
                logger.fine(()->"Handled "+finalUsersHandled+" users in job "+this);
            }));
        }
        for (final Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.log(Level.WARNING, "Exception while trying to wait for user permission check", e);
            }
        }
        return result.keySet();
    }

    @Override
    public User createSimpleUser(final String username, final String email, String password, String fullName,
            String company, Locale locale, final String validationBaseURL, UserGroup groupOwningUser,
            String requestClientIP, boolean enforceStrongPassword) throws UserManagementException, MailException, UserGroupManagementException {
        if (requestClientIP != null) {
            checkAndRecordUserCreationFromClientIP(requestClientIP);
        }
        logger.info("Creating user "+username);
        if (store.getUserByName(username) != null) {
            logger.warning("User "+username+" already exists");
            throw new UserManagementException(UserManagementException.USER_ALREADY_EXISTS);
        }
        if (username == null || username.length() < 3) {
            throw new UserManagementException(UserManagementException.USERNAME_DOES_NOT_MEET_REQUIREMENTS);
        } else if (enforceStrongPassword && !isPasswordGoodEnough(password)) {
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

    private boolean isPasswordGoodEnough(String password) {
        final boolean result;
        if (password == null) {
            result = false;
        } else {
            final Strength strength = passwordValidator.measure(password);
            result = strength.getGuessesLog10() > 8;
        }
        return result;
    }
    
    /**
     * Checks if the {@code clientIP} is currently blocked for user creation, and if so, throws a
     * {@link UserManagementException}. If the check is successful, it records a locking record for
     * this client IP, similar to the locking/banning that happens for failer bearer token
     * authentication requests (see {@link #failedBearerTokenAuthentication(String)}).
     */
    private void checkAndRecordUserCreationFromClientIP(String clientIP) throws UserManagementException {
        assert clientIP != null;
        // synchronize to ensure that no two threads can enter values into the map concurrently;
        // still the use of a ConcurrentMap is justified because there may be concurrent write access
        // through replication
        synchronized (clientIPBasedLockingAndBanningForUserCreation) {
            final LockingAndBanning lockingAndBanning = clientIPBasedLockingAndBanningForUserCreation.get(clientIP);
            if (lockingAndBanning == null || !lockingAndBanning.isAuthenticationLocked()) {
                apply(s->s.internalRecordUserCreationFromClientIP(clientIP));
            } else {
                throw new UserManagementException(UserManagementException.CLIENT_CURRENTLY_LOCKED_FOR_USER_CREATION);
            }
        }
    }
    
    @Override
    public LockingAndBanning internalRecordUserCreationFromClientIP(String clientIP) {
        final LockingAndBanning result = new LockingAndBanningImpl(TimePoint.now().plus(DEFAULT_CLIENT_IP_BASED_USER_CREATION_LOCKING_DURATION),
                DEFAULT_CLIENT_IP_BASED_USER_CREATION_LOCKING_DURATION);
        clientIPBasedLockingAndBanningForUserCreation.put(clientIP, result);
        scheduleCleanUpTask(clientIP, result, clientIPBasedLockingAndBanningForUserCreation,
                "client IPs locked for user creation");
        return result;
    }

    private void addUserRoleToUser(final User user) {
        addRoleForUserAndSetUserAsOwner(user, new Role(store.getRoleDefinitionByPrototype(UserRole.getInstance()),
                /* tenant qualifier */ null, /* user qualifier */ user, /* transitive */ true));
    }
    
    private void addUserRoleForGroupToUser(final UserGroup group, final User user) {
        addRoleForUserAndSetUserAsOwner(user, new Role(store.getRoleDefinitionByPrototype(UserRole.getInstance()),
                /* tenant qualifier */ group, /* user qualifier */ null, /* transitive */ true));
    }
    
    /**
     * A simple {@code synchronized} block synchronizing on the {@link #userGroupLock} monitor is used
     * in the {@link #getOrCreateTenantForUser(User)} method to avoid race conditions between multiple
     * threads, such as the migration code run in a background thread by the {@link Activator}'s {@code migrate}
     * method, and the regular creation of a user which also probes for the user's default group.
     */
    private final static Object userGroupLock = new Object();
    private UserGroup getOrCreateTenantForUser(User user) throws UserGroupManagementException {
        final String username = user.getName();
        final String defaultTenantNameForUsername = getDefaultTenantNameForUsername(username);
        synchronized (userGroupLock) {
            UserGroup tenant = store.getUserGroupByName(defaultTenantNameForUsername);
            if (tenant != null) {
                logger.info("Found existing tenant "+defaultTenantNameForUsername+" to be used as default tenant for new user "+username);
            } else {
                logger.info("Creating user group "+defaultTenantNameForUsername+" as default tenant for new user "+username);
                tenant = createUserGroupWithInitialUser(UUID.randomUUID(), defaultTenantNameForUsername, user);
                setOwnership(tenant.getIdentifier(), user, tenant);
            }
            return tenant;
        }
    }
    
    @Override
    public User internalCreateUser(String username, String email, Account... accounts) throws UserManagementException {
        final User result = store.createUser(username, email, new LockingAndBanningImpl(), accounts);
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
        if (!isPasswordGoodEnough(newPassword)) {
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
        logger.info("Password for user "+username+" was updated by "+SessionUtils.getPrincipal());
        user.passwordWasReset();
        store.updateUser(user);
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
        if (user.getLockingAndBanning().isAuthenticationLocked()) {
            throw new UserManagementException(UserManagementException.PASSWORD_AUTHENTICATION_CURRENTLY_LOCKED_FOR_USER);
        }
        final UsernamePasswordAccount account = (UsernamePasswordAccount) user.getAccount(AccountType.USERNAME_PASSWORD);
        String hashedOldPassword = hashPassword(password, account.getSalt());
        final boolean result = Util.equalsWithNull(hashedOldPassword, account.getSaltedPassword());
        if (!result) {
            logger.info("Failed password check for user "+username);
            apply(s->s.internalFailedPasswordAuthentication(username));
        } else {
            apply(s->s.internalSuccessfulPasswordAuthentication(username));
        }
        return result;
    }
    
    @Override
    public LockingAndBanning failedPasswordAuthentication(User user) {
        return apply(s->s.internalFailedPasswordAuthentication(user.getName()));
    }

    @Override
    public LockingAndBanning internalFailedPasswordAuthentication(String username) {
        final User user = getUserByName(username);
        final LockingAndBanning lockingAndBanning;
        if (user != null) {
            lockingAndBanning = user.getLockingAndBanning();
            lockingAndBanning.failedPasswordAuthentication();
            store.updateUser(user);
            logger.info("failed password authentication for user "+username+"; locking: "+lockingAndBanning);
        } else {
            lockingAndBanning = null;
        }
        return lockingAndBanning;
    }

    @Override
    public void successfulPasswordAuthentication(User user) {
        // replicate only if this really implied a change
        if (internalSuccessfulPasswordAuthentication(user.getName())) {
            replicate(s->s.internalSuccessfulPasswordAuthentication(user.getName()));
        }
    }
    
    @Override
    public Boolean internalSuccessfulPasswordAuthentication(String username) {
        final boolean changed;
        final User user = getUserByName(username);
        if (user != null) {
            changed = user.getLockingAndBanning().successfulPasswordAuthentication();
            if (changed) {
                store.updateUser(user);
            }
        } else {
            changed = false;
        }
        return changed;
    }

    @Override
    public LockingAndBanning failedBearerTokenAuthentication(String clientIP) {
        final LockingAndBanning result;
        final ReplicationService replicationService = getReplicationService();
        if (replicationService == null || !replicationService.isReplicationStarting()) {
            result = apply(s->s.internalFailedBearerTokenAuthentication(clientIP));
        } else {
            logger.warning("Replication is starting, so not recording failed bearer token authentication for client IP "+clientIP);
            result = null;
        }
        return result;
    }
    
    @Override
    public LockingAndBanning internalFailedBearerTokenAuthentication(String clientIP) {
        final LockingAndBanning lockingAndBanning = clientIPBasedLockingAndBanningForBearerTokenAuthentication.computeIfAbsent(escapeNullClientIP(clientIP), key->new LockingAndBanningImpl());
        lockingAndBanning.failedPasswordAuthentication();
        logger.info("failed bearer token authentication from client IP "+clientIP+"; locking: "+lockingAndBanning);
        scheduleCleanUpTask(clientIP, lockingAndBanning, clientIPBasedLockingAndBanningForBearerTokenAuthentication,
                "client IPs locked for bearer token authentication");
        return lockingAndBanning;
    }

    /**
     * Schedule a clean-up task to avoid leaking memory for the LockingAndBanning objects; schedule it in two times the
     * locking expiry of {@code lockingAndBanning}, but at least one hour, because if no authentication failure occurs
     * for that IP/user agent combination, we will entirely remove the {@link LockingAndBanning} from the map,
     * effectively resetting that IP to a short default locking duration again; this way, if during the double
     * expiration time another failed attempt is registered, we can still grow the locking duration because we have kept
     * the {@link LockingAndBanning} object available for a bit longer. Furthermore, for authentication requests, the
     * responsible {@link Realm} will let authentication requests get to here only if not locked, so if we were to
     * expunge entries immediately as they unlock, the locking duration could never grow.<p>
     * 
     * With the minimum of one hour, we ensure that failing requests done at a slower rate still grow the locking
     * expiry duration.
     */
    private void scheduleCleanUpTask(final String clientIPOrNull,
            final LockingAndBanning lockingAndBanning,
            final ConcurrentMap<String, LockingAndBanning> mapToRemoveFrom,
            final String nameOfMapForLog) {
        final long millisUntilLockingExpiry = Math.max(
                2*ApproximateTime.approximateNow().until(lockingAndBanning.getLockedUntil()).asMillis(),
                Duration.ONE_HOUR.asMillis());
        ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor().schedule(
                ()->{
                    final LockingAndBanning lab = mapToRemoveFrom.get(escapeNullClientIP(clientIPOrNull));
                    if (lab != null && !lab.isAuthenticationLocked()) {
                        mapToRemoveFrom.remove(escapeNullClientIP(clientIPOrNull));
                        logger.info("Removed "+clientIPOrNull+" from "+nameOfMapForLog+"; "
                                +mapToRemoveFrom.size()
                                +" locked client IP(s) remaining");
                    }
                },
                millisUntilLockingExpiry, TimeUnit.MILLISECONDS);
    }

    private String escapeNullClientIP(String clientIP) {
        return clientIP==null?CLIENT_IP_NULL_ESCAPE:clientIP;
    }

    @Override
    public void successfulBearerTokenAuthentication(String clientIP) {
        // replicate only if this truly caused a change in locking/banning:
        if (internalSuccessfulBearerTokenAuthentication(clientIP)) {
            replicate(s->s.internalSuccessfulBearerTokenAuthentication(clientIP));
        }
    }
    
    @Override
    public Boolean internalSuccessfulBearerTokenAuthentication(String clientIP) {
        final boolean changed;
        final LockingAndBanning lockingAndBanning = clientIPBasedLockingAndBanningForBearerTokenAuthentication.remove(escapeNullClientIP(clientIP));
        if (lockingAndBanning != null) {
            logger.info("Unlocked bearer token authentication from "+clientIP+"; last locking state was "+lockingAndBanning);
            changed = true;
        } else {
            changed = false;
        }
        return changed;
    }

    @Override
    public boolean isClientIPLockedForBearerTokenAuthentication(String clientIP) {
        final LockingAndBanning lockingAndBanning = clientIPBasedLockingAndBanningForBearerTokenAuthentication.get(escapeNullClientIP(clientIP));
        return lockingAndBanning != null && lockingAndBanning.isAuthenticationLocked();
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
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is changing e-mail address of user " + username + " to " + newEmail);
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
        store.updateUser(user);
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
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" created role "+name+" with ID "+roleId);
        return apply(new CreateRoleDefinitionOperation(roleId, name));
    }

    @Override
    public RoleDefinition internalCreateRoleDefinition(UUID roleId, String name) {
        return store.createRoleDefinition(roleId, name, Collections.emptySet());
    }
    
    @Override
    public void deleteRoleDefinition(RoleDefinition roleDefinition) {
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" deleted role "+roleDefinition);
        final UUID roleId = roleDefinition.getId();
        apply(new DeleteRoleDefinitionOperation(roleId));
    }

    @Override
    public Void internalDeleteRoleDefinition(UUID roleId) {
        final RoleDefinition roleDefinition = store.getRoleDefinition(roleId);
        permissionChangeListeners.roleDefinitionRemoved(roleDefinition);
        store.removeRoleDefinition(roleDefinition);
        return null;
    }

    @Override
    public void updateRoleDefinition(RoleDefinition roleDefinitionWithNewProperties) {
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" updated role "+roleDefinitionWithNewProperties);
        apply(new UpdateRoleDefinitionOperation(roleDefinitionWithNewProperties));
    }

    @Override
    public Void internalUpdateRoleDefinition(RoleDefinition roleWithNewProperties) {
        final RoleDefinition role = store.getRoleDefinition(roleWithNewProperties.getId());
        role.setName(roleWithNewProperties.getName());
        store.setRoleDefinitionDisplayName(roleWithNewProperties.getId(), role.getName());
        permissionChangeListeners.permissionAddedToOrRemovedFromRoleDefinition(role, role.getPermissions(), roleWithNewProperties.getPermissions());
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
        setOwnership(qualifiedTypeIdentifier, user, /* owning group */ null);
    }

    @Override
    public void addRoleForUser(User user, Role role) {
        addRoleForUser(user.getName(), role);
    }

    @Override
    public void addRoleForUser(String username, Role role) {
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" added role "+role+" to user "+username);
        final UUID roleDefinitionId = role.getRoleDefinition().getId();
        final UUID idOfTenantQualifyingRole = role.getQualifiedForTenant() == null ? null : role.getQualifiedForTenant().getId();
        final String nameOfUserQualifyingRole = role.getQualifiedForUser() == null ? null : role.getQualifiedForUser().getName();
        final Boolean transitive = role.isTransitive();
        apply(new AddRoleForUserOperation(username, roleDefinitionId, idOfTenantQualifyingRole, nameOfUserQualifyingRole, transitive));
    }

    @Override
    public Void internalAddRoleForUser(String username, UUID roleDefinitionId, UUID idOfTenantQualifyingRole,
            String nameOfUserQualifyingRole, Boolean transitive) throws UserManagementException {
        final Role role = new Role(getRoleDefinition(roleDefinitionId),
                getUserGroup(idOfTenantQualifyingRole), getUserByName(nameOfUserQualifyingRole), transitive);
        permissionChangeListeners.roleAddedToOrRemovedFromUser(getUserByName(username), role);
        store.addRoleForUser(username, role);
        return null;
    }

    @Override
    public void removeRoleFromUser(User user, Role role) {
        removeRoleFromUser(user.getName(), role);
    }
    
    @Override
    public void removeRoleFromUser(String username, Role role) {
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" removed role "+role+" to user "+username);
        final UUID roleDefinitionId = role.getRoleDefinition().getId();
        final UUID idOfTenantQualifyingRole = role.getQualifiedForTenant() == null ? null : role.getQualifiedForTenant().getId();
        final String nameOfUserQualifyingRole = role.getQualifiedForUser() == null ? null : role.getQualifiedForUser().getName();
        final Boolean transitive = role.isTransitive();
        apply(new RemoveRoleFromUserOperation(username, roleDefinitionId, idOfTenantQualifyingRole, nameOfUserQualifyingRole, transitive));
    }

    @Override
    public Void internalRemoveRoleFromUser(String username, UUID roleDefinitionId, UUID idOfTenantQualifyingRole,
            String nameOfUserQualifyingRole, Boolean transitive) throws UserManagementException {
        final Role role = new Role(getRoleDefinition(roleDefinitionId),
                getUserGroup(idOfTenantQualifyingRole), getUserByName(nameOfUserQualifyingRole), transitive);
        permissionChangeListeners.roleAddedToOrRemovedFromUser(getUserByName(username), role);
        store.removeRoleFromUser(username, role);
        return null;
    }

    @Override
    public void removePermissionFromUser(String username, WildcardPermission permissionToRemove) {
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is removing permission "+permissionToRemove+" from user "+username);
        apply(new RemovePermissionForUserOperation(username, permissionToRemove));
    }

    @Override
    public Void internalRemovePermissionForUser(String username, WildcardPermission permissionToRemove) throws UserManagementException {
        permissionChangeListeners.permissionAddedToOrRemovedFromUser(getUserByName(username), permissionToRemove);
        store.removePermissionFromUser(username, permissionToRemove);
        return null;
    }

    @Override
    public void addPermissionForUser(String username, WildcardPermission permissionToAdd) {
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is adding permission "+permissionToAdd+" to user "+username);
        apply(new AddPermissionForUserOperation(username, permissionToAdd));
    }

    @Override
    public Void internalAddPermissionForUser(String username, WildcardPermission permissionToAdd) throws UserManagementException {
        permissionChangeListeners.permissionAddedToOrRemovedFromUser(getUserByName(username), permissionToAdd);
        store.addPermissionForUser(username, permissionToAdd);
        return null;
    }

    @Override
    public void deleteUser(String username) throws UserManagementException {
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is deleting user "+username);
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
            permissionChangeListeners.userDeleted(userToDelete);
            // remove all permissions the user has
            accessControlStore.removeAllOwnershipsFor(userToDelete);
            store.deleteUser(username);
            final String defaultTenantNameForUsername = getDefaultTenantNameForUsername(username);
            final UserGroup defaultTenantUserGroup = getUserGroupByName(defaultTenantNameForUsername);
            if (defaultTenantUserGroup != null) {
                List<User> usersInGroupList = Util.asList(defaultTenantUserGroup.getUsers());
                if (usersInGroupList.isEmpty()) {
                    // no other user is in group, delete it as well and remove Owenerships
                    try {
                        internalDeleteUserGroup(defaultTenantUserGroup.getId());
                    } catch (UserGroupManagementException e) {
                        logger.log(Level.SEVERE, "Could not delete default tenant for user", e);
                    }
                }
            }
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
    public String getOrCreateTargetServerBearerToken(String targetServerUrlAsString, String targetServerUsername,
            String targetServerPassword, String targetServerBearerToken) {
        if ((Util.hasLength(targetServerUsername) || Util.hasLength(targetServerPassword))
                && Util.hasLength(targetServerBearerToken)) {
            final IllegalArgumentException e = new IllegalArgumentException("Please use either username/password or bearer token, not both.");
            logger.log(Level.WARNING, e.getMessage(), e);
            throw e;
        }
        final User user = getCurrentUser();
        // Default to current user's token
        final String effectiveTargetServerBearerToken;
        if (!Util.hasLength(targetServerUsername) && !Util.hasLength(targetServerPassword) && !Util.hasLength(targetServerBearerToken)) {
            effectiveTargetServerBearerToken = user == null ? null : getOrCreateAccessToken(user.getName());
        } else {
            effectiveTargetServerBearerToken = targetServerBearerToken;
        }
        final String token = (!Util.hasLength(effectiveTargetServerBearerToken)
                ? targetServerUsername != null ?
                        RemoteServerUtil.resolveBearerTokenForRemoteServer(targetServerUrlAsString, targetServerUsername, targetServerPassword) :
                        null // in case no effective bearer token has been provided but no user name either
                : effectiveTargetServerBearerToken);
        return token;
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
        logger.info("Subject "+SecurityUtils.getSubject().getPrincipal()+" is requesting a new access token for user "+username);
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
        final Subject subject = SecurityUtils.getSubject();
        if (hasCurrentUserUpdatePermission(getUserByName(username))) {
            logger.info("Subject "+subject.getPrincipal()+" is removing the access token for user "+username);
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
                securityDisplayName, actionWithResult, /* check for SERVER:CREATE_OBJECT */ true);
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
     */
    @Override
    public User checkPermissionForUserCreationAndRevertOnErrorForUserCreation(String username,
            Callable<User> createActionReturningCreatedObject) throws UserManagementException {
        QualifiedObjectIdentifier identifier = SecuredSecurityTypes.USER
                .getQualifiedObjectIdentifier(UserImpl.getTypeRelativeObjectIdentifier(username));
        User result = null;
        try {
            SecurityUtils.getSubject().checkPermission(identifier.getStringPermission(DefaultActions.CREATE));
            result = createActionReturningCreatedObject.call();
        } catch (AuthorizationException e) {
            logger.warning("Unauthorized request to create user with name \""+username+"\": "+e.getMessage());
            throw e;
        } catch (UserManagementException e) {
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
        deleteOwnership(identifier);
        deleteAccessControlList(identifier);
    }

    @Override
    public <T extends WithQualifiedObjectIdentifier> void filterObjectsWithPermissionForCurrentUser(
            HasPermissions.Action action, Iterable<T> objectsToFilter,
            Consumer<T> filteredObjectsConsumer) {
        objectsToFilter.forEach(objectToCheck -> {
            if (SecurityUtils.getSubject().isPermitted(new ShiroWildcardPermissionFromParts(
                    objectToCheck.getIdentifier().getPermission(action)))) {
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
                        .isPermitted(new ShiroWildcardPermissionFromParts(objectToCheck.getIdentifier().getPermission(actions[i])))) {
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
        return PermissionChecker.checkMetaPermission(permissionToCheck,
                hasPermissionsProvider.getAllHasPermissions(), getCurrentUser(), getAllUser(), ownership,
                aclResolver);
    }
    
    @Override
    public boolean hasCurrentUserMetaPermissionWithOwnershipLookup(WildcardPermission permissionToCheck) {
        return PermissionChecker.checkMetaPermissionWithOwnershipResolution(permissionToCheck,
                hasPermissionsProvider.getAllHasPermissions(), getCurrentUser(), getAllUser(),
                qualifiedObjectId -> {
                    OwnershipAnnotation ownershipAnnotation = accessControlStore.getOwnership(qualifiedObjectId);
                    return ownershipAnnotation == null ? null : ownershipAnnotation.getAnnotation();
                }, aclResolver);
    }
    
    @Override
    public boolean hasCurrentUserAnyPermission(WildcardPermission permissionToCheck) {
        User currentUser = getCurrentUser();
        return PermissionChecker.hasUserAnyPermission(permissionToCheck,
                hasPermissionsProvider.getAllHasPermissions(), currentUser, getAllUser(), /* ownership */ null);
    }
    
    @Override
    public boolean hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(final RoleDefinition roleDefinition,
            final Ownership qualificationForGrantedPermissions) {
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
    public RoleDefinition getOrCreateRoleDefinitionFromPrototype(final RolePrototype rolePrototype, boolean makeReadableForAll) {
        final RoleDefinition potentiallyExistingRoleDefinition = store.getRoleDefinition(rolePrototype.getId());
        final RoleDefinition result;
        if (potentiallyExistingRoleDefinition == null) {
            result = store.createRoleDefinition(rolePrototype.getId(), rolePrototype.getName(), rolePrototype.getPermissions());
            setOwnership(result.getIdentifier(), null, getServerGroup());
        } else if (rolePrototype.getPermissions() != null
                && !rolePrototype.getPermissions().equals(potentiallyExistingRoleDefinition.getPermissions())) {
            store.setRoleDefinitionPermissions(potentiallyExistingRoleDefinition.getId(),
                    rolePrototype.getPermissions());
            RoleDefinition roleDefinition = store.getRoleDefinition(rolePrototype.getId());
            result = roleDefinition;
        } else {
            result = potentiallyExistingRoleDefinition;
        }
        if (makeReadableForAll) {
            AccessControlListAnnotation acl = getOrCreateAccessControlList(result.getIdentifier());
            final Set<String> allowedActions = acl.getAnnotation().getAllowedActions(null);
            if (!allowedActions.contains(DefaultActions.READ.name())) {
                // make role publicly readable
                addToAccessControlList(result.getIdentifier(), 
                        /* for all users */ null, DefaultActions.READ.name());
            }
        }
        return result;
    }
    
    @Override
    public void setCORSFilterConfigurationToWildcard(String serverName) {
        apply(s->s.internalSetCORSFilterConfigurationToWildcard(serverName));
    }
    
    @Override
    public Void internalSetCORSFilterConfigurationToWildcard(String serverName) {
        if (Util.equalsWithNull(serverName, ServerInfo.getName())) {
            getCORSFilterConfiguration().setWildcard();
        }
        corsFilterConfigurationsByReplicaSetName.put(serverName, new Pair<>(true, Collections.emptySet()));
        PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().storeCORSFilterConfigurationIsWildcard(serverName);
        return null;
    }
    
    @Override
    public void setCORSFilterConfigurationAllowedOrigins(String serverName, String... allowedOrigins) throws IllegalArgumentException {
        for (final String allowedOrigin : allowedOrigins) {
            if (!HttpHeaderUtil.isValidOriginHeaderValue(allowedOrigin)) {
                throw new IllegalArgumentException("\""+allowedOrigin+"\" is not a valid format for a CORS origin");
            }
        }
        apply(s->s.internalSetCORSFilterConfigurationAllowedOrigins(serverName, allowedOrigins));
    }

    @Override
    public Void internalSetCORSFilterConfigurationAllowedOrigins(String serverName, String... allowedOrigins) {
        final Iterable<String> allowedOriginsAsList = allowedOrigins == null ? Collections.emptyList() : Arrays.asList(allowedOrigins);
        if (Util.equalsWithNull(serverName, ServerInfo.getName())) {
            getCORSFilterConfiguration().setOrigins(allowedOriginsAsList);
        }
        corsFilterConfigurationsByReplicaSetName.put(serverName, new Pair<>(false, Util.asNewSet(allowedOriginsAsList)));
        PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().storeCORSFilterConfigurationAllowedOrigins(serverName, allowedOrigins);
        return null;
    }
    
    @Override
    public Pair<Boolean, Set<String>> getCORSFilterConfiguration(String serverName) {
        return corsFilterConfigurationsByReplicaSetName.get(serverName);
    }

    // ----------------- Replication -------------
    @Override
    public void clearReplicaState() throws MalformedURLException, IOException, InterruptedException {
        store.clear();
        accessControlStore.clear();
        corsFilterConfigurationsByReplicaSetName.clear();
        clientIPBasedLockingAndBanningForBearerTokenAuthentication.clear();
        clientIPBasedLockingAndBanningForUserCreation.clear();
    }

    @Override
    public Serializable getId() {
        return getClass().getName();
    }
    
    @Override
    public ObjectInputStream createObjectInputStreamResolvingAgainstCache(InputStream is, Map<String, Class<?>> classLoaderCache) throws IOException {
        return new ObjectInputStreamResolvingAgainstSecurityCache(is, store, null, classLoaderCache);
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
        logger.info("Reading CORS filter configurations and possibly more...");
        final SecurityServiceInitialLoadExtensionsDTO initialLoadExtensions = (SecurityServiceInitialLoadExtensionsDTO) is.readObject();
        final ConcurrentMap<String, Pair<Boolean, Set<String>>> newCORSFilterConfigurations = initialLoadExtensions.getCorsFilterConfigurationsByReplicaSetName();
        corsFilterConfigurationsByReplicaSetName.putAll(newCORSFilterConfigurations);
        if (initialLoadExtensions.getClientIPBasedLockingAndBanningForBearerTokenAuthentication() != null) {
            // checking for null for backward compatibility; an older primary/master may not have known this field yet
            clientIPBasedLockingAndBanningForBearerTokenAuthentication.putAll(initialLoadExtensions.getClientIPBasedLockingAndBanningForBearerTokenAuthentication());
        }
        if (initialLoadExtensions.getClientIPBasedLockingAndBanningForUserCreation() != null) {
            // checking for null for backward compatibility; an older primary/master may not have known this field yet
            clientIPBasedLockingAndBanningForUserCreation.putAll(initialLoadExtensions.getClientIPBasedLockingAndBanningForUserCreation());
        }
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
        objectOutputStream.writeObject(new SecurityServiceInitialLoadExtensionsDTO(
                corsFilterConfigurationsByReplicaSetName,
                clientIPBasedLockingAndBanningForBearerTokenAuthentication,
                clientIPBasedLockingAndBanningForUserCreation));
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
                    final UserGroup tenantForUser = getOrCreateTenantForUser(user);
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
        // initialize ownerships on migration and fix objects that were orphaned by setting the owning user/group
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
    public void checkMigration(Iterable<? extends HasPermissions> allInstances) {
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
        return object == null ? true
                : SecurityUtils.getSubject().isPermitted(
                        object.getPermissionType().getStringPermissionForObject(DefaultActions.READ, object));
    }

    @Override
    public boolean hasCurrentUserUpdatePermission(WithQualifiedObjectIdentifier object) {
        return object == null ? true
                : SecurityUtils.getSubject().isPermitted(
                        object.getPermissionType().getStringPermissionForObject(DefaultActions.UPDATE, object));
    }

    @Override
    public boolean hasCurrentUserDeletePermission(WithQualifiedObjectIdentifier object) {
        return object == null ? true
                : SecurityUtils.getSubject().isPermitted(
                        object.getPermissionType().getStringPermissionForObject(DefaultActions.DELETE, object));
    }

    @Override
    public boolean hasCurrentUserExplicitPermissions(WithQualifiedObjectIdentifier object,
            HasPermissions.Action... actions) {
        boolean isPermitted = true;
        if (object != null) {
            for (int i = 0; i < actions.length; i++) {
                isPermitted &= SecurityUtils.getSubject()
                        .isPermitted(object.getPermissionType().getStringPermissionForObject(actions[i], object));
            }
        }
        return isPermitted;
    }

    @Override
    public boolean hasCurrentUserOneOfExplicitPermissions(WithQualifiedObjectIdentifier object,
            HasPermissions.Action... actions) {
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
            SecurityUtils.getSubject().checkPermission(
                    object.getPermissionType().getStringPermissionForObject(DefaultActions.READ, object));
        }
    }

    @Override
    public void checkCurrentUserUpdatePermission(WithQualifiedObjectIdentifier object) {
        if (object != null) {
            SecurityUtils.getSubject().checkPermission(
                    object.getPermissionType().getStringPermissionForObject(DefaultActions.UPDATE, object));
        }
    }

    @Override
    public void checkCurrentUserDeletePermission(WithQualifiedObjectIdentifier object) {
        if (object != null) {
            SecurityUtils.getSubject().checkPermission(
                    object.getPermissionType().getStringPermissionForObject(DefaultActions.DELETE, object));
        }
    }

    @Override
    public void checkCurrentUserDeletePermission(QualifiedObjectIdentifier identifier) {
        if (identifier != null) {
            SecurityUtils.getSubject().checkPermission(identifier.getStringPermission(DefaultActions.DELETE));
        }
    }

    @Override
    public void checkCurrentUserExplicitPermissions(WithQualifiedObjectIdentifier object,
            HasPermissions.Action... actions) {
        if (object != null) {
            for (int i = 0; i < actions.length; i++) {
                SecurityUtils.getSubject()
                        .checkPermission(object.getPermissionType().getStringPermissionForObject(actions[i], object));
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
    public <T> Map<String, T> getPreferenceObjectsByKey(String key) {
        return store.getPreferenceObjectsByKey(key);
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
        store.setDefaultTennantForUserAndUpdate(user, newDefaultTenant, serverName);
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
                    existingRole.getQualifiedForUser(), existingRole.isTransitive());
            addRoleForUser(userAndRole.getA(), copyRole);
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
        clientIPBasedLockingAndBanningForBearerTokenAuthentication.clear();
        clientIPBasedLockingAndBanningForUserCreation.clear();
    }

    @Override
    public void storeSession(String cacheName, Session session) {
        PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().storeSession(cacheName, session);
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
    public void updateUserSubscription(String username, Subscription newSubscription) throws UserManagementException {
        final User user = getUserByName(username);
        if (user != null) {
            apply(new UpdateUserSubscriptionOperation(username, newSubscription));
        } else {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
    }

    @Override
    public Void internalUpdateSubscription(String username, Subscription newSubscription)
            throws UserManagementException {
        final User user = getUserByName(username);
        if (user != null) {
            lockSubscriptionsForUser(user);
            try {
                final String newSubscriptionPlanId = newSubscription.getPlanId();
                final Subscription currentSubscription = user.getSubscriptionByPlan(newSubscriptionPlanId);
                if (shouldProcessNewSubscription(currentSubscription, newSubscription)) {
                    logger.info(() -> "Update user subscription for plan " + newSubscriptionPlanId);
                    logger.info(() -> "Current user plan subscription: "
                            + (currentSubscription != null ? currentSubscription.toString() : "null"));
                    logger.info(() -> "New plan subscription: "
                            + (newSubscription != null ? newSubscription.toString() : "null"));
                    // In some cases there is no invoice or transaction information. E.g. if the subscription has
                    // been cancelled.
                    // To ensure information about previous payments is preserved, the subscription is patched.
                    if (currentSubscription != null && newSubscription != null
                            && newSubscription.getSubscriptionId() != null
                            && newSubscription.getSubscriptionId().equals(currentSubscription.getSubscriptionId())) {
                        if (currentSubscription.getTransactionStatus() != null
                                && newSubscription.getTransactionStatus() == null) {
                            newSubscription.patchTransactionData(currentSubscription);
                        }
                        if (currentSubscription.getInvoiceId() != null && newSubscription.getInvoiceId() == null) {
                            newSubscription.patchInvoiceData(currentSubscription);
                        }
                    }
                    if (shouldUpdateUserRolesForSubscription(user, currentSubscription, newSubscription)) {
                        updateUserRolesOnSubscriptionChange(user, currentSubscription, newSubscription);
                    }
                    final Subscription[] newSubscriptions = buildNewUserSubscriptions(user, newSubscription);
                    if (newSubscriptions != null) {
                        user.setSubscriptions(newSubscriptions);
                    }
                    store.updateUser(user);
                } else {
                    logger.info(() -> "New subscription has been ignored: " + newSubscription);
                }
                return null;
            } finally {
                unlockSubscriptionsForUser(user);
            }
        } else {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
    }
    
    /**
     * Check if new subscription should be processed, such as if it has a valid plan, or it's the most recent
     * subscription of a plan
     */
    private boolean shouldProcessNewSubscription(Subscription currentSubscription, Subscription newSubscription) {
        final boolean shouldProcess;
        if (!newSubscription.hasPlan()) {
            // New subscription doesn't have plan id, that means it's an empty subscription model which is used for
            // clearing all user subscriptions
            shouldProcess = true;
        } else if (subscriptionPlanProvider.getAllSubscriptionPlans().get(newSubscription.getPlanId()) != null) {
            if (currentSubscription == null) {
                // New subscription plan is valid, but current subscription of the plan is empty
                shouldProcess = true;
            } else if (!newSubscription.hasSubscriptionId()) {
                // New subscription has plan id but doesn't have subscription id, this is an empty subscription for the
                // plan that holds updated dates data for the plan's subscription
                shouldProcess = newSubscription.isUpdatedMoreRecently(currentSubscription);
            } else {
                // Only process new subscription if it is the most recently created subscription
                shouldProcess = newSubscription.getSubscriptionCreatedAt().asMillis() >= currentSubscription
                        .getSubscriptionCreatedAt().asMillis();
            }
        } else {
            // New subscription doesn't have a valid plan
            shouldProcess = false;
        }
        return shouldProcess;
    }

    /**
     * Build new subscription list for user from new subscription. This might update a subscription model, or add new
     * one to user's subscription list. In case no updates for current user subscriptions then null will be returned
     */
    private Subscription[] buildNewUserSubscriptions(User user, Subscription newSubscription) {
        Subscription[] newUserSubscriptions = null;
        Iterable<Subscription> subscriptions = user.getSubscriptions();
        if (newSubscription != null) {
            if (subscriptions == null || !subscriptions.iterator().hasNext()) {
                newUserSubscriptions = new Subscription[] { newSubscription };
            } else if (!newSubscription.hasPlan()) {
                // New subscription has no plan, that means new subscription is just an empty one with some meta data
                // for updated dates, and user has been deleted from provider. In this case we need to remove all
                // current subscriptions of the user for this provider
                List<Subscription> newSubscriptionList = new ArrayList<Subscription>();
                for (Subscription subscription : subscriptions) {
                    if (!subscription.getProviderName().equals(newSubscription.getProviderName())) {
                        newSubscriptionList.add(subscription);
                    }
                }
                newSubscriptionList.add(newSubscription);
                newUserSubscriptions = newSubscriptionList.toArray(new Subscription[] {});
            } else {
                List<Subscription> newSubscriptionList = new ArrayList<Subscription>();
                boolean foundCurrentSubscription = false;
                for (Subscription subscription : subscriptions) {
                    if (!foundCurrentSubscription && ((!subscription.hasPlan()
                            && subscription.getProviderName().equals(newSubscription.getProviderName()))
                            || subscription.getPlanId().equals(newSubscription.getPlanId()))) {
                        newSubscriptionList.add(newSubscription);
                        foundCurrentSubscription = true;
                    } else {
                        newSubscriptionList.add(subscription);
                    }
                }
                if (!foundCurrentSubscription) {
                    newSubscriptionList.add(newSubscription);
                }
                newUserSubscriptions = newSubscriptionList.toArray(new Subscription[] {});
            }
        }
        return newUserSubscriptions;
    }

    /**
     * Add or remove subscription plan's roles for user
     */
    private void updateUserRolesOnSubscriptionChange(User user, Subscription currentSubscription,
            Subscription newSubscription) throws UserManagementException {
        logger.info(() -> "Update user subscription roles for user " + user.getName());
        // in case new subscription has no planId, it means the subscription is an empty one with just meta data for
        // update times, and user doesn't subscribe to any plans, so all plan's roles assigned to the user must be
        // removed, but only if no other plan that the user still is subscribed to implies an equal role:
        Map<Serializable, SubscriptionPlan> allSubscriptionPlans = subscriptionPlanProvider.getAllSubscriptionPlans();
        if (newSubscription != null && !newSubscription.hasPlan()) {
            Iterable<SubscriptionPlan> plans = allSubscriptionPlans.values();
            for (SubscriptionPlan plan : plans) {
                removeUserPlanRoles(user, plan, /* checkOverlappingRoles */ true);
            }
        } else {
            assert currentSubscription == null || newSubscription == null
                    || currentSubscription.getPlanId().equals(newSubscription.getPlanId());
            if (currentSubscription != null && currentSubscription.hasPlan()
                    && currentSubscription.isActiveSubscription() && newSubscription != null
                    && !newSubscription.isActiveSubscription()) {
                SubscriptionPlan currentPlan = allSubscriptionPlans.get(currentSubscription.getPlanId());
                removeUserPlanRoles(user, currentPlan, /* checkOverlappingRoles */ true);
            }
            if (newSubscription != null && newSubscription.hasPlan() && newSubscription.isActiveSubscription()) {
                SubscriptionPlan newPlan = allSubscriptionPlans.get(newSubscription.getPlanId());
                addUserPlanRoles(user, newPlan);
            }
        }
    }

    /**
     * Remove user roles implied by a subscription plan
     * 
     * @param checkOverlappingRoles
     *            true if it needs to check overlapping roles with other active subscription plans of the same user,
     *            otherwise roles will be removed without any overlap check
     */
    private void removeUserPlanRoles(User user, SubscriptionPlan plan, boolean checkOverlappingRoles)
            throws UserManagementException {
        if (plan != null) {
            logger.info(() -> "Remove user roles of subscription plan " + plan.getId());
            final Role[] rolesToRemove;
            if (checkOverlappingRoles) {
                rolesToRemove = getSubscriptionPlanUserRolesWithoutOverlapping(user, plan);
            } else {
                rolesToRemove = getSubscriptionPlanUserRoles(user, plan);
            }
            logger.info(() -> "Removing the following roles of subscription plan " + plan.getId()+" from user "+user.getName()+": "+
                    Arrays.asList(rolesToRemove));
            for (Role role : rolesToRemove) {
                store.removeRoleFromUser(user.getName(), role);
            }
        }
    }

    private void addUserPlanRoles(User user, SubscriptionPlan plan) throws UserManagementException {
        if (plan != null) {
            logger.info(() -> "Add user roles for subscription plan " + plan.getId());
            Role[] roles = getSubscriptionPlanUserRoles(user, plan);
            logger.info(() -> "Adding the following roles of subscription plan " + plan.getId()+" to user "+user.getName()+": "+
                    Arrays.asList(roles));
            for (Role role : roles) {
                addRoleForUserAndSetUserAsOwner(user, role);
            }
        }
    }

    @Override
    public Role[] getSubscriptionPlanUserRoles(User user, SubscriptionPlan plan) {
        final List<Role> roles = new ArrayList<Role>();
        for (SubscriptionPlanRole planRole : plan.getRoles()) {
            roles.add(getSubscriptionPlanUserRole(user, planRole));
        }
        return roles.toArray(new Role[] {});
    }
    
    /**
     * Get user roles of a plan that are not overlapping with roles of other user active subscription plan
     */
    private Role[] getSubscriptionPlanUserRolesWithoutOverlapping(User user, SubscriptionPlan plan) {
        final Set<Role> otherPlanRoles = new HashSet<Role>();
        Iterable<Subscription> subscriptions = user.getSubscriptions();
        for (Subscription subscription : subscriptions) {
            if (subscription.isActiveSubscription() && !subscription.getPlanId().equals(plan.getId())) {
                SubscriptionPlan otherPlan = subscriptionPlanProvider.getAllSubscriptionPlans().get(subscription.getPlanId());
                for (SubscriptionPlanRole planRole : otherPlan.getRoles()) {
                    otherPlanRoles.add(getSubscriptionPlanUserRole(user, planRole));
                }
            }
        }
        final List<Role> roles = new ArrayList<Role>();
        for (SubscriptionPlanRole planRole : plan.getRoles()) {
            Role role = getSubscriptionPlanUserRole(user, planRole);
            if (!otherPlanRoles.contains(role)) {
                roles.add(role);
            }
        }
        return roles.toArray(new Role[] {});
    }

    /**
     * Get a role {@code Role} for a subscription plan role definition {@code SubscriptionPlanRole}.
     * These roles are non transitive, hence they can not be granted to other users.
     */
    private Role getSubscriptionPlanUserRole(User user, SubscriptionPlanRole planRole) {
        final Role result;
        final User qualifiedUser = getSubscriptionPlanRoleQualifiedUser(user, planRole);
        final UserGroup qualifiedTenant = getSubscriptionPlanRoleQualifiedTenant(user, qualifiedUser, planRole);
        final RoleDefinition roleDefinition = getRoleDefinition(planRole.getRoleId());
        if (roleDefinition == null) {
            logger.severe("Role with ID "+planRole.getRoleId()+" for user "+user.getName()+" not found.");
            result = null;
        } else {
            result = new Role(roleDefinition, qualifiedTenant, qualifiedUser,
                    /* roles acquired through subscription are non-transitive, meaning the user cannot pass them on */ false);
        }
        return result;
    }

    /**
     * Check and return role qualified user {@code User} for a subscription plan role {@code SubscriptionPlanRole}
     * definition. If the {@link SubscriptionPlanRole#getUserQualificationMode() user qualification mode} is set to
     * {@link SubscriptionPlanRole.UserQualificationMode#SUBSCRIBING_USER} then {@code user} is returned. Else, if the
     * subscription plan role specifies an {@link SubscriptionPlanRole#getExplicitUserQualification() explicit user
     * qualification}, its user is returned. Otherwise, this method returns {@code null}.
     */
    private User getSubscriptionPlanRoleQualifiedUser(User user, SubscriptionPlanRole planRole) {
        final User qualifiedUser;
        if (planRole.getUserQualificationMode() != null
                && planRole.getUserQualificationMode() == SubscriptionPlanRole.UserQualificationMode.SUBSCRIBING_USER) {
            qualifiedUser = user;
        } else if (planRole.getExplicitUserQualification() != null
                && !planRole.getExplicitUserQualification().isEmpty()) {
            qualifiedUser = getUserByName(planRole.getExplicitUserQualification());
        } else {
            qualifiedUser = null;
        }
        return qualifiedUser;
    }

    /**
     * Check and return role qualified tenant {@code UserGroup} for a subscription plan role
     * {@code SubscriptionPlanRole} definition.
     * 
     * @param qualifiedUser
     *            qualified user from
     *            {@code SecurityServiceImpl#getSubscriptionPlanRoleQualifiedUser(User, SubscriptionPlanRole)}
     */
    private UserGroup getSubscriptionPlanRoleQualifiedTenant(User subscriptionUser, User qualifiedUser,
            SubscriptionPlanRole planRole) {
        final UserGroup qualifiedForGroup;
        final SubscriptionPlanRole.GroupQualificationMode groupQualificationMode = planRole.getGroupQualificationMode();
        if (groupQualificationMode != null
                && groupQualificationMode != SubscriptionPlanRole.GroupQualificationMode.NONE) {
            final User u;
            switch (groupQualificationMode) {
            case DEFAULT_QUALIFIED_USER_TENANT:
                if (qualifiedUser != null) {
                    u = qualifiedUser;
                } else {
                    u = null;
                }
                break;
            case SUBSCRIBING_USER_DEFAULT_TENANT:
                if (subscriptionUser != null) {
                    u = subscriptionUser;
                } else {
                    u = null;
                }
                break;
            default:
                u = null;
                break;
            }
            if (u != null) {
                // don't use the default tenant but the default tenant name to resolve the user's own default group;
                // example: the user may have set a default object creation group (e.g., "kielerwoche2020-server") for
                // the current server that is different from the user's own default group ("{username}-tenant"). Yet,
                // when assigning a role based on a subscription, the "default tenant/group" has to be the user's own
                // group, not the current object creation group that the user has currently set. Otherwise, those role
                // assignments would be specific to a server which they shall not.
                qualifiedForGroup = getUserGroupByName(getDefaultTenantNameForUsername(u.getName()));
            } else {
                qualifiedForGroup = null;
            }
        } else if (planRole.getIdOfExplicitGroupQualification() != null) {
            qualifiedForGroup = getUserGroup(planRole.getIdOfExplicitGroupQualification());
        } else {
            qualifiedForGroup = null;
        }
        return qualifiedForGroup;
    }

    /**
     * Role assignments that are based on plans subscribed need to be adjusted if the
     * {@link Subscription#isActiveSubscription() is-active} status of the user's subscription has changed.
     */
    private boolean shouldUpdateUserRolesForSubscription(User user, Subscription currentSubscription,
            Subscription newSubscription) {
        final boolean result;
        if (currentSubscription == null && newSubscription == null) {
            result = false;
        } else if (newSubscription == null) {
            // In case new subscription is null, user's subscriptions won't be changed
            result = false;
        } else if (!newSubscription.hasPlan()) {
            // New subscription doesn't have plan id, that means it's an empty subscription model which is used for
            // clearing all user subscriptions
            boolean isUserInPossessionOfRoles = false;
            for (SubscriptionPlan subscriptionPlan : getAllSubscriptionPlans().values()) {
                isUserInPossessionOfRoles = subscriptionPlan.isUserInPossessionOfRoles(user);
                if(isUserInPossessionOfRoles) {
                    break;
                }
            }
            result = isUserInPossessionOfRoles;
        } else if (currentSubscription == null || currentSubscription.getPlanId() == null) {
            // A case when there's no current subscription for a plan, if the plan's new subscription is active then
            // user roles need to be updated with granted new roles. Further, if the user is 
            // somehow in possession of roles he should not posess, the roles must be removed
            final SubscriptionPlan subscriptionPlanById = getSubscriptionPlanById(newSubscription.getPlanId());
            if (subscriptionPlanById == null) {
                result = false;
            } else {
                result = newSubscription.isActiveSubscription() || subscriptionPlanById.isUserInPossessionOfRoles(user);
            }
        } else {
            assert currentSubscription.getPlanId().equals(newSubscription.getPlanId());
            // in this case user roles will be needed to update only when subscription active status is changed
            if (newSubscription.isActiveSubscription() != currentSubscription.isActiveSubscription()) {
                result = true;
            } else {
                final SubscriptionPlan subscriptionPlanById = getSubscriptionPlanById(newSubscription.getPlanId());
                result = !subscriptionPlanById.isUserInPossessionOfRoles(user);
            }
        }
        return result;
    }

    @Override
    public void addPermissionChangeListener(WildcardPermission permission, PermissionChangeListener listener) {
        permissionChangeListeners.addPermissionChangeListener(permission, listener);
    }

    @Override
    public void removePermissionChangeListener(WildcardPermission permission, PermissionChangeListener listener) {
        permissionChangeListeners.removePermissionChangeListener(permission, listener);
    }

    @Override
    public void updateSubscriptionPlanPrices(Map<String, BigDecimal> itemPrices) {
        apply(new UpdateItemPriceOperation(itemPrices));
    }
    
    @Override
    public Void internalUpdateSubscriptionPlanPrices(Map<String, BigDecimal> updatedItemPrices) {
        final Map<Serializable, SubscriptionPlan> allSubscriptionPlans = getAllSubscriptionPlans();
        for (SubscriptionPlan subscriptionPlan : allSubscriptionPlans.values()) {
            for (SubscriptionPrice subscriptionPrice : subscriptionPlan.getPrices()) {
                final BigDecimal updatedPrice = updatedItemPrices.get(subscriptionPrice.getPriceId());
                if (updatedPrice != null) {
                    logger.log(Level.INFO, "Setting ItemPrice for SubscriptionPrice " + subscriptionPrice.getPriceId());
                    subscriptionPrice.setPrice(updatedPrice);
                }
            }
        }
        return null;
    }

    @Override
    public Role createRoleFromIDs(UUID roleDefinitionId, UUID qualifyingTenantId, String qualifyingUsername, boolean transitive) throws UserManagementException {
        final User user;
        if (qualifyingUsername == null || qualifyingUsername.trim().isEmpty()) {
            user = null;
        } else {
            user = getUserByName(qualifyingUsername);
            if (user == null) {
                throw new UserManagementException("User "+qualifyingUsername+" not found for role qualification");
            }
        }
        final UserGroup group;
        if (qualifyingTenantId == null) {
            group = null;
        } else {
            group = getUserGroup(qualifyingTenantId);
            if (group == null) {
                throw new UserManagementException("Group with ID "+qualifyingTenantId+" not found for role qualification");
            }
        }
        final RoleDefinition roleDefinition = getRoleDefinition(roleDefinitionId);
        if (roleDefinition == null) {
            throw new UserManagementException("Role definition with ID "+roleDefinitionId+" not found");
        }
        return new Role(roleDefinition, group, user, transitive);
    }

    /**
     * @return the role associated with the given IDs and qualifiers
     * @throws UserManagementException
     *             if the current user does not have the meta permission to give this specific, qualified role in this
     *             context.
     */
    @Override
    public Role getOrThrowRoleFromIDsAndCheckMetaPermissions(UUID roleDefinitionId, UUID tenantId, String userQualifierName, boolean transitive) throws UserManagementException {
        final Role role = createRoleFromIDs(roleDefinitionId, tenantId, userQualifierName, transitive);
        if (!hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(
                role.getRoleDefinition(), role.getQualificationAsOwnership())) {
            throw new UserManagementException("You are not allowed to take this role to the user.");
        }
        return role;
    }

    @Override
    public void lockSubscriptionsForUser(final User user) {
        LockUtil.lockForWrite(subscriptionLocksForUsers.computeIfAbsent(user, u->new NamedReentrantReadWriteLock("Subscriptions lock for user "+user.getName(), /* fair */ false)));
    }
    
    @Override
    public void unlockSubscriptionsForUser(final User user) {
        LockUtil.unlockAfterWrite(subscriptionLocksForUsers.computeIfAbsent(user, u->new NamedReentrantReadWriteLock("Subscriptions lock for user "+user.getName(), /* fair */ false)));
    }

    @Override
    public void fileTakedownNotice(TakedownNoticeRequestContext takedownNoticeRequestContext) throws MailException {
        final String SUPPORT_MAIL_ADDRESS = "support@sapsailing.com";
        final User user = getUserByName(takedownNoticeRequestContext.getUsername());
        final String email = user.getEmail();
        final StringBuilder sb = new StringBuilder()
                .append("User ")
                .append(takedownNoticeRequestContext.getUsername())
                .append(" with e-mail ")
                .append(email)
                .append(" requests that the media with URL ")
                .append(takedownNoticeRequestContext.getContentUrl())
                .append(" used in context ")
                .append(messages.get(Locale.ENGLISH, takedownNoticeRequestContext.getContextDescriptionMessageKey(), takedownNoticeRequestContext.getContextDescriptionMessageParameter()))
                .append(" on page ")
                .append(takedownNoticeRequestContext.getPageUrl())
                .append(" be removed from the site. The user provides the following comment:\n\n")
                .append("   \"")
                .append(takedownNoticeRequestContext.getReportingUserComment())
                .append("\"\n\n")
                .append("The claim is of nature ")
                .append(takedownNoticeRequestContext.getNatureOfClaim())
                .append(".");
        if (!Util.isEmpty(takedownNoticeRequestContext.getSupportingURLs())) {
            sb.append("\n\nThe user provided the following additional URLs to substantiate or prove the claim:\n");
            for (final String url : takedownNoticeRequestContext.getSupportingURLs()) {
                sb.append(" - ");
                sb.append(url);
                sb.append("\n");
            }
        }
        final String message = sb.toString();
        getMailService().sendMail(SUPPORT_MAIL_ADDRESS, "Media Take-Down Request", message);
        getMailService().sendMail(email, "Media Take-Down Request Confirmation", messages.get(user.getLocaleOrDefault(), "takedownRequestConfirmation",
                Util.hasLength(user.getFullName()) ? user.getFullName() : user.getName(), SUPPORT_MAIL_ADDRESS, message));
    }
    
    /**
     * For a {@link SecuredSecurityTypes#SERVER SERVER} object identified by {@code serverName}, determines the user set
     * as the server's owner, plus additional users that have the permission to execute
     * {@code alsoSendToAllUsersWithThisPermissionOnReplicaSet} on that server.
     * 
     * @param serverName
     *            identifies the server object; for the local server that would, e.g., be {@link ServerInfo#getName()}.
     *            For replica sets, this is the name of the replica set.
     * @param alsoSendToAllUsersWithThisPermissionOnReplicaSet
     *            when not empty, all users that have permission to this {@link SecuredSecurityTypes#SERVER SERVER}
     *            action on the {@code replicaSet} will receive the e-mail in addition to the server owner. No user will
     *            receive the e-mail twice.
     */
    @Override
    public Iterable<User> getUsersToInformAboutReplicaSet(String serverName, Optional<HasPermissions.Action> alsoSendToAllUsersWithThisPermissionOnReplicaSet) {
        final QualifiedObjectIdentifier serverIdentifier = SecuredSecurityTypes.SERVER.getQualifiedObjectIdentifier(new TypeRelativeObjectIdentifier(serverName));
        final OwnershipAnnotation serverOwnership = getOwnership(serverIdentifier);
        final User serverOwner;
        final Set<User> usersToSendMailTo = new HashSet<>();
        if (serverOwnership != null && serverOwnership.getAnnotation() != null && (serverOwner = serverOwnership.getAnnotation().getUserOwner()) != null) {
            usersToSendMailTo.add(serverOwner);
        }
        alsoSendToAllUsersWithThisPermissionOnReplicaSet.ifPresent(
                serverAction -> getUsersWithPermissions(serverIdentifier.getPermission(serverAction))
                .forEach(usersToSendMailTo::add));
        return usersToSendMailTo;
    }
}
