package com.sap.sailing.gwt.ui.leaderboard;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.IncrementalOrFullLeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.SingleRaceLeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.SingleRaceLeaderboardSettingsDialogComponent;
import com.sap.sailing.gwt.ui.actions.GetLeaderboardForRaceAction;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.filter.CompetitorRaceRankFilter;
import com.sap.sse.common.InvertibleComparator;
import com.sap.sse.common.SortingOrder;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.BinaryOperator;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.common.impl.InvertibleComparatorAdapter;
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
 * This type of {@link LeaderboardPanel} shows a single race. When the leaderboard uses fleet racing, this will cause
 * only those competitors to be shown that are part of the specific race.
 */
public class SingleRaceLeaderboardPanel extends LeaderboardPanel<SingleRaceLeaderboardSettings> {
    private boolean autoExpandPreSelectedRace;

    /**
     * If this is <code>null</code>, all leaderboard columns added by updating the leaderboard from the server are
     * automatically added to the table. Otherwise, only the column whose {@link RaceColumnDTO#getRaceIdentifier(String)
     * race identifier} matches the value of this attribute will be added.
     */
    private final RegattaAndRaceIdentifier preSelectedRace;
    private RaceRankColumn raceRankColumn;
    private boolean notSortedYet = true;

    private boolean showRaceRankColumn;
    
    public SingleRaceLeaderboardPanel(Component<?> parent, ComponentContext<?> context,
            SailingServiceAsync sailingService, AsyncActionsExecutor asyncActionsExecutor, SingleRaceLeaderboardSettings settings,
            boolean isEmbedded, RegattaAndRaceIdentifier preSelectedRace,
            RaceCompetitorSelectionProvider competitorSelectionProvider, Timer timer, String leaderboardGroupName,
            String leaderboardName, ErrorReporter errorReporter, StringMessages stringMessages, boolean showRaceDetails,
            CompetitorFilterPanel competitorSearchTextBox, boolean showSelectionCheckbox,
            RaceTimesInfoProvider optionalRaceTimesInfoProvider, boolean autoExpandLastRaceColumn,
            boolean adjustTimerDelay, boolean autoApplyTopNFilter, boolean showCompetitorFilterStatus,
            boolean enableSyncScroller,LeaderBoardStyle style, FlagImageResolver flagImageResolver, Iterable<DetailType> availableDetailTypes,
            WithSecurity withSecurity) {
        super(parent, context, sailingService, asyncActionsExecutor, settings, isEmbedded, competitorSelectionProvider,
                timer, leaderboardName, errorReporter, stringMessages, showRaceDetails,
                competitorSearchTextBox, showSelectionCheckbox, optionalRaceTimesInfoProvider, autoExpandLastRaceColumn,
                adjustTimerDelay, autoApplyTopNFilter, showCompetitorFilterStatus, enableSyncScroller, style,
                flagImageResolver, availableDetailTypes, withSecurity);
        assert preSelectedRace != null;
        this.preSelectedRace = preSelectedRace;
        this.showRaceRankColumn = settings.isShowRaceRankColumn();
        initialize(settings);
    }
    

    @Override
    protected int ensureRaceRankColumn(int rankColumnIndex) {
        boolean required = isShowRaceRankColumn() && preSelectedRace != null;
        final int indexOfNextColumn = required ? 1 : 0;
        if (getLeaderboardTable().getColumnCount() > rankColumnIndex) {
            if (required) {
                if (getLeaderboardTable().getColumn(rankColumnIndex) != getRaceRankColumn()) {
                    insertColumn(rankColumnIndex, getRaceRankColumn());
                }
            } else {
                if (getLeaderboardTable().getColumn(rankColumnIndex) == getRaceRankColumn()) {
                    removeColumn(rankColumnIndex);
                }
            }
        } else {
            if (required) {
                insertColumn(rankColumnIndex, getRaceRankColumn());
            }
        }
        return indexOfNextColumn;
    }

    private AbstractSortableColumnWithMinMax<LeaderboardRowDTO, ?> getRaceRankColumn() {
        if(raceRankColumn == null){
            raceRankColumn = new RaceRankColumn(preSelectedRace);
        }
        return raceRankColumn;
    }

    private boolean isShowRaceRankColumn() {
        return showRaceRankColumn;
    }

    @Override
    protected boolean canShowCompetitorBoatInfo() {
        return true;
    }

    @Override
    protected void setDefaultRaceColumnSelection(SingleRaceLeaderboardSettings settings) {
        raceColumnSelection = new ExplicitRaceColumnSelectionWithPreselectedRace(preSelectedRace);
    }

    /**
     * Updates the competitors and their boats in the competitorSelectionProvider with the competitors received from the {@link LeaderboardDTO}
     */
    protected void updateCompetitors(LeaderboardDTO leaderboard) {
        final RaceCompetitorSelectionProvider raceCompetitorSelection = (RaceCompetitorSelectionProvider) competitorSelectionProvider;
        RaceColumnDTO singleRaceColumn = null;
        for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
            if (leaderboard.raceIsTracked(raceColumn.getRaceColumnName()) && raceColumn.hasTrackedRace(preSelectedRace)) {
                singleRaceColumn = raceColumn;
                break;
            }
        }
        if (singleRaceColumn != null) {
            for (CompetitorDTO competitor : leaderboard.competitors) {
                BoatDTO boatOfCompetitor = leaderboard.getBoatOfCompetitor(singleRaceColumn.getRaceColumnName(), competitor);
                raceCompetitorSelection.setBoat(competitor, boatOfCompetitor);
                LeaderboardRowDTO leaderboardRowDTO = leaderboard.rows.get(competitor);
                leaderboardRowDTO.boat = boatOfCompetitor; 
            }
        }
        super.updateCompetitors(leaderboard);
    }

    @Override
    public SingleRaceLeaderboardSettings getSettings() {
        final SingleRaceLeaderboardSettings leaderboardSettings = new SingleRaceLeaderboardSettings(selectedManeuverDetails,
                selectedLegDetails, selectedRaceDetails, selectedOverallDetailColumns, timer.getRefreshInterval(),
                isShowAddedScores(), isShowCompetitorShortName(),
                isShowCompetitorFullName(), isShowCompetitorBoatInfo(), isShowCompetitorNationality, showRaceRankColumn,
                new SecurityChildSettingsContext(leaderboard, paywallResolver));
        return leaderboardSettings;
    }

    public boolean isAutoExpandPreSelectedRace() {
        return autoExpandPreSelectedRace;
    }

    private RaceCompetitorSelectionProvider getRaceCompetitorSelection() {
        return (RaceCompetitorSelectionProvider) competitorSelectionProvider;
    }
    
    @Override
    public String getCompetitorColor(CompetitorDTO competitor) {
        return isEmbedded() ? getRaceCompetitorSelection().getColor(competitor, preSelectedRace).getAsHtml() : null;
    }

    @Override
    public int getLegCount(LeaderboardDTO leaderboardDTO, String raceColumnName) {
        return leaderboardDTO.getLegCount(raceColumnName, preSelectedRace);
    }

    @Override
    protected SingleRaceLeaderboardSettings overrideDefaultsForNamesOfRaceColumns(SingleRaceLeaderboardSettings currentSettings,
            LeaderboardDTO result) {
        return currentSettings;
    }

    @Override
    protected void applyTop30FilterIfCompetitorSizeGreaterEqual40(LeaderboardDTO leaderboard) {
        int maxRaceRank = 30;
        if (leaderboard.competitors.size() >= 40) {
            CompetitorRaceRankFilter raceRankFilter = new CompetitorRaceRankFilter();
            raceRankFilter.setLeaderboardFetcher(this);
            raceRankFilter.setSelectedRace(preSelectedRace);
            raceRankFilter.setQuickRankProvider(this.competitorFilterPanel.getQuickRankProvider());
            raceRankFilter.setOperator(new BinaryOperator<Integer>(BinaryOperator.Operators.LessThanEquals));
            raceRankFilter.setValue(maxRaceRank);
            FilterSet<CompetitorDTO, Filter<CompetitorDTO>> activeFilterSet = competitorSelectionProvider
                    .getOrCreateCompetitorsFilterSet(stringMessages.topNCompetitorsByRaceRank(maxRaceRank));
            activeFilterSet.addFilter(raceRankFilter);
            competitorSelectionProvider.setCompetitorsFilterSet(activeFilterSet);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void processAutoExpands(AbstractSortableColumnWithMinMax<?, ?> c, RaceColumn<?> lastRaceColumn) {
        // Toggle pre-selected race, if the setting is set and it isn't open yet, or the last race column if
        // that was requested
        if ((!autoExpandPerformedOnce && isAutoExpandPreSelectedRace() && c instanceof LeaderboardPanel.RaceColumn
                && ((RaceColumn) c).getRace().hasTrackedRace(preSelectedRace))
                || (isAutoExpandLastRaceColumn() && c == lastRaceColumn)) {
            ExpandableSortableColumn<?> expandableSortableColumn = (ExpandableSortableColumn<?>) c;
            if (!expandableSortableColumn.isExpanded()) {
                expandableSortableColumn.changeExpansionState(/* expand */ true);
                autoExpandPerformedOnce = true;
            }
        }
        if (c instanceof RaceColumn && ((RaceColumn) c).getRace().hasTrackedRace(preSelectedRace)) {
            RaceColumnDTO raceColumn = ((RaceColumn) c).getRace();
            informLeaderboardUpdateListenersAboutRaceSelected(preSelectedRace, raceColumn);
        }
    }

    @Override
    protected void postApplySettings(LeaderboardSettings newSettings,
            List<ExpandableSortableColumn<?>> columnsToExpandAgain) {
        super.postApplySettings(newSettings, columnsToExpandAgain);
        if (notSortedYet) {
            final RaceColumn<?> raceColumnByRaceName = getRaceColumnByRaceName(preSelectedRace.getRaceName());
            if (raceColumnByRaceName != null) {
                getLeaderboardTable().sortColumn(raceColumnByRaceName, /* ascending */true);
                notSortedYet = false;
            }
        }
    }

    /**
     * Extracts the rows to display of the <code>leaderboard</code>. These are all {@link AbstractLeaderboardDTO#rows
     * rows} in case {@link #preSelectedRace} is <code>null</code>, or only the rows of the competitors who scored in
     * the race identified by {@link #preSelectedRace} otherwise.
     */
    @Override
    public Map<CompetitorDTO, LeaderboardRowDTO> getRowsToDisplay() {
        Map<CompetitorDTO, LeaderboardRowDTO> result;
        Iterable<CompetitorDTO> allFilteredCompetitors = competitorSelectionProvider.getFilteredCompetitors();
        result = new HashMap<>();
        for (CompetitorDTO competitorInPreSelectedRace : getCompetitors(preSelectedRace)) {
            if (Util.contains(allFilteredCompetitors, competitorInPreSelectedRace)) {
                result.put(competitorInPreSelectedRace, leaderboard.rows.get(competitorInPreSelectedRace));
            }
        }
        return result;
    }

    @Override
    public SettingsDialogComponent<SingleRaceLeaderboardSettings> getSettingsDialogComponent(
            SingleRaceLeaderboardSettings useTheseSettings) {
        return new SingleRaceLeaderboardSettingsDialogComponent(useTheseSettings, stringMessages, availableDetailTypes, paywallResolver, leaderboard);
    }

    @Override
    protected void openSettingsDialog() {
        SettingsDialog<SingleRaceLeaderboardSettings> settingsDialog = new SettingsDialog<SingleRaceLeaderboardSettings>(
                this, stringMessages);
        settingsDialog.ensureDebugId("LeaderboardSettingsDialog");
        settingsDialog.show();
    }

    @Override
    protected void applyRaceSelection(LeaderboardSettings newSettings) {
    }

    public void setAutoExpandPreSelected(boolean b) {
        autoExpandPreSelectedRace = b;
    }
    
    private class RaceRankColumn extends LeaderboardSortableColumnWithMinMax<LeaderboardRowDTO, String> {
        public RaceRankColumn(RegattaAndRaceIdentifier preSelectedRace) {
            super(new TextCell(), SortingOrder.ASCENDING, SingleRaceLeaderboardPanel.this);
            setHorizontalAlignment(ALIGN_CENTER);
            setSortable(true);
        }

        @Override
        public String getValue(LeaderboardRowDTO object) {
            int raceRank = getRacePlace(object);
            return "" + (raceRank == 0 ? "-" : raceRank);
        }

        private int getRacePlace(LeaderboardRowDTO object) {
            RaceColumn<?> raceColumn = getRaceColumnByRaceName(preSelectedRace.getRaceName());
            if(raceColumn.race == null){
                return -1;
            }
            List<CompetitorDTO> competitorsSorted = getLeaderboard().getCompetitorsFromBestToWorst(raceColumn.race);
            int raceRank = -1;
            for (int i = 0; i < competitorsSorted.size(); i++) {
                if (object.competitor.equals(competitorsSorted.get(i))) {
                    raceRank = i + 1;
                    break;
                }
            }
            return raceRank;
        }

        @Override
        public InvertibleComparator<LeaderboardRowDTO> getComparator() {
            return new InvertibleComparatorAdapter<LeaderboardRowDTO>() {
                @Override
                public int compare(LeaderboardRowDTO o1, LeaderboardRowDTO o2) {
                    int racePlace1 = getRacePlace(o1);
                    int racePlace2 = getRacePlace(o2);
                    return racePlace1 == 0 ? racePlace2 == 0 ? 0 : 1 : racePlace2 == 0 ? -1 : racePlace1 - racePlace2;
                }
            };
        }

        @Override
        public SafeHtmlHeader getHeader() {
            return new SafeHtmlHeaderWithTooltip(SafeHtmlUtils.fromString(stringMessages.raceRankShort()),
                    stringMessages.raceRank());
        }
    }
    
    @Override
    public void updateSettings(SingleRaceLeaderboardSettings newSettings) {
        super.updateSettings(newSettings);
        showRaceRankColumn = newSettings.isShowRaceRankColumn();
    }

    @Override
    protected void updateLeaderboardAndRun(Runnable callWhenExpansionDataIsLoaded) {
        final LeaderboardDTO previousLeaderboard = getLeaderboard();
        getSailingService().getLeaderboardForRace(preSelectedRace, getLeaderboardName(),
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
        return new GetLeaderboardForRaceAction(getSailingService(), getLeaderboardName(), preSelectedRace,
                useNullAsTimePoint() ? null : date,
                /* namesOfRaceColumnsForWhichToLoadLegDetails */getNamesOfExpandedRaceColumns(),
                shallAddOverallDetails(), /* previousLeaderboard */
                getLeaderboard(), isFillTotalPointsUncorrected(), timer, getErrorReporter(), stringMessages);
    }

}
