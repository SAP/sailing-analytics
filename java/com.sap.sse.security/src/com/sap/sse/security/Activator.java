package com.sap.sse.security;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sse.security.userstore.shared.UserStore;

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
    
    static BundleContext getContext() {
        return context;
    }
    
    static SecurityService getSecurityService(){
        return securityService;
    }

    /**
     * If no {@link #testUserStore} is available, start looking for a {@link UserStore} service in a background thread.
     * As soon as a service reference for a {@link UserStore} implementation becomes available in the service registry.
     * If a {@link UserStore} has been found, one way or another, the {@link SecurityService} is created and
     * registered as an OSGi service.
     */
    public void start(BundleContext bundleContext) throws Exception {
        if (testUserStore != null) {
            createAndRegisterSecurityService(testUserStore);
        } else {
            waitForUserStoreService(bundleContext);
        }
    }

    private void createAndRegisterSecurityService(UserStore store) {
        securityService = new SecurityServiceImpl(store);
        registration = context.registerService(SecurityService.class.getName(),
                securityService, null);
        Logger.getLogger(Activator.class.getName()).info("Security Service registered.");
    }

    private void waitForUserStoreService(BundleContext bundleContext) {
        context = bundleContext;
        final ServiceTracker<UserStore, UserStore> tracker = new ServiceTracker<>(bundleContext, UserStore.class, /* customizer */ null);
        tracker.open();
        new Thread("ServiceTracker waiting for UserStore service") {
            @Override
            public void run() {
                try {
                    logger.info("Waiting for UserStore service...");
                    UserStore userStore = tracker.waitForService(0);
                    logger.info("Obtained UserStore service "+userStore);
                    createAndRegisterSecurityService(userStore);
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
        registration.unregister();
        Activator.context = null;
    }

}
