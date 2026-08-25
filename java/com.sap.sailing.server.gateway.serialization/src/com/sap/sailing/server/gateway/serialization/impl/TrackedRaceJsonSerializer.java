package com.sap.sailing.server.gateway.serialization.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sse.shared.json.JsonSerializer;

public class TrackedRaceJsonSerializer implements JsonSerializer<TrackedRace> {
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_REGATTA = "regatta";
    public static final String FIELD_WINDSOURCES = "windSources";

    public static final String ALL_WINDSOURCES = "ALL";

    private final String windSourceToSerialize;
    private final String windSourceIdToSerialize;
    private final Function<WindSource, WindTrackJsonSerializer> windTrackSerializerProducer;

    public TrackedRaceJsonSerializer(final Function<WindSource, WindTrackJsonSerializer> windTrackSerializer,
            final String windSourceToSerialize, final String windSourceIdToSerialize) {
        this.windTrackSerializerProducer = windTrackSerializer;
        this.windSourceToSerialize = windSourceToSerialize;
        this.windSourceIdToSerialize = windSourceIdToSerialize;
    }

    public JSONObject serialize(TrackedRace trackedRace) {
        JSONObject jsonRace = new JSONObject();
        jsonRace.put(FIELD_NAME, trackedRace.getRace().getName());
        jsonRace.put(FIELD_REGATTA, trackedRace.getRaceIdentifier().getRegattaName());
        if (windTrackSerializerProducer != null) {
            JSONArray windTracks = new JSONArray();
            List<WindSource> windSources = getAvailableWindSources(trackedRace);
            JSONArray jsonWindSourcesDisplayed = new JSONArray();
            for (WindSource windSource : windSources) {
                JSONObject windSourceInformation = new JSONObject();
                windSourceInformation.put("typeName", windSource.getType().name());
                windSourceInformation.put("id", windSource.getId() != null ? windSource.getId().toString() : "");
                jsonWindSourcesDisplayed.add(windSourceInformation);
            }
            jsonRace.put("availableWindSources", jsonWindSourcesDisplayed);
            for (WindSource windSource : windSources) {
                if (ALL_WINDSOURCES.equals(windSourceToSerialize) || windSource.getType().name().equalsIgnoreCase(windSourceToSerialize)) {
                    if (windSourceIdToSerialize != null && windSource.getId() != null && !windSource.getId().toString().equalsIgnoreCase(windSourceIdToSerialize)) {
                        continue;
                    }
                    WindTrack windTrack = trackedRace.getOrCreateWindTrack(windSource);
                    JSONObject jsonWindTrack = windTrackSerializerProducer.apply(windSource).serialize(windTrack);
                    windTracks.add(jsonWindTrack);
                }
            }
            jsonRace.put(FIELD_WINDSOURCES, windTracks);
        }
        return jsonRace;
    }

    private List<WindSource> getAvailableWindSources(TrackedRace trackedRace) {
        List<WindSource> windSources = new ArrayList<WindSource>();
        for (WindSource windSource : trackedRace.getWindSources()) {
            windSources.add(windSource);
        }
        for (WindSource windSourceToExclude : trackedRace.getWindSourcesToExclude()) {
            windSources.remove(windSourceToExclude);
        }
        windSources.add(new WindSourceImpl(WindSourceType.COMBINED));
        for (final WindSource trackedLegMiddleWindSource : trackedRace.getWindSources(WindSourceType.LEG_MIDDLE)) {
            windSources.add(trackedLegMiddleWindSource);
        }
        return windSources;
    }
}