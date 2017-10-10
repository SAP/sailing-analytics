package com.sap.sailing.server.trackfiles.impl;

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.sap.sailing.domain.common.sensordata.BravoSensorDataMetadata;
import com.sap.sailing.server.trackfiles.common.GPSFixImporterRegistration;
import com.sap.sailing.server.trackfiles.common.SensorDataImporterRegistration;

public class Activator implements BundleActivator {
    private Set<ServiceRegistration<?>> registrations = new HashSet<>();

    @Override
    public void start(BundleContext context) throws Exception {
        registrations.addAll(GPSFixImporterRegistration.register(new RouteConverterGPSFixImporterImpl(), context));
        registrations.addAll(SensorDataImporterRegistration.register(
                new BravoDataImporterImpl(BravoSensorDataMetadata.getColumnNamesToIndexInDoubleFix()), context));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        for (ServiceRegistration<?> reg : registrations) {
            reg.unregister();
        }
        registrations.clear();
    }
}
