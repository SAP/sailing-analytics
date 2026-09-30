package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sse.common.Position;
import com.sap.sse.shared.json.JsonSerializer;

public class MongoDbFriendlyPositionJsonSerializer implements JsonSerializer<Position> {

    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TYPE_VALUE = "Point";
    public static final String FIELD_COORDINATES = "coordinates";

    @Override
    public JSONObject serialize(Position position) {
        JSONObject result = new JSONObject();
        JSONArray coordinates = new JSONArray();
        coordinates.add(position.getLngDeg());
        coordinates.add(position.getLatDeg());
        result.put(FIELD_TYPE, FIELD_TYPE_VALUE);
        result.put(FIELD_COORDINATES, coordinates);
        return result;
    }

}
