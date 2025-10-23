package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.ColumnFormatter;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.adminconsole.tractrac.TracTracConnectionDialog;
import com.sap.sailing.gwt.ui.adminconsole.tractrac.TracTracConnectionTableWrapper;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.TracTracRaceRecordDTO;
import com.sap.sse.common.Duration;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;

/**
 * Allows the user to start and stop tracking of events, regattas and races using the TracTrac connector. In particular,
 * previously configured connections can be retrieved from a drop-down list which then pre-populates all connection
 * parameters. The user can also choose to enter connection information manually. Using a "hierarchical" entry system
 * comparable to that of, e.g., the Eclipse CVS connection setup wizard, components entered will be used to
 * automatically assemble the full URL which can still be overwritten manually. There is a propagation order across the
 * fields. Hostname propagates to JSON URL, Live URI and Stored URI. Port Live Data propagates to Port Stored Data,
 * incremented by one. The ports propagate to Live URI and Stored URI, respectively. The event name propagates to the
 * JSON URL.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class TracTracEventManagementPanel extends AbstractEventManagementPanel implements FilterablePanelProvider<TracTracConfigurationWithSecurityDTO> {
    private final ErrorReporter errorReporter;
    
    private final List<TracTracRaceRecordDTO> availableTracTracRaces;
    
    private final ListDataProvider<TracTracRaceRecordDTO> raceList;
    
    private TracTracConnectionTableWrapper connectionsTable;

    private Label loadingMessageLabel;

    private LabeledAbstractFilterablePanel<TracTracRaceRecordDTO> racesFilterablePanel;
    private FlushableCellTable<TracTracRaceRecordDTO> racesTable;

    private final UserService userService;
    private final CellTableWithCheckboxResources tableResources;
    private final SailingServiceWriteAsync sailingServiceWrite;

    public TracTracEventManagementPanel(final Presenter presenter, StringMessages stringMessages,
            final CellTableWithCheckboxResources tableResources) {
        super(presenter, true, stringMessages);
        this.userService = presenter.getUserService();
        this.errorReporter = presenter.getErrorReporter();
        this.sailingServiceWrite = presenter.getSailingService();
        this.tableResources = tableResources;
        this.availableTracTracRaces = new ArrayList<TracTracRaceRecordDTO>();
        this.raceList = new ListDataProvider<TracTracRaceRecordDTO>();
        this.setWidget(createContent());
    }
    
    protected Widget createContent() {
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        CaptionPanel connectionsPanel = createConnectionsPanel();
        mainPanel.add(connectionsPanel);
        HorizontalPanel racesPanel = createRacesPanel();
        racesPanel.setWidth("100%");
        mainPanel.add(racesPanel);
        return mainPanel;
    }
    
    protected CaptionPanel createConnectionsPanel() {
        CaptionPanel connectionsPanel = new CaptionPanel("TracTrac " + stringMessages.connections());
        connectionsPanel.ensureDebugId("ConnectionsSection");
        connectionsPanel.setStyleName("bold");
        VerticalPanel tableAndConfigurationPanel = new VerticalPanel();
        connectionsTable = new TracTracConnectionTableWrapper(userService, sailingServiceWrite, stringMessages,
                errorReporter, true, tableResources, () -> {});
        connectionsTable.refreshTracTracConnectionList();
        final Grid grid = new Grid(1, 2);
        grid.setWidget(0, 0, new Label(stringMessages.racesWithHiddenState() + ":"));
        final CheckBox showHiddenRacesCheckbox = new CheckBox(stringMessages.show());
        showHiddenRacesCheckbox.ensureDebugId("ShowHiddenRacesCheckBox");
        grid.setWidget(0, 1, showHiddenRacesCheckbox);
        // Add TracTrac Connection
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, SecuredDomainType.TRACKED_RACE);
        buttonPanel.addUnsecuredAction(stringMessages.refresh(), () -> connectionsTable.refreshTracTracConnectionList());
        Button addCreateAction = buttonPanel.addCreateAction(stringMessages.addTracTracConnection(),
                () -> new TracTracConnectionDialog(
                        new DialogCallback<TracTracConfigurationWithSecurityDTO>() {
                            @Override
                            public void ok(TracTracConfigurationWithSecurityDTO editedConnection) {
                                sailingServiceWrite.createTracTracConfiguration(editedConnection.getName(),
                                        editedConnection.getJsonUrl(), editedConnection.getLiveDataURI(),
                                        editedConnection.getStoredDataURI(),
                                        editedConnection.getUpdateURI(),
                                        editedConnection.getTracTracApiToken(), new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                reportError("Exception trying to create configuration in DB: "
                                                        + caught.getMessage());
                                            }

                                            @Override
                                            public void onSuccess(Void voidResult) {
                                                connectionsTable.refreshTracTracConnectionList(/* selectWhenDone */ editedConnection);
                                            }
                                        }));
                            }

                            @Override
                            public void cancel() {
                            }
                        }, userService, errorReporter).show());
        addCreateAction.ensureDebugId("AddConnectionButton");
        buttonPanel.addRemoveAction(stringMessages.remove(), connectionsTable.getSelectionModel(), false, () -> {
            sailingServiceWrite.deleteTracTracConfigurations(connectionsTable.getSelectionModel().getSelectedSet(),
                    new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(
                                    "Exception trying to delete configuration(s) in DB: " + caught.getMessage());
                        }

                        @Override
                        public void onSuccess(Void result) {
                            connectionsTable.refreshTracTracConnectionList();
                        }
                    });
        });
        loadingMessageLabel = new Label();
        final Button listRacesButton = buttonPanel.addUnsecuredAction(stringMessages.listRaces(), () -> {
            loadingMessageLabel.setText(stringMessages.loading());
            fillRaces(sailingServiceWrite, showHiddenRacesCheckbox.getValue());
        });
        listRacesButton.ensureDebugId("ListRacesButton");
        listRacesButton.setEnabled(false);
        buttonPanel.addUnsecuredWidget(loadingMessageLabel);
        connectionsTable.getSelectionModel().addSelectionChangeHandler(e -> {
            final boolean objectSelected = connectionsTable.getSelectionModel().getSelectedSet().size() == 1;
            listRacesButton.setEnabled(objectSelected);
        });
        tableAndConfigurationPanel.add(buttonPanel);
        tableAndConfigurationPanel.add(grid);
        tableAndConfigurationPanel.add(connectionsTable);
        connectionsPanel.setContentWidget(tableAndConfigurationPanel);
        return connectionsPanel;
    }
    
    protected HorizontalPanel createRacesPanel() {
        HorizontalPanel racesPanel = new HorizontalPanel();
        CaptionPanel trackableRacesPanel = createTrackableRacesPanel();
        racesPanel.add(trackableRacesPanel);
        racesPanel.setCellWidth(trackableRacesPanel, "50%");
        CaptionPanel trackedRacesPanel = createTrackedRacesPanel();
        racesPanel.add(trackedRacesPanel);
        racesPanel.setCellWidth(trackedRacesPanel, "50%");
        return racesPanel;
    }
    
    protected CaptionPanel createTrackableRacesPanel() {
        CaptionPanel trackableRacesPanel = new CaptionPanel(stringMessages.trackableRaces());
        trackableRacesPanel.ensureDebugId("TrackableRacesSection");
        trackableRacesPanel.setStyleName("bold");
        
        FlexTable layoutTable = new FlexTable();
        layoutTable.setWidth("100%");

        ColumnFormatter columnFormatter = layoutTable.getColumnFormatter();
        FlexCellFormatter cellFormatter = layoutTable.getFlexCellFormatter();

        columnFormatter.setWidth(0, "130px");
        //columnFormatter.setWidth(1, "80%");

        // Regatta
        Label regattaForTrackingLabel = new Label(stringMessages.regattaUsedForTheTrackedRace());
        regattaForTrackingLabel.setWordWrap(false);
        int row = 0;
        layoutTable.setWidget(row, 0, regattaForTrackingLabel);
        layoutTable.setWidget(row, 1, getAvailableRegattasListBox());

        // Track settings (wind)
        Label trackSettingsLabel = new Label(stringMessages.trackSettings() + ":");

        final CheckBox trackWindCheckBox = new CheckBox(stringMessages.trackWind());
        trackWindCheckBox.ensureDebugId("TrackWindCheckBox");
        trackWindCheckBox.setWordWrap(false);
        trackWindCheckBox.setValue(Boolean.TRUE);

        final CheckBox correctWindCheckBox = new CheckBox(stringMessages.declinationCheckbox());
        correctWindCheckBox.ensureDebugId("CorrectWindCheckBox");
        correctWindCheckBox.setWordWrap(false);
        correctWindCheckBox.setValue(Boolean.TRUE);

        final SimulationPanel simulationPanel = new SimulationPanel(stringMessages);
        final CheckBox ignoreTracTracMarkPassingsCheckbox = new CheckBox(stringMessages.useInternalAlgorithm());
        ignoreTracTracMarkPassingsCheckbox.setWordWrap(false);
        ignoreTracTracMarkPassingsCheckbox.setValue(Boolean.FALSE);
        final CheckBox useOfficialResultsToUpdateRaceLogsCheckbox = new CheckBox(stringMessages.useOfficialResultsForAutomaticUpdates());
        ignoreTracTracMarkPassingsCheckbox.setWordWrap(false);
        ignoreTracTracMarkPassingsCheckbox.setValue(Boolean.FALSE);
        
        layoutTable.setWidget(++row, 0, trackSettingsLabel);
        layoutTable.setWidget(row, 1, trackWindCheckBox);
        layoutTable.setWidget(++row, 1, correctWindCheckBox);
        layoutTable.setWidget(++row, 1, simulationPanel);
        layoutTable.setWidget(++row, 1, ignoreTracTracMarkPassingsCheckbox);
        layoutTable.setWidget(++row, 1, useOfficialResultsToUpdateRaceLogsCheckbox);
        
        // Filter
        Label racesFilterLabel = new Label(stringMessages.filterRaces() + ":");
        AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);
        racesTable = new FlushableCellTable<TracTracRaceRecordDTO>(10000, tableResources);
        racesTable.ensureDebugId("TrackableRacesCellTable");
        this.racesFilterablePanel = new LabeledAbstractFilterablePanel<TracTracRaceRecordDTO>(racesFilterLabel,
                availableTracTracRaces, raceList, stringMessages) {
            @Override
            public List<String> getSearchableStrings(TracTracRaceRecordDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(t.regattaName);
                if (t.boatClassNames != null) {
                    for (String boatClassName : t.boatClassNames) {
                        strings.add(boatClassName);
                    }
                }
                return strings;
            }

            @Override
            public AbstractCellTable<TracTracRaceRecordDTO> getCellTable() {
                return racesTable;
            }
        };
        racesFilterablePanel.getTextBox().ensureDebugId("TrackableRacesFilterTextBox");
        layoutTable.setWidget(++row, 0, racesFilterLabel);
        layoutTable.setWidget(row, 1, racesFilterablePanel);

        // Races
        TextColumn<TracTracRaceRecordDTO> regattaNameColumn = new TextColumn<TracTracRaceRecordDTO>() {
            @Override
            public String getValue(TracTracRaceRecordDTO object) {
                return object.regattaName;
            }
        };
        regattaNameColumn.setSortable(false);
        TextColumn<TracTracRaceRecordDTO> boatClassColumn = new TextColumn<TracTracRaceRecordDTO>() {
            @Override
            public String getValue(TracTracRaceRecordDTO object) {
                return getBoatClassNamesAsString(object);
            }
        };
        boatClassColumn.setSortable(true);
        TextColumn<TracTracRaceRecordDTO> raceNameColumn = new TextColumn<TracTracRaceRecordDTO>() {
            @Override
            public String getValue(TracTracRaceRecordDTO object) {
                return object.getName();
            }
        };
        raceNameColumn.setSortable(true);
        TextColumn<TracTracRaceRecordDTO> raceStartTrackingColumn = new TextColumn<TracTracRaceRecordDTO>() {
            @Override
            public String getValue(TracTracRaceRecordDTO object) {
                return object.trackingStartTime == null ? "" : dateFormatter.render(object.trackingStartTime) + " "
                        + timeFormatter.render(object.trackingStartTime);
            }
        };
        raceStartTrackingColumn.setSortable(true);
        TextColumn<TracTracRaceRecordDTO> raceStatusColumn = new TextColumn<TracTracRaceRecordDTO>() {
            @Override
            public String getValue(TracTracRaceRecordDTO object) {
                return object.raceStatus;
            }
        };
        raceStatusColumn.setSortable(true);
        TextColumn<TracTracRaceRecordDTO> raceVisibilityColumn = new TextColumn<TracTracRaceRecordDTO>() {
            @Override
            public String getValue(TracTracRaceRecordDTO object) {
                return object.raceVisibility;
            }
        };
        raceVisibilityColumn.setSortable(true);
        SelectionCheckboxColumn<TracTracRaceRecordDTO> selectionCheckboxColumn = new SelectionCheckboxColumn<TracTracRaceRecordDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(),
                new EntityIdentityComparator<TracTracRaceRecordDTO>() {
                    @Override
                    public boolean representSameEntity(TracTracRaceRecordDTO dto1, TracTracRaceRecordDTO dto2) {
                        return dto1.id.equals(dto2.id);
                    }
                    @Override
                    public int hashCode(TracTracRaceRecordDTO t) {
                        return t.id.hashCode();
                    }
                }, racesFilterablePanel.getAllListDataProvider(), racesTable);
        racesTable.addColumn(selectionCheckboxColumn, selectionCheckboxColumn.getHeader());
        racesTable.addColumn(regattaNameColumn, stringMessages.event());
        racesTable.addColumn(raceNameColumn, stringMessages.race());
        racesTable.addColumn(boatClassColumn, stringMessages.boatClass());
        racesTable.addColumn(raceStartTrackingColumn, stringMessages.startTime());
        racesTable.addColumn(raceStatusColumn, stringMessages.raceStatusColumn());
        racesTable.addColumn(raceVisibilityColumn, stringMessages.raceVisibilityColumn());
        racesTable.addColumnSortHandler(getRaceTableColumnSortHandler(selectionCheckboxColumn, this.raceList.getList(),
                raceNameColumn, boatClassColumn, raceStartTrackingColumn, raceStatusColumn, raceVisibilityColumn));
        racesTable.setSelectionModel(selectionCheckboxColumn.getSelectionModel(), selectionCheckboxColumn.getSelectionManager());
        racesTable.setWidth("100%");
        raceList.addDataDisplay(racesTable);
        layoutTable.setWidget(++row, 0, racesTable);
        cellFormatter.setColSpan(row, 0, 2);
        final Button startTrackingButton = new Button(stringMessages.startTracking());
        startTrackingButton.setEnabled(false);
        connectionsTable.getSelectionModel().addSelectionChangeHandler(
                e -> startTrackingButton.setEnabled(connectionsTable.getSelectionModel().getSelectedSet().size() == 1));
        startTrackingButton.ensureDebugId("StartTrackingButton");
        startTrackingButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Duration offsetToStartTimeOfSimulatedRace = null;
                if (simulationPanel.isSimulate()) {
                    offsetToStartTimeOfSimulatedRace = getMillisecondsDurationFromMinutesAsString(simulationPanel.getOffsetToStartTimeInMinutes());
                }
                trackSelectedRaces(trackWindCheckBox.getValue(), correctWindCheckBox.getValue(),
                        offsetToStartTimeOfSimulatedRace, ignoreTracTracMarkPassingsCheckbox.getValue(),
                        useOfficialResultsToUpdateRaceLogsCheckbox.getValue());
            }
        });
        layoutTable.setWidget(++row, 1, startTrackingButton);
        trackableRacesPanel.setContentWidget(layoutTable);
        return trackableRacesPanel;
    }
    
    protected CaptionPanel createTrackedRacesPanel() {
        CaptionPanel trackedRacesPanel = new CaptionPanel(stringMessages.trackedRaces());
        trackedRacesPanel.ensureDebugId("TrackedRacesSection");
        trackedRacesPanel.setStyleName("bold");
        trackedRacesPanel.setContentWidget(this.trackedRacesListComposite);
        return trackedRacesPanel;
    }
    
    protected void reportError(String message) {
        this.errorReporter.reportError(message);
    }
    
    private ListHandler<TracTracRaceRecordDTO> getRaceTableColumnSortHandler(SelectionCheckboxColumn<TracTracRaceRecordDTO> selectionCheckboxColumn,
            List<TracTracRaceRecordDTO> raceRecords, Column<TracTracRaceRecordDTO, ?> nameColumn,
            Column<TracTracRaceRecordDTO, ?> boatClassColumn, Column<TracTracRaceRecordDTO, ?> trackingStartColumn, Column<TracTracRaceRecordDTO, ?> raceStatusColumn,
            Column<TracTracRaceRecordDTO, ?> raceVisibilityColumn) {
        ListHandler<TracTracRaceRecordDTO> result = new ListHandler<TracTracRaceRecordDTO>(raceRecords);
        result.setComparator(selectionCheckboxColumn, selectionCheckboxColumn.getComparator());
        result.setComparator(nameColumn, new Comparator<TracTracRaceRecordDTO>() {
            @Override
            public int compare(TracTracRaceRecordDTO o1, TracTracRaceRecordDTO o2) {
                return new NaturalComparator().compare(o1.getName(), o2.getName());
            }
        });
        result.setComparator(boatClassColumn, new Comparator<TracTracRaceRecordDTO>() {
            @Override
            public int compare(TracTracRaceRecordDTO o1, TracTracRaceRecordDTO o2) {
                return new NaturalComparator(false).compare(getBoatClassNamesAsString(o1), getBoatClassNamesAsString(o2));
            }
        });
        result.setComparator(trackingStartColumn, new Comparator<TracTracRaceRecordDTO>() {
            @Override
            public int compare(TracTracRaceRecordDTO o1, TracTracRaceRecordDTO o2) {
                return o1.trackingStartTime == null ? -1 : o2.trackingStartTime == null ? 1 : o1.trackingStartTime
                        .compareTo(o2.trackingStartTime);
            }
        });
        result.setComparator(raceStatusColumn, new Comparator<TracTracRaceRecordDTO>() {
            @Override
            public int compare(TracTracRaceRecordDTO o1, TracTracRaceRecordDTO o2) {
                return o1.raceStatus == null ? -1 : o2.raceStatus == null ? 1 : o1.raceStatus
                        .compareTo(o2.raceStatus);
            }
        });
        result.setComparator(raceVisibilityColumn, new Comparator<TracTracRaceRecordDTO>() {
            @Override
            public int compare(TracTracRaceRecordDTO o1, TracTracRaceRecordDTO o2) {
                return o1.raceVisibility == null ? -1 : o2.raceVisibility == null ? 1 : o1.raceVisibility
                        .compareTo(o2.raceVisibility);
            }
        });
        return result;
    }
    
    private String getBoatClassNamesAsString(TracTracRaceRecordDTO object) {
        StringBuilder boatClassNames = new StringBuilder();
        for (String boatClassName : object.boatClassNames) {
            boatClassNames.append(boatClassName);
            boatClassNames.append(", ");
        }
        return boatClassNames.substring(0, boatClassNames.length() - 2);
    }
    
    private Duration getMillisecondsDurationFromMinutesAsString(String minutesAsString) {
        Duration result = null;
        if (minutesAsString != null) {
            Double minutesAsDouble = Double.parseDouble(minutesAsString);
            if (minutesAsDouble != null) {
                result = Duration.ONE_MINUTE.times(minutesAsDouble.longValue());
            }
        }
        return result;
    }

    private void fillRaces(final SailingServiceAsync sailingService, boolean listHiddenRaces) {
        final Set<TracTracConfigurationWithSecurityDTO> selectedConnections = connectionsTable.getSelectionModel()
                .getSelectedSet();
        if (selectedConnections.size() == 1) {
            TracTracConfigurationWithSecurityDTO selectedConnection = selectedConnections.iterator().next();
            sailingService.listTracTracRacesInEvent(selectedConnection.getJsonUrl(), listHiddenRaces,
                    new MarkedAsyncCallback<com.sap.sse.common.Util.Pair<String, List<TracTracRaceRecordDTO>>>(
                new AsyncCallback<com.sap.sse.common.Util.Pair<String, List<TracTracRaceRecordDTO>>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        loadingMessageLabel.setText("");
                        reportError("Error trying to list races: " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(final com.sap.sse.common.Util.Pair<String, List<TracTracRaceRecordDTO>> result) {
                        loadingMessageLabel.setText("Building resultset and saving configuration...");
                        TracTracEventManagementPanel.this.availableTracTracRaces.clear();
                                    final TracTracConfigurationWithSecurityDTO updatedConnection = new TracTracConfigurationWithSecurityDTO(
                                            selectedConnection,
                                            result.getA());
                        final List<TracTracRaceRecordDTO> eventRaces = result.getB();
                        if (eventRaces != null) {
                            TracTracEventManagementPanel.this.availableTracTracRaces.addAll(eventRaces);
                            racesFilterablePanel.updateAll(availableTracTracRaces);
                        }
                        List<TracTracRaceRecordDTO> races = TracTracEventManagementPanel.this.raceList.getList();
                        races.clear();
                        races.addAll(TracTracEventManagementPanel.this.availableTracTracRaces);
                        TracTracEventManagementPanel.this.racesFilterablePanel.getTextBox().setText("");
                        TracTracEventManagementPanel.this.racesTable.setPageSize(races.size());
                        loadingMessageLabel.setText("");
                        // store a successful configuration in the database for later retrieval
                        sailingServiceWrite.updateTracTracConfiguration(updatedConnection,
                                new MarkedAsyncCallback<Void>(
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    reportError("Exception trying to store configuration in DB: "  + caught.getMessage());
                                }

                                @Override
                                public void onSuccess(Void voidResult) {
                                    connectionsTable.refreshTracTracConnectionList();
                                }
                            }));
                    }
                }));
        }
    }

    private void trackSelectedRaces(boolean trackWind, boolean correctWind,
            final Duration offsetToStartTimeOfSimulatedRace, boolean ignoreTracTracMarkPassings,
            boolean useOfficialResultsToUpdateRaceLogs) {
        final TracTracConfigurationWithSecurityDTO selectedConnection = connectionsTable.getSelectionModel()
                .getSelectedSet().iterator().next();
        String liveURI = selectedConnection.getLiveDataURI();
        String storedURI = selectedConnection.getStoredDataURI();
        String updateURI = selectedConnection.getUpdateURI();
        String jsonUrlAsKey = selectedConnection.getJsonUrl(); // key for the connection
        RegattaDTO selectedRegatta = getSelectedRegatta(); // null meaning "Default Regatta" selection
        RegattaIdentifier regattaIdentifier = null;
        if (selectedRegatta != null) {
            regattaIdentifier = new RegattaName(selectedRegatta.getName());
        }
        // Check if the assigned regatta makes sense; the following cases need to be distinguished:
        //  - non-default regatta explicitly selected: check that boat class matches; disallow loading when there is a mismatch
        //  - "Default Regatta" selected: if race was assigned to a regatta before, use that without further checks;
        //                                otherwise, warn user if a "persistent" regatta with the same boat class already exists
        //                                because it may be an accidental omission to select that regatta for loading
        List<TracTracRaceRecordDTO> allRaces = raceList.getList();
        SelectionModel<? super TracTracRaceRecordDTO> selectionModel = racesTable.getSelectionModel();
        final List<TracTracRaceRecordDTO> selectedRaces = new ArrayList<>();
        for (TracTracRaceRecordDTO race : allRaces) {
            if (selectionModel.isSelected(race)) {
                selectedRaces.add(race);
            }
        }
        if (checkBoatClassOK(selectedRegatta, selectedRaces)) {
            sailingServiceWrite.trackWithTracTrac(regattaIdentifier, selectedRaces, liveURI, storedURI,
                    updateURI, trackWind, correctWind, offsetToStartTimeOfSimulatedRace, ignoreTracTracMarkPassings,
                    useOfficialResultsToUpdateRaceLogs, jsonUrlAsKey,
                    new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            reportError(stringMessages.errorTryingToRegisterRacesForTracking(selectedRaces.toString(), caught.getMessage()));
                        }

                        @Override
                        public void onSuccess(Void result) {
                            TracTracEventManagementPanel.this.presenter.getRegattasRefresher().reloadAndCallFillAll();
                        }
                    }));
        }
    }
    
    public void refreshTracTracConnectors() {
        connectionsTable.refreshTracTracConnectionList();
    }
    
    @Override
    public AbstractFilterablePanel<TracTracConfigurationWithSecurityDTO> getFilterablePanel() {
        return connectionsTable.getFilterField();
    }
}
