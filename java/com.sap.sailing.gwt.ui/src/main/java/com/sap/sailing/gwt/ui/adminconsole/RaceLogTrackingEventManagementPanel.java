package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.Callback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.LeaderboardsDisplayer;
import com.sap.sailing.gwt.ui.client.LeaderboardsRefresher;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RaceLogSetStartTimeAndProcedureDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;

/**
 * Allows the user to start and stop tracking of races using the RaceLog-tracking connector.
 */
public class RaceLogTrackingEventManagementPanel extends AbstractLeaderboardConfigPanel implements LeaderboardsDisplayer {
    private Button startTrackingButton;
    private TrackFileImportDeviceIdentifierTableWrapper deviceIdentifierTable;
    
    public RaceLogTrackingEventManagementPanel(SailingServiceAsync sailingService,
            RegattaRefresher regattaRefresher, LeaderboardsRefresher leaderboardsRefresher,
            ErrorReporter errorReporter, StringMessages stringMessages) {
        super(sailingService, regattaRefresher, leaderboardsRefresher, errorReporter,
                stringMessages, new MultiSelectionModel<RaceColumnDTOAndFleetDTOWithNameBasedEquality>());
        
        //add upload panel
        CaptionPanel importPanel = new CaptionPanel(stringMessages.importFixes());
        VerticalPanel importContent = new VerticalPanel();
        mainPanel.add(importPanel);
        deviceIdentifierTable = new TrackFileImportDeviceIdentifierTableWrapper(sailingService, stringMessages, errorReporter);
        TrackFileImportWidget importWidget = new TrackFileImportWidget(deviceIdentifierTable, stringMessages);
        importPanel.add(importContent);
        importContent.add(importWidget);
        importContent.add(deviceIdentifierTable);
    }
    
    @Override
    protected void addColumnsToLeaderboardTable(CellTable<StrippedLeaderboardDTO> leaderboardTable) {
        ListHandler<StrippedLeaderboardDTO> leaderboardColumnListHandler = new ListHandler<StrippedLeaderboardDTO>(
                leaderboardList.getList());

        TextColumn<StrippedLeaderboardDTO> leaderboardNameColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.name;
            }
        };

        TextColumn<StrippedLeaderboardDTO> leaderboardDisplayNameColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.getDisplayName() != null ? leaderboard.getDisplayName() : "";
            }
        };

        ImagesBarColumn<StrippedLeaderboardDTO, RaceLogTrackingEventManagementImagesBarCell> leaderboardActionColumn =
                new ImagesBarColumn<StrippedLeaderboardDTO, RaceLogTrackingEventManagementImagesBarCell>(
                new RaceLogTrackingEventManagementImagesBarCell(stringMessages));
        leaderboardActionColumn.setFieldUpdater(new FieldUpdater<StrippedLeaderboardDTO, String>() {
            @Override
            public void update(int index, StrippedLeaderboardDTO leaderboardDTO, String value) {
                if (RaceLogTrackingEventManagementImagesBarCell.ACTION_DENOTE_FOR_RACELOG_TRACKING.equals(value)) {
                    denoteForRaceLogTracking(leaderboardDTO);
                }
            }
        });

        leaderboardTable.addColumn(leaderboardNameColumn, stringMessages.name());
        leaderboardTable.addColumn(leaderboardDisplayNameColumn, stringMessages.displayName());
        leaderboardTable.addColumn(leaderboardActionColumn, stringMessages.actions());
        leaderboardTable.addColumnSortHandler(leaderboardColumnListHandler);
    }
    
    private RaceLogTrackingState getTrackingState(RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
        return race.getA().getRaceLogTrackingInfo(race.getB()).raceLogTrackingState;
    }
    
    private boolean doesTrackerExist(RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
        return race.getA().getRaceLogTrackingInfo(race.getB()).raceLogTrackerExists;
    }
    
    private boolean doCompetitorResgistrationsExist(RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
        return race.getA().getRaceLogTrackingInfo(race.getB()).competitorRegistrationsExists;
    }
    
    @Override
    protected void addColumnsToRacesTable(CellTable<RaceColumnDTOAndFleetDTOWithNameBasedEquality> racesTable) {
        TextColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality> raceLogTrackingStateColumn = new TextColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality>() {
            @Override
            public String getValue(RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnAndFleetName) {
                RaceLogTrackingState state = getTrackingState(raceColumnAndFleetName);
                return state.name();
            }
        };

        TextColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality> trackerStateColumn = new TextColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality>() {
            @Override
            public String getValue(RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnAndFleetName) {
                return doesTrackerExist(raceColumnAndFleetName) ? "Active" : "None";
            }
        };

        ImagesBarColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality, RaceLogTrackingEventManagementRaceImagesBarCell> raceActionColumn =
                new ImagesBarColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality, RaceLogTrackingEventManagementRaceImagesBarCell>(
                        new RaceLogTrackingEventManagementRaceImagesBarCell(stringMessages));
        raceActionColumn.setFieldUpdater(new FieldUpdater<RaceColumnDTOAndFleetDTOWithNameBasedEquality, String>() {
            @Override
            public void update(int index, final RaceColumnDTOAndFleetDTOWithNameBasedEquality object, String value) {
                final String leaderboardName = getSelectedLeaderboardName();
                final String raceColumnName = object.getA().getName();
                final String fleetName = object.getB().getName();
                boolean editable = ! (doesTrackerExist(object) &&
                        getTrackingState(object) == RaceLogTrackingState.TRACKING);
                if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_DENOTE_FOR_RACELOG_TRACKING.equals(value)) {
                    denoteForRaceLogTracking(object.getA(), object.getB());
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_REMOVE_DENOTATION.equals(value)) {
                    removeDenotation(object.getA(), object.getB());
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_COMPETITOR_REGISTRATIONS.equals(value)) {
                    new RaceLogTrackingCompetitorRegistrationsDialog(sailingService, stringMessages, errorReporter,
                            getSelectedLeaderboardName(), object.getA().getName(), object.getB().getName(), editable,
                            new Callback<Boolean, Throwable>() {
                                @Override
                                public void onSuccess(Boolean result) {
                                    object.getA().getRaceLogTrackingInfo(object.getB()).competitorRegistrationsExists = result;
                                }
                                
                                @Override
                                public void onFailure(Throwable reason) {}
                            }).show();
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_DEFINE_COURSE.equals(value)) {
                    new RaceLogTrackingCourseDefinitionDialog(sailingService, stringMessages, errorReporter, leaderboardName, raceColumnName, fleetName).show();
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_MAP_DEVICES.equals(value)) {
                    new RaceLogTrackingDeviceMappingsDialog(sailingService, stringMessages, errorReporter, leaderboardName, raceColumnName, fleetName).show();
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_COPY.equals(value)) {
                    List<RaceColumnDTOAndFleetDTOWithNameBasedEquality> races =
                            new ArrayList<>(raceColumnTable.getDataProvider().getList());
                    races.remove(object);
                    new SelectRacesDialog(sailingService, errorReporter, stringMessages, races,
                            new DialogCallback<Set<RaceColumnDTOAndFleetDTOWithNameBasedEquality>>() {
                                @Override
                                public void ok(Set<RaceColumnDTOAndFleetDTOWithNameBasedEquality> editedObject) {
                                    Set<Triple<String, String, String>> toRaceLogs = new HashSet<>();
                                    for (RaceColumnDTOAndFleetDTOWithNameBasedEquality race : editedObject) {
                                        toRaceLogs.add(toTriple(leaderboardName, race));
                                    }
                                    sailingService.copyCourseAndCompetitorsToOtherRaceLogs(
                                            toTriple(leaderboardName, object), toRaceLogs, new AsyncCallback<Void>() {
                                                @Override
                                                public void onFailure(Throwable caught) {
                                                    errorReporter.reportError("Could not copy course and competitors: " + caught.getMessage());
                                                }

                                                @Override
                                                public void onSuccess(Void result) {
                                                    loadAndRefreshLeaderboard(leaderboardName, object.getA().getName());
                                                }
                                            });
                                            
                                }

                                @Override
                                public void cancel() {}
                    }).show();
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_SET_START_TIME.equals(value)) {
                    setStartTime(getSelectedRaceColumnWithFleet().getA(), getSelectedRaceColumnWithFleet().getB());
                }
            }
        });
        
        racesTable.addColumn(raceLogTrackingStateColumn, stringMessages.raceStatusColumn());
        racesTable.addColumn(trackerStateColumn, stringMessages.trackerStatus());
        racesTable.addColumn(raceActionColumn, stringMessages.actions());
        racesTable.setWidth("600px");
    }
    
    private Triple<String, String, String> toTriple(String leaderboardName,
            RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
        return new Triple<String, String, String>(leaderboardName, race.getA().getName(), race.getB().getName());
    }

    @Override
    protected void addLeaderboardConfigControls(Panel configPanel) {}

    @Override
    protected void addLeaderboardCreateControls(Panel createPanel) {}

    @Override
    protected void addSelectedLeaderboardRacesControls(Panel racesPanel) {
        startTrackingButton = new Button(stringMessages.startTracking());
        startTrackingButton.setEnabled(false);
        racesPanel.add(startTrackingButton);
        startTrackingButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                startTracking(raceColumnTableSelectionModel.getSelectedSet());
            }
        });
        
        raceColumnTableSelectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                enableStartTrackingButtonIfAppropriateRacesSelected();
            }
        });
    }
    
    private void enableStartTrackingButtonIfAppropriateRacesSelected() {
        boolean enable = raceColumnTableSelectionModel.getSelectedSet().size() > 0;
        for (RaceColumnDTOAndFleetDTOWithNameBasedEquality race : raceColumnTableSelectionModel.getSelectedSet()) {
            if (! getTrackingState(race).isForTracking() || doesTrackerExist(race)) {
                enable = false;
            }
        }
        startTrackingButton.setEnabled(enable);
    }

    @Override
    protected void leaderboardRaceColumnSelectionChanged() {
        RaceColumnDTOAndFleetDTOWithNameBasedEquality selectedRaceInLeaderboard = getSelectedRaceColumnWithFleet();
        if (selectedRaceInLeaderboard != null) {
            selectTrackedRaceInRaceList();
        } else {
            trackedRacesListComposite.clearSelection();
        }
        enableStartTrackingButtonIfAppropriateRacesSelected();
    }
    
    @Override
    protected void leaderboardSelectionChanged() {
        StrippedLeaderboardDTO selectedLeaderboard = getSelectedLeaderboard();
        if (leaderboardSelectionModel.getSelectedSet().size() == 1 && selectedLeaderboard != null) {
            raceColumnTable.getDataProvider().getList().clear();
            for (RaceColumnDTO raceColumn : selectedLeaderboard.getRaceList()) {
                for (FleetDTO fleet : raceColumn.getFleets()) {
                    raceColumnTable.getDataProvider().getList().add(new RaceColumnDTOAndFleetDTOWithNameBasedEquality(raceColumn, fleet));
                }
            }
            selectedLeaderBoardPanel.setVisible(true);
            selectedLeaderBoardPanel.setCaptionText("Details of leaderboard '" + selectedLeaderboard.name + "'");
            if (!selectedLeaderboard.type.isMetaLeaderboard()) {
                trackedRacesCaptionPanel.setVisible(true);
            }
        } else {
            selectedLeaderBoardPanel.setVisible(false);
            trackedRacesCaptionPanel.setVisible(false);
        }
        raceColumnTableSelectionModel.clear();
    }

    private void denoteForRaceLogTracking(final StrippedLeaderboardDTO leaderboard) {
        sailingService.denoteForRaceLogTracking(leaderboard.name, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadAndRefreshLeaderboard(leaderboard.name, null);
                raceColumnTableSelectionModel.clear();
            }

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Could not denote for RaceLog tracking: " + caught.getMessage());
            }
        });
    }

    private void denoteForRaceLogTracking(final RaceColumnDTO raceColumn, final FleetDTO fleet) {
        final StrippedLeaderboardDTO leaderboard = getSelectedLeaderboard();
        sailingService.denoteForRaceLogTracking(leaderboard.name, raceColumn.getName(), fleet.getName(), new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadAndRefreshLeaderboard(leaderboard.name, raceColumn.getName());
            }

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Could not denote for RaceLog tracking: " + caught.getMessage());
            }
        });
    }
    
    private void removeDenotation(final RaceColumnDTO raceColumn, final FleetDTO fleet) {
        final StrippedLeaderboardDTO leaderboard = getSelectedLeaderboard();
        sailingService.removeDenotationForRaceLogTracking(leaderboard.name, raceColumn.getName(), fleet.getName(), new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadAndRefreshLeaderboard(leaderboard.name, raceColumn.getName());
            }

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Could not remove denotation: " + caught.getMessage());
            }
        });
    }
    
    private void startTracking(Set<RaceColumnDTOAndFleetDTOWithNameBasedEquality> races) {
        final StrippedLeaderboardDTO leaderboard = getSelectedLeaderboard();
        
        //prompt user if competitor registrations are missing for same races
        String namesOfRacesMissingRegistrations = "";
        for (RaceColumnDTOAndFleetDTOWithNameBasedEquality race : races) {
            if (! doCompetitorResgistrationsExist(race)) {
                namesOfRacesMissingRegistrations += race.getA().getName() + "/" + race.getB().getName() + " ";
            }
        }
        if (! namesOfRacesMissingRegistrations.isEmpty()) {
            boolean proceed = Window.confirm(stringMessages.competitorRegistrationsMissingProceed(
                    namesOfRacesMissingRegistrations));
            if (! proceed) {
                return;
            }
        }

        for (RaceColumnDTOAndFleetDTOWithNameBasedEquality race : races) {
            final RaceColumnDTO raceColumn = race.getA();
            final FleetDTO fleet = race.getB();

            sailingService.startRaceLogTracking(leaderboard.name, raceColumn.getName(), fleet.getName(),
                    new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    loadAndRefreshLeaderboard(leaderboard.name, raceColumn.getName());
                    trackedRacesListComposite.regattaRefresher.fillRegattas();
                }

                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Failed to start tracking " + raceColumn.getName() + " - "
                            + fleet.getName() + ": " + caught.getMessage());
                }
            });
        }
    }
    
    private void setStartTime(RaceColumnDTO raceColumnDTO, FleetDTO fleetDTO) {
        new SetStartTimeDialog(sailingService, errorReporter, getSelectedLeaderboardName(), raceColumnDTO.getName(), 
                fleetDTO.getName(), stringMessages, new DataEntryDialog.DialogCallback<RaceLogSetStartTimeAndProcedureDTO>() {
            @Override
            public void ok(RaceLogSetStartTimeAndProcedureDTO editedObject) {
                sailingService.setStartTimeAndProcedure(editedObject, new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Boolean result) {
                        if (!result) {
                            Window.alert(stringMessages.failedToSetNewStartTime());
                        } else {
                            trackedRacesListComposite.regattaRefresher.fillRegattas();
                        }
                    }
                });
            }

            @Override
            public void cancel() { }
        }).show();
    }
}
