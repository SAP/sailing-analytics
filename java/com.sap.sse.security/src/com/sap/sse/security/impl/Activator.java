package com.sap.sse.security.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sse.mail.MailService;
import com.sap.sse.replication.Replicable;
import com.sap.sse.security.AccessControlStore;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.UserStore;
import com.sap.sse.security.UsernamePasswordRealm;
import com.sap.sse.util.ClearStateTestSupport;
import com.sap.sse.util.ServiceTrackerFactory;

public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());
    
    private static BundleContext context;
    private static SecurityService securityService;
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
    
    public static void setTestUserStore(UserStore theTestUserStore) {
        testUserStore = theTestUserStore;
        UsernamePasswordRealm.setTestUserStore(theTestUserStore);
    }
    
    public static void setSecurityService(SecurityService securityService) {
        Activator.securityService = securityService;
    }
    
    public static BundleContext getContext() {
        return context;
    }
    
    public static SecurityService getSecurityService() {
        return securityService;
    }

    /**
     * If no {@link #testUserStore} is available, start looking for a {@link UserStore} service in a background thread.
     * As soon as a service reference for a {@link UserStore} implementation becomes available in the service registry.
     * If a {@link UserStore} has been found, one way or another, the {@link SecurityService} is created and
     * registered as an OSGi service.
     */
    public void start(BundleContext bundleContext) throws Exception {
        context = bundleContext;
        if (testUserStore != null) {
            createAndRegisterSecurityService(bundleContext, testUserStore, null /* TODO what do I do here with the ACLStore?*/);
        } else {
            waitForUserStoreService(bundleContext);
        }
    }

    private void createAndRegisterSecurityService(BundleContext bundleContext, UserStore store, AccessControlStore aclStore) {
        securityService = new SecurityServiceImpl(ServiceTrackerFactory.createAndOpen(context, MailService.class), store, aclStore, /* setAsActivatorSecurityService */ true);
        registration = context.registerService(SecurityService.class.getName(),
                securityService, null);
        final Dictionary<String, String> replicableServiceProperties = new Hashtable<>();
        replicableServiceProperties.put(Replicable.OSGi_Service_Registry_ID_Property_Name, securityService.getId().toString());
        context.registerService(Replicable.class.getName(), securityService, replicableServiceProperties);
        context.registerService(ClearStateTestSupport.class.getName(), securityService, null);
        Logger.getLogger(Activator.class.getName()).info("Security Service registered.");
    }

    private void waitForUserStoreService(BundleContext bundleContext) {
        context = bundleContext;
        final ServiceTracker<UserStore, UserStore> tracker = new ServiceTracker<>(bundleContext, UserStore.class, /* customizer */ null);
        final ServiceTracker<AccessControlStore, AccessControlStore> aclStoreTracker = new ServiceTracker<>(bundleContext, AccessControlStore.class, /* customizer */ null);
        tracker.open();
        aclStoreTracker.open();
        new Thread("ServiceTracker waiting for UserStore service") {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setContextClassLoader(getClass().getClassLoader()); // ensure that classpath:... Shiro ini files are resolved properly
                    logger.info("Waiting for UserStore service...");
                    final UserStore userStore = tracker.waitForService(0);
                    final AccessControlStore aclStore = aclStoreTracker.waitForService(0);
                    logger.info("Obtained UserStore service "+userStore);
                    createAndRegisterSecurityService(bundleContext, userStore, aclStore);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Interrupted while waiting for UserStore service", e);
                }
            }
        }.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception {
        if (registration != null) {
            registration.unregister();
        }
        Activator.context = null;
    }

}
