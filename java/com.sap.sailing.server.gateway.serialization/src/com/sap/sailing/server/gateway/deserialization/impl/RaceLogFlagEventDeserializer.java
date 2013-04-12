package com.sap.sailing.server.gateway.deserialization.impl;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogFlagEventSerializer;

public class RaceLogFlagEventDeserializer extends BaseRaceLogEventDeserializer {
    
    public RaceLogFlagEventDeserializer(JsonDeserializer<Competitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint timePoint, int passId, List<Competitor> competitors) {

        Flags upperFlag = Flags.valueOf(object.get(RaceLogFlagEventSerializer.FIELD_UPPER_FLAG).toString());
        Flags lowerFlag = Flags.valueOf(object.get(RaceLogFlagEventSerializer.FIELD_LOWER_FLAG).toString());
        boolean isDisplayed = (Boolean) object.get(RaceLogFlagEventSerializer.FIELD_DISPLAYED);

        return factory.createFlagEvent(timePoint, id, competitors, passId, upperFlag, lowerFlag, isDisplayed);
    }

}
