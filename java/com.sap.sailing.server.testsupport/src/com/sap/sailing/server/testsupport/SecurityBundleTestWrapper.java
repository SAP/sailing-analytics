package com.sap.sailing.server.testsupport;

import java.util.logging.Logger;

import org.apache.shiro.SecurityUtils;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sse.security.SecurityService;
import com.sap.sse.security.impl.Activator;
import com.sap.sse.security.impl.SecurityServiceImpl;
import com.sap.sse.security.shared.subscription.SSESubscriptionPlan;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;
import com.sap.sse.util.ServiceTrackerFactory;

public class SecurityBundleTestWrapper {
    private static final Logger logger = Logger.getLogger(SecurityBundleTestWrapper.class.getName());

    public SecurityService initializeSecurityServiceForTesting() throws Exception {
        final SecurityService securityService;
        if (Activator.getContext() == null) {
            logger.info("Setup for TaggingServiceTest in a non-OSGi environment");
            final UserStoreImpl store = new UserStoreImpl("defaultTenant");
            store.ensureDefaultRolesExist();
            store.ensureServerGroupExists();
            final AccessControlStoreImpl accessControlStoreImpl = new AccessControlStoreImpl(store);
            Activator.setTestStores(store, accessControlStoreImpl);
            securityService = new SecurityServiceImpl(/* mailServiceTracker */ null, /* corsFilterConfigurationTracker */ null, store,
                    accessControlStoreImpl, new MockedHasPermissionProvider(), SSESubscriptionPlan::getAllInstances);
            ((SecurityServiceImpl) securityService).clearState();
            securityService.initialize();
            SecurityUtils.setSecurityManager(securityService.getSecurityManager());
            Activator.setSecurityService(securityService);
        } else {
            logger.info("Creating dummy UserStoreImpl to trigger loading of userstore mongodb bundle");
            new UserStoreImpl("defaultTenant"); // only to trigger bundle loading and activation so that security
                                                // service can find the bundle and its original user store
            logger.info("Setup for TaggingServiceTest in an OSGi environment");
            // Note: This timeout of 3 minutes is just for debugging purposes and should not be used in production!
            final ServiceTracker<SecurityService, SecurityService> serviceTracker = ServiceTrackerFactory
                    .createAndOpen(Activator.getContext(), SecurityService.class);
            if (serviceTracker == null) {
                logger.severe("Couldn't obtain service tracker for SecurityService");
                securityService = null;
            } else {
                securityService = serviceTracker.waitForService(180 * 1000);
                if (securityService == null) {
                    logger.severe("Waiting for the SecurityService timed out");
                } else {
                    // the security manager may have been set to other mock objects by other tests while the
                    // SecurityService survived
                    SecurityUtils.setSecurityManager(securityService.getSecurityManager());
                }
            }
        }
        return securityService;
    }
}
