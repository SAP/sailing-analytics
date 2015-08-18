package com.sap.sailing.gwt.home.client.place.event.regatta.tabs;

import com.google.gwt.place.shared.Prefix;
import com.sap.sailing.gwt.home.client.place.event.AbstractEventPlace;
import com.sap.sailing.gwt.home.client.place.event.EventContext;
import com.sap.sailing.gwt.home.client.place.event.EventPrefixes;
import com.sap.sailing.gwt.home.client.place.event.regatta.AbstractEventRegattaPlace;
import com.sap.sailing.gwt.home.shared.ExperimentalFeatures;
import com.sap.sailing.gwt.home.shared.app.MobileSupport;

public class RegattaRacesPlace extends AbstractEventRegattaPlace implements MobileSupport {
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

    @Prefix(EventPrefixes.RegattaRaces)
    public static class Tokenizer extends AbstractEventPlace.Tokenizer<RegattaRacesPlace> {
        @Override
        protected RegattaRacesPlace getRealPlace(EventContext context) {
            return new RegattaRacesPlace(context);
        }
    }
    
    @Override
    public boolean hasMobileVersion() {
        return ExperimentalFeatures.SHOW_RACES_ON_MOBILE;
    }
}
