package com.sap.sailing.domain.vakarosadapter;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sap.sailing.server.trackfiles.common.GPSFixImporterRegistration;
import com.sap.sailing.server.trackfiles.common.SensorDataImporterRegistration;

public class Activator implements BundleActivator {
    private static BundleContext context;

    static BundleContext getContext() {
        return context;
    }

    public void start(BundleContext bundleContext) throws Exception {
        context = bundleContext;
        GPSFixImporterRegistration.register(new VakarosGPSFixImporter(), bundleContext);
        SensorDataImporterRegistration.register(new VakarosExtendedDataImporterImpl(), bundleContext);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        Activator.context = null;
    }
}
