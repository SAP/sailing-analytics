package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.server.gateway.serialization.impl.WindJsonSerializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class WindJsonDeserializer implements JsonDeserializer<Wind> {
    private JsonDeserializer<Position> positionDeserializer;

    public WindJsonDeserializer(JsonDeserializer<Position> positionDeserializer) {
        this.positionDeserializer = positionDeserializer;
    }

    public Wind deserialize(JSONObject object) throws JsonDeserializationException {
        JSONObject positionJsonObject = Helpers.getNestedObjectSafe(object, WindJsonSerializer.FIELD_POSITION);
        Position position = positionJsonObject == null ? null : positionDeserializer.deserialize(positionJsonObject);
        Number timeStamp = (Number) object.get(WindJsonSerializer.FIELD_TIMEPOINT);
        Number direction = (Number) object.get(WindJsonSerializer.FIELD_DIRECTION);
        if (direction == null) {
            // try again with backward-compatible field name
            direction = (Number) object.get("bearing");
        }
        Number speedInKnots = (Number) object.get(WindJsonSerializer.FIELD_SPEED_IN_KNOTS);
        Bearing degreeBearing = new DegreeBearingImpl(direction.doubleValue());
        SpeedWithBearing speedBearing = new KnotSpeedWithBearingImpl(speedInKnots.doubleValue(), degreeBearing);
        Wind wind = new WindImpl(position, new MillisecondsTimePoint(timeStamp.longValue()), speedBearing);
        return wind;
    }

}
