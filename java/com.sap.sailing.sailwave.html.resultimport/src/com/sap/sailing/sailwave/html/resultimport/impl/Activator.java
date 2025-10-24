package com.sap.sailing.sailwave.html.resultimport.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.resultimport.impl.ResultUrlRegistryServiceTrackerCustomizer;

public class Activator implements BundleActivator {
    private ServiceTracker<ResultUrlRegistry, ResultUrlRegistry> resultUrlRegistryServiceTracker;

    public void start(BundleContext bundleContext) throws Exception {
        resultUrlRegistryServiceTracker = new ServiceTracker<>(bundleContext, ResultUrlRegistry.class,
                new ResultUrlRegistryServiceTrackerCustomizer(bundleContext) {
                    @Override
                    protected ScoreCorrectionProvider configureScoreCorrectionProvider(
                            ResultUrlRegistry resultUrlRegistry) {
                        final ScoreCorrectionProviderImpl service = new ScoreCorrectionProviderImpl(resultUrlRegistry);
                        return service;
                    }
                });
        resultUrlRegistryServiceTracker.open();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        resultUrlRegistryServiceTracker.close();
    }
}
