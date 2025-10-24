package com.sap.sailing.gwt.home.mobile.places.series;

import java.util.Collection;

import com.google.gwt.user.client.ui.IsWidget;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystem;
import com.sap.sailing.gwt.home.communication.event.EventAndLeaderboardReferenceWithStateDTO;
import com.sap.sailing.gwt.home.communication.fakeseries.EventSeriesViewDTO;
import com.sap.sailing.gwt.home.mobile.places.SeriesLeaderboardNavigationProvider;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesContext;
import com.sap.sse.security.ui.client.UserService;

public interface SeriesView extends IsWidget {

    void setQuickFinderValues(String seriesName, Collection<EventAndLeaderboardReferenceWithStateDTO> eventsOfSeries);

    public interface Presenter extends OverallLeaderboardNavigationProvider, SeriesLeaderboardNavigationProvider {
        SeriesContext getCtx();

        SailingDispatchSystem getDispatch();

        PlaceNavigation<?> getEventNavigation(String eventId);

        EventSeriesViewDTO getSeriesDTO();
        
        UserService getUserService();
    }
}

