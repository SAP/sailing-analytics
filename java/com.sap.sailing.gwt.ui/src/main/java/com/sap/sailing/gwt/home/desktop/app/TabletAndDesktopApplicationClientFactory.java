package com.sap.sailing.gwt.home.desktop.app;

import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.home.client.place.error.TabletAndDesktopErrorView;
import com.sap.sailing.gwt.home.client.place.events.EventsView;
import com.sap.sailing.gwt.home.client.place.events.TabletAndDesktopEventsView;
import com.sap.sailing.gwt.home.client.place.searchresult.SearchResultView;
import com.sap.sailing.gwt.home.client.place.searchresult.TabletAndDesktopSearchResultView;
import com.sap.sailing.gwt.home.client.place.solutions.SolutionsPlace.SolutionsNavigationTabs;
import com.sap.sailing.gwt.home.client.place.solutions.SolutionsView;
import com.sap.sailing.gwt.home.client.place.solutions.TabletAndDesktopSolutionsView;
import com.sap.sailing.gwt.home.client.place.sponsoring.SponsoringView;
import com.sap.sailing.gwt.home.client.place.sponsoring.TabletAndDesktopSponsoringView;
import com.sap.sailing.gwt.home.client.place.start.StartView;
import com.sap.sailing.gwt.home.client.place.start.TabletAndDesktopStartView;
import com.sap.sailing.gwt.home.client.place.whatsnew.TabletAndDesktopWhatsNewView;
import com.sap.sailing.gwt.home.client.place.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.client.place.whatsnew.WhatsNewView;
import com.sap.sailing.gwt.home.shared.dispatch.DispatchSystem;
import com.sap.sailing.gwt.home.shared.dispatch.DispatchSystemImpl;


public class TabletAndDesktopApplicationClientFactory extends AbstractApplicationClientFactory implements DesktopClientFactory {
    private final DispatchSystem dispatch = new DispatchSystemImpl();
    
    public TabletAndDesktopApplicationClientFactory() {
        this(new SimpleEventBus());
    }
    
    private TabletAndDesktopApplicationClientFactory(EventBus eventBus) {
        this(eventBus, new PlaceController(eventBus));
    }

    private TabletAndDesktopApplicationClientFactory(EventBus eventBus, PlaceController placeController) {
        super(new TabletAndDesktopApplicationView(new DesktopPlacesNavigator(placeController), eventBus), eventBus, placeController);
    }

    @Override
    public TabletAndDesktopErrorView createErrorView(String errorMessage, Throwable errorReason) {
        return new TabletAndDesktopErrorView(errorMessage, errorReason, null);
    }

    @Override
    public EventsView createEventsView() {
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
    public SolutionsView createSolutionsView(SolutionsNavigationTabs navigationTab) {
        return new TabletAndDesktopSolutionsView(navigationTab, getHomePlacesNavigator());
    }

    @Override
    public SearchResultView createSearchResultView() {
        return new TabletAndDesktopSearchResultView(getHomePlacesNavigator());
    }

    @Override
    public WhatsNewView createWhatsNewView(WhatsNewNavigationTabs navigationTab) {
        return new TabletAndDesktopWhatsNewView(navigationTab, getHomePlacesNavigator());
    }

    @Override
    public DispatchSystem getDispatch() {
        return dispatch;
    }
}
