package com.sap.sailing.server.gateway.serialization.racelog.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.orc.RaceLogORCScratchBoatEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sse.shared.json.JsonSerializer;

/**
 * Serializer for {@link com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataEvent ORCLegDataEvent}.
 * 
 * @author Daniel Lisunkin (i505543)
 * @author Axel Uhl (d043530)
 */
public class RaceLogORCScratchBoatEventSerializer extends BaseRaceLogEventSerializer {

    public static final String VALUE_CLASS = RaceLogORCScratchBoatEvent.class.getSimpleName();
    public static final String ORC_CERTIFICATE = "certificate";
    
    public RaceLogORCScratchBoatEventSerializer(JsonSerializer<Competitor> competitorSerializer) {
        super(competitorSerializer);
    }

    @Override
    public JSONObject serialize(RaceLogEvent object) {
        RaceLogORCScratchBoatEvent scratchBoatEvent = (RaceLogORCScratchBoatEvent) object;
        JSONObject result = super.serialize(scratchBoatEvent);
        return result;
    }

    @Override
    protected String getClassFieldValue() {
        return VALUE_CLASS;
    }
}
