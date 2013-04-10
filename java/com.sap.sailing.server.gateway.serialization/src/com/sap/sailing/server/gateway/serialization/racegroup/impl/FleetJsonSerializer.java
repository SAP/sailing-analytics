package com.sap.sailing.server.gateway.serialization.racegroup.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.common.Color;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;

public class FleetJsonSerializer implements JsonSerializer<Fleet> {
    public static final String FIELD_NAME =	"name";
    public static final String FIELD_ORDERING =	"ordering";
    public static final String FIELD_COLOR = "color";

    private final JsonSerializer<Color> colorSerializer;

    public FleetJsonSerializer(JsonSerializer<Color> colorSerializer) {
        this.colorSerializer = colorSerializer;
    }

    @Override
    public JSONObject serialize(Fleet object) {
        JSONObject result = new JSONObject();

        result.put(FIELD_NAME, object.getName());
        result.put(FIELD_ORDERING, object.getOrdering());
        if (object.getColor() != null) {
            result.put(FIELD_COLOR, colorSerializer.serialize(object.getColor()));
        }


        return result;
    }

}
