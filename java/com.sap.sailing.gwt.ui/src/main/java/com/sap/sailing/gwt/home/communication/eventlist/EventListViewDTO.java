package com.sap.sailing.gwt.home.communication.eventlist;

import java.util.ArrayList;
import java.util.Map;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.statistics.Statistics;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sse.gwt.dispatch.shared.commands.Result;
import com.sap.sse.gwt.shared.DTO;

public class EventListViewDTO implements DTO, Result {
    
    private ArrayList<EventListEventDTO> upcomingEvents = new ArrayList<>();
    private ArrayList<EventListYearDTO> recentEvents = new ArrayList<>();
    
    public ArrayList<EventListEventDTO> getUpcomingEvents() {
        return upcomingEvents;
    }

    private void addUpcomingEvent(EventListEventDTO event) {
        for(int i = 0; i < upcomingEvents.size(); i++) {
            if(upcomingEvents.get(i).getStartDate().compareTo(event.getStartDate()) > 0) {
                upcomingEvents.add(i, event);
                return;
            }
        }
        upcomingEvents.add(event);
    }
    
    public ArrayList<EventListYearDTO> getRecentEvents() {
        return recentEvents;
    }

    @GwtIncompatible
    private EventListYearDTO getYear(int year, boolean ensureYearExists) {
        for(int i = 0; i < recentEvents.size(); i++) {
            EventListYearDTO yearDTO = recentEvents.get(i);
            if(year == yearDTO.getYear()) {
                return yearDTO;
            }
            if(year > yearDTO.getYear()) {
                if(!ensureYearExists) {
                    return null;
                }
                EventListYearDTO newYear = new EventListYearDTO(year);
                recentEvents.add(i, newYear);
                return newYear;
            }
        }
        if(!ensureYearExists) {
            return null;
        }
        EventListYearDTO newYear = new EventListYearDTO(year);
        recentEvents.add(newYear);
        return newYear;
    }
    
    @GwtIncompatible
    public void addEvent(EventListEventDTO event, int year) {
        if (event.getState() == EventState.RUNNING || event.getState() == EventState.FINISHED) {
            // recent event or live event
            getYear(year, true).addEvent(event);
        } else {
            // upcoming event
            addUpcomingEvent(event);
        }
    }
    
    @GwtIncompatible
    public void addStatistics(Map<Integer, Statistics> statisticsByYear) {
        statisticsByYear.forEach((year, statistics) -> {
            EventListYearDTO yearDTO = getYear(year, false);
            if(yearDTO != null) {
                yearDTO.addStatistics(statistics);
            }
        });
    }
}
