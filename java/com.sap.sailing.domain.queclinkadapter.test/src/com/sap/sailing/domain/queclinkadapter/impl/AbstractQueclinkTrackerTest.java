package com.sap.sailing.domain.queclinkadapter.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.mongodb.MongoException;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.CollectionNames;
import com.sap.sailing.domain.persistence.racelog.tracking.impl.MongoSensorFixStoreImpl;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.shared.util.Wait;

public class AbstractQueclinkTrackerTest {
    protected final MockSmartphoneImeiServiceFinderFactory serviceFinderFactory = new MockSmartphoneImeiServiceFinderFactory();
    protected final DeviceIdentifier deviceIdentifier = new SmartphoneImeiIdentifierImpl("860599002480051");
    protected SensorFixStore store;
    private ClientSession clientSession;
    
    @BeforeEach
    public void setUp() throws MongoException, IOException {
        clientSession = MongoDBService.INSTANCE.startCausallyConsistentSession();
        dropPersistedData();
        newStore();
    }

    private void newStore() {
        store = new MongoSensorFixStoreImpl(PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(), serviceFinderFactory, ReadConcern.MAJORITY,
                WriteConcern.MAJORITY, clientSession, clientSession);
    }

    /**
     * Waits for asynchronous socket ingestion and metadata updates instead of assuming a fixed database latency.
     * Majority writes through an explicit session may take longer on a replica set than on a standalone server.
     */
    protected void assertNumberOfFixesEventually(final long expected) throws Exception {
        final long actual = Wait.wait(() -> store.getNumberOfFixes(deviceIdentifier), count -> count >= expected,
                /* retryOnException */ false, Optional.of(Duration.ONE_MINUTE), Duration.ONE_SECOND,
                /* logLevel */ null, /* retryLogMessage */ null);
        assertEquals(expected, actual);
    }

    /**
     * Completes asynchronous metadata writes before dropping their collection and closing the test's session.
     */
    @AfterEach
    public void after() throws IOException {
        try {
            if (store != null) {
                assertDoesNotThrow(() -> store.getNumberOfFixes(deviceIdentifier));
            }
            dropPersistedData();
        } finally {
            clientSession.close();
        }
    }

    private void dropPersistedData() {
        final MongoDatabase db = PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().getDatabase();
        db.getCollection(CollectionNames.GPS_FIXES.name()).withWriteConcern(WriteConcern.MAJORITY).drop(clientSession);
        db.getCollection(CollectionNames.GPS_FIXES_METADATA.name()).withWriteConcern(WriteConcern.MAJORITY).drop(clientSession);
    }
}
