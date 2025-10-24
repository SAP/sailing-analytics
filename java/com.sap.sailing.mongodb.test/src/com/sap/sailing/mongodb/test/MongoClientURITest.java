package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.mongodb.ReadPreference;
import com.sap.sse.mongodb.MongoDBConfiguration;

public class MongoClientURITest {
    @Test
    public void testDefaultConnectionOptions() {
        MongoDBConfiguration config = new MongoDBConfiguration("mongodb://humba:12345/mydb?readPreference=primaryPreferred");
        assertEquals("humba", config.getHostname());
        assertEquals(12345, config.getPort());
        assertEquals("mydb", config.getDatabaseName());
        assertSame(ReadPreference.primaryPreferred(), config.getMongoClientURI().getReadPreference()); 
    }

    @Test
    public void testSpecificConnectionOptions() {
        MongoDBConfiguration config = new MongoDBConfiguration("mongodb://humba:12345/mydb?replicaset=rs0&readpreference=primary");
        assertEquals("humba", config.getHostname());
        assertEquals(12345, config.getPort());
        assertEquals("mydb", config.getDatabaseName());
        assertSame(ReadPreference.primary(), config.getMongoClientURI().getReadPreference()); 
        assertEquals("rs0", config.getMongoClientURI().getRequiredReplicaSetName());
    }
}
