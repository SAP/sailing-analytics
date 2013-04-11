package com.sap.sailing.server.gateway.deserialization.impl;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogRaceStatusEventSerializer;

public class RaceLogRaceStatusEventDeserializer extends BaseRaceLogEventDeserializer {
    
    public RaceLogRaceStatusEventDeserializer(JsonDeserializer<Competitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint timePoint, int passId, List<Competitor> competitors)
            throws JsonDeserializationException {

        String statusValue = object.get(RaceLogRaceStatusEventSerializer.FIELD_NEXT_STATUS).toString();
        RaceLogRaceStatus nextStatus = RaceLogRaceStatus.valueOf(statusValue);

        return factory.createRaceStatusEvent(timePoint, id, competitors, passId, nextStatus);
    }

}
