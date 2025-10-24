package com.sap.sailing.gwt.home.desktop.partials.eventsrecent;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.eventlist.EventListYearDTO;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class EventsOverviewRecent extends Composite {

    interface EventsOverviewUiBinder extends UiBinder<Widget, EventsOverviewRecent> {
    }
    
    private static EventsOverviewUiBinder uiBinder = GWT.create(EventsOverviewUiBinder.class);

    private final DesktopPlacesNavigator navigator;

    @UiField FlowPanel year;
    
    public EventsOverviewRecent(DesktopPlacesNavigator navigator) {
        this.navigator = navigator;
        
        EventsOverviewRecentResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void updateEvents(List<EventListYearDTO> years, TakedownNoticeService takedownNoticeService) {
        // remove old widgets
        year.clear();
        boolean oneYearIsExpanded = false;
        for (EventListYearDTO yearDTO : years) {
            EventsOverviewRecentYear recentEventsOfOneYear = new EventsOverviewRecentYear(yearDTO, navigator, !oneYearIsExpanded, takedownNoticeService);
            year.add(recentEventsOfOneYear);
            oneYearIsExpanded = true;
        }
    }
}
