package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.Handler;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.adminconsole.swisstiming.SwissTimingConnectionDialog;
import com.sap.sailing.gwt.ui.adminconsole.swisstiming.SwissTimingConnectionTableWrapper;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingEventRecordDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingRaceRecordDTO;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;

/**
 * Allows the user to start and stop tracking of races using the SwissTiming connector. In particular,
 * previously configured connections can be retrieved from a drop-down list which then pre-populates all connection
 * parameters. The user can also choose to enter connection information manually.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class SwissTimingEventManagementPanel extends AbstractEventManagementPanel implements FilterablePanelProvider<SwissTimingConfigurationWithSecurityDTO> {
    private static final AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
    
    private final LabeledAbstractFilterablePanel<SwissTimingRaceRecordDTO> filterablePanelEvents;
    private final ListDataProvider<SwissTimingRaceRecordDTO> raceList;
    private final FlushableCellTable<SwissTimingRaceRecordDTO> raceTable;
    private final List<SwissTimingRaceRecordDTO> availableSwissTimingRaces = new ArrayList<SwissTimingRaceRecordDTO>();
    private final SwissTimingConnectionTableWrapper connectionsTable;

    public SwissTimingEventManagementPanel(final Presenter presenter, StringMessages stringConstants,
            final CellTableWithCheckboxResources tableResources) {
        super(presenter, true, stringConstants);
        this.errorReporter = presenter.getErrorReporter();
        VerticalPanel mainPanel = new VerticalPanel();
        this.setWidget(mainPanel);
        mainPanel.setWidth("100%");
        CaptionPanel captionPanelConnections = new CaptionPanel(stringConstants.connections());
        mainPanel.add(captionPanelConnections);
        VerticalPanel verticalPanel = new VerticalPanel();
        captionPanelConnections.setContentWidget(verticalPanel);
        captionPanelConnections.setStyleName("bold");
        connectionsTable = new SwissTimingConnectionTableWrapper(presenter.getUserService(), sailingServiceWrite, stringConstants,
                errorReporter, true, tableResources, () -> {});
        connectionsTable.refreshSwissTimingConnectionList();
        // Add button panel
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(presenter.getUserService(),
                SecuredDomainType.TRACKED_RACE);
        verticalPanel.add(buttonPanel);
        verticalPanel.add(connectionsTable);
        buttonPanel.addUnsecuredAction(stringMessages.refresh(),
                () -> connectionsTable.refreshSwissTimingConnectionList());
        // Add SwissTiming Connection
        buttonPanel.addCreateAction(stringMessages.addSwissTimingConnection(),
                () -> new SwissTimingConnectionDialog(
                        new DialogCallback<SwissTimingConfigurationWithSecurityDTO>() {
                            @Override
                            public void ok(SwissTimingConfigurationWithSecurityDTO editedConnection) {
                                sailingServiceWrite.createSwissTimingConfiguration(editedConnection.getName(),
                                        editedConnection.getJsonUrl(), editedConnection.getHostname(),
                                        editedConnection.getPort(), editedConnection.getUpdateURL(),
                                        editedConnection.getApiToken(),
                                        new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                errorReporter
                                                        .reportError("Exception trying to create configuration in DB: "
                                                                + caught.getMessage());
                                            }

                                            @Override
                                            public void onSuccess(Void voidResult) {
                                                connectionsTable.refreshSwissTimingConnectionList();
                                                connectionsTable.getFilterField().search(editedConnection.getJsonUrl());
                                            }
                                        }));
                            }

                            @Override
                            public void cancel() {
                            }
                        }, presenter.getUserService(), errorReporter).show());
        // Remove SwissTiming Connection
        buttonPanel.addRemoveAction(stringMessages.remove(), connectionsTable.getSelectionModel(), false, () -> {
            sailingServiceWrite.deleteSwissTimingConfigurations(connectionsTable.getSelectionModel().getSelectedSet(),
                    new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(
                                    "Exception trying to delete configuration in DB: " + caught.getMessage());
                        }

                        @Override
                        public void onSuccess(Void result) {
                            connectionsTable.refreshSwissTimingConnectionList();
                        }
                    });
        });
        // List Races in SwissTiming Connection
        final Button listRacesButton = buttonPanel.addUnsecuredAction(stringMessages.listRaces(), () -> {
            fillRaces(sailingServiceWrite);
        });
        listRacesButton.setEnabled(false);
        connectionsTable.getSelectionModel().addSelectionChangeHandler(e -> {
            listRacesButton.setEnabled(connectionsTable.getSelectionModel().getSelectedSet().size() == 1);
        });
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
        final CheckBox simulateWithStartTimeNow = new CheckBox(stringMessages.simulateAsLiveRace());
        simulateWithStartTimeNow.setWordWrap(false);
        simulateWithStartTimeNow.setValue(false);
        trackableRacesPanel.add(simulateWithStartTimeNow);
        final CheckBox useInternalMarkPassingAlgorithmCheckbox = new CheckBox(stringMessages.useInternalAlgorithm());
        useInternalMarkPassingAlgorithmCheckbox.setWordWrap(false);
        useInternalMarkPassingAlgorithmCheckbox.setValue(Boolean.FALSE);
        trackableRacesPanel.add(useInternalMarkPassingAlgorithmCheckbox);
        // text box for filtering the cell table
        HorizontalPanel filterPanel = new HorizontalPanel();
        filterPanel.setSpacing(5);
        trackableRacesPanel.add(filterPanel);
        Label lblFilterEvents = new Label(stringConstants.filterRaces() + ":");
        filterPanel.add(lblFilterEvents);
        filterPanel.setCellVerticalAlignment(lblFilterEvents, HasVerticalAlignment.ALIGN_MIDDLE);
        raceTable = new FlushableCellTable<SwissTimingRaceRecordDTO>(/* pageSize */10000, tableRes);
        raceTable.setWidth("300px");
        raceList = new ListDataProvider<SwissTimingRaceRecordDTO>();
        filterablePanelEvents = new LabeledAbstractFilterablePanel<SwissTimingRaceRecordDTO>(lblFilterEvents,
                availableSwissTimingRaces, raceList, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(SwissTimingRaceRecordDTO t) {
                List<String> strings = new ArrayList<>();
                strings.add(t.regattaName);
                strings.add(t.seriesName);
                strings.add(t.getName());
                strings.add(t.raceStatus);
                strings.add(t.boatClass);
                strings.add(t.gender);
                if (t.raceStartTime != null) {
                    strings.add(dateFormatter.render(t.raceStartTime));
                }
                return strings;
            }

            @Override
            public AbstractCellTable<SwissTimingRaceRecordDTO> getCellTable() {
                return raceTable;
            }
        };
        final EntityIdentityComparator<SwissTimingRaceRecordDTO> entityIdentityComparator = new EntityIdentityComparator<SwissTimingRaceRecordDTO>() {
            @Override
            public boolean representSameEntity(SwissTimingRaceRecordDTO dto1, SwissTimingRaceRecordDTO dto2) {
                return dto1.raceId.equals(dto2.raceId);
            }
            @Override
            public int hashCode(SwissTimingRaceRecordDTO t) {
                return t.raceId.hashCode();
            }
        };
        TextColumn<SwissTimingRaceRecordDTO> raceNameColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.getName();
            }
        };
        TextColumn<SwissTimingRaceRecordDTO> regattaNameColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.regattaName;
            }
        };
        SelectionCheckboxColumn<SwissTimingRaceRecordDTO> selectionColumn = new SelectionCheckboxColumn<SwissTimingRaceRecordDTO>(
                tableRes.cellTableStyle().cellTableCheckboxSelected(),
                tableRes.cellTableStyle().cellTableCheckboxDeselected(),
                tableRes.cellTableStyle().cellTableCheckboxColumnCell(), entityIdentityComparator, raceList, raceTable);

        TextColumn<SwissTimingRaceRecordDTO> seriesNameColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.seriesName;
            }
        };
        TextColumn<SwissTimingRaceRecordDTO> raceIdColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.raceId;
            }
        };
        TextColumn<SwissTimingRaceRecordDTO> boatClassColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.boatClass != null ? object.boatClass : "";
            }
        };
        TextColumn<SwissTimingRaceRecordDTO> genderColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.gender != null ? object.gender : "";
            }
        };
        TextColumn<SwissTimingRaceRecordDTO> raceStatusColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.raceStatus != null ? object.raceStatus : "";
            }
        };
        TextColumn<SwissTimingRaceRecordDTO> raceStartTimeColumn = new TextColumn<SwissTimingRaceRecordDTO>() {
            @Override
            public String getValue(SwissTimingRaceRecordDTO object) {
                return object.raceStartTime==null?"":dateFormatter.render(object.raceStartTime) + " " + timeFormatter.render(object.raceStartTime);
            }
        };
        raceNameColumn.setSortable(true);
        raceStartTimeColumn.setSortable(true);
        boatClassColumn.setSortable(true);
        raceIdColumn.setSortable(true);
        genderColumn.setSortable(true);
        raceStatusColumn.setSortable(true);
        regattaNameColumn.setSortable(true);
        seriesNameColumn.setSortable(true);
        raceTable.addColumn(selectionColumn, selectionColumn.getHeader());
        raceTable.addColumn(regattaNameColumn, stringConstants.regatta());
        raceTable.addColumn(seriesNameColumn, stringConstants.series());
        raceTable.addColumn(raceNameColumn, stringConstants.name());
        raceTable.addColumn(raceStatusColumn, stringConstants.status());
        raceTable.addColumn(boatClassColumn, stringConstants.boatClass());
        raceTable.addColumn(genderColumn, stringConstants.gender());
        raceTable.addColumn(raceStartTimeColumn, stringConstants.startTime());
        raceTable.setSelectionModel(selectionColumn.getSelectionModel(), selectionColumn.getSelectionManager());
        trackableRacesPanel.add(raceTable);
        raceList.addDataDisplay(raceTable);
        Handler columnSortHandler = getRaceTableColumnSortHandler(raceList.getList(), regattaNameColumn, seriesNameColumn,
        		raceNameColumn, raceStartTimeColumn, raceIdColumn, boatClassColumn, genderColumn, raceStatusColumn);
        raceTable.addColumnSortHandler(columnSortHandler);
        trackedRacesPanel.add(trackedRacesListComposite);
        filterPanel.add(filterablePanelEvents);
        HorizontalPanel racesButtonPanel = new HorizontalPanel();
        trackableRacesPanel.add(racesButtonPanel);
        Button btnTrack = new Button(stringConstants.startTracking());
        racesButtonPanel.add(btnTrack);
        racesButtonPanel.setSpacing(10);
        btnTrack.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                trackSelectedRaces(trackWindCheckbox.getValue(), declinationCheckbox.getValue(),
                        useInternalMarkPassingAlgorithmCheckbox.getValue());
            }
        });
        btnTrack.setEnabled(false);
        connectionsTable.getSelectionModel().addSelectionChangeHandler(
                e -> btnTrack.setEnabled(connectionsTable.getSelectionModel().getSelectedSet().size() == 1));
    }

    private ListHandler<SwissTimingRaceRecordDTO> getRaceTableColumnSortHandler(List<SwissTimingRaceRecordDTO> raceRecords,
    		Column<SwissTimingRaceRecordDTO, ?> regattaNameColumn, Column<SwissTimingRaceRecordDTO, ?> seriesNameColumn, 
    		Column<SwissTimingRaceRecordDTO, ?> nameColumn, Column<SwissTimingRaceRecordDTO, ?> trackingStartColumn,
            Column<SwissTimingRaceRecordDTO, ?> raceIdColumn, Column<SwissTimingRaceRecordDTO, ?> boatClassColumn,
            Column<SwissTimingRaceRecordDTO, ?> genderColumn, Column<SwissTimingRaceRecordDTO, ?> statusColumn) {
        ListHandler<SwissTimingRaceRecordDTO> result = new ListHandler<SwissTimingRaceRecordDTO>(raceRecords);
        result.setComparator(regattaNameColumn, new Comparator<SwissTimingRaceRecordDTO>() {
            @Override
            public int compare(SwissTimingRaceRecordDTO o1, SwissTimingRaceRecordDTO o2) {
                return new NaturalComparator().compare(o1.regattaName,  o2.regattaName);
            }
        });
        result.setComparator(seriesNameColumn, new Comparator<SwissTimingRaceRecordDTO>() {
            @Override
            public int compare(SwissTimingRaceRecordDTO o1, SwissTimingRaceRecordDTO o2) {
                return new NaturalComparator().compare(o1.seriesName,  o2.seriesName);
            }
        });
        result.setComparator(nameColumn, new Comparator<SwissTimingRaceRecordDTO>() {
            @Override
            public int compare(SwissTimingRaceRecordDTO o1, SwissTimingRaceRecordDTO o2) {
                return new NaturalComparator().compare(o1.getName(),  o2.getName());
            }
        });
        result.setComparator(trackingStartColumn, new Comparator<SwissTimingRaceRecordDTO>() {
            @Override
            public int compare(SwissTimingRaceRecordDTO o1, SwissTimingRaceRecordDTO o2) {
                return o1.raceStartTime == null ? -1 : o2.raceStartTime == null ? 1 : o1.raceStartTime
                        .compareTo(o2.raceStartTime);
            }
        });
        result.setComparator(raceIdColumn, new Comparator<SwissTimingRaceRecordDTO>()  {
            @Override
            public int compare(SwissTimingRaceRecordDTO o1, SwissTimingRaceRecordDTO o2) {
                return o1.raceId == null ? -1 : o2.raceId == null ? 1 : 
                    new NaturalComparator().compare(o1.raceId, o2.raceId);
            }
        });
        result.setComparator(boatClassColumn, new Comparator<SwissTimingRaceRecordDTO>() {
            @Override
            public int compare(SwissTimingRaceRecordDTO o1, SwissTimingRaceRecordDTO o2) {
                return o1.boatClass == null ? -1 : o2.boatClass == null ? 1 : new NaturalComparator(false).compare(o1.boatClass, o2.boatClass);
            }
        });
        result.setComparator(genderColumn, new Comparator<SwissTimingRaceRecordDTO>() {
            @Override
            public int compare(SwissTimingRaceRecordDTO o1, SwissTimingRaceRecordDTO o2) {
                return o1.gender == null ? -1 : o2.gender == null ? 1 : new NaturalComparator(false).compare(o1.gender, o2.gender);
            }
        });
        result.setComparator(statusColumn, new Comparator<SwissTimingRaceRecordDTO>() {
            @Override
            public int compare(SwissTimingRaceRecordDTO o1, SwissTimingRaceRecordDTO o2) {
                return new NaturalComparator().compare(o1.raceStatus,  o2.raceStatus);
            }
        });
        return result;
    }


    private void fillRaces(final SailingServiceWriteAsync sailingServiceWrite) {
        final SwissTimingConfigurationWithSecurityDTO selectedObject = connectionsTable.getSelectionModel()
                .getSelectedSet().iterator().next();
        sailingServiceWrite.getRacesOfSwissTimingEvent(selectedObject.getJsonUrl(),
                new AsyncCallback<SwissTimingEventRecordDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        SwissTimingEventManagementPanel.this.errorReporter
                                .reportError("Error trying to list races: " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(final SwissTimingEventRecordDTO result) {
                        availableSwissTimingRaces.clear();
                        if (result != null) {
                            availableSwissTimingRaces.addAll(result.races);
                        }
                        raceList.getList().clear();
                        raceList.getList().addAll(availableSwissTimingRaces);
                        filterablePanelEvents.getTextBox().setText(null);
                        filterablePanelEvents.updateAll(result.races);
                        // store a successful configuration in the database for later retrieval
                        final SwissTimingConfigurationWithSecurityDTO updatedDTO = new SwissTimingConfigurationWithSecurityDTO(
                                selectedObject, result.trackingDataHost, result.trackingDataPort, result.eventName);
                        sailingServiceWrite.updateSwissTimingConfiguration(updatedDTO, new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(
                                        "Exception trying to update configuration in DB: " + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Void voidResult) {
                                connectionsTable.refreshSwissTimingConnectionList();
                            }
                        });
                    }
                });
    }

    private void trackSelectedRaces(boolean trackWind, boolean correctWindByDeclination, boolean useInternalMarkPassingAlgorithm) {
        final SwissTimingConfigurationWithSecurityDTO selectedObject = connectionsTable.getSelectionModel()
                .getSelectedSet().iterator().next();
        final String hostname = selectedObject.getHostname();
        final Integer port = selectedObject.getPort();
        final String updateURL = selectedObject.getUpdateURL();
        final String updateUsername = selectedObject.getApiToken();
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
        // Check if the assigned regatta makes sense
        if (checkBoatClassOK(selectedRegatta, selectedRaces)) {
            sailingServiceWrite.trackWithSwissTiming(/* regattaToAddTo */ regattaIdentifier, selectedRaces, hostname, port==null?0:port,
                    trackWind, correctWindByDeclination, useInternalMarkPassingAlgorithm, updateURL, updateUsername,
                    selectedObject.getName(), selectedObject.getJsonUrl(), new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError("Error trying to register races " + selectedRaces
                                    + " for tracking: " + caught.getMessage());
                        }

                        @Override
                        public void onSuccess(Void result) {
                            presenter.getRegattasRefresher().reloadAndCallFillAll();
                        }
                    });
        }
    }

    @Override
    public AbstractFilterablePanel<SwissTimingConfigurationWithSecurityDTO> getFilterablePanel() {
        return connectionsTable.getFilterField();
    }
}
