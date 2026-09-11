package com.sap.sailing.server.gateway.deserialization.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.landscape.common.EventLiveContent;
import com.sap.sailing.landscape.common.LiveContentCheckResult;
import com.sap.sailing.landscape.common.RaceLiveContent;
import com.sap.sailing.landscape.common.ReplicaSetLiveContent;
import com.sap.sailing.server.gateway.serialization.impl.LiveContentCheckResultJsonSerializer;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public final class LiveContentCheckResultJsonDeserializer implements JsonDeserializer<LiveContentCheckResult> {
    @Override
    public LiveContentCheckResult deserialize(final JSONObject object) throws JsonDeserializationException {
        final List<ReplicaSetLiveContent> replicaSets = new ArrayList<>();
        for (final Object serializedReplicaSetObject : (JSONArray) object.get(
                LiveContentCheckResultJsonSerializer.REPLICA_SETS)) {
            final JSONObject serializedReplicaSet = (JSONObject) serializedReplicaSetObject;
            final List<EventLiveContent> events = new ArrayList<>();
            for (final Object serializedEventObject : (JSONArray) serializedReplicaSet.get(
                    LiveContentCheckResultJsonSerializer.EVENTS)) {
                final JSONObject serializedEvent = (JSONObject) serializedEventObject;
                final List<RaceLiveContent> races = new ArrayList<>();
                for (final Object serializedRaceObject : (JSONArray) serializedEvent.get(
                        LiveContentCheckResultJsonSerializer.RACES)) {
                    final JSONObject serializedRace = (JSONObject) serializedRaceObject;
                    races.add(new RaceLiveContent(
                            (String) serializedRace.get(LiveContentCheckResultJsonSerializer.REGATTA_NAME),
                            (String) serializedRace.get(LiveContentCheckResultJsonSerializer.RACE_NAME),
                            ((Number) serializedRace.get(LiveContentCheckResultJsonSerializer.TRACKING_START_MILLIS))
                                    .longValue(),
                            asLong(serializedRace.get(LiveContentCheckResultJsonSerializer.TRACKING_END_MILLIS))));
                }
                events.add(new EventLiveContent(
                        (String) serializedEvent.get(LiveContentCheckResultJsonSerializer.EVENT_ID),
                        (String) serializedEvent.get(LiveContentCheckResultJsonSerializer.EVENT_NAME),
                        asLong(serializedEvent.get(LiveContentCheckResultJsonSerializer.EVENT_START_MILLIS)),
                        asLong(serializedEvent.get(LiveContentCheckResultJsonSerializer.EVENT_END_MILLIS)), races));
            }
            replicaSets.add(new ReplicaSetLiveContent(
                    (String) serializedReplicaSet.get(LiveContentCheckResultJsonSerializer.REPLICA_SET_NAME), events));
        }
        return new LiveContentCheckResult(
                ((Number) object.get(LiveContentCheckResultJsonSerializer.CHECKED_AT_MILLIS)).longValue(), replicaSets);
    }

    private Long asLong(final Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
