package com.sap.sailing.gwt.ui.raceboard;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettingsFactory;
import com.sap.sailing.gwt.settings.client.leaderboard.SingleRaceLeaderboardSettings;
import com.sap.sailing.gwt.ui.leaderboard.SingleRaceLeaderboardPanel;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.security.ui.settings.ComponentContextWithSettingsStorageAndAdditionalSettingsLayers.OnSettingsReloadedCallback;

/**
 * This mode best applies to non-live "replay" races. It sets the time to the "end of race" time point and makes sure
 * the race column in the leaderboard is expanded. The timer is put into {@link PlayStates#Paused} mode.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class FullAnalysisMode extends AbstractRaceBoardMode {
    private boolean leaderboardSettingsAdjusted;
    private boolean timerAdjusted;
    
    /**
     * Listening for race times info continues until information about the end of the race has been received. The leaderboard
     * settings are adjusted as soon as a valid leaderboard has been received.
     */
    @Override
    protected void trigger() {
        if (!timerAdjusted && getRaceTimesInfoForRace() != null && getRaceTimesInfoForRace().endOfRace != null) {
            timerAdjusted = true;
            stopReceivingRaceTimesInfos();
            if (getTimer().getPlayMode() == PlayModes.Live) {
                getTimer().setPlayMode(PlayModes.Replay);
            }
            getTimer().setTime(getRaceTimesInfoForRace().endOfRace.getTime());
        }
        if (!leaderboardSettingsAdjusted && getLeaderboard() != null) {
            leaderboardSettingsAdjusted = true;
            // it's important to first unregister the listener before updateSettings is called because
            // updateSettings will trigger another leaderboard load, leading to an endless recursion otherwise
            stopReceivingLeaderboard();
            adjustLeaderboardSettings();
        }
    }

    private void adjustLeaderboardSettings() {
        final SingleRaceLeaderboardPanel leaderboardPanel = getLeaderboardPanel();
        final List<DetailType> raceDetailsToShow = new ArrayList<>();
        raceDetailsToShow.add(DetailType.DISPLAY_LEGS);
        raceDetailsToShow.add(DetailType.RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS);
        raceDetailsToShow.add(DetailType.RACE_DISTANCE_TRAVELED);
        raceDetailsToShow.add(DetailType.RACE_GAP_TO_LEADER_IN_SECONDS);
        final SingleRaceLeaderboardSettings additiveSettings = LeaderboardSettingsFactory.getInstance().createNewSettingsWithCustomRaceDetails(raceDetailsToShow);
        ((RaceBoardComponentContext) leaderboardPanel.getComponentContext()).addModesPatching(leaderboardPanel, additiveSettings, new OnSettingsReloadedCallback<SingleRaceLeaderboardSettings>() {

            @Override
            public void onSettingsReloaded(SingleRaceLeaderboardSettings patchedSettings) {
                LeaderboardSettings settingsToUse = LeaderboardSettingsFactory.getInstance().createSettingsWithCustomExpandPreselectedRaceState(patchedSettings, true);
                leaderboardPanel.updateSettings(settingsToUse);
            }
            
        });
    }
}
