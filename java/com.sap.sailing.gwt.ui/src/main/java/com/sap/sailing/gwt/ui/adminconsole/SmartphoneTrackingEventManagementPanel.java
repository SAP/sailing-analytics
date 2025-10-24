package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.READ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.abstractlog.TimePointSpecificationFoundInLog;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.Refresher;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapSettings;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetFinishingAndFinishTimeDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetStartTimeAndProcedureDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

/**
 * Allows the user to start and stop tracking of races using the RaceLog-tracking connector.
 */
public class SmartphoneTrackingEventManagementPanel extends AbstractLeaderboardConfigPanel {
    private ToggleButton startStopTrackingButton;
    private TrackFileImportDeviceIdentifierTableWrapper deviceIdentifierTable;
    private CheckBox correctWindDirectionForDeclination;
    private CheckBox trackWind;
    protected boolean regattaHasCompetitors = false; 
    private Map<Triple<String, String, String>, Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog>> raceWithStartAndEndOfTrackingTime = new HashMap<>();
    private CaptionPanel importPanel;
    private final Refresher<CompetitorDTO> competitorsRefresher;
    private final Refresher<BoatDTO> boatsRefresher;
    
    public SmartphoneTrackingEventManagementPanel(final Presenter presenter, StringMessages stringMessages) {
        super(presenter, stringMessages, /* multiSelection */ true);
        this.competitorsRefresher = presenter.getCompetitorsRefresher();
        this.boatsRefresher = presenter.getBoatsRefresher();
        // add upload panel
        importPanel = new CaptionPanel(stringMessages.importFixes());
        importPanel.setVisible(false);
        VerticalPanel importContent = new VerticalPanel();
        mainPanel.add(importPanel);
        deviceIdentifierTable = new TrackFileImportDeviceIdentifierTableWrapper(sailingServiceWrite, stringMessages, errorReporter);
        TrackFileImportWidget importWidget = new TrackFileImportWidget(deviceIdentifierTable, stringMessages,
                sailingServiceWrite, errorReporter);
        importPanel.add(importContent);
        importContent.add(importWidget);
        importContent.add(deviceIdentifierTable);
        trackedRacesListComposite.addTrackedRaceChangeListener(new TrackedRaceChangedListener() {
            @Override
            public void racesStoppedTracking(Iterable<? extends RegattaAndRaceIdentifier> regattaAndRaceIdentifiers) {
                loadAndRefreshLeaderboard(getSelectedLeaderboard().getName()); 
            }
            
            @Override
            public void racesRemoved(Iterable<? extends RegattaAndRaceIdentifier> regattaAndRaceIdentifiers) {
                loadAndRefreshLeaderboard(getSelectedLeaderboard().getName()); 
            }
        });
        this.userService.addUserStatusEventHandler(new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
                boolean couldPotentitallyChangeAnyLeaderboard = userService.hasCurrentUserAnyPermission(
                        SecuredDomainType.LEADERBOARD.getPermission(DefaultActions.UPDATE), null);
                importPanel.setVisible(couldPotentitallyChangeAnyLeaderboard);
            }
        }, true);
    }

    /**
     * When doing race log tracking, the Remove and Stop Tracking buttons are required.
     */
    protected boolean isActionButtonsEnabled() {
        return /* actionButtonsEnabled */ true;
    }
    
    @Override
    protected void addColumnsToLeaderboardTableAndSetSelectionModel(UserService userService,
            FlushableCellTable<StrippedLeaderboardDTO> leaderboardTable,
            AdminConsoleTableResources tableResources,
            ListDataProvider<StrippedLeaderboardDTO> listDataProvider) {
        ListHandler<StrippedLeaderboardDTO> leaderboardColumnListHandler = new ListHandler<StrippedLeaderboardDTO>(
                filteredLeaderboardList.getList());
        SelectionCheckboxColumn<StrippedLeaderboardDTO> selectionCheckboxColumn = createSortableSelectionCheckboxColumn(
                leaderboardTable, tableResources, leaderboardColumnListHandler, listDataProvider);
        TextColumn<StrippedLeaderboardDTO> leaderboardNameColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.getName();
            }
        };
        leaderboardNameColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(leaderboardNameColumn,
                new Comparator<StrippedLeaderboardDTO>() {
            @Override
                    public int compare(StrippedLeaderboardDTO o1, StrippedLeaderboardDTO o2) {
                return new NaturalComparator(false).compare(o1.getName(), o2.getName());
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
                        return new NaturalComparator(false).compare(o1.getDisplayName(), o2.getDisplayName());
                    }
                });
        TextColumn<StrippedLeaderboardDTO> leaderboardCanBoatsOfCompetitorsChangePerRaceColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.canBoatsOfCompetitorsChangePerRace ? stringMessages.yes() : stringMessages.no();
            }
        };
        leaderboardCanBoatsOfCompetitorsChangePerRaceColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(leaderboardCanBoatsOfCompetitorsChangePerRaceColumn, (l1, l2)->
            Boolean.valueOf(l1.canBoatsOfCompetitorsChangePerRace).compareTo(Boolean.valueOf(l2.canBoatsOfCompetitorsChangePerRace)));
        final HasPermissions type = SecuredDomainType.EVENT;
        final EditOwnershipDialog.DialogConfig<StrippedLeaderboardDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, leaderboard -> listDataProvider.refresh(), stringMessages);
        final EditACLDialog.DialogConfig<StrippedLeaderboardDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, leaderboard -> leaderboard.getAccessControlList(),
                stringMessages);
        final AccessControlledActionsColumn<StrippedLeaderboardDTO, RaceLogTrackingEventManagementImagesBarCell> leaderboardActionColumn = AccessControlledActionsColumn
                .create(new RaceLogTrackingEventManagementImagesBarCell(stringMessages), userService);
        leaderboardActionColumn.addAction(
                RaceLogTrackingEventManagementImagesBarCell.ACTION_DENOTE_FOR_RACELOG_TRACKING, DefaultActions.UPDATE,
                this::denoteForRaceLogTracking);
        leaderboardActionColumn.addAction(RaceLogTrackingEventManagementImagesBarCell.ACTION_COMPETITOR_REGISTRATIONS,
                DefaultActions.UPDATE, this::handleCompetitorRegistration);
        leaderboardActionColumn.addAction(RaceLogTrackingEventManagementImagesBarCell.ACTION_BOAT_REGISTRATIONS,
                DefaultActions.UPDATE, this::handleBoatRegistration);
        leaderboardActionColumn.addAction(RaceLogTrackingEventManagementImagesBarCell.ACTION_MAP_DEVICES,
                DefaultActions.UPDATE, this::handleDeviceMappings);
        leaderboardActionColumn.addAction(RaceLogTrackingEventManagementImagesBarCell.ACTION_INVITE_BUOY_TENDERS,
                DefaultActions.UPDATE, t -> openChooseEventDialogAndSendMails(t.getName()));
        leaderboardActionColumn.addAction(RaceLogTrackingEventManagementImagesBarCell.ACTION_SHOW_REGATTA_LOG,
                DefaultActions.UPDATE, t -> showRegattaLog());
        leaderboardActionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP, DefaultActions.UPDATE,
                configOwnership::openOwnershipDialog);
        leaderboardActionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.UPDATE,
                configACL::openDialog);
        leaderboardTable.addColumn(selectionCheckboxColumn, selectionCheckboxColumn.getHeader());
        leaderboardTable.addColumn(leaderboardNameColumn, stringMessages.name());
        leaderboardTable.addColumn(leaderboardDisplayNameColumn, stringMessages.displayName());
        leaderboardTable.addColumn(leaderboardCanBoatsOfCompetitorsChangePerRaceColumn,
                stringMessages.canBoatsChange());
        SecuredDTOOwnerColumn.configureOwnerColumns(leaderboardTable, leaderboardColumnListHandler, stringMessages);
        leaderboardTable.addColumn(leaderboardActionColumn, stringMessages.actions());
        leaderboardTable.addColumnSortHandler(leaderboardColumnListHandler);
        leaderboardTable.setSelectionModel(selectionCheckboxColumn.getSelectionModel(),
                selectionCheckboxColumn.getSelectionManager());
    }
    
    private RaceLogTrackingState getTrackingState(
            RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
        return race.getA().getRaceLogTrackingInfo(race.getB()).raceLogTrackingState;
    }
    
    private boolean trackerExists(RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
        return race.getA().getRaceLogTrackingInfo(race.getB()).raceLogTrackerExists;
    }
    
    private boolean isFinished(RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
        RaceDTO raceDTO = race.getA().getRace(race.getB());
        boolean raceFinished = false;
        if (raceDTO != null) {
            raceFinished = raceDTO.status.status.equals(TrackedRaceStatusEnum.FINISHED);
        }
        return raceFinished;
    }
    
    private boolean doesTrackerExist(
            RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
        return race.getA().getRaceLogTrackingInfo(race.getB()).raceLogTrackerExists;
    }

    private boolean doesCourseExist(RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
        return race.getA().getRaceLogTrackingInfo(race.getB()).courseExists;
    }

    private boolean doCompetitorRegistrationsExist(
            RaceColumnDTOAndFleetDTOWithNameBasedEquality race) {
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

        TextColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality> courseStateColumn = new TextColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality>() {
            @Override
            public String getValue(RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnAndFleetName) {
                return doesCourseExist(raceColumnAndFleetName) ? stringMessages.ok() : stringMessages.none();
            }
        };
        final AccessControlledActionsColumn<RaceColumnDTOAndFleetDTOWithNameBasedEquality, RaceLogTrackingEventManagementRaceImagesBarCell> raceActionColumn = AccessControlledActionsColumn
                .create(new RaceLogTrackingEventManagementRaceImagesBarCell(stringMessages, this), userService,
                        t -> t.getC());
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_DENOTE_FOR_RACELOG_TRACKING,
                DefaultActions.UPDATE, this::denoteForRaceLogTracking);
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_REMOVE_DENOTATION,
                DefaultActions.UPDATE, this::removeDenotation);
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_COMPETITOR_REGISTRATIONS,
                DefaultActions.UPDATE, this::handleCompetitorRegistration);
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_DEFINE_COURSE,
                DefaultActions.UPDATE, this::handleDefineCourse);
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_COPY, DefaultActions.UPDATE,
                this::handleCopy);
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_EDIT, DefaultActions.UPDATE,
                this::editRaceColumnOfLeaderboard);
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_UNLINK, DefaultActions.UPDATE,
                t -> unlinkRaceColumnFromTrackedRace(t.getA().getRaceColumnName(), t.getB()));
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_REFRESH_RACELOG, DefaultActions.UPDATE,
                t -> refreshRaceLog(t.getA(), t.getB(), true));
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_SET_STARTTIME,
                DefaultActions.UPDATE, this::setStartTime);
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_SET_FINISHING_AND_FINISH_TIME,
                DefaultActions.UPDATE, this::setEndTime);
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_SHOW_RACELOG,
                DefaultActions.READ, t -> showRaceLog(t.getA(), t.getB()));
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_SET_TRACKING_TIMES,
                DefaultActions.UPDATE, this::showSetTrackingTimesDialog);
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_START_TRACKING,
                DefaultActions.UPDATE, t -> startTracking(Collections.singleton(t), trackWind.getValue(),
                        correctWindDirectionForDeclination.getValue()));
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_STOP_TRACKING,
                DefaultActions.UPDATE, t -> stopTracking(Collections.singleton(t)));
        raceActionColumn.addAction(
                RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_EDIT_COMPETITOR_TO_BOAT_MAPPINGS,
                DefaultActions.UPDATE, this::showCompetitorToBoatMappings);
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_CERTIFICATE_ASSIGNMENT, READ,
                t -> assignCertificates(t));
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_SCRATCH_BOAT_SELECTION, READ,
                t -> selectScratchBoat(t));
        raceActionColumn.addAction(RaceLogTrackingEventManagementRaceImagesBarCell.ACTION_SET_IMPLIED_WIND, READ,
                t -> setImpliedWind(t));
        
        racesTable.addColumn(raceLogTrackingStateColumn, stringMessages.raceStatusColumn());
        racesTable.addColumn(trackerStateColumn, stringMessages.trackerStatus());
        racesTable.addColumn(courseStateColumn, stringMessages.courseStatus());
        racesTable.addColumn(raceActionColumn, stringMessages.actions());
    }

    private void handleCopy(RaceColumnDTOAndFleetDTOWithNameBasedEquality t) {
        final String leaderboardName = t.getC().getName();
        List<RaceColumnDTOAndFleetDTOWithNameBasedEquality> races = new ArrayList<>(
                raceColumnTable.getDataProvider().getList());
        races.remove(t);
        Distance buoyZoneRadius = getSelectedRegatta() == null ? RaceMapSettings.DEFAULT_BUOY_ZONE_RADIUS
                : getSelectedRegatta().getCalculatedBuoyZoneRadius();
        new CopyCourseAndCompetitorsDialog(sailingServiceWrite, errorReporter, stringMessages, races, t, availableLeaderboardList,
                leaderboardName, buoyZoneRadius, new DialogCallback<CourseAndCompetitorCopyOperation>() {
                    @Override
                    public void ok(CourseAndCompetitorCopyOperation operation) {
                        operation.perform(leaderboardName, t, /* onSuccessCallback */ new Runnable() {
                            @Override
                            public void run() {
                                loadAndRefreshLeaderboard(leaderboardName);
                            }
                        });
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }

    private void handleDefineCourse(RaceColumnDTOAndFleetDTOWithNameBasedEquality t) {
        final String leaderboardName = t.getC().getName();
        final String raceColumnName = t.getA().getName();
        final String fleetName = t.getB().getName();
        new RaceLogTrackingCourseDefinitionDialog(presenter, stringMessages, leaderboardName,
                raceColumnName, fleetName, new DialogCallback<RaceLogTrackingCourseDefinitionDialog.Result>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(RaceLogTrackingCourseDefinitionDialog.Result waypointPairs) {
                        sailingServiceWrite.addCourseDefinitionToRaceLog(leaderboardName, raceColumnName, fleetName,
                                waypointPairs.getWaypoints(), waypointPairs.getPriority(), new AsyncCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        sailingServiceWrite.setORCPerformanceCurveLegInfo(leaderboardName, raceColumnName, fleetName,
                                                waypointPairs.getORCLegData(), new AsyncCallback<Void>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                errorReporter.reportError(stringMessages.errorUpdatingRaceCourse(caught.getMessage()));
                                            }

                                            @Override
                                            public void onSuccess(Void result) {
                                                loadAndRefreshLeaderboard(leaderboardName);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError("Could note save course: " + caught.getMessage());
                                    }
                                });
                    }
                }).show();
    }

    private void handleCompetitorRegistration(RaceColumnDTOAndFleetDTOWithNameBasedEquality t) {
        final String leaderboardName = t.getC().getName();
        final boolean canBoatsOfCompetitorsChangePerRace = canBoatsOfCompetitorsChangePerRace();
        final String raceColumnName = t.getA().getName();
        final String fleetName = t.getB().getName();
        final boolean editable = !(doesTrackerExist(t) && getTrackingState(t) == RaceLogTrackingState.TRACKING);
        registerCompetitorsInRaceLog(getSelectedRegatta(), editable, leaderboardName,
                canBoatsOfCompetitorsChangePerRace, raceColumnName, fleetName, t);
    }

    private void registerCompetitorsInRaceLog(RegattaDTO selectedRegatta, boolean editable, String leaderboardName, 
            boolean canBoatsOfCompetitorsChangePerRace, String raceColumnName, String fleetName,
            RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnDTOAndFleetDTO) {
        RegattaDTO regatta = getSelectedRegatta();
        String boatClassName = regatta.boatClass.getName();
        RaceLogCompetitorRegistrationDialog dialog = new RaceLogCompetitorRegistrationDialog(boatClassName,
                sailingServiceWrite, userService, competitorsRefresher, boatsRefresher, stringMessages, errorReporter,
                editable, leaderboardName, canBoatsOfCompetitorsChangePerRace, raceColumnName, fleetName,
                raceColumnDTOAndFleetDTO.getA().getFleets(), new DialogCallback<Set<CompetitorDTO>>() {
                @Override
                public void ok(final Set<CompetitorDTO> registeredCompetitors) {
                    if (canBoatsOfCompetitorsChangePerRace) {
                        sailingServiceWrite.getCompetitorAndBoatRegistrationsInRaceLog(leaderboardName, raceColumnName, fleetName, new AsyncCallback<Map<CompetitorDTO, BoatDTO>>() {
                            @Override
                            public void onSuccess(Map<CompetitorDTO, BoatDTO> existingCompetitorToBoatMappings) {
                                // remove the competitors which has been removed in the first dialog (competitor selection)
                                Map<CompetitorDTO, BoatDTO> newCompetitorToBoatMappings = new HashMap<>();
                                for (CompetitorDTO competitorDTO : registeredCompetitors) {
                                    if (existingCompetitorToBoatMappings.containsKey((competitorDTO))) {
                                        BoatDTO boatDTO = existingCompetitorToBoatMappings.get(competitorDTO);
                                        newCompetitorToBoatMappings.put(competitorDTO, boatDTO);
                                    } else {
                                        newCompetitorToBoatMappings.put(competitorDTO, null);
                                    }
                                }
                                new CompetitorToBoatMappingsDialog(sailingServiceWrite, stringMessages,
                                        errorReporter, leaderboardName, newCompetitorToBoatMappings, new DialogCallback<Map<CompetitorDTO, BoatDTO>>() {
                                    @Override
                                    public void ok(final Map<CompetitorDTO, BoatDTO> competitorToBoatMappings) {
                                        sailingServiceWrite.setCompetitorRegistrationsInRaceLog(leaderboardName, raceColumnName,
                                            fleetName, competitorToBoatMappings, new AsyncCallback<Void>() {
                                            @Override
                                            public void onSuccess(Void result) {
                                            }

                                            @Override
                                            public void onFailure(Throwable caught) {
                                                errorReporter.reportError("Could not save competitor and boat registrations: " + caught.getMessage());
                                            }
                                        });
                                    }
                                    @Override
                                    public void cancel() {
                                    }
                                                    }, userService).show();
                            }
                            
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError("Could not read the competitor/boat assignments: " + caught.getMessage());
                            }
                        });
                    } else {
                        final Set<CompetitorWithBoatDTO> registeredCompetitorsWithBoat = new HashSet<>();
                        for (final CompetitorDTO competitor : registeredCompetitors) {
                            registeredCompetitorsWithBoat.add((CompetitorWithBoatDTO) competitor);
                        }
                        sailingServiceWrite.setCompetitorRegistrationsInRaceLog(leaderboardName, raceColumnName,
                            fleetName, registeredCompetitorsWithBoat, new AsyncCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError("Could not save competitor registrations: " + caught.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void cancel() {
                }
            });
        
        dialog.show();
    }
    
    @Override
    protected void addLeaderboardControls(final AccessControlledButtonPanel buttonPanel) {}

    @Override
    protected void addSelectedLeaderboardRacesControls(Panel racesPanel) {
        trackWind = new CheckBox(stringMessages.trackWind());
        trackWind.setValue(true);
        correctWindDirectionForDeclination = new CheckBox(stringMessages.declinationCheckbox());
        correctWindDirectionForDeclination.setValue(true);
        startStopTrackingButton = new ToggleButton(stringMessages.startTracking(), stringMessages.stopTracking());
        startStopTrackingButton.ensureDebugId("StartTrackingButton");
        startStopTrackingButton.setEnabled(false);
        racesPanel.add(trackWind);
        racesPanel.add(correctWindDirectionForDeclination);
        racesPanel.add(startStopTrackingButton);
        startStopTrackingButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (startStopTrackingButton.isDown()){
                    startTracking(raceColumnTableSelectionModel.getSelectedSet(), trackWind.getValue(), correctWindDirectionForDeclination.getValue());
                } else {
                    stopTracking(raceColumnTableSelectionModel.getSelectedSet());
                }
                refreshTrackingActionButtons();
            }
        });
        raceColumnTableSelectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                enableStartTrackingButtonIfAppropriateRacesSelected();
            }
        });
    }
    
    private void stopTracking(
            final Set<RaceColumnDTOAndFleetDTOWithNameBasedEquality> selectedSet) {
        final List<RegattaAndRaceIdentifier> racesToStopTracking = new ArrayList<RegattaAndRaceIdentifier>();        
        for (RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnDTOAndFleetDTOWithNameBasedEquality : selectedSet) {
            RaceDTO race = raceColumnDTOAndFleetDTOWithNameBasedEquality.getA().getRace(raceColumnDTOAndFleetDTOWithNameBasedEquality.getB());
            if (race != null && race.isTracked){
                racesToStopTracking.add(race.getRaceIdentifier());
            }   
        }
        sailingServiceWrite.stopTrackingRaces(racesToStopTracking, new MarkedAsyncCallback<Void>(
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorStoppingRaceTracking(Util.toStringOrNull(racesToStopTracking), caught.getMessage()));
                    }
        
                    @Override
                    public void onSuccess(Void result) {
                        trackedRacesListComposite.regattaRefresher.reloadAndCallFillAll();
                        for (TrackedRaceChangedListener listener : trackedRacesListComposite.raceIsTrackedRaceChangeListener) {
                            listener.racesStoppedTracking(racesToStopTracking);
                        }
                        loadAndRefreshLeaderboard(getSelectedLeaderboard().getName());
                    }
                }));
    }

    private void enableStartTrackingButtonIfAppropriateRacesSelected() {
        boolean onlyUntrackedRacesPresent = raceColumnTableSelectionModel.getSelectedSet().size() > 0;
        boolean onlyTrackedRacesPresent = raceColumnTableSelectionModel.getSelectedSet().size() > 0;
        boolean onlyRacesWithNonExistentTracker = raceColumnTableSelectionModel.getSelectedSet().size() > 0;
        for (RaceColumnDTOAndFleetDTOWithNameBasedEquality race : raceColumnTableSelectionModel.getSelectedSet()) {
            if (getTrackingState(race).isForTracking() && isFinished(race)) {
                onlyUntrackedRacesPresent = false;
                onlyTrackedRacesPresent = false;
            }
            if (!getTrackingState(race).isForTracking() || doesTrackerExist(race) || isFinished(race)) {
                onlyUntrackedRacesPresent = false;
            } else {
                onlyTrackedRacesPresent = false;
            }
            if (trackerExists(race)) {
                onlyRacesWithNonExistentTracker = false;
            }
        }
        boolean hasPermissionToChange = leaderboardSelectionModel.getSelectedSet().stream()
                .filter(new Predicate<StrippedLeaderboardDTO>() {
                    @Override
                    public boolean test(StrippedLeaderboardDTO t) {
                        return userService.hasPermission(t, DefaultActions.UPDATE);
                    }
                }).count() > 0;
        if ((!onlyTrackedRacesPresent && !onlyUntrackedRacesPresent)) {
            startStopTrackingButton.setEnabled(false);
        }
        if (onlyTrackedRacesPresent) {
            startStopTrackingButton.setDown(true);
            startStopTrackingButton.setEnabled(hasPermissionToChange);
        }
        if (onlyUntrackedRacesPresent || onlyRacesWithNonExistentTracker) {
            startStopTrackingButton.setDown(false);
            startStopTrackingButton.setEnabled(hasPermissionToChange);
        }
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
        final StrippedLeaderboardDTO selectedLeaderboard = getSelectedLeaderboard();
        regattaHasCompetitors = false;
        if (leaderboardSelectionModel.getSelectedSet().size() == 1 && selectedLeaderboard != null) {
            List<Triple<String, String, String>> raceColumnsAndFleets = new ArrayList<Triple<String, String, String>>();
            for (RaceColumnDTO raceColumn : selectedLeaderboard.getRaceList()) {
                for (FleetDTO fleet : raceColumn.getFleets()) {
                    raceColumnsAndFleets.add(new Triple<String, String, String>(selectedLeaderboard.getName(), raceColumn.getName(), fleet.getName()));
                }
            }
            sailingServiceWrite.getTrackingTimes(raceColumnsAndFleets,
                    new AsyncCallback<Map<Triple<String, String, String>, Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog>>>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error retrieving tracking times: " + caught.getMessage());
                }

                @Override
                public void onSuccess(Map<Triple<String, String, String>, Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog>> result) {
                    raceWithStartAndEndOfTrackingTime = result;
                    raceColumnTable.getDataProvider().getList().clear();
                    for (RaceColumnDTO raceColumn : selectedLeaderboard.getRaceList()) {
                        for (FleetDTO fleet : raceColumn.getFleets()) {
                            RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnDTOAndFleet2 = new RaceColumnDTOAndFleetDTOWithNameBasedEquality(raceColumn, fleet, getSelectedLeaderboard());
                            raceColumnTable.getDataProvider().getList().add(raceColumnDTOAndFleet2);
                        }
                    }
                }
            });
            selectedLeaderBoardPanel.setVisible(true);
            selectedLeaderBoardPanel.setCaptionText("Details of leaderboard '" + selectedLeaderboard.getName() + "'");
            if (!selectedLeaderboard.type.isMetaLeaderboard()) {
                trackedRacesListComposite.setRegattaFilterValue(selectedLeaderboard.regattaName);
                trackedRacesCaptionPanel.setVisible(true);
            }
            sailingServiceWrite.doesRegattaLogContainCompetitors(((StrippedLeaderboardDTO) leaderboardSelectionModel.getSelectedSet().toArray()[0]).getName(), new RegattaLogCallBack());
        } else {
            selectedLeaderBoardPanel.setVisible(false);
            trackedRacesCaptionPanel.setVisible(false);
        }
    }
    
    public Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> getTrackingTimesFor(RaceColumnDTOAndFleetDTOWithNameBasedEquality raceColumnDTOAndFleet){
        return raceWithStartAndEndOfTrackingTime.get(new Triple<String, String, String>(getSelectedLeaderboard().getName(), raceColumnDTOAndFleet.getA().getName(), raceColumnDTOAndFleet.getB().getName()));
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
        final ChooseNameDenoteEventDialog dialog = new ChooseNameDenoteEventDialog(stringMessages, leaderboard,
                new DialogCallback<String>() {
                    @Override
                    public void ok(String prefix) {
                        sailingServiceWrite.denoteForRaceLogTracking(leaderboard.getName(), prefix, new AsyncCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                loadAndRefreshLeaderboard(leaderboard.getName());
                                updateRegattaConfigDesignerModeToByMarks(leaderboard.regattaName);
                                denoteRacesWithMultipleFleetsToDefineTheirOwnCompetitors(leaderboard.getName(), leaderboard.getRaceList());
                                raceColumnTableSelectionModel.clear();
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter
                                        .reportError("Could not denote for RaceLog tracking: " + caught.getMessage());
                            }
                        });

                    }

                    @Override
                    public void cancel() {
                    }
                });
        dialog.show();
    }

    /**
     * When a race in a race column is denoted for smartphone tracking and the race column has more than one fleet then it
     * is inevitable for the race to define its own set of competitors so that competitors don't overlap for the races
     * in a single race column. This method denotes all races in the columns specified to define their own set of competitors
     * through their race logs if the race column has more than one fleet.
     */
    private void denoteRacesWithMultipleFleetsToDefineTheirOwnCompetitors(String leaderboardName, Iterable<RaceColumnDTO> raceColumns) {
        final Set<Pair<RaceColumnDTO, FleetDTO>> raceCoordinatesUsingOwnCompetitors = new HashSet<>();
        for (final RaceColumnDTO raceColumn : raceColumns) {
            if (raceColumn.getFleets().size() > 1) {
                for (final FleetDTO fleet : raceColumn.getFleets()) {
                    sailingServiceWrite.enableCompetitorRegistrationsForRace(leaderboardName, raceColumn.getName(), fleet.getName(),
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError(
                                            stringMessages.errorSettingRaceToDefineItsOwnCompetitors(leaderboardName,
                                                    raceColumn.getName(), fleet.getName(), caught.getMessage()),
                                            /* silentMode */ true);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                }
                    });
                    raceCoordinatesUsingOwnCompetitors.add(new Pair<>(raceColumn, fleet));
                }
            }
        }
    }
    
    private void updateRegattaConfigDesignerModeToByMarks(final String regattaName) {
        final RegattaDTO regatta = getRegattaByName(regattaName);
        if (regatta != null) {
            DeviceConfigurationDTO.RegattaConfigurationDTO configuration = regatta.configuration;
            if (configuration == null) {
                configuration = new DeviceConfigurationDTO.RegattaConfigurationDTO();
                configuration.defaultCourseDesignerMode = CourseDesignerMode.BY_MARKS;
                updateRegattaConfiguration(regatta, configuration);
            } else {
                if (configuration.defaultCourseDesignerMode != CourseDesignerMode.BY_MARKS) {
                    DialogBox dialogBox = createOverrideConfigurationDialog(regatta, configuration);
                    dialogBox.center();
                }
            }
        }
    }

    private DialogBox createOverrideConfigurationDialog(final RegattaDTO regatta,
            final DeviceConfigurationDTO.RegattaConfigurationDTO configuration) {
        final DialogBox dialogBox = new DialogBox(true, true);
        dialogBox.setText(stringMessages.allRacesHaveBeenDenoted());

        VerticalPanel contentPanel = new VerticalPanel();
        contentPanel.add(new HTML(new SafeHtmlBuilder()
                .appendEscapedLines(stringMessages.warningOverrideRegattaConfigurationCourseDesignerToByMarks())
                .toSafeHtml()));

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setSpacing(5);
        contentPanel.add(buttonPanel);

        Button yesButton = new Button(stringMessages.yes());
        yesButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                configuration.defaultCourseDesignerMode = CourseDesignerMode.BY_MARKS;
                updateRegattaConfiguration(regatta, configuration);
                dialogBox.hide();
            }
        });
        buttonPanel.add(yesButton);

        Button noButton = new Button(stringMessages.no(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dialogBox.hide();
            }
        });
        buttonPanel.add(noButton);

        dialogBox.setWidget(contentPanel);
        return dialogBox;
    }

    private void updateRegattaConfiguration(final RegattaDTO regatta,
            DeviceConfigurationDTO.RegattaConfigurationDTO configuration) {
        final RegattaIdentifier regattaIdentifier = new RegattaName(regatta.getName());
        sailingServiceWrite.updateRegatta(regattaIdentifier, regatta.startDate, regatta.endDate,
                Util.mapToArrayList(regatta.courseAreas, CourseAreaDTO::getId), configuration, regatta.buoyZoneRadiusInHullLengths,
                regatta.useStartTimeInference, regatta.controlTrackingFromStartAndFinishTimes,
                regatta.autoRestartTrackingUponCompetitorSetChange, regatta.registrationLinkSecret,
                regatta.competitorRegistrationType, new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(
                                stringMessages.errorUpdatingRegatta(regatta.getName(), caught.getMessage()));
                    }

                    @Override
                    public void onSuccess(Void result) {
                        Notification.notify(stringMessages.notificationRegattaConfigurationUpdatedUsingByMarks(),
                                NotificationType.SUCCESS);
                    }
                }));
    }

    private void denoteForRaceLogTracking(final RaceColumnDTOAndFleetDTOWithNameBasedEquality t) {
        sailingServiceWrite.denoteForRaceLogTracking(t.getC().getName(), t.getA().getName(), t.getB().getName(),
                new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result == true) {
                    loadAndRefreshLeaderboard(t.getC().getName());
                    denoteRacesWithMultipleFleetsToDefineTheirOwnCompetitors(t.getC().getName(), Collections.singleton(t.getA()));
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorLoadingRaceLog(caught.getMessage()));
            }
        });
    }
    
    private void removeDenotation(final RaceColumnDTOAndFleetDTOWithNameBasedEquality t) {
        sailingServiceWrite.removeDenotationForRaceLogTracking(t.getC().getName(), t.getA().getName(), t.getB().getName(),
                new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        loadAndRefreshLeaderboard(t.getC().getName());
                        trackedRacesListComposite.regattaRefresher.reloadAndCallFillAll();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Could not remove denotation: " + caught.getMessage());
                    }
                });
    }
    
    private void startTracking(Set<RaceColumnDTOAndFleetDTOWithNameBasedEquality> races, boolean trackWind,
            boolean correctWindByDeclination) {
        final StrippedLeaderboardDTO leaderboard = getSelectedLeaderboard();
        // prompt user if competitor registrations are missing for same races
        String namesOfRacesMissingRegistrations = "";
        if (!regattaHasCompetitors) {
            for (RaceColumnDTOAndFleetDTOWithNameBasedEquality race : races) {
                if (!doCompetitorRegistrationsExist(race)) {
                    namesOfRacesMissingRegistrations += race.getA().getName() + "/" + race.getB().getName() + " ";
                }
            }
        }
        if (!namesOfRacesMissingRegistrations.isEmpty()) {
            boolean proceed = Window
                    .confirm(stringMessages.competitorRegistrationsMissingProceed(namesOfRacesMissingRegistrations));
            if (!proceed) {
                return;
            }
        }
        final List<Triple<String, String, String>> leaderboardRaceColumnFleetNames = new ArrayList<>();
        for (RaceColumnDTOAndFleetDTOWithNameBasedEquality race : races) {
            final RaceColumnDTO raceColumn = race.getA();
            final FleetDTO fleet = race.getB();
            leaderboardRaceColumnFleetNames
                    .add(new Triple<>(leaderboard.getName(), raceColumn.getName(), fleet.getName()));
        }
        sailingServiceWrite.startRaceLogTracking(leaderboardRaceColumnFleetNames, trackWind, correctWindByDeclination,
                new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        loadAndRefreshLeaderboard(leaderboard.getName());
                        trackedRacesListComposite.regattaRefresher.reloadAndCallFillAll();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorStartingTracking(
                                Util.toStringOrNull(leaderboardRaceColumnFleetNames), caught.getMessage()));
                    }
                });
    }

    private void setStartTime(RaceColumnDTOAndFleetDTOWithNameBasedEquality t) {
        new SetStartTimeDialog(sailingServiceWrite, errorReporter, t.getC().getName(), t.getA().getName(),
                t.getB().getName(), stringMessages,
                new DataEntryDialog.DialogCallback<RaceLogSetStartTimeAndProcedureDTO>() {
                    @Override
                    public void ok(RaceLogSetStartTimeAndProcedureDTO editedObject) {
                        sailingServiceWrite.setStartTimeAndProcedure(editedObject, new AsyncCallback<Boolean>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Boolean result) {
                                if (!result) {
                                    Notification.notify(stringMessages.failedToSetNewStartTime(),
                                            NotificationType.ERROR);
                                } else {
                                    trackedRacesListComposite.regattaRefresher.reloadAndCallFillAll();
                                }
                            }
                        });
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }
    
    private void setEndTime(RaceColumnDTOAndFleetDTOWithNameBasedEquality t) {
        new SetFinishingAndFinishedTimeDialog(sailingServiceWrite, errorReporter, t.getC().getName(), t.getA().getName(),
                t.getB().getName(), stringMessages, new DialogCallback<RaceLogSetFinishingAndFinishTimeDTO>() {
                    @Override
                    public void ok(RaceLogSetFinishingAndFinishTimeDTO editedObject) {
                        sailingServiceWrite.setFinishingAndEndTime(editedObject,
                                new AsyncCallback<Pair<Boolean, Boolean>>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError(caught.getMessage());
                                    }

                                    @Override
                                    public void onSuccess(Pair<Boolean, Boolean> result) {
                                        if (!result.getA() || !result.getB()) {
                                            Notification.notify(stringMessages.failedToSetNewFinishingAndFinishTime(),
                                                    NotificationType.ERROR);
                                        } else {
                                            trackedRacesListComposite.regattaRefresher.reloadAndCallFillAll();
                                        }
                                    }
                                });
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }
    
    private void refreshTrackingActionButtons() {
        leaderboardSelectionChanged();
    }

    private void showSetTrackingTimesDialog(RaceColumnDTOAndFleetDTOWithNameBasedEquality t) {
        new SetTrackingTimesDialog(sailingServiceWrite, errorReporter, t.getC().getName(), t.getA().getName(),
                t.getB().getName(), stringMessages, new DataEntryDialog.DialogCallback<RaceLogSetTrackingTimesDTO>() {
                    @Override
                    public void ok(RaceLogSetTrackingTimesDTO editedObject) {
                        sailingServiceWrite.setTrackingTimes(editedObject, new AsyncCallback<Void>(){
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError("Error while setting tracking times: " + caught.getMessage());
                                refreshTrackingActionButtons();
                            }

                            @Override
                            public void onSuccess(Void result) {
                                refreshTrackingActionButtons();
                            }
                        });
                    }

                    @Override
                    public void cancel() {
                        //toggle buttons in dialog lead to a change although dialog is canceled --> reload tracking times
                        refreshTrackingActionButtons();
                    }
                }).show();
    }

    private void showCompetitorToBoatMappings(RaceColumnDTOAndFleetDTOWithNameBasedEquality t) {
        final String selectedLeaderboardName = t.getC().getName();
        final String raceColumnName = t.getA().getName();
        final String fleetName = t.getB().getName();
        final String raceName = LeaderboardNameConstants.DEFAULT_FLEET_NAME.equals(fleetName) ? raceColumnName : raceColumnName + ", " + fleetName;
        ShowCompetitorToBoatMappingsDialog dialog = new ShowCompetitorToBoatMappingsDialog(sailingServiceWrite, 
                stringMessages, errorReporter, selectedLeaderboardName, raceColumnName, fleetName, 
                raceName, userService);
        dialog.center();
    }

    private String getLocaleInfo() {
        return LocaleInfo.getCurrentLocale().getLocaleName();
    }
    
    private void openChooseEventDialogAndSendMails(final String leaderboardName) {
        new InviteBuoyTenderDialog(stringMessages, sailingServiceWrite, leaderboardName, errorReporter, new DialogCallback<Triple<EventDTO, String, String>>() {
            @Override
            public void ok(Triple<EventDTO, String, String> result) {
                sailingServiceWrite.inviteBuoyTenderViaEmail(result.getB(), result.getA(), leaderboardName, result.getC(),
                        null,
                        stringMessages.playStoreBuoyPingerApp(),
                        getLocaleInfo(), new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Notification.notify(stringMessages.sendingMailsFailed() + caught.getMessage(), NotificationType.ERROR);
                            }

                            @Override
                            public void onSuccess(Void result) {
                                Notification.notify(stringMessages.sendingMailsSuccessful(), NotificationType.SUCCESS);
                            }
                        });
            }

            @Override
            public void cancel() {
                
            }
        }).show();
    }

    private void handleCompetitorRegistration(StrippedLeaderboardDTO t) {
        RegattaDTO regatta = getSelectedRegatta();
        String boatClassName = regatta.boatClass.getName();
        new RegattaLogCompetitorRegistrationDialog(boatClassName, sailingServiceWrite, userService,
                competitorsRefresher, boatsRefresher, stringMessages, errorReporter, /* editable */true, t.getName(),
                t.canBoatsOfCompetitorsChangePerRace, new DialogCallback<Set<CompetitorDTO>>() {
                    @Override
                    public void ok(Set<CompetitorDTO> registeredCompetitors) {
                        sailingServiceWrite.setCompetitorRegistrationsInRegattaLog(t.getName(), registeredCompetitors,
                                new AsyncCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        // pass
                                    }

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError(
                                                "Could not save competitor registrations: " + caught.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }

    private void handleBoatRegistration(StrippedLeaderboardDTO t) {
        if (t.canBoatsOfCompetitorsChangePerRace) {
            RegattaDTO regatta = getSelectedRegatta();
            String boatClassName = regatta.boatClass.getName();

            new RegattaLogBoatRegistrationDialog(boatClassName, sailingServiceWrite, userService, boatsRefresher,
                    competitorsRefresher, stringMessages, errorReporter, /* editable */true, t.getName(),
                    t.canBoatsOfCompetitorsChangePerRace, new DialogCallback<Set<BoatDTO>>() {
                        @Override
                        public void ok(Set<BoatDTO> registeredBoats) {
                            sailingServiceWrite.setBoatRegistrationsInRegattaLog(t.getName(), registeredBoats,
                                    new AsyncCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            // pass
                                        }

                                        @Override
                                        public void onFailure(Throwable caught) {
                                            errorReporter.reportError(
                                                    "Could not save boat registrations: " + caught.getMessage());
                                        }
                                    });
                        }

                        @Override
                        public void cancel() {
                        }
                    }).show();
        } else {
            Notification.notify(stringMessages.canNotRegisterBoats(), NotificationType.ERROR);
        }
    }

    private void handleDeviceMappings(StrippedLeaderboardDTO t) {
        sailingServiceWrite.getSecretForRegattaByName(t.getName(), new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                // if this happens, the user did apparently not have sufficient rights.
                Notification.notify(stringMessages.youDontHaveRequiredPermission(), NotificationType.ERROR);
            }

            @Override
            public void onSuccess(String secret) {
                new RegattaLogTrackingDeviceMappingsDialog(sailingServiceWrite, userService, stringMessages, errorReporter,
                        t.getName(), secret, new DialogCallback<Void>() {
                            @Override
                            public void ok(Void editedObject) {
                            }

                            @Override
                            public void cancel() {
                            }
                        }).show();
            }
        });
    }
}
