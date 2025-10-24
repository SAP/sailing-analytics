package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.CollectionNames;
import com.sap.sse.common.Util;

public class StoreAndLoadBoatsTest extends AbstractMongoDBTest {
    private DomainFactory domainFactory;
    private final static BoatClass boatClass505 = new BoatClassImpl("505", /* typicallyStartsUpwind */ true);
    
    public StoreAndLoadBoatsTest() throws UnknownHostException, MongoException {
        super();
    }

    @BeforeEach
    public void setUp() {
        // clear the domainFactory competitor store for a clean start:
        domainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
    }
    
    private void dropBoatCollection() {
        MongoDatabase db = getMongoService().getDB();
        MongoCollection<org.bson.Document> boatCollection = db.getCollection(CollectionNames.BOATS.name());
        boatCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).drop();
    }
    
    @Test
    public void testStoreAndUpdateBoat() throws URISyntaxException {
        String sailID1 = "GER 1234";
        String sailID2 = "GER 12345";
        String boatName1 = "Hassos Boot";
        String boatName2 = "Hasso Plattners Boot";

        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropBoatCollection();
        
        DynamicBoat boat = (DynamicBoat) new BoatImpl("id12345", boatName1, boatClass505, sailID1); 
        mongoObjectFactory.storeBoat(boat);
        
        Collection<DynamicBoat> allBoats = domainObjectFactory.loadAllBoats();
        assertEquals(1, Util.size(allBoats));
        DynamicBoat loadedBoat = (DynamicBoat) allBoats.iterator().next();
        assertEquals(sailID1, loadedBoat.getSailID());
        assertEquals(boatName1, loadedBoat.getName());
        
        loadedBoat.setName(boatName2);
        loadedBoat.setSailId(sailID2);
        mongoObjectFactory.storeBoat(loadedBoat);

        allBoats = domainObjectFactory.loadAllBoats();
        assertEquals(1, Util.size(allBoats));
        loadedBoat = (DynamicBoat) allBoats.iterator().next();
        assertEquals(sailID2, loadedBoat.getSailID());
        assertEquals(boatName2, loadedBoat.getName());
    }

    @Test
    public void testStoreAndUpdateBoatWithUUIDAsId() {
        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropBoatCollection();

        DynamicBoat boat = (DynamicBoat) new BoatImpl(UUID.randomUUID(), "Hasso", boatClass505, "GER 1234");
        mongoObjectFactory.storeBoat(boat);
        assertEquals(1, Util.size(domainObjectFactory.loadAllBoats()));
        boat.setName("Hasso Plattner");
        mongoObjectFactory.storeBoat(boat);
        assertEquals(1, Util.size(domainObjectFactory.loadAllBoats()));
    }

    @Test
    public void testStoreAndRemoveBoat() {
        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropBoatCollection();
        
        DynamicBoat boat = (DynamicBoat) new BoatImpl("id12345", "Hassos boot", boatClass505, "GER 1234"); 
        mongoObjectFactory.storeBoat(boat);
        assertEquals(1, Util.size(domainObjectFactory.loadAllBoats()));

        mongoObjectFactory.removeBoat(boat);
        assertEquals(0, Util.size(domainObjectFactory.loadAllBoats()));
    }

    @Test
    public void testStoreAndRemoveCompetitorWithUUIDAsId() {
        MongoObjectFactory mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        DomainObjectFactory domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), domainFactory);
        dropBoatCollection();
        
        DynamicBoat boat = (DynamicBoat) new BoatImpl(UUID.randomUUID(), "Hassos boot", boatClass505, "GER 1234"); 
        mongoObjectFactory.storeBoat(boat);
        assertEquals(1, Util.size(domainObjectFactory.loadAllBoats()));

        mongoObjectFactory.removeBoat(boat);
        assertEquals(0, Util.size(domainObjectFactory.loadAllBoats()));
    }
}
