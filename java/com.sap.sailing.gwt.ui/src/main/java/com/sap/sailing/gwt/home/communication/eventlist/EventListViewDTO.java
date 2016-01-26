package com.sap.sailing.gwt.home.communication.eventlist;

import java.util.ArrayList;

import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sse.gwt.dispatch.client.commands.DTO;
import com.sap.sse.gwt.dispatch.client.commands.Result;

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

    private  EventListYearDTO getYear(int year) {
        for(int i = 0; i < recentEvents.size(); i++) {
            EventListYearDTO yearDTO = recentEvents.get(i);
            if(year == yearDTO.getYear()) {
                return yearDTO;
            }
            if(year > yearDTO.getYear()) {
                EventListYearDTO newYear = new EventListYearDTO(year);
                recentEvents.add(i, newYear);
                return newYear;
            }
        }
        EventListYearDTO newYear = new EventListYearDTO(year);
        recentEvents.add(newYear);
        return newYear;
    }

    public void addEvent(EventListEventDTO event, int year) {
        if (event.getState() == EventState.RUNNING || event.getState() == EventState.FINISHED) {
            // recent event or live event
            getYear(year).addEvent(event);
        } else {
            // upcoming event
            addUpcomingEvent(event);
        }
    }
}
