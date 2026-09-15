package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import com.sap.sailing.landscape.common.EventLiveContent;
import com.sap.sailing.landscape.common.LiveContentCheckResult;
import com.sap.sailing.landscape.common.RaceLiveContent;
import com.sap.sailing.landscape.common.ReplicaSetLiveContent;
import com.sap.sailing.server.gateway.deserialization.impl.LiveContentCheckResultJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.LiveContentCheckResultJsonSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.shared.json.JsonDeserializationException;

public class LiveContentCheckResultJsonSerializerTest {
    private static final TimePoint CHECKED_AT = TimePoint.of(1700000000000L);

    @Test
    public void testRoundTripWithLiveContentAndUndetermined() throws JsonDeserializationException {
        final RaceLiveContent race = new RaceLiveContent("49er", "R1", TimePoint.of(1699999000000L), null);
        final EventLiveContent event = new EventLiveContent("event-id", "Kiel Week", 1699900000000L, 1700400000000L,
                Collections.singleton(race));
        final ReplicaSetLiveContent replicaSet = new ReplicaSetLiveContent("alpha", Collections.singleton(event));
        final LiveContentCheckResult original = new LiveContentCheckResult(CHECKED_AT,
                Collections.singleton(replicaSet), Arrays.asList("beta", "gamma"));
        final JSONObject serialized = new LiveContentCheckResultJsonSerializer().serialize(original);
        final LiveContentCheckResult roundTripped = new LiveContentCheckResultJsonDeserializer().deserialize(serialized);
        assertEquals(CHECKED_AT, roundTripped.getCheckedAt());
        assertTrue(roundTripped.hasLiveContent());
        assertEquals("R1", Util.get(Util.get(Util.get(roundTripped.getReplicaSetsWithLiveContent(), 0)
                .getEventsWithLiveContent(), 0).getRacesWithLiveContent(), 0).getRaceName());
        assertTrue(roundTripped.hasUndeterminedReplicaSets());
        assertEquals(Arrays.asList("beta", "gamma"), Util.asList(roundTripped.getUndeterminedReplicaSetNames()));
    }

    @Test
    public void testDeserializeToleratesAbsentUndeterminedField() throws JsonDeserializationException {
        // A JSON produced before the undetermined field existed carries no such key; it must deserialize to an empty
        // undetermined list rather than failing.
        final LiveContentCheckResult original = new LiveContentCheckResult(CHECKED_AT, Collections.emptyList());
        final JSONObject serialized = new LiveContentCheckResultJsonSerializer().serialize(original);
        serialized.remove(LiveContentCheckResultJsonSerializer.UNDETERMINED_REPLICA_SETS);
        final LiveContentCheckResult roundTripped = new LiveContentCheckResultJsonDeserializer().deserialize(serialized);
        assertFalse(roundTripped.hasLiveContent());
        assertFalse(roundTripped.hasUndeterminedReplicaSets());
        assertFalse(roundTripped.getUndeterminedReplicaSetNames().iterator().hasNext());
    }
}
