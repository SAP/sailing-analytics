package com.sap.sailing.server.gateway.serialization;

import com.sap.sailing.server.gateway.serialization.impl.LeaderboardGroupBaseJsonSerializer;

public interface LeaderboardGroupConstants {
    String ID = "id";
    String NAME = "name";
    String DISPLAYNAME = "displayName";
    String DESCRIPTION = "description";
    String TIMEPOINT = "timepoint";
    String TIMEPOINT_MILLIS = "timepoint-ms";
    String EVENTS = "events";
    String LEADERBOARDS = "leaderboards";
    String HAS_OVERALL_LEADERBOARD = LeaderboardGroupBaseJsonSerializer.FIELD_HAS_OVERALL_LEADERBOARD;
    String OVERALL_LEADERBOARD_NAME = LeaderboardGroupBaseJsonSerializer.FIELD_OVERALL_LEADERBOARD_NAME;
}
