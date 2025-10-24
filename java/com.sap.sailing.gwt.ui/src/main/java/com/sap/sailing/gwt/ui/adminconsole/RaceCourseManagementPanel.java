package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Date;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RaceCourseDTO;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

/**
 * A panel that has a race selection (inherited from {@link AbstractRaceManagementPanel}) and which adds a table
 * for a selected race showing the race's waypoints together with the number of mark passings already received for that
 * waypoint. Also, the control can be used to send course updates into the tracked race, mostly to simulate these types
 * of events. Conceivably, this may in the future also become a way to set up and edit courses for a tracked race.
 * 
 * @author Frank Mittag (C5163874), Axel Uhl (D043530)
 */
public class RaceCourseManagementPanel extends AbstractRaceManagementPanel {
    private final CourseManagementWidget courseManagementWidget;

    public RaceCourseManagementPanel(final Presenter presenter, final StringMessages stringMessages) {
        super(presenter, /* actionButtonsEnabled */ false, stringMessages);
        courseManagementWidget = new CourseManagementWidget(presenter, stringMessages,
                () -> selectedRaceHasOrcPcsRankingMetric()) {
            @Override
            protected void save() {
                sailingServiceWrite.updateRaceCourse(singleSelectedRace, createWaypointPairs(), new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorUpdatingRaceCourse(caught.getMessage()));
                    }

                    @Override
                    public void onSuccess(Void result) {
                        Notification.notify(stringMessages.successfullyUpdatedCourse(), NotificationType.INFO);
                        sailingServiceWrite.setORCPerformanceCurveLegInfo(singleSelectedRace, getORCPerformanceCurveLegInfoByOneBasedWaypointIndex(), new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(stringMessages.errorUpdatingRaceCourse(caught.getMessage()));
                            }

                            @Override
                            public void onSuccess(Void result) {
                                refreshSelectedRaceData();
                            }
                        });
                    }
                });
            }
            
            @Override
            protected LegGeometrySupplier getLegGeometrySupplier() {
                return (zeroBasedLegIndices, legTypes, callback)->
                    sailingServiceWrite.getLegGeometry(singleSelectedRace, zeroBasedLegIndices, legTypes, callback);
            }

            @Override
            public void refresh() {
                if (singleSelectedRace != null && selectedRaceDTO != null) {
                    mainPanel.setVisible(true);
                    // TODO bug 1351: never use System.currentTimeMillis() on the client when trying to compare anything with "server time"; this one is not so urgent as it is reached only in the AdminConsole and we expect administrators to have proper client-side time settings
                    sailingServiceWrite.getRaceCourse(singleSelectedRace, new Date(), new AsyncCallback<RaceCourseDTO>() {
                        @Override
                        public void onSuccess(RaceCourseDTO raceCourseDTO) {
                            updateWaypointsAndControlPoints(raceCourseDTO, selectedRaceDTO);
                            refreshORCPerformanceCurveLegs();
                            marks.refresh(raceCourseDTO.getMarks());
                        }
            
                        @Override
                        public void onFailure(Throwable caught) {
                            RaceCourseManagementPanel.this.errorReporter.reportError(
                                    RaceCourseManagementPanel.this.stringMessages.errorTryingToObtainTheMarksOfTheRace(
                                    caught.getMessage()));
                        }
                    });
                } else {
                    mainPanel.setVisible(false);
                }
            }
            
            private void refreshORCPerformanceCurveLegs() {
                if (singleSelectedRace != null) {
                    sailingServiceWrite.getORCPerformanceCurveLegInfo(singleSelectedRace,
                            new AsyncCallback<Map<Integer, ORCPerformanceCurveLegImpl>>() {
                                @Override
                                public void onSuccess(Map<Integer, ORCPerformanceCurveLegImpl> result) {
                                    refreshORCPerformanceCurveLegs(result);
                                }
    
                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError("Could not load ORC Polar Curve leg information: " + caught.getMessage());
                                }
                            });
                }
            }
        };
        FlowPanel courseManagementPanel = new FlowPanel();
        courseManagementPanel.add(courseManagementWidget);
        HorizontalPanel buttonsPanel = new HorizontalPanel();
        Button refreshBtn = new Button(stringMessages.refresh());
        refreshBtn.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    courseManagementWidget.refresh();
                }
        });
        buttonsPanel.add(refreshBtn);
        Button saveBtn = new Button(stringMessages.save());
        saveBtn.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    courseManagementWidget.save();
                }
            });
        trackedRacesListComposite.getSelectionModel().addSelectionChangeHandler(h -> {
            saveBtn.setVisible(presenter.getUserService().hasPermission(selectedRaceDTO, DefaultActions.UPDATE));
        });
        buttonsPanel.add(saveBtn);
        this.selectedRaceContentPanel.add(courseManagementWidget);
        this.selectedRaceContentPanel.add(buttonsPanel);
    }

    private boolean selectedRaceHasOrcPcsRankingMetric() {
        final RankingMetrics rankingMetricType = selectedRaceDTO == null ? null : selectedRaceDTO.getRankingMetricType();
        return rankingMetricType == RankingMetrics.ORC_PERFORMANCE_CURVE ||
                rankingMetricType == RankingMetrics.ORC_PERFORMANCE_CURVE_BY_IMPLIED_WIND ||
                rankingMetricType == RankingMetrics.ORC_PERFORMANCE_CURVE_LEADER_FOR_BASELINE;
    }

    @Override
    void refreshSelectedRaceData() {
        courseManagementWidget.refresh();
    }
}
