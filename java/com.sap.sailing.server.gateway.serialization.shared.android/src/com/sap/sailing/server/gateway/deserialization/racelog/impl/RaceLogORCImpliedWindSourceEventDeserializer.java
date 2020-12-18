package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCImpliedWindSourceEvent;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCImpliedWindSourceEventImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.common.orc.ImpliedWindSource;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogORCImpliedWindSourceEventSerializer;
import com.sap.sse.common.TimePoint;

/**
 * Deserializer for {@link RaceLogORCImpliedWindSourceEvent}.
 * 
 * @author Axel Uhl (d043530)
 */
public class RaceLogORCImpliedWindSourceEventDeserializer extends BaseRaceLogEventDeserializer {
    public RaceLogORCImpliedWindSourceEventDeserializer(JsonDeserializer<DynamicCompetitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint timePoint, int passId, List<Competitor> competitors)
            throws JsonDeserializationException {
        final ImpliedWindSource impliedWindSource = new ImpliedWindSourceDeserializer().deserialize((JSONObject) object.get(RaceLogORCImpliedWindSourceEventSerializer.ORC_IMPLIED_WIND_SOURCE));
        final RaceLogEvent result;
        result = new RaceLogORCImpliedWindSourceEventImpl(createdAt, timePoint, author, id, passId, impliedWindSource);
        return result;
    }

}
