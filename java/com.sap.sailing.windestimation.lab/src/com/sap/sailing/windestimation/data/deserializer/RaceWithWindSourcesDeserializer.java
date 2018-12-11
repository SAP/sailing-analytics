package com.sap.sailing.windestimation.data.deserializer;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.MongoDbFriendlyPositionJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.WindJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.RaceWindJsonSerializer;
import com.sap.sailing.windestimation.data.RaceWithWindSources;
import com.sap.sailing.windestimation.data.WindSourceMetadata;
import com.sap.sailing.windestimation.data.WindSourceWithFixes;

public class RaceWithWindSourcesDeserializer implements JsonDeserializer<RaceWithWindSources> {

    public static final String COMPETITOR_TRACKS = "competitorTracks";
    public static final String RACE_NAME = "raceName";
    public static final String REGATTA_NAME = "regattaName";
    public static final String WIND_QUALITY = "windQuality";

    private final MongoDbFriendlyPositionJsonDeserializer positionDeserializer = new MongoDbFriendlyPositionJsonDeserializer();
    private final WindJsonDeserializer windDeserializer = new WindJsonDeserializer(positionDeserializer);
    private final WindSourceMetadataJsonDeserializer windSourceMetadataDeserializer = new WindSourceMetadataJsonDeserializer(
            positionDeserializer);

    @Override
    public RaceWithWindSources deserialize(JSONObject object) throws JsonDeserializationException {
        WindSourceMetadata raceMetadata = windSourceMetadataDeserializer.deserialize(object);
        JSONArray windSourcesJson = (JSONArray) object.get(RaceWindJsonSerializer.WIND_SOURCES);
        List<WindSourceWithFixes> windSources = new ArrayList<>(windSourcesJson.size());
        for (Object windSourceObj : windSourcesJson) {
            JSONObject windSourceJson = (JSONObject) windSourceObj;
            WindSourceMetadata windSourceMetadata = windSourceMetadataDeserializer.deserialize(windSourceJson);
            WindSourceType windSourceType = WindSourceType
                    .values()[(int) windSourceJson.get(RaceWindJsonSerializer.TYPE)];
            JSONArray windFixesJson = (JSONArray) object.get(RaceWindJsonSerializer.FIXES);
            List<Wind> windFixes = new ArrayList<>(windFixesJson.size());
            for (Object windObj : windFixesJson) {
                JSONObject windJson = (JSONObject) windObj;
                Wind wind = windDeserializer.deserialize(windJson);
                windFixes.add(wind);
            }
            WindSourceWithFixes windSource = new WindSourceWithFixes(windSourceMetadata, windSourceType, windFixes);
            windSources.add(windSource);
        }
        RaceWithWindSources raceWithWindSources = new RaceWithWindSources(raceMetadata, windSources);
        return raceWithWindSources;
    }

}
