package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sse.shared.json.JsonSerializer;

public class LeaderboardGroupBaseJsonSerializer implements JsonSerializer<LeaderboardGroupBase> {
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_DISPLAY_NAME = "displayName";
    public static final String FIELD_HAS_OVERALL_LEADERBOARD = "hasOverallLeaderboard";
    public static final String FIELD_OVERALL_LEADERBOARD_NAME = "overallLeaderboardName";

    public LeaderboardGroupBaseJsonSerializer() {
    }

    public JSONObject serialize(LeaderboardGroupBase leaderboardGroup) {
        JSONObject result = new JSONObject();
        result.put(FIELD_ID, leaderboardGroup.getId().toString());
        result.put(FIELD_NAME, leaderboardGroup.getName());
        result.put(FIELD_DESCRIPTION, leaderboardGroup.getDescription());
        result.put(FIELD_DISPLAY_NAME, leaderboardGroup.getDisplayName());
        result.put(FIELD_HAS_OVERALL_LEADERBOARD, leaderboardGroup.hasOverallLeaderboard());
        if (leaderboardGroup.hasOverallLeaderboard()) {
            result.put(FIELD_OVERALL_LEADERBOARD_NAME, leaderboardGroup.getOverallLeaderboardName());
        }
        return result;
    }
}
