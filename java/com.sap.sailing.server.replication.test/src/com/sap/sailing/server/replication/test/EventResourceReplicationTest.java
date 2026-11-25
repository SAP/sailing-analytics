package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.server.gateway.jaxrs.api.EventsResource;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sse.common.Util;
import com.sap.sse.replication.OperationExecutionListener;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.testsupport.SecurityServiceMockFactory;

public class EventResourceReplicationTest extends AbstractServerReplicationTest {
    private final SecurityService securityService = SecurityServiceMockFactory.mockSecurityService();
    private final UriInfo uriInfo = mock(UriInfo.class);
    private final EventsResource eventsResource = new EventsResource() {
        @Override
        public RacingEventService getService() {
            return master;
        }

        @Override
        protected SecurityService getSecurityService() {
            return securityService;
        }
    };

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(uriInfo.getBaseUri()).thenReturn(new URI("http://127.0.0.1:8888/"));
    }

    @Test
    public void testCreateEventWithNewGroupReplicatesLink() throws Exception {
        final JSONObject response = createEvent("New group", Collections.emptyList(), /* createGroup */ true,
                /* createRegatta */ false);
        final UUID eventId = UUID.fromString((String) response.get("eventid"));
        final UUID groupId = UUID.fromString((String) response.get("leaderboardgroupid"));
        awaitReplication();
        assertNotSame(master.getEvent(eventId), replica.getEvent(eventId));
        assertNotSame(master.getLeaderboardGroupByID(groupId), replica.getLeaderboardGroupByID(groupId));
        for (final RacingEventService service : Arrays.asList(master, replica)) {
            assertEventGroups(service, eventId, Collections.singletonList(groupId));
            assertLeaderboards(service, groupId, Collections.emptyList());
        }
    }

    @Test
    public void testCreateEventWithNewGroupAndRegattaReplicatesGraph() throws Exception {
        final LeaderboardGroup existingGroup = createGroup("Existing group", Collections.emptyList(),
                /* discardThresholds */ null, /* scoringScheme */ null);
        final JSONObject response = createEvent("New regatta", Collections.singletonList(existingGroup.getId().toString()),
                /* createGroup */ true, /* createRegatta */ true);
        final UUID eventId = UUID.fromString((String) response.get("eventid"));
        final UUID groupId = UUID.fromString((String) response.get("leaderboardgroupid"));
        final String leaderboardName = (String) response.get("leaderboard");
        awaitReplication();
        for (final RacingEventService service : Arrays.asList(master, replica)) {
            final Event event = assertEventGroups(service, eventId, Arrays.asList(existingGroup.getId(), groupId));
            assertLeaderboards(service, groupId, Collections.singletonList(leaderboardName));
            final RegattaLeaderboard leaderboard = (RegattaLeaderboard) service.getLeaderboardByName(leaderboardName);
            assertSame(service.getRegattaByName((String) response.get("regatta")), leaderboard.getRegatta());
            assertEquals(/* expected */ 1, Util.size(event.getVenue().getCourseAreas()));
            assertTrue(Util.contains(leaderboard.getRegatta().getCourseAreas(),
                    event.getVenue().getCourseAreas().iterator().next()));
        }
    }

    @Test
    public void testCreateEventWithExistingDefaultGroupReplicatesMembership() throws Exception {
        final String eventName = "Existing default group";
        final Leaderboard previousLeaderboard = master.apply(new CreateFlexibleLeaderboard("Previous leaderboard",
                /* leaderboardDisplayName */ null, /* discardThresholds */ new int[0], new LowPoint(),
                /* courseAreaIds */ null));
        final int[] discardThresholds = { 3, 5 };
        final LeaderboardGroup group = createGroup(eventName, Collections.singletonList(previousLeaderboard.getName()),
                discardThresholds, ScoringSchemeType.HIGH_POINT);
        final JSONObject response = createEvent(eventName, Collections.singletonList(group.getId().toString()),
                /* createGroup */ false, /* createRegatta */ true);
        final UUID eventId = UUID.fromString((String) response.get("eventid"));
        final String leaderboardName = (String) response.get("leaderboard");
        awaitReplication();
        for (final RacingEventService service : Arrays.asList(master, replica)) {
            assertEventGroups(service, eventId, Collections.singletonList(group.getId()));
            final LeaderboardGroup actualGroup = assertLeaderboards(service, group.getId(),
                    Arrays.asList(previousLeaderboard.getName(), leaderboardName));
            assertEquals("Group description", actualGroup.getDescription());
            assertEquals("Group display name", actualGroup.getDisplayName());
            assertNotNull(actualGroup.getOverallLeaderboard());
            assertSame(service.getLeaderboardByName(actualGroup.getOverallLeaderboard().getName()),
                    actualGroup.getOverallLeaderboard());
            assertEquals(ScoringSchemeType.HIGH_POINT, actualGroup.getOverallLeaderboard().getScoringScheme().getType());
            assertArrayEquals(discardThresholds, ((ThresholdBasedResultDiscardingRule) actualGroup.getOverallLeaderboard()
                    .getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces());
        }
    }

    @Test
    public void testUpdateEventReplicatesFieldsAndGroupAssignment() throws Exception {
        final JSONObject created = createEvent("Before update", Collections.emptyList(), /* createGroup */ false,
                /* createRegatta */ false);
        final UUID eventId = UUID.fromString((String) created.get("eventid"));
        final LeaderboardGroup group = createGroup("Assigned group", Collections.emptyList(),
                /* discardThresholds */ null, /* scoringScheme */ null);
        final long startDate = 1700000000000L;
        final long endDate = 1700100000000L;
        final Response response = eventsResource.updateEvent(uriInfo, eventId.toString(), "After update", "Updated description",
                /* startDateParam */ null, startDate, /* endDateParam */ null, endDate, "Updated venue",
                /* venueLat */ null, /* venueLng */ null, /* isPublicParam */ "true", "https://example.org/event",
                "https://example.org/base", Collections.singletonList(group.getId().toString()),
                /* createLeaderboardGroupParam */ null, /* createRegattaParam */ null, /* boatClassNameParam */ null,
                /* numberOfRacesParam */ null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        awaitReplication();
        for (final RacingEventService service : Arrays.asList(master, replica)) {
            final Event event = assertEventGroups(service, eventId, Collections.singletonList(group.getId()));
            assertEquals("After update", event.getName());
            assertEquals("Updated description", event.getDescription());
            assertEquals(startDate, event.getStartDate().asMillis());
            assertEquals(endDate, event.getEndDate().asMillis());
            assertEquals("Updated venue", event.getVenue().getName());
            assertTrue(event.isPublic());
            assertEquals("https://example.org/event", event.getOfficialWebsiteURL().toExternalForm());
            assertEquals("https://example.org/base", event.getBaseURL().toExternalForm());
        }
    }

    @Test
    public void testUpdateEventPreservesOmittedGroupsAndReplicatesClearing() throws Exception {
        final LeaderboardGroup group = createGroup("Preserved group", Collections.emptyList(),
                /* discardThresholds */ null, /* scoringScheme */ null);
        final JSONObject created = createEvent("Event with group", Collections.singletonList(group.getId().toString()),
                /* createGroup */ false, /* createRegatta */ false);
        final UUID eventId = UUID.fromString((String) created.get("eventid"));
        for (final List<String> groupIds : Arrays.asList(Collections.<String>emptyList(), Collections.singletonList(""))) {
            final Response response = eventsResource.updateEvent(uriInfo, eventId.toString(), "Updated event",
                    /* eventDescriptionParam */ null, /* startDateParam */ null, /* startDateAsMillis */ null,
                    /* endDateParam */ null, /* endDateAsMillis */ null, /* venueNameParam */ null, /* venueLat */ null,
                    /* venueLng */ null, /* isPublicParam */ null, /* officialWebsiteURLParam */ null,
                    /* baseURLParam */ null, groupIds, /* createLeaderboardGroupParam */ null,
                    /* createRegattaParam */ null, /* boatClassNameParam */ null, /* numberOfRacesParam */ null);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            awaitReplication();
            for (final RacingEventService service : Arrays.asList(master, replica)) {
                final Event event = assertEventGroups(service, eventId,
                        groupIds.isEmpty() ? Collections.singletonList(group.getId()) : Collections.emptyList());
                assertEquals("Updated event", event.getName());
            }
        }
    }

    private JSONObject createEvent(final String name, final List<String> groupIds, final boolean createGroup,
            final boolean createRegatta) throws Exception {
        final Response response = eventsResource.createEvent(uriInfo, name, name, /* startDateParam */ null,
                /* startDateAsMillis */ null, /* endDateParam */ null, /* endDateAsMillis */ null, "Venue",
                /* venueLat */ null, /* venueLng */ null, /* isPublicParam */ "false", /* officialWebsiteURLParam */ null,
                /* baseURLParam */ null, groupIds, Boolean.toString(createGroup), Boolean.toString(createRegatta),
                "A_CAT", /* numberOfRacesParam */ "2", /* canBoatsOfCompetitorsChangePerRace */ false,
                CompetitorRegistrationType.CLOSED.name(), "registration-secret", /* rankingMetricParam */ null,
                /* scoringSchemeParam */ null, /* leaderboardDiscardThresholdsParam */ null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(output);
        return (JSONObject) JSONValue.parse(output.toString("UTF-8"));
    }

    private LeaderboardGroup createGroup(final String name, final List<String> leaderboardNames,
            final int[] discardThresholds, final ScoringSchemeType scoringScheme) {
        return master.apply(new CreateLeaderboardGroup(UUID.randomUUID(), name, "Group description", "Group display name",
                /* displayGroupsInReverseOrder */ false, leaderboardNames, discardThresholds, scoringScheme));
    }

    private Event assertEventGroups(final RacingEventService service, final UUID eventId, final List<UUID> groupIds) {
        final Event event = service.getEvent(eventId);
        assertNotNull(event);
        assertEquals(groupIds, StreamSupport.stream(event.getLeaderboardGroups().spliterator(), /* parallel */ false)
                .map(LeaderboardGroup::getId).collect(Collectors.toList()), service == master ? "Master event groups" : "Replica event groups");
        for (final LeaderboardGroup group : event.getLeaderboardGroups()) {
            assertSame(service.getLeaderboardGroupByID(group.getId()), group);
        }
        return event;
    }

    private LeaderboardGroup assertLeaderboards(final RacingEventService service, final UUID groupId,
            final List<String> leaderboardNames) {
        final LeaderboardGroup group = service.getLeaderboardGroupByID(groupId);
        assertNotNull(group);
        assertEquals(leaderboardNames, StreamSupport.stream(group.getLeaderboards().spliterator(), /* parallel */ false)
                .map(Leaderboard::getName).collect(Collectors.toList()), service == master ? "Master membership" : "Replica membership");
        for (final Leaderboard leaderboard : group.getLeaderboards()) {
            assertSame(service.getLeaderboardByName(leaderboard.getName()), leaderboard);
        }
        return group;
    }

    private void awaitReplication() throws InterruptedException {
        // A marker after the REST request also waits for operations still in transit to the replica's queue.
        final UUID markerId = UUID.randomUUID();
        final CountDownLatch markerApplied = new CountDownLatch(/* count */ 1);
        final OperationExecutionListener<RacingEventService> listener = new OperationExecutionListener<RacingEventService>() {
            @Override
            public <T> void executed(final OperationWithResult<RacingEventService, T> operation) {
                if (replica.getLeaderboardGroupByID(markerId) != null) {
                    markerApplied.countDown();
                }
            }
        };
        replica.addOperationExecutionListener(listener);
        try {
            master.apply(new CreateLeaderboardGroup(markerId, markerId.toString(), "Replication marker",
                    /* displayName */ null, /* displayGroupsInReverseOrder */ false, Collections.emptyList(),
                    /* overallLeaderboardDiscardThresholds */ null, /* overallLeaderboardScoringSchemeType */ null));
            assertTrue(markerApplied.await(/* timeout */ 10, TimeUnit.SECONDS), "Replica did not receive the replication marker");
        } finally {
            replica.removeOperationExecutionListener(listener);
        }
    }
}
