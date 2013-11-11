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
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.racelog.RaceLogEventFactory;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;

public class TestStoringAndRetrievingRaceLogEventData extends AbstractMongoDBTest {
    private static final String RACELOG_TEST_COLLECTION = "racelog_test_collection";

    public TestStoringAndRetrievingRaceLogEventData() throws UnknownHostException, MongoException {
        super();
    }

    @Before
    @Override
    public void dropTestDB() throws UnknownHostException, MongoException, InterruptedException {
        super.dropTestDB();
        db.getCollection(RACELOG_TEST_COLLECTION).drop();
    }

    @Test
    public void testDBConnection() throws UnknownHostException, MongoException {
        DBCollection coll = db.getCollection(RACELOG_TEST_COLLECTION);
        assertNotNull(coll);
        BasicDBObject doc = new BasicDBObject();
        doc.put("upperflag", "AP");
        doc.put("lowerflag", "ALPHA");
        coll.insert(doc);
    }

    @Test
    public void testDBRead() throws UnknownHostException, MongoException, InterruptedException {
        {
            DBCollection coll = db.getCollection(RACELOG_TEST_COLLECTION);
            assertNotNull(coll);
            BasicDBObject doc = new BasicDBObject();
            doc.put("upperflag", "AP");
            doc.put("lowerflag", "ALPHA");
            coll.insert(doc);
        }

        {
            DBCollection coll = db.getCollection(RACELOG_TEST_COLLECTION);
            assertNotNull(coll);
            DBObject object = coll.findOne();
            assertEquals("AP", object.get("upperflag"));
            assertEquals("ALPHA", object.get("lowerflag"));
        }
    }

    @Test
    public void storeRaceLogFlagEvent() throws UnknownHostException, MongoException, InterruptedException {
        TimePoint now = MillisecondsTimePoint.now();
        RaceLogFlagEvent rcEvent = RaceLogEventFactory.INSTANCE.createFlagEvent(now, 0, Flags.AP, Flags.ALPHA, true);
        {
            DBObject rcEventForMongo = ((MongoObjectFactoryImpl) PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory())
                    .storeRaceLogFlagEvent(rcEvent);
            DBCollection coll = db.getCollection(RACELOG_TEST_COLLECTION);
            coll.insert(rcEventForMongo);
        }

        {
            DBCollection coll = db.getCollection(RACELOG_TEST_COLLECTION);
            assertNotNull(coll);
            DBObject object = coll.findOne();
            RaceLogFlagEvent readRcEvent = (RaceLogFlagEvent) ((DomainObjectFactoryImpl) PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory()).loadRaceLogEvent(object);
            assertEquals(rcEvent.getTimePoint(), readRcEvent.getTimePoint());
            assertEquals(rcEvent.getId(), readRcEvent.getId());
            assertEquals(rcEvent.getPassId(), readRcEvent.getPassId());
            assertEquals(rcEvent.getUpperFlag(), readRcEvent.getUpperFlag());
            assertEquals(rcEvent.getLowerFlag(), readRcEvent.getLowerFlag());
        }
    }

}
