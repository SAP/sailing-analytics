package com.sap.sailing.domain.queclinkadapter.impl;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import com.sap.sse.mongodb.MongoDBService;

public class AbstractQueclinkTrackerTest {
    protected final MockSmartphoneImeiServiceFinderFactory serviceFinderFactory = new MockSmartphoneImeiServiceFinderFactory();
    protected final DeviceIdentifier deviceIdentifier = new SmartphoneImeiIdentifierImpl("860599002480051");
    protected SensorFixStore store;
    private static ClientSession clientSession;

    @BeforeAll
    public static void setUpClass() {
        clientSession = MongoDBService.INSTANCE.startCausallyConsistentSession();
    }
    
    @BeforeEach
    public void setUp() throws MongoException, IOException {
        dropPersistedData();
        newStore();
    }

    private void newStore() {
        store = new MongoSensorFixStoreImpl(PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(), serviceFinderFactory, ReadConcern.MAJORITY,
                WriteConcern.MAJORITY, clientSession, clientSession);
    }

    @AfterEach
    public void after() throws IOException {
        dropPersistedData();
    }

    private void dropPersistedData() {
        MongoDatabase db = PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().getDatabase();
        db.getCollection(CollectionNames.GPS_FIXES.name()).withWriteConcern(WriteConcern.MAJORITY).drop(clientSession);
        db.getCollection(CollectionNames.GPS_FIXES_METADATA.name()).withWriteConcern(WriteConcern.MAJORITY).drop(clientSession);
    }
}
