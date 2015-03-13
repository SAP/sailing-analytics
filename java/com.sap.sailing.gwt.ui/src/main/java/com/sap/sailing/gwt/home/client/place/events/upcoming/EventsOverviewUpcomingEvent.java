package com.sap.sailing.gwt.home.client.place.events.upcoming;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.client.app.HomePlacesNavigator;
import com.sap.sailing.gwt.home.client.app.PlaceNavigation;
import com.sap.sailing.gwt.home.client.place.event2.EventDefaultPlace;
import com.sap.sailing.gwt.home.client.shared.EventDatesFormatterUtil;
import com.sap.sailing.gwt.ui.shared.eventlist.EventListEventDTO;

public class EventsOverviewUpcomingEvent extends Composite {

    @UiField DivElement eventName;
    @UiField SpanElement venueName;
    @UiField SpanElement eventStartDate;
    @UiField Anchor eventOverviewLink;

    private final HomePlacesNavigator navigator;
    private final PlaceNavigation<EventDefaultPlace> eventNavigation; 

    interface UpcomingEventUiBinder extends UiBinder<Widget, EventsOverviewUpcomingEvent> {
    }
    
    private static UpcomingEventUiBinder uiBinder = GWT.create(UpcomingEventUiBinder.class);

    public EventsOverviewUpcomingEvent(final EventListEventDTO event, final HomePlacesNavigator navigator) {
        this.navigator = navigator;

        EventsOverviewUpcomingResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));

        eventNavigation = navigator.getEventNavigation(event.getId().toString(), event.getBaseURL(), event.isOnRemoteServer());
        eventOverviewLink.setHref(eventNavigation.getTargetUrl());
        
        eventName.setInnerText(event.getDisplayName());
        venueName.setInnerText(event.getVenue());
        eventStartDate.setInnerText(EventDatesFormatterUtil.formatDateRangeWithoutYear(event.getStartDate(), event.getEndDate()));
    }
    
    @UiHandler("eventOverviewLink")
    public void goToEventOverview(ClickEvent e) {
        navigator.goToPlace(eventNavigation);
        e.preventDefault();
    }

}
