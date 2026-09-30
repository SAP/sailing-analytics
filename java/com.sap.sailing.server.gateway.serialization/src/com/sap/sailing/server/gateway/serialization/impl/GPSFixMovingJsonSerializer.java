package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.server.gateway.deserialization.TypeBasedJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.GPSFixMovingJsonDeserializer;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.shared.json.JsonSerializer;

public class GPSFixMovingJsonSerializer implements JsonSerializer<GPSFixMoving> {

    @Override
    public JSONObject serialize(GPSFixMoving object) {
        return serialize(object, object.getSpeed());
    }

    public JSONObject serialize(GPSFixMoving object, SpeedWithBearing speedWithCourse) {
        JSONObject result = new GPSFixJsonSerializer().serialize(object);

        result.put(TypeBasedJsonDeserializer.FIELD_TYPE, GPSFixMovingJsonDeserializer.TYPE);
        result.put(GPSFixMovingJsonDeserializer.FIELD_BEARING_DEG, speedWithCourse.getBearing().getDegrees());
        result.put(GPSFixMovingJsonDeserializer.FIELD_SPEED_KNOTS, speedWithCourse.getKnots());
        if (object.getOptionalTrueHeading() != null) {
            result.put(GPSFixMovingJsonDeserializer.FIELD_TRUE_HEADING_DEG, object.getOptionalTrueHeading().getDegrees());
        }
        return result;
    }
}
