package com.sap.sailing.polars.impl;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.polars.jaxrs.client.PolarDataClient;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.replication.Replicable;
import com.sap.sse.util.ClearStateTestSupport;

/**
 * Bundle activator that constructs and registers the {@link PolarDataService} in the OSGi
 * registry exactly once, only after the service is fully initialized. Initialization consists of:
 * <ol>
 *   <li>Waiting for the {@link RacingEventService} to be registered (via an OSGi
 *   {@link ServiceTracker}), then obtaining the {@link DomainFactory} from it.</li>
 *   <li>Installing that domain factory on the {@link PolarDataServiceImpl} instance so that
 *   subsequent deserialization (e.g. via {@link PolarDataService#runWithDomainFactory}) resolves
 *   boat classes against the same factory the rest of the server uses.</li>
 *   <li>Optionally fetching polar regression data from a remote URL (if
 *   {@value #POLAR_DATA_SOURCE_URL_PROPERTY_NAME} is set), which populates the miner's
 *   regressions before any consumer sees the service.</li>
 *   <li>Registering the service in the OSGi registry.</li>
 * </ol>
 *
 * With this ordering, the OSGi contract "if it's registered, it's ready" holds. Consumers
 * (notably {@code RacingEventServiceImpl.setPolarDataService}) are only invoked once with a
 * usable service and the previous unregister/re-register dance around the remote fetch is gone.
 * See bug6241.
 *
 * @author D054528 (Frederik Petersen)
 */
public class Activator implements BundleActivator {

    private static final String POLAR_DATA_SOURCE_URL_PROPERTY_NAME = "polardata.source.url";
    private static final String POLAR_DATA_SOURCE_BEARER_TOKEN_PROPERTY_NAME = "polardata.source.bearertoken";

    private static final Logger logger = Logger.getLogger(Activator.class.getName());

    private final Set<ServiceRegistration<?>> registrations = new HashSet<>();
    private volatile Thread initializerThread;
    private volatile ServiceTracker<RacingEventService, RacingEventService> racingEventServiceTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        logger.info("PolarDataService bundle started; awaiting RacingEventService before initializing and registering");
        final String polarDataSourceURL = System.getProperty(POLAR_DATA_SOURCE_URL_PROPERTY_NAME);
        final String polarDataBearerToken = System.getProperty(POLAR_DATA_SOURCE_BEARER_TOKEN_PROPERTY_NAME);
        // We wait on the OSGi RacingEventService instead of relying on the previous pattern where
        // PolarDataService was registered early and then unregistered/re-registered around the
        // remote data fetch. Reversing the dependency direction (polar bundle actively pulls the
        // domain factory from RacingEventService via a ServiceTracker) lets us register the
        // PolarDataService exactly once, after full initialization. See bug6241.
        racingEventServiceTracker = new ServiceTracker<>(context, RacingEventService.class, null);
        racingEventServiceTracker.open();
        final Thread thread = new Thread(() -> {
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader()); // classpath:... Shiro ini files
                initializeAndRegisterPolarDataService(context, polarDataSourceURL,
                        Optional.ofNullable(polarDataBearerToken));
            } catch (InterruptedException e) {
                logger.log(Level.WARNING,
                        "Interrupted while waiting for RacingEventService; polar-data service will not be registered",
                        e);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize and register PolarDataService", e);
            }
        }, "PolarDataService initializer");
        thread.setDaemon(true);
        initializerThread = thread;
        thread.start();
    }

    private void initializeAndRegisterPolarDataService(final BundleContext context,
            final String polarDataSourceURL, final Optional<String> polarDataBearerToken)
            throws InterruptedException {
        logger.info("Waiting for RacingEventService to become available...");
        final RacingEventService racingEventService = racingEventServiceTracker.waitForService(0);
        if (racingEventService == null) {
            logger.warning("RacingEventService tracker returned null (bundle stopping?); aborting polar-data initialization");
        } else {
            final DomainFactory domainFactory = racingEventService.getBaseDomainFactory();
            logger.info("Obtained DomainFactory from RacingEventService; constructing PolarDataService");
            // Gated mode: RacingEventServiceImpl.restoreTrackedRaces() promises to call
            // markLoadingOfAllRacesToRestoreStarted() once its enumeration loop has fired for every
            // race to restore. Until then, runWhenPolarLoadingFinishedFor(...) callbacks are held
            // to avoid firing on a transient idle window of the loading pipeline between races.
            // See bug6241.Why
            final PolarDataServiceImpl service = new PolarDataServiceImpl(
                    /* waitForLoadingOfAllRacesToRestoreToBeStarted */ true);
            service.registerDomainFactory(domainFactory);
            if (polarDataSourceURL != null && !polarDataSourceURL.isEmpty()) {
                logger.info("Fetching polar regression data from " + polarDataSourceURL);
                final PolarDataClient polarDataClient = new PolarDataClient(polarDataSourceURL, service,
                        polarDataBearerToken);
                try {
                    polarDataClient.updatePolarDataRegressions();
                    logger.info("Polar regression data loaded from " + polarDataSourceURL);
                } catch (Exception e) {
                    // Log SEVERE but continue with registration: an empty regression state is
                    // preferable to no service at all, and the polars can still be updated later
                    // through other channels (JAX-RS API, replication).
                    logger.log(Level.SEVERE, "Exception while trying to import polar data from " + polarDataSourceURL
                            + "; registering PolarDataService anyway with whatever state it has now", e);
                }
            }
            logger.info("Registering PolarDataService");
            registrations.add(context.registerService(PolarDataService.class, service, null));
            final Dictionary<String, String> replicableServiceProperties = new Hashtable<>();
            replicableServiceProperties.put(Replicable.OSGi_Service_Registry_ID_Property_Name,
                    service.getId().toString());
            registrations.add(context.registerService(Replicable.class, service, replicableServiceProperties));
            registrations.add(context.registerService(ClearStateTestSupport.class.getName(), service, null));
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logger.info("Unregistering PolarDataService");
        final Thread thread = initializerThread;
        if (thread != null) {
            thread.interrupt();
        }
        final ServiceTracker<RacingEventService, RacingEventService> tracker = racingEventServiceTracker;
        if (tracker != null) {
            tracker.close();
        }
        for (final ServiceRegistration<?> reg : registrations) {
            reg.unregister();
        }
        registrations.clear();
    }

}
