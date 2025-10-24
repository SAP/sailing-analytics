package com.sap.sailing.gwt.home.desktop.partials.eventsrecent;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.eventlist.EventListEventDTO;
import com.sap.sailing.gwt.home.communication.eventlist.EventListEventSeriesDTO;
import com.sap.sailing.gwt.home.communication.eventlist.EventListYearDTO;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.partials.statistics.DesktopStatisticsBoxView;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.statistics.YearStatisticsBox;
import com.sap.sailing.gwt.home.shared.places.event.EventDefaultPlace;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesContext;
import com.sap.sailing.gwt.home.shared.utils.CollapseAnimation;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class EventsOverviewRecentYear extends Composite {

    interface EventsOverviewUiBinder extends UiBinder<Widget, EventsOverviewRecentYear> {
    }
    
    private static EventsOverviewUiBinder uiBinder = GWT.create(EventsOverviewUiBinder.class);

    @UiField SpanElement year;
    @UiField SpanElement eventsCount;
    @UiField FlowPanel recentEventsTeaserPanel;
    @UiField DivElement contentDiv;
    @UiField HTMLPanel headerDiv;
    @UiField StringMessages i18n;
    
    private boolean isContentVisible;
    
    private final CollapseAnimation animation;
    
    public EventsOverviewRecentYear(EventListYearDTO yearDTO, DesktopPlacesNavigator navigator, boolean showInitial, TakedownNoticeService takedownNoticeService) {
        List<EventListEventDTO> events = yearDTO.getEvents();
        EventsOverviewRecentResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        this.year.setInnerText(String.valueOf(yearDTO.getYear()));
        this.eventsCount.setInnerText(i18n.eventsCount(yearDTO.getEventCount()));
        for (EventListEventDTO eventDTO : events) {
            final PlaceNavigation<EventDefaultPlace> eventNavigation = navigator.getEventNavigation(eventDTO.getId().toString(), eventDTO.getBaseURL(), eventDTO.isOnRemoteServer());
            final RecentEventTeaser recentEvent = new RecentEventTeaser(eventNavigation, eventDTO, eventDTO.getState().getListStateMarker(), takedownNoticeService);
            final EventListEventSeriesDTO eventSeriesData = eventDTO.getEventSeries();
            if (eventSeriesData != null) {
                final SeriesContext ctx = SeriesContext
                        .createWithLeaderboardGroupId(eventSeriesData.getSeriesLeaderboardGroupId());
                final String baseUrl = eventDTO.getBaseURL();
                final PlaceNavigation<?> seriesNavigation = navigator.getEventSeriesNavigation(ctx, baseUrl, eventDTO.isOnRemoteServer());
                recentEvent.setSeriesInformation(seriesNavigation, eventDTO.getEventSeries());
            }
            recentEventsTeaserPanel.add(recentEvent);
        }
        recentEventsTeaserPanel.add(new YearStatisticsBox(new DesktopStatisticsBoxView(true,
                StringMessages.INSTANCE.statisticsFor(Integer.toString(yearDTO.getYear()))), yearDTO));
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
