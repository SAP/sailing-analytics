package com.sap.sailing.server.racelog.tracking;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.sap.sailing.domain.common.racelog.tracking.NoCorrespondingServiceRegisteredException;
import com.sap.sailing.domain.common.racelog.tracking.TypeBasedServiceFinder;

/**
 * Caches OSGI services of a certain type, based on their type property. Has to be registered as the
 * {@link ServiceTrackerCustomizer} of a {@link ServiceTracker}.
 * 
 * @author Fredrik Teschke
 * 
 * @param <ServiceT>
 *            the type of service to be tracked
 */
public class CachedOsgiTypeBasedServiceFinder<ServiceT> implements ServiceTrackerCustomizer<ServiceT, ServiceT>,
        TypeBasedServiceFinder<ServiceT> {
    private final Map<String, ServiceT> services = new HashMap<>();
    private final BundleContext context;
    private Class<ServiceT> serviceType;

    public CachedOsgiTypeBasedServiceFinder(Class<ServiceT> serviceType, BundleContext context) {
    	this.serviceType = serviceType;
        this.context = context;
    }

    @Override
    public ServiceT findService(String type) {
        ServiceT service = services.get(type);

        if (service == null)
            throw new NoCorrespondingServiceRegisteredException("Could not find service", type, serviceType);

        return service;
    }

    @Override
    public ServiceT addingService(ServiceReference<ServiceT> reference) {
        String type = (String) reference.getProperty(TypeBasedServiceFinder.TYPE);
        ServiceT service = context.getService(reference);
        services.put(type, service);
        return service;
    }

    @Override
    public void modifiedService(ServiceReference<ServiceT> reference, ServiceT service) {
        addingService(reference);

    }

    @Override
    public void removedService(ServiceReference<ServiceT> reference, ServiceT service) {
        String type = (String) reference.getProperty(TypeBasedServiceFinder.TYPE);
        services.remove(type);
    }
}
