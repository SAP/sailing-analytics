package com.sap.sailing.landscape.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.sap.sailing.landscape.common.EventLiveContent;
import com.sap.sailing.landscape.common.LiveContentAwareOperationResult;
import com.sap.sailing.landscape.common.LiveContentCheckResult;
import com.sap.sailing.landscape.common.RaceLiveContent;
import com.sap.sailing.landscape.common.ReplicaSetLiveContent;

public class TestLiveContentValueTypes {
    @Test
    public void testEmptyCheckResult() {
        final LiveContentCheckResult result = new LiveContentCheckResult(42L, Collections.emptyList());
        assertFalse(result.hasLiveContent());
        assertEquals(42L, result.getCheckedAtMillis());
    }

    @Test
    public void testNestedLiveContent() {
        final RaceLiveContent race = new RaceLiveContent("regatta", "race", 10L, null);
        final EventLiveContent event = new EventLiveContent("event-id", "event", 1L, 100L,
                Collections.singleton(race));
        final ReplicaSetLiveContent replicaSet = new ReplicaSetLiveContent("replica-set",
                Collections.singleton(event));
        final LiveContentCheckResult result = new LiveContentCheckResult(42L, Collections.singleton(replicaSet));
        assertTrue(result.hasLiveContent());
        assertEquals("race", result.getReplicaSetsWithLiveContent().get(0).getEventsWithLiveContent().get(0)
                .getRacesWithLiveContent().get(0).getRaceName());
        assertNull(race.getTrackingEndMillis());
    }

    @Test
    public void testOperationResultInvariants() {
        final LiveContentCheckResult conflict = new LiveContentCheckResult(42L, Collections.emptyList());
        assertTrue(LiveContentAwareOperationResult.success("ok").isSuccessful());
        assertFalse(LiveContentAwareOperationResult.liveContentConflict(conflict).isSuccessful());
        assertThrows(IllegalArgumentException.class, () -> LiveContentAwareOperationResult.liveContentConflict(null));
    }
}
