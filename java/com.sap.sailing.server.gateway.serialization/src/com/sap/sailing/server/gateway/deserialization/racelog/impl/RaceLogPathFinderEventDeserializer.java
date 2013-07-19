package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogPathfinderEventSerializer;

public class RaceLogPathFinderEventDeserializer extends BaseRaceLogEventDeserializer implements
        JsonDeserializer<RaceLogEvent> {

    public RaceLogPathFinderEventDeserializer(JsonDeserializer<Competitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint createdAt, TimePoint timePoint, int passId,
            List<Competitor> competitors) throws JsonDeserializationException {
        String pathfinderId = object.get(RaceLogPathfinderEventSerializer.FIELD_PATHFINDER_ID).toString();
        return factory.createPathfinderEvent(createdAt, author, timePoint, id, competitors, passId, pathfinderId);
    }

}
