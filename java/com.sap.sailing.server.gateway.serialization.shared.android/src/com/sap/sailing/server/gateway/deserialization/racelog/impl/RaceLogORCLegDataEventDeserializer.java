package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCLegDataEventImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogORCLegDataEventSerializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.NauticalMileDistance;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

/**
 * Deserializer for {@link com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataEvent ORCLegDataEvent}.
 * 
 * @author Daniel Lisunkin (I505543)
 */
public class RaceLogORCLegDataEventDeserializer extends BaseRaceLogEventDeserializer {

    public RaceLogORCLegDataEventDeserializer(JsonDeserializer<DynamicCompetitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint timePoint, int passId, List<Competitor> competitors)
            throws JsonDeserializationException {
        final int legNr = ((Number) object.get(RaceLogORCLegDataEventSerializer.ORC_LEG_NR)).intValue();
        final Number twaInDegrees = (Number) object.get(RaceLogORCLegDataEventSerializer.ORC_LEG_TWA);
        final Bearing twa = twaInDegrees==null?null:new DegreeBearingImpl(twaInDegrees.doubleValue());
        final Number lengthInNauticalMiles = (Number) object.get(RaceLogORCLegDataEventSerializer.ORC_LEG_LENGTH);
        final Distance length = lengthInNauticalMiles==null?null:new NauticalMileDistance(lengthInNauticalMiles.doubleValue());
        final ORCPerformanceCurveLegTypes type = ORCPerformanceCurveLegTypes.valueOf((String) object.get(RaceLogORCLegDataEventSerializer.ORC_LEG_TYPE));
        return new RaceLogORCLegDataEventImpl(createdAt, timePoint, author, id, passId, legNr, twa, length, type);
    }

}
