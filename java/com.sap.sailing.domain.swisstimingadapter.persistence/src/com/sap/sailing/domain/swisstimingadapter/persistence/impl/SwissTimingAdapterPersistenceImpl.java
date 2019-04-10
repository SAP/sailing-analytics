package com.sap.sailing.domain.swisstimingadapter.persistence.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingArchiveConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.persistence.SwissTimingAdapterPersistence;
import com.sap.sse.mongodb.MongoDBService;

public class SwissTimingAdapterPersistenceImpl implements SwissTimingAdapterPersistence {

    private final MongoDatabase database;

    private final SwissTimingFactory swissTimingFactory;

    private static final Logger logger = Logger.getLogger(SwissTimingAdapterPersistenceImpl.class.getName());
    
    public SwissTimingAdapterPersistenceImpl(MongoDBService mongoDBService, SwissTimingFactory swissTimingFactory) {
        super();
        this.database = mongoDBService.getDB();
        this.swissTimingFactory = swissTimingFactory;
    }

    @Override
    public Iterable<SwissTimingConfiguration> getSwissTimingConfigurations() {
        List<SwissTimingConfiguration> result = new ArrayList<SwissTimingConfiguration>();
        try {
            MongoCollection<org.bson.Document> stConfigs = database.getCollection(CollectionNames.SWISSTIMING_CONFIGURATIONS.name());
            for (Document o : stConfigs.find()) {
                SwissTimingConfiguration stConfig = loadSwissTimingConfiguration(o);
                // the old SwissTiming configuration was not based on a JSON URL -> ignore such configurations
                if (stConfig.getJsonURL() != null) {
                    result.add(stConfig);
                }
            }
        } catch (Exception e) {
            // something went wrong during DB access; report, then use empty new wind track
            logger.log(Level.SEVERE,
                    "Error connecting to MongoDB, unable to load recorded SwissTiming configurations. Check MongoDB settings.");
            logger.throwing(SwissTimingAdapterPersistenceImpl.class.getName(), "getSwissTimingConfigurations", e);
        }
        return result;
    }

    private SwissTimingConfiguration loadSwissTimingConfiguration(Document object) {
        String name = (String) object.get(FieldNames.ST_CONFIG_NAME.name());
        String jsonURL = (String) object.get(FieldNames.ST_CONFIG_JSON_URL.name());
        String hostname = (String) object.get(FieldNames.ST_CONFIG_HOSTNAME.name());
        Integer port = (Integer) object.get(FieldNames.ST_CONFIG_PORT.name());
        String updateURL = (String) object.get(FieldNames.ST_CONFIG_UPDATE_URL.name());
        String updateUsername = (String) object.get(FieldNames.ST_CONFIG_UPDATE_USERNAME.name());
        String updatePassword = (String) object.get(FieldNames.ST_CONFIG_UPDATE_PASSWORD.name());
        String creatorName = object.getString(FieldNames.ST_CONFIG_CREATOR_NAME.name());
        return swissTimingFactory.createSwissTimingConfiguration(name, jsonURL, hostname, port, updateURL,
                updateUsername, updatePassword, creatorName);
    }

    @Override
    public Iterable<SwissTimingArchiveConfiguration> getSwissTimingArchiveConfigurations() {
        List<SwissTimingArchiveConfiguration> result = new ArrayList<SwissTimingArchiveConfiguration>();
        try {
            MongoCollection<org.bson.Document> stConfigs = database.getCollection(CollectionNames.SWISSTIMING_ARCHIVE_CONFIGURATIONS.name());
            for (Document o : stConfigs.find()) {
                SwissTimingArchiveConfiguration stConfig = loadSwissTimingArchiveConfiguration(o);
                result.add(stConfig);
            }
        } catch (Exception e) {
            // something went wrong during DB access; report, then use empty new wind track
            logger.log(Level.SEVERE,
                    "Error connecting to MongoDB, unable to load recorded SwissTiming archive configurations. Check MongoDB settings.");
            logger.throwing(SwissTimingAdapterPersistenceImpl.class.getName(), "getSwissTimingArchiveConfigurations", e);
        }
        return result;
    }

    private SwissTimingArchiveConfiguration loadSwissTimingArchiveConfiguration(Document object) {
        final String jsonUrl = object.getString(FieldNames.ST_ARCHIVE_JSON_URL.name());
        String creatorName = object.getString(FieldNames.ST_ARCHIVE_CREATOR_NAME.name());
        
        final boolean needsUpdate = (creatorName == null);
        if (needsUpdate) {
            // No creator is set yet -> existing configurations are assumed to belong to the admin
            creatorName = "admin";
        }
        
        final SwissTimingArchiveConfiguration loadedSwissTimingArchiveConfiguration = swissTimingFactory.createSwissTimingArchiveConfiguration(
                jsonUrl,
                creatorName);
        
        if (needsUpdate) {
            // recreating the config on the DB because the composite key changed
            deleteSwissTimingArchiveConfiguration(
                    swissTimingFactory.createSwissTimingArchiveConfiguration(jsonUrl, null));
            createSwissTimingArchiveConfiguration(loadedSwissTimingArchiveConfiguration);
        }
        return loadedSwissTimingArchiveConfiguration;
    }

    @Override
    public void createSwissTimingConfiguration(SwissTimingConfiguration swissTimingConfiguration) {
        MongoCollection<org.bson.Document> stConfigCollection = database.getCollection(CollectionNames.SWISSTIMING_CONFIGURATIONS.name());
        Document result = storeSwissTimingConfiguration(swissTimingConfiguration);
        final Document updateQuery = new Document(FieldNames.ST_CONFIG_JSON_URL.name(),
                swissTimingConfiguration.getJsonURL());
        stConfigCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(updateQuery, result,
                new UpdateOptions().upsert(true));
    }

    private Document storeSwissTimingConfiguration(SwissTimingConfiguration swissTimingConfiguration) {
        Document result = new Document();
        result.put(FieldNames.ST_CONFIG_NAME.name(), swissTimingConfiguration.getName());
        result.put(FieldNames.ST_CONFIG_JSON_URL.name(), swissTimingConfiguration.getJsonURL());
        result.put(FieldNames.ST_CONFIG_HOSTNAME.name(), swissTimingConfiguration.getHostname());
        result.put(FieldNames.ST_CONFIG_PORT.name(), swissTimingConfiguration.getPort());
        result.put(FieldNames.ST_CONFIG_UPDATE_URL.name(), swissTimingConfiguration.getUpdateURL());
        result.put(FieldNames.ST_CONFIG_UPDATE_USERNAME.name(), swissTimingConfiguration.getUpdateUsername());
        result.put(FieldNames.ST_CONFIG_UPDATE_PASSWORD.name(), swissTimingConfiguration.getUpdatePassword());
        result.put(FieldNames.ST_CONFIG_CREATOR_NAME.name(), swissTimingConfiguration.getCreatorName());
        return result;
    }

    @Override
    public void createSwissTimingArchiveConfiguration(
            SwissTimingArchiveConfiguration config) {
        MongoCollection<org.bson.Document> stArchiveConfigCollection = database.getCollection(CollectionNames.SWISSTIMING_ARCHIVE_CONFIGURATIONS.name());
        Document result = storeSwissTimingArchiveConfiguration(config);
        stArchiveConfigCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).insertOne(result);
    }

    @Override
    public void updateSwissTimingArchiveConfiguration(
            SwissTimingArchiveConfiguration config) {
        MongoCollection<org.bson.Document> stArchiveConfigCollection = database
                .getCollection(CollectionNames.SWISSTIMING_ARCHIVE_CONFIGURATIONS.name());
        Document result = storeSwissTimingArchiveConfiguration(config);
        stArchiveConfigCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(result, result,
                new UpdateOptions().upsert(true));
    }

    private Document storeSwissTimingArchiveConfiguration(SwissTimingArchiveConfiguration config) {
        Document result = new Document();
        result.put(FieldNames.ST_ARCHIVE_JSON_URL.name(), config.getJsonURL());
        result.put(FieldNames.ST_ARCHIVE_CREATOR_NAME.name(), config.getCreatorName());
        return result;
    }

    @Override
    public void deleteSwissTimingArchiveConfiguration(
            SwissTimingArchiveConfiguration config) {
        MongoCollection<org.bson.Document> stArchiveConfigCollection = database
                .getCollection(CollectionNames.SWISSTIMING_ARCHIVE_CONFIGURATIONS.name());
        Document result = storeSwissTimingArchiveConfiguration(config);
        stArchiveConfigCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).deleteOne(result);
    }

    @Override
    public void deleteSwissTimingConfiguration(String creatorName, String jsonURL) {
        MongoCollection<org.bson.Document> stArchiveConfigCollection = database
                .getCollection(CollectionNames.SWISSTIMING_ARCHIVE_CONFIGURATIONS.name());
        Document result = new Document();
        result.put(FieldNames.ST_CONFIG_JSON_URL.name(), jsonURL);
        result.put(FieldNames.ST_CONFIG_CREATOR_NAME.name(), creatorName);
        stArchiveConfigCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).deleteOne(result);
    }

    @Override
    public void updateSwissTimingConfiguration(SwissTimingConfiguration config) {
        MongoCollection<org.bson.Document> stArchiveConfigCollection = database
                .getCollection(CollectionNames.SWISSTIMING_CONFIGURATIONS.name());
        Document result = storeSwissTimingConfiguration(config);
        stArchiveConfigCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(result, result,
                new UpdateOptions().upsert(true));
    }
}
