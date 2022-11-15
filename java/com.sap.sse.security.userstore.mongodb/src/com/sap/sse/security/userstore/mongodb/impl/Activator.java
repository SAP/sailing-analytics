package com.sap.sse.security.userstore.mongodb.impl;

import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sse.ServerInfo;
import com.sap.sse.common.Util;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.PreferenceConverterRegistrationManager;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.subscription.SubscriptionApiService;
import com.sap.sse.security.subscription.SubscriptionDataHandler;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;
import com.sap.sse.util.ServiceTrackerFactory;

public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());
    private static BundleContext context;
    private static ServiceTracker<SubscriptionApiService, SubscriptionApiService> subscriptionApiServiceTracker;
    private ServiceRegistration<?> accessControlStoreRegistration;
    private ServiceRegistration<?> userStoreRegistration;
    private PreferenceConverterRegistrationManager preferenceConverterRegistrationManager;

    static BundleContext getContext() {
        return context;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext bundleContext) throws Exception {
        Activator.context = bundleContext;
        final String defaultServerGroupName = System.getProperty(UserStore.DEFAULT_SERVER_GROUP_NAME_PROPERTY_NAME, ServerInfo.getServerGroupName());
        logger.info("Creating user store");
        subscriptionApiServiceTracker = ServiceTrackerFactory.createAndOpen(context, SubscriptionApiService.class);
        final UserStoreImpl userStore = new UserStoreImpl(defaultServerGroupName);
        AccessControlStoreImpl accessControlStore = new AccessControlStoreImpl(userStore);
        accessControlStoreRegistration = context.registerService(AccessControlStore.class.getName(), accessControlStore, null);
        userStoreRegistration = context.registerService(UserStore.class.getName(), userStore, null);
        preferenceConverterRegistrationManager = new PreferenceConverterRegistrationManager(bundleContext, userStore);
        logger.info("User store registered.");
        for (CollectionNames name : CollectionNames.values()) {
            MongoDBService.INSTANCE.registerExclusively(CollectionNames.class, name.name());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception {
        preferenceConverterRegistrationManager.stop();
        accessControlStoreRegistration.unregister();
        userStoreRegistration.unregister();
        Activator.context = null;
    }

    public static SubscriptionDataHandler getSubscriptionDataHandler(String providerName) {
        if (subscriptionApiServiceTracker != null) {
            ServiceReference<SubscriptionApiService>[] serviceReferences = subscriptionApiServiceTracker.getServiceReferences();
            for (final ServiceReference<SubscriptionApiService> serviceReference : serviceReferences) {
                if (Util.equalsWithNull(serviceReference.getProperty(SubscriptionApiService.PROVIDER_NAME_OSGI_REGISTRY_KEY), providerName)) {
                    return context.getService(serviceReference).getDataHandler();
                }
            }
        }
        return null;
    }
}
