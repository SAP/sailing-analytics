package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.server.gateway.serialization.impl.PositionJsonSerializer;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class PositionJsonDeserializer implements JsonDeserializer<Position> {

    public Position deserialize(JSONObject object) throws JsonDeserializationException {
        Number latitudeDeg = (Number) object.get(PositionJsonSerializer.FIELD_LATITUDE_DEG);
        Number longitudeDeg = (Number) object.get(PositionJsonSerializer.FIELD_LONGITUDE_DEG);
        return new DegreePosition(latitudeDeg.doubleValue(), longitudeDeg.doubleValue());
    }

}