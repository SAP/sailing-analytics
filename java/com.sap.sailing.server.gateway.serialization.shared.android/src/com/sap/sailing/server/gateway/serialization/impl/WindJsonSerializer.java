package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Wind;
import com.sap.sse.common.Position;
import com.sap.sse.shared.json.JsonSerializer;

public class WindJsonSerializer implements JsonSerializer<Wind> {
    public static final String FIELD_POSITION = "position";
    public static final String FIELD_TIMEPOINT = "timepoint";
    public static final String FIELD_SPEED_IN_KNOTS = "speedinknots";
    public static final String FIELD_DIRECTION = "direction";

    private final JsonSerializer<Position> positionSerializer;

    public WindJsonSerializer(JsonSerializer<Position> positionSerializer) {
        this.positionSerializer = positionSerializer;
    }

    @Override
    public JSONObject serialize(Wind wind) {
        JSONObject result = new JSONObject();
        Position position = wind.getPosition();
        if (position != null) {
            result.put(FIELD_POSITION, positionSerializer.serialize(position));
        }
        result.put(FIELD_TIMEPOINT, wind.getTimePoint().asMillis());
        result.put(FIELD_SPEED_IN_KNOTS, wind.getKnots());
        result.put(FIELD_DIRECTION, wind.getBearing().getDegrees());
        return result;
    }
}
