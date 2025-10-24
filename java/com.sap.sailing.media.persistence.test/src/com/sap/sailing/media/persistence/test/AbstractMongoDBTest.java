package com.sap.sailing.media.persistence.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;

public abstract class AbstractMongoDBTest {
    protected final MongoClient mongo;
    protected final MongoDatabase db;
    private final MongoDBConfiguration dbConfiguration;
    private MongoDBService service;
    
    public AbstractMongoDBTest() throws UnknownHostException, MongoException {
        dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
        service = getDBConfiguration().getService();
        mongo = newMongo();
        db = service.getDB();
    }
    
    protected MongoDBConfiguration getDBConfiguration() {
        return dbConfiguration;
    }
    
    protected MongoClient newMongo() throws UnknownHostException, MongoException {
        return MongoClients.create(MongoClientSettings.builder().applyToClusterSettings(
                clusterSettings->clusterSettings.hosts(Arrays.asList(
                        new ServerAddress(System.getProperty("mongo.host", "127.0.0.1"), dbConfiguration.getPort())))).build());
    }
    
    @BeforeEach
    public void dropTestDB() throws UnknownHostException, MongoException, InterruptedException {
        assertNotNull(mongo);
        dropAllCollections(db);
        assertNotNull(db);
    }

    private void dropAllCollections(MongoDatabase theDB) throws InterruptedException {
        db.drop();
    }

    protected MongoDBService getMongoService() {
        return service;
    }
}
