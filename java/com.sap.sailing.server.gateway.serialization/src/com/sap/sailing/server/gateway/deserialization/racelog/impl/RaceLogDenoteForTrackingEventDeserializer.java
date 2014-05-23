package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogDenoteForTrackingEventSerializer;

public class RaceLogDenoteForTrackingEventDeserializer extends BaseRaceLogEventDeserializer {	
    public RaceLogDenoteForTrackingEventDeserializer(JsonDeserializer<Competitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint createdAt, RaceLogEventAuthor author, TimePoint timePoint, int passId, List<Competitor> competitors)
            throws JsonDeserializationException {
    	String raceName = (String) object.get(RaceLogDenoteForTrackingEventSerializer.FIELD_RACE_NAME);
    	BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass(
    			(String) object.get(RaceLogDenoteForTrackingEventSerializer.FIELD_BOAT_CLASS));
    	Serializable raceId = (Serializable) object.get(RaceLogDenoteForTrackingEventSerializer.FIELD_RACE_ID);
        
        return factory.createDenoteForTrackingEvent(createdAt, author, timePoint, id, passId, raceName, boatClass, raceId);
    }
}
