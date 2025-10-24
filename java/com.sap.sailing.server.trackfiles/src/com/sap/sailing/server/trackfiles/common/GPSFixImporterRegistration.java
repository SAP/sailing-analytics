package com.sap.sailing.server.trackfiles.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sse.common.TypeBasedServiceFinder;

public class GPSFixImporterRegistration {
    private static Dictionary<String, String> getDict(String type) {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(TypeBasedServiceFinder.TYPE, type);
        return properties;
    }
    
    /**
     * Registers the <code>importer</code> as an OSGi service for the {@link GPSFixImporter} interface, once for each
     * {@link GPSFixImporter#getSupportedFileExtensions() supported file extension}, using its
     * {@link GPSFixImporter#getType() type} as the {@link TypeBasedServiceFinder#TYPE} property and the
     * file extension as the {@link GPSFixImporter#FILE_EXTENSION_PROPERTY} property.
     */
    public static Collection<ServiceRegistration<GPSFixImporter>>
        register(GPSFixImporter importer, BundleContext context) {
        List<ServiceRegistration<GPSFixImporter>> registrations = new ArrayList<>();
        boolean registered = false;
        for (String ext : importer.getSupportedFileExtensions()) {
            Dictionary<String, String> properties = getDict(importer.getType());
            properties.put(GPSFixImporter.FILE_EXTENSION_PROPERTY, ext);
            registrations.add(context.registerService(GPSFixImporter.class, importer, properties));
            registered = true;
        }
        
        if (! registered) {
            registrations.add(context.registerService(GPSFixImporter.class, importer, getDict(importer.getType())));
        }
        return registrations;
    }
}
