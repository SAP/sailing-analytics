package com.sap.sailing.gwt.home.desktop.partials.mainevents;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.eventlist.EventListEventDTO;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.partials.eventsrecent.RecentEventTeaser;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.event.EventDefaultPlace;
import com.sap.sailing.gwt.home.shared.places.events.EventsPlace;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class MainEvents extends Composite {
    interface MainEventsUiBinder extends UiBinder<Widget, MainEvents> {
    }
    
    private static MainEventsUiBinder uiBinder = GWT.create(MainEventsUiBinder.class);

    @UiField FlowPanel recentEventsTeaserPanel;
    @UiField Anchor showAllEventsAnchor;
    
    private final DesktopPlacesNavigator navigator;
    private final PlaceNavigation<EventsPlace> eventsNavigation;
    
    public MainEvents(DesktopPlacesNavigator navigator) {
        this.navigator = navigator;
        MainEventsResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        eventsNavigation = navigator.getEventsNavigation();
        showAllEventsAnchor.setHref(eventsNavigation.getTargetUrl());
    }

    public void setRecentEvents(List<EventListEventDTO> recentEvents, TakedownNoticeService takedownNoticeService) {
        final int MAX_RECENT_EVENTS_ON_HOME_PAGE = 3;
        recentEventsTeaserPanel.clear();
        for (int i=0; i<recentEvents.size() && i<MAX_RECENT_EVENTS_ON_HOME_PAGE; i++) {
            EventListEventDTO event = recentEvents.get(i);
            final PlaceNavigation<EventDefaultPlace> eventNavigation = navigator.getEventNavigation(event.getId().toString(), event.getBaseURL(), event.isOnRemoteServer());
            final RecentEventTeaser recentEvent = new RecentEventTeaser(eventNavigation, event, event.getState().getListStateMarker(), takedownNoticeService);
            recentEventsTeaserPanel.add(recentEvent);
        }
    }
    
    @UiHandler("showAllEventsAnchor")
    public void showAllEvents(ClickEvent e) {
        eventsNavigation.goToPlace();
        e.preventDefault();
   }

}
