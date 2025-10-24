package com.sap.sailing.news;

import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.sap.sailing.news.impl.EventNewsProviderRegistryImpl;
import com.sap.sailing.news.impl.EventNewsServiceImpl;
import com.sap.sailing.news.impl.LeaderboardUpdateEventNewsProvider;
import com.sap.sse.util.ServiceTrackerFactory;

public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());
    private ServiceTracker<EventNewsProvider, EventNewsProvider> newsProviderTracker;

    public void start(BundleContext context) throws Exception {
        final EventNewsProviderRegistry providerRegistry = new EventNewsProviderRegistryImpl();
        final EventNewsService newsService = new EventNewsServiceImpl(providerRegistry);
        context.registerService(EventNewsService.class, newsService, null);
        newsProviderTracker = ServiceTrackerFactory.createAndOpen(context, EventNewsProvider.class, new ServiceTrackerCustomizer<EventNewsProvider, EventNewsProvider>() {
            @Override
            public EventNewsProvider addingService(ServiceReference<EventNewsProvider> reference) {
                final EventNewsProvider eventNewsProvider = context.getService(reference);
                logger.info("Registering event news provider "+eventNewsProvider);
                providerRegistry.registerNewsProvider(eventNewsProvider);
                return eventNewsProvider;
            }

            @Override
            public void modifiedService(ServiceReference<EventNewsProvider> reference, EventNewsProvider service) {
            }

            @Override
            public void removedService(ServiceReference<EventNewsProvider> reference, EventNewsProvider service) {
                final EventNewsProvider eventNewsProvider = context.getService(reference);
                logger.info("De-registering event news provider "+eventNewsProvider);
                providerRegistry.deregisterNewsProvider(eventNewsProvider);
            }
        });
        logger.fine("News provider tracker created: "+newsProviderTracker);
        context.registerService(EventNewsProvider.class, new LeaderboardUpdateEventNewsProvider(), null);
        // The json event news provider is only a fallback and should not be activated per default:
        // context.registerService(EventNewsProvider.class, new LeaderboardUpdateEventNewsProvider(), null);
        logger.info("EventNews Service registered.");
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }
}
