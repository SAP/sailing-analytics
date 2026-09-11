package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.landscape.common.EventLiveContent;
import com.sap.sailing.landscape.common.LiveContentCheckResult;
import com.sap.sailing.landscape.common.RaceLiveContent;
import com.sap.sailing.landscape.common.ReplicaSetLiveContent;
import com.sap.sse.shared.json.JsonSerializer;

public final class LiveContentCheckResultJsonSerializer implements JsonSerializer<LiveContentCheckResult> {
    public static final String CHECKED_AT_MILLIS = "checkedAtMillis";
    public static final String REPLICA_SETS = "replicaSets";
    public static final String REPLICA_SET_NAME = "replicaSetName";
    public static final String EVENTS = "events";
    public static final String EVENT_ID = "eventId";
    public static final String EVENT_NAME = "eventName";
    public static final String EVENT_START_MILLIS = "eventStartMillis";
    public static final String EVENT_END_MILLIS = "eventEndMillis";
    public static final String RACES = "races";
    public static final String REGATTA_NAME = "regattaName";
    public static final String RACE_NAME = "raceName";
    public static final String TRACKING_START_MILLIS = "trackingStartMillis";
    public static final String TRACKING_END_MILLIS = "trackingEndMillis";

    @Override
    public JSONObject serialize(final LiveContentCheckResult liveContentCheckResult) {
        final JSONObject result = new JSONObject();
        result.put(CHECKED_AT_MILLIS, liveContentCheckResult.getCheckedAtMillis());
        final JSONArray replicaSets = new JSONArray();
        for (final ReplicaSetLiveContent replicaSet : liveContentCheckResult.getReplicaSetsWithLiveContent()) {
            final JSONObject serializedReplicaSet = new JSONObject();
            serializedReplicaSet.put(REPLICA_SET_NAME, replicaSet.getReplicaSetName());
            final JSONArray events = new JSONArray();
            for (final EventLiveContent event : replicaSet.getEventsWithLiveContent()) {
                final JSONObject serializedEvent = new JSONObject();
                serializedEvent.put(EVENT_ID, event.getEventId());
                serializedEvent.put(EVENT_NAME, event.getEventName());
                serializedEvent.put(EVENT_START_MILLIS, event.getEventStartMillis());
                serializedEvent.put(EVENT_END_MILLIS, event.getEventEndMillis());
                final JSONArray races = new JSONArray();
                for (final RaceLiveContent race : event.getRacesWithLiveContent()) {
                    final JSONObject serializedRace = new JSONObject();
                    serializedRace.put(REGATTA_NAME, race.getRegattaName());
                    serializedRace.put(RACE_NAME, race.getRaceName());
                    serializedRace.put(TRACKING_START_MILLIS, race.getTrackingStartMillis());
                    serializedRace.put(TRACKING_END_MILLIS, race.getTrackingEndMillis());
                    races.add(serializedRace);
                }
                serializedEvent.put(RACES, races);
                events.add(serializedEvent);
            }
            serializedReplicaSet.put(EVENTS, events);
            replicaSets.add(serializedReplicaSet);
        }
        result.put(REPLICA_SETS, replicaSets);
        return result;
    }
}
