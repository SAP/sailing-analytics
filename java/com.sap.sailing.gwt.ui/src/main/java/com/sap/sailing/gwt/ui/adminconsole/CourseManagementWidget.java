package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SetSelectionModel;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataEvent;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.gwt.ui.adminconsole.WaypointCreationDialog.DefaultPassingInstructionProvider;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.ControlPointDTO;
import com.sap.sailing.gwt.ui.shared.GateDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.RaceCourseDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.WaypointDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.RefreshableSingleSelectionModel;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.Validator;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;

public abstract class CourseManagementWidget implements IsWidget {
    protected final MarkTableWrapper<RefreshableMultiSelectionModel<MarkDTO>> marks;
    protected final ControlPointTableWrapper<RefreshableSingleSelectionModel<ControlPointDTO>> multiMarkControlPoints;
    protected final WaypointTableWrapper<RefreshableSingleSelectionModel<WaypointDTO>> waypoints;
    protected final Grid mainPanel;
    protected final SailingServiceWriteAsync sailingServiceWrite;
    protected final ErrorReporter errorReporter;
    protected final StringMessages stringMessages;
    protected final HorizontalPanel waypointsBtnsPanel;
    protected final HorizontalPanel controlPointsBtnsPanel;
    protected final HorizontalPanel marksBtnsPanel;
    protected final Button insertWaypointBefore;
    protected final Button insertWaypointAfter;
    protected final Button addControlPoint;
    private PassingInstruction lastSingleMarkPassingInstruction = PassingInstruction.Port; // the usual default
    protected final AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
    private final UserService userService;
    private SecuredDTO securedDtoForWaypointsPermissionCheck;
    
    /**
     * ORC polar curve leg info for the leg ending at the key waypoint
     */
    private final Map<WaypointDTO, ORCPerformanceCurveLegImpl> orcPerformanceCurveLegInfo;
    
    /**
     * Abstracts from how the leg geometry for a single leg is obtained; implementations may, e.g., use
     * different ways to identify the race from which to fetch the data.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    public static interface LegGeometrySupplier {
        /**
         * The lengths of the {@code zeroBasedLegNumbers} and {@code orcPerformanceCurveLegTypes} arrays must be equal,
         * and the indices bind the values together. The leg geometries will be determined based on the leg type
         * selected. If the leg type is {@code null} for a leg, the server-defined leg type will be determined and
         * applied, so that windward / leeward legs will have their length computed by projecting onto the wind
         * direction, and reaching legs will be judged based on rhumb line distance; the same applies for
         * {@link ORCPerformanceCurveLegTypes#WINDWARD_LEEWARD},
         * {@link ORCPerformanceCurveLegTypes#WINDWARD_LEEWARD_REAL_LIVE} and
         * {@link ORCPerformanceCurveLegTypes#TWA}-typed legs. For all other leg types, rhumb-line distance is to be
         * computed.
         */
        void getLegGeometry(int[] zeroBasedLegNumbers, ORCPerformanceCurveLegTypes[] orcPerformanceCurveLegTypes, AsyncCallback<ORCPerformanceCurveLegImpl[]> callback);
    }
    
    public static class SingleLegValidator implements Validator<ORCPerformanceCurveLegImpl> {
        private final StringMessages stringMessages;
        
        public SingleLegValidator(StringMessages stringMessages) {
            this.stringMessages = stringMessages;
        }

        @Override
        public String getErrorMessage(ORCPerformanceCurveLegImpl valueToValidate) {
               final String result;
               if (valueToValidate == null) {
                   result = null; // empty is allowed
               } else {
                   if (valueToValidate.getLength() == null) {
                       result = stringMessages.pleaseEnterADistance();
                   } else if (valueToValidate.getType() == ORCPerformanceCurveLegTypes.TWA && valueToValidate.getTwa() == null) {
                       result = stringMessages.pleaseEnterATwa();
                   } else {
                       result = null;
                   }
               }
               return result;
        }
   }
    
    @Override
    public Widget asWidget() {
        return mainPanel;
    }
    
    /**
     * @param showOrcPcsLegEditActions
     *            Depending on the ranking metric it may or may not make sense to show the user the actions to maintain
     *            ORC PCS leg data. By default, these actions are enabled, particularly to cover the case where this
     *            widget is used without an existing {@code TrackedRace} and only with a race log.
     */
    public CourseManagementWidget(final Presenter presenter,
            final StringMessages stringMessages, final Supplier<Boolean> showOrcPcsLegEditActions) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.errorReporter = presenter.getErrorReporter();
        this.stringMessages = stringMessages;
        this.userService = presenter.getUserService();
        this.orcPerformanceCurveLegInfo = new HashMap<>();
        mainPanel = new Grid(2, 3);
        mainPanel.setCellPadding(5);
        mainPanel.getRowFormatter().setVerticalAlign(0, HasVerticalAlignment.ALIGN_TOP);
        waypoints = new WaypointTableWrapper<RefreshableSingleSelectionModel<WaypointDTO>>(
                /* multiSelection */ false, sailingServiceWrite, stringMessages, errorReporter);
        multiMarkControlPoints = new ControlPointTableWrapper<RefreshableSingleSelectionModel<ControlPointDTO>>(
                /* multiSelection */ false, sailingServiceWrite, stringMessages, errorReporter);
        marks = new MarkTableWrapper<RefreshableMultiSelectionModel<MarkDTO>>(
                /* multiSelection */ true, sailingServiceWrite, stringMessages, errorReporter);
        marks.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                markSelectionChanged();
            }
        });
        CaptionPanel waypointsPanel = new CaptionPanel(stringMessages.waypoints());
        CaptionPanel controlPointsPanel = new CaptionPanel(stringMessages.twoMarkControlPoint());
        CaptionPanel marksPanel = new CaptionPanel(stringMessages.mark());
        waypointsPanel.add(waypoints);
        controlPointsPanel.add(multiMarkControlPoints);
        marksPanel.add(marks);
        mainPanel.setWidget(0, 0, waypointsPanel);
        mainPanel.setWidget(0, 1, controlPointsPanel);
        mainPanel.setWidget(0, 2, marksPanel);
        waypointsBtnsPanel = new HorizontalPanel();
        controlPointsBtnsPanel = new HorizontalPanel();
        marksBtnsPanel = new HorizontalPanel();
        mainPanel.setWidget(1, 0, waypointsBtnsPanel);
        mainPanel.setWidget(1, 1, controlPointsBtnsPanel);
        mainPanel.setWidget(1, 2, marksBtnsPanel);
        final AccessControlledActionsColumn<WaypointDTO, WaypointImagesBarCell> waypointsActionColumn = create(
                new WaypointImagesBarCell(stringMessages, waypoints.getDataProvider(), showOrcPcsLegEditActions), userService,
                s -> securedDtoForWaypointsPermissionCheck);
        // update permission for tracked race is required for deleting waypoints...
        waypointsActionColumn.addAction(DefaultActions.DELETE.name(), DefaultActions.UPDATE,
                waypoint -> removeWaypoint(waypoint));
        // ...as well as for setting any ORC PCS-related leg details:
        waypointsActionColumn.addAction(WaypointImagesBarCell.ACTION_ORC_PCS_DEFINE_LEG, DefaultActions.UPDATE,
                waypoint -> createOrcPcsLegEventForLegEndingAt(waypoint));
        waypointsActionColumn.addAction(WaypointImagesBarCell.ACTION_ORC_PCS_DEFINE_ALL_LEGS, DefaultActions.UPDATE,
                waypoint -> createOrcPcsLegEventsForAllLegs());
        waypoints.addColumn(waypointsActionColumn);
        waypoints.getSelectionModel().addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                WaypointDTO waypoint = waypoints.getSelectionModel().getSelectedObject();
                if (waypoint != null) {
                    selectControlPoints(waypoint);
                    selectMarks(waypoint.controlPoint.getMarks());
                }
                updateWaypointButtons();
            }
        });
        multiMarkControlPoints.getSelectionModel().addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (multiMarkControlPoints.getSelectionModel().getSelectedSet().size() > 0) {
                    ControlPointDTO first = multiMarkControlPoints.getSelectionModel().getSelectedSet().iterator().next();
                    selectMarks(first.getMarks());
                }
            }
        });
        insertWaypointBefore = new Button(stringMessages.insertWaypointBeforeSelected());
        insertWaypointBefore.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                insertWaypoint(/* before */ true);
            }
        });
        insertWaypointBefore.setEnabled(false);
        waypointsBtnsPanel.add(insertWaypointBefore);
        insertWaypointAfter = new Button(stringMessages.insertWaypointAfterSelected());
        insertWaypointAfter.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                insertWaypoint(/* before */ false);
            }
        });
        insertWaypointAfter.setEnabled(false);
        waypointsBtnsPanel.add(insertWaypointAfter);
        addControlPoint = new Button(stringMessages.add(stringMessages.twoMarkControlPoint()));
        addControlPoint.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addMultiMarkControlPoint();
            }
        });
        controlPointsBtnsPanel.add(addControlPoint);
    }
    
    abstract protected LegGeometrySupplier getLegGeometrySupplier();
    
    protected Map<Integer, ORCPerformanceCurveLegImpl> getORCPerformanceCurveLegInfoByOneBasedWaypointIndex() {
        final List<WaypointDTO> waypointList = waypoints.getDataProvider().getList();
        final Map<Integer, ORCPerformanceCurveLegImpl> result = new HashMap<>();
        for (final Entry<WaypointDTO, ORCPerformanceCurveLegImpl> e : orcPerformanceCurveLegInfo.entrySet()) {
            result.put(waypointList.indexOf(e.getKey()), e.getValue());
        }
        return result;
    }
    
    /**
     * Shows a dialog that allows the user to enter the details for a {@link RaceLogORCLegDataEvent}
     * for the leg ending at {@code waypoint}.
     */
    private void createOrcPcsLegEventForLegEndingAt(WaypointDTO waypoint) {
        new ORCPerformanceCurveLegDialog(stringMessages, waypoint, waypoints.getDataProvider(),
                orcPerformanceCurveLegInfo.get(waypoint), getLegGeometrySupplier(),
                new SingleLegValidator(stringMessages), new DialogCallback<ORCPerformanceCurveLegImpl>() {
            @Override
            public void ok(ORCPerformanceCurveLegImpl legInfoForWaypoint) {
                orcPerformanceCurveLegInfo.put(waypoint, legInfoForWaypoint);
            }
            
            @Override
            public void cancel() {
            }
        }).show();
    }
    
    /**
     * Shows an editable dialog that is filled with the current ORC PCS definitions for all legs. Other than the
     * {@link ORCPerformanceCurveLegDialog} that is good only for a single leg, the dialog presented by this method
     * allows the user to
     * <ul>
     * <li>see the total distance of the course by adding up all leg distances</li>
     * <li>set a common leg type, such as {@link ORCPerformanceCurveLegTypes#CIRCULAR_RANDOM}, for all legs at once</li>
     * <li>set a total course distance and break it down proportionally to the legs, based on their current length</li>
     * </ul>
     */
    private void createOrcPcsLegEventsForAllLegs() {
        new ORCPerformanceCurveAllLegsDialog(stringMessages, waypoints.getDataProvider(), getExplicitOrcPerformanceCurveLegInfos(),
                getLegGeometrySupplier(), new Validator<ORCPerformanceCurveLegImpl[]>() {
                    @Override
                    public String getErrorMessage(ORCPerformanceCurveLegImpl[] valueToValidate) {
                        final SingleLegValidator singleLegValidator = new SingleLegValidator(stringMessages);
                        for (final ORCPerformanceCurveLegImpl legToValidate : valueToValidate) {
                            final String errorMessage = singleLegValidator.getErrorMessage(legToValidate);
                            if (errorMessage != null) {
                                return errorMessage;
                            }
                        }
                        return null;
                    }
                    
                },
                new DialogCallback<ORCPerformanceCurveLegImpl[]>() {
                    @Override
                    public void ok(ORCPerformanceCurveLegImpl[] legInfos) {
                        for (int i=0; i<legInfos.length; i++) {
                            orcPerformanceCurveLegInfo.put(waypoints.getDataProvider().getList().get(i+1), legInfos[i]);
                        }
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }

    private ORCPerformanceCurveLegImpl[] getExplicitOrcPerformanceCurveLegInfos() {
        final ORCPerformanceCurveLegImpl[] result = new ORCPerformanceCurveLegImpl[waypoints.getDataProvider().getList().size()-1];
        for (int i=0; i<result.length; i++) {
            result[i] = orcPerformanceCurveLegInfo.get(waypoints.getDataProvider().getList().get(i+1));
        }
        return result;
    }

    protected void markSelectionChanged() {
    }

    private void removeWaypoint(WaypointDTO waypoint) {
        waypoints.getDataProvider().getList().remove(waypoint);
    }
    
    private void selectControlPoints(WaypointDTO waypoint) {
        for (ControlPointDTO controlPoint : multiMarkControlPoints.getDataProvider().getList()) {
            multiMarkControlPoints.getSelectionModel().setSelected(
                    controlPoint, waypoint.controlPoint == controlPoint);
        }
    }
    
    private void selectMarks(Iterable<MarkDTO> newMarks) {
        marks.getSelectionModel().clear();
        for (MarkDTO toSelect : newMarks) {
            marks.getSelectionModel().setSelected(toSelect, true);
        }
    }

    protected void save(){};
    
    private <T> T getFirstSelected(SetSelectionModel<T> selectionModel) {
        if (selectionModel.getSelectedSet().isEmpty()) {
            return null;
        }
        return selectionModel.getSelectedSet().iterator().next();
    }
    
    private <T> void insert(TableWrapper<T, ? extends SetSelectionModel<T>> tableWrapper, T toInsert, boolean beforeSelection) {
        int index = getInsertIndex(tableWrapper, beforeSelection);
        if (index != -1) {
            tableWrapper.getDataProvider().getList().add(index, toInsert);
        }
    }

    private <T> int getInsertIndex(TableWrapper<T, ? extends SetSelectionModel<T>> tableWrapper,
            boolean beforeSelection) {
        T selected = getFirstSelected(tableWrapper.getSelectionModel());
        int index = -1;
        if (tableWrapper.getDataProvider().getList().isEmpty()) {
            index = 0;
        } else if (selected != null) {
            index = tableWrapper.getDataProvider().getList().indexOf(selected) + (beforeSelection?0:1);
        }
        return index;
    }

    private void addMultiMarkControlPoint() {
        new GateCreationDialog(sailingServiceWrite, errorReporter, stringMessages, tableRes,
                marks.getDataProvider().getList(), new DataEntryDialog.DialogCallback<GateDTO>() {
            @Override
            public void cancel() {}

            @Override
            public void ok(GateDTO result) {
                multiMarkControlPoints.getDataProvider().getList().add(result);
            }
        }).show();
    }
    
    private void insertWaypoint(final boolean beforeSelection) {
        List<ControlPointDTO> allControlPoints = new ArrayList<>();
        allControlPoints.addAll(multiMarkControlPoints.getDataProvider().getList());
        allControlPoints.addAll(marks.getDataProvider().getList());
        final int insertIndex = getInsertIndex(waypoints, beforeSelection);
        new WaypointCreationDialog(sailingServiceWrite, errorReporter, stringMessages, tableRes, allControlPoints,
                new DefaultPassingInstructionProvider() {
                    @Override
                    public PassingInstruction getDefaultPassingInstruction(int numberOfMarksInControlPoint, String controlPointIdAsString) {
                        final PassingInstruction result;
                        if (numberOfMarksInControlPoint == 2) {
                            if (insertIndex == 0 ||
                                    (!waypoints.getDataProvider().getList().isEmpty() && Util.equalsWithNull(controlPointIdAsString, waypoints.getDataProvider().getList().get(0).controlPoint.getIdAsString())
                                    && insertIndex == waypoints.getDataProvider().getList().size())) {
                                // two-mark control point used as first waypoint or
                                // same control point as for the first waypoint now used as the last one;
                                // suggest Line:
                                result = PassingInstruction.Line;
                            } else {
                                result = PassingInstruction.Gate;
                            }
                        } else if (numberOfMarksInControlPoint == 1) {
                            result = lastSingleMarkPassingInstruction;
                        } else {
                            result = null;
                        }
                        return result;
                    }
                }, new DataEntryDialog.DialogCallback<WaypointDTO>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(WaypointDTO result) {
                        insert(waypoints, result, beforeSelection);
                        waypoints.getSelectionModel().setSelected(result, true);
                        if (Util.size(result.controlPoint.getMarks()) == 1) {
                            lastSingleMarkPassingInstruction = result.passingInstructions;
                        }
                    }
                }).show();
    }

    public abstract void refresh();
    
    protected void updateWaypointsAndControlPoints(RaceCourseDTO raceCourseDTO, String leaderboardName) {
        this.sailingServiceWrite.getLeaderboardWithSecurity(leaderboardName,
                new AsyncCallback<StrippedLeaderboardDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Notification.notify(caught.getMessage(), NotificationType.ERROR);
                    }

                    @Override
                    public void onSuccess(StrippedLeaderboardDTO result) {
                        updateWaypointsAndControlPointsForSecuredObject(raceCourseDTO, result);
                    }
                });
    }

    protected void updateWaypointsAndControlPoints(RaceCourseDTO raceCourseDTO, RaceDTO raceDTO) {
        updateWaypointsAndControlPointsForSecuredObject(raceCourseDTO, raceDTO);
    }

    private void updateWaypointsAndControlPointsForSecuredObject(RaceCourseDTO raceCourseDTO, SecuredDTO securedDTO) {
        securedDtoForWaypointsPermissionCheck = securedDTO;
        waypoints.getDataProvider().getList().clear();
        multiMarkControlPoints.getDataProvider().getList().clear();
        waypoints.getDataProvider().getList().addAll(raceCourseDTO.waypoints);
        Map<String, ControlPointDTO> noDuplicateCPs = new HashMap<>();
        for (ControlPointDTO controlPoint : raceCourseDTO.getControlPoints()) {
            if (controlPoint instanceof GateDTO) {
                noDuplicateCPs.put(controlPoint.getIdAsString(), controlPoint);
            }
        }
        multiMarkControlPoints.getDataProvider().getList().addAll(noDuplicateCPs.values());
        updateWaypointButtons();
        final boolean hasUpdatePermission = userService.hasPermission(securedDTO, DefaultActions.UPDATE);
        insertWaypointAfter.setVisible(hasUpdatePermission);
        insertWaypointBefore.setVisible(hasUpdatePermission);
        addControlPoint.setVisible(hasUpdatePermission);
    }
    
    protected List<com.sap.sse.common.Util.Pair<ControlPointDTO, PassingInstruction>> createWaypointPairs() {
        List<com.sap.sse.common.Util.Pair<ControlPointDTO, PassingInstruction>> result = new ArrayList<>();
        for (WaypointDTO waypoint : waypoints.getDataProvider().getList()) {
            result.add(new com.sap.sse.common.Util.Pair<>(waypoint.controlPoint, waypoint.passingInstructions));
        }
        return result;
    }
    
    protected void updateWaypointButtons() {
        if (waypoints.getDataProvider().getList().isEmpty() ||
                waypoints.getSelectionModel().getSelectedObject() != null) {
            insertWaypointAfter.setEnabled(true);
            insertWaypointBefore.setEnabled(true);
        } else {
            insertWaypointBefore.setEnabled(false);
            insertWaypointAfter.setEnabled(false);
        }
    }

    /**
     * Assumes that {@link #waypoints} has a list of {@link WaypointDTO}s that are consistent with the numbering scheme
     * of {@code oneBasedLegIndexAndFixedLegData}. The {@link #orcPerformanceCurveLegInfo} will be cleared and then
     * filled with the {@link ORCPerformanceCurveLegImpl} objects, keyed by those {@link WaypointDTO}s from
     * {@link #waypoints} that correspond with {@code oneBasedLegIndexAndFixedLegData}.
     */
    protected void refreshORCPerformanceCurveLegs(Map<Integer, ORCPerformanceCurveLegImpl> oneBasedLegIndexAndFixedLegData) {
        orcPerformanceCurveLegInfo.clear();
        final List<WaypointDTO> waypointList = waypoints.getDataProvider().getList();
        for (final Entry<Integer, ORCPerformanceCurveLegImpl> e : oneBasedLegIndexAndFixedLegData.entrySet()) {
            orcPerformanceCurveLegInfo.put(waypointList.get(e.getKey()), e.getValue());
        }
    }
}
