package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.gwt.ui.client.LeaderboardsDisplayer;
import com.sap.sailing.gwt.ui.client.LeaderboardsRefresher;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.SelectionCheckboxColumn;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetStartTimeAndProcedureDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;

/**
 * Allows the user to start and stop tracking of races using the RaceLog-tracking connector.
 */
public class RaceLogTrackingEventManagementPanel extends AbstractLeaderboardConfigPanel implements LeaderboardsDisplayer {
    private Button startTrackingButton;
    private TrackFileImportDeviceIdentifierTableWrapper deviceIdentifierTable;
    private CheckBox correctWindDirectionForDeclination;
    private CheckBox trackWind;
    protected boolean regattaHasCompetitors = false;
    
    public RaceLogTrackingEventManagementPanel(SailingServiceAsync sailingService,
            RegattaRefresher regattaRefresher, LeaderboardsRefresher leaderboardsRefresher,
            ErrorReporter errorReporter, StringMessages stringMessages) {
        super(sailingService, regattaRefresher, leaderboardsRefresher, errorReporter,
                stringMessages, /* multiSelection */ true);
        
        // add upload panel
        CaptionPanel importPanel = new CaptionPanel(stringMessages.importFixes());
        VerticalPanel importContent = new VerticalPanel();
        mainPanel.add(importPanel);
        deviceIdentifierTable = new TrackFileImportDeviceIdentifierTableWrapper(sailingService, stringMessages, errorReporter);
        TrackFileImportWidget importWidget = new TrackFileImportWidget(deviceIdentifierTable, stringMessages,
                sailingService, errorReporter);
        importPanel.add(importContent);
        importContent.add(importWidget);
        importContent.add(deviceIdentifierTable);
    }
    
    /**
     * When doing race log tracking, the Remove and Stop Tracking buttons are required.
     */
    protected boolean isActionButtonsEnabled() {
        return /* actionButtonsEnabled */ true;
    }
    
    @Override
    protected void addColumnsToLeaderboardTableAndSetSelectionModel(CellTable<StrippedLeaderboardDTO> leaderboardTable, AdminConsoleTableResources tableResources) {
        ListHandler<StrippedLeaderboardDTO> leaderboardColumnListHandler = new ListHandler<StrippedLeaderboardDTO>(
                leaderboardList.getList());
        SelectionCheckboxColumn<StrippedLeaderboardDTO> selectionCheckboxColumn = createSortableSelectionCheckboxColumn(
                leaderboardTable, tableResources, leaderboardColumnListHandler);
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
                final String leaderboardName = leaderboardDTO.name;
//                final String eventIdAsString = leaderboardDTO.
                if (RaceLogTrackingEventManagementImagesBarCell.ACTION_DENOTE_FOR_RACELOG_TRACKING.equals(value)) {
                    denoteForRaceLogTracking(leaderboardDTO);
                } else if (RaceLogTrackingEventManagementImagesBarCell.ACTION_COMPETITOR_REGISTRATIONS.equals(value)) {
                    new RegattaLogCompetitorRegistrationDialog(sailingService, stringMessages, errorReporter, /*editable*/ true, 
                            leaderboardName, new DialogCallback<Set<CompetitorDTO>>() {

                                @Override
                                public void ok(Set<CompetitorDTO> registeredCompetitors) {
                                    sailingService.setCompetitorRegistrationsInRegattaLog(leaderboardName, registeredCompetitors,
                                            new AsyncCallback<Void>() {
                                                @Override
                                                public void onSuccess(Void result) {
                                                    // pass
                                                }

                                                @Override
                                                public void onFailure(Throwable caught) {
                                                    errorReporter.reportError("Could not save competitor registrations: "
                                                            + caught.getMessage());
                                                }
                                    });
                                }

                                @Override
                                public void cancel() {
                                    
                                }

                            }).show();
                } else if (RaceLogTrackingEventManagementImagesBarCell.ACTION_MAP_DEVICES.equals(value)) {
                    new AddDeviceMappingToRegattaLogDialog(sailingService, errorReporter, stringMessages,
                            leaderboardName).show();
                } else if (RaceLogTrackingEventManagementImagesBarCell.ACTION_INVITE_BUOY_TENDERS.equals(value)) {
                    openChooseEventDialogAndSendMails(leaderboardName);
                } else if (RaceLogTrackingEventManagementImagesBarCell.ACTION_SHOW_REGATTA_LOG.equals(value)) {
                    showRegattaLog();
                }
            }
        });
        leaderboardTable.addColumn(selectionCheckboxColumn, selectionCheckboxColumn.getHeader());
        leaderboardTable.addColumn(leaderboardNameColumn, stringMessages.name());
        leaderboardTable.addColumn(leaderboardDisplayNameColumn, stringMessages.displayName());
        leaderboardTable.addColumn(leaderboardActionColumn, stringMessages.actions());
        leaderboardTable.addColumnSortHandler(leaderboardColumnListHandler);
        leaderboardTable.setSelectionModel(selectionCheckboxColumn.getSelectionModel(), selectionCheckboxColumn.getSelectionManager());
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
                return doesTrackerExist(raceColumnAndFleetName) ? stringMessages.active() : stringMessages.none();
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
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_COMPETITOR_REGISTRATIONS
                        .equals(value)) {
                    new RaceLogCompetitorRegistrationDialog(sailingService, stringMessages, errorReporter, editable, leaderboardName, 
                            raceColumnName, fleetName, new DialogCallback<Set<CompetitorDTO>>() {

                        @Override
                        public void ok(final Set<CompetitorDTO> registeredCompetitors) {
                            sailingService.setCompetitorRegistrationsInRaceLog(leaderboardName, raceColumnName,
                                    fleetName, registeredCompetitors, new AsyncCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    object.getA().getRaceLogTrackingInfo(object.getB()).competitorRegistrationsExists = !registeredCompetitors.isEmpty();
                                }

                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter
                                    .reportError("Could not save competitor registrations: "
                                            + caught.getMessage());
                                }
                            });
                        }

                        @Override
                        public void cancel() {
                        }
                    }).show();
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_DEFINE_COURSE.equals(value)) {
                    new RaceLogTrackingCourseDefinitionDialog(sailingService, stringMessages, errorReporter,
                            leaderboardName, raceColumnName, fleetName).show();
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_MAP_DEVICES.equals(value)) {
                    new RaceLogTrackingDeviceMappingsDialog(sailingService, stringMessages, errorReporter, leaderboardName, raceColumnName, fleetName).show();
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_COPY.equals(value)) {
                    List<RaceColumnDTOAndFleetDTOWithNameBasedEquality> races =
                            new ArrayList<>(raceColumnTable.getDataProvider().getList());
                    races.remove(object);
                    new SelectRacesDialog(sailingService, errorReporter, stringMessages, races,
                            leaderboardName, new DialogCallback<Set<RaceColumnDTOAndFleetDTOWithNameBasedEquality>>() {
                                @Override
                                public void ok(Set<RaceColumnDTOAndFleetDTOWithNameBasedEquality> editedObject) {
                                    Set<Util.Triple<String, String, String>> toRaceLogs = new java.util.HashSet<>();
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
                } else if (LeaderboardRaceConfigImagesBarCell.ACTION_EDIT.equals(value)) {
                    editRaceColumnOfLeaderboard(object);
                } else if (LeaderboardRaceConfigImagesBarCell.ACTION_UNLINK.equals(value)) {
                    unlinkRaceColumnFromTrackedRace(object.getA().getRaceColumnName(), object.getB());
                } else if (LeaderboardRaceConfigImagesBarCell.ACTION_REFRESH_RACELOG.equals(value)) {
                    refreshRaceLog(object.getA(), object.getB(), true);
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_SET_STARTTIME.equals(value)) {
                    setStartTime(getSelectedRaceColumnWithFleet().getA(), getSelectedRaceColumnWithFleet().getB());
                } else if (LeaderboardRaceConfigImagesBarCell.ACTION_SHOW_RACELOG.equals(value)) {
                    showRaceLog(object.getA(), object.getB());
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_SET_TRACKING_TIMES.equals(value)) {
                    setTrackingTimes(getSelectedRaceColumnWithFleet().getA(), getSelectedRaceColumnWithFleet().getB());;
                }
            }
        });
        
        racesTable.addColumn(raceLogTrackingStateColumn, stringMessages.raceStatusColumn());
        racesTable.addColumn(trackerStateColumn, stringMessages.trackerStatus());
        racesTable.addColumn(raceActionColumn, stringMessages.actions());
        racesTable.setWidth("600px");
    }
    
    private Util.Triple<String, String, String> toTriple(String leaderboardName,
            RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
        return new Util.Triple<String, String, String>(leaderboardName, race.getA().getName(), race.getB().getName());
    }

    @Override
    protected void addLeaderboardControls(Panel controlsPanel) {}

    @Override
    protected void addSelectedLeaderboardRacesControls(Panel racesPanel) {
        trackWind = new CheckBox(stringMessages.trackWind());
        correctWindDirectionForDeclination = new CheckBox(stringMessages.declinationCheckbox());
        startTrackingButton = new Button(stringMessages.startTracking());
        startTrackingButton.setEnabled(false);
        racesPanel.add(trackWind);
        racesPanel.add(correctWindDirectionForDeclination);
        racesPanel.add(startTrackingButton);
        startTrackingButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                startTracking(raceColumnTableSelectionModel.getSelectedSet(), trackWind.getValue(), correctWindDirectionForDeclination.getValue());
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
        regattaHasCompetitors = false;
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
            
            sailingService.doesRegattaLogContainCompetitors(((StrippedLeaderboardDTO) leaderboardSelectionModel.getSelectedSet().toArray()[0]).name, new RegattaLogCallBack());
        } else {
            selectedLeaderBoardPanel.setVisible(false);
            trackedRacesCaptionPanel.setVisible(false);
        }
        raceColumnTableSelectionModel.clear();
    }

    
    private class RegattaLogCallBack implements AsyncCallback<Boolean>{
        @Override
        public void onFailure(Throwable caught) {
            regattaHasCompetitors = false;
        }

        @Override
        public void onSuccess(Boolean result) {
            regattaHasCompetitors = true;
        }
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
    
    private void startTracking(Set<RaceColumnDTOAndFleetDTOWithNameBasedEquality> races, boolean trackWind, boolean correctWindByDeclination) {
        final StrippedLeaderboardDTO leaderboard = getSelectedLeaderboard();
        //prompt user if competitor registrations are missing for same races
        String namesOfRacesMissingRegistrations = "";
        
        if (!regattaHasCompetitors) {
            for (RaceColumnDTOAndFleetDTOWithNameBasedEquality race : races) {
                if (!doCompetitorResgistrationsExist(race)) {
                    namesOfRacesMissingRegistrations += race.getA().getName() + "/" + race.getB().getName() + " ";
                }
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
                    trackWind, correctWindByDeclination,
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
    
    private void setTrackingTimes(RaceColumnDTO raceColumn, FleetDTO fleet) {
        new SetTrackingTimesDialog(sailingService, errorReporter, getSelectedLeaderboardName(), raceColumn.getName(),
                fleet.getName(), stringMessages, new DataEntryDialog.DialogCallback<RaceLogSetTrackingTimesDTO>() {

                    @Override
                    public void ok(RaceLogSetTrackingTimesDTO editedObject) {
                        sailingService.setTrackingTimes(editedObject, new AsyncCallback<Void>(){

                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError("Error while setting tracking times: " + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Void result) {
                            }
                            
                        });
                        
                    }

                    @Override
                    public void cancel() {}
                }).show();
    }

    private String getLocaleInfo() {
        return LocaleInfo.getCurrentLocale().getLocaleName();
    }
    
    private void openChooseEventDialogAndSendMails(final String leaderBoardName) {
        new InviteBuoyTenderDialog(stringMessages, sailingService, leaderBoardName, errorReporter, new DialogCallback<Triple<EventDTO, String, String>>() {
            @Override
            public void ok(Triple<EventDTO, String, String> result) {
                sailingService.inviteBuoyTenderViaEmail(result.getB(), result.getA(), leaderBoardName, result.getC(), getLocaleInfo(), new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Window.alert(stringMessages.sendingMailsFailed() + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                Window.alert(stringMessages.sendingMailsSuccessful());
                            }
                        });
            }

            @Override
            public void cancel() {
                
            }
        }).show();
        
    }
}
