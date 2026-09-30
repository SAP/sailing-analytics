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
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class TestLiveContentValueTypes {
    private static final TimePoint CHECKED_AT = TimePoint.of(42L);

    @Test
    public void testEmptyCheckResult() {
        final LiveContentCheckResult result = new LiveContentCheckResult(CHECKED_AT, Collections.emptyList());
        assertFalse(result.hasLiveContent());
        assertEquals(CHECKED_AT, result.getCheckedAt());
    }

    @Test
    public void testNestedLiveContent() {
        final RaceLiveContent race = new RaceLiveContent("regatta", "race", TimePoint.of(10L), null);
        final EventLiveContent event = new EventLiveContent("event-id", "event", 1L, 100L,
                Collections.singleton(race));
        final ReplicaSetLiveContent replicaSet = new ReplicaSetLiveContent("replica-set",
                Collections.singleton(event));
        final LiveContentCheckResult result = new LiveContentCheckResult(CHECKED_AT, Collections.singleton(replicaSet));
        assertTrue(result.hasLiveContent());
        assertEquals("race", Util.get(Util.get(Util.get(result.getReplicaSetsWithLiveContent(), 0)
                .getEventsWithLiveContent(), 0).getRacesWithLiveContent(), 0).getRaceName());
        assertNull(race.getTrackingEnd());
    }

    @Test
    public void testOperationResultInvariants() {
        final LiveContentCheckResult conflict = new LiveContentCheckResult(CHECKED_AT, Collections.emptyList());
        assertTrue(LiveContentAwareOperationResult.success("ok").isSuccessful());
        assertFalse(LiveContentAwareOperationResult.liveContentConflict(conflict).isSuccessful());
        assertThrows(IllegalArgumentException.class, () -> LiveContentAwareOperationResult.liveContentConflict(null));
    }

    @Test
    public void testUndeterminedReplicaSets() {
        final LiveContentCheckResult withoutUndetermined = new LiveContentCheckResult(CHECKED_AT,
                Collections.emptyList());
        assertFalse(withoutUndetermined.hasUndeterminedReplicaSets());
        assertFalse(withoutUndetermined.getUndeterminedReplicaSetNames().iterator().hasNext());
        final LiveContentCheckResult withUndetermined = new LiveContentCheckResult(CHECKED_AT, Collections.emptyList(),
                Collections.singleton("old-server"));
        assertFalse(withUndetermined.hasLiveContent());
        assertTrue(withUndetermined.hasUndeterminedReplicaSets());
        assertEquals(Collections.singletonList("old-server"),
                Util.asList(withUndetermined.getUndeterminedReplicaSetNames()));
    }
}
