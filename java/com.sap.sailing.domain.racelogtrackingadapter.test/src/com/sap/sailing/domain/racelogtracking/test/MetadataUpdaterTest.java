package com.sap.sailing.domain.racelogtracking.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.CollectionNames;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.racelog.tracking.FixMongoHandler;
import com.sap.sailing.domain.persistence.racelog.tracking.impl.MetadataCollection;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.mongodb.MongoDBService;

public class MetadataUpdaterTest {
    private static ClientSession clientSession;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static TypeBasedServiceFinder<FixMongoHandler<?>> createFixServiceFinder(TypeBasedServiceFinderFactory serviceFinderFactory) {
        return (TypeBasedServiceFinder) serviceFinderFactory.createServiceFinder(FixMongoHandler.class);
    }
    
    @BeforeAll
    public static void createClientSession() {
        clientSession = MongoDBService.INSTANCE.startCausallyConsistentSession();
    }
    
    @BeforeEach
    public void dropData() {
        MongoDatabase db = PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().getDatabase();
        db.getCollection(CollectionNames.GPS_FIXES.name()).withWriteConcern(WriteConcern.MAJORITY).drop(clientSession);
        db.getCollection(CollectionNames.GPS_FIXES_METADATA.name()).withWriteConcern(WriteConcern.MAJORITY).drop(clientSession);
    }
    
    private static class MyMetadataCollection extends MetadataCollection {
        public MyMetadataCollection(MongoObjectFactoryImpl mongoOF,
                TypeBasedServiceFinder<FixMongoHandler<?>> fixServiceFinder,
                TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceServiceFinder, ReadConcern readConcern,
                WriteConcern writeConcern, ClientSession clientSession) {
            super(mongoOF, fixServiceFinder, deviceServiceFinder, readConcern, writeConcern, clientSession);
        }

        @Override
        public synchronized <FixT extends Timed> void enqueueMetadataUpdate(DeviceIdentifier device, final Object dbDeviceId,
                final int nrOfTotalFixes, TimeRange fixesTimeRange, FixT latestFix) throws TransformationException {
            super.enqueueMetadataUpdate(device, dbDeviceId, nrOfTotalFixes, fixesTimeRange, latestFix);
        }

        @Override
        public long getNumberOfFixes(DeviceIdentifier device)
                throws TransformationException, NoCorrespondingServiceRegisteredException {
            return super.getNumberOfFixes(device);
        }
    }
    
    @Test
    public void testSynchronization() throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        final MockSmartphoneImeiServiceFinderFactory serviceFinderFactory = new MockSmartphoneImeiServiceFinderFactory();
        final DeviceIdentifier deviceIdentifier = new SmartphoneImeiIdentifierImpl(UUID.randomUUID().toString());
        final ClientSession clientSession = MongoDBService.INSTANCE.startCausallyConsistentSession();
        MongoObjectFactoryImpl mongoOF = (MongoObjectFactoryImpl) PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory();
        TypeBasedServiceFinder<FixMongoHandler<?>> fixServiceFinder = createFixServiceFinder(serviceFinderFactory);
        TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceServiceFinder = serviceFinderFactory.createServiceFinder(DeviceIdentifierMongoHandler.class);
        MyMetadataCollection metadataCollection = new MyMetadataCollection(mongoOF, fixServiceFinder, deviceServiceFinder, ReadConcern.MAJORITY, WriteConcern.MAJORITY, clientSession);
        Object dbDeviceId = com.sap.sailing.shared.persistence.impl.MongoObjectFactoryImpl.storeDeviceId(deviceServiceFinder, deviceIdentifier);
        final GPSFixMoving latestFix1 = new GPSFixMovingImpl(new DegreePosition(1, 2), TimePoint.now(), new KnotSpeedWithBearingImpl(6, new DegreeBearingImpl(123)), new DegreeBearingImpl(123));
        final TimeRange fixesTimeRange1 = TimeRange.create(latestFix1.getTimePoint(), latestFix1.getTimePoint().plus(Duration.ONE_MILLISECOND));
        metadataCollection.enqueueMetadataUpdate(deviceIdentifier, dbDeviceId, 1, fixesTimeRange1, latestFix1);
        Thread.sleep(5); // ensure we get a different time point
        final GPSFixMoving latestFix2 = new GPSFixMovingImpl(new DegreePosition(1, 2.0000001), TimePoint.now(), new KnotSpeedWithBearingImpl(6.1, new DegreeBearingImpl(123.1)), new DegreeBearingImpl(123.1));
        final TimeRange fixesTimeRange2 = TimeRange.create(latestFix2.getTimePoint(), latestFix2.getTimePoint().plus(Duration.ONE_MILLISECOND));
        metadataCollection.enqueueMetadataUpdate(deviceIdentifier, dbDeviceId, 1, fixesTimeRange2, latestFix2);
        assertEquals(2, metadataCollection.getNumberOfFixes(deviceIdentifier));
    }
}
