package com.sap.sailing.gwt.server;

import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.eventlist.EventListEventDTO;
import com.sap.sailing.gwt.home.communication.eventlist.EventListEventSeriesDTO;
import com.sap.sailing.gwt.home.communication.eventlist.EventListViewDTO;
import com.sap.sailing.gwt.server.HomeServiceUtil.EventVisitor;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.util.EventUtil;

public class EventListDataCalculator implements EventVisitor {
    
    private final Map<UUID, EventListEventDTO> lastestEventPerSeries = new HashMap<>();
    private final Map<UUID, Integer> numberOfEventsPerSeries = new HashMap<>();
    private final Map<UUID, String> seriesNames = new HashMap<>();
    private final EventListViewDTO result = new EventListViewDTO();
    private final RacingEventService service;

    public EventListDataCalculator(RacingEventService service) {
        this.service = service;
    }

    @Override
    public void visit(EventBase event, boolean onRemoteServer, URL baseURL) {
        if (event.getStartDate() != null) {
            EventListEventDTO eventDTO = HomeServiceUtil.convertToEventListDTO(event, baseURL, onRemoteServer);
            if (HomeServiceUtil.calculateEventState(event) != EventState.UPCOMING && EventUtil.isFakeSeries(event)) {
                final LeaderboardGroupBase seriesLeaderboardGroup = event.getLeaderboardGroups().iterator().next();
                final UUID seriesLeaderboardGroupId = seriesLeaderboardGroup.getId();
                if (!seriesNames.containsKey(seriesLeaderboardGroupId)) {
                    seriesNames.put(seriesLeaderboardGroupId, HomeServiceUtil.getLeaderboardDisplayName(seriesLeaderboardGroup));
                }
                EventListEventDTO latestEvent = lastestEventPerSeries.get(seriesLeaderboardGroupId);
                if (latestEvent == null || latestEvent.getStartDate().before(eventDTO.getStartDate())) {
                    lastestEventPerSeries.put(seriesLeaderboardGroupId, eventDTO);
                }
                increaseNumberOfEvents(seriesLeaderboardGroupId);
            } else {
                addEventToResults(eventDTO);
            }
        }
    }
    
    private void increaseNumberOfEvents(final UUID seriesLeaderboardGroupId) {
        Integer currentValue = numberOfEventsPerSeries.get(seriesLeaderboardGroupId);
        currentValue = currentValue == null ? 0 : currentValue;
        numberOfEventsPerSeries.put(seriesLeaderboardGroupId, ++currentValue);
    }
    
    public EventListViewDTO getResult() {
        for (Entry<UUID, EventListEventDTO> latestEventInSeries : lastestEventPerSeries.entrySet()) {
            final UUID seriesLeaderboardGroupId = latestEventInSeries.getKey();
            EventListEventDTO latestEvent = latestEventInSeries.getValue();
            latestEvent.setEventSeries(new EventListEventSeriesDTO(
                    seriesNames.get(seriesLeaderboardGroupId), seriesLeaderboardGroupId));
            latestEvent.getEventSeries().setEventsCount(numberOfEventsPerSeries.get(seriesLeaderboardGroupId));
            addEventToResults(latestEvent);
        }
        result.addStatistics(service.getOverallStatisticsByYear());
        return result;
    }
    
    private void addEventToResults(EventListEventDTO eventDTO) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(eventDTO.getStartDate());
        result.addEvent(eventDTO, cal.get(Calendar.YEAR));
    }

}
