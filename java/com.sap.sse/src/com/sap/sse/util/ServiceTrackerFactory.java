package com.sap.sse.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ServiceTrackerFactory {
    private static final Logger logger = Logger.getLogger(ServiceTrackerFactory.class.getName());
    
    /**
     * @return {@code null} if {@code context} is {@code null}
     */
    public static <T> ServiceTracker<T, T> createAndOpen(BundleContext context, Class<T> clazz) {
        return createAndOpen(context, clazz, /* customizer */ null);
    }
    
    public static <T> ServiceTracker<T, T> createAndOpen(BundleContext context, Class<T> clazz, ServiceTrackerCustomizer<T, T> customizer) {
        final ServiceTracker<T, T> result;
        if (context == null) {
            logger.info("No BundleContext provided. Returning null.");
            result = null;
        } else {
            result = new ServiceTracker<T, T>(context, clazz, customizer);
            result.open();
        }
        return result;
    }
    
    public static <T> Future<T> createServiceFuture(final BundleContext bundleContext, final Class<T> serviceClass) {
        final ServiceTracker<T, T> tracker = new ServiceTracker<>(bundleContext, serviceClass, /* customizer */ null);
        tracker.open();
        final FutureTask<T> result = new FutureTask<>(new Callable<T>() {
            @Override
            public T call() throws InterruptedException {
                try {
                    logger.info("Waiting for "+serviceClass.getSimpleName()+" service...");
                    T service = tracker.waitForService(0);
                    logger.info("Obtained UserStore service "+service);
                    return service;
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Interrupted while waiting for "+serviceClass.getSimpleName()+" service", e);
                    throw e;
                }
            }
        });
        new Thread("ServiceTracker waiting for "+serviceClass.getSimpleName()+" service") {
            @Override
            public void run() {
                try {
                    result.run();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Exception while waiting for "+serviceClass.getSimpleName()+" service", e);
                }
            }
        }.start();
        return result;
    }
}
