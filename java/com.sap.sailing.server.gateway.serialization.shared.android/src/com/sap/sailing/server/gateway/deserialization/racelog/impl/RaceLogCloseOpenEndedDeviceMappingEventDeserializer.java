package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceLogCloseOpenEndedDeviceMappingEventDeserializer extends BaseRaceLogEventDeserializer {
    public static final String FIELD_DEVICE_MAPPING_EVENT_ID = "deviceMappingEventId";
    public static final String FIELD_CLOSING_TIMEPOINT_MILLIS = "closingTimePointMillis";
    
    public RaceLogCloseOpenEndedDeviceMappingEventDeserializer(JsonDeserializer<Competitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint createdAt, AbstractLogEventAuthor author, TimePoint timePoint, int passId, List<Competitor> competitors)
            throws JsonDeserializationException {
    	Serializable deviceMappingEventId = Helpers.tryUuidConversion((Serializable) object.get(FIELD_DEVICE_MAPPING_EVENT_ID));
    	TimePoint closingTimePoint = new MillisecondsTimePoint((Long) object.get(FIELD_CLOSING_TIMEPOINT_MILLIS));
        
        return new com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogCloseOpenEndedDeviceMappingEventImpl(createdAt, timePoint, author, id, passId,
                deviceMappingEventId, closingTimePoint);
    }
}
