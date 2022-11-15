package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.ui.adminconsole.AbstractLeaderboardConfigPanel.RaceColumnDTOAndFleetDTOWithNameBasedEquality;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.gwt.client.celltable.RefreshableSingleSelectionModel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;

/**
 * Allows a user to select a "race slot" consisting of a triple of a leaderboard, a race column, and a fleet. An
 * exclusion list can be provided, e.g., in order to avoid recursions in case this panel is used to model dependencies
 * between races.
 * <p>
 * 
 * The widget displays two tables next to each other. The table on the left shows the filterable list of leaderboards.
 * When a selection is made in the leaderboards table, a table on the right is shown that contains an entry for all race
 * column and fleet combinations that the leaderboard has, excluding those listed in the exclude list.
 * <p>
 * 
 * A combined {@link #getSelectionModel() selection model can be obtained} and is available for subscribing listeners to
 * and fetch the current selection from.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class RaceSlotSelectionPanel extends HorizontalPanel {
    private final FlushableCellTable<StrippedLeaderboardDTO> leaderboardTable;

    private final RaceTableWrapper<RefreshableSelectionModel<RaceColumnDTOAndFleetDTOWithNameBasedEquality>> raceColumnTable;
    
    private final ListDataProvider<StrippedLeaderboardDTO> filteredLeaderboardList;
    
    private final RefreshableSingleSelectionModel<StrippedLeaderboardDTO> leaderboardSelectionModel;

    public RaceSlotSelectionPanel(final SailingServiceWriteAsync sailingServiceWrite, final UserService userService,
            final StringMessages stringMessages, final ErrorReporter errorReporter, final boolean multiSelection,
            Iterable<StrippedLeaderboardDTO> availableLeaderboards, RaceColumnDTOAndFleetDTOWithNameBasedEquality preselected) {
        final Resources tableRes = GWT.create(AdminConsoleTableResources.class);
        leaderboardTable = new FlushableCellTable<StrippedLeaderboardDTO>(/* pageSize */10000, tableRes);
        filteredLeaderboardList = new ListDataProvider<StrippedLeaderboardDTO>();
        leaderboardSelectionModel = new RefreshableSingleSelectionModel<StrippedLeaderboardDTO>(
                new NameBasedStrippedLeaderboardDTOEntityIdentityComparator(), filteredLeaderboardList);
        leaderboardTable.setSelectionModel(leaderboardSelectionModel);
        final Label leaderboardFilterLabel = new Label(stringMessages.filterByLeaderboard());
        LabeledAbstractFilterablePanel<StrippedLeaderboardDTO> filterLeaderboardPanel = new LabeledAbstractFilterablePanel<StrippedLeaderboardDTO>(
                leaderboardFilterLabel, availableLeaderboards, filteredLeaderboardList, stringMessages) {
            @Override
            public List<String> getSearchableStrings(StrippedLeaderboardDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(t.displayName);
                return strings;
            }

            @Override
            public AbstractCellTable<StrippedLeaderboardDTO> getCellTable() {
                return leaderboardTable;
            }
        };
        ListHandler<StrippedLeaderboardDTO> leaderboardColumnListHandler = new ListHandler<StrippedLeaderboardDTO>(
                filteredLeaderboardList.getList());
        TextColumn<StrippedLeaderboardDTO> leaderboardNameColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.getName() != null ? leaderboard.getName() : "";
            }
        };
        leaderboardNameColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(leaderboardNameColumn, new Comparator<StrippedLeaderboardDTO>() {
            @Override
            public int compare(StrippedLeaderboardDTO o1, StrippedLeaderboardDTO o2) {
                boolean ascending = isSortedAscending();
                if (o1.getName().equals(o2.getName())) {
                    return 0;
                }
                int val = -1;
                val = (o1 != null && o2 != null && ascending) ? (o1.getName().compareTo(o2.getName()))
                        : -(o2.getName().compareTo(o1.getName()));

                return val;
            }

            private boolean isSortedAscending() {
                ColumnSortList sortList = leaderboardTable.getColumnSortList();
                return sortList.size() > 0 & sortList.get(0).isAscending();
            }
        });
        TextColumn<StrippedLeaderboardDTO> leaderboardDisplayNameColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.getDisplayName() != null ? leaderboard.getDisplayName() : "";
            }
        };
        leaderboardDisplayNameColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(leaderboardDisplayNameColumn,
                new Comparator<StrippedLeaderboardDTO>() {
                    @Override
                    public int compare(StrippedLeaderboardDTO o1, StrippedLeaderboardDTO o2) {
                        return new NaturalComparator().compare(o1.getDisplayName(), o2.getDisplayName());
                    }
                });
        leaderboardTable.addColumnSortHandler(leaderboardColumnListHandler);
        leaderboardTable.addColumn(leaderboardNameColumn, stringMessages.name());
        leaderboardTable.addColumn(leaderboardDisplayNameColumn, stringMessages.displayName());
        filterLeaderboardPanel.getTextBox().ensureDebugId("LeaderboardsFilterInRaceSlotSelectionPanelTextBox");
        filterLeaderboardPanel.setUpdatePermissionFilterForCheckbox(
                leaderboard -> userService.hasPermission(leaderboard, DefaultActions.UPDATE));
        filterLeaderboardPanel.filter();
        leaderboardTable.ensureDebugId("AvailableLeaderboardsInRaceSlotSelectionPanelTable");
        filteredLeaderboardList.addDataDisplay(leaderboardTable);
        leaderboardSelectionModel.addSelectionChangeHandler(e->updateRaceColumnTableAfterLeaderboardSelectionChange());
        raceColumnTable = new RaceTableWrapper<RefreshableSelectionModel<RaceColumnDTOAndFleetDTOWithNameBasedEquality>>(
                sailingServiceWrite, stringMessages, errorReporter, multiSelection);
        raceColumnTable.asWidget().ensureDebugId("RaceColumnInRaceSlotSelectionPanelTable");
        final Grid tableGrid = new Grid(2, 2);
        tableGrid.setWidget(0, 0, filterLeaderboardPanel);
        tableGrid.setWidget(1, 0, leaderboardTable);
        tableGrid.setWidget(1, 1, raceColumnTable.getTable());
        add(tableGrid);
        // update data into table
        // apply selection if requested
        if (preselected != null) {
            leaderboardTable.getSelectionModel().setSelected(preselected.getC(), true);
            Scheduler.get().scheduleDeferred(()->{
                raceColumnTable.setSelectedLeaderboardName(preselected.getC().getName());
                raceColumnTable.getSelectionModel().setSelected(preselected, true);
            });
        }
    }

    private void updateRaceColumnTableAfterLeaderboardSelectionChange() {
        final StrippedLeaderboardDTO selectedLeaderboard = getSelectedLeaderboard();
        if (selectedLeaderboard != null) {
            List<Triple<String, String, String>> raceColumnsAndFleets = new ArrayList<Triple<String, String, String>>();
            for (RaceColumnDTO raceColumn : selectedLeaderboard.getRaceList()) {
                for (FleetDTO fleet : raceColumn.getFleets()) {
                    raceColumnsAndFleets.add(new Triple<String, String, String>(selectedLeaderboard.getName(), raceColumn.getName(), fleet.getName()));
                }
            }
            raceColumnTable.getDataProvider().getList().clear();
            for (RaceColumnDTO raceColumn : selectedLeaderboard.getRaceList()) {
                for (FleetDTO fleet : raceColumn.getFleets()) {
                    RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnDTOAndFleet2 = new RaceColumnDTOAndFleetDTOWithNameBasedEquality(raceColumn, fleet, getSelectedLeaderboard());
                    raceColumnTable.getDataProvider().getList().add(raceColumnDTOAndFleet2);
                }
            }
            raceColumnTable.getTable().setVisible(true);
        } else {
            raceColumnTable.getTable().setVisible(false);
        }
    }

    private StrippedLeaderboardDTO getSelectedLeaderboard() {
        return leaderboardSelectionModel.getSelectedObject();
    }

    public RefreshableSelectionModel<RaceColumnDTOAndFleetDTOWithNameBasedEquality> getSelectionModel() {
        return raceColumnTable.getSelectionModel();
    }
}
