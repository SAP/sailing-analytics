package com.sap.sailing.server.impl;

import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.sap.sailing.competitorimport.CompetitorProvider;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.MasterDataImportClassLoaderService;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.domain.common.WindFinderReviewedSpotsCollectionIdProvider;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.subscription.PremiumRole;
import com.sap.sailing.domain.common.subscription.SailingSubscriptionPlan;
import com.sap.sailing.domain.common.tracking.impl.DoubleVectorFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.racelog.tracking.FixMongoHandler;
import com.sap.sailing.domain.persistence.racelog.tracking.impl.DoubleVectorFixMongoHandlerImpl;
import com.sap.sailing.domain.persistence.racelog.tracking.impl.GPSFixMongoHandlerImpl;
import com.sap.sailing.domain.persistence.racelog.tracking.impl.GPSFixMovingMongoHandlerImpl;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.racelog.tracking.SensorFixStoreSupplier;
import com.sap.sailing.domain.tracking.TrackedRegattaListener;
import com.sap.sailing.domain.windestimation.WindEstimationFactoryService;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.server.RacingEventServiceMXBean;
import com.sap.sailing.server.impl.preferences.model.BoatClassNotificationPreferences;
import com.sap.sailing.server.impl.preferences.model.CompetitorNotificationPreferences;
import com.sap.sailing.server.impl.preferences.model.StoredDataMiningQueryPreferences;
import com.sap.sailing.server.impl.preferences.model.StoredDataMiningReportPreferences;
import com.sap.sailing.server.impl.preferences.model.TrackedEventPreferences;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.notification.impl.SailingNotificationServiceImpl;
import com.sap.sailing.server.preferences.SailorProfilePreferences;
import com.sap.sailing.server.security.EventManagerRole;
import com.sap.sailing.server.security.SailingViewerRole;
import com.sap.sailing.server.statistics.TrackedRaceStatisticsCache;
import com.sap.sailing.server.statistics.TrackedRaceStatisticsCacheImpl;
import com.sap.sailing.shared.server.SharedSailingData;
import com.sap.sse.classloading.ServiceTrackerCustomizerForClassLoaderSupplierRegistrations;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.Util;
import com.sap.sse.mail.MailService;
import com.sap.sse.mail.queue.MailQueue;
import com.sap.sse.mail.queue.impl.ExecutorMailQueue;
import com.sap.sse.osgi.CachedOsgiTypeBasedServiceFinderFactory;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.security.SecurityInitializationCustomizer;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SecurityUrlPathProvider;
import com.sap.sse.security.interfaces.PreferenceConverter;
import com.sap.sse.security.shared.HasPermissionsProvider;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.ServerAdminRole;
import com.sap.sse.security.shared.SubscriptionPlanProvider;
import com.sap.sse.security.shared.subscription.AllDataMiningRole;
import com.sap.sse.security.shared.subscription.ArchiveDataMiningRole;
import com.sap.sse.security.util.GenericJSONPreferenceConverter;
import com.sap.sse.util.ClearStateTestSupport;
import com.sap.sse.util.ServiceTrackerFactory;

public class Activator implements BundleActivator {

    private static final Logger logger = Logger.getLogger(Activator.class.getName());

    private static final String CLEAR_PERSISTENT_COMPETITORS_PROPERTY_NAME = "persistentcompetitors.clear";
    
    private static final String RESTORE_TRACKED_RACES_PROPERTY_NAME = "restore.tracked.races";

    private static ExtenderBundleTracker extenderBundleTracker;

    private static BundleContext context;

    private CachedOsgiTypeBasedServiceFinderFactory serviceFinderFactory;

    private RacingEventServiceImpl racingEventService;

    private final boolean clearPersistentCompetitors;
    
    private final boolean restoreTrackedRaces;

    private Set<ServiceRegistration<?>> registrations = new HashSet<>();

    private ObjectName mBeanName;

    private ServiceTracker<MasterDataImportClassLoaderService, MasterDataImportClassLoaderService> masterDataImportClassLoaderServiceTracker;

    private ServiceTracker<PolarDataService, PolarDataService> polarDataServiceTracker;
    
    private ServiceTracker<WindEstimationFactoryService, WindEstimationFactoryService> windEstimationFactoryServiceTrack;

    private OSGiBasedTrackedRegattaListener trackedRegattaListener;

    private MailQueue mailQueue;

    private SailingNotificationServiceImpl notificationService;

    private ServiceTracker<MailService, MailService> mailServiceTracker;

    private FullyInitializedReplicableTracker<SecurityService> securityServiceTracker;

    private FullyInitializedReplicableTracker<SharedSailingData> sharedSailingDataTracker;
    
    private ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker;
    
    public Activator() {
        clearPersistentCompetitors = Boolean
                .valueOf(System.getProperty(CLEAR_PERSISTENT_COMPETITORS_PROPERTY_NAME, "" + false));
        restoreTrackedRaces = Boolean
                .valueOf(System.getProperty(RESTORE_TRACKED_RACES_PROPERTY_NAME, "" + false));
        logger.log(Level.INFO,
                "setting " + CLEAR_PERSISTENT_COMPETITORS_PROPERTY_NAME + " to " + clearPersistentCompetitors);
        // there is exactly one instance of the racingEventService in the whole server
    }

    public void start(BundleContext context) throws Exception {
        Activator.context = context;
        extenderBundleTracker = new ExtenderBundleTracker(context);
        extenderBundleTracker.open();
        mailServiceTracker = ServiceTrackerFactory.createAndOpen(context, MailService.class);
        replicationServiceTracker = ServiceTrackerFactory.createAndOpen(context, ReplicationService.class);
        sharedSailingDataTracker = FullyInitializedReplicableTracker.createAndOpen(context, SharedSailingData.class);
        securityServiceTracker = FullyInitializedReplicableTracker.createAndOpen(context, SecurityService.class);
        new Thread(""+this+" initializing RacingEventService in the background") {
            public void run() {
                try {
                    // we used to wait for the SecurityService here, but this now (see bug 4006) would be suspended until replication
                    // is finished with its initial load, and it's important to get RacingEventService registered with the OSGi service
                    // registry before the first access to the SecurityService, because only registering RacingEventService can unblock
                    // the replication and hence make a fully-initialized SecurityService with the initial load already completed available.
                    internalStartBundle(context);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Could not start RacingEvent service properly", e);
                }
            };
        }.start();
    }

    /**
     * {@link #racingEventService} must already be initialized when calling this method
     */
    protected void registerPreferenceConvertersForUserStore(BundleContext context) {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        // it's okay to re-use the properties Dictionary for several registrations because the registry clones its contents
        properties.put(PreferenceConverter.KEY_PARAMETER_NAME, CompetitorNotificationPreferences.PREF_NAME);
        registrations.add(context.registerService(PreferenceConverter.class,
                new GenericJSONPreferenceConverter<>(() -> new CompetitorNotificationPreferences()),
                properties));
        properties.put(PreferenceConverter.KEY_PARAMETER_NAME, BoatClassNotificationPreferences.PREF_NAME);
        registrations.add(context.registerService(PreferenceConverter.class,
                new GenericJSONPreferenceConverter<>(() -> new BoatClassNotificationPreferences(racingEventService)),
                properties));
        properties.put(PreferenceConverter.KEY_PARAMETER_NAME, StoredDataMiningQueryPreferences.PREF_NAME);
        registrations.add(context.registerService(PreferenceConverter.class,
                new GenericJSONPreferenceConverter<>(StoredDataMiningQueryPreferences::new), properties));
        properties.put(PreferenceConverter.KEY_PARAMETER_NAME, StoredDataMiningReportPreferences.PREF_NAME);
        registrations.add(context.registerService(PreferenceConverter.class,
                new GenericJSONPreferenceConverter<>(StoredDataMiningReportPreferences::new), properties));
        properties.put(PreferenceConverter.KEY_PARAMETER_NAME, SailorProfilePreferences.PREF_NAME);
        registrations.add(context.registerService(PreferenceConverter.class,
                new GenericJSONPreferenceConverter<>(
                        () -> new SailorProfilePreferences(racingEventService.getCompetitorAndBoatStore())),
                properties));
        properties.put(PreferenceConverter.KEY_PARAMETER_NAME, TrackedEventPreferences.PREF_NAME);
        registrations.add(context.registerService(PreferenceConverter.class,
                new GenericJSONPreferenceConverter<>(TrackedEventPreferences::new), properties));
    }

    public static BundleContext getContext() {
        return context;
    }

    public void stop(BundleContext context) throws Exception {
        masterDataImportClassLoaderServiceTracker.close();
        if (extenderBundleTracker != null) {
            extenderBundleTracker.close();
        }
        if (serviceFinderFactory != null) {
            serviceFinderFactory.close();
        }
        // stop the tracking of the wind and all races
        for (Util.Triple<Regatta, RaceDefinition, String> windTracker : racingEventService.getWindTrackedRaces()) {
            racingEventService.stopTrackingWind(windTracker.getA(), windTracker.getB());
        }
        for (Regatta regatta : racingEventService.getAllRegattas()) {
            racingEventService.stopTracking(regatta, /* willBeRemoved */ true);
        }
        for (ServiceRegistration<?> reg : registrations) {
            reg.unregister();
        }
        trackedRegattaListener.close();
        registrations.clear();
        notificationService.stop();
        mailQueue.stop();
        mailServiceTracker.close();
        sharedSailingDataTracker.close();
        replicationServiceTracker.close();
        securityServiceTracker.close();
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        mbs.unregisterMBean(mBeanName);
    }

    private void internalStartBundle(BundleContext context) throws MalformedURLException, MalformedObjectNameException,
            InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, InterruptedException {
        mailQueue = new ExecutorMailQueue(mailServiceTracker);
        notificationService = new SailingNotificationServiceImpl(context, mailQueue);
        trackedRegattaListener = new OSGiBasedTrackedRegattaListener(context);
        final Dictionary<String, String> sailingSecurityUrlPathProviderProperties = new Hashtable<>();
        sailingSecurityUrlPathProviderProperties.put(TypeBasedServiceFinder.TYPE,
                SecurityUrlPathProviderSailingImpl.APPLICATION);
        registrations.add(context.registerService(SecurityUrlPathProvider.class,
                new SecurityUrlPathProviderSailingImpl(), sailingSecurityUrlPathProviderProperties));
        registrations
                .add(context.registerService(HasPermissionsProvider.class, SecuredDomainType::getAllInstances, null));
        registrations.add(context.registerService(SubscriptionPlanProvider.class,
                SailingSubscriptionPlan::getAllInstances, null));
        registrations.add(context.registerService(SecurityInitializationCustomizer.class,
                (SecurityInitializationCustomizer) securityService -> {
                    final Thread backgroundThread = new Thread(()->{
                        ReplicationService replicationService;
                        try {
                            replicationService = ServiceTrackerFactory.createAndOpen(context, ReplicationService.class).waitForService(0);
                            if (!replicationService.isReplicationStarting() && securityService.getMasterDescriptor() == null) {
                                // see also bug 5569: this must only be done if it is clear that this instance is not to become a replica
                                // TODO: Registering RoleDefinitions here requires additional maintenance. Consider
                                // implementing another Construct like OSGIHasPermissionsProvider
                                final RoleDefinition sailingViewerRoleDefinition = securityService
                                        .getOrCreateRoleDefinitionFromPrototype(SailingViewerRole.getInstance(), /* makeReadableForAll */ true);
                                securityService.getOrCreateRoleDefinitionFromPrototype(EventManagerRole.getInstance(), /* makeReadableForAll */ true);
                                securityService.getOrCreateRoleDefinitionFromPrototype(ServerAdminRole.getInstance(), /* makeReadableForAll */ true);
                                securityService.getOrCreateRoleDefinitionFromPrototype(PremiumRole.getInstance(), /* makeReadableForAll */ true);
                                securityService.getOrCreateRoleDefinitionFromPrototype(ArchiveDataMiningRole.getInstance(), /* makeReadableForAll */ true);
                                securityService.getOrCreateRoleDefinitionFromPrototype(AllDataMiningRole.getInstance(), /* makeReadableForAll */ true);
                                if (securityService.isNewServer()) {
                                    // The server is initially set to be public by adding sailing_viewer role to the server group
                                    // with forAll=true
                                    securityService.putRoleDefinitionToUserGroup(securityService.getServerGroup(),
                                            sailingViewerRoleDefinition, true);
                                }
                            }
                        } catch (InterruptedException e) {
                            logger.log(Level.SEVERE, "Couldn't get a hold of the ReplicationService to tell whether this SecurityService is to become a replica; "+
                                    "not setting server to public, not enforcing READability of sailing_viewer role", e);
                        }
                    }, "Waiting for replication service to tell whether this SecurityService will become a replica");
                    backgroundThread.setDaemon(true);
                    backgroundThread.start();
                }, null));
        final TrackedRaceStatisticsCache trackedRaceStatisticsCache = new TrackedRaceStatisticsCacheImpl();
        registrations.add(context.registerService(TrackedRaceStatisticsCache.class.getName(),
                trackedRaceStatisticsCache, null));
        registrations.add(context.registerService(TrackedRegattaListener.class.getName(),
                trackedRaceStatisticsCache, null));
        // At this point the OSGi resolver is used as device type service finder.
        // In the case that we are not in an OSGi context (e.g. running a JUnit test instead),
        // this code block is not run, and the test case can inject some other type of finder
        // instead.
        serviceFinderFactory = new CachedOsgiTypeBasedServiceFinderFactory(context);
        ServiceTracker<ScoreCorrectionProvider, ScoreCorrectionProvider> scoreCorrectionProviderServiceTracker =
                ServiceTrackerFactory.createAndOpen(context, ScoreCorrectionProvider.class);
        ServiceTracker<CompetitorProvider, CompetitorProvider> competitorProviderServiceTracker =
                ServiceTrackerFactory.createAndOpen(context, CompetitorProvider.class);
        ServiceTracker<ResultUrlRegistry, ResultUrlRegistry> resultUrlRegistryServiceTracker = ServiceTrackerFactory
                .createAndOpen(context, ResultUrlRegistry.class);
        racingEventService = new RacingEventServiceImpl(clearPersistentCompetitors,
                /* sensorFixStore */ null, serviceFinderFactory, trackedRegattaListener,
                notificationService, trackedRaceStatisticsCache, restoreTrackedRaces, securityServiceTracker,
                sharedSailingDataTracker, replicationServiceTracker, scoreCorrectionProviderServiceTracker, competitorProviderServiceTracker, resultUrlRegistryServiceTracker);
        notificationService.setRacingEventService(racingEventService);
        // start watching out for MasterDataImportClassLoaderService instances in the OSGi service registry and manage
        // the combined class loader accordingly:
        masterDataImportClassLoaderServiceTracker = ServiceTrackerCustomizerForClassLoaderSupplierRegistrations
                .createClassLoaderSupplierServiceTracker(context, MasterDataImportClassLoaderService.class, racingEventService.getMasterDataClassLoaders());
        polarDataServiceTracker = new ServiceTracker<PolarDataService, PolarDataService>(context,
                PolarDataService.class,
                new PolarDataServiceTrackerCustomizer(context, racingEventService));
        polarDataServiceTracker.open();
        windEstimationFactoryServiceTrack = new ServiceTracker<WindEstimationFactoryService, WindEstimationFactoryService>(context,
                WindEstimationFactoryService.class, new WindEstimationFactoryServiceTrackerCustomizer(context, racingEventService));
        windEstimationFactoryServiceTrack.open();
        // register the racing service in the OSGi registry
        racingEventService.setBundleContext(context);
        context.registerService(MongoObjectFactory.class, racingEventService.getMongoObjectFactory(), /* properties */ null);
        context.registerService(DomainObjectFactory.class, racingEventService.getDomainObjectFactory(), /* properties */ null);
        final Dictionary<String, String> replicableServiceProperties = new Hashtable<>();
        replicableServiceProperties.put(Replicable.OSGi_Service_Registry_ID_Property_Name,
                racingEventService.getId().toString());
        context.registerService(Replicable.class, racingEventService, replicableServiceProperties);
        context.registerService(RacingEventService.class, racingEventService, null);
        context.registerService(RaceLogResolver.class, racingEventService, null);
        context.registerService(ClearStateTestSupport.class, racingEventService, null);
        context.registerService(SensorFixStoreSupplier.class, racingEventService, null);
        context.registerService(WindFinderReviewedSpotsCollectionIdProvider.class, racingEventService,
                null);
        Dictionary<String, String> properties = new Hashtable<String, String>();
        final GPSFixMongoHandlerImpl gpsFixMongoHandler = new GPSFixMongoHandlerImpl(
                racingEventService.getMongoObjectFactory(),
                racingEventService.getDomainObjectFactory());
        properties.put(TypeBasedServiceFinder.TYPE, GPSFixImpl.class.getName());
        registrations
                .add(context.registerService(FixMongoHandler.class, gpsFixMongoHandler, properties));
        // legacy type name; some DBs may still contain fixes marked with this old package name:
        properties.put(TypeBasedServiceFinder.TYPE, "com.sap.sailing.domain.tracking.impl.GPSFixImpl");
        registrations
                .add(context.registerService(FixMongoHandler.class, gpsFixMongoHandler, properties));
        final GPSFixMovingMongoHandlerImpl gpsFixMovingMongoHandler = new GPSFixMovingMongoHandlerImpl(
                racingEventService.getMongoObjectFactory(),
                racingEventService.getDomainObjectFactory());
        properties.put(TypeBasedServiceFinder.TYPE, GPSFixMovingImpl.class.getName());
        registrations.add(
                context.registerService(FixMongoHandler.class, gpsFixMovingMongoHandler, properties));
        // legacy type name; some DBs may still contain fixes marked with this old package name:
        properties.put(TypeBasedServiceFinder.TYPE,
                "com.sap.sailing.domain.tracking.impl.GPSFixMovingImpl");
        registrations.add(
                context.registerService(FixMongoHandler.class, gpsFixMovingMongoHandler, properties));
        properties.put(TypeBasedServiceFinder.TYPE, DoubleVectorFixImpl.class.getName());
        registrations.add(context.registerService(FixMongoHandler.class,
                new DoubleVectorFixMongoHandlerImpl(racingEventService.getMongoObjectFactory(),
                        racingEventService.getDomainObjectFactory()),
                properties));
        registerPreferenceConvertersForUserStore(context);
        // Add an MBean for the service to the JMX bean server:
        RacingEventServiceMXBean mbean = new RacingEventServiceMXBeanImpl(racingEventService);
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        mBeanName = new ObjectName("com.sap.sailing:type=RacingEventService");
        mbs.registerMBean(mbean, mBeanName);
        logger.log(Level.INFO, "Started " + context.getBundle().getSymbolicName()
                + ". Character encoding: " + Charset.defaultCharset());
        // do initial setup/migration logic; do this after the RacingEventService has been published to the OSGi
        // registry because this will require the SecurityService and that can only become available once the initial
        // load has been finished in case this is a replica with auto-replication.
        racingEventService.ensureOwnerships();
        racingEventService.migrateCompetitorNotificationPreferencesWithCompetitorNames();
    }

    private class PolarDataServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<PolarDataService, PolarDataService> {
        private final BundleContext context;
        private RacingEventServiceImpl racingEventService;

        public PolarDataServiceTrackerCustomizer(BundleContext context, RacingEventServiceImpl racingEventService) {
            this.context = context;
            this.racingEventService = racingEventService;
        }

        @Override
        public PolarDataService addingService(ServiceReference<PolarDataService> reference) {
            PolarDataService service = context.getService(reference);
            racingEventService.setPolarDataService(service);
            return service;
        }

        @Override
        public void modifiedService(ServiceReference<PolarDataService> reference, PolarDataService service) {
        }

        @Override
        public void removedService(ServiceReference<PolarDataService> reference, PolarDataService service) {
            racingEventService.unsetPolarDataService(service);
        }
    }
    
    private class WindEstimationFactoryServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<WindEstimationFactoryService, WindEstimationFactoryService> {
        private final BundleContext context;
        private final RacingEventServiceImpl racingEventService;

        public WindEstimationFactoryServiceTrackerCustomizer(BundleContext context,
                RacingEventServiceImpl racingEventService) {
            this.context = context;
            this.racingEventService = racingEventService;
        }

        @Override
        public WindEstimationFactoryService addingService(ServiceReference<WindEstimationFactoryService> reference) {
            WindEstimationFactoryService service = context.getService(reference);
            service.addWindEstimationModelsChangedListenerAndReceiveUpdate(windEstimationReady -> {
                // setting the wind estimation factory service to null here in case it becomes
                // unavailable is the reason we may not need a specific implementation of
                // removedService(...) here. Yet, just to be on the safe side, we'll also
                // set the wind estimation factory service to null there. Maybe the service
                // is de-registered without shutting the service down...
                racingEventService.setWindEstimationFactoryService(windEstimationReady ? service : null);
            });
            return service;
        }

        @Override
        public void modifiedService(ServiceReference<WindEstimationFactoryService> reference,
                WindEstimationFactoryService service) {
        }

        @Override
        public void removedService(ServiceReference<WindEstimationFactoryService> reference,
                WindEstimationFactoryService service) {
            racingEventService.setWindEstimationFactoryService(null);
        }
    }
}
