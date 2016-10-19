package com.sap.sailing.gwt.ui.raceboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapSettings;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.ui.shared.MarkPassingTimesDTO;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.client.player.Timer.PlayModes;

/**
 * Puts the race viewer in a mode where the user can see what may be called the "Winning Lanes." For this,
 * the timer is set to the point in time when the first competitor finishes the race, or, for live races,
 * to the current point in time. The tail length is chosen such that it covers the full track of the
 * competitor farthest ahead.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class WinningLanesMode extends RaceBoardModeWithPerRaceCompetitors {
    private Duration tailLength;
    
    private boolean adjustedLeaderboardSettings;
    
    private boolean adjustedCompetitorSelection;

    @Override
    protected void updateCompetitorSelection() {
        final int howManyCompetitorsToSelect = getHowManyCompetitorsToSelect(getLeaderboardPanel().getCompetitors(getRaceIdentifier()));
        updateCompetitorSelection(howManyCompetitorsToSelect, getLeaderboardForSpecificTimePoint());
    }

    private void adjustLeaderboardSettings() {
        final LeaderboardSettings existingSettings = getLeaderboardPanel().getSettings();
        final List<DetailType> raceDetailsToShow = new ArrayList<>(existingSettings.getRaceDetailsToShow());
        raceDetailsToShow.add(DetailType.RACE_AVERAGE_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS);
        raceDetailsToShow.add(DetailType.RACE_AVERAGE_SIGNED_CROSS_TRACK_ERROR_IN_METERS);
        raceDetailsToShow.add(DetailType.RACE_DISTANCE_TRAVELED);
        raceDetailsToShow.add(DetailType.RACE_TIME_TRAVELED);
        raceDetailsToShow.remove(DetailType.DISPLAY_LEGS);
        final LeaderboardSettings newSettings = new LeaderboardSettings(
                Util.cloneListOrNull(existingSettings.getManeuverDetailsToShow()),
                Util.cloneListOrNull(existingSettings.getLegDetailsToShow()),
                raceDetailsToShow,
                Util.cloneListOrNull(existingSettings.getOverallDetailsToShow()),
                Util.cloneListOrNull(existingSettings.getNamesOfRaceColumnsToShow()),
                Util.cloneListOrNull(existingSettings.getNamesOfRacesToShow()),
                existingSettings.getNumberOfLastRacesToShow(), /* auto-expand pre-selected race */ true,
                existingSettings.getDelayBetweenAutoAdvancesInMilliseconds(),
                existingSettings.getNameOfRaceToSort(), existingSettings.isSortAscending(),
                existingSettings.isUpdateUponPlayStateChange(),
                existingSettings.getActiveRaceColumnSelectionStrategy(),
                existingSettings.isShowAddedScores(),
                existingSettings.isShowOverallColumnWithNumberOfRacesCompletedPerCompetitor(),
                existingSettings.isShowCompetitorSailIdColumn(),
                existingSettings.isShowCompetitorFullNameColumn());
        getLeaderboardPanel().updateSettings(newSettings);
    }

    private void adjustMapSettings() {
        final RaceMapSettings existingMapSettings = getRaceBoardPanel().getMap().getSettings();
        final RaceMapSettings newMapSettings = new RaceMapSettings(existingMapSettings.getZoomSettings(),
                existingMapSettings.getHelpLinesSettings(),
                existingMapSettings.getTransparentHoverlines(),
                existingMapSettings.getHoverlineStrokeWeight(),
                tailLength.asMillis(),
                /* existingMapSettings.isWindUp() */ true,
                existingMapSettings.getBuoyZoneRadius(),
                /* existingMapSettings.isShowOnlySelectedCompetitors() */ true, // show the top n competitors and their tails quickly
                existingMapSettings.isShowSelectedCompetitorsInfo(),
                existingMapSettings.isShowWindStreamletColors(),
                existingMapSettings.isShowWindStreamletOverlay(),
                existingMapSettings.isShowSimulationOverlay(),
                existingMapSettings.isShowMapControls(),
                existingMapSettings.getManeuverTypesToShow(),
                existingMapSettings.isShowDouglasPeuckerPoints());
        getRaceBoardPanel().getMap().updateSettings(newMapSettings);
    }

    /**
     * Based on the set of competitors in the leaderboard, determines how many competitors to select for the "winning lanes"
     * view. The number is determined to be at least one tenth of the number of competitors, but at least one if there are
     * one or more competitors.
     */
    private int getHowManyCompetitorsToSelect(Iterable<CompetitorDTO> competitors) {
        final int numberOfCompetitors = Util.size(competitors);
        return numberOfCompetitors==0 ? 0 : numberOfCompetitors<=4 ? 1 : numberOfCompetitors <=9 ? 2 : 3;
    }

    @Override
    protected void trigger() {
        final Date startOfRace;
        if (getRaceTimesInfoForRace() != null && (startOfRace=getRaceTimesInfoForRace().startOfRace) != null) {
            if (getTimer().getPlayMode() == PlayModes.Live) {
                // adjust tail length such that for the leading boat the tail is shown since the start time point
                tailLength = new MillisecondsTimePoint(startOfRace).until(getTimer().getLiveTimePoint());
            } else {
                final List<MarkPassingTimesDTO> markPassingTimes = getRaceTimesInfoForRace().getMarkPassingTimes();
                final Date firstPassingOfLastWaypointPassed = markPassingTimes == null || markPassingTimes.isEmpty() ? null :
                    markPassingTimes.get(markPassingTimes.size()-1).firstPassingDate;
                final Date endOfRace = getRaceTimesInfoForRace().endOfRace;
                final Date end = firstPassingOfLastWaypointPassed != null ? firstPassingOfLastWaypointPassed :
                    endOfRace != null ? endOfRace : getRaceTimesInfoForRace().endOfTracking;
                if (end != null) {
                    tailLength = new MillisecondsTimePoint(startOfRace).until(new MillisecondsTimePoint(end));
                    getTimer().setTime(end.getTime());
                }
            }
            // we've done our adjustments; remove listener and let go
            stopReceivingRaceTimesInfos();
        }
        if (!adjustedLeaderboardSettings && getLeaderboard() != null) {
            adjustedLeaderboardSettings = true;
            // it's important to first unregister the listener before updateSettings is called because
            // updateSettings will trigger another leaderboard load, leading to an endless recursion otherwise
            stopReceivingLeaderboard();
            adjustLeaderboardSettings();
        }
        if (adjustedLeaderboardSettings && tailLength != null) {
            adjustMapSettings();
        }
        if (getLeaderboardForSpecificTimePoint() == null && tailLength != null && getLeaderboard() != null && getRaceColumn() != null) {
            loadLeaderboardForSpecificTimePoint(getLeaderboard().name, getRaceColumn().getName(), getTimer().getTime());
        }
        if (!adjustedCompetitorSelection && getLeaderboardForSpecificTimePoint() != null && getCompetitorsInRace() != null) {
            stopReceivingCompetitorsInRace();
            adjustedCompetitorSelection = true;
        }
        updateCompetitorSelection();
    }

}
