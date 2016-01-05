package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.shared.GWT;
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
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.gwt.ui.client.LeaderboardsDisplayer;
import com.sap.sailing.gwt.ui.client.LeaderboardsRefresher;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.FlushableCellTable;
import com.sap.sailing.gwt.ui.client.shared.controls.SelectionCheckboxColumn;
import com.sap.sailing.gwt.ui.shared.ControlPointDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetStartTimeAndProcedureDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;

/**
 * Allows the user to start and stop tracking of races using the RaceLog-tracking connector.
 */
public class SmartphoneTrackingEventManagementPanel extends AbstractLeaderboardConfigPanel implements LeaderboardsDisplayer {
    private Button startTrackingButton;
    private TrackFileImportDeviceIdentifierTableWrapper deviceIdentifierTable;
    private CheckBox correctWindDirectionForDeclination;
    private CheckBox trackWind;
    protected boolean regattaHasCompetitors = false;
    
    
    public SmartphoneTrackingEventManagementPanel(SailingServiceAsync sailingService,
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
    
    private static interface ShowWithBoatClass {
        void showWithBoatClass(String boatClassName);
    }
    
    /**
     * When doing race log tracking, the Remove and Stop Tracking buttons are required.
     */
    protected boolean isActionButtonsEnabled() {
        return /* actionButtonsEnabled */ true;
    }
    
    @Override
    protected void addColumnsToLeaderboardTableAndSetSelectionModel(FlushableCellTable<StrippedLeaderboardDTO> leaderboardTable, 
            AdminConsoleTableResources tableResources, ListDataProvider<StrippedLeaderboardDTO> listDataProvider) {
        ListHandler<StrippedLeaderboardDTO> leaderboardColumnListHandler = new ListHandler<StrippedLeaderboardDTO>(
                leaderboardList.getList());
        SelectionCheckboxColumn<StrippedLeaderboardDTO> selectionCheckboxColumn = createSortableSelectionCheckboxColumn(
                leaderboardTable, tableResources, leaderboardColumnListHandler, listDataProvider);
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
                if (RaceLogTrackingEventManagementImagesBarCell.ACTION_DENOTE_FOR_RACELOG_TRACKING.equals(value)) {
                    denoteForRaceLogTracking(leaderboardDTO);
                } else if (RaceLogTrackingEventManagementImagesBarCell.ACTION_COMPETITOR_REGISTRATIONS.equals(value)) {
                    ShowWithBoatClass showWithBoatClass = new ShowWithBoatClass() {
                        @Override
                        public void showWithBoatClass(String boatClassName) {
                            new RegattaLogCompetitorRegistrationDialog(boatClassName, sailingService, stringMessages,
                                    errorReporter, /* editable */true, leaderboardName,
                                    new DialogCallback<Set<CompetitorDTO>>() {
                                        @Override
                                        public void ok(Set<CompetitorDTO> registeredCompetitors) {
                                            sailingService.setCompetitorRegistrationsInRegattaLog(leaderboardName,
                                                    registeredCompetitors, new AsyncCallback<Void>() {
                                                        @Override
                                                        public void onSuccess(Void result) {
                                                            // pass
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
                        }

                    };
                    searchBoatClass(showWithBoatClass);
                } else if (RaceLogTrackingEventManagementImagesBarCell.ACTION_MAP_DEVICES.equals(value)) {
                    new RegattaLogTrackingDeviceMappingsDialog(sailingService, stringMessages, errorReporter,
                            leaderboardName, new DialogCallback<Void>() {
                                @Override
                                public void ok(Void editedObject) {
                                }

                                @Override
                                public void cancel() {
                                }
                            }).show();
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
            public void update(int index, final RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnDTOAndFleetDTO, String value) {
                final String leaderboardName = getSelectedLeaderboardName();
                final String raceColumnName = raceColumnDTOAndFleetDTO.getA().getName();
                final String fleetName = raceColumnDTOAndFleetDTO.getB().getName();
                final boolean editable = ! (doesTrackerExist(raceColumnDTOAndFleetDTO) &&
                        getTrackingState(raceColumnDTOAndFleetDTO) == RaceLogTrackingState.TRACKING);
                if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_DENOTE_FOR_RACELOG_TRACKING.equals(value)) {
                    denoteForRaceLogTracking(raceColumnDTOAndFleetDTO.getA(), raceColumnDTOAndFleetDTO.getB());
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_REMOVE_DENOTATION.equals(value)) {
                    removeDenotation(raceColumnDTOAndFleetDTO.getA(), raceColumnDTOAndFleetDTO.getB());
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_COMPETITOR_REGISTRATIONS
                        .equals(value)) {
                    ShowWithBoatClass showWithBoatClass = new ShowWithBoatClass() {
                        @Override
                        public void showWithBoatClass(String boatClassName) {
                            new RaceLogCompetitorRegistrationDialog(boatClassName, sailingService, stringMessages,
                                    errorReporter, editable, leaderboardName, raceColumnName, fleetName,
                                    new DialogCallback<Set<CompetitorDTO>>() {

                                        @Override
                                        public void ok(final Set<CompetitorDTO> registeredCompetitors) {
                                            sailingService.setCompetitorRegistrationsInRaceLog(leaderboardName,
                                                    raceColumnName, fleetName, registeredCompetitors,
                                                    new AsyncCallback<Void>() {
                                                        @Override
                                                        public void onSuccess(Void result) {
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
                        }
                    };
                    searchBoatClass(showWithBoatClass);
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_DEFINE_COURSE.equals(value)) {
                    new RaceLogTrackingCourseDefinitionDialog(sailingService, stringMessages, errorReporter, leaderboardName, raceColumnName, 
                            fleetName, new DialogCallback<List<com.sap.sse.common.Util.Pair<ControlPointDTO,PassingInstruction>>>() {
                        @Override
                        public void cancel() {
                        }

                        @Override
                        public void ok(List<Pair<ControlPointDTO, PassingInstruction>> waypointPairs) {
                            sailingService.addCourseDefinitionToRaceLog(leaderboardName, raceColumnName, fleetName, waypointPairs,
                                    new AsyncCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                }

                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError("Could note save course: " + caught.getMessage());
                                }
                            });
                        }
                    }).show();

                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_MAP_DEVICES.equals(value)) {
                    new RaceLogTrackingDeviceMappingsDialog(sailingService, stringMessages, errorReporter, leaderboardName, raceColumnName, fleetName, new DialogCallback<Void>() {
                        @Override
                        public void ok(Void editedObject) {
                        }

                        @Override
                        public void cancel() {
                        }
                    }).show();
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_COPY.equals(value)) {
                    List<RaceColumnDTOAndFleetDTOWithNameBasedEquality> races =
                            new ArrayList<>(raceColumnTable.getDataProvider().getList());
                    races.remove(raceColumnDTOAndFleetDTO);
                    new CopyCourseAndCompetitorsDialog(sailingService, errorReporter, stringMessages, races,
                            leaderboardName, new DialogCallback<CourseAndCompetitorCopyOperation>() {
                                @Override
                                public void ok(CourseAndCompetitorCopyOperation operation) {
                                    operation.perform(leaderboardName, raceColumnDTOAndFleetDTO);
                                    loadAndRefreshLeaderboard(leaderboardName, raceColumnDTOAndFleetDTO.getA().getName());
                                }

                                @Override
                                public void cancel() {}
                    }).show();
                } else if (LeaderboardRaceConfigImagesBarCell.ACTION_EDIT.equals(value)) {
                    editRaceColumnOfLeaderboard(raceColumnDTOAndFleetDTO);
                } else if (LeaderboardRaceConfigImagesBarCell.ACTION_UNLINK.equals(value)) {
                    unlinkRaceColumnFromTrackedRace(raceColumnDTOAndFleetDTO.getA().getRaceColumnName(), raceColumnDTOAndFleetDTO.getB());
                } else if (LeaderboardRaceConfigImagesBarCell.ACTION_REFRESH_RACELOG.equals(value)) {
                    refreshRaceLog(raceColumnDTOAndFleetDTO.getA(), raceColumnDTOAndFleetDTO.getB(), true);
                } else if (RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_SET_STARTTIME.equals(value)) {
                    setStartTime(getSelectedRaceColumnWithFleet().getA(), getSelectedRaceColumnWithFleet().getB());
                } else if (LeaderboardRaceConfigImagesBarCell.ACTION_SHOW_RACELOG.equals(value)) {
                    showRaceLog(raceColumnDTOAndFleetDTO.getA(), raceColumnDTOAndFleetDTO.getB());
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

    @Override
    protected void addLeaderboardControls(Panel controlsPanel) {}

    @Override
    protected void addSelectedLeaderboardRacesControls(Panel racesPanel) {
        trackWind = new CheckBox(stringMessages.trackWind());
        correctWindDirectionForDeclination = new CheckBox(stringMessages.declinationCheckbox());
        startTrackingButton = new Button(stringMessages.startTracking());
        startTrackingButton.ensureDebugId("StartTrackingButton");
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
    
    private void searchBoatClass(final ShowWithBoatClass showWithBoatClass) {
        final String result;
        RegattaDTO regatta = null;
        if (getSelectedLeaderboard().regattaName != null) {
        if (allRegattas != null) {
            for (RegattaDTO i : allRegattas) {
                    if (getSelectedLeaderboard().regattaName.equals(i.getName())) {
                        regatta = i;
                        break;
                    }
                }
            }
        }
        if (regatta != null) {
            result = regatta.boatClass.getName();
            showWithBoatClass.showWithBoatClass(result);
        } else {
            sailingService.getCompetitorsOfLeaderboard(getSelectedLeaderboardName(),
                    new AsyncCallback<Iterable<CompetitorDTO>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            GWT.log("Error while searching BoatClass.");
                            showWithBoatClass.showWithBoatClass(null);
                        }
                        
                        @Override
                        public void onSuccess(Iterable<CompetitorDTO> result) {
                            String boatClass = null;
                            if (result != null) {
                                List<String> boatClassen = new ArrayList<>();
                                for (CompetitorDTO comp : result) {
                                    boatClassen.add(comp.getBoatClass().getName());
                                }
                                boatClass = Util.getDominantObject(boatClassen);
                            }
                            showWithBoatClass.showWithBoatClass(boatClass);
                        }
                    });
        }
    }
}
