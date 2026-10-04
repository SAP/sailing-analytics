package com.sap.sailing.gwt.ui.client.shared.racemap.maneuver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType.TrackedRaceActions;
import com.sap.sailing.gwt.ui.actions.GetManeuversForCompetitorsAction;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.ManeuverTypeFormatter;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardPanel.LeaderBoardStyle;
import com.sap.sailing.gwt.ui.shared.ManeuverDTO;
import com.sap.sailing.gwt.ui.shared.RaceWithCompetitorsAndBoatsDTO;
import com.sap.sse.common.Color;
import com.sap.sse.common.InvertibleComparator;
import com.sap.sse.common.SortingOrder;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.common.impl.InvertibleComparatorAdapter;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.celltable.AbstractSortableColumnWithMinMax;
import com.sap.sse.gwt.client.celltable.SortableColumn;
import com.sap.sse.gwt.client.celltable.SortedCellTableWithStylableHeaders;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.TimeRangeProvider;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomModel;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.shared.components.AbstractCompositeComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.UserStatusEventHandler;

public class ManeuverTablePanel extends AbstractCompositeComponent<ManeuverTableSettings>
        implements CompetitorSelectionChangeListener, TimeListener {

    private static final Supplier<Long> LOADING_OFFSET_TO_NEXT_MANEUVER_PROVIDER = () -> 2500L;

    private final ManeuverTablePanelResources resources = GWT.create(ManeuverTablePanelResources.class);

    private final StringMessages stringMessages;
    private final RegattaAndRaceIdentifier raceIdentifier;
    private final RaceCompetitorSelectionProvider competitorSelectionModel;

    private final SimplePanel contentPanel = new SimplePanel();
    private final Label importantMessageLabel = new Label();
    private final SortedCellTableWithStylableHeaders<ManeuverTableData> maneuverCellTable;
    private final SortableColumn<ManeuverTableData, ?> competitorColumn, timeColumn;
    
    /**
     * keys are the competitor IDs as string
     */
    private final CachedRaceDataProvider<String, ManeuverDTO> competitorDataProvider;

    private ManeuverTableSettings settings;
    private boolean hasCanReplayDuringLiveRacesPermission;

    public ManeuverTablePanel(final Component<?> parent, ComponentContext<?> context,
            final SailingServiceAsync sailingService, final AsyncActionsExecutor asyncActionsExecutor,
            final RegattaAndRaceIdentifier raceIdentifier, final StringMessages stringMessages,
            final RaceCompetitorSelectionProvider competitorSelectionModel, final ErrorReporter errorReporter,
            final Timer timer, final ManeuverTableSettings initialSettings,
            final TimeRangeWithZoomModel timeRangeWithZoomProvider, final LeaderBoardStyle style,
            final UserService userService, final RaceWithCompetitorsAndBoatsDTO raceDTO) {
        super(parent, context);
        final UserStatusEventHandler userStatusChangeHandler = new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
                hasCanReplayDuringLiveRacesPermission = userService.hasPermission(raceDTO,
                        TrackedRaceActions.CAN_REPLAY_DURING_LIVE_RACES);
            }
        };
        userService.addUserStatusEventHandler(userStatusChangeHandler);
        userStatusChangeHandler.onUserStatusChange(userService.getCurrentUser(), /* preAuthenticated */ true);
        this.resources.css().ensureInjected();
        this.settings = initialSettings;
        this.raceIdentifier = raceIdentifier;
        this.competitorSelectionModel = competitorSelectionModel;
        this.stringMessages = stringMessages;
        this.competitorDataProvider = new CachedManeuverTableDataProvider(timeRangeWithZoomProvider, timer,
                sailingService, asyncActionsExecutor);
        this.competitorSelectionModel.addCompetitorSelectionChangeListener(this);
        timer.addTimeListener(this);
        final FlowPanel rootPanel = new FlowPanel();
        rootPanel.addStyleName(resources.css().maneuverPanel());
        this.contentPanel.addStyleName(resources.css().contentContainer());
        rootPanel.add(contentPanel);
        final Button settingsButton = SettingsDialog.createSettingsButton(this, stringMessages);
        settingsButton.setStyleName(resources.css().settingsButton());
        rootPanel.add(settingsButton);
        this.importantMessageLabel.addStyleName(resources.css().importantMessage());
        this.maneuverCellTable = new SortedCellTableWithStylableHeaders<>(Integer.MAX_VALUE, style.getTableresources());
        this.maneuverCellTable.addStyleName(resources.css().maneuverTable());
        final SingleSelectionModel<ManeuverTableData> selectionModel = new SingleSelectionModel<>();
        this.maneuverCellTable.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                final ManeuverTableData selected = selectionModel.getSelectedObject();
                if (selected != null) {
                    if (timer.getPlayMode() == PlayModes.Live && !hasCanReplayDuringLiveRacesPermission) {
                        timer.pause();
                    }
                    timer.setPlayMode(PlayModes.Replay);
                    timer.setTime(selected.getTimePointBefore().getTime());
                }
            }
        });
        this.maneuverCellTable.addColumn(competitorColumn = createCompetitorColumn());
        this.maneuverCellTable.addColumn(createManeuverTypeColumn());
        this.maneuverCellTable.addColumn(createMarkPassingColumn());
        this.maneuverCellTable.addColumn(timeColumn = createTimeColumn());
        this.maneuverCellTable.addColumn(createSortableMinMaxColumn(ManeuverTableData::getSpeedBeforeInKnots,
                this.stringMessages.speedIn(), this.stringMessages.knotsUnit()));
        this.maneuverCellTable.addColumn(createSortableMinMaxColumn(ManeuverTableData::getSpeedAfterInKnots,
                this.stringMessages.speedOut(), this.stringMessages.knotsUnit()));
        this.maneuverCellTable.addColumn(createSortableMinMaxColumn(ManeuverTableData::getSpeedChangeInKnots,
                this.stringMessages.speedChange(), this.stringMessages.knotsUnit()));
        this.maneuverCellTable.addColumn(createSortableMinMaxColumn(ManeuverTableData::getLowestSpeedInKnots,
                this.stringMessages.lowestSpeed(), this.stringMessages.knotsUnit()));
        this.maneuverCellTable.addColumn(createSortableMinMaxColumn(ManeuverTableData::getMaximumTurningRate,
                this.stringMessages.maxTurningRate(), this.stringMessages.degreesPerSecondUnit()));
        this.maneuverCellTable.addColumn(createSortableMinMaxColumn(ManeuverTableData::getAverageTurningRate,
                this.stringMessages.avgTurningRate(), this.stringMessages.degreesPerSecondUnit()));
        this.maneuverCellTable.addColumn(createSortableMinMaxColumn(ManeuverTableData::getManeuverLossInMeters,
                this.stringMessages.maneuverLoss(), stringMessages.metersUnit()));
        this.maneuverCellTable.addColumn(createSortableAbsMinMaxColumn(ManeuverTableData::getDirectionChange,
                stringMessages.directionChange(), this.stringMessages.degreesShort()));
        initWidget(rootPanel);
        setVisible(false);
    }

    /**
     * Creates a sortable column with the absolute value. Whereas {@link #createSortableMinMaxColumn()} creates a
     * sortable column with signed values.
     */
    private SortableColumn<ManeuverTableData, String> createSortableAbsMinMaxColumn(
            Function<ManeuverTableData, Double> extractor, String title, String unit) {
        return new SortableMinMaxColumn(extractor, title, unit, maneuverCellTable.getDataProvider(), /* absolute */ true);
    }
    
    /**
     * Creates a sortable column with signed values. 
     */
    private SortableColumn<ManeuverTableData, String> createSortableMinMaxColumn(
            Function<ManeuverTableData, Double> extractor, String title, String unit) {
        return new SortableMinMaxColumn(extractor, title, unit, maneuverCellTable.getDataProvider(), /* absolute */ false);
    }

    private SortableColumn<ManeuverTableData, String> createManeuverTypeColumn() {
        return new SortableColumn<ManeuverTableData, String>(new TextCell(), SortingOrder.ASCENDING) {
            @Override
            public InvertibleComparator<ManeuverTableData> getComparator() {
                return new InvertibleComparatorAdapter<ManeuverTableData>() {
                    @Override
                    public int compare(ManeuverTableData o1, ManeuverTableData o2) {
                        return o1.getManeuverType().compareTo(o2.getManeuverType());
                    }
                };
            }

            @Override
            public Header<?> getHeader() {
                return new TextHeader(stringMessages.maneuverType());
            }

            @Override
            public String getValue(ManeuverTableData object) {
                return ManeuverTypeFormatter.format(object.getManeuverType(), stringMessages);
            }
        };
    }

    private SortableColumn<ManeuverTableData, Date> createTimeColumn() {
        final InvertibleComparator<ManeuverTableData> comparator = new InvertibleComparatorAdapter<ManeuverTableData>() {
            @Override
            public int compare(ManeuverTableData o1, ManeuverTableData o2) {
                return o1.getTimePoint().compareTo(o2.getTimePoint());
            }
        };
        final SortableColumn<ManeuverTableData, Date> col = new SortableColumn<ManeuverTableData, Date>(
                new DateCell(DateTimeFormat.getFormat(PredefinedFormat.TIME_LONG)), SortingOrder.ASCENDING) {
            @Override
            public InvertibleComparator<ManeuverTableData> getComparator() {
                return comparator;
            }

            @Override
            public Header<?> getHeader() {
                return new TextHeader(stringMessages.time());
            }

            @Override
            public Date getValue(ManeuverTableData object) {
                return object.getTimePoint();
            }
        };
        col.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        return col;
    }

    private SortableColumn<ManeuverTableData, Boolean> createMarkPassingColumn() {
        final InvertibleComparator<ManeuverTableData> comparator = new InvertibleComparatorAdapter<ManeuverTableData>() {
            public int compare(ManeuverTableData o1, ManeuverTableData o2) {
                return -Boolean.compare(o1.isMarkPassing(), o2.isMarkPassing());
            }
        };
        final SortableColumn<ManeuverTableData, Boolean> column = new SortableColumn<ManeuverTableData, Boolean>(
                new AbstractCell<Boolean>() {
                    @Override
                    public void render(Context context, Boolean value, SafeHtmlBuilder sb) {
                        sb.append(value ? SafeHtmlUtils.fromTrustedString("&#10004;") : SafeHtmlUtils.EMPTY_SAFE_HTML);
                    }
                }, SortingOrder.ASCENDING) {

            @Override
            public InvertibleComparator<ManeuverTableData> getComparator() {
                return comparator;
            }

            @Override
            public Header<?> getHeader() {
                return new TextHeader(stringMessages.markPassing());
            }

            @Override
            public Boolean getValue(ManeuverTableData object) {
                return object.isMarkPassing();
            }
        };
        column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        return column;
    }

    private SortableColumn<ManeuverTableData, ManeuverTableData> createCompetitorColumn() {
        InvertibleComparator<ManeuverTableData> comparator = new InvertibleComparatorAdapter<ManeuverTableData>() {
            @Override
            public int compare(ManeuverTableData o1, ManeuverTableData o2) {
                return o1.getCompetitorName().compareTo(o2.getCompetitorName());
            }
        };
        return new SortableColumn<ManeuverTableData, ManeuverTableData>(new AbstractCell<ManeuverTableData>() {
            @Override
            public void render(Context context, ManeuverTableData data, SafeHtmlBuilder sb) {
                final String color = data.getCompetitorColor();
                final String divStyle = color == null ? "border: none;" : "border-bottom: 2px solid " + color + ";";
                sb.appendHtmlConstant("<div style=\"" + divStyle + "\">");
                sb.appendEscaped(data.getCompetitorName());
                sb.appendHtmlConstant("</div>");
            }
        }, SortingOrder.ASCENDING) {
            @Override
            public InvertibleComparator<ManeuverTableData> getComparator() {
                return comparator;
            }

            @Override
            public Header<?> getHeader() {
                return new TextHeader(stringMessages.competitor());
            }

            @Override
            public ManeuverTableData getValue(ManeuverTableData object) {
                return object;
            }
        };
    }

    private void rerender() {
        if (isVisible()) {
            if (Util.isEmpty(competitorSelectionModel.getSelectedCompetitors())) {
                this.importantMessageLabel.setText(stringMessages.selectAtLeastOneCompetitorManeuver());
                this.contentPanel.setWidget(importantMessageLabel);
            } else if (!competitorDataProvider.hasCachedData()) {
                this.importantMessageLabel.setText(stringMessages.noDataFound());
                this.contentPanel.setWidget(importantMessageLabel);
            } else {
                this.contentPanel.setWidget(maneuverCellTable);
                this.showCompetitorColumn(Util.size(competitorSelectionModel.getSelectedCompetitors()) != 1);
                this.updateManeuverTableData();
                this.updateManeuverTableColumnsWithMinMax();
                this.maneuverCellTable.restoreColumnSortInfos(timeColumn);
                this.maneuverCellTable.redraw();
            }
        }
    }

    private void showCompetitorColumn(boolean show) {
        if (show && maneuverCellTable.getColumnIndex(competitorColumn) == -1) {
            maneuverCellTable.insertColumn(0, competitorColumn);
        } else if (!show && maneuverCellTable.getColumnIndex(competitorColumn) > -1) {
            maneuverCellTable.removeColumn(competitorColumn);
        }
    }

    private void updateManeuverTableData() {
        final ArrayList<ManeuverTableData> data = new ArrayList<>();
        final Map<String, Iterable<ManeuverDTO>> cachedData = competitorDataProvider.getCachedData();
        for (final Entry<String, Iterable<ManeuverDTO>> entry : cachedData.entrySet()) {
            for (ManeuverDTO maneuver : entry.getValue()) {
                if (settings.getSelectedManeuverTypes().contains(maneuver.getType())) {
                    final CompetitorDTO competitor = competitorSelectionModel.getSelectedCompetitor(entry.getKey());
                    final Color competitorColor = competitorSelectionModel.getColor(competitor, raceIdentifier);
                    data.add(new ManeuverTableData(competitor, competitorColor.getAsHtml(), maneuver));
                }
            }
        }
        this.maneuverCellTable.setList(data);
    }

    private void updateManeuverTableColumnsWithMinMax() {
        for (int i = 0; i < maneuverCellTable.getColumnCount(); i++) {
            final Column<ManeuverTableData, ?> column = maneuverCellTable.getColumn(i);
            if (column instanceof AbstractSortableColumnWithMinMax) {
                ((AbstractSortableColumnWithMinMax<ManeuverTableData, ?>) column).updateMinMax();
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        final boolean wasVisible = isVisible();
        super.setVisible(visible);
        if (wasVisible && !visible) {
            this.competitorDataProvider.removeAllEntries();
        } else if (!wasVisible && visible) {
            this.rerender();
            this.competitorDataProvider.ensureEntries(competitorSelectionModel.getSelectedCompetitorIdsAsStrings());
        }
    }

    @Override
    public void addedToSelection(CompetitorDTO competitor) {
        if (isVisible()) {
            this.competitorDataProvider.ensureEntry(competitor.getIdAsString());
            this.rerender();
        }
    }

    @Override
    public void removedFromSelection(CompetitorDTO competitor) {
        if (isVisible()) {
            this.competitorDataProvider.removeEntry(competitor.getIdAsString());
            this.rerender();
        }
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.maneuverTable();
    }

    @Override
    public String getDependentCssClassName() {
        return "table";
    }

    @Override
    public void filterChanged(FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> oldFilterSet,
            FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> newFilterSet) {
    }

    @Override
    public void competitorsListChanged(Iterable<CompetitorDTO> competitors) {
    }

    @Override
    public void filteredCompetitorsListChanged(Iterable<CompetitorDTO> filteredCompetitors) {
    }

    @Override
    public ManeuverTableSettings getSettings() {
        return settings;
    }

    @Override
    public String getId() {
        return ManeuverTableLifecycle.ID;
    }

    @Override
    public SettingsDialogComponent<ManeuverTableSettings> getSettingsDialogComponent(
            ManeuverTableSettings useTheseSettings) {
        return new ManeuverTableSettingsDialogComponent(useTheseSettings, stringMessages);
    }

    @Override
    public void updateSettings(ManeuverTableSettings newSettings) {
        settings = newSettings;
        rerender();
    }

    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        if (isVisible()) {
            this.competitorDataProvider.updateEntryData();
        }
    }

    private class CachedManeuverTableDataProvider extends CachedRaceDataProvider<String, ManeuverDTO> {
        private final AsyncActionsExecutor asyncActionsExecutor;
        private final SailingServiceAsync sailingService;

        private CachedManeuverTableDataProvider(final TimeRangeProvider timeRangeProvider, final Timer timer,
                final SailingServiceAsync sailingService, final AsyncActionsExecutor asyncActionsExecutor) {
            super(timeRangeProvider, timer, m -> m.getTimePoint(), LOADING_OFFSET_TO_NEXT_MANEUVER_PROVIDER, true);
            this.asyncActionsExecutor = asyncActionsExecutor;
            this.sailingService = sailingService;
        }

        @Override
        protected void loadData(final Map<String, TimeRange> competitorIdsAsStringsAndTimeRanges,
                final boolean incremental,
                final AsyncCallback<Map<String, List<ManeuverDTO>>> callback) {
            final GetManeuversForCompetitorsAction action = new GetManeuversForCompetitorsAction(sailingService, raceIdentifier, competitorIdsAsStringsAndTimeRanges);
            if (incremental) {
                asyncActionsExecutor.execute(action, callback);
            } else {
                // AsyncActionExecutor is explicitly not used here, to ensure full updates are always executed.
                // Because full updates are triggered in specific situations only, this shouldn't cause server overload.
                action.execute(callback);
            }
        }

        @Override
        protected void onEntriesDataChange(final Iterable<String> updatedCompetitorIdsAsStrings) {
            ManeuverTablePanel.this.rerender();
        }
    }
}