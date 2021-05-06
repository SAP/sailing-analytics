package com.sap.sailing.server.gateway.serialization.racelog.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogSuppressedMarkPassingsEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sse.shared.json.JsonSerializer;

public class RaceLogSuppressedMarkPassingsEventSerializer extends BaseRaceLogEventSerializer implements JsonSerializer<RaceLogEvent> {

    public RaceLogSuppressedMarkPassingsEventSerializer(JsonSerializer<Competitor> competitorSerializer) {
        super(competitorSerializer);
    }
    
    public static final String VALUE_CLASS = RaceLogSuppressedMarkPassingsEvent.class.getSimpleName();
    
    public final static String FIELD_INDEX_OF_FIRST_SUPPRESSED_WAYPOINTS = "indexOfFirstSuppressedWaypoints";

    @Override
    protected String getClassFieldValue() {
        return VALUE_CLASS;
    }
    
    @Override
    public JSONObject serialize(RaceLogEvent object) {
        RaceLogSuppressedMarkPassingsEvent event = (RaceLogSuppressedMarkPassingsEvent) object;
        
        JSONObject result = super.serialize(event);
        result.put(FIELD_INDEX_OF_FIRST_SUPPRESSED_WAYPOINTS, event.getZeroBasedIndexOfFirstSuppressedWaypoint());
        return result;
    }


}
