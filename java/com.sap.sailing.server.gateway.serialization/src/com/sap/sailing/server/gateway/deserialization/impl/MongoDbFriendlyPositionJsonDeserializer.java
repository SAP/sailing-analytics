package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.server.gateway.serialization.impl.MongoDbFriendlyPositionJsonSerializer;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class MongoDbFriendlyPositionJsonDeserializer implements JsonDeserializer<Position> {
    @Override
    public Position deserialize(JSONObject object) throws JsonDeserializationException {
        JSONArray coordinatesJson = (JSONArray) object.get(MongoDbFriendlyPositionJsonSerializer.FIELD_COORDINATES);
        Number longitudeDeg = (Number) coordinatesJson.get(0);
        Number latitudeDeg = (Number) coordinatesJson.get(1);
        return new DegreePosition(latitudeDeg.doubleValue(), longitudeDeg.doubleValue());
    }
}
