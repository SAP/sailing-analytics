package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
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
import com.sap.sailing.gwt.ui.adminconsole.swisstiming.SwissTimingArchivedConnectionDialog;
import com.sap.sailing.gwt.ui.adminconsole.swisstiming.SwissTimingArchivedConnectionTableWrapper;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingArchiveConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingReplayRaceDTO;
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
 * Allows the user to start and stop tracking of events, regattas and races using the SwissTiming connector.
 * Inspired by TracTracEventManagementPanel  
 * 
 * @author Jens Rommel (D047974)
 * 
 */
public class SwissTimingReplayConnectorPanel extends AbstractEventManagementPanel implements FilterablePanelProvider<SwissTimingArchiveConfigurationWithSecurityDTO> {
    private final ErrorReporter errorReporter;
    private final LabeledAbstractFilterablePanel<SwissTimingReplayRaceDTO> filterablePanelEvents;
    private final ListDataProvider<SwissTimingReplayRaceDTO> raceList;
    private final FlushableCellTable<SwissTimingReplayRaceDTO> raceTable;
    private final List<SwissTimingReplayRaceDTO> availableSwissTimingRaces;

    private final SwissTimingArchivedConnectionTableWrapper connectionsTable;

    public SwissTimingReplayConnectorPanel(final Presenter presenter, StringMessages stringMessages, CellTableWithCheckboxResources tableResources) {
        super(presenter, true, stringMessages);
        this.errorReporter = presenter.getErrorReporter();
        availableSwissTimingRaces = new ArrayList<SwissTimingReplayRaceDTO>();
        // setup UI
        final VerticalPanel mainPanel = new VerticalPanel();
        this.setWidget(mainPanel);
        mainPanel.setWidth("100%");
        final CaptionPanel captionPanelConnections = new CaptionPanel(stringMessages.connections());
        mainPanel.add(captionPanelConnections);
        final VerticalPanel verticalPanel = new VerticalPanel();
        captionPanelConnections.setContentWidget(verticalPanel);
        captionPanelConnections.setStyleName("bold");
        // add connections table
        connectionsTable = new SwissTimingArchivedConnectionTableWrapper(presenter.getUserService(), sailingServiceWrite, stringMessages,
                errorReporter, true, tableResources, () -> {
                });
        connectionsTable.refreshConnectionList();
        // create button UI
        final AccessControlledButtonPanel buttonPanel = createButtonPanel(sailingServiceWrite, presenter.getUserService(), errorReporter,
                stringMessages);
        verticalPanel.add(buttonPanel);
        verticalPanel.add(connectionsTable);
        // Table
        TextColumn<SwissTimingReplayRaceDTO> regattaNameColumn = new TextColumn<SwissTimingReplayRaceDTO>() {
            @Override
            public String getValue(SwissTimingReplayRaceDTO object) {
                return object.rsc;
            }
        };
        TextColumn<SwissTimingReplayRaceDTO> raceNameColumn = new TextColumn<SwissTimingReplayRaceDTO>() {
            @Override
            public String getValue(SwissTimingReplayRaceDTO object) {
                return object.getName();
            }
        };
        TextColumn<SwissTimingReplayRaceDTO> raceStartTrackingColumn = new TextColumn<SwissTimingReplayRaceDTO>() {
            @Override
            public String getValue(SwissTimingReplayRaceDTO object) {
                return object.startTime == null ? "" : dateFormatter.render(object.startTime) + " " + timeFormatter.render(object.startTime);
            }
        };
        TextColumn<SwissTimingReplayRaceDTO> boatClassNamesColumn = new TextColumn<SwissTimingReplayRaceDTO>() {
            @Override
            public String getValue(SwissTimingReplayRaceDTO object) {
                return object.boat_class;
            }
        };
        HorizontalPanel racesSplitPanel = new HorizontalPanel();
        mainPanel.add(racesSplitPanel);
        CaptionPanel racesCaptionPanel = new CaptionPanel(stringMessages.trackableRaces());
        racesSplitPanel.add(racesCaptionPanel);
        racesCaptionPanel.setWidth("50%");
        CaptionPanel trackedRacesCaptionPanel = new CaptionPanel(stringMessages.trackedRaces());
        racesSplitPanel.add(trackedRacesCaptionPanel);
        trackedRacesCaptionPanel.setWidth("50%");
        VerticalPanel racesPanel = new VerticalPanel();
        racesCaptionPanel.setContentWidget(racesPanel);
        racesCaptionPanel.setStyleName("bold");
        VerticalPanel trackedRacesPanel = new VerticalPanel();
        trackedRacesPanel.setWidth("100%");
        trackedRacesCaptionPanel.setContentWidget(trackedRacesPanel);
        trackedRacesCaptionPanel.setStyleName("bold");
        // text box for filtering the cell table
        // the regatta selection for a tracked race
        HorizontalPanel regattaPanel = new HorizontalPanel();
        racesPanel.add(regattaPanel);
        Label lblRegattas = new Label(stringMessages.regattaUsedForTheTrackedRace());
        lblRegattas.setWordWrap(false);
        regattaPanel.setCellVerticalAlignment(lblRegattas, HasVerticalAlignment.ALIGN_MIDDLE);
        regattaPanel.setSpacing(5);
        regattaPanel.add(lblRegattas);
        regattaPanel.add(getAvailableRegattasListBox());
        regattaPanel.setCellVerticalAlignment(getAvailableRegattasListBox(), HasVerticalAlignment.ALIGN_MIDDLE);
        HorizontalPanel filterPanel = new HorizontalPanel();
        filterPanel.setSpacing(5);
        racesPanel.add(filterPanel);
        Label lblFilterEvents = new Label(stringMessages.filterRaces()+ ":");
        filterPanel.add(lblFilterEvents);
        filterPanel.setCellVerticalAlignment(lblFilterEvents, HasVerticalAlignment.ALIGN_MIDDLE);
        HorizontalPanel racesHorizontalPanel = new HorizontalPanel();
        racesPanel.add(racesHorizontalPanel);
        VerticalPanel trackPanel = new VerticalPanel();
        trackPanel.setStyleName("paddedPanel");
        raceNameColumn.setSortable(true);
        raceStartTrackingColumn.setSortable(true);
        boatClassNamesColumn.setSortable(true);
        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        raceTable = new FlushableCellTable<>(10000, tableRes);
        raceTable.setWidth("300px");
        raceList = new ListDataProvider<SwissTimingReplayRaceDTO>();
        filterablePanelEvents = new LabeledAbstractFilterablePanel<SwissTimingReplayRaceDTO>(lblFilterEvents,
                availableSwissTimingRaces, raceList, stringMessages) {
            @Override
            public List<String> getSearchableStrings(SwissTimingReplayRaceDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.addAll(Arrays.asList(t.boat_class, t.flight_number, t.getName(), t.rsc));
                return strings;
            }

            @Override
            public AbstractCellTable<SwissTimingReplayRaceDTO> getCellTable() {
                return raceTable;
            }
        };
        final SelectionCheckboxColumn<SwissTimingReplayRaceDTO> checkColumn = new SelectionCheckboxColumn<SwissTimingReplayRaceDTO>(
                tableRes.cellTableStyle().cellTableCheckboxSelected(),
                tableRes.cellTableStyle().cellTableCheckboxDeselected(),
                tableRes.cellTableStyle().cellTableCheckboxColumnCell(),
                new EntityIdentityComparator<SwissTimingReplayRaceDTO>() {
                    @Override
                    public boolean representSameEntity(SwissTimingReplayRaceDTO a, SwissTimingReplayRaceDTO b) {
                        return a.race_id.equals(b.race_id);
                    }
                    @Override
                    public int hashCode(SwissTimingReplayRaceDTO t) {
                        return t.race_id.hashCode();
                    }
                }, filterablePanelEvents.getAllListDataProvider());
        checkColumn.setSortable(false);
        final Header<Boolean> selectAllHeader = checkColumn.createHeader();
        raceTable.setSelectionModel(checkColumn.getSelectionModel(), checkColumn.getSelectionManager());
        raceTable.addColumn(checkColumn, selectAllHeader);
        raceTable.addColumn(raceNameColumn, stringMessages.race());
        raceTable.addColumn(regattaNameColumn, "RSC");
        raceTable.addColumn(boatClassNamesColumn, stringMessages.boatClass());
        raceTable.addColumn(raceStartTrackingColumn, stringMessages.startTime());
        racesHorizontalPanel.add(raceTable);
        racesHorizontalPanel.add(trackPanel);
        raceList.addDataDisplay(raceTable);
        Handler columnSortHandler = getRaceTableColumnSortHandler(raceList.getList(), raceNameColumn, boatClassNamesColumn, raceStartTrackingColumn);
        raceTable.addColumnSortHandler(columnSortHandler);
        filterPanel.add(filterablePanelEvents);
        
        Label lblTrackSettings = new Label(stringMessages.trackNewEvent());
        trackPanel.add(lblTrackSettings);
        
        final CheckBox trackWindCheckbox = new CheckBox(stringMessages.trackWind());
        trackWindCheckbox.setWordWrap(false);
        trackWindCheckbox.setValue(true);
        trackPanel.add(trackWindCheckbox);

        final CheckBox declinationCheckbox = new CheckBox(stringMessages.declinationCheckbox());
        declinationCheckbox.setWordWrap(false);
        declinationCheckbox.setValue(true);
        trackPanel.add(declinationCheckbox);
        
        final CheckBox useInternalMarkPassingAlgorithmCheckbox = new CheckBox(stringMessages.useInternalAlgorithm());
        useInternalMarkPassingAlgorithmCheckbox.setWordWrap(false);
        useInternalMarkPassingAlgorithmCheckbox.setValue(Boolean.FALSE);
        trackPanel.add(useInternalMarkPassingAlgorithmCheckbox);

        trackedRacesPanel.add(trackedRacesListComposite);

        HorizontalPanel racesButtonPanel = new HorizontalPanel();
        racesPanel.add(racesButtonPanel);

        Button btnTrack = new Button(stringMessages.startTracking());
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

    private AccessControlledButtonPanel createButtonPanel(final SailingServiceWriteAsync sailingServiceWrite,
            UserService userService, ErrorReporter errorReporter, StringMessages stringMessages) {
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService,
                SecuredDomainType.SWISS_TIMING_ARCHIVE_ACCOUNT);
        // Refresh action
        buttonPanel.addUnsecuredAction(stringMessages.refresh(), () -> connectionsTable.refreshConnectionList());
        // Create action
        buttonPanel.addCreateAction(stringMessages.addSwissTimingAchivedConnection(),
                () -> new SwissTimingArchivedConnectionDialog(
                        new DialogCallback<SwissTimingArchiveConfigurationWithSecurityDTO>() {
                            @Override
                            public void ok(SwissTimingArchiveConfigurationWithSecurityDTO editedConnection) {
                                sailingServiceWrite.createSwissTimingArchiveConfiguration(editedConnection.getJsonUrl(),
                                        new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                errorReporter
                                                        .reportError("Exception trying to create configuration in DB: "
                                                                + caught.getMessage());
                                            }

                                            @Override
                                            public void onSuccess(Void voidResult) {
                                                connectionsTable.refreshConnectionList();
                                                connectionsTable.getFilterField().search(editedConnection.getJsonUrl());
                                            }
                                        }));
                            }

                            @Override
                            public void cancel() {
                            }
                        }, userService, errorReporter).show());
        // Remove action
        buttonPanel.addRemoveAction(stringMessages.remove(), connectionsTable.getSelectionModel(), false, () -> {
            sailingServiceWrite.deleteSwissTimingArchiveConfigurations(connectionsTable.getSelectionModel().getSelectedSet(),
                    new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(
                                    "Exception trying to delete configuration(s) in DB: " + caught.getMessage());
                        }

                        @Override
                        public void onSuccess(Void result) {
                            connectionsTable.refreshConnectionList();
                        }
                    });
        });
        // List Race action
        final Button listRacesButton = buttonPanel.addUnsecuredAction(stringMessages.listRaces(), () -> {
            fillRaces(sailingServiceWrite);
        });
        listRacesButton.setEnabled(false);
        // add change handlers to enable and disable List Races and Remove
        connectionsTable.getSelectionModel().addSelectionChangeHandler(e -> {
            final boolean objectSelected = connectionsTable.getSelectionModel().getSelectedSet().size() == 1;
            listRacesButton.setEnabled(objectSelected);
        });
        return buttonPanel;
    }

    private String getBoatClassNamesAsString(SwissTimingReplayRaceDTO object) {
        return object.boat_class;
    }

    private ListHandler<SwissTimingReplayRaceDTO> getRaceTableColumnSortHandler(List<SwissTimingReplayRaceDTO> raceRecords,
            Column<SwissTimingReplayRaceDTO, ?> nameColumn, Column<SwissTimingReplayRaceDTO, ?> boatClassColumn,
            Column<SwissTimingReplayRaceDTO, ?> startTimeColumn) {
        ListHandler<SwissTimingReplayRaceDTO> result = new ListHandler<SwissTimingReplayRaceDTO>(raceRecords);
        result.setComparator(nameColumn, new Comparator<SwissTimingReplayRaceDTO>() {
            @Override
            public int compare(SwissTimingReplayRaceDTO o1, SwissTimingReplayRaceDTO o2) {
                return new NaturalComparator().compare(o1.getName(), o2.getName());
            }
        });
        result.setComparator(boatClassColumn, new Comparator<SwissTimingReplayRaceDTO>() {
            @Override
            public int compare(SwissTimingReplayRaceDTO o1, SwissTimingReplayRaceDTO o2) {
                return new NaturalComparator(false).compare(getBoatClassNamesAsString(o1), getBoatClassNamesAsString(o2));
            }
        });
        result.setComparator(startTimeColumn, new Comparator<SwissTimingReplayRaceDTO>() {
            @Override
            public int compare(SwissTimingReplayRaceDTO o1, SwissTimingReplayRaceDTO o2) {
                return o1.startTime == null ? -1 : o2.startTime == null ? 1 : o1.startTime
                        .compareTo(o2.startTime);
            }
        });
        return result;
    }

    private void fillRaces(final SailingServiceAsync sailingService) {
        final Set<SwissTimingArchiveConfigurationWithSecurityDTO> selectedObjects = connectionsTable.getSelectionModel()
                .getSelectedSet();
        if (selectedObjects.size() == 1) {
            SwissTimingArchiveConfigurationWithSecurityDTO selectedObject = selectedObjects.iterator().next();
            sailingService.listSwissTiminigReplayRaces(selectedObject.getJsonUrl(),
                    new AsyncCallback<List<SwissTimingReplayRaceDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                SwissTimingReplayConnectorPanel.this.errorReporter.reportError("Error trying to list SwissTiming races: "
                        + caught.getMessage());
            }

            @Override
            public void onSuccess(final List<SwissTimingReplayRaceDTO> races) {
                availableSwissTimingRaces.clear();
                availableSwissTimingRaces.addAll(races);
                raceList.getList().clear();
                raceList.getList().addAll(availableSwissTimingRaces);
                filterablePanelEvents.getTextBox().setText(null);
                filterablePanelEvents.updateAll(races);
            }
        });
        }
    }

    private void trackSelectedRaces(boolean trackWind, boolean correctWindByDeclination, boolean useInternalMarkPassingAlgorithm) {
        RegattaDTO selectedRegatta = getSelectedRegatta();
        RegattaIdentifier regattaIdentifier = null;
        if (selectedRegatta != null) {
            regattaIdentifier = new RegattaName(selectedRegatta.getName());
        }
        
        // Check if the assigned regatta makes sense
        final List<SwissTimingReplayRaceDTO> selectedRaces = new ArrayList<>();  
        for (SwissTimingReplayRaceDTO replayRace : raceList.getList()) {
            if (raceTable.getSelectionModel().isSelected(replayRace)) {
                selectedRaces.add(replayRace);
            }
        }
        if (checkBoatClassOK(selectedRegatta, selectedRaces)) {
            sailingServiceWrite.replaySwissTimingRace(regattaIdentifier, selectedRaces, trackWind, correctWindByDeclination,
                useInternalMarkPassingAlgorithm, new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to register races " + selectedRaces
                                + " for tracking: " + caught.getMessage() + ". Check live/stored URI syntax.");
                    }

                    @Override
                    public void onSuccess(Void result) {
                        presenter.getRegattasRefresher().reloadAndCallFillAll();
                    }
                });
        }
    }

    @Override
    public AbstractFilterablePanel<SwissTimingArchiveConfigurationWithSecurityDTO> getFilterablePanel() {
        return connectionsTable.getFilterField();
    }
}
