package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sse.shared.json.JsonSerializer;

public class DataImportProgressJsonSerializer implements JsonSerializer<DataImportProgress> {
    public static final String EVENT_IDS_IMPORTED = "eventIdsImported";
    public static final String OVERALL_PROGRESS_PERCENT = "overallProgressPercent";
    public static final String CURRENT_SUB_PROGRESS = "currentSubProgress";
    public static final String CURRENT_SUB_PROGRESS_PERCENT = "currentSubProgressPercent";
    public static final String RESULT = "result";
    public static final String OPERATION_ID = "operationId";
    public static final String FAILED = "failed";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String LEADERBOARD_COUNT = "leaderboardCount";
    public static final String LEADERBOARD_GROUP_COUNT = "leaderboardGroupCount";
    public static final String EVENT_COUNT = "eventCount";
    public static final String REGATTA_COUNT = "regattaCount";
    public static final String MEDIA_TRACK_COUNT = "mediaTrackCount";
    public static final String OVERWRITTEN_REGATTA_NAMES = "overwrittenRegattaNames";
    public static final String TRACKED_RACES_COUNT = "trackedRacesCount";
    
    @Override
    public JSONObject serialize(final DataImportProgress object) {
        final JSONObject result = new JSONObject();
        result.put(OVERALL_PROGRESS_PERCENT, object.getOverallProgressPct());
        result.put(CURRENT_SUB_PROGRESS, object.getCurrentSubProgress()==null?null:object.getCurrentSubProgress().name());
        result.put(CURRENT_SUB_PROGRESS_PERCENT, object.getCurrentSubProgressPct());
        if (object.getResult() != null) {
            final JSONObject counts = new JSONObject();
            result.put(RESULT, counts);
            counts.put(LEADERBOARD_COUNT, object.getResult().getLeaderboardCount());
            counts.put(LEADERBOARD_GROUP_COUNT, object.getResult().getLeaderboardGroupCount());
            counts.put(EVENT_COUNT, object.getResult().getEventCount());
            counts.put(REGATTA_COUNT, object.getResult().getRegattaCount());
            counts.put(MEDIA_TRACK_COUNT, object.getResult().getMediaTrackCount());
            final JSONArray overwrittenRegattaNames = new JSONArray();
            if (object.getResult().getNamesOfOverwrittenRegattaNames() != null) {
                for (final String overwrittenRegattaName : object.getResult().getNamesOfOverwrittenRegattaNames()) {
                    overwrittenRegattaNames.add(overwrittenRegattaName);
                }
                counts.put(OVERWRITTEN_REGATTA_NAMES, overwrittenRegattaNames);
            }
            counts.put(TRACKED_RACES_COUNT, object.getResult().getTrackedRacesCount());
            if (object.getResult().getEventIdsImported() != null) {
                final JSONArray eventIds = new JSONArray();
                object.getResult().getEventIdsImported().forEach(id -> eventIds.add(id.toString()));
                counts.put(EVENT_IDS_IMPORTED, eventIds);
            }
        };
        result.put(OPERATION_ID, object.getOperationId().toString());
        result.put(FAILED, object.failed());
        result.put(ERROR_MESSAGE, object.getErrorMessage());
        return result;
    }
}
