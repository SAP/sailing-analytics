package com.sap.sailing.gwt.home.client.app;

import java.util.List;

import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.home.client.place.error.TabletAndDesktopErrorView;
import com.sap.sailing.gwt.home.client.place.event.EventPlace.EventNavigationTabs;
import com.sap.sailing.gwt.home.client.place.event.EventView;
import com.sap.sailing.gwt.home.client.place.event.EventWithoutRegattasView;
import com.sap.sailing.gwt.home.client.place.event.TabletAndDesktopEventView;
import com.sap.sailing.gwt.home.client.place.event.TabletAndDesktopEventWithoutRegattasView;
import com.sap.sailing.gwt.home.client.place.events.EventsActivity;
import com.sap.sailing.gwt.home.client.place.events.EventsView;
import com.sap.sailing.gwt.home.client.place.events.TabletAndDesktopEventsView;
import com.sap.sailing.gwt.home.client.place.leaderboard.AnalyticsView;
import com.sap.sailing.gwt.home.client.place.leaderboard.TabletAndDesktopLeaderboardView;
import com.sap.sailing.gwt.home.client.place.searchresult.SearchResultView;
import com.sap.sailing.gwt.home.client.place.searchresult.TabletAndDesktopSearchResultView;
import com.sap.sailing.gwt.home.client.place.solutions.SolutionsView;
import com.sap.sailing.gwt.home.client.place.solutions.TabletAndDesktopSolutionsView;
import com.sap.sailing.gwt.home.client.place.solutions.analytics.SailingAnalyticsView;
import com.sap.sailing.gwt.home.client.place.solutions.analytics.TabletAndDesktopSailingAnalyticsView;
import com.sap.sailing.gwt.home.client.place.sponsoring.SponsoringView;
import com.sap.sailing.gwt.home.client.place.sponsoring.TabletAndDesktopSponsoringView;
import com.sap.sailing.gwt.home.client.place.start.StartView;
import com.sap.sailing.gwt.home.client.place.start.TabletAndDesktopStartView;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupDTO;
import com.sap.sse.gwt.client.player.Timer;


public class TabletAndDesktopApplicationClientFactory extends AbstractApplicationClientFactory implements ApplicationClientFactory {
    public TabletAndDesktopApplicationClientFactory() {
        this(new SimpleEventBus());
    }
    
    private TabletAndDesktopApplicationClientFactory(EventBus eventBus) {
        this(eventBus, new PlaceController(eventBus));
    }

    private TabletAndDesktopApplicationClientFactory(EventBus eventBus, PlaceController placeController) {
        super(new TabletAndDesktopApplicationView(new HomePlacesNavigator(placeController)), eventBus, placeController);
    }

    @Override
    public EventView createEventView(EventDTO event, EventNavigationTabs navigationTab, List<RaceGroupDTO> raceGroups, String leaderboardName, Timer timerForClientServerOffset) {
        return new TabletAndDesktopEventView(getSailingService(), event, navigationTab, raceGroups, leaderboardName, timerForClientServerOffset, getHomePlacesNavigator());
    }

    @Override
    public TabletAndDesktopErrorView createErrorView(String errorMessage, Throwable errorReason) {
        return new TabletAndDesktopErrorView(errorMessage, errorReason);
    }

    @Override
    public EventWithoutRegattasView createEventWithoutRegattasView(EventDTO event) {
        return new TabletAndDesktopEventWithoutRegattasView(getSailingService(), event);
    }

    @Override
    public EventsView createEventsView(EventsActivity activity) {
        return new TabletAndDesktopEventsView(getHomePlacesNavigator());
    }

    @Override
    public StartView createStartView() {
        return new TabletAndDesktopStartView(getHomePlacesNavigator());
    }

    @Override
    public SponsoringView createSponsoringView() {
        return new TabletAndDesktopSponsoringView();
    }

    @Override
    public SolutionsView createSolutionsView() {
        return new TabletAndDesktopSolutionsView(getHomePlacesNavigator());
    }

    @Override
    public AnalyticsView createLeaderboardView(EventDTO event, String leaderboardName, Timer timerForClientServerOffset) {
        return new TabletAndDesktopLeaderboardView(event, leaderboardName, timerForClientServerOffset, getHomePlacesNavigator());
    }

    @Override
    public SearchResultView createSearchResultView() {
        return new TabletAndDesktopSearchResultView(getHomePlacesNavigator());
    }

    @Override
    public SailingAnalyticsView createSailingAnalyticsView() {
        return new TabletAndDesktopSailingAnalyticsView();
    }
}
