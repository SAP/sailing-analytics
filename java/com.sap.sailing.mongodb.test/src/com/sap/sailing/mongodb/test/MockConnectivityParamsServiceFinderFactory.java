package com.sap.sailing.mongodb.test;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.persistence.MongoRegattaLogStoreFactory;
import com.sap.sailing.domain.racelog.tracking.test.mock.AbstractTypeBasedServiceFinder;
import com.sap.sailing.domain.racelogtracking.impl.RaceLogConnectivityParams;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingadapter.impl.SwissTimingAdapterFactoryImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.SwissTimingTrackingConnectivityParameters;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayServiceFactory;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingReplayConnectivityParameters;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingReplayConnectivityParamsHandler;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParametersHandler;
import com.sap.sailing.domain.tractracadapter.TracTracAdapterFactory;
import com.sap.sailing.domain.tractracadapter.impl.RaceTrackingConnectivityParametersImpl;
import com.sap.sailing.domain.tractracadapter.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.tractracadapter.persistence.impl.TracTracConnectivityParamsHandler;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sailing.shared.persistence.device.impl.PlaceHolderDeviceIdentifierMongoHandler;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.security.SecurityService;

/**
 * A simplified implementation of the {@link TypeBasedServiceFinder} interface that, when the device type
 * {@link SmartphoneImeiIdentifierImpl#TYPE} is requested, returns a specific handler that was passed to this object's
 * constructor.
 * 
 * @author Fredrik Teschke
 *
 */
public class MockConnectivityParamsServiceFinderFactory implements TypeBasedServiceFinderFactory {
    Map<Class<?>, TypeBasedServiceFinder<?>> serviceFinders = new HashMap<Class<?>, TypeBasedServiceFinder<?>>();
    private RacingEventService racingEventService;

    public MockConnectivityParamsServiceFinderFactory() {
        serviceFinders.put(RaceTrackingConnectivityParametersHandler.class, new AbstractTypeBasedServiceFinder<RaceTrackingConnectivityParametersHandler>() {
            @Override
            public RaceTrackingConnectivityParametersHandler findService(String type)
                    throws NoCorrespondingServiceRegisteredException {
                final MongoObjectFactory mongoObjectFactory = racingEventService.getMongoObjectFactory();
                final DomainObjectFactory domainObjectFactory = racingEventService.getDomainObjectFactory();
                final DomainFactory swissTimingDomainFactory = new SwissTimingAdapterFactoryImpl().getOrCreateSwissTimingAdapter(domainObjectFactory.getBaseDomainFactory()).getSwissTimingDomainFactory();
                final SecurityService securityServiceMock = mock(SecurityService.class);
                switch (type) {
                case RaceTrackingConnectivityParametersImpl.TYPE:
                    return new TracTracConnectivityParamsHandler(
                            MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(mongoObjectFactory, domainObjectFactory),
                            MongoRegattaLogStoreFactory.INSTANCE.getMongoRegattaLogStore(mongoObjectFactory, domainObjectFactory),
                            TracTracAdapterFactory.INSTANCE.getOrCreateTracTracAdapter(domainObjectFactory.getBaseDomainFactory()).getTracTracDomainFactory(),
                            new MongoObjectFactoryImpl(mongoObjectFactory.getDatabase()), securityServiceMock);
                case SwissTimingTrackingConnectivityParameters.TYPE:
                    return new com.sap.sailing.domain.swisstimingadapter.persistence.impl.SwissTimingConnectivityParamsHandler(
                            MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(mongoObjectFactory, domainObjectFactory),
                            MongoRegattaLogStoreFactory.INSTANCE.getMongoRegattaLogStore(mongoObjectFactory, domainObjectFactory),
                            swissTimingDomainFactory, securityServiceMock);
                case SwissTimingReplayConnectivityParameters.TYPE:
                    return new SwissTimingReplayConnectivityParamsHandler(
                            MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(mongoObjectFactory, domainObjectFactory),
                            MongoRegattaLogStoreFactory.INSTANCE.getMongoRegattaLogStore(mongoObjectFactory, domainObjectFactory),
                            swissTimingDomainFactory,
                            SwissTimingReplayServiceFactory.INSTANCE.createSwissTimingReplayService(swissTimingDomainFactory, racingEventService), securityServiceMock);
                case RaceLogConnectivityParams.TYPE:
                    return new com.sap.sailing.domain.racelogtracking.impl.RaceLogConnectivityParamsHandler(racingEventService);
                default:
                    return null;
                }
            }

            @Override
            public Set<RaceTrackingConnectivityParametersHandler> findAllServices() {
                final Set<RaceTrackingConnectivityParametersHandler> result = new HashSet<>();
                        for (final String type : new String[] {
                                RaceTrackingConnectivityParametersImpl.TYPE,
                                SwissTimingTrackingConnectivityParameters.TYPE,
                                SwissTimingReplayConnectivityParameters.TYPE,
                                RaceLogConnectivityParams.TYPE }) {
                            result.add(findService(type));
                        }
                return result;
            }

            @Override
            public void setFallbackService(RaceTrackingConnectivityParametersHandler fallback) {
            }
        });
        serviceFinders.put(DeviceIdentifierMongoHandler.class, new AbstractTypeBasedServiceFinder<DeviceIdentifierMongoHandler>() {
            @Override
            public DeviceIdentifierMongoHandler findService(String type)
                    throws NoCorrespondingServiceRegisteredException {
                switch (type) {
                case RaceTrackingConnectivityParametersImpl.TYPE:
                    return new PlaceHolderDeviceIdentifierMongoHandler();
                default:
                    return null;
                }
            }

            @Override
            public Set<DeviceIdentifierMongoHandler> findAllServices() {
                return null;
            }

            @Override
            public void setFallbackService(DeviceIdentifierMongoHandler fallback) {
            }
        });
    }
    
    /**
     * Must be called right after the constructor invocation when this object has been used to initialize a
     * {@link RacingEventServiceImpl} object.
     */
    public void setRacingEventService(RacingEventService racingEventService) {
        this.racingEventService = racingEventService;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <ServiceT> TypeBasedServiceFinder<ServiceT> createServiceFinder(Class<ServiceT> clazz) {
        return (TypeBasedServiceFinder<ServiceT>) serviceFinders.get(clazz);
    }

}
