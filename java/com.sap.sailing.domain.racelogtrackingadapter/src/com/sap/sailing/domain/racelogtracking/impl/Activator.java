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

import com.sap.sailing.domain.base.MasterDataImportClassLoaderService;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.racelog.tracking.SensorFixMapper;
import com.sap.sailing.domain.racelogsensortracking.impl.SensorFixMapperFactoryImpl;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifierStringSerializationHandler;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.domain.racelogtracking.SmartphoneUUIDIdentifier;
import com.sap.sailing.domain.racelogtracking.impl.fixtracker.RegattaLogFixTrackerRegattaListener;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParametersHandler;
import com.sap.sailing.domain.tracking.TrackedRegattaListener;
import com.sap.sailing.server.gateway.deserialization.impl.GPSFixJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.GPSFixMovingJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.GPSFixJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.GPSFixMovingJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.GPSFixJsonHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.impl.GPSFixJsonHandlerImpl;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.impl.SmartphoneUUIDJsonHandler;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.Replicable;
import com.sap.sse.shared.json.JsonDeserializer;
import com.sap.sse.shared.json.JsonSerializer;
import com.sap.sse.util.ServiceTrackerFactory;

public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());
    
    private static BundleContext context;
    private FullyInitializedReplicableTracker<RacingEventService> racingEventServiceTracker;
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
        registrations.add(context.registerService(DeviceIdentifierMongoHandler.class, new SmartphoneImeiMongoHandler(), getDict(SmartphoneImeiIdentifierImpl.TYPE)));
        registrations.add(context.registerService(DeviceIdentifierJsonHandler.class, new SmartphoneImeiJsonHandler(), getDict(SmartphoneImeiIdentifierImpl.TYPE)));
        registrations.add(context.registerService(DeviceIdentifierStringSerializationHandler.class, new SmartphoneImeiStringSerializationHandler(), getDict(SmartphoneImeiIdentifierImpl.TYPE)));
        registerGPSFixJsonService(context, new GPSFixJsonDeserializer(), new GPSFixJsonSerializer(), GPSFixJsonDeserializer.TYPE);
        registerGPSFixJsonService(context, new GPSFixMovingJsonDeserializer(), new GPSFixMovingJsonSerializer(), GPSFixMovingJsonDeserializer.TYPE);
        registrations.add(context.registerService(RaceLogTrackingAdapterFactory.class, RaceLogTrackingAdapterFactory.INSTANCE, null));
        registrations.add(context.registerService(MasterDataImportClassLoaderService.class,
                new MasterDataImportClassLoaderServiceImpl(), null));
        registrations.add(context.registerService(SensorFixMapper.class, new BravoDataFixMapper(), null));
        registrations.add(context.registerService(SensorFixMapper.class, new BravoExtendedDataFixMapper(), null));
        registrations.add(context.registerService(SensorFixMapper.class, new ExpeditionExtendedDataFixMapper(), null));
        sensorFixMapperTracker = createSensorFixMapperServiceTracker(context);
        racingEventServiceTracker = FullyInitializedReplicableTracker.createAndOpen(context, RacingEventService.class);
        RegattaLogFixTrackerRegattaListener regattaLogSensorDataTrackerTrackedRegattaListener = new RegattaLogFixTrackerRegattaListener(
                racingEventServiceTracker, new SensorFixMapperFactoryImpl(sensorFixMapperTracker));
        registrations.add(context.registerService(TrackedRegattaListener.class,
                regattaLogSensorDataTrackerTrackedRegattaListener, null));
        final Dictionary<String, String> replicableServiceProperties = new Hashtable<>();
        replicableServiceProperties.put(Replicable.OSGi_Service_Registry_ID_Property_Name,
                regattaLogSensorDataTrackerTrackedRegattaListener.getId().toString());
        registrations.add(context.registerService(Replicable.class,
                regattaLogSensorDataTrackerTrackedRegattaListener, replicableServiceProperties));
        new Thread(()->{
            try {
                registrations.add(context.registerService(RaceTrackingConnectivityParametersHandler.class,
                    new RaceLogConnectivityParamsHandler(racingEventServiceTracker.waitForService(0)), getDict(RaceLogConnectivityParams.TYPE)));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception trying to register race log tracking connectivity params persistence handler", e);
            }
        }, "RaceLog tracking activator registering connectivity params persistence handler").start();
        logger.log(Level.INFO, "Started "+context.getBundle().getSymbolicName());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ServiceTracker<SensorFixMapper<?, ?, ?>, SensorFixMapper<?, ?, ?>> createSensorFixMapperServiceTracker(BundleContext context) {
        return (ServiceTracker) ServiceTrackerFactory.createAndOpen(context, SensorFixMapper.class);
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
