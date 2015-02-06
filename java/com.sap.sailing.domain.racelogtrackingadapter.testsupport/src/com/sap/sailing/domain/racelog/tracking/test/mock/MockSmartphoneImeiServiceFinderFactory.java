package com.sap.sailing.domain.racelog.tracking.test.mock;

import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.domain.persistence.racelog.tracking.DeviceIdentifierMongoHandler;
import com.sap.sailing.domain.persistence.racelog.tracking.GPSFixMongoHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.GPSFixJsonHandler;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;

/**
 * A simplified implementation of the {@link TypeBasedServiceFinder} interface that, when the device type
 * {@link SmartphoneImeiIdentifier#TYPE} is requested, returns a specific handler that was passed to this object's
 * constructor.
 * 
 * @author Fredrik Teschke
 *
 */
public class MockSmartphoneImeiServiceFinderFactory implements TypeBasedServiceFinderFactory {
    Map<Class<?>, TypeBasedServiceFinder<?>> serviceFinders = new HashMap<Class<?>, TypeBasedServiceFinder<?>>();

    public MockSmartphoneImeiServiceFinderFactory() {
        serviceFinders.put(DeviceIdentifierMongoHandler.class, new MockServiceFinder<>(new SmartphoneImeiMongoHandler()));
        serviceFinders.put(DeviceIdentifierJsonHandler.class, new MockServiceFinder<>(new SmartphoneImeiJsonHandler()));
        serviceFinders.put(GPSFixMongoHandler.class, new MockGPSFixMongoServiceFinder());
        serviceFinders.put(GPSFixJsonHandler.class, new MockServiceFinder<>(new MockGPSFixJsonHandler()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ServiceT> TypeBasedServiceFinder<ServiceT> createServiceFinder(Class<ServiceT> clazz) {
        return (TypeBasedServiceFinder<ServiceT>) serviceFinders.get(clazz);
    }
}
