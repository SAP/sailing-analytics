package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.configuration.RacingProcedureConfiguration;
import com.sap.sailing.domain.base.configuration.impl.RacingProcedureConfigurationImpl;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.server.gateway.serialization.impl.RacingProcedureConfigurationJsonSerializer;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class RacingProcedureConfigurationJsonDeserializer implements JsonDeserializer<RacingProcedureConfiguration> {

    public static RacingProcedureConfigurationJsonDeserializer create() {
        return new RacingProcedureConfigurationJsonDeserializer();
    }

    public RacingProcedureConfigurationJsonDeserializer() {
    }

    @Override
    public RacingProcedureConfiguration deserialize(JSONObject object) throws JsonDeserializationException {
        Boolean inidividualRecall = null;
        if (object.containsKey(RacingProcedureConfigurationJsonSerializer.FIELD_INIDIVIDUAL_RECALL)) {
            inidividualRecall = ((Boolean) object
                    .get(RacingProcedureConfigurationJsonSerializer.FIELD_INIDIVIDUAL_RECALL));
        }
        Boolean resultEntryEnabled = null;
        if (object.containsKey(RacingProcedureConfigurationJsonSerializer.FIELD_RESULT_ENTRY_ENABLED)) {
            resultEntryEnabled = ((Boolean) object
                    .get(RacingProcedureConfigurationJsonSerializer.FIELD_RESULT_ENTRY_ENABLED));
        }
        Flags classFlag = null;
        if (object.containsKey(RacingProcedureConfigurationJsonSerializer.FIELD_CLASS_FLAG)) {
            classFlag = Flags.valueOf(object.get(RacingProcedureConfigurationJsonSerializer.FIELD_CLASS_FLAG)
                    .toString());
        }
        return createResult(object, inidividualRecall, resultEntryEnabled, classFlag);
    }

    protected RacingProcedureConfiguration createResult(JSONObject object, Boolean inidividualRecall, Boolean resultEntryEnabled, Flags classFlag) 
            throws JsonDeserializationException{
        RacingProcedureConfigurationImpl result = new RacingProcedureConfigurationImpl();
        result.setHasIndividualRecall(inidividualRecall);
        result.setResultEntryEnabled(resultEntryEnabled);
        result.setClassFlag(classFlag);
        return result;
    }

}