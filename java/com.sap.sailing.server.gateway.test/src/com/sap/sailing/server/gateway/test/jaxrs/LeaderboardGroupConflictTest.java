package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.server.gateway.jaxrs.exceptions.ExceptionManager;
import com.sap.sse.common.ObjectAlreadyExistsException;

public class LeaderboardGroupConflictTest extends AbstractJaxRsApiTest {
    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void overallNameConflictMapsToHttp409() {
        final String groupName = "Conflict " + UUID.randomUUID();
        final String overallName = LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(groupName);
        final Leaderboard existing = racingEventService.addFlexibleLeaderboard(overallName, "Original",
                new int[0], new LowPoint(), Collections.emptyList());
        final ObjectAlreadyExistsException conflict = assertThrows(ObjectAlreadyExistsException.class,
                () -> leaderboardGroupsResource.createLeaderboardGroup(groupName, "Description", "Display",
                        /* displayGroupsInReverseOrder */ false, ScoringSchemeType.LOW_POINT.name(), Collections.emptyList()));
        final Response response = new ExceptionManager().toResponse(conflict);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        assertEquals(conflict.getMessage(), response.getEntity());
        assertSame(existing, racingEventService.getLeaderboardByName(overallName));
        assertNull(racingEventService.getLeaderboardGroupByName(groupName));
    }
}
