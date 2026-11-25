package com.sap.sailing.gwt.managementconsole.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.VenueDTO;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MeterDistance;

public class EventServiceTest {
    @Test
    public void canClearExistingGeometry() {
        final Server server = new Server();
        server.areas.set(/* index */ 0, edited(server.original));
        final Result result = server.save(Collections.singletonList(server.original));
        assertNull(result.failure);
        assertArea(server.original, result.event.getVenue().getCourseAreas().get(/* index */ 0));
        assertTrue(server.calls.contains("updateCourseAreas"));
    }

    @Test
    public void updatesExistingAttributesWithoutRequiringDeletePermission() {
        final Server server = new Server();
        final CourseAreaDTO renamed = edited(server.original);
        final Result result = server.save(Collections.singletonList(renamed));
        assertNull(result.failure);
        assertEquals(Arrays.asList("getEventById", "updateEvent", "updateCourseAreas", "getEventById"), server.calls);
        assertArea(renamed, result.event.getVenue().getCourseAreas().get(/* index */ 0));
        assertEquals("https://keep.example", result.event.getOfficialWebsiteURL());
        assertEquals("Original", server.original.getName());
    }

    @Test
    public void skipsAllAreaWritesForMetadataOnlyEdit() {
        final Server server = new Server();
        final Result result = server.save(Collections.singletonList(server.original));
        assertNull(result.failure);
        assertEquals(Arrays.asList("getEventById", "updateEvent", "getEventById"), server.calls);
        assertEquals("Edited event", result.event.getName());
    }

    @Test
    public void retriesEveryFailedWriteIncludingLostResponsesWithoutDuplicatingAreas() {
        for (final String failedMethod : Arrays.asList("updateEvent", "createCourseAreas", "updateCourseAreas", "removeCourseAreas")) {
            for (final boolean responseLost : new boolean[] {false, true}) {
                final Server server = new Server();
                final CourseAreaDTO removed = new CourseAreaDTO(UUID.randomUUID(), "Remove");
                server.areas.add(removed);
                final CourseAreaDTO renamed = edited(server.original);
                final CourseAreaDTO copied = new CourseAreaDTO(UUID.randomUUID(), renamed.getName(),
                        renamed.getCenterPosition(), renamed.getRadius());
                final List<CourseAreaDTO> desired = Arrays.asList(renamed, copied);
                server.failMethod = failedMethod;
                server.failAfterWrite = responseLost;
                assertNotNull(server.save(desired).failure, failedMethod);
                final Result retry = server.save(desired);
                assertNull(retry.failure, failedMethod);
                assertEquals(/* expected */ 2, retry.event.getVenue().getCourseAreas().size());
                assertArea(renamed, server.areas.get(/* index */ 0));
                assertArea(copied, server.areas.get(/* index */ 1));
                if (responseLost) {
                    assertEquals(/* expected */ 1, Collections.frequency(server.calls, "createCourseAreas"));
                }
            }
        }
    }

    @Test
    public void retriesAfterFinalRefreshFails() {
        final Server server = new Server();
        server.failFinalRead = true;
        final List<CourseAreaDTO> desired = Arrays.asList(server.original, new CourseAreaDTO(UUID.randomUUID(), "Copy"));
        assertNotNull(server.save(desired).failure);
        assertNull(server.save(desired).failure);
        assertEquals(/* expected */ 1, Collections.frequency(server.calls, "createCourseAreas"));
    }

    @Test
    public void preventsConcurrentSavesAndReleasesGuardAfterFailure() {
        final Server server = new Server();
        server.holdRead = true;
        final Result first = server.save(Collections.singletonList(server.original));
        assertNotNull(server.save(Collections.singletonList(server.original)).failure);
        assertEquals(Collections.singletonList("getEventById"), server.calls);
        server.pendingRead.onFailure(new RuntimeException("Read failed"));
        assertNotNull(first.failure);
        server.holdRead = false;
        assertNull(server.save(Collections.singletonList(server.original)).failure);
    }

    private static CourseAreaDTO edited(final CourseAreaDTO original) {
        return new CourseAreaDTO(original.getId(), "Renamed", new DegreePosition(/* latDeg */ 54, /* lngDeg */ 10),
                new MeterDistance(/* meters */ 850));
    }

    private static void assertArea(final CourseAreaDTO expected, final CourseAreaDTO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getCenterPosition(), actual.getCenterPosition());
        assertEquals(expected.getRadius(), actual.getRadius());
    }

    private static class Result implements AsyncCallback<EventDTO> {
        private EventDTO event;
        private Throwable failure;
        @Override
        public void onFailure(final Throwable caught) {
            failure = caught;
        }
        @Override
        public void onSuccess(final EventDTO result) {
            event = result;
        }
    }

    /** Simulates independent RPC snapshots and failures before a write or after a committed write's response is lost. */
    private static class Server implements InvocationHandler {
        private final UUID eventId = UUID.randomUUID();
        private final CourseAreaDTO original = new CourseAreaDTO(UUID.randomUUID(), "Original");
        private final List<CourseAreaDTO> areas = new ArrayList<>(Collections.singletonList(original));
        private final List<String> calls = new ArrayList<>();
        private final EventService service;
        private String name = "Event";
        private String failMethod;
        private boolean failAfterWrite;
        private boolean failFinalRead;
        private boolean holdRead;
        private AsyncCallback<Object> pendingRead;

        private Server() {
            final SailingServiceWriteAsync rpc = (SailingServiceWriteAsync) Proxy.newProxyInstance(
                    SailingServiceWriteAsync.class.getClassLoader(), new Class<?>[] {SailingServiceWriteAsync.class}, this);
            service = new EventService(rpc, /* errorReporter */ null, /* eventBus */ null);
        }

        private Result save(final List<CourseAreaDTO> desired) {
            final Result result = new Result();
            service.updateEvent(eventId, "Edited event", "Description", "Venue", /* startDate */ null,
                    /* endDate */ null, desired, /* isPublic */ false, result);
            return result;
        }

        private EventDTO snapshot() {
            final EventDTO result = new EventDTO(name);
            result.id = eventId;
            result.setOfficialWebsiteURL("keep.example");
            result.setVenue(new VenueDTO("Venue"));
            result.getVenue().setCourseAreas(new ArrayList<>(areas));
            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            final String methodName = method.getName();
            calls.add(methodName);
            final AsyncCallback<Object> callback = (AsyncCallback<Object>) args[args.length - 1];
            final boolean fail = methodName.equals(failMethod);
            if (fail) {
                failMethod = null;
            }
            if (holdRead && methodName.equals("getEventById")) {
                pendingRead = callback;
            } else if (fail && !failAfterWrite) {
                callback.onFailure(new RuntimeException("Write rejected"));
            } else {
                Object result = null;
                switch (methodName) {
                case "getEventById":
                    result = snapshot();
                    break;
                case "updateEvent":
                    name = ((EventDTO) args[0]).getName();
                    result = snapshot();
                    break;
                case "createCourseAreas":
                    areas.addAll((List<CourseAreaDTO>) args[1]);
                    break;
                case "updateCourseAreas":
                    for (final CourseAreaDTO area : (List<CourseAreaDTO>) args[1]) {
                        assertTrue(areas.contains(area));
                        areas.set(areas.indexOf(area), area);
                    }
                    break;
                case "removeCourseAreas":
                    final List<UUID> ids = Arrays.asList((UUID[]) args[1]);
                    areas.removeIf(area -> ids.contains(area.getId()));
                    break;
                default:
                    throw new AssertionError("Unexpected RPC: " + methodName);
                }
                if (fail || (failFinalRead && methodName.equals("getEventById") && calls.size() > 1)) {
                    failFinalRead = false;
                    callback.onFailure(new RuntimeException("Response lost"));
                } else {
                    callback.onSuccess(result);
                }
            }
            return null;
        }
    }
}
