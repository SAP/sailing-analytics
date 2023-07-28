package com.sap.sailing.server.gateway.deserialization.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sailing.domain.base.LeaderboardSearchResultBase;
import com.sap.sailing.domain.base.impl.LeaderboardBaseImpl;
import com.sap.sailing.domain.base.impl.LeaderboardSearchResultBaseImpl;
import com.sap.sailing.server.gateway.serialization.impl.LeaderboardSearchResultJsonSerializer;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class LeaderboardSearchResultBaseJsonDeserializer implements JsonDeserializer<LeaderboardSearchResultBase> {
    private final EventBaseJsonDeserializer eventDeserializer;
    
    private final LeaderboardGroupBaseJsonDeserializer leaderboardGroupDeserializer;

    public LeaderboardSearchResultBaseJsonDeserializer(EventBaseJsonDeserializer eventDeserializer,
            LeaderboardGroupBaseJsonDeserializer leaderboardGroupDeserializer) {
        super();
        this.eventDeserializer = eventDeserializer;
        this.leaderboardGroupDeserializer = leaderboardGroupDeserializer;
    }

    @Override
    public LeaderboardSearchResultBase deserialize(JSONObject object) throws JsonDeserializationException {
        final JSONArray eventsJson = (JSONArray) object.get(LeaderboardSearchResultJsonSerializer.FIELD_EVENTS);
        Collection<EventBase> events = new ArrayList<>();
        if (eventsJson != null) {
            for (Object eventJson : eventsJson) {
                final EventBase event = eventDeserializer.deserialize((JSONObject) eventJson);
                events.add(event);
            }
        } else {
            // for backward compatibility try the FIELD_EVENT approach:
            JSONObject deprecatedEventJson = (JSONObject) object.get("event");
            if (deprecatedEventJson == null) {
                events = Collections.emptySet();
            } else {
                events = Collections.singleton(eventDeserializer.deserialize(deprecatedEventJson));
            }
        }
        final JSONObject leaderboardJson = Helpers.getNestedObjectSafe(object, LeaderboardSearchResultJsonSerializer.FIELD_LEADERBOARD);
        final String leaderboardName = (String) leaderboardJson.get(LeaderboardSearchResultJsonSerializer.FIELD_LEADERBOARD_NAME);
        final String leaderboardDisplayName = (String) leaderboardJson.get(LeaderboardSearchResultJsonSerializer.FIELD_LEADERBOARD_DISPLAY_NAME);
        final String boatClassName = (String) leaderboardJson.get(LeaderboardSearchResultJsonSerializer.FIELD_LEADERBOARD_BOAT_CLASS_NAME);
        final String regattaName = (String) leaderboardJson.get(LeaderboardSearchResultJsonSerializer.FIELD_LEADERBOARD_REGATTA_NAME);
        final JSONArray leaderboardGroupsJson = Helpers.getNestedArraySafe(leaderboardJson, LeaderboardSearchResultJsonSerializer.FIELD_LEADERBOARD_IN_LEADERBOARD_GROUPS);
        final List<LeaderboardGroupBase> leaderboardGroups = new ArrayList<LeaderboardGroupBase>();
        for (final Object leaderboardGroupJson : leaderboardGroupsJson) {
            leaderboardGroups.add(leaderboardGroupDeserializer.deserialize((JSONObject) leaderboardGroupJson));
        }
        return new LeaderboardSearchResultBaseImpl(new LeaderboardBaseImpl(leaderboardName, leaderboardDisplayName),
                regattaName, boatClassName, leaderboardGroups, events);
    }

}
