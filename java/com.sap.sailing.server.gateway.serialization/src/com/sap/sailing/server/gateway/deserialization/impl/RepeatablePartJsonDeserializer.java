package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.coursetemplate.RepeatablePart;
import com.sap.sailing.domain.coursetemplate.impl.RepeatablePartImpl;
import com.sap.sailing.server.gateway.serialization.impl.RepeatablePartJsonSerializer;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class RepeatablePartJsonDeserializer implements JsonDeserializer<RepeatablePart> {

    @Override
    public RepeatablePart deserialize(JSONObject json) throws JsonDeserializationException {
        final Integer zeroBasedIndexOfRepeatablePartStart = ((Number) json
                .get(RepeatablePartJsonSerializer.FIELD_REPEATABLE_PART_START)).intValue();
        final Integer zeroBasedIndexOfRepeatablePartEnd = ((Number) json
                .get(RepeatablePartJsonSerializer.FIELD_REPEATABLE_PART_END)).intValue();
        return new RepeatablePartImpl(zeroBasedIndexOfRepeatablePartStart, zeroBasedIndexOfRepeatablePartEnd);
    }
}
