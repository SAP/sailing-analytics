package com.sap.sailing.domain.racelog.tracking.test.mock;

import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.domain.persistence.racelog.tracking.FixMongoHandler;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDMongoHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.GPSFixJsonHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.impl.SmartphoneUUIDJsonHandler;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;

/**
 * A simplified implementation of the {@link TypeBasedServiceFinder} interface that, when the device type
 * {@link SmartphoneImeiIdentifierImpl#TYPE} is requested, returns a specific handler that was passed to this object's
 * constructor.
 * 
 * @author Fredrik Teschke
 *
 */
public class MockSmartphoneUuidServiceFinderFactory implements TypeBasedServiceFinderFactory {
    Map<Class<?>, TypeBasedServiceFinder<?>> serviceFinders = new HashMap<Class<?>, TypeBasedServiceFinder<?>>();

    public MockSmartphoneUuidServiceFinderFactory() {
        serviceFinders.put(DeviceIdentifierMongoHandler.class, new MockServiceFinder<>(new SmartphoneUUIDMongoHandler()));
        serviceFinders.put(DeviceIdentifierJsonHandler.class, new MockServiceFinder<>(new SmartphoneUUIDJsonHandler()));
        serviceFinders.put(FixMongoHandler.class, new MockGPSFixMongoServiceFinder());
        serviceFinders.put(GPSFixJsonHandler.class, new MockServiceFinder<>(new MockGPSFixJsonHandler()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ServiceT> TypeBasedServiceFinder<ServiceT> createServiceFinder(Class<ServiceT> clazz) {
        return (TypeBasedServiceFinder<ServiceT>) serviceFinders.get(clazz);
    }
}
