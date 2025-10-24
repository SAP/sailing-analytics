package com.sap.sailing.gwt.ui.leaderboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.IncrementalOrFullLeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiRaceLeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiRaceLeaderboardSettingsDialogComponent;
import com.sap.sailing.gwt.ui.actions.GetLeaderboardByNameAction;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.filter.CompetitorRaceRankFilter;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.BinaryOperator;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncAction;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.AbstractSortableColumnWithMinMax;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.WithSecurity;

/**
 * This type of {@link LeaderboardPanel} shows a customizable set of leaderboard columns. In case of fleet racing it's
 * not possible to only show a physical race.
 */
public class MultiRaceLeaderboardPanel extends LeaderboardPanel<MultiRaceLeaderboardSettings> {

    public MultiRaceLeaderboardPanel(Component<?> parent, ComponentContext<?> context,
            SailingServiceAsync sailingService, AsyncActionsExecutor asyncActionsExecutor,
            MultiRaceLeaderboardSettings settings, boolean isEmbedded,
            CompetitorSelectionProvider competitorSelectionProvider, Timer timer,
            String leaderboardName, ErrorReporter errorReporter, StringMessages stringMessages, boolean showRaceDetails,
            CompetitorFilterPanel competitorSearchTextBox, boolean showSelectionCheckbox,
            RaceTimesInfoProvider optionalRaceTimesInfoProvider, boolean autoExpandLastRaceColumn,
            boolean adjustTimerDelay, boolean autoApplyTopNFilter, boolean showCompetitorFilterStatus,
            boolean enableSyncScroller, LeaderBoardStyle style, FlagImageResolver flagImageResolver, Iterable<DetailType> availableDetailTypes, WithSecurity sailingCF) {
        super(parent, context, sailingService, asyncActionsExecutor, settings, isEmbedded, competitorSelectionProvider,
                timer, leaderboardName, errorReporter, stringMessages, showRaceDetails,
                competitorSearchTextBox, showSelectionCheckbox, optionalRaceTimesInfoProvider, autoExpandLastRaceColumn,
                adjustTimerDelay, autoApplyTopNFilter, showCompetitorFilterStatus, enableSyncScroller, style, flagImageResolver, availableDetailTypes, sailingCF);
        initialize(settings);
    }

    public MultiRaceLeaderboardPanel(Component<?> parent, ComponentContext<?> context,
            SailingServiceAsync sailingService, AsyncActionsExecutor asyncActionsExecutor,
            MultiRaceLeaderboardSettings settings, CompetitorSelectionProvider competitorSelectionProvider,
            String leaderboardName, ErrorReporter errorReporter, StringMessages stringMessages,
            boolean showRaceDetails, LeaderBoardStyle style, FlagImageResolver flagImageResolver, Iterable<DetailType> availableDetailTypes, WithSecurity sailingCF) {
        super(parent, context, sailingService, asyncActionsExecutor, settings, competitorSelectionProvider,
                leaderboardName, errorReporter, stringMessages, showRaceDetails,style, flagImageResolver, availableDetailTypes, sailingCF);
        initialize(settings);
    }

    @Override
    public MultiRaceLeaderboardSettings getSettings() {
        Iterable<RaceColumnDTO> selectedRaceColumns = raceColumnSelection
                .getSelectedRaceColumnsOrderedAsInLeaderboard(leaderboard);
        List<String> namesOfRaceColumnsToShow = new ArrayList<>();
        for (RaceColumnDTO raceColumn : selectedRaceColumns) {
            namesOfRaceColumnsToShow.add(raceColumn.getName());
        }
        final MultiRaceLeaderboardSettings leaderboardSettings = new MultiRaceLeaderboardSettings(selectedManeuverDetails,
                selectedLegDetails, selectedRaceDetails, selectedOverallDetailColumns, namesOfRaceColumnsToShow,
                raceColumnSelection.getNumberOfLastRaceColumnsToShow(), timer.getRefreshInterval(),
                raceColumnSelection.getType(), isShowAddedScores(), isShowCompetitorShortName(),
                isShowCompetitorFullName(), isShowCompetitorBoatInfo(), isShowCompetitorNationality, new SecurityChildSettingsContext(leaderboard, paywallResolver));
        return leaderboardSettings;
    }

    @Override
    protected void setDefaultRaceColumnSelection(MultiRaceLeaderboardSettings settings) {
        switch (settings.getActiveRaceColumnSelectionStrategy()) {
        case EXPLICIT:
            raceColumnSelection = new ExplicitRaceColumnSelection();
            break;
        case LAST_N:
            setRaceColumnSelectionToLastNStrategy(settings.getNumberOfLastRacesToShow());
            break;
        }
    }

    @Override
    protected boolean canShowCompetitorBoatInfo() {
        return !this.leaderboard.canBoatsOfCompetitorsChangePerRace && !leaderboard.type.isMetaLeaderboard();
    }

    @Override
    public String getCompetitorColor(CompetitorDTO competitor) {
        return null; // Not used in multi-race leaderboard
    }

    @Override
    public int getLegCount(LeaderboardDTO leaderboardDTO, String raceColumnName) {
        return leaderboardDTO.getLegCount(raceColumnName, null);
    }

    @Override
    protected MultiRaceLeaderboardSettings overrideDefaultsForNamesOfRaceColumns(
            MultiRaceLeaderboardSettings currentSettings, LeaderboardDTO result) {
        return currentSettings.withNamesOfRaceColumnsToShowDefaults(result.getNamesOfRaceColumns(),
                new SecurityChildSettingsContext(leaderboard, paywallResolver));
    }

    @Override
    protected void applyTop30FilterIfCompetitorSizeGreaterEqual40(LeaderboardDTO leaderboard) {
        int maxRaceRank = 30;
        if (leaderboard.competitors.size() >= 40) {
            CompetitorRaceRankFilter raceRankFilter = new CompetitorRaceRankFilter();
            raceRankFilter.setLeaderboardFetcher(this);
            raceRankFilter.setQuickRankProvider(this.competitorFilterPanel.getQuickRankProvider());
            raceRankFilter.setOperator(new BinaryOperator<Integer>(BinaryOperator.Operators.LessThanEquals));
            raceRankFilter.setValue(maxRaceRank);
            FilterSet<CompetitorDTO, Filter<CompetitorDTO>> activeFilterSet = competitorSelectionProvider
                    .getOrCreateCompetitorsFilterSet(stringMessages.topNCompetitorsByRaceRank(maxRaceRank));
            activeFilterSet.addFilter(raceRankFilter);
            competitorSelectionProvider.setCompetitorsFilterSet(activeFilterSet);
        }
    }

    /**
     * Extracts the rows to display of the <code>leaderboard</code>. These are all {@link AbstractLeaderboardDTO#rows
     * rows} in case {@link #preSelectedRace} is <code>null</code>, or only the rows of the competitors who scored in
     * the race identified by {@link #preSelectedRace} otherwise.
     */
    @Override
    public Map<CompetitorDTO, LeaderboardRowDTO> getRowsToDisplay() {
        final Map<CompetitorDTO, LeaderboardRowDTO> result = new HashMap<>();
        Iterable<CompetitorDTO> allFilteredCompetitors = competitorSelectionProvider.getFilteredCompetitors();
        for (CompetitorDTO competitor : leaderboard.rows.keySet()) {
            if (Util.contains(allFilteredCompetitors, competitor)) {
                result.put(competitor, leaderboard.rows.get(competitor));
            }
        }
        return result;
    }

    @Override
    public SettingsDialogComponent<MultiRaceLeaderboardSettings> getSettingsDialogComponent(
            MultiRaceLeaderboardSettings useTheseSettings) {
        return new MultiRaceLeaderboardSettingsDialogComponent(useTheseSettings, leaderboard.getNamesOfRaceColumns(),
                stringMessages, availableDetailTypes, canShowCompetitorBoatInfo(), paywallResolver, leaderboard);
    }

    @Override
    protected void openSettingsDialog() {
        SettingsDialog<MultiRaceLeaderboardSettings> settingsDialog = new SettingsDialog<MultiRaceLeaderboardSettings>(
                this, stringMessages);
        settingsDialog.ensureDebugId("LeaderboardSettingsDialog");
        settingsDialog.show();
    }

    @Override
    protected void applyRaceSelection(final LeaderboardSettings ns) {
        MultiRaceLeaderboardSettings newSettings = (MultiRaceLeaderboardSettings) ns;
        Iterable<String> oldNamesOfRaceColumnsToShow = null;
        if (newSettings.getNamesOfRaceColumnsToShow() == null) {
            oldNamesOfRaceColumnsToShow = raceColumnSelection.getSelectedRaceColumnNames();
        }
        switch (newSettings.getActiveRaceColumnSelectionStrategy()) {
        case EXPLICIT:
            setDefaultRaceColumnSelection(newSettings);
            if (newSettings.getNamesOfRaceColumnsToShow() != null) {
                raceColumnSelection.requestClear();
                for (String nameOfRaceColumnToShow : newSettings.getNamesOfRaceColumnsToShow()) {
                    RaceColumnDTO raceColumnToShow = getRaceByColumnName(nameOfRaceColumnToShow);
                    if (raceColumnToShow != null) {
                        raceColumnSelection.requestRaceColumnSelection(raceColumnToShow);
                    }
                }
            } else {
                // apply the old column selections again
                for (String oldNameOfRaceColumnToShow : oldNamesOfRaceColumnsToShow) {
                    final RaceColumnDTO raceColumnByName = getLeaderboard()
                            .getRaceColumnByName(oldNameOfRaceColumnToShow);
                    if (raceColumnByName != null) {
                        raceColumnSelection.requestRaceColumnSelection(raceColumnByName);
                    }
                }
            }
            break;
        case LAST_N:
            setRaceColumnSelectionToLastNStrategy(newSettings.getNumberOfLastRacesToShow());
            break;
        }
    }

    private RaceColumnDTO getRaceByColumnName(String columnName) {
        if (getLeaderboard() != null) {
            for (RaceColumnDTO race : getLeaderboard().getRaceList()) {
                if (columnName.equals(race.getRaceColumnName())) {
                    return race;
                }
            }
        }
        return null;
    }

    @Override
    protected void processAutoExpands(AbstractSortableColumnWithMinMax<?, ?> c, RaceColumn<?> lastRaceColumn) {
        // Toggle or the last race column if that was requested
        if (isAutoExpandLastRaceColumn() && c == lastRaceColumn) {
            ExpandableSortableColumn<?> expandableSortableColumn = (ExpandableSortableColumn<?>) c;
            if (!expandableSortableColumn.isExpanded()) {
                expandableSortableColumn.changeExpansionState(/* expand */ true);
                autoExpandPerformedOnce = true;
            }
        }
    }

    @Override
    protected void updateLeaderboardAndRun(Runnable callWhenExpansionDataIsLoaded) {
        final LeaderboardDTO previousLeaderboard = getLeaderboard();
        getSailingService().getLeaderboardByName(getLeaderboardName(),
                timer.getPlayMode() == PlayModes.Live ? null : getLeaderboardDisplayDate(),
                /* namesOfRacesForWhichToLoadLegDetails */getNamesOfExpandedRaceColumns(), shallAddOverallDetails(),
                previousLeaderboard.getId(), /* fillTotalPointsUncorrected */ false,
                new MarkedAsyncCallback<IncrementalOrFullLeaderboardDTO>(
                        new AsyncCallback<IncrementalOrFullLeaderboardDTO>() {
                            @Override
                            public void onSuccess(IncrementalOrFullLeaderboardDTO result) {
                                updateLeaderboard(result.getLeaderboardDTO(previousLeaderboard));
                                callWhenExpansionDataIsLoaded.run();
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                getErrorReporter().reportError(
                                        stringMessages.errorTryingToObtainLeaderboardContents(caught.getMessage()),
                                        true /* silentMode */);
                            }
                        }));
    }

    @Override
    protected AsyncAction<LeaderboardDTO> getRetrieverAction() {
        final Date date = getLeaderboardDisplayDate();
        // need to call super here otherwise it might take the instantiated types method implementation, 
        // but fields there might not be initialized if we are within a constructor call.
        SailingServiceAsync sailingService = super.getSailingService(); 
        return new GetLeaderboardByNameAction(sailingService, getLeaderboardName(),
                useNullAsTimePoint() ? null : date,
                /* namesOfRaceColumnsForWhichToLoadLegDetails */getNamesOfExpandedRaceColumns(),
                shallAddOverallDetails(), /* previousLeaderboard */
                getLeaderboard(), isFillTotalPointsUncorrected(), timer, getErrorReporter(), stringMessages);
    }
}
