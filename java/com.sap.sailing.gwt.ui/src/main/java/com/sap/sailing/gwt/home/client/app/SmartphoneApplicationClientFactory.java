package com.sap.sailing.gwt.home.client.app;

import java.util.List;

import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.home.client.place.event.EventView;
import com.sap.sailing.gwt.home.client.place.event.EventWithoutRegattasView;
import com.sap.sailing.gwt.home.client.place.event.TabletAndDesktopEventView;
import com.sap.sailing.gwt.home.client.place.event.TabletAndDesktopEventWithoutRegattasView;
import com.sap.sailing.gwt.home.client.place.events.EventsActivity;
import com.sap.sailing.gwt.home.client.place.events.EventsView;
import com.sap.sailing.gwt.home.client.place.events.TabletAndDesktopEventsView;
import com.sap.sailing.gwt.home.client.place.searchresult.SearchResultView;
import com.sap.sailing.gwt.home.client.place.searchresult.TabletAndDesktopSearchResultView;
import com.sap.sailing.gwt.home.client.place.solutions.SolutionsActivity;
import com.sap.sailing.gwt.home.client.place.solutions.SolutionsView;
import com.sap.sailing.gwt.home.client.place.solutions.TabletAndDesktopSolutionsView;
import com.sap.sailing.gwt.home.client.place.sponsoring.SponsoringActivity;
import com.sap.sailing.gwt.home.client.place.sponsoring.SponsoringView;
import com.sap.sailing.gwt.home.client.place.sponsoring.TabletAndDesktopSponsoringView;
import com.sap.sailing.gwt.home.client.place.start.StartView;
import com.sap.sailing.gwt.home.client.place.start.TabletAndDesktopStartView;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupDTO;
import com.sap.sse.gwt.client.player.Timer;


public class SmartphoneApplicationClientFactory extends AbstractApplicationClientFactory implements ApplicationClientFactory {
    public SmartphoneApplicationClientFactory() {
        this(new SimpleEventBus());
    }
    
    private SmartphoneApplicationClientFactory(EventBus eventBus) {
        this(eventBus, new PlaceController(eventBus));
    }

    private SmartphoneApplicationClientFactory(EventBus eventBus, PlaceController placeController) {
        super(new TabletAndDesktopApplicationView(new PlaceNavigatorImpl(placeController)), eventBus, placeController);
    }

    @Override
    public EventView createEventView(EventDTO event, List<RaceGroupDTO> raceGroups, String leaderboardName, Timer timerForClientServerOffset) {
        return new TabletAndDesktopEventView(getSailingService(), event, raceGroups, leaderboardName, timerForClientServerOffset);
    }

    @Override
    public EventWithoutRegattasView createEventWithoutRegattasView(EventDTO event) {
        return new TabletAndDesktopEventWithoutRegattasView(getSailingService(), event);
    }

    @Override
    public EventsView createEventsView(EventsActivity activity) {
        return new TabletAndDesktopEventsView(getPlaceNavigator());
    }

    @Override
    public StartView createStartView() {
        return new TabletAndDesktopStartView(getPlaceNavigator());
    }

    @Override
    public SponsoringView createSponsoringView(SponsoringActivity activity) {
        return new TabletAndDesktopSponsoringView();
    }

    @Override
    public SolutionsView createSolutionsView(SolutionsActivity activity) {
        return new TabletAndDesktopSolutionsView();
    }

    @Override
    public SearchResultView createSearchResultView() {
        return new TabletAndDesktopSearchResultView(getPlaceNavigator());
    }
}
