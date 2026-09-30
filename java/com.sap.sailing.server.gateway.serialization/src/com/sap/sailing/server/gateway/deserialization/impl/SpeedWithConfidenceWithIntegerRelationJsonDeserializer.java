package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.impl.SpeedWithConfidenceImpl;
import com.sap.sailing.server.gateway.serialization.impl.SpeedWithConfidenceWithIntegerRelationJsonSerializer;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class SpeedWithConfidenceWithIntegerRelationJsonDeserializer implements
        JsonDeserializer<SpeedWithConfidence<Integer>> {

    @Override
    public SpeedWithConfidence<Integer> deserialize(JSONObject object) throws JsonDeserializationException {
        double speedInKnots = (Double) object.get(SpeedWithConfidenceWithIntegerRelationJsonSerializer.FIELD_SPEED);
        double confidence = (Double) object.get(SpeedWithConfidenceWithIntegerRelationJsonSerializer.FIELD_CONFIDENCE);
        int relativeToValue = (Integer) object
                .get(SpeedWithConfidenceWithIntegerRelationJsonSerializer.FIELD_RELATION_INT);
        Speed speed = new KnotSpeedImpl(speedInKnots);
        return new SpeedWithConfidenceImpl<Integer>(speed, confidence, relativeToValue);
    }

}
