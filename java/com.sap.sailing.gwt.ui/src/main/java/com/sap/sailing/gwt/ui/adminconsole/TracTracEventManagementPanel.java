package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.ColumnFormatter;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.gwt.ui.client.RaceSelectionModel;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.SelectionCheckboxColumn;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.TracTracRaceRecordDTO;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;

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
public class TracTracEventManagementPanel extends AbstractEventManagementPanel {
    private final ErrorReporter errorReporter;
    
    private final Map<String, TracTracConfigurationDTO> previousConfigurations;

    private final List<TracTracRaceRecordDTO> availableTracTracRaces;
    
    private final ListDataProvider<TracTracRaceRecordDTO> raceList;
    
    private ListBox connectionsHistoryListBox;
    
    private TextBox storedURITextBox;
    private TextBox liveURITextBox;
    private TextBox jsonURLTextBox;
    private TextBox tracTracUpdateURITextBox;
    private TextBox tractracUsernameTextBox;
    private TextBox tractracPasswordTextBox;
    private Label loadingMessageLabel;

    private LabeledAbstractFilterablePanel<TracTracRaceRecordDTO> racesFilterablePanel;
    private CellTable<TracTracRaceRecordDTO> racesTable;
    private static final String ZERO_AS_STRING = "0";
    
    public TracTracEventManagementPanel(final SailingServiceAsync sailingService, ErrorReporter errorReporter,
            RegattaRefresher regattaRefresher, StringMessages stringMessages) {
        super(sailingService, regattaRefresher, errorReporter, new RaceSelectionModel(), true, stringMessages);
        this.errorReporter = errorReporter;
        this.previousConfigurations = new HashMap<String, TracTracConfigurationDTO>();
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
        
        fillConfigurations();
        
        return mainPanel;
    }
    
    protected CaptionPanel createConnectionsPanel() {
        CaptionPanel connectionsPanel = new CaptionPanel("TracTrac " + stringMessages.connections());
        connectionsPanel.ensureDebugId("ConnectionsSection");
        connectionsPanel.setStyleName("bold");

        Grid grid = new Grid(10, 3);
        connectionsPanel.setContentWidget(grid);
        
        // Existing TracTrac connections
        Label connectionsHistoryLabel = new Label("TracTrac " + stringMessages.connections() + ":");

        connectionsHistoryListBox = new ListBox();
        connectionsHistoryListBox.ensureDebugId("ConnectionHistory");
        connectionsHistoryListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                updatePanelFromSelectedStoredConfiguration();
            }
        });
        connectionsHistoryListBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                // TODO: This leads to a strange behavior (values are updated by opening the drop down) but at the
                //       moment there is no other way to "reset" the values to the selected value, if the selection does
                //       not change since there is no close event or something similar.
                updatePanelFromSelectedStoredConfiguration();
            }
        });

        grid.setWidget(0, 0, connectionsHistoryLabel);
        grid.setWidget(0, 1, connectionsHistoryListBox);

        // URIs (Live and Stored)
        grid.setWidget(1, 0, new Label(stringMessages.details() + ":"));

        Label liveURILabel = new Label(stringMessages.liveUri() + ":");
        liveURILabel.setTitle(stringMessages.leaveEmptyForDefault());

        liveURITextBox = new TextBox();
        liveURITextBox.ensureDebugId("LiveURITextBox");
        liveURITextBox.setVisibleLength(40);
        liveURITextBox.setTitle(stringMessages.leaveEmptyForDefault());

        grid.setWidget(2, 0, liveURILabel);
        grid.setWidget(2, 1, liveURITextBox);

        Label storedURILabel = new Label(stringMessages.storedUri() + ":");
        storedURILabel.setTitle(stringMessages.leaveEmptyForDefault());

        storedURITextBox = new TextBox();
        storedURITextBox.ensureDebugId("StoredURITextBox");
        storedURITextBox.setVisibleLength(40);
        storedURITextBox.setTitle(stringMessages.leaveEmptyForDefault());

        grid.setWidget(3, 0, storedURILabel);
        grid.setWidget(3, 1, storedURITextBox);

        // JSON URL
        Label jsonURLLabel = new Label(stringMessages.jsonUrl() + ":");

        jsonURLTextBox = new TextBox();
        jsonURLTextBox.ensureDebugId("JsonURLTextBox");
        jsonURLTextBox.setVisibleLength(100);

        grid.setWidget(4, 0, jsonURLLabel);
        grid.setWidget(4, 1, jsonURLTextBox);
        
        // Course design Update URL
        Label tracTracUpdateURLLabel = new Label(stringMessages.tracTracUpdateUrl() + ":");
        
        tracTracUpdateURITextBox = new TextBox();
        tracTracUpdateURITextBox.ensureDebugId("TracTracUpdateURITextBox");
        tracTracUpdateURITextBox.setVisibleLength(100);
        
        grid.setWidget(5, 0, tracTracUpdateURLLabel);
        grid.setWidget(5, 1, tracTracUpdateURITextBox);
        
        // TracTrac Username
        tractracUsernameTextBox = new TextBox();
        tractracUsernameTextBox.ensureDebugId("TracTracUsernameTextBox");
        tractracUsernameTextBox.setVisibleLength(40);
        
        grid.setWidget(6, 0, new Label(stringMessages.tractracUsername() + ":"));
        grid.setWidget(6, 1, tractracUsernameTextBox);
        
        // TracTrac Password
        tractracPasswordTextBox = new TextBox();
        tractracPasswordTextBox.ensureDebugId("TracTracPasswordTextBox");
        tractracPasswordTextBox.setVisibleLength(40);
        
        grid.setWidget(7, 0, new Label(stringMessages.tractracPassword() + ":"));
        grid.setWidget(7, 1, tractracPasswordTextBox);
        
        grid.setWidget(8, 0, new Label(stringMessages.racesWithHiddenState() + ":"));
        final CheckBox showHiddenRacesCheckbox = new CheckBox(stringMessages.show());
        showHiddenRacesCheckbox.ensureDebugId("ShowHiddenRacesCheckBox");
        grid.setWidget(8, 1, showHiddenRacesCheckbox);

        // List Races
        Button listRacesButton = new Button(stringMessages.listRaces());
        listRacesButton.ensureDebugId("ListRacesButton");
        listRacesButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                loadingMessageLabel.setText(stringMessages.loading());
                fillRaces(sailingService, showHiddenRacesCheckbox.getValue());
            }
        });
        grid.setWidget(9, 1, listRacesButton);
        
        loadingMessageLabel = new Label();
        grid.setWidget(9, 2, loadingMessageLabel);

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
        layoutTable.setWidget(0, 0, regattaForTrackingLabel);
        layoutTable.setWidget(0, 1, getAvailableRegattasListBox());

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

        final TextBox offsetToStartTimeOfSimulatedRaceTextBox = new TextBox();
        offsetToStartTimeOfSimulatedRaceTextBox.setWidth("40px");
        offsetToStartTimeOfSimulatedRaceTextBox.setEnabled(false);
        offsetToStartTimeOfSimulatedRaceTextBox.setValue(ZERO_AS_STRING);
        
        final CheckBox simulateWithStartTimeNowCheckBox = new CheckBox(stringMessages.simulateAsLiveRace());
        simulateWithStartTimeNowCheckBox.ensureDebugId("SimulateWithStartTimeNowCheckBox");
        simulateWithStartTimeNowCheckBox.setWordWrap(false);
        simulateWithStartTimeNowCheckBox.setValue(Boolean.FALSE);
        simulateWithStartTimeNowCheckBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                offsetToStartTimeOfSimulatedRaceTextBox.setEnabled(simulateWithStartTimeNowCheckBox.getValue());
                offsetToStartTimeOfSimulatedRaceTextBox.setFocus(simulateWithStartTimeNowCheckBox.getValue());
            }
        });

        final FlowPanel simulateAsLiveRacePanel = new FlowPanel();
        simulateAsLiveRacePanel.add(simulateWithStartTimeNowCheckBox);
        
        final Label offsetToStartLabel = new Label(stringMessages.simulateWithOffset());
        
        final HorizontalPanel simulateWithOffsetPanel = new HorizontalPanel();
        simulateWithOffsetPanel.add(offsetToStartLabel);
        simulateWithOffsetPanel.add(offsetToStartTimeOfSimulatedRaceTextBox);
        
        final CheckBox ignoreTracTracMarkPassingsCheckbox = new CheckBox(stringMessages.useInternalAlgorithm());
        ignoreTracTracMarkPassingsCheckbox.setWordWrap(false);
        ignoreTracTracMarkPassingsCheckbox.setValue(Boolean.FALSE);
        
        layoutTable.setWidget(1, 0, trackSettingsLabel);
        layoutTable.setWidget(1, 1, trackWindCheckBox);
        layoutTable.setWidget(2, 1, correctWindCheckBox);
        layoutTable.setWidget(3, 1, simulateAsLiveRacePanel);
        layoutTable.setWidget(4, 1, simulateWithOffsetPanel);
        layoutTable.setWidget(5, 1, ignoreTracTracMarkPassingsCheckbox);
        
        // Filter
        Label racesFilterLabel = new Label(stringMessages.filterRacesByName() + ":");
        AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);
        racesTable = new CellTable<TracTracRaceRecordDTO>(10000, tableResources);
        racesTable.ensureDebugId("TrackableRacesCellTable");
        this.racesFilterablePanel = new LabeledAbstractFilterablePanel<TracTracRaceRecordDTO>(racesFilterLabel, availableTracTracRaces, racesTable, raceList) {
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
        };
        racesFilterablePanel.getTextBox().ensureDebugId("TrackableRacesFilterTextBox");
        layoutTable.setWidget(5, 0, racesFilterLabel);
        layoutTable.setWidget(5, 1, racesFilterablePanel);

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
        
        SelectionCheckboxColumn<TracTracRaceRecordDTO> selectionCheckboxColumn = new SelectionCheckboxColumn<TracTracRaceRecordDTO>(tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(), tableResources.cellTableStyle().cellTableCheckboxColumnCell()) {
                    @Override
                    public Boolean getValue(TracTracRaceRecordDTO row) {
                        return racesTable.getSelectionModel().isSelected(row);
                    }

                    @Override
                    protected ListDataProvider<TracTracRaceRecordDTO> getListDataProvider() {
                        return raceList;
                    }
        };
        racesTable.addColumn(selectionCheckboxColumn, selectionCheckboxColumn.getHeader());
        racesTable.addColumn(regattaNameColumn, stringMessages.event());
        racesTable.addColumn(raceNameColumn, stringMessages.race());
        racesTable.addColumn(boatClassColumn, stringMessages.boatClass());
        racesTable.addColumn(raceStartTrackingColumn, stringMessages.startTime());
        racesTable.addColumn(raceStatusColumn, stringMessages.raceStatusColumn());
        racesTable.addColumn(raceVisibilityColumn, stringMessages.raceVisibilityColumn());
        racesTable.addColumnSortHandler(getRaceTableColumnSortHandler(selectionCheckboxColumn, this.raceList.getList(),
                raceNameColumn, boatClassColumn, raceStartTrackingColumn, raceStatusColumn));
        racesTable.setSelectionModel(selectionCheckboxColumn.getSelectionModel(), selectionCheckboxColumn.getSelectionManager());
        racesTable.setWidth("100%");

        raceList.addDataDisplay(racesTable);

        layoutTable.setWidget(6, 0, racesTable);
        cellFormatter.setColSpan(6, 0, 2);
        
        Button startTrackingButton = new Button(stringMessages.startTracking());
        startTrackingButton.ensureDebugId("StartTrackingButton");
        startTrackingButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Duration offsetToStartTimeOfSimulatedRace = null;
                if(simulateWithStartTimeNowCheckBox.getValue().booleanValue()) {
                    offsetToStartTimeOfSimulatedRace = getMillisecondsDurationFromMinutesAsString(offsetToStartTimeOfSimulatedRaceTextBox.getValue());
                }
                trackSelectedRaces(trackWindCheckBox.getValue(), correctWindCheckBox.getValue(),
                        offsetToStartTimeOfSimulatedRace, ignoreTracTracMarkPassingsCheckbox.getValue());
            }
        });

        layoutTable.setWidget(7, 1, startTrackingButton);

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
            Column<TracTracRaceRecordDTO, ?> boatClassColumn, Column<TracTracRaceRecordDTO, ?> trackingStartColumn, Column<TracTracRaceRecordDTO, ?> raceStatusColumn) {
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
                result = new MillisecondsDurationImpl(minutesAsDouble.longValue() * 60000/* One Minute in Milliseconds */);
            }
        }
        return result;
    }

    private void fillConfigurations() {
        this.sailingService.getPreviousTracTracConfigurations(new MarkedAsyncCallback<List<TracTracConfigurationDTO>>(
                new AsyncCallback<List<TracTracConfigurationDTO>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        reportError("Remote Procedure Call getPreviousConfigurations() - Failure: " + caught.getMessage());
                    }
        
                    @Override
                    public void onSuccess(List<TracTracConfigurationDTO> result) {
                        TracTracEventManagementPanel.this.previousConfigurations.clear();
                        TracTracEventManagementPanel.this.connectionsHistoryListBox.clear();
                        Collections.sort(result, new Comparator<TracTracConfigurationDTO>() {
                            @Override
                            public int compare(TracTracConfigurationDTO c1, TracTracConfigurationDTO c2) {
                                return c1.name.compareTo(c2.name);
                            }
                        });
                        for (TracTracConfigurationDTO config : result) {
                            TracTracEventManagementPanel.this.previousConfigurations.put(config.name, config);
                            TracTracEventManagementPanel.this.connectionsHistoryListBox.addItem(config.name);
                        }
                        
                        if (!result.isEmpty()) {
                            updatePanelFromSelectedStoredConfiguration();
                        }
                    }
                }));
    }

    private void fillRaces(final SailingServiceAsync sailingService, boolean listHiddenRaces) {
        final String jsonURL = jsonURLTextBox.getValue();
        final String liveDataURI = liveURITextBox.getValue();
        final String storedDataURI = storedURITextBox.getValue();
        final String courseDesignUpdateURI = tracTracUpdateURITextBox.getValue();
        final String tractracUsername = tractracUsernameTextBox.getValue();
        final String tractracPassword = tractracPasswordTextBox.getValue();

        sailingService.listTracTracRacesInEvent(jsonURL, listHiddenRaces, new MarkedAsyncCallback<com.sap.sse.common.Util.Pair<String, List<TracTracRaceRecordDTO>>>(
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
                        final String eventName = result.getA();
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
                        sailingService.storeTracTracConfiguration(eventName, jsonURL, liveDataURI, storedDataURI,
                                courseDesignUpdateURI, tractracUsername, tractracPassword, new MarkedAsyncCallback<Void>(
                                        new AsyncCallback<Void>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                reportError("Exception trying to store configuration in DB: "  + caught.getMessage());
                                            }
                
                                            @Override
                                            public void onSuccess(Void voidResult) {
                                                // refresh list of previous configurations
                                                TracTracConfigurationDTO config = new TracTracConfigurationDTO(eventName, jsonURL,
                                                        liveDataURI, storedDataURI, courseDesignUpdateURI, tractracUsername, tractracPassword);
                                                
                                                if (TracTracEventManagementPanel.this.previousConfigurations.put(config.name, config) == null) {
                                                    TracTracEventManagementPanel.this.connectionsHistoryListBox.addItem(config.name);
                                                }
                                            }
                                        }));
                    }
                }));
    }

    private void trackSelectedRaces(boolean trackWind, boolean correctWind, final Duration offsetToStartTimeOfSimulatedRace, boolean ignoreTracTracMarkPassings) {
        String liveURI = liveURITextBox.getValue();
        String storedURI = storedURITextBox.getValue();
        String courseDesignUpdateURI = tracTracUpdateURITextBox.getValue();
        String tractracUsername = tractracUsernameTextBox.getValue();
        String tractracPassword = tractracPasswordTextBox.getValue();
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
        final Set<TracTracRaceRecordDTO> selectedRaces = new LinkedHashSet<>();
        for (TracTracRaceRecordDTO race : allRaces) {
            if (selectionModel.isSelected(race)) {
                selectedRaces.add(race);
            }
        }
        if (checkBoatClassOK(selectedRegatta, selectedRaces)) {
            sailingService.trackWithTracTrac(regattaIdentifier, selectedRaces, liveURI, storedURI,
                    courseDesignUpdateURI, trackWind, correctWind, offsetToStartTimeOfSimulatedRace, ignoreTracTracMarkPassings, tractracUsername,
                    tractracPassword, new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            reportError(stringMessages.errorTryingToRegisterRacesForTracking(selectedRaces.toString(), caught.getMessage()));
                        }

                        @Override
                        public void onSuccess(Void result) {
                            TracTracEventManagementPanel.this.regattaRefresher.fillRegattas();
                        }
                    }));
        }
    }
    
    private void updatePanelFromSelectedStoredConfiguration() {
        int index = connectionsHistoryListBox.getSelectedIndex();

        if (index == -1)
            return;
        
        String configurationKey = connectionsHistoryListBox.getItemText(index);
        TracTracConfigurationDTO config = previousConfigurations.get(configurationKey);

        jsonURLTextBox.setValue(config.jsonURL);
        liveURITextBox.setValue(config.liveDataURI);
        storedURITextBox.setValue(config.storedDataURI);
        tracTracUpdateURITextBox.setValue(config.courseDesignUpdateURI);
        tractracUsernameTextBox.setValue(config.tractracUsername);
        tractracPasswordTextBox.setValue(config.tractracPassword);
    }

}
