package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogStartTimeEventSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class RaceLogStartTimeEventDeserializer extends RaceLogRaceStatusEventDeserializer {
    
    public RaceLogStartTimeEventDeserializer(JsonDeserializer<DynamicCompetitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint createdAt, AbstractLogEventAuthor author, TimePoint timePoint, int passId, List<Competitor> competitors)
            throws JsonDeserializationException {
        final long startTime = (Long) object.get(RaceLogStartTimeEventSerializer.FIELD_START_TIME);
        final String courseAreaIdAsString = (String) object.get(RaceLogStartTimeEventSerializer.FIELD_COURSE_AREA_ID_AS_STRING);
        final UUID courseAreaId;
        if (courseAreaIdAsString != null) {
            courseAreaId = UUID.fromString(courseAreaIdAsString);
        } else {
            courseAreaId = null;
        }
        RaceLogRaceStatusEvent event = (RaceLogRaceStatusEvent) super.deserialize(object, id, createdAt, author, timePoint, passId, competitors);
        return new RaceLogStartTimeEventImpl(event.getCreatedAt(), event.getLogicalTimePoint(), author, event.getId(), event.getPassId(), new MillisecondsTimePoint(startTime), event.getNextStatus(), courseAreaId);
    }

}
