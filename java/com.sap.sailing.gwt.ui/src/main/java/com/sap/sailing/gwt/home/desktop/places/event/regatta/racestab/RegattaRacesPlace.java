package com.sap.sailing.gwt.home.desktop.places.event.regatta.racestab;

import com.google.gwt.place.shared.Prefix;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.AbstractEventRegattaPlace;
import com.sap.sailing.gwt.home.shared.app.HasMobileVersion;
import com.sap.sailing.gwt.home.shared.places.PlaceTokenPrefixes;
import com.sap.sailing.gwt.home.shared.places.event.AbstractEventPlace;
import com.sap.sailing.gwt.home.shared.places.event.EventContext;

public class RegattaRacesPlace extends AbstractEventRegattaPlace implements HasMobileVersion {
    public RegattaRacesPlace(String id, String leaderboardName) {
        super(id, leaderboardName);
    }
    
    public RegattaRacesPlace(EventContext context) {
        super(context);
    }

    @Override
    public AbstractEventRegattaPlace newInstanceWithContext(EventContext ctx) {
        return new RegattaRacesPlace(ctx);
    }

    @Prefix(PlaceTokenPrefixes.RegattaRaces)
    public static class Tokenizer extends AbstractEventPlace.Tokenizer<RegattaRacesPlace> {
        @Override
        protected RegattaRacesPlace getRealPlace(EventContext context) {
            return new RegattaRacesPlace(context);
        }
    }
}
