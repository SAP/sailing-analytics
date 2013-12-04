package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.configuration.RacingProcedureConfiguration;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;

public class RegattaConfigurationJsonSerializer implements JsonSerializer<RegattaConfiguration> {

    public static RegattaConfigurationJsonSerializer create() {
        return new RegattaConfigurationJsonSerializer(RRS26ConfigurationJsonSerializer.create(),
                GateStartConfigurationJsonSerializer.create(), ESSConfigurationJsonSerializer.create(),
                RacingProcedureConfigurationJsonSerializer.create());
    }

    public static final String FIELD_RRS26 = "rrs26";
    public static final String FIELD_GATE_START = "gateStart";
    public static final String FIELD_ESS = "ess";
    public static final String FIELD_BASIC = "basic";

    private final JsonSerializer<RacingProcedureConfiguration> rrs26Serializer;
    private final JsonSerializer<RacingProcedureConfiguration> gateStartSerializer;
    private final JsonSerializer<RacingProcedureConfiguration> essSerializer;
    private final JsonSerializer<RacingProcedureConfiguration> basicSerializer;

    public RegattaConfigurationJsonSerializer(JsonSerializer<RacingProcedureConfiguration> rrs26,
            JsonSerializer<RacingProcedureConfiguration> gateStart, JsonSerializer<RacingProcedureConfiguration> ess,
            JsonSerializer<RacingProcedureConfiguration> basicSerializer) {
        this.rrs26Serializer = rrs26;
        this.gateStartSerializer = gateStart;
        this.essSerializer = ess;
        this.basicSerializer = basicSerializer;
    }

    @Override
    public JSONObject serialize(RegattaConfiguration object) {
        JSONObject result = new JSONObject();
        if (object.getRRS26Configuration() != null) {
            result.put(FIELD_RRS26, rrs26Serializer.serialize(object.getRRS26Configuration()));
        }

        if (object.getGateStartConfiguration() != null) {
            result.put(FIELD_GATE_START, gateStartSerializer.serialize(object.getGateStartConfiguration()));
        }
        
        if (object.getESSConfiguration() != null) {
            result.put(FIELD_ESS, essSerializer.serialize(object.getESSConfiguration()));
        }
        
        if (object.getBasicConfiguration() != null) {
            result.put(FIELD_BASIC, basicSerializer.serialize(object.getBasicConfiguration()));
        }
        return result;
    }

}
