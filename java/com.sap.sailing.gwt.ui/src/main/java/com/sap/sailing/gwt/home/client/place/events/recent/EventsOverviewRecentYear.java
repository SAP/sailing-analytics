package com.sap.sailing.gwt.home.client.place.events.recent;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.client.place.event.EventDefaultPlace;
import com.sap.sailing.gwt.home.client.place.events.CollapseAnimation;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.eventlist.EventListEventDTO;
import com.sap.sailing.gwt.ui.shared.eventlist.EventListYearDTO;

public class EventsOverviewRecentYear extends Composite {

    interface EventsOverviewUiBinder extends UiBinder<Widget, EventsOverviewRecentYear> {
    }
    
    private static EventsOverviewUiBinder uiBinder = GWT.create(EventsOverviewUiBinder.class);

    @UiField SpanElement year;
    @UiField SpanElement eventsCount;
    @UiField SpanElement countriesCount;
    @UiField SpanElement sailorsCount;
    @UiField SpanElement trackedRacesCount;
    @UiField Element countriesContainer;
    @UiField Element sailorsContainer;
    @UiField Element trackedRacesContainer;
    @UiField FlowPanel recentEventsTeaserPanel;
    @UiField DivElement contentDiv;
    @UiField HTMLPanel headerDiv;
    @UiField StringMessages i18n;
    
    private boolean isContentVisible;
    
    private final CollapseAnimation animation;
    
    public EventsOverviewRecentYear(EventListYearDTO yearDTO, DesktopPlacesNavigator navigator, boolean showInitial) {
        List<EventListEventDTO> events = yearDTO.getEvents();
        
        EventsOverviewRecentResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        
        this.year.setInnerText(String.valueOf(yearDTO.getYear()));
        this.eventsCount.setInnerText(i18n.eventsCount(events.size()));
        if(yearDTO.getSailorCount() > 0) {
            sailorsCount.setInnerText(i18n.competitorsCount(yearDTO.getSailorCount()));
        } else {
            sailorsContainer.removeFromParent();
        }
        if(yearDTO.getCountryCount() > 0) {
            countriesCount.setInnerText(i18n.countriesCount(yearDTO.getCountryCount()));
        } else {
            countriesContainer.removeFromParent();
        }
        if(yearDTO.getTrackedRacesCount() > 0) {
            trackedRacesCount.setInnerText(i18n.trackedRacesCount(yearDTO.getTrackedRacesCount()));
        } else {
            trackedRacesContainer.removeFromParent();
        }
        for (EventListEventDTO eventDTO : events) {
            PlaceNavigation<EventDefaultPlace> eventNavigation = navigator.getEventNavigation(eventDTO.getId().toString(), eventDTO.getBaseURL(), eventDTO.isOnRemoteServer());
            RecentEventTeaser recentEvent = new RecentEventTeaser(eventNavigation, eventDTO, eventDTO.getState().getListStateMarker());
            recentEventsTeaserPanel.add(recentEvent);
        }
        headerDiv.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onHeaderCicked();
            }
        }, ClickEvent.getType());

        isContentVisible = showInitial;
        animation = new CollapseAnimation(contentDiv, showInitial);
        updateAccordionState();
    }

    private void onHeaderCicked() {
        isContentVisible = !isContentVisible;
        updateContentVisibility();
    }
    
    private void updateContentVisibility() {
        animation.animate(isContentVisible);
        updateAccordionState();
    }

    private void updateAccordionState() {
        if(isContentVisible) {
            getElement().removeClassName(EventsOverviewRecentResources.INSTANCE.css().accordioncollapsed());
        } else {
            getElement().addClassName(EventsOverviewRecentResources.INSTANCE.css().accordioncollapsed());
        }
    }
}
