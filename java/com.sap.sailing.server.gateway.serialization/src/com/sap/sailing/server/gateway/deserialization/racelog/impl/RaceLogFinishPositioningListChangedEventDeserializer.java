package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogFinishPositioningListChangedEventSerializer;

public class RaceLogFinishPositioningListChangedEventDeserializer extends BaseRaceLogEventDeserializer {
    
    public RaceLogFinishPositioningListChangedEventDeserializer(JsonDeserializer<Competitor> competitorDeserializer) {
        super(competitorDeserializer);
    }

    @Override
    protected RaceLogEvent deserialize(JSONObject object, Serializable id, TimePoint createdAt, RaceLogEventAuthor author, TimePoint timePoint, int passId, List<Competitor> competitors)
            throws JsonDeserializationException {
        
        JSONArray jsonPositionedCompetitors = Helpers.getNestedArraySafe(object, RaceLogFinishPositioningListChangedEventSerializer.FIELD_POSITIONED_COMPETITORS);
        List<Triple<Serializable, String, MaxPointsReason>> positionedCompetitors = deserializePositionedCompetitors(jsonPositionedCompetitors);

        return factory.createFinishPositioningListChangedEvent(createdAt, author, timePoint, id, competitors, passId, positionedCompetitors);
    }

    private List<Triple<Serializable, String, MaxPointsReason>> deserializePositionedCompetitors(JSONArray jsonPositionedCompetitors) throws JsonDeserializationException {
        List<Triple<Serializable, String, MaxPointsReason>> positionedCompetitors = new ArrayList<Triple<Serializable, String, MaxPointsReason>>();
        
        for (Object object : jsonPositionedCompetitors) {
            JSONObject jsonPositionedCompetitor = Helpers.toJSONObjectSafe(object);
            
            Serializable competitorId = (Serializable) jsonPositionedCompetitor.get(RaceLogFinishPositioningListChangedEventSerializer.FIELD_COMPETITOR_ID);
            competitorId = Helpers.tryUuidConversion(competitorId);
            String competitorName = (String) jsonPositionedCompetitor.get(RaceLogFinishPositioningListChangedEventSerializer.FIELD_COMPETITOR_NAME);
            
            String maxPointsReasonName = (String) jsonPositionedCompetitor.get(RaceLogFinishPositioningListChangedEventSerializer.FIELD_SCORE_CORRECTIONS_MAX_POINTS_REASON);
            MaxPointsReason maxPointsReason = MaxPointsReason.valueOf(maxPointsReasonName);
            
            Triple<Serializable, String, MaxPointsReason> positionedCompetitor = new Triple<Serializable, String, MaxPointsReason>(competitorId, competitorName, maxPointsReason);
            positionedCompetitors.add(positionedCompetitor);
        }
        
        return positionedCompetitors;
    }

}
