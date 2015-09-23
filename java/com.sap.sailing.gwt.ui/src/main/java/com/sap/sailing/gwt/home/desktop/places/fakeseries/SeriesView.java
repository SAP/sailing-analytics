package com.sap.sailing.gwt.home.desktop.places.fakeseries;

import java.util.UUID;

import com.google.gwt.place.shared.Place;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabView;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.event.EventDefaultPlace;
import com.sap.sailing.gwt.home.shared.places.events.EventsPlace;
import com.sap.sailing.gwt.home.shared.places.fakeseries.AbstractSeriesPlace;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesContext;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesDefaultPlace;
import com.sap.sailing.gwt.home.shared.places.start.StartPlace;
import com.sap.sailing.gwt.ui.shared.media.MediaDTO;
import com.sap.sse.gwt.client.player.Timer;

public interface SeriesView<PLACE extends AbstractSeriesPlace, PRES extends SeriesView.Presenter> extends IsWidget {

    public interface Presenter {
        SeriesContext getCtx();

        void handleTabPlaceSelection(TabView<?, ? extends Presenter> selectedActivity);

        SafeUri getUrl(AbstractSeriesPlace place);
        void navigateTo(Place place);
        
        PlaceNavigation<StartPlace> getHomeNavigation();
        PlaceNavigation<EventsPlace> getEventsNavigation();
        PlaceNavigation<SeriesDefaultPlace> getCurrentEventSeriesNavigation();

        PlaceNavigation<EventDefaultPlace> getEventNavigation(UUID eventId);
        
        void ensureMedia(AsyncCallback<MediaDTO> asyncCallback);
        boolean hasMedia();
        
        Timer getAutoRefreshTimer();
    }
    
    /**
     * This is the presenter the view can talk to.
     * 
     * @param currentPresenter
     */
    void registerPresenter(PRES currentPresenter);

    /**
     * Tell the view to process tabbar place navigation
     * 
     * @param place
     */
    void navigateTabsTo(PLACE place);
    
    void showErrorInCurrentTab(IsWidget errorView);
    
}
