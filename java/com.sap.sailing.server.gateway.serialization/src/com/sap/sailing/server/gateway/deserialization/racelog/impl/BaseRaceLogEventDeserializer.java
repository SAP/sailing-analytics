package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventRestoreFactory;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sailing.server.gateway.serialization.racelog.impl.BaseRaceLogEventSerializer;

public abstract class BaseRaceLogEventDeserializer implements JsonDeserializer<RaceLogEvent> {

    protected RaceLogEventRestoreFactory factory;
    protected JsonDeserializer<Competitor> competitorDeserializer;
    
    public BaseRaceLogEventDeserializer(JsonDeserializer<Competitor> competitorDeserializer) {
        this.factory = RaceLogEventRestoreFactory.INSTANCE;
        this.competitorDeserializer = competitorDeserializer;
    }
    
    protected abstract RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint createdAt, TimePoint timePoint, int passId, List<Competitor> competitors)
            throws JsonDeserializationException;

    @Override
    public RaceLogEvent deserialize(JSONObject object) throws JsonDeserializationException {
        // Factory handles class field and subclassing...
        Serializable id = (Serializable) object.get(BaseRaceLogEventSerializer.FIELD_ID);
        Number createdAt = (Number) object.get(BaseRaceLogEventSerializer.FIELD_CREATED_AT);
        Number timeStamp = (Number) object.get(BaseRaceLogEventSerializer.FIELD_TIMESTAMP);
        Number passId = (Number) object.get(BaseRaceLogEventSerializer.FIELD_PASS_ID);
        
        JSONArray jsonCompetitors = Helpers.getNestedArraySafe(object, BaseRaceLogEventSerializer.FIELD_COMPETITORS);
        List<Competitor> competitors = new ArrayList<Competitor>();
        
        for (Object competitorObject : jsonCompetitors) {
            JSONObject jsonCompetitor = (JSONObject) competitorObject;
            Competitor competitor = competitorDeserializer.deserialize(jsonCompetitor);
            competitors.add(competitor);
        }

        return deserialize(
                object, 
                Helpers.tryUuidConversion(id),
                new MillisecondsTimePoint(createdAt.longValue()),
                new MillisecondsTimePoint(timeStamp.longValue()), 
                passId.intValue(),
                competitors);
    }

}
