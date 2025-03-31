package com.sap.sailing.server.gateway.serialization.racegroup.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sse.shared.json.ExtendableJsonSerializer;
import com.sap.sse.shared.json.ExtensionJsonSerializer;
import com.sap.sse.shared.json.JsonSerializer;

public class RaceGroupJsonSerializer extends ExtendableJsonSerializer<RaceGroup> {
    public static final String FIELD_NAME = "name";
    public static final String FIELD_BOAT_CLASS = "boatClass";
    public static final String FIELD_REGATTA_CONFIGURATION = "procedures";
    public static final String FIELD_DISPLAY_NAME = "displayName";
    public static final String FIELD_CAN_BOATS_OF_COMPETITORS_CHANGE_PER_RACE = "canBoatsOfCompetitorsChangePerRace";

    private final JsonSerializer<BoatClass> boatClassSerializer;
    private final JsonSerializer<RegattaConfiguration> configurationSerializer;

    public RaceGroupJsonSerializer(
            JsonSerializer<BoatClass> boatClassSerializer,
            JsonSerializer<RegattaConfiguration> configurationSerializer,
            ExtensionJsonSerializer<RaceGroup, ?> extensionSerializer) {
        super(extensionSerializer);
        this.boatClassSerializer = boatClassSerializer;
        this.configurationSerializer = configurationSerializer;
    }

    @Override
    protected JSONObject serializeFields(RaceGroup object) {
        JSONObject result = new JSONObject();
        result.put(FIELD_NAME, object.getName());
        if (object.getBoatClass() != null) {
            result.put(FIELD_BOAT_CLASS, boatClassSerializer.serialize(object.getBoatClass()));
        }
        if (object.getRegattaConfiguration() != null) {
            result.put(FIELD_REGATTA_CONFIGURATION, 
                    configurationSerializer.serialize(object.getRegattaConfiguration()));
        }
        result.put(FIELD_DISPLAY_NAME, object.getDisplayName());
        result.put(FIELD_CAN_BOATS_OF_COMPETITORS_CHANGE_PER_RACE, object.canBoatsOfCompetitorsChangePerRace());
        return result;
    }


}
