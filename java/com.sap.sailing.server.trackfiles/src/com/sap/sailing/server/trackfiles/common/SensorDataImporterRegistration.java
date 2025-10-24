package com.sap.sailing.server.trackfiles.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.sap.sailing.domain.trackimport.DoubleVectorFixImporter;
import com.sap.sse.common.TypeBasedServiceFinder;

public class SensorDataImporterRegistration {
    private static Dictionary<String, String> getDict(String type) {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(TypeBasedServiceFinder.TYPE, type);
        return properties;
    }

    public static Collection<ServiceRegistration<DoubleVectorFixImporter>> register(DoubleVectorFixImporter importer, BundleContext context) {
        List<ServiceRegistration<DoubleVectorFixImporter>> registrations = new ArrayList<>();
        registrations.add(context.registerService(DoubleVectorFixImporter.class, importer, getDict(importer.getType())));
        return registrations;
    }
}
