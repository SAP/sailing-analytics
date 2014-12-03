package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;

public class RegattaJsonSerializer implements JsonSerializer<Regatta> {
    public static final String FIELD_NAME = "name";
    public static final String FIELD_BOATCLASS = "boatclass";
    public static final String FIELD_START_DATE = "startDate";
    public static final String FIELD_END_DATE = "endDate";
    public static final String FIELD_SCORINGSYSTEM = "scoringSystem";
    public static final String FIELD_SERIES = "series";
    public static final String FIELD_COMPETITORS = "competitors";
    public static final String FIELD_TRACKED_RACES = "trackedRaces";

    private final JsonSerializer<Series> seriesSerializer;
    private final JsonSerializer<Competitor> competitorSerializer;

    public RegattaJsonSerializer() {
        this(null, null);
    }

    public RegattaJsonSerializer(JsonSerializer<Series> seriesSerializer, JsonSerializer<Competitor> competitorSerializer) {
        this.seriesSerializer = seriesSerializer;
        this.competitorSerializer = competitorSerializer;
    }

    public JSONObject serialize(Regatta regatta) {
        JSONObject result = new JSONObject();

        result.put(FIELD_NAME, regatta.getName());
        result.put(FIELD_START_DATE, regatta.getStartDate() != null ? regatta.getStartDate().asMillis() : null);
        result.put(FIELD_END_DATE, regatta.getStartDate() != null ? regatta.getEndDate().asMillis() : null);
        result.put(FIELD_SCORINGSYSTEM, regatta.getScoringScheme().getType().name());
        result.put(FIELD_BOATCLASS, regatta.getBoatClass() != null ? regatta.getBoatClass().getName(): null);
        
        if(seriesSerializer != null) {
            JSONArray seriesJson = new JSONArray();
            for (Series series : regatta.getSeries()) {
                seriesJson.add(seriesSerializer.serialize(series));
            }
            result.put(FIELD_SERIES, seriesJson);
        }

        if(competitorSerializer != null) {
            JSONArray competitorsJson = new JSONArray();
            for (Competitor competitor: regatta.getCompetitors()) {
                competitorsJson.add(competitorSerializer.serialize(competitor));
            }
            result.put(FIELD_COMPETITORS, competitorsJson);
        }
        
        return result;
    }
}
