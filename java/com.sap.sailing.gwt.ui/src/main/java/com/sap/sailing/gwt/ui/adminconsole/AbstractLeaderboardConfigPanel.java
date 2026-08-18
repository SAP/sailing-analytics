package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sailing.domain.common.security.SecuredDomainType.LEADERBOARD;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.orc.ImpliedWindSource;
import com.sap.sailing.gwt.common.client.help.HelpButton;
import com.sap.sailing.gwt.common.client.help.HelpButtonResources;
import com.sap.sailing.gwt.ui.adminconsole.RaceColumnInLeaderboardDialog.RaceColumnDescriptor;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.Refresher;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RaceLogDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.RegattaLogDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.async.ParallelExecutionCallback;
import com.sap.sse.gwt.client.async.ParallelExecutionHolder;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.dto.NamedDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;

public abstract class AbstractLeaderboardConfigPanel extends FormPanel
        implements SelectedLeaderboardProvider<StrippedLeaderboardDTO>, TrackedRaceChangedListener,
        FilterablePanelProvider<StrippedLeaderboardDTO> {
    protected final VerticalPanel mainPanel;

    protected final TrackedRacesListComposite trackedRacesListComposite;

    protected final StringMessages stringMessages;

    protected final SailingServiceWriteAsync sailingServiceWrite;

    protected final ListDataProvider<StrippedLeaderboardDTO> filteredLeaderboardList;

    protected final ErrorReporter errorReporter;

    protected final FlushableCellTable<StrippedLeaderboardDTO> leaderboardTable;

    protected final RaceTableWrapper<RefreshableSelectionModel<RaceColumnDTOAndFleetDTOWithNameBasedEquality>> raceColumnTable;
    protected final RefreshableSelectionModel<RaceColumnDTOAndFleetDTOWithNameBasedEquality> raceColumnTableSelectionModel;

    protected RaceColumnDTOAndFleetDTOWithNameBasedEquality selectedRaceInLeaderboard;

    protected final CaptionPanel selectedLeaderBoardPanel;
    protected final CaptionPanel trackedRacesCaptionPanel;
    protected final List<RegattaDTO> allRegattas;

    protected LabeledAbstractFilterablePanel<StrippedLeaderboardDTO> filterLeaderboardPanel;

    protected List<StrippedLeaderboardDTO> availableLeaderboardList;

    protected final RefreshableMultiSelectionModel<StrippedLeaderboardDTO> leaderboardSelectionModel;

    protected final RefreshableSelectionModel<RaceDTO> refreshableTrackedRaceSelectionModel;
    protected final SelectionChangeEvent.Handler trackedRaceListHandler;
    protected HandlerRegistration trackedRaceListHandlerRegistration;

    protected final Presenter presenter;
    private final Button reloadAllRaceLogs;

    protected UserService userService;
    protected SubscriptionServiceFactory subscriptionServiceFactory;
    
    private final Displayer<StrippedLeaderboardDTO> leaderboardsDisplayer = new Displayer<StrippedLeaderboardDTO>() {
        @Override
        public void fill(Iterable<StrippedLeaderboardDTO> result) {
            fillLeaderboards(result);
        }
    };
    
    public Displayer<StrippedLeaderboardDTO> getLeaderboardsDisplayer() {
        return leaderboardsDisplayer;
    }
    
    private final Displayer<RegattaDTO> regattasDisplayer = new Displayer<RegattaDTO>() {
        @Override
        public void fill(Iterable<RegattaDTO> result) {
            fillRegattas(result);
        }
    };
    
    public Displayer<RegattaDTO> getRegattasDisplayer() {
        return regattasDisplayer;
    }

    public static class RaceColumnDTOAndFleetDTOWithNameBasedEquality
            extends Triple<RaceColumnDTO, FleetDTO, StrippedLeaderboardDTO> {
        private static final long serialVersionUID = -8742476113296862662L;

        public RaceColumnDTOAndFleetDTOWithNameBasedEquality(RaceColumnDTO a, FleetDTO b,
                StrippedLeaderboardDTO c) {
            super(a, b, c);
        }

        @Override
        public int hashCode() {
            return getA().getName().hashCode() ^ getB().getName().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else {
                if (obj == null) {
                    return false;
                } else {
                    if (obj instanceof RaceColumnDTOAndFleetDTOWithNameBasedEquality) {
                        RaceColumnDTOAndFleetDTOWithNameBasedEquality namedObj = (RaceColumnDTOAndFleetDTOWithNameBasedEquality) obj;
                        return equalNamesOrBothNull(getA(), namedObj.getA())
                                && equalNamesOrBothNull(getB(), namedObj.getB());
                    } else {
                        throw new IllegalArgumentException("Unexpected compare");
                    }
                }
            }
        }

        private boolean equalNamesOrBothNull(NamedDTO a, NamedDTO b) {
            if (a == null) {
                return b == null;
            } else {
                if (b == null) {
                    return false;
                } else {
                    return Util.equalsWithNull(a.getName(), b.getName());
                }
            }
        }
    }

    public AbstractLeaderboardConfigPanel(final Presenter presenter, StringMessages theStringConstants, boolean multiSelection) {
        this.stringMessages = theStringConstants;
        this.sailingServiceWrite = presenter.getSailingService();
        this.userService = presenter.getUserService();
        this.subscriptionServiceFactory = presenter.getSubscriptionServiceFactory();
        filteredLeaderboardList = new ListDataProvider<>();
        allRegattas = new ArrayList<RegattaDTO>();
        this.errorReporter = presenter.getErrorReporter();
        this.presenter = presenter;
        this.availableLeaderboardList = new ArrayList<>();
        mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        this.setWidget(mainPanel);
        // Create leaderboards list and functionality
        CaptionPanel leaderboardsCaptionPanel = new CaptionPanel(stringMessages.leaderboards());
        leaderboardsCaptionPanel.setStyleName("bold");
        mainPanel.add(leaderboardsCaptionPanel);
        VerticalPanel leaderboardsPanel = new VerticalPanel();
        leaderboardsCaptionPanel.add(leaderboardsPanel);
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, LEADERBOARD);
        Label lblFilterEvents = new Label(stringMessages.filterLeaderboardsByName() + ": ");
        leaderboardsPanel.add(buttonPanel);
        final Button createLeaderboardRefreshBtn = buttonPanel.addAction(stringMessages.refresh(), ()->true, new Command() {
            @Override
            public void execute() {
                getLeaderboardsRefresher().reloadAndCallFillAll();
            }
        });
        createLeaderboardRefreshBtn.ensureDebugId("LeaderboardRefreshButton");
        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        leaderboardTable = new FlushableCellTable<StrippedLeaderboardDTO>(/* pageSize */10000, tableRes);
        filterLeaderboardPanel = new LabeledAbstractFilterablePanel<StrippedLeaderboardDTO>(lblFilterEvents,
                availableLeaderboardList, filteredLeaderboardList, stringMessages) {
            @Override
            public List<String> getSearchableStrings(StrippedLeaderboardDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(t.displayName);
                for (final CourseAreaDTO courseArea : t.courseAreas) {
                    strings.add(courseArea.getName());
                }
                return strings;
            }

            @Override
            public AbstractCellTable<StrippedLeaderboardDTO> getCellTable() {
                return leaderboardTable;
            }
        };
        filterLeaderboardPanel.getTextBox().ensureDebugId("LeaderboardsFilterTextBox");
        filterLeaderboardPanel
                .setUpdatePermissionFilterForCheckbox(leaderboard -> userService.hasPermission(leaderboard, DefaultActions.UPDATE));
        leaderboardsPanel.add(filterLeaderboardPanel);
        leaderboardTable.ensureDebugId("AvailableLeaderboardsTable");
        addColumnsToLeaderboardTableAndSetSelectionModel(userService, leaderboardTable, tableRes,
                filterLeaderboardPanel.getAllListDataProvider());
        @SuppressWarnings("unchecked")
        RefreshableMultiSelectionModel<StrippedLeaderboardDTO> multiSelectionModel = (RefreshableMultiSelectionModel<StrippedLeaderboardDTO>) leaderboardTable
                .getSelectionModel();
        leaderboardSelectionModel = multiSelectionModel;
        leaderboardTable.setWidth("100%");
        leaderboardSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            public void onSelectionChange(SelectionChangeEvent event) {
                if (trackedRacesListComposite != null) {
                    trackedRacesListComposite.setRegattaFilterValue(getSelectedLeaderboardName());
                }
                leaderboardSelectionChanged();
                reloadAllRaceLogs.setVisible(userService.hasPermission(getSelectedLeaderboard(), UPDATE));
                raceColumnTable.setSelectedLeaderboardName(getSelectedLeaderboardName());
            }
        });
        addLeaderboardControls(buttonPanel);
        filteredLeaderboardList.addDataDisplay(leaderboardTable);
        leaderboardsPanel.add(leaderboardTable);
        final Grid hPanel = new Grid(1, 2);
        Label helpLabel = new Label(stringMessages.helptextLinkingRaces());
        hPanel.setWidget(0, 0, helpLabel);
        hPanel.setWidget(0, 1, new HelpButton(HelpButtonResources.INSTANCE,
                stringMessages.videoGuide(), "https://sapsailing-documentation.s3-eu-west-1.amazonaws.com/adminconsole/LinkingEx.mp4"));
        mainPanel.add(hPanel);
        // caption panels for the selected leaderboard and tracked races
        final HorizontalPanel splitPanel = new HorizontalPanel();
        splitPanel.ensureDebugId("LeaderboardDetailsPanel");
        splitPanel.setWidth("100%");
        mainPanel.add(splitPanel);
        selectedLeaderBoardPanel = new CaptionPanel(stringMessages.leaderboard());
        splitPanel.add(selectedLeaderBoardPanel);
        VerticalPanel vPanel = new VerticalPanel();
        selectedLeaderBoardPanel.setContentWidget(vPanel);
        trackedRacesCaptionPanel = new CaptionPanel(stringMessages.trackedRaces());
        splitPanel.add(trackedRacesCaptionPanel);
        VerticalPanel trackedRacesPanel = new VerticalPanel();
        trackedRacesCaptionPanel.setContentWidget(trackedRacesPanel);
        trackedRacesCaptionPanel.setStyleName("bold");
        trackedRacesListComposite = new TrackedRacesListComposite(null, null, presenter, stringMessages,
                /* multiselection */false, isActionButtonsEnabled());
        refreshableTrackedRaceSelectionModel = trackedRacesListComposite.getSelectionModel();
        trackedRacesListComposite.ensureDebugId("TrackedRacesListComposite");
        trackedRacesPanel.add(trackedRacesListComposite);
        trackedRacesListComposite.addTrackedRaceChangeListener(this);
        trackedRaceListHandler = new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Set<RaceDTO> selectedRaces = refreshableTrackedRaceSelectionModel.getSelectedSet();
                RaceColumnDTOAndFleetDTOWithNameBasedEquality selectedRaceColumnAndFleetName = getSelectedRaceColumnWithFleet();
                // if no leaderboard column is selected, ignore the race selection change
                if (selectedRaceColumnAndFleetName != null) {
                    final StrippedLeaderboardDTO selectedLeaderboard = getSelectedLeaderboard();
                    if (userService.hasPermission(selectedLeaderboard, UPDATE)) {
                        RaceColumnDTO selectedRaceColumn = selectedRaceColumnAndFleetName.getA();
                        FleetDTO selectedRaceColumnFleet = selectedRaceColumnAndFleetName.getB();
                        if (selectedRaces.isEmpty()) {
                            if (hasLink(selectedRaceColumnAndFleetName)) {
                                unlinkRaceColumnFromTrackedRace(selectedRaceColumn.getRaceColumnName(),
                                        selectedRaceColumnFleet);
                            }
                        } else {
                            RaceDTO selectedRace = selectedRaces.iterator().next();
                            if (hasLink(selectedRaceColumnAndFleetName)
                                    && !isLinkedToRace(selectedRaceColumnAndFleetName, selectedRace)) {
                                if (Window.confirm(stringMessages.trackedRaceAlreadyLinked())) {
                                    linkTrackedRaceToSelectedRaceColumn(selectedRaceColumn, selectedRaceColumnFleet,
                                            selectedRace.getRaceIdentifier());
                                } else {
                                    selectTrackedRaceInRaceList();
                                }
                            } else {
                                linkTrackedRaceToSelectedRaceColumn(selectedRaceColumn, selectedRaceColumnFleet,
                                        selectedRace.getRaceIdentifier());
                            }
                        }
                    } else {
                        removeTrackedRaceListHandlerTemporarily();
                        raceColumnTableSelectionModel.clear();
                        if (!selectedRaces.isEmpty()) {
                            Scheduler.get().scheduleDeferred(() -> {
                                final RaceDTO race = selectedRaces.iterator().next();
                                trackedRacesListComposite.selectRaceByIdentifier(race.getRaceIdentifier());
                            });
                        }
                    }
                }
            }

            private boolean hasLink(RaceColumnDTOAndFleetDTOWithNameBasedEquality selectedRaceColumnAndFleetName) {
                return selectedRaceColumnAndFleetName.getA()
                        .getRaceIdentifier(selectedRaceColumnAndFleetName.getB()) != null;
            }

            private boolean isLinkedToRace(RaceColumnDTOAndFleetDTOWithNameBasedEquality selectedRaceColumnAndFleetName,
                    RaceDTO selectedRace) {
                return selectedRaceColumnAndFleetName.getA().getRaceIdentifier(selectedRaceColumnAndFleetName.getB())
                        .equals(selectedRace.getRaceIdentifier());
            }
        };
        trackedRaceListHandlerRegistration = refreshableTrackedRaceSelectionModel
                .addSelectionChangeHandler(trackedRaceListHandler);
        this.reloadAllRaceLogs = new Button(stringMessages.reloadAllRaceLogs());
        reloadAllRaceLogs.ensureDebugId("ReloadAllRaceLogsButton");
        reloadAllRaceLogs.addClickHandler(event -> {
            StrippedLeaderboardDTO leaderboard = getSelectedLeaderboard();
            for (RaceColumnDTO column : leaderboard.getRaceList()) {
                for (FleetDTO fleet : column.getFleets()) {
                    refreshRaceLog(column, fleet, false);
                }
            }
            Notification.notify(stringMessages.raceLogReloaded(), NotificationType.SUCCESS);
        });

        vPanel.add(reloadAllRaceLogs);
        Label lblRaceNamesIn = new Label(stringMessages.races());
        vPanel.add(lblRaceNamesIn);
        raceColumnTable = new RaceTableWrapper<RefreshableSelectionModel<RaceColumnDTOAndFleetDTOWithNameBasedEquality>>(
                sailingServiceWrite, stringMessages, errorReporter, multiSelection);
        raceColumnTable.asWidget().ensureDebugId("RaceColumnTable");
        raceColumnTable.getTable().setWidth("100%");
        addColumnsToRacesTable(raceColumnTable.getTable());
        this.raceColumnTableSelectionModel = raceColumnTable.getSelectionModel();
        raceColumnTableSelectionModel.addSelectionChangeHandler(event -> {
            // If the selection on the raceColumnTable changes,
            // you don't want to link or unlink raceColumns with the
            // trackedRaceListHandler.
            removeTrackedRaceListHandlerTemporarily();
            leaderboardRaceColumnSelectionChanged();
        });
        vPanel.add(raceColumnTable);
        HorizontalPanel selectedLeaderboardRaceButtonPanel = new HorizontalPanel();
        selectedLeaderboardRaceButtonPanel.setSpacing(5);
        vPanel.add(selectedLeaderboardRaceButtonPanel);
        addSelectedLeaderboardRacesControls(selectedLeaderboardRaceButtonPanel);
    }

    protected boolean isActionButtonsEnabled() {
        return /* actionButtonsEnabled */ false;
    }

    protected abstract void addLeaderboardControls(final AccessControlledButtonPanel buttonPanel);

    protected abstract void addSelectedLeaderboardRacesControls(Panel racesPanel);

    protected abstract void addColumnsToLeaderboardTableAndSetSelectionModel(UserService userService,
            FlushableCellTable<StrippedLeaderboardDTO> leaderboardTable,
            AdminConsoleTableResources tableRes, ListDataProvider<StrippedLeaderboardDTO> listDataProvider);

    protected abstract void addColumnsToRacesTable(CellTable<RaceColumnDTOAndFleetDTOWithNameBasedEquality> racesTable);

    protected SelectionCheckboxColumn<StrippedLeaderboardDTO> createSelectionCheckboxColumn(
            final FlushableCellTable<StrippedLeaderboardDTO> leaderboardTable,
            AdminConsoleTableResources tableResources,
            ListHandler<StrippedLeaderboardDTO> leaderboardColumnListHandler,
            ListDataProvider<StrippedLeaderboardDTO> listDataProvider) {
        final SelectionCheckboxColumn<StrippedLeaderboardDTO> selectionCheckboxColumn = new SelectionCheckboxColumn<StrippedLeaderboardDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(),
                new NameBasedStrippedLeaderboardDTOEntityIdentityComparator(), listDataProvider);
        final Header<Boolean> selectAllHeader = selectionCheckboxColumn.createHeader();
        leaderboardTable.addColumn(selectionCheckboxColumn, selectAllHeader);
        return selectionCheckboxColumn;
    }

    public void fillLeaderboards(Iterable<StrippedLeaderboardDTO> result) {
        availableLeaderboardList.clear();
        Util.addAll(result, availableLeaderboardList);
        filterLeaderboardPanel.updateAll(availableLeaderboardList); // also maintains the filtered leaderboardList    
        leaderboardSelectionChanged();
        leaderboardRaceColumnSelectionChanged();
    }

    public void loadAndRefreshLeaderboard(final String leaderboardName) {
        MarkedAsyncCallback<StrippedLeaderboardDTO> callback = new MarkedAsyncCallback<StrippedLeaderboardDTO>(
                new AsyncCallback<StrippedLeaderboardDTO>() {
                    @Override
                    public void onSuccess(StrippedLeaderboardDTO leaderboard) {
                        for (StrippedLeaderboardDTO leaderboardDTO : leaderboardSelectionModel
                                .getSelectedSet()) {
                            if (leaderboardDTO.getName().equals(leaderboardName)) {
                                leaderboardSelectionModel.setSelected(leaderboardDTO, false);
                                break;
                            }
                        }
                        replaceLeaderboardInList(availableLeaderboardList, leaderboardName, leaderboard);
                        filterLeaderboardPanel.updateAll(availableLeaderboardList); // also updates leaderboardList
                                                                                    // provider
                        leaderboardSelectionModel.setSelected(leaderboard, true);
                        leaderboardSelectionChanged();
                        getLeaderboardsRefresher().updateAndCallFillForAll(filteredLeaderboardList.getList(),
                                AbstractLeaderboardConfigPanel.this.getLeaderboardsDisplayer());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        AbstractLeaderboardConfigPanel.this.errorReporter
                                .reportError("Error trying to update leaderboard with name " + leaderboardName + " : "
                                        + t.getMessage());
                    }
                });
        sailingServiceWrite.getLeaderboardWithSecurity(leaderboardName, callback);
    }
    
    public void loadAndRefreshLeaderboard(final StrippedLeaderboardDTO leaderboard) {
        for (StrippedLeaderboardDTO leaderboardDTO : leaderboardSelectionModel.getSelectedSet()) {
            if (leaderboardDTO.getName().equals(leaderboard.getName())) {
                leaderboardSelectionModel.setSelected(leaderboardDTO, false);
                break;
            }
        }
        replaceLeaderboardInList(availableLeaderboardList, leaderboard.getName(), leaderboard);
        filterLeaderboardPanel.updateAll(availableLeaderboardList); // also updates leaderboardList provider
        leaderboardSelectionModel.setSelected(leaderboard, true);
        leaderboardSelectionChanged();
        getLeaderboardsRefresher().updateAndCallFillForAll(filteredLeaderboardList.getList(), this.getLeaderboardsDisplayer());

    }

    private void replaceLeaderboardInList(List<StrippedLeaderboardDTO> leaderboardList,
            String leaderboardToReplace, StrippedLeaderboardDTO newLeaderboard) {
        int index = -1;
        for (StrippedLeaderboardDTO existingLeaderboard : leaderboardList) {
            index++;
            if (existingLeaderboard.getName().equals(leaderboardToReplace)) {
                break;
            }
        }
        if (index >= 0) {
            leaderboardList.set(index, newLeaderboard);
        }
    }

    protected void unlinkRaceColumnFromTrackedRace(final String raceColumnName, final FleetDTO fleet) {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        sailingServiceWrite.disconnectLeaderboardColumnFromTrackedRace(selectedLeaderboardName, raceColumnName,
                fleet.getName(), new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to unlink tracked race from column " + raceColumnName
                                + " from leaderboard " + selectedLeaderboardName + ": " + t.getMessage());
                    }

                    @Override
                    public void onSuccess(Void arg0) {
                        trackedRacesListComposite.clearSelection();
                        getSelectedRaceColumnWithFleet().getA().setRaceIdentifier(fleet, null);
                        raceColumnTable.getDataProvider().refresh();
                    }
                });
    }

    protected void refreshRaceLog(final RaceColumnDTO raceColumnDTO, final FleetDTO fleet, final boolean showAlerts) {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        sailingServiceWrite.reloadRaceLog(selectedLeaderboardName, raceColumnDTO, fleet,
                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        if (showAlerts) {
                            errorReporter.reportError(caught.getMessage());
                        }
                    }

                    @Override
                    public void onSuccess(Void result) {
                        if (showAlerts) {
                            Notification.notify(stringMessages.raceLogReloaded(), NotificationType.SUCCESS);
                        }
                    }
                }));
    }

    protected abstract void leaderboardRaceColumnSelectionChanged();

    protected void selectRaceColumn(String raceColumnName) {
        List<RaceColumnDTOAndFleetDTOWithNameBasedEquality> list = raceColumnTable.getDataProvider().getList();
        for (RaceColumnDTOAndFleetDTOWithNameBasedEquality pair : list) {
            if (pair.getA().getName().equals(raceColumnName)) {
                raceColumnTable.getSelectionModel().setSelected(pair, true);
                break;
            }
        }
    }

    protected void selectTrackedRaceInRaceList() {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        if (selectedLeaderboardName != null) {
            final RaceColumnDTOAndFleetDTOWithNameBasedEquality selectedRaceColumnAndFleetNameInLeaderboard = getSelectedRaceColumnWithFleet();
            final String selectedRaceColumnName = selectedRaceColumnAndFleetNameInLeaderboard.getA()
                    .getRaceColumnName();
            final String selectedFleetName = selectedRaceColumnAndFleetNameInLeaderboard.getB().getName();
            sailingServiceWrite.getRegattaAndRaceNameOfTrackedRaceConnectedToLeaderboardColumn(selectedLeaderboardName,
                    selectedRaceColumnName, new MarkedAsyncCallback<Map<String, RegattaAndRaceIdentifier>>(
                            new AsyncCallback<Map<String, RegattaAndRaceIdentifier>>() {
                                @Override
                                public void onFailure(Throwable t) {
                                    errorReporter
                                            .reportError("Error trying to determine tracked race linked to race column "
                                                    + selectedRaceColumnName + " in leaderboard "
                                                    + selectedLeaderboardName + ": " + t.getMessage());
                                }

                                @Override
                                public void onSuccess(
                                        Map<String, RegattaAndRaceIdentifier> regattaAndRaceNamesPerFleet) {
                                    // This method should select the linked trackedRace.
                                    // So you don't want to link or unlink it again throw the trackedRaceListHandler.
                                    removeTrackedRaceListHandlerTemporarily();
                                    if (regattaAndRaceNamesPerFleet != null && !regattaAndRaceNamesPerFleet.isEmpty()) {
                                        RegattaAndRaceIdentifier raceIdentifier = regattaAndRaceNamesPerFleet
                                                .get(selectedFleetName);
                                        if (raceIdentifier != null) {
                                            selectRaceInList(raceIdentifier.getRegattaName(),
                                                    raceIdentifier.getRaceName());
                                        } else {
                                            trackedRacesListComposite.clearSelection();
                                        }
                                    } else {
                                        trackedRacesListComposite.clearSelection();
                                    }
                                }
                            }));
        }
    }

    protected void selectRaceInList(String regattaName, String raceName) {
        RegattaNameAndRaceName raceIdentifier = new RegattaNameAndRaceName(regattaName, raceName);
        trackedRacesListComposite.selectRaceByIdentifier(raceIdentifier);
    }

    protected RaceColumnDTOAndFleetDTOWithNameBasedEquality getSelectedRaceColumnWithFleet() {
        if (raceColumnTable.getSelectionModel().getSelectedSet().isEmpty()) {
            return null;
        }
        return raceColumnTable.getSelectionModel().getSelectedSet().iterator().next();
    }

    protected String getSelectedLeaderboardName() {
        return getSelectedLeaderboard() != null ? getSelectedLeaderboard().getName() : null;
    }

    protected boolean canBoatsOfCompetitorsChangePerRace() {
        return getSelectedLeaderboard() != null ? getSelectedLeaderboard().canBoatsOfCompetitorsChangePerRace : false;
    }

    protected abstract void leaderboardSelectionChanged();

    public void fillRegattas(Iterable<RegattaDTO> regattas) {
        removeTrackedRaceListHandlerTemporarily();
        trackedRacesListComposite.fillRegattas(regattas);
        allRegattas.clear();
        Util.addAll(regattas, allRegattas);
    }

    @Override
    public void racesStoppedTracking(Iterable<? extends RegattaAndRaceIdentifier> regattaAndRaceIdentifiers) {
        // nothing needs to be done here; the race doesn't change its linkedness status only because it stopped tracking
    }

    /**
     * When a race is removed from the server, it will also have been unlinked. Represent the unlinking by clearing the
     * tracked race link for any race column / fleet that points to it:
     */
    @Override
    public void racesRemoved(Iterable<? extends RegattaAndRaceIdentifier> regattaAndRaceIdentifiers) {
        for (RegattaAndRaceIdentifier regattaAndRaceIdentifier : regattaAndRaceIdentifiers) {
            for (StrippedLeaderboardDTO leaderboard : filteredLeaderboardList.getList()) {
                for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                    for (FleetDTO fleet : raceColumn.getFleets()) {
                        if (Util.equalsWithNull(raceColumn.getRaceIdentifier(fleet), regattaAndRaceIdentifier)) {
                            raceColumn.setRaceIdentifier(fleet, null); // remove link from leaderboard to tracked race
                            raceColumn.getRaceLogTrackingInfo(fleet).raceLogTrackerExists = false;
                        }
                    }
                }
            }
        }
        raceColumnTable.getDataProvider().refresh();
    }

    protected void onTrackedRaceForRaceInRaceColumnTableRemoved(
            RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnAndFleetName) {
        raceColumnAndFleetName.getA().setRaceIdentifier(raceColumnAndFleetName.getB(), null);
    }

    private void linkTrackedRaceToSelectedRaceColumn(final RaceColumnDTO selectedRaceInLeaderboard,
            final FleetDTO fleet, final RegattaAndRaceIdentifier selectedRace) {
        sailingServiceWrite.connectTrackedRaceToLeaderboardColumn(getSelectedLeaderboardName(),
                selectedRaceInLeaderboard.getRaceColumnName(), fleet.getName(), selectedRace,
                new MarkedAsyncCallback<Boolean>(new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to link tracked race " + selectedRace
                                + " to race column named " + selectedRaceInLeaderboard.getRaceColumnName()
                                + " of leaderboard " + getSelectedLeaderboardName() + ": " + t.getMessage());
                        trackedRacesListComposite.clearSelection();
                    }

                    @Override
                    public void onSuccess(Boolean success) {
                        if (success) {
                            // TODO consider enabling the Unlink button
                            selectedRaceInLeaderboard.setRaceIdentifier(fleet, selectedRace);
                            raceColumnTable.getDataProvider().refresh();
                        }
                    }
                }));
    }

    @Override
    public StrippedLeaderboardDTO getSelectedLeaderboard() {
        return leaderboardSelectionModel.getSelectedSet().isEmpty() ? null
                : leaderboardSelectionModel.getSelectedSet().iterator().next();
    }

    protected Refresher<StrippedLeaderboardDTO> getLeaderboardsRefresher() {
        return presenter.getLeaderboardsRefresher();
    }

    protected void editRaceColumnOfLeaderboard(
            final RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnWithFleet) {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        final boolean oldIsMedalRace = raceColumnWithFleet.getA().isMedalRace();
        final String oldRaceColumnName = raceColumnWithFleet.getA().getRaceColumnName();
        final Double oldExplicitFactor = raceColumnWithFleet.getA().getExplicitFactor();
        // use a set to avoid duplicates in the case of regatta leaderboards with multiple fleets per column
        Set<RaceColumnDTO> existingRacesWithoutThisRace = new HashSet<RaceColumnDTO>();
        for (RaceColumnDTOAndFleetDTOWithNameBasedEquality pair : raceColumnTable.getDataProvider().getList()) {
            existingRacesWithoutThisRace.add(pair.getA());
        }
        existingRacesWithoutThisRace.remove(raceColumnWithFleet.getA());
        final RaceColumnInLeaderboardDialog raceDialog = new RaceColumnInLeaderboardDialog(existingRacesWithoutThisRace,
                raceColumnWithFleet.getA(), getSelectedLeaderboard().type.isRegattaLeaderboard(), stringMessages,
                new DialogCallback<RaceColumnDescriptor>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(final RaceColumnDescriptor result) {
                        boolean rename = !oldRaceColumnName.equals(result.getName());
                        boolean updateIsMedalRace = oldIsMedalRace != result.isMedalRace();
                        boolean updateFactor = oldExplicitFactor != result.getExplicitFactor();
                        List<ParallelExecutionCallback<Void>> callbacks = new ArrayList<ParallelExecutionCallback<Void>>();
                        final ParallelExecutionCallback<Void> renameLeaderboardColumnCallback = new ParallelExecutionCallback<Void>();
                        if (rename) {
                            callbacks.add(renameLeaderboardColumnCallback);
                        }
                        final ParallelExecutionCallback<Void> updateIsMedalRaceCallback = new ParallelExecutionCallback<Void>();
                        if (updateIsMedalRace) {
                            callbacks.add(updateIsMedalRaceCallback);
                        }
                        final ParallelExecutionCallback<Void> updateLeaderboardColumnFactorCallback = new ParallelExecutionCallback<Void>();
                        if (updateFactor) {
                            callbacks.add(updateLeaderboardColumnFactorCallback);
                        }
                        new ParallelExecutionHolder(callbacks.toArray(new ParallelExecutionCallback<?>[0])) {
                            @Override
                            public void handleSuccess() {
                                loadAndRefreshLeaderboard(selectedLeaderboardName);
                            }

                            @Override
                            public void handleFailure(Throwable t) {
                                errorReporter
                                        .reportError("Error trying to update data of race column " + oldRaceColumnName
                                                + " in leaderboard " + selectedLeaderboardName + ": " + t.getMessage());
                            }
                        };
                        if (rename) {
                            sailingServiceWrite.renameLeaderboardColumn(selectedLeaderboardName, oldRaceColumnName,
                                    result.getName(), renameLeaderboardColumnCallback);
                        }
                        if (updateIsMedalRace) {
                            sailingServiceWrite.updateIsMedalRace(selectedLeaderboardName, result.getName(),
                                    result.isMedalRace(), updateIsMedalRaceCallback);
                        }
                        if (updateFactor) {
                            sailingServiceWrite.updateLeaderboardColumnFactor(selectedLeaderboardName, result.getName(),
                                    result.getExplicitFactor(), updateLeaderboardColumnFactorCallback);
                        }
                    }
                });
        raceDialog.show();
    }

    protected void showRaceLog(final RaceColumnDTO raceColumnDTO, final FleetDTO fleetDTO) {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        sailingServiceWrite.getRaceLog(selectedLeaderboardName, raceColumnDTO, fleetDTO,
                new MarkedAsyncCallback<RaceLogDTO>(new AsyncCallback<RaceLogDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(caught.getMessage(), true);
                    }

                    @Override
                    public void onSuccess(RaceLogDTO result) {
                        openRaceLogDialog(result);
                    }
                }));
    }

    private void openRaceLogDialog(RaceLogDTO raceLogDTO) {
        RaceLogDialog dialog = new RaceLogDialog(raceLogDTO, stringMessages, new DialogCallback<RaceLogDTO>() {
            @Override
            public void cancel() {
            }

            @Override
            public void ok(RaceLogDTO result) {
            }
        });
        dialog.show();
    }

    protected void showRegattaLog() {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        sailingServiceWrite.getRegattaLog(selectedLeaderboardName,
                new MarkedAsyncCallback<RegattaLogDTO>(new AsyncCallback<RegattaLogDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(caught.getMessage(), true);
                    }

                    @Override
                    public void onSuccess(RegattaLogDTO result) {
                        openRegattaLogDialog(result);
                    }
                }));
    }

    private void openRegattaLogDialog(RegattaLogDTO regattaLogDTO) {
        RegattaLogDialog dialog = new RegattaLogDialog(regattaLogDTO, stringMessages,
                new DialogCallback<RegattaLogDTO>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(RegattaLogDTO result) {
                    }
                });
        dialog.show();
    }

    /**
     * Removes the {@link SelectionChangeEvent.Handler} until the browser regains control. The handler will be added
     * again using {@link Scheduler#scheduleDeferred(ScheduledCommand)} method.
     * <p>
     * Use this method if you change the {@link ListDataProvider} or {@link RefreshableSelectionModel} of
     * {@link TrackedRacesListComposite} and you don't want to trigger the
     * {@link SelectionChangeEvent.Handler#onSelectionChange(SelectionChangeEvent)}.
     */
    private void removeTrackedRaceListHandlerTemporarily() {
        if (trackedRaceListHandlerRegistration == null) {
            return;
        }
        trackedRaceListHandlerRegistration.removeHandler();
        trackedRaceListHandlerRegistration = null;
        // It is necessary to do this with the ScheduleDeferred() method,
        // because the SelectionChangeEvent isn't fired directly after
        // selection changes. So an remove of SelectionChangeHandler before
        // the selection change and and new registration directly after it
        // isn't possible.
        Scheduler.get().scheduleDeferred(() -> trackedRaceListHandlerRegistration = refreshableTrackedRaceSelectionModel
                .addSelectionChangeHandler(trackedRaceListHandler));
    }

    /**
     * Looks up the regatta for the selected leaderboard by name in {@link #allRegattas}
     */
    protected RegattaDTO getSelectedRegatta() {
        final String regattaName = getSelectedLeaderboard() == null ? "" : getSelectedLeaderboard().regattaName;
        return getRegattaByName(regattaName);
    }

    /**
     * Looks up a regatta with name {@code regattaName} in {@link #allRegattas}
     */
    protected RegattaDTO getRegattaByName(final String regattaName) {
        RegattaDTO regatta = null;
        if (regattaName != null) {
            if (allRegattas != null) {
                for (RegattaDTO i : allRegattas) {
                    if (regattaName.equals(i.getName())) {
                        regatta = i;
                        break;
                    }
                }
            }
        }
        return regatta;
    }

    protected void assignCertificates(RaceColumnDTOAndFleetDTOWithNameBasedEquality object) {
        BoatCertificateAssignmentDialog dialog = new BoatCertificateAssignmentDialog(sailingServiceWrite, userService,
                stringMessages, errorReporter, new RaceBoatCertificatesPanel(sailingServiceWrite, userService, object.getC(), object.getA(), object.getB(), stringMessages, errorReporter));
        dialog.show();
    }

    protected void selectScratchBoat(RaceColumnDTOAndFleetDTOWithNameBasedEquality object) {
        sailingServiceWrite.getORCPerformanceCurveScratchBoat(object.getC().getName(), object.getA().getName(), object.getB().getName(), new AsyncCallback<CompetitorDTO>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorObtainingScratchBoat(caught.getMessage()), /* silent */ true);
            }

            @Override
            public void onSuccess(CompetitorDTO scratchBoatSoFar) {
                new CompetitorSelectionDialog(sailingServiceWrite, userService, errorReporter,
                        stringMessages.selectScratchBoat(), stringMessages.selectScratchBoat(), getRaceCompetitorProvider(object), stringMessages, scratchBoatSoFar,
                        new DialogCallback<CompetitorDTO>() {
                            @Override
                            public void ok(CompetitorDTO newScratchBoat) {
                                sailingServiceWrite.setORCPerformanceCurveScratchBoat(object.getC().getName(), object.getA().getName(), object.getB().getName(),
                                        newScratchBoat, new AsyncCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError(stringMessages.errorSettingScratchBoat(caught.getMessage()), /* silent */ true);
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        Notification.notify(stringMessages.scratchBoatSetSuccessfully(), NotificationType.SUCCESS);
                                    }
                                });
                            }

                            @Override
                            public void cancel() {}
                }).show();
            }
        });
    }
    
    protected void setImpliedWind(RaceColumnDTOAndFleetDTOWithNameBasedEquality object) {
        final String raceDisplayName = object.getC().getName() + "/" + object.getA().getName() + "/" + object.getB().getName();
        sailingServiceWrite.getImpliedWindSource(object.getC().getName(), object.getA().getName(), object.getB().getName(),
                new AsyncCallback<ImpliedWindSource>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorObtainingImpliedWindSourceForRace(
                                raceDisplayName,
                                caught.getMessage()));
                    }

                    @Override
                    public void onSuccess(ImpliedWindSource result) {
                        new ImpliedWindSourceEditDialog(object, result, availableLeaderboardList, stringMessages, errorReporter, sailingServiceWrite, userService, new DialogCallback<ImpliedWindSource>() {
                            @Override
                            public void ok(ImpliedWindSource editedObject) {
                                sailingServiceWrite.setImpliedWindSource(object.getC().getName(), object.getA().getName(), object.getB().getName(), editedObject,
                                        new AsyncCallback<Void>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                errorReporter.reportError(
                                                        stringMessages.errorSettingImpliedWindSourceForRace(raceDisplayName,
                                                                caught.getMessage()));
                                            }

                                            @Override
                                            public void onSuccess(Void result) {
                                                Notification.notify(stringMessages.impliedWindForRaceSetSuccessfully(raceDisplayName), NotificationType.SUCCESS);
                                            }
                                });
                            }

                            @Override
                            public void cancel() {
                            }
                        }).show();
                    }
        });
    }

    /**
     * Helps in obtaining competitors for a specific "race slot" identified by leaderboard name, race column name and
     * fleet name. In particular, the implementation is expected to return a consumer that when called with a callback
     * fetches the competitors for the particular race identified by {@code raceSlotIdentifier} and sends them to the
     * callback's {@link AsyncCallback#onSuccess(Object)} method.
     */
    protected Consumer<AsyncCallback<Iterable<? extends CompetitorDTO>>> getRaceCompetitorProvider(
            RaceColumnDTOAndFleetDTOWithNameBasedEquality raceSlotIdentifier) {
        return callback -> sailingServiceWrite.getCompetitorsAndBoatsOfRace(raceSlotIdentifier.getC().getName(),
                raceSlotIdentifier.getA().getName(), raceSlotIdentifier.getB().getName(), new AsyncCallback<Map<? extends CompetitorDTO, BoatDTO>>() {
            @Override
            public void onFailure(Throwable e) {
                callback.onFailure(e);
            }
            
            @Override
            public void onSuccess(Map<? extends CompetitorDTO, BoatDTO> competitorToBoatMap) {
                callback.onSuccess(competitorToBoatMap.keySet());
            }
        });
    }
    
    @Override
    public AbstractFilterablePanel<StrippedLeaderboardDTO> getFilterablePanel() {
        return filterLeaderboardPanel;
    }
}