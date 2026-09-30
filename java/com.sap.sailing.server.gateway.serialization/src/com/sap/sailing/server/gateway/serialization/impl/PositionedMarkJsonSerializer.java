package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Mark;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util;
import com.sap.sse.shared.json.JsonSerializer;

public class PositionedMarkJsonSerializer implements JsonSerializer<Util.Pair<Mark, Position>> {
    public static final String FIELD_MARK = "mark";
    public static final String FIELD_POSITION = "position";

    private final JsonSerializer<ControlPoint> markSerializer;
    private final JsonSerializer<Position> positionSerializer;

    public PositionedMarkJsonSerializer(JsonSerializer<ControlPoint> markSerializer, JsonSerializer<Position> positionSerializer) {
        this.markSerializer = markSerializer;
        this.positionSerializer = positionSerializer;
    }

    @Override
    public JSONObject serialize(Util.Pair<Mark, Position> positionedMark) {
        JSONObject result = new JSONObject();

        result.put(FIELD_MARK, markSerializer.serialize(positionedMark.getA()));
        result.put(FIELD_POSITION, positionSerializer.serialize(positionedMark.getB()));

        return result;
    }
}
