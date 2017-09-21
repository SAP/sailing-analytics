package com.sap.sailing.gwt.settings.client.leaderboard;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.settings.generic.support.SettingsUtil;

public class MultiRaceLeaderboardPanelLifecycle extends LeaderboardPanelLifecycle<MultiRaceLeaderboardSettings> {

    protected final List<String> namesOfRaceColumns;
    
    public MultiRaceLeaderboardPanelLifecycle(AbstractLeaderboardDTO leaderboard, StringMessages stringMessages) {
        super(stringMessages);
        this.namesOfRaceColumns = leaderboard != null ? leaderboard.getNamesOfRaceColumns() : new ArrayList<String>();
    }
    @Override
    public MultiRaceLeaderboardSettings extractUserSettings(MultiRaceLeaderboardSettings currentLeaderboardSettings) {
        MultiRaceLeaderboardSettings defaultLeaderboardSettings = createDefaultSettings();
        MultiRaceLeaderboardSettings globalLeaderboardSettings = new MultiRaceLeaderboardSettings(
                currentLeaderboardSettings.getManeuverDetailsToShow(), currentLeaderboardSettings.getLegDetailsToShow(),
                currentLeaderboardSettings.getRaceDetailsToShow(), currentLeaderboardSettings.getOverallDetailsToShow(),
                defaultLeaderboardSettings.getNamesOfRaceColumnsToShow(),
                currentLeaderboardSettings.getNumberOfLastRacesToShow(),
                currentLeaderboardSettings.getDelayBetweenAutoAdvancesInMilliseconds(),
                currentLeaderboardSettings.getActiveRaceColumnSelectionStrategy(),
                currentLeaderboardSettings.isShowAddedScores(),
                currentLeaderboardSettings.isShowOverallColumnWithNumberOfRacesCompletedPerCompetitor(),
                currentLeaderboardSettings.isShowCompetitorShortNameColumn(),
                currentLeaderboardSettings.isShowCompetitorFullNameColumn(),
                currentLeaderboardSettings.isShowCompetitorBoatInfoColumn(),
                currentLeaderboardSettings.isShowCompetitorNationality());
        return SettingsUtil.copyValues(globalLeaderboardSettings, defaultLeaderboardSettings);
    }
    
    @Override
    public MultiRaceLeaderboardSettings extractDocumentSettings(MultiRaceLeaderboardSettings currentLeaderboardSettings) {
        MultiRaceLeaderboardSettings defaultLeaderboardSettings = createDefaultSettings();
        MultiRaceLeaderboardSettings contextSpecificLeaderboardSettings = new MultiRaceLeaderboardSettings(
                currentLeaderboardSettings.getManeuverDetailsToShow(), currentLeaderboardSettings.getLegDetailsToShow(),
                currentLeaderboardSettings.getRaceDetailsToShow(), currentLeaderboardSettings.getOverallDetailsToShow(),
                defaultLeaderboardSettings.getNamesOfRaceColumnsToShow(),
                currentLeaderboardSettings.getNumberOfLastRacesToShow(),
                currentLeaderboardSettings.getDelayBetweenAutoAdvancesInMilliseconds(),
                currentLeaderboardSettings.getActiveRaceColumnSelectionStrategy(),
                currentLeaderboardSettings.isShowAddedScores(),
                currentLeaderboardSettings.isShowOverallColumnWithNumberOfRacesCompletedPerCompetitor(),
                currentLeaderboardSettings.isShowCompetitorShortNameColumn(),
                currentLeaderboardSettings.isShowCompetitorFullNameColumn(),
                currentLeaderboardSettings.isShowCompetitorBoatInfoColumn(),
                currentLeaderboardSettings.isShowCompetitorNationality());
        return SettingsUtil.copyValues(contextSpecificLeaderboardSettings, defaultLeaderboardSettings);
    }
    
    @Override
    public MultiRaceLeaderboardSettingsDialogComponent getSettingsDialogComponent(MultiRaceLeaderboardSettings settings) {
        return new MultiRaceLeaderboardSettingsDialogComponent(settings, namesOfRaceColumns, stringMessages, false);
    }

    @Override
    public MultiRaceLeaderboardSettings createDefaultSettings() {
        return LeaderboardSettingsFactory.getInstance()
                .createNewDefaultSettingsWithRaceColumns(namesOfRaceColumns);
    }
}
