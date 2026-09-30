package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONObject;

import com.sap.sse.common.Position;
import com.sap.sse.shared.json.JsonSerializer;

public class PositionJsonSerializer implements JsonSerializer<Position> {
    public static final String FIELD_LATITUDE_DEG = "latitude_deg";
    public static final String FIELD_LONGITUDE_DEG = "longitude_deg";

    @Override
    public JSONObject serialize(Position position) {
        JSONObject result = new JSONObject();
        result.put(FIELD_LATITUDE_DEG, position.getLatDeg());
        result.put(FIELD_LONGITUDE_DEG, position.getLngDeg());
        return result;
    }
}
