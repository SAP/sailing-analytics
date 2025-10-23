package com.sap.sailing.domain.tractracadapter.persistence.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.sap.sailing.domain.tractracadapter.TracTracConfiguration;
import com.sap.sailing.domain.tractracadapter.persistence.MongoObjectFactory;

public class MongoObjectFactoryImpl implements MongoObjectFactory {
    private final MongoDatabase database;
    
    public MongoObjectFactoryImpl(MongoDatabase database) {
        super();
        this.database = database;
    }
    
    @Override
    public void clear() {
        database.getCollection(CollectionNames.TRACTRAC_CONFIGURATIONS.name())
                .withWriteConcern(WriteConcern.ACKNOWLEDGED).drop();
    }

    @Override
    public void createTracTracConfiguration(TracTracConfiguration tracTracConfiguration) {
        MongoCollection<Document> ttConfigCollection = database.getCollection(CollectionNames.TRACTRAC_CONFIGURATIONS.name());
        final Bson result = getUpdateForTracTracConfiguration(tracTracConfiguration, /* isTracTracApiTokenAvailable */ true); // first-time creation
        ttConfigCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).updateOne(
                getMongoQueryForConfiguration(tracTracConfiguration), result, new UpdateOptions().upsert(true));
    }

    @Override
    public void updateTracTracConfiguration(TracTracConfiguration tracTracConfiguration, boolean isTracTracApiTokenAvailable) {
        MongoCollection<Document> ttConfigCollection = database
                .getCollection(CollectionNames.TRACTRAC_CONFIGURATIONS.name());
        final Bson result = getUpdateForTracTracConfiguration(tracTracConfiguration, isTracTracApiTokenAvailable);
        // Object with given name is updated or created if it does not exist yet
        final Document updateQuery = getMongoQueryForConfiguration(tracTracConfiguration);
        updateQuery.put(FieldNames.TT_CONFIG_CREATOR_NAME.name(), tracTracConfiguration.getCreatorName());
        ttConfigCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).updateOne(updateQuery, result,
                new UpdateOptions().upsert(true));
    }

    private Document getMongoQueryForConfiguration(TracTracConfiguration tracTracConfiguration) {
        final String jsonUrl = tracTracConfiguration.getJSONURL();
        return getMongoQueryForJsonUrl(jsonUrl);
    }

    static Document getMongoQueryForJsonUrl(final String jsonUrl) {
        final Document updateQuery = new Document(FieldNames.TT_CONFIG_JSON_URL.name(), jsonUrl);
        return updateQuery;
    }

    private Bson getUpdateForTracTracConfiguration(TracTracConfiguration tracTracConfiguration, boolean isTracTracApiTokenAvailable) {
        final List<Bson> updates = new ArrayList<>();
        updates.addAll(Arrays.asList(Updates.set(FieldNames.TT_CONFIG_CREATOR_NAME.name(), tracTracConfiguration.getCreatorName()),
                Updates.set(FieldNames.TT_CONFIG_NAME.name(), tracTracConfiguration.getName()),
                Updates.set(FieldNames.TT_CONFIG_JSON_URL.name(), tracTracConfiguration.getJSONURL()),
                Updates.set(FieldNames.TT_CONFIG_LIVE_DATA_URI.name(), tracTracConfiguration.getLiveDataURI()),
                Updates.set(FieldNames.TT_CONFIG_STORED_DATA_URI.name(), tracTracConfiguration.getStoredDataURI()),
                Updates.set(FieldNames.TT_CONFIG_COURSE_DESIGN_UPDATE_URI.name(), tracTracConfiguration.getUpdateURI())));
        if (isTracTracApiTokenAvailable) {
            updates.add(Updates.set(FieldNames.TT_CONFIG_TRACTRAC_API_TOKEN.name(), tracTracConfiguration.getTracTracApiToken()));
        }
        return Updates.combine(updates.toArray(new Bson[updates.size()]));
    }

    @Override
    public void deleteTracTracConfiguration(String creatorName, String jsonUrl) {
        MongoCollection<Document> ttConfigCollection = database.getCollection(CollectionNames.TRACTRAC_CONFIGURATIONS.name());
        final Document deleteQuery = getMongoQueryForJsonUrl(jsonUrl);
        deleteQuery.put(FieldNames.TT_CONFIG_CREATOR_NAME.name(), creatorName);
        ttConfigCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).deleteOne(deleteQuery);
    }
}
