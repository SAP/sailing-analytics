package com.sap.sailing.gwt.autoplay.client.app;

import java.util.Map;

public interface PlaceNavigator {
    void goToStart();
    void goToPlayer(String eventUuidAsString, String locale, boolean fullscreen, 
            Map<String, String> leaderboardParameters, Map<String, String> raceboardParameters); 
}
