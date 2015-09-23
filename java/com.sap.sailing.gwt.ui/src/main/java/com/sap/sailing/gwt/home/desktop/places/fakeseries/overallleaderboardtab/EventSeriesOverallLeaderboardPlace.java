package com.sap.sailing.gwt.home.desktop.places.fakeseries.overallleaderboardtab;

import com.google.gwt.place.shared.Prefix;
import com.sap.sailing.gwt.home.client.place.event.EventPrefixes;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.AbstractSeriesTabPlace;
import com.sap.sailing.gwt.home.shared.places.fakeseries.AbstractSeriesPlace;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesContext;

public class EventSeriesOverallLeaderboardPlace extends AbstractSeriesTabPlace {
    public EventSeriesOverallLeaderboardPlace(String id) {
        super(id);
    }
    
    public EventSeriesOverallLeaderboardPlace(SeriesContext context) {
        super(context);
    }

    @Prefix(EventPrefixes.EventSeriesOverallLeaderboard)
    public static class Tokenizer extends AbstractSeriesPlace.Tokenizer<EventSeriesOverallLeaderboardPlace> {
        @Override
        protected EventSeriesOverallLeaderboardPlace getRealPlace(SeriesContext context) {
            return new EventSeriesOverallLeaderboardPlace(context);
        }
    }
}
