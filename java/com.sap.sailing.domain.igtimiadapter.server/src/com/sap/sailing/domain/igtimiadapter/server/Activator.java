package com.sap.sailing.domain.igtimiadapter.server;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sailing.domain.igtimiadapter.persistence.DomainObjectFactory;
import com.sap.sailing.domain.igtimiadapter.persistence.MongoObjectFactory;
import com.sap.sailing.domain.igtimiadapter.persistence.PersistenceFactory;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sailing.domain.igtimiadapter.server.riot.impl.RiotServerImpl;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.Replicable;
import com.sap.sse.security.SecurityService;
import com.sap.sse.util.ClearStateTestSupport;

/**
 * When {@link #start(BundleContext) started}, creates a {@link RiotServer} instance listening on the
 * TCP port as specified by the property named {@link #RIOT_PORT_PROPERTY_NAME}. If this property is not
 * provided, the {@link RiotServer} will be started listening on an automatically assigned port so far unused.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());

    private static final String RIOT_PORT_PROPERTY_NAME = "igtimi.riot.port";

    private static BundleContext context;

    private static Activator instance;
    private RiotServer riotServer;
    private FullyInitializedReplicableTracker<SecurityService> securityServiceServiceTracker;

    private SecurityService securityServiceTest;
    
    public Activator() {
        instance = this;
    }

    static BundleContext getContext() {
        return context;
    }

    public void start(BundleContext bundleContext) throws Exception {
        Activator.context = bundleContext;
        final String riotPortAsString = System.getProperty(RIOT_PORT_PROPERTY_NAME, "0");
        final int bindPort = Integer.valueOf(riotPortAsString);
        final DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory();
        final MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory();
        riotServer = RiotServer.create(bindPort, domainObjectFactory, mongoObjectFactory, context);
        context.registerService(RiotServer.class, riotServer, null);
        final Dictionary<String, String> replicableServiceProperties = new Hashtable<>();
        replicableServiceProperties.put(Replicable.OSGi_Service_Registry_ID_Property_Name, riotServer.getId().toString());
        context.registerService(Replicable.class, riotServer, replicableServiceProperties);
        context.registerService(ClearStateTestSupport.class.getName(), new ClearStateTestSupport() {
            @Override
            public void clearState() throws Exception {
                ((RiotServerImpl) riotServer).clear();
            }
        }, null);
        new Thread(() -> {
            securityServiceServiceTracker = FullyInitializedReplicableTracker.createAndOpen(context, SecurityService.class);
            try {
                final SecurityService securityService = securityServiceServiceTracker.getInitializedService(0);
                for (DataAccessWindow daw : riotServer.getDataAccessWindows()) {
                    securityService.migrateOwnership(daw);
                }
                for (Device device : riotServer.getDevices()) {
                    securityService.migrateOwnership(device);
                }
                @SuppressWarnings("deprecation") // legacy secured type; SecurityService may still hold ownerships/ACLs somewhere...
                final String igtimiAccountTypeName = SecuredDomainType.IGTIMI_ACCOUNT.getName();
                securityService.assumeOwnershipMigrated(igtimiAccountTypeName);
                securityService.assumeOwnershipMigrated(SecuredDomainType.IGTIMI_DATA_ACCESS_WINDOW.getName());
                securityService.assumeOwnershipMigrated(SecuredDomainType.IGTIMI_DEVICE.getName());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error trying to create missing ownerships for Igtimi entities", e);
            }
        }).start();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        getRiotServer().stop();
        Activator.context = null;
    }
    
    public static Activator getInstance() {
        if (instance == null) {
            instance = new Activator();
        }
        return instance;
    }
    
    public RiotServer getRiotServer() {
        return riotServer;
    }
    
    /** Only used by tests. */
    public void setSecurityService(SecurityService securityService) {
        securityServiceTest = securityService;
    }

    public SecurityService getSecurityService() {
        try {
            return securityServiceTest == null ? securityServiceServiceTracker.getInitializedService(0) : securityServiceTest;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
