package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogProtestStartTimeEventSerializer;

public class RaceLogProtestStartTimeEventDeserializer extends BaseRaceLogEventDeserializer {

    public RaceLogProtestStartTimeEventDeserializer(JsonDeserializer<Competitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint createdAt, RaceLogEventAuthor author,
            TimePoint timePoint, int passId, List<Competitor> competitors) throws JsonDeserializationException {
        Long protestStartTime = (Long) object.get(RaceLogProtestStartTimeEventSerializer.FIELD_PROTEST_START_TIME);
        return factory.createProtestStartTimeEvent(createdAt, author, timePoint, id, competitors,
                passId, new MillisecondsTimePoint(protestStartTime));
    }

}
