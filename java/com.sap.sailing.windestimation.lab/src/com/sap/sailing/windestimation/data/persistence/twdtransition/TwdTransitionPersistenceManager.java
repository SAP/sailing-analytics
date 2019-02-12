package com.sap.sailing.windestimation.data.persistence.twdtransition;

import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.json.simple.JSONObject;

import com.mongodb.BasicDBObject;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.windestimation.data.LabelledTwdTransition;
import com.sap.sailing.windestimation.data.persistence.maneuver.AbstractPersistenceManager;
import com.sap.sailing.windestimation.data.serialization.TwdTransitionJsonDeserializer;
import com.sap.sailing.windestimation.data.serialization.TwdTransitionJsonSerializer;

public class TwdTransitionPersistenceManager extends AbstractPersistenceManager<LabelledTwdTransition> {

    private static final String COLLECTION_NAME = "twdTransitions";
    private final TwdTransitionJsonSerializer serializer = new TwdTransitionJsonSerializer();

    public TwdTransitionPersistenceManager() throws UnknownHostException {
        BasicDBObject indexes = new BasicDBObject(TwdTransitionJsonSerializer.FROM_MANEUVER_TYPE, 1);
        indexes.append(TwdTransitionJsonSerializer.TO_MANEUVER_TYPE, 1);
        getCollection().createIndex(indexes);
    }

    @Override
    public String getCollectionName() {
        return COLLECTION_NAME;
    }

    @Override
    protected JsonDeserializer<LabelledTwdTransition> getNewJsonDeserializer() {
        return new TwdTransitionJsonDeserializer();
    }

    public void add(LabelledTwdTransition twdTransition) {
        JSONObject jsonObject = serializer.serialize(twdTransition);
        Document dbObject = parseJsonString(jsonObject.toString());
        getDb().getCollection(getCollectionName()).insertOne(dbObject);
    }

    public void add(List<LabelledTwdTransition> twdTransitions) {
        List<Document> dbObjects = twdTransitions.stream()
                .map(twdTransition -> parseJsonString(serializer.serialize(twdTransition).toString()))
                .collect(Collectors.toList());
        getDb().getCollection(getCollectionName()).insertMany(dbObjects);
    }

}
