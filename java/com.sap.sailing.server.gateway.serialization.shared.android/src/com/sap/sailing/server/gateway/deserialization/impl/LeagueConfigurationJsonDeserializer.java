package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.configuration.RacingProcedureConfiguration;
import com.sap.sailing.domain.base.configuration.impl.LeagueConfigurationImpl;
import com.sap.sailing.domain.base.configuration.procedures.LeagueConfiguration;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;

public class LeagueConfigurationJsonDeserializer extends RacingProcedureConfigurationJsonDeserializer implements
        JsonDeserializer<RacingProcedureConfiguration> {

    public static LeagueConfigurationJsonDeserializer create() {
        return new LeagueConfigurationJsonDeserializer();
    }

    public LeagueConfigurationJsonDeserializer() {
        super();
    }

    @Override
    public LeagueConfiguration deserialize(JSONObject object) throws JsonDeserializationException {
        return (LeagueConfiguration) super.deserialize(object);
    }

    @Override
    protected LeagueConfiguration createResult(JSONObject object, Boolean individualRecall, Boolean resultEntryEnabled, Flags classFlag)
            throws JsonDeserializationException {
        LeagueConfigurationImpl result = new LeagueConfigurationImpl();
        result.setClassFlag(classFlag);
        result.setHasIndividualRecall(individualRecall);
        result.setResultEntryEnabled(resultEntryEnabled);
        return result;
    }

}