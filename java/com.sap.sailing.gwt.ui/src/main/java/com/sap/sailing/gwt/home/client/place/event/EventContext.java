package com.sap.sailing.gwt.home.client.place.event;

import java.util.List;

import com.sap.sailing.gwt.home.client.place.event.regatta.RegattaAnalyticsDataManager;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupDTO;
import com.sap.sailing.gwt.ui.shared.eventview.EventViewDTO;
import com.sap.sailing.gwt.ui.shared.eventview.EventViewDTO.EventType;
import com.sap.sailing.gwt.ui.shared.eventview.RegattaMetadataDTO;
import com.sap.sailing.gwt.ui.shared.media.MediaDTO;

/**
 * Common context used by the different tabs in the event place.
 * 
 * @author pgtaboada
 *
 */
public class EventContext {
    private String eventId;
    private String regattaId;
    private List<RaceGroupDTO> raceGroups;
    private List<LeaderboardGroupDTO> leaderboardGroups;
    private RegattaAnalyticsDataManager regattaAnalyticsManager;

    /**
     * Common state required by all tabs/ places in event
     */
    private EventViewDTO eventDTO;
    private MediaDTO media;

    public EventContext() {
    }

    public EventContext(EventContext ctx) {
        updateContext(ctx.getEventDTO());
        withRaceGroups(ctx.raceGroups);
        withLeaderboardGroups(ctx.leaderboardGroups);
        withMedia(ctx.media);
        withRegattaId(ctx.regattaId);
        withRegattaAnalyticsManager(ctx.regattaAnalyticsManager);
    }

    public EventContext withId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public EventContext withRegattaId(String regattaId) {
        this.regattaId = regattaId;
        return this;
    }

    /**
     * Used to update context with dto instance
     */
    public EventContext updateContext(EventViewDTO dto) {
        this.eventDTO = dto;
        if (eventDTO == null) {
            withId(null);
        } else {
            withId(dto.getId().toString());
        }
        return this;
    }

    public EventViewDTO getEventDTO() {
        return eventDTO;
    }

    public String getEventId() {
        return eventId;
    }

    public String getRegattaId() {
        if(regattaId != null) {
            return regattaId;
        }
        if(eventDTO != null && (eventDTO.getType() == EventType.SINGLE_REGATTA || eventDTO.getType() == EventType.SERIES_EVENT)) {
            return eventDTO.getRegattas().iterator().next().getId();
        }
        return null;
    }

    public RegattaMetadataDTO getRegatta() {
        String regattaId = getRegattaId();
        if(regattaId == null || eventDTO == null) {
            return null;
        }
        for (RegattaMetadataDTO regatta : eventDTO.getRegattas()) {
            if(regattaId.equals(regatta.getId())) {
                return regatta;
            }
        }
        return null;
    }

    public List<RaceGroupDTO> getRaceGroups() {
        return raceGroups;
    }

    public EventContext withRaceGroups(List<RaceGroupDTO> raceGroups) {
        this.raceGroups = raceGroups;
        return this;
    }
    
    public List<LeaderboardGroupDTO> getLeaderboardGroups() {
        return leaderboardGroups;
    }
    
    public EventContext withLeaderboardGroups(List<LeaderboardGroupDTO> leaderboardGroups) {
        this.leaderboardGroups = leaderboardGroups;
        return this;
    }

    public RegattaAnalyticsDataManager getRegattaAnalyticsManager() {
        return regattaAnalyticsManager;
    }

    public EventContext withRegattaAnalyticsManager(RegattaAnalyticsDataManager regattaAnalyticsManager) {
        this.regattaAnalyticsManager = regattaAnalyticsManager;
        return this;
    }
    
    public MediaDTO getMedia() {
        return media;
    }

    public EventContext withMedia(MediaDTO mediaDTO) {
        this.media = mediaDTO;
        return this;
    }
}
