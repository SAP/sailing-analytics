package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.UnknownHostException;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TestStoringAndRetrievingWindData extends AbstractMongoDBTest {
    private static final String WIND_TEST_COLLECTION = "wind_test_collection";
    
    public TestStoringAndRetrievingWindData() throws UnknownHostException, MongoException {
        super();
    }

    @BeforeEach
    @Override
    public void dropTestDB() throws UnknownHostException, MongoException, InterruptedException {
        super.dropTestDB();
        db.getCollection(WIND_TEST_COLLECTION).drop();
    }
    
    @Test
    public void testDBConnection() throws UnknownHostException, MongoException {
        MongoCollection<Document> coll = db.getCollection(WIND_TEST_COLLECTION);
        assertNotNull(coll);
        Document doc = new Document();
        doc.put("truebearingdeg", 234.3);
        doc.put("knotspeed", 10.7);
        coll.insertOne(doc);
    }

    @Test
    public void testDBRead() throws UnknownHostException, MongoException, InterruptedException {
        {
            MongoCollection<Document> coll = db.getCollection(WIND_TEST_COLLECTION);
            assertNotNull(coll);
            Document doc = new Document();
            doc.put("truebearingdeg", 234.3);
            doc.put("knotspeed", 10.7);
            coll.insertOne(doc);
        }

        {
            MongoCollection<Document> coll = db.getCollection(WIND_TEST_COLLECTION);
            assertNotNull(coll);
            Document object = coll.find().first();
            assertEquals(234.3, object.get("truebearingdeg"));
            assertEquals(10.7, object.get("knotspeed"));
        }
    }
    
    @Test
    public void storeWindObject() throws UnknownHostException, MongoException, InterruptedException {
        TimePoint now = MillisecondsTimePoint.now();
        Wind wind = new WindImpl(new DegreePosition(123, 45), now, new KnotSpeedWithBearingImpl(10.4,
                new DegreeBearingImpl(355.5)));
        {
            Document windForMongo = ((MongoObjectFactoryImpl) PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory()).storeWind(wind);
            MongoCollection<Document> coll = db.getCollection(WIND_TEST_COLLECTION);
            coll.insertOne(windForMongo);
        }
        
        {
            MongoCollection<Document> coll = db.getCollection(WIND_TEST_COLLECTION);
            assertNotNull(coll);
            Document object = coll.find().first();
            Wind readWind = ((DomainObjectFactoryImpl) PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory()).loadWind(object);
            assertEquals(wind.getPosition(), readWind.getPosition());
            assertEquals(wind.getKnots(), readWind.getKnots(), 0.00000001);
            assertEquals(wind.getBearing().getDegrees(), readWind.getBearing().getDegrees(), 0.00000001);
        }
    }
}
