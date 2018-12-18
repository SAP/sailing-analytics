package com.sap.sailing.domain.tractracadapter.persistence.impl;

import org.bson.Document;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.sap.sailing.domain.tractracadapter.TracTracConfiguration;
import com.sap.sailing.domain.tractracadapter.persistence.MongoObjectFactory;

public class MongoObjectFactoryImpl implements MongoObjectFactory {
    private final MongoDatabase database;
    
    public MongoObjectFactoryImpl(MongoDatabase database) {
        super();
        this.database = database;
    }

    @Override
    public void storeTracTracConfiguration(TracTracConfiguration tracTracConfiguration) {
        MongoCollection<Document> ttConfigCollection = database.getCollection(CollectionNames.TRACTRAC_CONFIGURATIONS.name());
        // remove old, non working index
        dropIndexSafe(ttConfigCollection, "TRACTRAC_CONFIGURATIONS_1", "tt_config_name_unique");
        // adding unique index by JSON URL
        ttConfigCollection.createIndex(new Document(FieldNames.TT_CONFIG_JSON_URL.name(), 1),
                new IndexOptions().name("tt_config_json_url_unique").unique(true));
        
        final Document result = new Document();
        result.put(FieldNames.TT_CONFIG_NAME.name(), tracTracConfiguration.getName());
        result.put(FieldNames.TT_CONFIG_JSON_URL.name(), tracTracConfiguration.getJSONURL());
        result.put(FieldNames.TT_CONFIG_LIVE_DATA_URI.name(), tracTracConfiguration.getLiveDataURI());
        result.put(FieldNames.TT_CONFIG_STORED_DATA_URI.name(), tracTracConfiguration.getStoredDataURI());
        result.put(FieldNames.TT_CONFIG_COURSE_DESIGN_UPDATE_URI.name(), tracTracConfiguration.getCourseDesignUpdateURI());
        result.put(FieldNames.TT_CONFIG_TRACTRAC_USERNAME.name(), tracTracConfiguration.getTracTracUsername());
        result.put(FieldNames.TT_CONFIG_TRACTRAC_PASSWORD.name(), tracTracConfiguration.getTracTracPassword());
        
        // Object with given name is updated or created if it does not exist yet
        final Document updateQuery = new Document(FieldNames.TT_CONFIG_JSON_URL.name(),
                tracTracConfiguration.getJSONURL());
        ttConfigCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).updateOne(updateQuery, result,
                new UpdateOptions().upsert(true));
    }

    private void dropIndexSafe(MongoCollection<Document> collection, String... indexNames) {
        collection.listIndexes().forEach((Document indexInfo) -> {
            for (String indexName : indexNames) {
                if (indexName.equals(indexInfo.get("name"))) {
                    collection.dropIndex(indexName);
                }
            }
        });
    }
}
