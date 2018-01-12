package com.sap.sailing.domain.bravoadapter.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sap.sailing.server.trackfiles.common.GPSFixImporterRegistration;

public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        GPSFixImporterRegistration.register(new BravoGPSFixImporter(), context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
