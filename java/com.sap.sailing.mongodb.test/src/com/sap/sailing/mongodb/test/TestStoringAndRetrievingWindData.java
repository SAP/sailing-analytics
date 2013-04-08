package com.sap.sailing.mongodb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.persistence.MongoFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.impl.WindImpl;

public class TestStoringAndRetrievingWindData extends AbstractMongoDBTest {
    private static final String WIND_TEST_COLLECTION = "wind_test_collection";
    
    public TestStoringAndRetrievingWindData() throws UnknownHostException, MongoException {
        super();
    }

    @Before
    @Override
    public void dropTestDB() throws UnknownHostException, MongoException, InterruptedException {
        super.dropTestDB();
        db.getCollection(WIND_TEST_COLLECTION).drop();
    }
    
    @Test
    public void testDBConnection() throws UnknownHostException, MongoException {
        DBCollection coll = db.getCollection(WIND_TEST_COLLECTION);
        assertNotNull(coll);
        BasicDBObject doc = new BasicDBObject();
        doc.put("truebearingdeg", 234.3);
        doc.put("knotspeed", 10.7);
        coll.insert(doc);
    }

    @Test
    public void testDBRead() throws UnknownHostException, MongoException, InterruptedException {
        {
            DBCollection coll = db.getCollection(WIND_TEST_COLLECTION);
            assertNotNull(coll);
            BasicDBObject doc = new BasicDBObject();
            doc.put("truebearingdeg", 234.3);
            doc.put("knotspeed", 10.7);
            coll.insert(doc);
        }

        {
            DBCollection coll = db.getCollection(WIND_TEST_COLLECTION);
            assertNotNull(coll);
            DBObject object = coll.findOne();
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
            DBObject windForMongo = ((MongoObjectFactoryImpl) MongoFactory.INSTANCE.getDefaultMongoObjectFactory()).storeWind(wind);
            DBCollection coll = db.getCollection(WIND_TEST_COLLECTION);
            coll.insert(windForMongo);
        }
        
        {
            DBCollection coll = db.getCollection(WIND_TEST_COLLECTION);
            assertNotNull(coll);
            DBObject object = coll.findOne();
            Wind readWind = ((DomainObjectFactoryImpl) MongoFactory.INSTANCE.getDefaultDomainObjectFactory()).loadWind(object);
            assertEquals(wind.getPosition(), readWind.getPosition());
            assertEquals(wind.getKnots(), readWind.getKnots(), 0.00000001);
            assertEquals(wind.getBearing().getDegrees(), readWind.getBearing().getDegrees(), 0.00000001);
        }
    }
}
