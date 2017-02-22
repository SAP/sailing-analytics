package com.sap.sailing.gwt.home.desktop.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.Window;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettings.RaceColumnSelectionStrategies;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardUrlSettings;

public class EventParamUtils {
    public static LeaderboardSettings createLeaderboardSettingsFromURLParameters(Map<String, List<String>> parameterMap) {
        LeaderboardSettings result;
        Long refreshIntervalMillis = parameterMap.containsKey(LeaderboardUrlSettings.PARAM_REFRESH_INTERVAL_MILLIS) ? Long
                .valueOf(parameterMap.get(LeaderboardUrlSettings.PARAM_REFRESH_INTERVAL_MILLIS).get(0)) : null;
        RaceColumnSelectionStrategies raceColumnSelectionStrategy;
        final Integer numberOfLastRacesToShow;
        if (parameterMap.containsKey(LeaderboardUrlSettings.PARAM_NAME_LAST_N)) {
            raceColumnSelectionStrategy = RaceColumnSelectionStrategies.LAST_N;
            numberOfLastRacesToShow = Integer
                    .valueOf(parameterMap.get(LeaderboardUrlSettings.PARAM_NAME_LAST_N).get(0));
        } else if (WindowUtils.isSmallWidth()) {
            raceColumnSelectionStrategy = RaceColumnSelectionStrategies.LAST_N;
            int width = Window.getClientWidth();
            numberOfLastRacesToShow = (width - 275) / 40;
        } else {
            raceColumnSelectionStrategy = RaceColumnSelectionStrategies.EXPLICIT;
            numberOfLastRacesToShow = null;
        }
        if (parameterMap.containsKey(LeaderboardUrlSettings.PARAM_RACE_NAME)
                || parameterMap.containsKey(LeaderboardUrlSettings.PARAM_RACE_DETAIL)
                || parameterMap.containsKey(LeaderboardUrlSettings.PARAM_LEG_DETAIL)
                || parameterMap.containsKey(LeaderboardUrlSettings.PARAM_MANEUVER_DETAIL)
                || parameterMap.containsKey(LeaderboardUrlSettings.PARAM_OVERALL_DETAIL)
                || parameterMap.containsKey(LeaderboardUrlSettings.PARAM_SHOW_ADDED_SCORES)
                || parameterMap
                        .containsKey(LeaderboardUrlSettings.PARAM_SHOW_OVERALL_COLUMN_WITH_NUMBER_OF_RACES_COMPLETED)) {
            List<DetailType> maneuverDetails = getDetailTypeListFromParamValue(parameterMap
                    .get(LeaderboardUrlSettings.PARAM_MANEUVER_DETAIL));
            List<DetailType> raceDetails = getDetailTypeListFromParamValue(parameterMap
                    .get(LeaderboardUrlSettings.PARAM_RACE_DETAIL));
            List<DetailType> overallDetails = getDetailTypeListFromParamValue(parameterMap
                    .get(LeaderboardUrlSettings.PARAM_OVERALL_DETAIL));
            if (overallDetails.isEmpty()) {
                overallDetails = Collections.singletonList(DetailType.REGATTA_RANK);
            }
            List<DetailType> legDetails = getDetailTypeListFromParamValue(parameterMap
                    .get(LeaderboardUrlSettings.PARAM_LEG_DETAIL));
            List<String> namesOfRacesToShow = EventParamUtils.getStringListFromParamValue(parameterMap
                    .get(LeaderboardUrlSettings.PARAM_RACE_NAME));
            boolean showAddedScores = parameterMap.containsKey(LeaderboardUrlSettings.PARAM_SHOW_ADDED_SCORES) ? Boolean
                    .valueOf(parameterMap.get(LeaderboardUrlSettings.PARAM_SHOW_ADDED_SCORES).get(0)) : false;
            boolean showOverallColumnWithNumberOfRacesSailedPerCompetitor = parameterMap
                    .containsKey(LeaderboardUrlSettings.PARAM_SHOW_OVERALL_COLUMN_WITH_NUMBER_OF_RACES_COMPLETED) ? Boolean
                    .valueOf(parameterMap.get(
                            LeaderboardUrlSettings.PARAM_SHOW_OVERALL_COLUMN_WITH_NUMBER_OF_RACES_COMPLETED).get(0))
                    : false;
            boolean autoExpandPreSelectedRace = parameterMap
                    .containsKey(LeaderboardUrlSettings.PARAM_AUTO_EXPAND_PRESELECTED_RACE) ? Boolean
                    .valueOf(parameterMap.get(LeaderboardUrlSettings.PARAM_AUTO_EXPAND_PRESELECTED_RACE).get(0))
                    : (namesOfRacesToShow != null && namesOfRacesToShow.size() == 1);
            boolean showCompetitorSailIdColumn = true;
            boolean showCompetitorFullNameColumn = true;
            if (parameterMap.containsKey(LeaderboardUrlSettings.PARAM_SHOW_COMPETITOR_NAME_COLUMNS)) {
                String value = parameterMap.get(LeaderboardUrlSettings.PARAM_SHOW_COMPETITOR_NAME_COLUMNS).get(0);
                if (value.equals(LeaderboardUrlSettings.COMPETITOR_NAME_COLUMN_FULL_NAME)) {
                    showCompetitorSailIdColumn = false;
                } else if (value.equals(LeaderboardUrlSettings.COMPETITOR_NAME_COLUMN_SAIL_ID)) {
                    showCompetitorFullNameColumn = false;
                } else if (value.trim().equals("")) {
                    showCompetitorFullNameColumn = false;
                    showCompetitorSailIdColumn = false;
                }
            }
            result = new LeaderboardSettings(maneuverDetails, legDetails, raceDetails, overallDetails,
            /* namesOfRaceColumnsToShow */null, namesOfRacesToShow, numberOfLastRacesToShow, autoExpandPreSelectedRace,
                    refreshIntervalMillis, /* sort by column */
                    (namesOfRacesToShow != null && !namesOfRacesToShow.isEmpty()) ? namesOfRacesToShow.get(0) : null,
                    /* ascending */true, /* updateUponPlayStateChange */raceDetails.isEmpty() && legDetails.isEmpty(),
                    raceColumnSelectionStrategy, showAddedScores,
                    showOverallColumnWithNumberOfRacesSailedPerCompetitor, showCompetitorSailIdColumn,
                    showCompetitorFullNameColumn);
    
        } else {
            return new LeaderboardSettings(refreshIntervalMillis, numberOfLastRacesToShow, raceColumnSelectionStrategy);
        }
        return result;
    }
    
    private static List<DetailType> getDetailTypeListFromParamValue(List<String> list) {
        List<DetailType> result = new ArrayList<DetailType>();
        if (list != null) {
            for (String entry : list) {
                try {
                    result.add(DetailType.valueOf(entry));
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return result;
    }

    private static List<String> getStringListFromParamValue(List<String> list) {
        List<String> result = new ArrayList<String>();
        if (list != null) {
            result.addAll(list);
        }
        return result;
    }

}
