package com.sap.sailing.gwt.home.desktop.places.events;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.partials.eventsrecent.EventsOverviewRecent;
import com.sap.sailing.gwt.home.desktop.partials.eventsupcoming.EventsOverviewUpcoming;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.events.EventsPlace;
import com.sap.sailing.gwt.home.shared.places.start.StartPlace;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.breadcrumb.BreadcrumbPane;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class TabletAndDesktopEventsView extends AbstractEventsView {
    private static EventsPageViewUiBinder uiBinder = GWT.create(EventsPageViewUiBinder.class);

    interface EventsPageViewUiBinder extends UiBinder<Widget, TabletAndDesktopEventsView> {
    }
    
    @UiField StringMessages i18n;
    @UiField(provided=true) EventsOverviewRecent recentEventsWidget;
    @UiField(provided=true) EventsOverviewUpcoming upcomingEventsWidget;
    @UiField BreadcrumbPane breadcrumbs;
    
    private final DesktopPlacesNavigator navigator;
    
    public TabletAndDesktopEventsView(DesktopPlacesNavigator navigator) {
        super();
        this.navigator = navigator;
        recentEventsWidget = new EventsOverviewRecent(navigator);
        upcomingEventsWidget = new EventsOverviewUpcoming(navigator);
        
        initWidget(uiBinder.createAndBindUi(this));
        
        initBreadCrumbs();
    }
    
    private void initBreadCrumbs() {
        breadcrumbs.getElement().getStyle().setDisplay(Display.NONE);
        final PlaceNavigation<StartPlace> homeNavigation = navigator.getHomeNavigation();
        breadcrumbs.addBreadcrumbItem(i18n.home(), homeNavigation.getTargetUrl(), new Runnable() {
            @Override
            public void run() {
                navigator.goToPlace(homeNavigation);
            }
        });
        final PlaceNavigation<EventsPlace> eventsNavigation = navigator.getEventsNavigation();
        breadcrumbs.addBreadcrumbItem(i18n.events(), eventsNavigation.getTargetUrl(), new Runnable() {
            @Override
            public void run() {
                navigator.goToPlace(eventsNavigation);
            }
        });
    }

    @Override 
    protected void updateEventsUI(TakedownNoticeService takedownNoticeService) {
        recentEventsWidget.updateEvents(eventListView.getRecentEvents(), takedownNoticeService);
        upcomingEventsWidget.updateEvents(eventListView.getUpcomingEvents());
    }
}
