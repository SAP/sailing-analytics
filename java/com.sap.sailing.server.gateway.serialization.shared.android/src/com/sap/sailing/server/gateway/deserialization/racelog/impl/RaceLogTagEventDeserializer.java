package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogTagEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogTagEventSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

/**
 * Deserializer for {@link com.sap.sailing.domain.abstractlog.race.RaceLogTagEvent RaceLogTagEvent}.
 */
public class RaceLogTagEventDeserializer extends BaseRaceLogEventDeserializer {

    public RaceLogTagEventDeserializer(JsonDeserializer<DynamicCompetitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint timePoint, int passId, List<Competitor> competitors)
            throws JsonDeserializationException {
        String tag = object.get(RaceLogTagEventSerializer.FIELD_TAG).toString();
        String comment = object.get(RaceLogTagEventSerializer.FIELD_COMMENT).toString();
        String hiddenInfo = object.get(RaceLogTagEventSerializer.FIELD_HIDDEN_INFO).toString();
        String imageUrl = object.get(RaceLogTagEventSerializer.FIELD_URL).toString();
        String resizedImageURL = object.get(RaceLogTagEventSerializer.FIELD_RESIZED_URL).toString();
        return new RaceLogTagEventImpl(tag, comment, hiddenInfo, imageUrl, resizedImageURL, createdAt, timePoint, author, id, passId);
    }
}
