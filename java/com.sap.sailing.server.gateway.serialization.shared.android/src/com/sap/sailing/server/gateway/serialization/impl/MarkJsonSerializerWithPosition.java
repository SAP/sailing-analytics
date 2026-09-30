package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.MarkJsonSerializer;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonSerializer;

public class MarkJsonSerializerWithPosition implements JsonSerializer<Pair<ControlPoint, Position>> {
    public static final String FIELD_POSITION = "position";

    private final FlatGPSFixJsonSerializer gpsFixSerializer;
    private final MarkJsonSerializer markSerializer;

    public MarkJsonSerializerWithPosition(MarkJsonSerializer markSerializer, FlatGPSFixJsonSerializer gpsFixSerializer) {
        this.markSerializer = markSerializer;
        this.gpsFixSerializer = gpsFixSerializer;
    }

    @Override
    public JSONObject serialize(Pair<ControlPoint, Position> object) {
        final JSONObject result = markSerializer.serialize(object.getA());
        result.put(FIELD_POSITION,
                object.getB() == null ? null : gpsFixSerializer.serialize(new GPSFixImpl(object.getB(),
                        MillisecondsTimePoint.now())));
        return result;
    }
}
