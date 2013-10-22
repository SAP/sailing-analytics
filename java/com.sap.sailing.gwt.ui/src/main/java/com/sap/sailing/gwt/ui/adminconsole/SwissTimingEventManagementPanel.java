package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.Handler;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.RaceSelectionModel;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingRaceRecordDTO;

/**
 * Allows the user to start and stop tracking of races using the SwissTiming connector. In particular,
 * previously configured connections can be retrieved from a drop-down list which then pre-populates all connection
 * parameters. The user can also choose to enter connection information manually.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class SwissTimingEventManagementPanel extends AbstractEventManagementPanel {
    private final IntegerBox portIntegerbox;
    private final TextBox hostnameTextbox;
    private final TextBox filterEventsTextbox;
    private final ListDataProvider<SwissTimingRaceRecordDTO> raceList;
    private final CellTable<SwissTimingRaceRecordDTO> raceTable;
    private final Map<String, SwissTimingConfigurationDTO> previousConfigurations;
    private final ListBox previousConfigurationsComboBox;
    private final CheckBox canSendRequestsCheckbox;
    private final List<SwissTimingRaceRecordDTO> availableSwissTimingRaces = new ArrayList<SwissTimingRaceRecordDTO>();

    public SwissTimingEventManagementPanel(final SailingServiceAsync sailingService, ErrorReporter errorReporter,
            RegattaRefresher regattaRefresher, StringMessages stringConstants) {
        super(sailingService, regattaRefresher, errorReporter, new RaceSelectionModel(), stringConstants);
        this.errorReporter = errorReporter;

        VerticalPanel mainPanel = new VerticalPanel();
        this.setWidget(mainPanel);
        mainPanel.setWidth("100%");
        
        CaptionPanel captionPanelConnections = new CaptionPanel(stringConstants.connections());
        mainPanel.add(captionPanelConnections);

        VerticalPanel verticalPanel = new VerticalPanel();
        
        captionPanelConnections.setContentWidget(verticalPanel);
        captionPanelConnections.setStyleName("bold");
        Grid connectionsGrid = new Grid(7, 2);
        verticalPanel.add(connectionsGrid);
        verticalPanel.setCellWidth(connectionsGrid, "100%");
        
        Label lblPredefined = new Label(stringConstants.connections() +":");
        connectionsGrid.setWidget(0, 0, lblPredefined);
        
        previousConfigurations = new HashMap<String, SwissTimingConfigurationDTO>();
        previousConfigurationsComboBox = new ListBox();
        connectionsGrid.setWidget(0, 1, previousConfigurationsComboBox);
        previousConfigurationsComboBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                updatePanelFromSelectedStoredConfiguration();
            }
        });
        previousConfigurationsComboBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                updatePanelFromSelectedStoredConfiguration();
            }
        });
        fillConfigurations();

        Label lblTrackNewEvent = new Label(stringConstants.defineNewConnection());
        connectionsGrid.setWidget(2, 0, lblTrackNewEvent);
        
        Label lblHostname = new Label(stringConstants.hostname() + ":");
        connectionsGrid.setWidget(3, 0, lblHostname);
        
        hostnameTextbox = new TextBox();
        hostnameTextbox.setText("");
        connectionsGrid.setWidget(3, 1, hostnameTextbox);
        
        Label lblPort = new Label(stringConstants.port() + ":");
        connectionsGrid.setWidget(4, 0, lblPort);
        
        portIntegerbox = new IntegerBox();
        connectionsGrid.setWidget(4, 1, portIntegerbox);

        Label lblCanSendRequests = new Label(stringConstants.canSendRequests());
        connectionsGrid.setWidget(5, 0, lblCanSendRequests);

        canSendRequestsCheckbox = new CheckBox();
        canSendRequestsCheckbox.setValue(false);
        connectionsGrid.setWidget(5, 1, canSendRequestsCheckbox);

        Button btnListRaces = new Button(stringConstants.connectAndReadRaces());
        connectionsGrid.setWidget(6, 1, btnListRaces);
        btnListRaces.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                fillRaces(sailingService);
            }
        });

        TextColumn<SwissTimingRaceRecordDTO> raceNameColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.ID;
            }
        };

        TextColumn<SwissTimingRaceRecordDTO> raceDescriptionColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.description;
            }
        };
        
        TextColumn<SwissTimingRaceRecordDTO> boatClassColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                String result = object.boatClass != null ? object.boatClass : "";
                if(object.discipline != null) {
                    result += " (" + object.discipline + ")";
                }
                return result;
            }
        };

        TextColumn<SwissTimingRaceRecordDTO> raceStateColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                String result = object.hasCourse ? "C✓ " : "";
                result += object.hasStartlist ? "S✓" : "";
                return result;
            }
        };
        
        TextColumn<SwissTimingRaceRecordDTO> raceStartTimeColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.raceStartTime==null?"":dateFormatter.render(object.raceStartTime) + " " + timeFormatter.render(object.raceStartTime);
            }
        };

        HorizontalPanel racesSplitPanel = new HorizontalPanel();
        mainPanel.add(racesSplitPanel);
        
        CaptionPanel trackableRacesCaptionPanel = new CaptionPanel(stringConstants.trackableRaces());
        racesSplitPanel.add(trackableRacesCaptionPanel);
        trackableRacesCaptionPanel.setWidth("50%");

        CaptionPanel trackedRacesCaptionPanel = new CaptionPanel(stringConstants.trackedRaces());
        racesSplitPanel.add(trackedRacesCaptionPanel);
        trackedRacesCaptionPanel.setWidth("50%");

        VerticalPanel trackableRacesPanel = new VerticalPanel();
        trackableRacesCaptionPanel.setContentWidget(trackableRacesPanel);
        trackableRacesCaptionPanel.setStyleName("bold");

        VerticalPanel trackedRacesPanel = new VerticalPanel();
        trackedRacesPanel.setWidth("100%");
        trackedRacesCaptionPanel.setContentWidget(trackedRacesPanel);
        trackedRacesCaptionPanel.setStyleName("bold");

        // Regatta selection
        HorizontalPanel regattaPanel = new HorizontalPanel();
        regattaPanel.setSpacing(5);
        Label regattaForTrackingLabel = new Label(stringMessages.regattaUsedForTheTrackedRace());
        regattaForTrackingLabel.setWordWrap(false);
        regattaPanel.add(regattaForTrackingLabel);
        regattaPanel.add(getAvailableRegattasListBox());
        trackableRacesPanel.add(regattaPanel);

        Label lblTrackSettings = new Label(stringConstants.trackSettings());
        trackableRacesPanel.add(lblTrackSettings);

        final CheckBox trackWindCheckbox = new CheckBox(stringConstants.trackWind());
        trackWindCheckbox.setWordWrap(false);
        trackWindCheckbox.setValue(true);
        trackableRacesPanel.add(trackWindCheckbox);

        final CheckBox declinationCheckbox = new CheckBox(stringConstants.declinationCheckbox());
        declinationCheckbox.setWordWrap(false);
        declinationCheckbox.setValue(true);
        trackableRacesPanel.add(declinationCheckbox);

        // text box for filtering the cell table
        HorizontalPanel filterPanel = new HorizontalPanel();
        filterPanel.setSpacing(5);
        trackableRacesPanel.add(filterPanel);

        Label lblFilterEvents = new Label(stringConstants.filterRacesByName() + ":");
        filterPanel.add(lblFilterEvents);
        filterPanel.setCellVerticalAlignment(lblFilterEvents, HasVerticalAlignment.ALIGN_MIDDLE);
        
        filterEventsTextbox = new TextBox();
        filterEventsTextbox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                fillRaceListFromAvailableRacesApplyingFilter(SwissTimingEventManagementPanel.this.filterEventsTextbox.getText());
            }
        });
        filterPanel.add(filterEventsTextbox);

        raceNameColumn.setSortable(true);
        raceStartTimeColumn.setSortable(true);
        
        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        raceTable = new CellTable<SwissTimingRaceRecordDTO>(/* pageSize */ 10000, tableRes);
        raceTable.addColumn(raceNameColumn, stringConstants.name());
        raceTable.addColumn(raceDescriptionColumn, stringConstants.description());
        raceTable.addColumn(boatClassColumn, stringConstants.boatClass());
        raceTable.addColumn(raceStateColumn, stringConstants.status());
        raceTable.addColumn(raceStartTimeColumn, stringConstants.startTime());
        raceTable.setWidth("300px");
        raceTable.setSelectionModel(new MultiSelectionModel<SwissTimingRaceRecordDTO>() {});

        trackableRacesPanel.add(raceTable);

        raceList = new ListDataProvider<SwissTimingRaceRecordDTO>();
        raceList.addDataDisplay(raceTable);
        Handler columnSortHandler = getRaceTableColumnSortHandler(raceList.getList(), raceNameColumn, raceStartTimeColumn);
        raceTable.addColumnSortHandler(columnSortHandler);
        
        trackedRacesPanel.add(trackedRacesListComposite);

        HorizontalPanel racesButtonPanel = new HorizontalPanel();
        trackableRacesPanel.add(racesButtonPanel);

        Button btnTrack = new Button(stringConstants.startTracking());
        racesButtonPanel.add(btnTrack);
        racesButtonPanel.setSpacing(10);
        btnTrack.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                trackSelectedRaces(trackWindCheckbox.getValue(), declinationCheckbox.getValue());
            }
        });
    }

    private ListHandler<SwissTimingRaceRecordDTO> getRaceTableColumnSortHandler(List<SwissTimingRaceRecordDTO> raceRecords,
            Column<SwissTimingRaceRecordDTO, ?> nameColumn, Column<SwissTimingRaceRecordDTO, ?> trackingStartColumn) {
        ListHandler<SwissTimingRaceRecordDTO> result = new ListHandler<SwissTimingRaceRecordDTO>(raceRecords);
        result.setComparator(nameColumn, new Comparator<SwissTimingRaceRecordDTO>() {
            @Override
            public int compare(SwissTimingRaceRecordDTO o1, SwissTimingRaceRecordDTO o2) {
                return o1.ID.compareTo(o2.ID);
            }
        });
        result.setComparator(trackingStartColumn, new Comparator<SwissTimingRaceRecordDTO>() {
            @Override
            public int compare(SwissTimingRaceRecordDTO o1, SwissTimingRaceRecordDTO o2) {
                return o1.raceStartTime == null ? -1 : o2.raceStartTime == null ? 1 : o1.raceStartTime
                        .compareTo(o2.raceStartTime);
            }
        });
        return result;
    }

    private void fillConfigurations() {
        sailingService.getPreviousSwissTimingConfigurations(new AsyncCallback<List<SwissTimingConfigurationDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Remote Procedure Call getPreviousConfigurations() - Failure: "
                        + caught.getMessage());
            }

            @Override
            public void onSuccess(List<SwissTimingConfigurationDTO> result) {
                while (previousConfigurationsComboBox.getItemCount() > 0) {
                    previousConfigurationsComboBox.removeItem(0);
                }
                for (SwissTimingConfigurationDTO stConfig : result) {
                    previousConfigurations.put(stConfig.name, stConfig);
                    previousConfigurationsComboBox.addItem(stConfig.name);
                }
                if (!result.isEmpty()) {
                    updatePanelFromSelectedStoredConfiguration();
                }
            }
        });
    }

    private void fillRaces(final SailingServiceAsync sailingService) {
        final String hostname = hostnameTextbox.getValue();
        final int port = portIntegerbox.getValue();
        final boolean canSendRequests = canSendRequestsCheckbox.getValue();
        sailingService.listSwissTimingRaces(hostname, port, canSendRequests,
                new AsyncCallback<List<SwissTimingRaceRecordDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                SwissTimingEventManagementPanel.this.errorReporter.reportError("Error trying to list races: "
                        + caught.getMessage());
            }

            @Override
            public void onSuccess(final List<SwissTimingRaceRecordDTO> result) {
                availableSwissTimingRaces.clear();
                if (result != null)
                    availableSwissTimingRaces.addAll(result);

                raceList.getList().clear();
                raceList.getList().addAll(availableSwissTimingRaces);

                filterEventsTextbox.setText(null);

                // store a successful configuration in the database for later retrieval
                final String configName = hostname+":"+port;
                sailingService.storeSwissTimingConfiguration(configName, hostname, port, canSendRequests,
                        new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError("Exception trying to store configuration in DB: "
                                        + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Void voidResult) {
                                // refresh list of previous configurations
                                SwissTimingConfigurationDTO stConfig = new SwissTimingConfigurationDTO(configName,
                                        hostname, port, canSendRequests);
                                if (previousConfigurations.put(stConfig.name, stConfig) == null) {
                                    previousConfigurationsComboBox.addItem(stConfig.name);
                                }
                            }
                        });
            }
        });
    }

    private void trackSelectedRaces(boolean trackWind, boolean correctWindByDeclination) {
        String hostname = hostnameTextbox.getValue();
        int port = portIntegerbox.getValue();
        final List<SwissTimingRaceRecordDTO> selectedRaces = new ArrayList<SwissTimingRaceRecordDTO>();
        for (final SwissTimingRaceRecordDTO race : this.raceList.getList()) {
            if (raceTable.getSelectionModel().isSelected(race)) {
                selectedRaces.add(race);
            }
        }
        RegattaDTO selectedRegatta = getSelectedRegatta();
        RegattaIdentifier regattaIdentifier = null;
        if (selectedRegatta != null) {
            regattaIdentifier = new RegattaName(selectedRegatta.getName());
        }
        sailingService.trackWithSwissTiming(
                /* regattaToAddTo */ regattaIdentifier,
                selectedRaces, hostname, port, /* canSendRequests */false, trackWind, correctWindByDeclination,
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to register races " + selectedRaces + " for tracking: "
                                + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        regattaRefresher.fillRegattas();
                    }
                });
    }

    private void updatePanelFromSelectedStoredConfiguration() {
        if (previousConfigurationsComboBox.getSelectedIndex() >= 0) {
            SwissTimingConfigurationDTO stConfig = previousConfigurations.get(previousConfigurationsComboBox
                    .getItemText(previousConfigurationsComboBox.getSelectedIndex()));
            if (stConfig != null) {
                hostnameTextbox.setValue(stConfig.hostname);
                portIntegerbox.setValue(stConfig.port);
                canSendRequestsCheckbox.setValue(stConfig.canSendRequests);
            }
        }
    }

    private void fillRaceListFromAvailableRacesApplyingFilter(String text) {
        List<String> wordsToFilter = Arrays.asList(text.split(" "));
        raceList.getList().clear();
        if (text != null && !text.isEmpty()) {
            for (SwissTimingRaceRecordDTO triple : availableSwissTimingRaces) {
                boolean found = textContainsStringsToCheck(wordsToFilter, triple.ID);
                if (found) {
                    raceList.getList().add(triple);
                }
            }
        } else {
            raceList.getList().addAll(availableSwissTimingRaces);
        }
        // now sort again according to selected criterion
        ColumnSortEvent.fire(raceTable, raceTable.getColumnSortList());
    }
}
