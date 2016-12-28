package com.sap.sailing.domain.tractracadapter.persistence.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.persistence.MongoRegattaLogStoreFactory;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParametersHandler;
import com.sap.sailing.domain.tractracadapter.TracTracAdapterFactory;
import com.sap.sailing.domain.tractracadapter.impl.ConnectivityParamsHandler;
import com.sap.sailing.domain.tractracadapter.impl.RaceTrackingConnectivityParametersImpl;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.util.ServiceTrackerFactory;

public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());
    
    /**
     * Requires a base {@link DomainFactory} as well as a {@link RaceLogStore} and a {@link RegattaLogStore}. In order
     * to instantiate those two stores, the method requires a {@link MongoObjectFactory} and a {@link DomainObjectFactory}
     * which are waited for as services from the OSGi registry. This happens in a separate thread that the {@link #start(BundleContext)}
     * method launches.
     */
    @Override
    public void start(BundleContext context) throws Exception {
        for (CollectionNames name : CollectionNames.values()) {
            MongoDBService.INSTANCE.registerExclusively(CollectionNames.class, name.name());
        }
        new Thread(() -> {
            final ServiceTracker<MongoObjectFactory, MongoObjectFactory> mongoObjectFactoryServiceTracker = ServiceTrackerFactory.createAndOpen(context, MongoObjectFactory.class);
            final ServiceTracker<DomainObjectFactory, DomainObjectFactory> domainObjectFactoryServiceTracker = ServiceTrackerFactory.createAndOpen(context, DomainObjectFactory.class);
            try {
                final MongoObjectFactory mongoObjectFactory = mongoObjectFactoryServiceTracker.waitForService(0);
                final DomainObjectFactory domainObjectFactory = domainObjectFactoryServiceTracker.waitForService(0);
                final Dictionary<String, Object> properties = new Hashtable<String, Object>();
                final com.sap.sailing.domain.tractracadapter.DomainFactory domainFactory = TracTracAdapterFactory.INSTANCE.getOrCreateTracTracAdapter(
                        domainObjectFactory.getBaseDomainFactory()).getTracTracDomainFactory();
                final ConnectivityParamsHandler paramsHandler = new ConnectivityParamsHandler(
                        MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(mongoObjectFactory, domainObjectFactory),
                        MongoRegattaLogStoreFactory.INSTANCE.getMongoRegattaLogStore(mongoObjectFactory, domainObjectFactory),
                        domainFactory);
                properties.put(TypeBasedServiceFinder.TYPE, RaceTrackingConnectivityParametersImpl.TYPE);
                context.registerService(RaceTrackingConnectivityParametersHandler.class, paramsHandler, properties);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception trying to register TracTrac RaceTrackingConnectivityParametersHandler implementation", e);
            }
        }, getClass().getName() + " registering connectivity handler").start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
