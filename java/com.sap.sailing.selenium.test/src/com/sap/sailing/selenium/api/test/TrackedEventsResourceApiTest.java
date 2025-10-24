package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static com.sap.sailing.selenium.api.core.ApiContext.createApiContext;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.opentest4j.AssertionFailedError;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.EventApi.Event;
import com.sap.sailing.selenium.api.event.SecurityApi;
import com.sap.sailing.selenium.api.event.TrackedEventsApi;
import com.sap.sailing.selenium.api.event.TrackedEventsApi.TrackedElement;
import com.sap.sailing.selenium.api.event.TrackedEventsApi.TrackedEvent;
import com.sap.sailing.selenium.api.event.TrackedEventsApi.TrackedEvents;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;

public class TrackedEventsResourceApiTest extends AbstractSeleniumTest {

    private static final String TEST_USER_PASSWORD = "tesOJDS8729438*&tuser";

    private final TrackedEventsApi trackedEventsApi = new TrackedEventsApi();

    private final EventApi eventApi = new EventApi();
    private final SecurityApi securityApi = new SecurityApi();

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
    }

    @SeleniumTestCase
    public void testGetTrackedEventsEmpty() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        final TrackedEvents trackedEvents = trackedEventsApi.getTrackedEvents(adminCtx, true);
        Assertions.assertNotNull(trackedEvents, "Tracked Events element should not be null");
        Assertions.assertTrue(Util.isEmpty(trackedEvents.getEvents()), "Expected empty list of tracked events");
    }

    @SeleniumTestCase
    public void testCreateMultipleTrackingEventsAndEditOne() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        final String competitorId = UUID.randomUUID().toString();
        final String boatId = UUID.randomUUID().toString();
        final String markId = UUID.randomUUID().toString();

        final String eventBaseUrl = "testUrl";
        final String deviceId = UUID.randomUUID().toString();

        final Event evt = eventApi.createEvent(adminCtx, "TestEvent-" + UUID.randomUUID().toString(),
                "75QMNATIONALEKREUZER", CompetitorRegistrationType.CLOSED, "Mannheim");

        final Event evt2 = eventApi.createEvent(adminCtx, "TestEvent-" + UUID.randomUUID().toString(),
                "75QMNATIONALEKREUZER", CompetitorRegistrationType.CLOSED, "Mannheim");

        final Set<Triple<String, String, String>> trackedIds = new HashSet<>();
        trackedIds.add(new Triple<>(competitorId, null, null));
        trackedIds.add(new Triple<>(null, boatId, null));
        trackedEventsApi.updateOrCreateTrackedEvent(adminCtx, evt.getId(), evt.getName(), eventBaseUrl, deviceId,
                trackedIds, evt.getSecret());
        trackedEventsApi.updateOrCreateTrackedEvent(adminCtx, evt2.getId(), evt2.getName(), eventBaseUrl, deviceId,
                new HashSet<>(Arrays.asList(new Triple<>(null, null, markId))), evt2.getSecret());

        trackedEventsApi.setArchived(adminCtx, evt.getId(), evt.getName(), true);
        trackedEventsApi.setArchived(adminCtx, evt2.getId(), evt2.getName(), false);
        // check if created event is still there
        final TrackedEvents trackedEvents = trackedEventsApi.getTrackedEvents(adminCtx, false);

        validateTrackedEvent(trackedEvents, evt2);
        validateTrackedElements(trackedEvents, evt2, null, null, markId, deviceId, 1);
        assertThat(trackedEvents.getEvents(), iterableWithSize(1));
    }

    @SeleniumTestCase
    public void testCreateUpdateTrackingEventsMultiThreaded() throws InterruptedException {
        final ExecutorService threadPool = Executors.newFixedThreadPool(20);
        final List<Future<?>> executions = new ArrayList<>();
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        final String eventBaseUrl = "testUrl";
        final String deviceId = UUID.randomUUID().toString();

        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            Event event = eventApi.createEvent(adminCtx, "TestEvent_" + i, "75QMNATIONALEKREUZER",
                    CompetitorRegistrationType.CLOSED, "Mannheim" + i);
            events.add(event);
            if (i % 2 == 0) {
                executions.add((Future<?>) threadPool.submit(() -> trackedEventsApi.updateOrCreateTrackedEvent(adminCtx,
                        event.getId(), event.getName(), eventBaseUrl, deviceId, null, event.getSecret())));
            }
        }

        for (int i = 0; i < 2000; i++) {
            Event event = events.get(i);
            if (i % 2 == 0) {
                // A:competitor id B:boat id C:mark id
                Set<Triple<String, String, String>> trackedIds = new HashSet<>();
                for (int j = 0; j < 10; j++) {
                    trackedIds.add(
                            new Triple<String, String, String>("competitor" + i + "_" + j, "boat" + i + "_" + j, null));
                }
                final int ii = i;
                executions.add((Future<?>) threadPool.submit(() -> {
                    trackedEventsApi.updateOrCreateTrackedEvent(adminCtx, event.getId(), event.getName(),
                            eventBaseUrl + ii, deviceId + ii, null, event.getSecret());
                }));

                executions.add(threadPool.submit(() -> {
                    trackedEventsApi.getTrackedEvents(adminCtx, false);
                }));
            }
        }
        threadPool.awaitTermination(60, SECONDS);
        executions.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException e) {
                Assertions.fail("interrupted exception occurred");
            } catch (ExecutionException e) {
                Assertions.fail("execution exception:" + e.getCause());
            }
        });
    }

    private void validateTrackedElements(TrackedEvents actualEvents, Event expected, String expectedBoatId,
            String expectedCompetitorId, String expectedMarkId, String expectedDeviceId, long expectedElementCount) {
        TrackedEvent actual = StreamSupport.stream(actualEvents.getEvents().spliterator(), false)
                .filter(evt -> evt.getEventId().equals(expected.getId())).findFirst()
                .orElseThrow(AssertionFailedError::new);

        for (final TrackedElement actualElement : actual.getTrackedElements()) {
            assertThat(actualElement.getBoatId(), equalTo(expectedBoatId));
            assertThat(actualElement.getCompetitorId(), equalTo(expectedCompetitorId));
            assertThat(actualElement.getMarkId(), equalTo(expectedMarkId));
            assertThat("Unexpected device ID", actualElement.getDeviceId(), equalTo(expectedDeviceId));
        }
        assertThat(expectedElementCount,
                equalTo(StreamSupport.stream(actual.getTrackedElements().spliterator(), false).count()));
    }

    private TrackedEvent validateTrackedEvent(TrackedEvents actualEvents, Event expected) {
        TrackedEvent actual = StreamSupport.stream(actualEvents.getEvents().spliterator(), false)
                .filter(evt -> evt.getEventId().equals(expected.getId())).findFirst()
                .orElseThrow(AssertionFailedError::new);
        assertThat("Unexpected event ID", actual.getEventId(), equalTo(expected.getId()));
        assertThat("Unexpected leaderboard name", actual.getLeaderboardName(), equalTo(expected.getName()));
        assertThat("Unexpected regatta secret", actual.getRegattaSecret(), equalTo(expected.getSecret()));
        return actual;
    }

    @SeleniumTestCase
    public void testCreateMultipleTrackingEvents() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        final String competitorId = UUID.randomUUID().toString();
        final String boatId = UUID.randomUUID().toString();
        final String markId = UUID.randomUUID().toString();

        final String eventBaseUrl = "testUrl";
        final String deviceId = UUID.randomUUID().toString();

        final Event evt = eventApi.createEvent(adminCtx, "TestEvent-" + UUID.randomUUID().toString(),
                "75QMNATIONALEKREUZER", CompetitorRegistrationType.CLOSED, "Mannheim");
        final String eventId = evt.getId();
        final String leaderboardName = evt.getName();
        final String regattaSecret = evt.getSecret();

        final Event evt2 = eventApi.createEvent(adminCtx, "TestEvent-" + UUID.randomUUID().toString(),
                "75QMNATIONALEKREUZER", CompetitorRegistrationType.CLOSED, "Mannheim");
        final String eventId2 = evt2.getId();
        final String leaderboardName2 = evt2.getName();
        final String regattaSecret2 = evt2.getSecret();

        final Set<Triple<String, String, String>> trackedIds = new HashSet<>();
        trackedIds.add(new Triple<>(competitorId, null, null));
        trackedIds.add(new Triple<>(null, boatId, null));
        trackedEventsApi.updateOrCreateTrackedEvent(adminCtx, eventId, leaderboardName, eventBaseUrl, deviceId,
                trackedIds, regattaSecret);
        trackedEventsApi.updateOrCreateTrackedEvent(adminCtx, eventId2, leaderboardName2, eventBaseUrl, deviceId,
                new HashSet<>(Arrays.asList(new Triple<>(null, null, markId))), regattaSecret2);

        // check if created event is still there
        final TrackedEvents trackedEvents = trackedEventsApi.getTrackedEvents(adminCtx, true);

        int cntEvents = 0;
        for (final TrackedEvent event : trackedEvents.getEvents()) {
            cntEvents++;
            Assertions.assertEquals(eventBaseUrl, event.getEventBaseUrl(), "Unexpected event base url");
            if (eventId.equals(event.getEventId())) {
                Assertions.assertEquals(eventId, event.getEventId(), "Unexpected event ID");
                Assertions.assertEquals(leaderboardName, event.getLeaderboardName(), "Unexpected leaderboard name");
                Assertions.assertEquals(regattaSecret, event.getRegattaSecret(), "Unexpected regatta secret");
            } else if (eventId2.equals(event.getEventId())) {
                Assertions.assertEquals(eventId2, event.getEventId(), "Unexpected event ID");
                Assertions.assertEquals(leaderboardName2, event.getLeaderboardName(), "Unexpected leaderboard name");
                Assertions.assertEquals(regattaSecret2, event.getRegattaSecret(), "Unexpected regatta secret");
            } else {
                Assertions.fail("Invalid event id.");
            }

            int cntElements = 0;
            for (final TrackedElement elem : event.getTrackedElements()) {
                cntElements++;
                final boolean correctBoatId = boatId.equals(elem.getBoatId());
                final boolean correctCompetitorId = competitorId.equals(elem.getCompetitorId());
                final boolean correctMarkId = markId.equals(elem.getMarkId());

                Assertions.assertEquals(deviceId, elem.getDeviceId(), "Unexpected device ID");
                Assertions.assertTrue(correctBoatId ^ correctCompetitorId ^ correctMarkId,
                        "More than one or zero items tracked in this element");
            }
            Assertions.assertEquals(eventId.equals(event.getEventId()) ? 2 : 1, cntElements,
                    "Invalid numer of elements in this event");
        }

        Assertions.assertEquals(2, cntEvents, "Expected 2 events");
    }

    @SeleniumTestCase
    public void testCreateMultipleTrackingsForSameEvent() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        final String eventName = "TestEvent-" + UUID.randomUUID().toString();
        final String competitorId = UUID.randomUUID().toString();
        final String boatId = UUID.randomUUID().toString();
        final String markId = UUID.randomUUID().toString();

        final String eventBaseUrl = "testUrl";
        final String deviceId = UUID.randomUUID().toString();

        final Event evt = eventApi.createEvent(adminCtx, eventName, "75QMNATIONALEKREUZER",
                CompetitorRegistrationType.CLOSED, "Mannheim");
        final String eventId = evt.getId();
        final String leaderboardName = evt.getName();
        final String regattaSecret = evt.getSecret();

        final Set<Triple<String, String, String>> trackedIds = new HashSet<>();
        trackedIds.add(new Triple<>(competitorId, null, null));
        trackedIds.add(new Triple<>(null, boatId, null));
        trackedIds.add(new Triple<>(null, null, markId));
        trackedEventsApi.updateOrCreateTrackedEvent(adminCtx, eventId, leaderboardName, eventBaseUrl, deviceId,
                trackedIds, regattaSecret);

        // check if created event is still there
        final TrackedEvents trackedEvents = trackedEventsApi.getTrackedEvents(adminCtx, true);

        int cntEvents = 0;
        for (final TrackedEvent event : trackedEvents.getEvents()) {
            cntEvents++;
            Assertions.assertEquals(eventBaseUrl, event.getEventBaseUrl(), "Unexpected event base url");
            Assertions.assertEquals(eventId, event.getEventId(), "Unexpected event ID");
            Assertions.assertEquals(leaderboardName, event.getLeaderboardName(), "Unexpected leaderboard name");
            Assertions.assertEquals(regattaSecret, event.getRegattaSecret(), "Unexpected regatta secret");

            int cntElements = 0;
            for (final TrackedElement elem : event.getTrackedElements()) {
                cntElements++;
                final boolean correctBoatId = boatId.equals(elem.getBoatId());
                final boolean correctCompetitorId = competitorId.equals(elem.getCompetitorId());
                final boolean correctMarkId = markId.equals(elem.getMarkId());

                Assertions.assertEquals(deviceId, elem.getDeviceId(), "Unexpected device ID");
                Assertions.assertTrue(correctBoatId ^ correctCompetitorId ^ correctMarkId,
                        "More than one or zero items tracked in this element");
            }
            Assertions.assertEquals(3, cntElements, "Expected 3 event elements");
        }

        Assertions.assertEquals(1, cntEvents, "Expected exactly 1 event");
    }

    @SeleniumTestCase
    public void testCreateTrackedEventWithCompetitorTracking() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        testCreateTrackedEventWithTracking(UUID.randomUUID().toString(), null, null, adminCtx,
                "TestEvent-" + UUID.randomUUID().toString());
    }

    @SeleniumTestCase
    public void testCreateTrackedEventWithBoatTracking() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        testCreateTrackedEventWithTracking(null, UUID.randomUUID().toString(), null, adminCtx,
                "TestEvent-" + UUID.randomUUID().toString());
    }

    @SeleniumTestCase
    public void testCreateTrackedEventWithMarkTracking() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        testCreateTrackedEventWithTracking(null, null, UUID.randomUUID().toString(), adminCtx,
                "TestEvent-" + UUID.randomUUID().toString());
    }

    private String testCreateTrackedEventWithTracking(final String competitorId, final String boatId,
            final String markId, final ApiContext adminCtx, final String eventName) {
        final String eventBaseUrl = "testUrl";
        final String deviceId = UUID.randomUUID().toString();

        final Event evt = eventApi.createEvent(adminCtx, eventName, "75QMNATIONALEKREUZER",
                CompetitorRegistrationType.CLOSED, "Mannheim");
        final String eventId = evt.getId();
        final String leaderboardName = evt.getName();
        final String regattaSecret = evt.getSecret();

        trackedEventsApi.updateOrCreateTrackedEvent(adminCtx, eventId, leaderboardName, eventBaseUrl, deviceId,
                new HashSet<>(Arrays.asList(new Triple<>(competitorId, boatId, markId))), regattaSecret);

        // check if created event is still there
        final TrackedEvents trackedEvents = trackedEventsApi.getTrackedEvents(adminCtx, true);

        boolean hasEvents = false;
        for (final TrackedEvent event : trackedEvents.getEvents()) {
            hasEvents = true;
            Assertions.assertEquals(eventBaseUrl, event.getEventBaseUrl(), "Unexpected event base url");
            Assertions.assertEquals(eventId, event.getEventId(), "Unexpected event ID");
            Assertions.assertEquals(leaderboardName, event.getLeaderboardName(), "Unexpected leaderboard name");
            Assertions.assertEquals(regattaSecret, event.getRegattaSecret(), "Unexpected regatta secret");

            boolean hasElements = false;
            for (final TrackedElement elem : event.getTrackedElements()) {
                hasElements = true;
                Assertions.assertEquals(boatId, elem.getBoatId(), "Unexpected boat ID");
                Assertions.assertEquals(competitorId, elem.getCompetitorId(), "Unexpected competitor ID");
                Assertions.assertEquals(deviceId, elem.getDeviceId(), "Unexpected device ID");
                Assertions.assertEquals(markId, elem.getMarkId(), "Unexpected mark ID");
            }
            Assertions.assertTrue(hasElements, "Expected at least one element");
        }

        Assertions.assertTrue(hasEvents, "Expected at least one event");
        return eventId;
    }

    @SeleniumTestCase
    public void testDeleteEvent() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        final String leaderboardName = "TestEvent-" + UUID.randomUUID().toString();
        final String eventId = testCreateTrackedEventWithTracking(UUID.randomUUID().toString(), null, null, adminCtx,
                leaderboardName);

        trackedEventsApi.deleteEventTrackings(adminCtx, eventId, leaderboardName);

        final TrackedEvents trackedEvents = trackedEventsApi.getTrackedEvents(adminCtx, true);
        Assertions.assertNotNull(trackedEvents, "Tracked Events element should not be null");
        Assertions.assertTrue(Util.isEmpty(trackedEvents.getEvents()), "Expected empty list of tracked events");
    }

    @SeleniumTestCase
    public void testMultipleUsers() {

        // create test user
        final ApiContext adminSecurityCtx = createAdminApiContext(getContextRoot(), ApiContext.SECURITY_CONTEXT);
        securityApi.createUser(adminSecurityCtx, "tuser", "Test User", null, TEST_USER_PASSWORD);
        final ApiContext userCtx = createApiContext(getContextRoot(), SERVER_CONTEXT, "tuser", TEST_USER_PASSWORD);

        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);

        // add an event in the user context of admin user
        testCreateTrackedEventWithTracking(null, UUID.randomUUID().toString(), null, adminCtx,
                "TestEvent-" + UUID.randomUUID().toString());

        // check if event is invisible to the other user
        final TrackedEvents trackedEvents = trackedEventsApi.getTrackedEvents(userCtx, true);
        Assertions.assertNotNull(trackedEvents, "Tracked Events element should not be null");
        Assertions.assertTrue(Util.isEmpty(trackedEvents.getEvents()), "Expected empty list of tracked events");
    }

}
