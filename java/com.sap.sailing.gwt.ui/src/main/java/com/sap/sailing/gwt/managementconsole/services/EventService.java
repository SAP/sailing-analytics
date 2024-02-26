package com.sap.sailing.gwt.managementconsole.services;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
}
