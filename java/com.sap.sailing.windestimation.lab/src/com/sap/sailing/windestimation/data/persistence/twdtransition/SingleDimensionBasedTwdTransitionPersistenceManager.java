package com.sap.sailing.windestimation.data.persistence.twdtransition;

import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.json.simple.JSONObject;

import com.mongodb.BasicDBObject;
import com.sap.sailing.windestimation.data.SingleDimensionBasedTwdTransition;
import com.sap.sailing.windestimation.data.persistence.maneuver.AbstractPersistenceManager;
import com.sap.sailing.windestimation.data.persistence.maneuver.PersistedElementsIterator;
import com.sap.sailing.windestimation.data.serialization.SingleDimensionBasedTwdTransitionJsonDeserializer;
import com.sap.sailing.windestimation.data.serialization.SingleDimensionBasedTwdTransitionJsonSerializer;
import com.sap.sse.shared.json.JsonDeserializer;
import com.sap.sse.shared.json.JsonSerializer;

public class SingleDimensionBasedTwdTransitionPersistenceManager
        extends AbstractPersistenceManager<SingleDimensionBasedTwdTransition> {

    private final JsonSerializer<SingleDimensionBasedTwdTransition> serializer = new SingleDimensionBasedTwdTransitionJsonSerializer();
    private final String collectionName;

    public SingleDimensionBasedTwdTransitionPersistenceManager(SingleDimensionType dimensionType)
            throws UnknownHostException {
        this.collectionName = dimensionType.getCollectioName();
        BasicDBObject indexes = new BasicDBObject(SingleDimensionBasedTwdTransitionJsonSerializer.DIMENSION_VALUE, 1);
        getCollection().createIndex(indexes);
    }

    @Override
    public String getCollectionName() {
        return collectionName;
    }

    @Override
    protected JsonDeserializer<SingleDimensionBasedTwdTransition> getNewJsonDeserializer() {
        return new SingleDimensionBasedTwdTransitionJsonDeserializer();
    }

    public void add(SingleDimensionBasedTwdTransition twdTransition) {
        JSONObject jsonObject = serializer.serialize(twdTransition);
        Document dbObject = parseJsonString(jsonObject.toString());
        getDb().getCollection(getCollectionName()).insertOne(dbObject);
    }

    public void add(List<SingleDimensionBasedTwdTransition> twdTransitions) {
        List<Document> dbObjects = twdTransitions.stream()
                .map(twdTransition -> parseJsonString(serializer.serialize(twdTransition).toString()))
                .collect(Collectors.toList());
        getDb().getCollection(getCollectionName()).insertMany(dbObjects);
    }

    public PersistedElementsIterator<SingleDimensionBasedTwdTransition> getIteratorSorted() {
        return super.getIterator(null,
                "{'" + SingleDimensionBasedTwdTransitionJsonSerializer.DIMENSION_VALUE + "': 1}");
    }

    public enum SingleDimensionType {
        DISTANCE("distanceTwdTransition"), DURATION("durationTwdTransition");

        private final String collectioName;

        private SingleDimensionType(String collectionName) {
            this.collectioName = collectionName;
        }

        public String getCollectioName() {
            return collectioName;
        }
    }

}
