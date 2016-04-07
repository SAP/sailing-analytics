package com.sap.sailing.domain.racelogtracking.impl;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.persistence.racelog.tracking.DeviceIdentifierMongoHandler;
import com.sap.sailing.domain.racelog.tracking.SensorFixMapper;
import com.sap.sailing.domain.racelogsensortracking.impl.SensorFixMapperFactoryImpl;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifierStringSerializationHandler;
import com.sap.sailing.domain.racelogtracking.PingDeviceIdentifierImpl;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.domain.racelogtracking.SmartphoneUUIDIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.tracking.TrackedRegattaListener;
import com.sap.sailing.server.MasterDataImportClassLoaderService;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.GPSFixJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.GPSFixMovingJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.GPSFixJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.GPSFixMovingJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.GPSFixJsonHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.impl.GPSFixJsonHandlerImpl;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.impl.SmartphoneUUIDJsonHandler;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.util.ServiceTrackerFactory;

public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());
    
    private static BundleContext context;
    private ServiceTracker<RacingEventService, RacingEventService> racingEventServiceTracker;
    private ServiceTracker<SensorFixMapper<?, ?, ?>, SensorFixMapper<?, ?, ?>> sensorFixMapperTracker;

    public static BundleContext getContext() {
        return context;
    }
	
    private Set<ServiceRegistration<?>> registrations = new HashSet<>();

    private Dictionary<String, String> getDict(String type) {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(TypeBasedServiceFinder.TYPE, type);
        return properties;
    }

    private <T extends GPSFix> void registerGPSFixJsonService(BundleContext context, JsonDeserializer<T> deserializer,
            JsonSerializer<T> serializer, String type) {
        registrations.add(context.registerService(GPSFixJsonHandler.class,
                new GPSFixJsonHandlerImpl<T>(deserializer, serializer), getDict(type)));
    }
    
    @Override
    public void start(BundleContext context) throws Exception {
        Activator.context = context;
        
        registrations.add(context.registerService(DeviceIdentifierMongoHandler.class, new SmartphoneUUIDMongoHandler(), getDict(SmartphoneUUIDIdentifier.TYPE)));
        registrations.add(context.registerService(DeviceIdentifierJsonHandler.class, new SmartphoneUUIDJsonHandler(), getDict(SmartphoneUUIDIdentifier.TYPE)));
        registrations.add(context.registerService(DeviceIdentifierStringSerializationHandler.class, new SmartphoneUUIDStringSerializationHandler(), getDict(SmartphoneUUIDIdentifier.TYPE)));
        
        registrations.add(context.registerService(DeviceIdentifierMongoHandler.class, new PingDeviceIdentifierMongoHandler(), getDict(PingDeviceIdentifierImpl.TYPE)));
        registrations.add(context.registerService(DeviceIdentifierJsonHandler.class, new PingDeviceIdentifierJsonHandler(), getDict(PingDeviceIdentifierImpl.TYPE)));
        
        registrations.add(context.registerService(DeviceIdentifierMongoHandler.class, new TrackFileImportDeviceIdentifierMongoHandler(), getDict(TrackFileImportDeviceIdentifier.TYPE)));
        registrations.add(context.registerService(DeviceIdentifierJsonHandler.class, new TrackFileImportDeviceIdentifierJsonHandler(), getDict(TrackFileImportDeviceIdentifier.TYPE)));
        registrations.add(context.registerService(DeviceIdentifierStringSerializationHandler.class, new TrackFileImportDeviceIdentifierStringSerializationHandler(), getDict(TrackFileImportDeviceIdentifier.TYPE)));
        
        registerGPSFixJsonService(context, new GPSFixJsonDeserializer(), new GPSFixJsonSerializer(), GPSFixJsonDeserializer.TYPE);
        registerGPSFixJsonService(context, new GPSFixMovingJsonDeserializer(), new GPSFixMovingJsonSerializer(), GPSFixMovingJsonDeserializer.TYPE);
        
        registrations.add(context.registerService(RaceLogTrackingAdapterFactory.class, RaceLogTrackingAdapterFactory.INSTANCE, null));

        registrations.add(context.registerService(MasterDataImportClassLoaderService.class,
                new MasterDataImportClassLoaderServiceImpl(), null));
        registrations.add(context.registerService(SensorFixMapper.class, new BravoDataFixMapper(), null));
        
        sensorFixMapperTracker = (ServiceTracker) ServiceTrackerFactory.createAndOpen(context, SensorFixMapper.class);
        
        racingEventServiceTracker = ServiceTrackerFactory.createAndOpen(
                context,
                RacingEventService.class);

        registrations.add(context.registerService(TrackedRegattaListener.class,
                new RegattaLogSensorDataTrackerTrackedRegattaListener(racingEventServiceTracker,
                        new SensorFixMapperFactoryImpl(sensorFixMapperTracker)), null));
        
        logger.log(Level.INFO, "Started "+context.getBundle().getSymbolicName());
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        Activator.context = null;
        racingEventServiceTracker.close();
        sensorFixMapperTracker.close();
        for (ServiceRegistration<?> reg : registrations) {
            reg.unregister();
        }
        registrations.clear();
    }
}
