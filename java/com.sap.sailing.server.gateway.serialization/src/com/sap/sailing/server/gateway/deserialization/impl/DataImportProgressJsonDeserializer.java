package com.sap.sailing.server.gateway.deserialization.impl;

import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.DataImportSubProgress;
import com.sap.sailing.domain.common.impl.BaseMasterDataImportObjectCreationCountImpl;
import com.sap.sailing.domain.common.impl.DataImportProgressImpl;
import com.sap.sailing.server.gateway.serialization.impl.DataImportProgressJsonSerializer;
import com.sap.sse.common.Util;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class DataImportProgressJsonDeserializer implements JsonDeserializer<DataImportProgress> {
    @Override
    public DataImportProgress deserialize(final JSONObject object) throws JsonDeserializationException {
        final DataImportProgress result = new DataImportProgressImpl(UUID.fromString(object.get(DataImportProgressJsonSerializer.OPERATION_ID).toString()));
        result.setOverAllProgressPct(((Number) object.get(DataImportProgressJsonSerializer.OVERALL_PROGRESS_PERCENT)).doubleValue());
        if (object.get(DataImportProgressJsonSerializer.CURRENT_SUB_PROGRESS) != null) {
            result.setCurrentSubProgress(DataImportSubProgress.valueOf(object.get(DataImportProgressJsonSerializer.CURRENT_SUB_PROGRESS).toString()));
        }
        result.setCurrentSubProgressPct(((Number) object.get(DataImportProgressJsonSerializer.CURRENT_SUB_PROGRESS_PERCENT)).doubleValue());
        if ((Boolean) object.get(DataImportProgressJsonSerializer.FAILED)) {
            result.setFailed();
        }
        if (object.get(DataImportProgressJsonSerializer.ERROR_MESSAGE) != null) {
            result.setErrorMessage(object.get(DataImportProgressJsonSerializer.ERROR_MESSAGE).toString());
        }
        final JSONObject objectCountsJson = (JSONObject) object.get(DataImportProgressJsonSerializer.RESULT);
        if (objectCountsJson != null) {
            final BaseMasterDataImportObjectCreationCountImpl objectCounts = new BaseMasterDataImportObjectCreationCountImpl(
                    ((Number) objectCountsJson.get(DataImportProgressJsonSerializer.LEADERBOARD_COUNT)).intValue(),
                    ((Number) objectCountsJson.get(DataImportProgressJsonSerializer.LEADERBOARD_GROUP_COUNT)).intValue(),
                    ((Number) objectCountsJson.get(DataImportProgressJsonSerializer.EVENT_COUNT)).intValue(),
                    ((Number) objectCountsJson.get(DataImportProgressJsonSerializer.REGATTA_COUNT)).intValue(),
                    ((Number) objectCountsJson.get(DataImportProgressJsonSerializer.MEDIA_TRACK_COUNT)).intValue(),
                    ((Number) objectCountsJson.get(DataImportProgressJsonSerializer.TRACKED_RACES_COUNT)).intValue(),
                    Util.map(((JSONArray) objectCountsJson.get(DataImportProgressJsonSerializer.OVERWRITTEN_REGATTA_NAMES)), o->o.toString()));
            final JSONArray eventIds = (JSONArray) objectCountsJson.get(DataImportProgressJsonSerializer.EVENT_IDS_IMPORTED);
            if (eventIds != null) {
                objectCounts.setEventIdsImported(Util.map(eventIds, id -> UUID.fromString(id.toString())));
            }
            result.setResult(objectCounts);
        }
        return result;
    }
}
