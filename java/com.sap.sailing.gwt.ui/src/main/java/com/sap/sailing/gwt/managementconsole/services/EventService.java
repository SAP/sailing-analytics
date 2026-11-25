package com.sap.sailing.gwt.managementconsole.services;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.common.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.common.communication.event.EventSeriesMetadataDTO;
import com.sap.sailing.gwt.managementconsole.events.EventListResponseEvent;
import com.sap.sailing.gwt.managementconsole.events.EventSeriesListResponseEvent;
import com.sap.sailing.gwt.managementconsole.events.ListResponseEvent;
import com.sap.sailing.gwt.managementconsole.services.factories.LeaderboardGroupFactory;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardGroupDialog.LeaderboardGroupDescriptor;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.VenueDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;

public class EventService {

    private static final Logger LOG = Logger.getLogger(EventService.class.getName());

    private final SailingServiceWriteAsync sailingService;
    private final ErrorReporter errorReporter;
    private final EventBus eventBus;

    private final Map<UUID, EventMetadataDTO> eventIdToEventMap = new HashMap<>();
    private final Map<UUID, EventSeriesMetadataDTO> leaderboardGroupIdToEventSeriesMap = new HashMap<>();
    private final Set<UUID> savingEventIds = new HashSet<>();

    public EventService(final SailingServiceWriteAsync sailingService, final ErrorReporter errorReporter,
            final EventBus eventBus) {
        this.sailingService = sailingService;
        this.errorReporter = errorReporter;
        this.eventBus = eventBus;
    }

    public void requestEventSeriesList(final boolean forceRequestFromService) {
        requestList(leaderboardGroupIdToEventSeriesMap, sailingService::getEventSeriesList,
                EventSeriesMetadataDTO::getSeriesLeaderboardGroupId, EventSeriesListResponseEvent::new,
                forceRequestFromService);
    }

    public void requestEventList(final boolean forceRequestFromService) {
        requestList(eventIdToEventMap, sailingService::getEventList, EventMetadataDTO::getId,
                EventListResponseEvent::new, forceRequestFromService);
    }

    private <T> void requestList(final Map<UUID, T> idToEntryMap, final Consumer<AsyncCallback<List<T>>> listService,
            final Function<T, UUID> idExtractor, final Function<List<T>, ListResponseEvent<T, ?>> eventFactory,
            final boolean forceRequestFromService) {
        if (forceRequestFromService || idToEntryMap.isEmpty()) {
            listService.accept(new AsyncCallback<List<T>>() {

                @Override
                public void onFailure(final Throwable caught) {
                    LOG.severe("requestList :: Cannot load data!");
                    errorReporter.reportError("Error", "Cannot load data!");
                }

                @Override
                public void onSuccess(final List<T> result) {
                    LOG.info("requestList :: onSuccess");
                    idToEntryMap.clear();
                    idToEntryMap.putAll(result.stream().collect(toMap(idExtractor, Function.identity())));
                    eventBus.fireEvent(eventFactory.apply(result));
                }
            });
        } else {
            eventBus.fireEvent(eventFactory.apply(new ArrayList<>(idToEntryMap.values())));
        }
    }

    public void createEvent(final String name, final String venue, final Date startDate,
            final List<String> courseAreaNames, final AsyncCallback<EventDTO> callback) {
        createDefaultLeaderboardGroup(new AsyncCallback<LeaderboardGroupDTO>() {
            @Override
            public void onFailure(final Throwable t) {
                callback.onFailure(t);
            }

            @Override
            public void onSuccess(final LeaderboardGroupDTO result) {
                final List<UUID> leaderboardGroupIDs = Arrays.asList(result.getId());
                final List<CourseAreaDTO> courseAreaDtos = new ArrayList<CourseAreaDTO>();
                courseAreaNames.forEach(name -> courseAreaDtos.add(new CourseAreaDTO(UUID.randomUUID(), name)));
                sailingService.createEvent(name, /* eventDescription */ null, startDate, /* endDate */ null, venue,
                        /* isPublic */ false, courseAreaDtos, /* officialWebsiteURL */null, /* baseURL */ null,
                        /* sailorsInfoWebsiteURLsByLocaleName */ new HashMap<String, String>(),
                        /* images */ new ArrayList<ImageDTO>(), /* videos */ new ArrayList<VideoDTO>(),
                        leaderboardGroupIDs, callback);
            }
        });
    }

    public void createEventSeries(String name, String description, String shortName, boolean isPublic, String baseUrlAsString, 
            ScoringSchemeType scoringSchemeType, int[] discardThresholds, final AsyncCallback<LeaderboardGroupDTO> callback) {
        sailingService.createEventSeries(name, description, shortName, isPublic, baseUrlAsString, scoringSchemeType, 
                discardThresholds, callback);
    }

    private void createDefaultLeaderboardGroup(final AsyncCallback<LeaderboardGroupDTO> leaderboardGroupCallback) {
        final LeaderboardGroupDescriptor newGroup = LeaderboardGroupFactory.createDefaultLeaderboardGroupDescriptor();
        sailingService.createLeaderboardGroup(newGroup.getName(), newGroup.getDescription(),
                newGroup.getDisplayName(), newGroup.isDisplayLeaderboardsInReverseOrder(),
                newGroup.getOverallLeaderboardDiscardThresholds(), newGroup.getOverallLeaderboardScoringSchemeType(),
                new MarkedAsyncCallback<>(leaderboardGroupCallback));
    }

    public void getEvent(final UUID eventId, final AsyncCallback<EventDTO> callback) {
        sailingService.getEventById(eventId, /* withStatisticalData */ false, callback);
    }

    public void getAllEvents(final AsyncCallback<List<EventDTO>> callback) {
        sailingService.getEvents(callback);
    }

    public void getRemoteEvents(final String baseUrl, final String bearerTokenOrNull,
            final AsyncCallback<List<EventDTO>> callback) {
        sailingService.getRemoteEvents(baseUrl, bearerTokenOrNull, callback);
    }

    /**
     * Reconciles the edited fields against fresh server state on every attempt, including retries after partially
     * completed writes. Fields outside this form are retained from the server. Success returns the final persisted state.
     */
    public void updateEvent(final UUID eventId, final String name, final String description, final String venue,
            final Date startDate, final Date endDate, final List<CourseAreaDTO> courseAreas, final boolean isPublic,
            final AsyncCallback<EventDTO> callback) {
        if (savingEventIds.add(eventId)) {
            final List<CourseAreaDTO> desiredAreas = new ArrayList<>();
            for (final CourseAreaDTO area : courseAreas) {
                desiredAreas.add(new CourseAreaDTO(area.getId(), area.getName(), area.getCenterPosition(), area.getRadius()));
            }
            final AsyncCallback<EventDTO> completion = new AsyncCallback<EventDTO>() {
                @Override
                public void onFailure(final Throwable caught) {
                    savingEventIds.remove(eventId);
                    eventIdToEventMap.clear();
                    callback.onFailure(caught);
                }
                @Override
                public void onSuccess(final EventDTO result) {
                    savingEventIds.remove(eventId);
                    eventIdToEventMap.clear();
                    callback.onSuccess(result);
                }
            };
            getEvent(eventId, new AsyncCallback<EventDTO>() {
                @Override
                public void onFailure(final Throwable caught) {
                    completion.onFailure(caught);
                }
                @Override
                public void onSuccess(final EventDTO current) {
                    if (current == null) {
                        completion.onFailure(new IllegalArgumentException("Event no longer exists: " + eventId));
                    } else {
                        final List<Consumer<AsyncCallback<Void>>> writes = courseAreaWrites(eventId,
                                current.getVenue().getCourseAreas(), desiredAreas);
                        current.setName(name);
                        current.setDescription(description);
                        current.startDate = startDate;
                        current.endDate = endDate;
                        current.isPublic = isPublic;
                        current.setVenue(new VenueDTO(venue));
                        sailingService.updateEvent(current, new AsyncCallback<EventDTO>() {
                            @Override
                            public void onFailure(final Throwable caught) {
                                completion.onFailure(caught);
                            }
                            @Override
                            public void onSuccess(final EventDTO ignored) {
                                runCourseAreaWrites(eventId, writes.iterator(), completion);
                            }
                        });
                    }
                }
            });
        } else {
            callback.onFailure(new IllegalStateException("Event save already in progress"));
        }
    }

    private List<Consumer<AsyncCallback<Void>>> courseAreaWrites(final UUID eventId,
            final List<CourseAreaDTO> currentAreas, final List<CourseAreaDTO> desiredAreas) {
        final Map<UUID, CourseAreaDTO> remainingAreas = new HashMap<>();
        for (final CourseAreaDTO area : currentAreas) {
            remainingAreas.put(area.getId(), area);
        }
        final List<CourseAreaDTO> additions = new ArrayList<>();
        final List<CourseAreaDTO> updates = new ArrayList<>();
        for (final CourseAreaDTO desired : desiredAreas) {
            final CourseAreaDTO current = remainingAreas.remove(desired.getId());
            if (current == null) {
                additions.add(desired);
            } else if (!sameAttributes(current, desired)) {
                updates.add(desired);
            }
        }
        final List<Consumer<AsyncCallback<Void>>> writes = new ArrayList<>();
        if (!additions.isEmpty()) {
            writes.add(callback -> sailingService.createCourseAreas(eventId, additions, callback));
        }
        if (!updates.isEmpty()) {
            writes.add(callback -> sailingService.updateCourseAreas(eventId, updates, callback));
        }
        if (!remainingAreas.isEmpty()) {
            final UUID[] removals = remainingAreas.keySet().toArray(new UUID[remainingAreas.size()]);
            writes.add(callback -> sailingService.removeCourseAreas(eventId, removals, callback));
        }
        return writes;
    }

    private boolean sameAttributes(final CourseAreaDTO first, final CourseAreaDTO second) {
        final boolean samePosition = first.getCenterPosition() == null || second.getCenterPosition() == null
                ? first.getCenterPosition() == second.getCenterPosition()
                : Double.compare(first.getCenterPosition().getLatDeg(), second.getCenterPosition().getLatDeg()) == 0
                        && Double.compare(first.getCenterPosition().getLngDeg(), second.getCenterPosition().getLngDeg()) == 0;
        final boolean sameRadius = first.getRadius() == null || second.getRadius() == null
                ? first.getRadius() == second.getRadius()
                : Double.compare(first.getRadius().getMeters(), second.getRadius().getMeters()) == 0;
        return Util.equalsWithNull(first.getName(), second.getName()) && samePosition && sameRadius;
    }

    private void runCourseAreaWrites(final UUID eventId, final Iterator<Consumer<AsyncCallback<Void>>> writes,
            final AsyncCallback<EventDTO> callback) {
        if (writes.hasNext()) {
            writes.next().accept(new AsyncCallback<Void>() {
                @Override
                public void onFailure(final Throwable caught) {
                    callback.onFailure(caught);
                }
                @Override
                public void onSuccess(final Void ignored) {
                    runCourseAreaWrites(eventId, writes, callback);
                }
            });
        } else {
            getEvent(eventId, callback);
        }
    }

    public void deleteEvent(final UUID eventId, final AsyncCallback<Void> callback) {
        sailingService.removeEvent(eventId, new AsyncCallback<Void>() {
            @Override
            public void onFailure(final Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(final Void result) {
                // Invalidate the event list cache so it gets reloaded
                eventIdToEventMap.clear();
                callback.onSuccess(result);
            }
        });
    }
}
