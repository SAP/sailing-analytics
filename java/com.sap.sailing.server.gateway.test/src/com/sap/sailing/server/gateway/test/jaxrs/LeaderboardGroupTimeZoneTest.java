package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.TimeZone;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.server.gateway.serialization.LeaderboardGroupConstants;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.rest.StreamingOutputUtil;
import com.sap.sse.security.SecurityService;

public class LeaderboardGroupTimeZoneTest {
    @Test
    @ResourceLock("java.util.TimeZone.default")
    public void testLastScoringUpdateIsUtcRegardlessOfDefaultTimeZone() throws Exception {
        final TimeZone originalTimeZone = TimeZone.getDefault();
        try {
            // Start outside UTC so initialization cannot accidentally capture UTC as the default.
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
            final TimePoint lastUpdate = new MillisecondsTimePoint(Instant.parse("2026-10-09T12:13:04Z").toEpochMilli());
            final SettableScoreCorrection scoreCorrection = mock(SettableScoreCorrection.class);
            when(scoreCorrection.getTimePointOfLastCorrectionsValidity()).thenReturn(lastUpdate);
            final Leaderboard leaderboard = mock(Leaderboard.class);
            when(leaderboard.getName()).thenReturn("CNR_2026_F36");
            when(leaderboard.getScoreCorrection()).thenReturn(scoreCorrection);
            when(leaderboard.getScoringScheme()).thenReturn(mock(ScoringScheme.class));
            final LeaderboardGroup leaderboardGroup = mock(LeaderboardGroup.class);
            when(leaderboardGroup.getName()).thenReturn("CNR_2026_GROUP");
            when(leaderboardGroup.getId()).thenReturn(UUID.fromString("12e275d9-37ef-441d-8376-221d2ef07e04"));
            when(leaderboardGroup.getLeaderboards()).thenReturn(Collections.singletonList(leaderboard));
            final RacingEventService service = mock(RacingEventService.class);
            when(service.getLeaderboardGroupByName(leaderboardGroup.getName())).thenReturn(leaderboardGroup);
            when(service.getAllEvents()).thenReturn(Collections.emptyList());
            final SecurityService securityService = mock(SecurityService.class);
            when(securityService.hasCurrentUserReadPermission(leaderboardGroup)).thenReturn(/* value */ true);
            when(securityService.hasCurrentUserReadPermission(leaderboard)).thenReturn(/* value */ true);
            final DummyLeaderboardGroupsResource resource = spy(new DummyLeaderboardGroupsResource());
            doReturn(service).when(resource).getService();
            doReturn(securityService).when(resource).getSecurityService();
            for (final String zoneId : new String[] { "Europe/Berlin", "UTC", "America/Los_Angeles", "UTC",
                    "Asia/Kolkata", "Europe/Berlin", "UTC" }) {
                TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
                final Response response = resource.getLeaderboardGroup(leaderboardGroup.getName());
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), zoneId);
                final JSONObject json = (JSONObject) JSONValue.parseWithException(
                        StreamingOutputUtil.getEntityAsString(response.getEntity()));
                final JSONObject jsonLeaderboard = (JSONObject) ((JSONArray) json.get(
                        LeaderboardGroupConstants.LEADERBOARDS)).get(/* index */ 0);
                assertEquals("Fri Oct 09 12:13:04 UTC 2026",
                        jsonLeaderboard.get(LeaderboardNameConstants.LASTSCORINGUPDATE), zoneId);
                assertEquals(lastUpdate.asMillis(),
                        jsonLeaderboard.get(LeaderboardNameConstants.LASTSCORINGUPDATE_MILLIS), zoneId);
            }
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }
}
