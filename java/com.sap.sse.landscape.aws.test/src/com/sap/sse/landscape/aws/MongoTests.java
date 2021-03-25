package com.sap.sse.landscape.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.Random;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.mongodb.client.MongoDatabase;
import com.sap.sse.common.Duration;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.aws.orchestration.CopyAndCompareMongoDatabase;
import com.sap.sse.landscape.mongodb.MongoProcess;
import com.sap.sse.landscape.mongodb.impl.DatabaseImpl;
import com.sap.sse.landscape.mongodb.impl.MongoProcessImpl;
import com.sap.sse.landscape.mongodb.impl.MongoProcessInReplicaSetImpl;
import com.sap.sse.landscape.mongodb.impl.MongoReplicaSetImpl;

public class MongoTests {
    private static final Optional<Duration> optionalTimeout = Optional.of(Duration.ONE_MINUTE.times(5));

    private Host localhost;
    private MongoProcessImpl mongoProcess;
    private MongoProcessInReplicaSetImpl mongoProcessInReplicaSet;
    private MongoReplicaSetImpl mongoReplicaSet;
    
    @Before
    public void setUp() throws UnknownHostException {
        localhost = Mockito.mock(Host.class);
        Mockito.when(localhost.getPrivateAddress()).thenReturn(InetAddress.getByName("127.0.0.1"));
        mongoProcess = new MongoProcessImpl(localhost);
        mongoReplicaSet = new MongoReplicaSetImpl("rs0");
        mongoProcessInReplicaSet = new MongoProcessInReplicaSetImpl(mongoReplicaSet, 10222, localhost);
        mongoReplicaSet.addReplica(mongoProcessInReplicaSet);
    }
    
    @Test
    public void testMongoReadiness() {
        assertTrue(mongoProcess.isReady(optionalTimeout));
    }
    
    @Test
    public void testMongoProcessInReplicaSetIsAvailable() {
        assertTrue(mongoProcessInReplicaSet.isReady(optionalTimeout));
    }
    
    @Test
    public void testMongoProcessInReplicaSetCanReportPriority() throws URISyntaxException {
        assertTrue(mongoProcessInReplicaSet.isInReplicaSet());
    }
    
    @Test
    public void testMd5() throws URISyntaxException {
        final String hash = mongoReplicaSet.getMD5Hash("local");
        assertNotNull(hash);
        assertTrue(!hash.isEmpty());
    }

    @Test
    public void testImport() throws URISyntaxException {
        final String dbName = "importtest"+new Random().nextInt();
        try { 
            final MongoDatabase exportFrom = mongoReplicaSet.getMongoDatabase(dbName);
            exportFrom.drop();
            mongoProcess.getMongoDatabase(dbName).drop();
            exportFrom.getCollection("C1").insertOne(new Document("a", "b"));
            exportFrom.getCollection("C2").insertOne(new Document("c", "d"));
            exportFrom.getCollection("C2").insertOne(new Document("e", "f"));
            exportFrom.createCollection("C3"); // force an empty C3 collection
            mongoProcess.importDatabase(exportFrom);
            final String hashExport = mongoReplicaSet.getMD5Hash(dbName);
            final String hashImport = mongoProcess.getMD5Hash(dbName);
            assertNotNull(hashExport);
            assertEquals(hashExport, hashImport);
        } finally {
            mongoProcess.getMongoDatabase(dbName).drop();
            mongoReplicaSet.getMongoDatabase(dbName).drop();
        }
    }
    
    @Test
    public void testDatabaseArchiving() throws Exception {
        final String dbName = "importtest"+new Random().nextInt();
        try { 
            final MongoDatabase exportFrom = mongoReplicaSet.getMongoDatabase(dbName);
            exportFrom.drop();
            mongoProcess.getMongoDatabase(dbName).drop();
            exportFrom.getCollection("C1").insertOne(new Document("a", "b"));
            exportFrom.getCollection("C2").insertOne(new Document("c", "d"));
            exportFrom.getCollection("C2").insertOne(new Document("e", "f"));
            exportFrom.createCollection("C3"); // force an empty C3 collection
        CopyAndCompareMongoDatabase.builder()
                .setSourceDatabase(new DatabaseImpl(mongoReplicaSet, dbName))
                .setTargetDatabase(new DatabaseImpl(mongoProcess, dbName))
                .build().run();
        } finally {
            mongoProcess.getMongoDatabase(dbName).drop();
            mongoReplicaSet.getMongoDatabase(dbName).drop();
        }
    }

    /**
     * An SSH tunnel locally bound to port 10202 to dbserver.internal.sapsailing.com:10202 is required for
     * this test.
     */
    @Test
    public void testImportSCL2018StPetersburgThroughTunnel() throws URISyntaxException {
        final String dbName = "scl2018-sanktpetersburg";
        try { 
            final MongoProcess archive = new MongoProcessImpl(localhost, 10202);
            final MongoDatabase exportFrom = archive.getMongoDatabase(dbName);
            mongoProcess.getMongoDatabase(dbName).drop();
            mongoProcess.importDatabase(exportFrom);
            final String hashExport = archive.getMD5Hash(dbName);
            final String hashImport = mongoProcess.getMD5Hash(dbName);
            assertNotNull(hashExport);
            assertEquals(hashExport, hashImport);
        } finally {
            mongoProcess.getMongoDatabase(dbName).drop();
        }
    }
}
