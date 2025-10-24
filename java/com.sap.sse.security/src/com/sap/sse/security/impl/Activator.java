package com.sap.sse.security.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.cache.CacheManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.sap.sse.ServerInfo;
import com.sap.sse.classloading.ServiceTrackerCustomizerForClassLoaderSupplierRegistrations;
import com.sap.sse.mail.MailService;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.rest.CORSFilterConfiguration;
import com.sap.sse.security.SecurityInitializationCustomizer;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SecurityServiceInitialLoadClassLoaderSupplier;
import com.sap.sse.security.UsernamePasswordRealm;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.HasPermissionsProvider;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.SubscriptionPlanProvider;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.PermissionAndRoleAssociation;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.subscription.SSESubscriptionPlan;
import com.sap.sse.security.subscription.SubscriptionApiRequestProcessor;
import com.sap.sse.security.subscription.SubscriptionApiRequestProcessorImpl;
import com.sap.sse.security.subscription.SubscriptionApiService;
import com.sap.sse.security.subscription.SubscriptionBackgroundUpdater;
import com.sap.sse.security.subscription.SubscriptionPlanBackgroundUpdater;
import com.sap.sse.security.subscription.chargebee.ChargebeeApiService;
import com.sap.sse.security.subscription.chargebee.ChargebeeConfiguration;
import com.sap.sse.util.ClearStateTestSupport;
import com.sap.sse.util.ServiceTrackerFactory;
import com.sap.sse.util.ThreadPoolUtil;

public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());
    
    private static BundleContext context;
    private static CompletableFuture<SecurityService> securityService = new CompletableFuture<>();
    private ServiceRegistration<?> registration;
    
    /**
     * In a non-OSGi test environment, having Shiro instantiate this class with a
     * default constructor makes it difficult to get access to the user store
     * implementation which may live in a bundle that this bundle has no direct
     * access to. Therefore, test cases must set the UserStore implementation
     * by invoking {@link #setTestUserStore} before the default constructor is
     * invoked.
     */
    private static UserStore testUserStore;
    private static AccessControlStore testAccessControlStore;

    private ServiceTracker<SecurityInitializationCustomizer, SecurityInitializationCustomizer> securityInitializationCustomizerTracker;

    private ServiceTracker<UserStore, UserStore> userStoreTracker;

    private ServiceTracker<AccessControlStore, AccessControlStore> accessControlStoreTracker;
    
    public static final String SHARED_ACROSS_SUBDOMAINS_OF_PROPERTY_NAME = "security.sharedAcrossSubdomainsOf";
    /**
     * Should be {@code null} if the security service instantiated by the bundle activated by this activator is not
     * shared across different sub-domains; otherwise, the parent domain across which the security service and its
     * replicas are shared shall be provided here, e.g., {@code "sapsailing.com"}.
     * <p>
     * 
     * The value can be specified by a system property whose name is provided by
     * {@link #SHARED_ACROSS_SUBDOMAINS_OF_PROPERTY_NAME}. If a non-{@code null} value is provided for
     * {@link #baseUrlForCrossDomainStorage} then a non-{@code null} value should be provided here, too.
     */
    private final String sharedAcrossSubdomainsOf;
    
    public static final String BASE_URL_FOR_CROSS_DOMAIN_STORAGE_PROPERTY_NAME = "security.baseUrlForCrossDomainStorage";
    /**
     * Should be {@code null} if the security service instantiated by the bundle activated by this activator is not
     * shared across different sub-domains; otherwise, the base URL where {@code /gwt-base/StorageMessaging.html} can be
     * loaded from shall be provided, and this base URL will then form the "origin" for the local browser storage used
     * for session storage and other local data stored in the browser which will then be available to all servers using
     * the same setting for this field. Example value: {@code "https://security-service.sapsailing.com"}.
     * <p>
     * 
     * The value can be specified by a system property whose name is provided by
     * {@link #BASE_URL_FOR_CROSS_DOMAIN_STORAGE_PROPERTY_NAME}. If a non-{@code null} value is provided for
     * {@link #sharedAcrossSubdomainsOf} then a non-{@code null} value should be provided here, too.
     */
    private final String baseUrlForCrossDomainStorage;

    private ServiceTracker<SecurityServiceInitialLoadClassLoaderSupplier, SecurityServiceInitialLoadClassLoaderSupplier> classLoaderSupplierServiceTracker;
    
    public static void setTestStores(UserStore theTestUserStore, AccessControlStore theTestAccessControlStore) {
        testUserStore = theTestUserStore;
        testAccessControlStore = theTestAccessControlStore;
        UsernamePasswordRealm.setTestStores(theTestUserStore, theTestAccessControlStore);
    }
    
    public static void setSecurityService(SecurityService securityService) {
        Activator.securityService.complete(securityService);
    }
    
    public static BundleContext getContext() {
        return context;
    }
    
    public static SecurityService getSecurityService() {
        try {
            return securityService.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, "Failure to get SecurityService", e);
            throw new RuntimeException(e);
        }
    }

    public Activator() {
        sharedAcrossSubdomainsOf = System.getProperty(SHARED_ACROSS_SUBDOMAINS_OF_PROPERTY_NAME);
        baseUrlForCrossDomainStorage = System.getProperty(BASE_URL_FOR_CROSS_DOMAIN_STORAGE_PROPERTY_NAME);
        if ((sharedAcrossSubdomainsOf == null) != (baseUrlForCrossDomainStorage == null)) {
            logger.warning("This looks like an inconsistent configuration. The security service uses "
                    + sharedAcrossSubdomainsOf + " as shared cookie domain but " + baseUrlForCrossDomainStorage
                    + " as cross-domain storage origin. " + " Either leave both null or set both consistently, e.g., "
                    + "to \"sapsailing.com\" and \"https://www.sapsailing.com\", respectively");
        }
    }
    
    /**
     * If no {@link #testUserStore} is available, start looking for a {@link UserStore} service in a background thread.
     * As soon as a service reference for a {@link UserStore} implementation becomes available in the service registry.
     * If a {@link UserStore} has been found, one way or another, the {@link SecurityService} is created and
     * registered as an OSGi service.
     */
    public void start(BundleContext bundleContext) throws Exception {
        context = bundleContext;
        final SubscriptionPlanProvider subscriptionPlanProvider = createAndOpenSubscriptionPlanProvider(bundleContext);
        createAndRegisterSubscriptionServices(subscriptionPlanProvider);
        if (testUserStore != null && testAccessControlStore != null) {
            createAndRegisterSecurityService(bundleContext, testUserStore, testAccessControlStore, subscriptionPlanProvider);
        } else {
            waitForUserStoreService(bundleContext, subscriptionPlanProvider);
        }
        context.registerService(ClearStateTestSupport.class, new ClearStateTestSupport() {
            @Override
            public void clearState() throws Exception {
                Activator.this.clearState();
            }
        }, null);
    }
    
    private SubscriptionPlanProvider createAndOpenSubscriptionPlanProvider(BundleContext bundleContext) {
        final ServiceTracker<SubscriptionPlanProvider, SubscriptionPlanProvider> subscriptionPlanProviderTracker = new ServiceTracker<>(
                bundleContext, SubscriptionPlanProvider.class, /* customizer */ null);
        subscriptionPlanProviderTracker.open();
        return new OSGISubscriptionPlanProvider(subscriptionPlanProviderTracker);
    }

    private void createAndRegisterSubscriptionServices(SubscriptionPlanProvider subscriptionPlanProvider) {
        final SubscriptionApiRequestProcessor subscriptionApiRequestProcessor = new SubscriptionApiRequestProcessorImpl(
                ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor());
        final ChargebeeApiService chargebeeApiService = new ChargebeeApiService(ChargebeeConfiguration.getInstance(),
                subscriptionApiRequestProcessor, subscriptionPlanProvider);
        final Dictionary<String, String> chargebeeProviderProperties = new Hashtable<>();
        chargebeeProviderProperties.put(SubscriptionApiService.PROVIDER_NAME_OSGI_REGISTRY_KEY,
                chargebeeApiService.getProviderName());
        context.registerService(SubscriptionApiService.class.getName(), chargebeeApiService,
                chargebeeProviderProperties);
    }

    /**
     * This method will clean the userStore and AccessControlStore, and reset the securityService. It will then
     * reinitialize them in the same fashion an empty server will do.
     */
    protected void clearState() throws InterruptedException, UserGroupManagementException, UserManagementException {
        CacheManager cm = getSecurityService().getCacheManager();
        if (cm instanceof ReplicatingCacheManager) {
            ((ReplicatingCacheManager) cm).clear();
        }
        UserStore userStore = userStoreTracker.waitForService(0);
        AccessControlStore accessControlStore = accessControlStoreTracker.waitForService(0);
        userStore.clear();
        accessControlStore.clear();
        userStore.ensureDefaultRolesExist();
        userStore.ensureServerGroupExists();
        getSecurityService().initialize();
        applyCustomizations();
    }

    private void createAndRegisterSecurityService(BundleContext bundleContext, UserStore userStore,
            AccessControlStore accessControlStore, SubscriptionPlanProvider subscriptionPlanProvider) {
        final ServiceTracker<HasPermissionsProvider, HasPermissionsProvider> hasPermissionsProviderTracker = new ServiceTracker<>(
                bundleContext, HasPermissionsProvider.class, /* customizer */ null);
        hasPermissionsProviderTracker.open();
        SecurityService initialSecurityService = new SecurityServiceImpl(
                ServiceTrackerFactory.createAndOpen(context, MailService.class),
                ServiceTrackerFactory.createAndOpen(context, CORSFilterConfiguration.class),
                ServiceTrackerFactory.createAndOpen(context, ReplicationService.class),
                userStore, accessControlStore,
                new OSGIHasPermissionsProvider(hasPermissionsProviderTracker), subscriptionPlanProvider, sharedAcrossSubdomainsOf, baseUrlForCrossDomainStorage);
        initialSecurityService.initialize();
        securityService.complete(initialSecurityService);
        registration = context.registerService(SecurityService.class, initialSecurityService, null);
        final Dictionary<String, String> replicableServiceProperties = new Hashtable<>();
        replicableServiceProperties.put(Replicable.OSGi_Service_Registry_ID_Property_Name,
                initialSecurityService.getId().toString());
        context.registerService(Replicable.class.getName(), initialSecurityService, replicableServiceProperties);
        context.registerService(ClearStateTestSupport.class.getName(), initialSecurityService, null);
        context.registerService(HasPermissionsProvider.class, SecuredSecurityTypes::getAllInstances, null);
        context.registerService(SubscriptionPlanProvider.class, SSESubscriptionPlan::getAllInstances, null);
        classLoaderSupplierServiceTracker =
                ServiceTrackerCustomizerForClassLoaderSupplierRegistrations.createClassLoaderSupplierServiceTracker(bundleContext,
                        SecurityServiceInitialLoadClassLoaderSupplier.class,
                        initialSecurityService.getInitialLoadClassLoaderRegistry());
        logger.info("Successfully created service tracker for class loader suppliers: "+classLoaderSupplierServiceTracker);
        context.registerService(SubscriptionPlanProvider.class, SSESubscriptionPlan::getAllInstances, null);
        Logger.getLogger(Activator.class.getName()).info("Security Service registered.");
    }
    
    private void applyCustomizations() {
        if (securityInitializationCustomizerTracker != null) {
            // In test cases, this method will be called multiple times. To not leak ServiceTracker instances, we need
            // to correctly stop he previously used instance.
            securityInitializationCustomizerTracker.close();
        }
        securityInitializationCustomizerTracker = new ServiceTracker<>(
                context, SecurityInitializationCustomizer.class, /* customizer */ new ServiceTrackerCustomizer<SecurityInitializationCustomizer, SecurityInitializationCustomizer>() {
                    @Override
                    public SecurityInitializationCustomizer addingService(ServiceReference<SecurityInitializationCustomizer> reference) {
                        final SecurityInitializationCustomizer service = context.getService(reference);
                        getSecurityService().registerCustomizer(service);
                        return service;
                    }
                    
                    @Override
                    public void modifiedService(ServiceReference<SecurityInitializationCustomizer> reference,
                            SecurityInitializationCustomizer service) {
                    }
                    
                    @Override
                    public void removedService(ServiceReference<SecurityInitializationCustomizer> reference,
                            SecurityInitializationCustomizer service) {
                    }
                });
        securityInitializationCustomizerTracker.open();
    }

    private void waitForUserStoreService(BundleContext bundleContext, SubscriptionPlanProvider subscriptionPlanProvider) {
        context = bundleContext;
        userStoreTracker = new ServiceTracker<>(bundleContext, UserStore.class, /* customizer */ null);
        accessControlStoreTracker = new ServiceTracker<>(bundleContext, AccessControlStore.class,
                /* customizer */ null);
        userStoreTracker.open();
        accessControlStoreTracker.open();
        new Thread("ServiceTracker waiting for UserStore service") {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setContextClassLoader(getClass().getClassLoader()); // ensure that classpath:... Shiro ini files are resolved properly
                    logger.info("Waiting for UserStore service...");
                    final UserStore userStore = userStoreTracker.waitForService(0);
                    final AccessControlStore accessControlStore = accessControlStoreTracker.waitForService(0);
                    logger.info("Obtained UserStore service "+userStore);
                    // must be called after the definition of the prototypes and after loading actual roles, but before
                    // loading users
                    userStore.ensureDefaultRolesExist();
                    // actually load the users and migrate them if required
                    userStore.loadAndMigrateUsers();
                    // loading ACLs and Ownerships requires users and UserGroups to be correctly loaded
                    accessControlStore.loadACLsAndOwnerships();
                    // create security service, it will also create a default admin user if no users exist
                    createAndRegisterSecurityService(bundleContext, userStore, accessControlStore, subscriptionPlanProvider);
                    applyCustomizations();
                    migrate(userStore, securityService.get());
                    final ReplicationMasterDescriptor masterDescriptor = securityService.get().getMasterDescriptor();
                    if (masterDescriptor == null) {
                        startSubscriptionDataUpdateTask(bundleContext);
                        startSubscriptionPlanUpdateTask(bundleContext);
                    }
                } catch (InterruptedException | UserStoreManagementException | ExecutionException e) {
                    logger.log(Level.SEVERE, "Interrupted while waiting for UserStore service", e);
                }
            }
        }.start();
    }
    
    private void migrate(UserStore userStore, SecurityService securityService) {
        for (User user : userStore.getUsers()) {
            securityService.migrateUser(user);
        }
        for (UserGroup group : userStore.getUserGroups()) {
            securityService.migrateOwnership(group);
        }
        for (RoleDefinition role : userStore.getRoleDefinitions()) {
            securityService.migrateOwnership(role);
        }
        for (User user : securityService.getUserList()) {
            for (Role role : user.getRoles()) {
                TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(role, user);
                QualifiedObjectIdentifier associationQualifiedIdentifier = SecuredSecurityTypes.ROLE_ASSOCIATION
                        .getQualifiedObjectIdentifier(associationTypeIdentifier);
                securityService.migrateOwnership(associationQualifiedIdentifier, associationTypeIdentifier.toString());
                if (securityService.isInitialOrMigration()) {
                    securityService.addToAccessControlList(associationQualifiedIdentifier, null,
                            DefaultActions.READ.name());
                }
            }
            for (WildcardPermission permission : user.getPermissions()) {
                securityService.migratePermission(user, permission, this::getPermissionReplacement);
                TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(permission,
                        user);
                QualifiedObjectIdentifier associationQualifiedIdentifier = SecuredSecurityTypes.PERMISSION_ASSOCIATION
                        .getQualifiedObjectIdentifier(associationTypeIdentifier);
                securityService.migrateOwnership(associationQualifiedIdentifier, associationTypeIdentifier.toString());
                if (securityService.isInitialOrMigration()) {
                    securityService.addToAccessControlList(associationQualifiedIdentifier, null,
                            DefaultActions.READ.name());
                }
            }
        }
        securityService.migrateServerObject();
        securityService.checkMigration(SecuredSecurityTypes.getAllInstances());
    }
    
    public WildcardPermission getPermissionReplacement(WildcardPermission permission) {
        final WildcardPermission replacement;
        if ("data_mining".equalsIgnoreCase(permission.toString())) {
            replacement = SecuredSecurityTypes.SERVER
                    .getQualifiedObjectIdentifier(new TypeRelativeObjectIdentifier(ServerInfo.getName()))
                    .getPermission(ServerActions.DATA_MINING);
        } else {
            replacement = null;
        }
        return replacement;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception {
        if (securityInitializationCustomizerTracker != null) {
            securityInitializationCustomizerTracker.close();
            securityInitializationCustomizerTracker = null;
        }
        if (registration != null) {
            registration.unregister();
        }
        Activator.context = null;
    }
    
    /**
     * Schedule background task to fetch and update user subscriptions from provider. Requires the {@link #securityService} to
     * be set.
     */
    private void startSubscriptionDataUpdateTask(BundleContext bundleContext) {
        new SubscriptionBackgroundUpdater(context).start(securityService);
    }
    
    private void startSubscriptionPlanUpdateTask(BundleContext bundleContext) {
        new SubscriptionPlanBackgroundUpdater(bundleContext).start(securityService);;
    }
}
