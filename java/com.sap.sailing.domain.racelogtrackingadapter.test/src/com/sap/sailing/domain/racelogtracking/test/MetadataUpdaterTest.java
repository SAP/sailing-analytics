package com.sap.sailing.domain.racelogtracking.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.sap.sailing.domain.common.DeviceIdentifier;
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
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
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
    
    @AfterAll
    public static void closeClientSession() {
        clientSession.close();
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

    /**
     * Holds one device's database update open while another device completes its update. Session-free production
     * access must remain parallel even though explicit-session access is serialized. Latches make the overlap
     * observable without relying on MongoDB latency or a particular replica-set configuration.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testDifferentDevicesUpdateConcurrentlyWithoutSession() throws Exception {
        final MongoObjectFactoryImpl mongoOF = Mockito.mock(MongoObjectFactoryImpl.class);
        final MongoCollection<Document> collection = Mockito.mock(MongoCollection.class);
        final FindIterable<Document> emptyResult = Mockito.mock(FindIterable.class);
        Mockito.when(mongoOF.getGPSFixMetadataCollection()).thenReturn(collection);
        Mockito.when(collection.withReadConcern(ReadConcern.DEFAULT)).thenReturn(collection);
        Mockito.when(collection.withWriteConcern(WriteConcern.UNACKNOWLEDGED)).thenReturn(collection);
        Mockito.when(collection.find(Mockito.any(Bson.class))).thenReturn(emptyResult);
        final CountDownLatch firstUpdateStarted = new CountDownLatch(/* count */ 1);
        final CountDownLatch secondUpdateCompleted = new CountDownLatch(/* count */ 1);
        final CountDownLatch releaseFirstUpdate = new CountDownLatch(/* count */ 1);
        final AtomicInteger updatesStarted = new AtomicInteger();
        final AtomicBoolean firstUpdateTimedOut = new AtomicBoolean();
        Mockito.when(collection.updateOne(Mockito.any(Bson.class), Mockito.any(Bson.class), Mockito.any(UpdateOptions.class)))
                .thenAnswer(invocation -> {
                    if (updatesStarted.incrementAndGet() == 1) {
                        firstUpdateStarted.countDown();
                        firstUpdateTimedOut.set(!releaseFirstUpdate.await(/* timeout */ 30, TimeUnit.SECONDS));
                    } else {
                        secondUpdateCompleted.countDown();
                    }
                    return null;
                });
        final MockSmartphoneImeiServiceFinderFactory serviceFinderFactory = new MockSmartphoneImeiServiceFinderFactory();
        final TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceServiceFinder =
                serviceFinderFactory.createServiceFinder(DeviceIdentifierMongoHandler.class);
        final MyMetadataCollection metadataCollection = new MyMetadataCollection(mongoOF, createFixServiceFinder(serviceFinderFactory),
                deviceServiceFinder, ReadConcern.DEFAULT, WriteConcern.UNACKNOWLEDGED, /* clientSession */ null);
        final DeviceIdentifier firstDevice = new SmartphoneImeiIdentifierImpl("first");
        final DeviceIdentifier secondDevice = new SmartphoneImeiIdentifierImpl("second");
        final TimePoint now = TimePoint.now();
        final TimeRange timeRange = TimeRange.create(now, now.plus(Duration.ONE_MILLISECOND));
        try {
            metadataCollection.enqueueMetadataUpdate(firstDevice,
                    com.sap.sailing.shared.persistence.impl.MongoObjectFactoryImpl.storeDeviceId(deviceServiceFinder, firstDevice),
                    /* nrOfTotalFixes */ 1, timeRange, /* latestFix */ null);
            assertTrue(firstUpdateStarted.await(/* timeout */ 10, TimeUnit.SECONDS));
            metadataCollection.enqueueMetadataUpdate(secondDevice,
                    com.sap.sailing.shared.persistence.impl.MongoObjectFactoryImpl.storeDeviceId(deviceServiceFinder, secondDevice),
                    /* nrOfTotalFixes */ 1, timeRange, /* latestFix */ null);
            assertTrue(secondUpdateCompleted.await(/* timeout */ 10, TimeUnit.SECONDS),
                    "A different device must complete its update while the first device's update is blocked");
        } finally {
            releaseFirstUpdate.countDown();
            metadataCollection.getNumberOfFixes(firstDevice);
            metadataCollection.getNumberOfFixes(secondDevice);
        }
        assertFalse(firstUpdateTimedOut.get());
        assertEquals(/* expected */ 2, updatesStarted.get());
    }
}
