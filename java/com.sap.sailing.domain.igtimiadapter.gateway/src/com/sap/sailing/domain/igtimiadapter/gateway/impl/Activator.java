package com.sap.sailing.domain.igtimiadapter.gateway.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.security.SecurityService;
import com.sap.sse.util.ServiceTrackerFactory;

/**
 * Maintains a tracker for the {@link SecurityService} that REST resources in this bundle can access through
 * the {@link #getInstance()}.{@link #getSecurityService()} combination.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class Activator implements BundleActivator {
    private static Activator INSTANCE;
    private FullyInitializedReplicableTracker<SecurityService> securityServiceTracker;
    private ServiceTracker<RiotServer, RiotServer> riotServerTracker;
    
    public Activator() {
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        INSTANCE = this;
        securityServiceTracker = FullyInitializedReplicableTracker.createAndOpen(context, SecurityService.class);
        riotServerTracker = ServiceTrackerFactory.createAndOpen(context, RiotServer.class);
    }
    
    public static Activator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Activator(); // probably non-OSGi case, as in test execution
        }
        return INSTANCE;
    }
    
    public RiotServer getRiotServer() {
        return riotServerTracker.getService();
    }
    
    public SecurityService getSecurityService() {
        try {
            return securityServiceTracker.getInitializedService(0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        securityServiceTracker.close();
        securityServiceTracker = null;
    }
}
