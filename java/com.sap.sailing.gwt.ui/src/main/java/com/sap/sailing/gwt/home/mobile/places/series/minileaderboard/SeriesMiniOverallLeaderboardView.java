package com.sap.sailing.gwt.home.mobile.places.series.minileaderboard;

import java.util.Collection;

import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystem;
import com.sap.sailing.gwt.home.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.home.communication.fakeseries.EventSeriesViewDTO;
import com.sap.sailing.gwt.home.mobile.places.SeriesLeaderboardNavigationProvider;
import com.sap.sailing.gwt.home.mobile.places.series.OverallLeaderboardNavigationProvider;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesContext;

public interface SeriesMiniOverallLeaderboardView {

    public interface Presenter extends OverallLeaderboardNavigationProvider, SeriesLeaderboardNavigationProvider {
        SailingDispatchSystem getDispatch();

        SeriesContext getCtx();

        PlaceNavigation<?> getSeriesNavigation();

        EventSeriesViewDTO getSeriesDTO();
    }

    Widget asWidget();
    
    void setQuickFinderValues(String seriesName, Collection<EventMetadataDTO> eventsOfSeries);
}
