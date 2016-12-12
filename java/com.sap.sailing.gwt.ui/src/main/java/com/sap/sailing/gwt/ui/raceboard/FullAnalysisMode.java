package com.sap.sailing.gwt.ui.raceboard;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardSettingsFactory;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;

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
        if (!leaderboardSettingsAdjusted && getLeaderboard() != null) {
            leaderboardSettingsAdjusted = true;
            // it's important to first unregister the listener before updateSettings is called because
            // updateSettings will trigger another leaderboard load, leading to an endless recursion otherwise
            stopReceivingLeaderboard();
            adjustLeaderboardSettings();
        }
        if (!timerAdjusted && getRaceTimesInfoForRace() != null && getRaceTimesInfoForRace().endOfRace != null) {
            timerAdjusted = true;
            stopReceivingRaceTimesInfos();
            if (getTimer().getPlayMode() == PlayModes.Live) {
                getTimer().setPlayMode(PlayModes.Replay);
            }
            getTimer().setTime(getRaceTimesInfoForRace().endOfRace.getTime());
        }
    }

    private void adjustLeaderboardSettings() {
        final LeaderboardSettings existingSettings = getLeaderboardPanel().getSettings();
        final List<DetailType> raceDetailsToShow = new ArrayList<>(existingSettings.getRaceDetailsToShow());
        raceDetailsToShow.add(DetailType.RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS);
        raceDetailsToShow.add(DetailType.RACE_DISTANCE_TRAVELED);
        raceDetailsToShow.add(DetailType.RACE_GAP_TO_LEADER_IN_SECONDS);
        final LeaderboardSettings newSettings = LeaderboardSettingsFactory.getInstance().overrideDefaultValuesForRaceDetails(existingSettings, raceDetailsToShow);
        getLeaderboardPanel().updateSettings(newSettings);
    }
}
