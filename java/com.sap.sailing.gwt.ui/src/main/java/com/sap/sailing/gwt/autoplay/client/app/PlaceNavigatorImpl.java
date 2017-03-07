package com.sap.sailing.gwt.autoplay.client.app;

import com.google.gwt.place.shared.PlaceController;
import com.sap.sailing.gwt.autoplay.client.place.player.AutoPlayerConfiguration;
import com.sap.sailing.gwt.autoplay.client.place.player.PlayerPlace;
import com.sap.sailing.gwt.autoplay.client.place.sixtyinch.base.ConfigurationSixtyInch;
import com.sap.sailing.gwt.autoplay.client.shared.leaderboard.LeaderboardWithHeaderPerspectiveLifecycle;
import com.sap.sailing.gwt.autoplay.client.shared.leaderboard.LeaderboardWithHeaderPerspectiveSettings;
import com.sap.sailing.gwt.ui.raceboard.RaceBoardPerspectiveLifecycle;
import com.sap.sailing.gwt.ui.raceboard.RaceBoardPerspectiveSettings;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveLifecycleWithAllSettings;

public class PlaceNavigatorImpl implements PlaceNavigator {
    private final PlaceController placeController;
    
    public PlaceNavigatorImpl(PlaceController placeController) {
        super();
        this.placeController = placeController;
    }

    @Override
    public void goToPlayer(
            AutoPlayerConfiguration playerConfig, 
            PerspectiveLifecycleWithAllSettings<LeaderboardWithHeaderPerspectiveLifecycle, LeaderboardWithHeaderPerspectiveSettings> leaderboardPerspectiveLifecycleWithAllSettings,
            PerspectiveLifecycleWithAllSettings<RaceBoardPerspectiveLifecycle, RaceBoardPerspectiveSettings> raceboardPerspectiveLifecyclesWithAllSettings) {
        PlayerPlace playerPlace = new PlayerPlace(playerConfig, leaderboardPerspectiveLifecycleWithAllSettings, raceboardPerspectiveLifecyclesWithAllSettings);
        placeController.goTo(playerPlace); 
    }

    @Override
    public void goToPlayerSixtyInch(ConfigurationSixtyInch configurationSixtyInch) {
        throw new IllegalStateException("Todo start player view");
    }

}
