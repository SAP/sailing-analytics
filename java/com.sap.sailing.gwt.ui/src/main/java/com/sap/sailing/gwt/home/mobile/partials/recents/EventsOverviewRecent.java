package com.sap.sailing.gwt.home.mobile.partials.recents;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.eventlist.EventListEventDTO;
import com.sap.sailing.gwt.home.communication.eventlist.EventListYearDTO;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class EventsOverviewRecent extends Composite {

    interface EventsOverviewUiBinder extends UiBinder<Widget, EventsOverviewRecent> {
    }
    
    private static EventsOverviewUiBinder uiBinder = GWT.create(EventsOverviewUiBinder.class);

    private final MobilePlacesNavigator navigator;

    @UiField FlowPanel year;
    
    public EventsOverviewRecent(MobilePlacesNavigator navigator) {
        this.navigator = navigator;
        EventsOverviewRecentResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void updateEvents(List<EventListYearDTO> years, TakedownNoticeService takedownNoticeService) {
        // remove old widgets
        year.clear();
        List<EventListEventDTO> allEvents = new LinkedList<EventListEventDTO>();
        boolean oneYearIsExpanded = false;
        for (EventListYearDTO yearDTO : years) {
            EventsOverviewRecentYear recentEventsOfOneYear = new EventsOverviewRecentYear(yearDTO, navigator, !oneYearIsExpanded, takedownNoticeService);
            year.add(recentEventsOfOneYear);
            oneYearIsExpanded = true;
            allEvents.addAll(yearDTO.getEvents());
        }
    }
}
