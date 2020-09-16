package com.sap.sailing.server.gateway.serialization.racelog.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.race.RaceLogDependentStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;

public class RaceLogDependentStartTimeEventSerializer extends RaceLogRaceStatusEventSerializer implements JsonSerializer<RaceLogEvent> {

    public RaceLogDependentStartTimeEventSerializer(JsonSerializer<Competitor> competitorSerializer) {
        super(competitorSerializer);
    }

    public static final String VALUE_CLASS = RaceLogDependentStartTimeEvent.class.getSimpleName();

    public static final String FIELD_DEPDENDENT_ON_REGATTALIKE = "dependentOnRegattaLike";
    public static final String FIELD_DEPDENDENT_ON_RACECOLUMN = "dependentOnRaceColumn";
    public static final String FIELD_DEPDENDENT_ON_FLEET = "dependentOnFleet";
    public static final String FIELD_START_TIME_DIFFERENCE = "startTimeDifference";
    public static final Object FIELD_COURSE_AREA_ID_AS_STRING = "courseAreaIdAsString";
    
    @Override
    protected String getClassFieldValue() {
        return VALUE_CLASS;
    }
    
    @Override
    public JSONObject serialize(RaceLogEvent object) {
        RaceLogDependentStartTimeEvent event = (RaceLogDependentStartTimeEvent) object;
        JSONObject result = super.serialize(event);
        result.put(FIELD_DEPDENDENT_ON_FLEET, event.getDependentOnRaceIdentifier().getFleetName());
        result.put(FIELD_DEPDENDENT_ON_RACECOLUMN, event.getDependentOnRaceIdentifier().getRaceColumnName());
        result.put(FIELD_DEPDENDENT_ON_REGATTALIKE, event.getDependentOnRaceIdentifier().getRegattaLikeParentName());
        result.put(FIELD_START_TIME_DIFFERENCE, event.getStartTimeDifference().asMillis());
        result.put(FIELD_NEXT_STATUS, event.getNextStatus().toString());
        result.put(FIELD_COURSE_AREA_ID_AS_STRING, event.getCourseAreaId()==null?null:event.getCourseAreaId().toString());
        return result;
    }

}
