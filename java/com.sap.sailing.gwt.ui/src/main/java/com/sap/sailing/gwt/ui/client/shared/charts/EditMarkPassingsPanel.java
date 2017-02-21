package com.sap.sailing.gwt.ui.client.shared.charts;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.charts.RaceIdentifierToLeaderboardRaceColumnAndFleetMapper.LeaderboardNameRaceColumnNameAndFleetName;
import com.sap.sailing.gwt.ui.shared.RaceCourseDTO;
import com.sap.sailing.gwt.ui.shared.WaypointDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.common.settings.AbstractSettings;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.shared.components.AbstractCompositeComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.perspective.ComponentContext;

public class EditMarkPassingsPanel extends AbstractCompositeComponent<AbstractSettings> implements CompetitorSelectionChangeListener {
    private static class AnchorCell extends AbstractCell<SafeHtml> {
        @Override
        public void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml safeHtml, SafeHtmlBuilder sb) {
            sb.append(safeHtml);
        }
    }

    private final SailingServiceAsync sailingService;
    private RegattaAndRaceIdentifier raceIdentifier;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final CompetitorSelectionProvider competitorSelectionModel;
    private final RaceIdentifierToLeaderboardRaceColumnAndFleetMapper raceIdentifierToLeaderboardRaceColumnAndFleetMapper;

    private CompetitorDTO competitor;
    
    /**
     * The fixed mark passings; keys are zero-based waypoint indexes
     */
    private Map<Integer, Date> currentCompetitorEdits = new HashMap<>();
    
    private Integer zeroBasedIndexOfFirstSuppressedWaypoint;

    /**
     * The waypoints table uses pairs of zero-based waypoint numbers and current passing times.
     */
    private final CellTable<Util.Pair<Integer, Date>> wayPointSelectionTable;
    private final ListDataProvider<Util.Pair<Integer, Date>> waypointList;
    private final SingleSelectionModel<Util.Pair<Integer, Date>> waypointSelectionModel;
    private List<WaypointDTO> currentWaypoints;

    private final Button setTimeAsMarkPassingsButton;
    private final Button removeFixedMarkPassingsButton;
    private final Button suppressPassingsButton;
    private final Button removeSuppressedPassingButton;
    private Label selectCompetitorLabel = new Label();

    public EditMarkPassingsPanel(Component<?> parent, ComponentContext<?> context,
            final SailingServiceAsync sailingService,
            final RegattaAndRaceIdentifier raceIdentifier,
            final StringMessages stringMessages, final CompetitorSelectionProvider competitorSelectionModel,
            final ErrorReporter errorReporter, final Timer timer) {
        super(parent, context);
        this.raceIdentifierToLeaderboardRaceColumnAndFleetMapper = new RaceIdentifierToLeaderboardRaceColumnAndFleetMapper();
        this.sailingService = sailingService;
        this.raceIdentifier = raceIdentifier;
        this.errorReporter = errorReporter;
        this.competitorSelectionModel = competitorSelectionModel;
        this.stringMessages = stringMessages;
        competitorSelectionModel.addCompetitorSelectionChangeListener(this);

        // Waypoint list
        currentWaypoints = new ArrayList<>();
        waypointList = new ListDataProvider<>();
        waypointSelectionModel = new SingleSelectionModel<Util.Pair<Integer, Date>>();
        waypointSelectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Pair<Integer, Date> selectedObject = waypointSelectionModel.getSelectedObject();
                if (selectedObject != null) {
                    Date timePoint = selectedObject.getB();
                    if (timePoint != null) {
                        timer.setTime(timePoint.getTime());
                    }
                }
                enableButtons();
            }
        });
        wayPointSelectionTable = new BaseCelltable<Util.Pair<Integer, Date>>();
        wayPointSelectionTable.addColumn(new Column<Util.Pair<Integer, Date>, SafeHtml>(new AnchorCell()) {
            @Override
            public SafeHtml getValue(final Util.Pair<Integer, Date> object) {
                return new SafeHtml() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String asString() {
                        return currentWaypoints.get(object.getA()).getName();
                    }
                };
            }
        }, stringMessages.waypoint());
        final DateTimeFormat timeFormat = DateTimeFormat.getFormat("HH:mm:ss");
        wayPointSelectionTable.addColumn(new Column<Util.Pair<Integer, Date>, SafeHtml>(new AnchorCell()) {
            @Override
            public SafeHtml getValue(final Pair<Integer, Date> object) {
                final Date date;
                date = object.getB();
                return new SafeHtml() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String asString() {
                        // TODO this is really unclean (Problem: no calendar)
                        String string;
                        if (date != null) {
                            string = timeFormat.format(date);
                            if (currentCompetitorEdits.containsKey(object.getA())) {
                                string += " "+stringMessages.fixedMarkPassing();
                            }
                        } else if (zeroBasedIndexOfFirstSuppressedWaypoint != null
                                && !(object.getA() < zeroBasedIndexOfFirstSuppressedWaypoint)) {
                            string = " "+stringMessages.suppressedMarkPassing();
                        } else {
                            string = "";
                        }
                        return string;
                    }
                };
            }
        }, stringMessages.markPassing());

        waypointList.addDataDisplay(wayPointSelectionTable);
        wayPointSelectionTable.setSelectionModel(waypointSelectionModel);

        // Buttons for fixing
        removeFixedMarkPassingsButton = new Button(stringMessages.removeFixedPassings());
        removeFixedMarkPassingsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final LeaderboardNameRaceColumnNameAndFleetName leaderboardNameRaceColumnNameAndFleetName =
                        raceIdentifierToLeaderboardRaceColumnAndFleetMapper.getLeaderboardNameAndRaceColumnNameAndFleetName(raceIdentifier);
                if (leaderboardNameRaceColumnNameAndFleetName != null) {
                    sailingService.updateFixedMarkPassing(leaderboardNameRaceColumnNameAndFleetName.getLeaderboardName(),
                                leaderboardNameRaceColumnNameAndFleetName.getRaceColumnName(),
                                leaderboardNameRaceColumnNameAndFleetName.getFleetName(),
                                waypointSelectionModel.getSelectedObject().getA(), null, competitor, new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorRemovingFixedPassing(caught.getMessage()));
                        }
    
                        @Override
                        public void onSuccess(Void result) {
                            refillList();
                        }
                    });
                }
            }
        });
        setTimeAsMarkPassingsButton = new Button(stringMessages.setFixedPassing());
        setTimeAsMarkPassingsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final LeaderboardNameRaceColumnNameAndFleetName leaderboardNameRaceColumnNameAndFleetName =
                        raceIdentifierToLeaderboardRaceColumnAndFleetMapper.getLeaderboardNameAndRaceColumnNameAndFleetName(raceIdentifier);
                if (leaderboardNameRaceColumnNameAndFleetName != null) {
                    final Integer waypoint = waypointSelectionModel.getSelectedObject().getA();
                    if (isSettingFixedTimePossible(timer, stringMessages)) {
                    final Date time = timer.getTime();
                    sailingService.updateFixedMarkPassing(leaderboardNameRaceColumnNameAndFleetName.getLeaderboardName(),
                            leaderboardNameRaceColumnNameAndFleetName.getRaceColumnName(),
                            leaderboardNameRaceColumnNameAndFleetName.getFleetName(), waypoint, time, competitor,
                                    new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorSettingFixedPassing(caught.getMessage()));
                        }
    
                        @Override
                        public void onSuccess(Void result) {
                            refillList();
                        }
                    });
                    } else {
                        Window.alert(stringMessages.warningSettingFixedPassing(currentWaypoints.get(waypoint).getName()));
                }
            }
            }
        });

        // Button for suppressing
        suppressPassingsButton = new Button(stringMessages.setSuppressedPassing());
        suppressPassingsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final LeaderboardNameRaceColumnNameAndFleetName leaderboardNameRaceColumnNameAndFleetName =
                        raceIdentifierToLeaderboardRaceColumnAndFleetMapper.getLeaderboardNameAndRaceColumnNameAndFleetName(raceIdentifier);
                if (leaderboardNameRaceColumnNameAndFleetName != null) {
                    sailingService.updateSuppressedMarkPassings(leaderboardNameRaceColumnNameAndFleetName.getLeaderboardName(),
                            leaderboardNameRaceColumnNameAndFleetName.getRaceColumnName(),
                            leaderboardNameRaceColumnNameAndFleetName.getFleetName(),
                            waypointSelectionModel.getSelectedObject().getA(),
                            competitor, new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorSuppressingPassing(caught.getMessage()));
                        }
    
                        @Override
                        public void onSuccess(Void result) {
                            refillList();
                        }
                    });
                }
            }
        });

        removeSuppressedPassingButton = new Button(stringMessages.removeSuppressedPassing());
        removeSuppressedPassingButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final LeaderboardNameRaceColumnNameAndFleetName leaderboardNameRaceColumnNameAndFleetName =
                        raceIdentifierToLeaderboardRaceColumnAndFleetMapper.getLeaderboardNameAndRaceColumnNameAndFleetName(raceIdentifier);
                if (leaderboardNameRaceColumnNameAndFleetName != null) {
                    sailingService.updateSuppressedMarkPassings(leaderboardNameRaceColumnNameAndFleetName.getLeaderboardName(),
                            leaderboardNameRaceColumnNameAndFleetName.getRaceColumnName(),
                            leaderboardNameRaceColumnNameAndFleetName.getFleetName(),
                            null, competitor,
                                new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorRemovingSupressedPassing(caught.getMessage()));
                        }
    
                        @Override
                        public void onSuccess(Void result) {
                            refillList();
                        }
                    });
                }
            }
        });
        selectCompetitorLabel.setText(stringMessages.selectCompetitor());
        refreshWaypoints();
        AbsolutePanel rootPanel = new AbsolutePanel();
        HorizontalPanel tableAndButtons = new HorizontalPanel();
        rootPanel.add(tableAndButtons, 0, 0);
        tableAndButtons.setSpacing(3);
        tableAndButtons.add(wayPointSelectionTable);
        VerticalPanel buttonPanel = new VerticalPanel();
        buttonPanel.setSpacing(3);
        tableAndButtons.add(buttonPanel);
        buttonPanel.add(setTimeAsMarkPassingsButton);
        buttonPanel.add(removeFixedMarkPassingsButton);
        buttonPanel.add(suppressPassingsButton);
        buttonPanel.add(removeSuppressedPassingButton);
        buttonPanel.add(selectCompetitorLabel);
        enableButtons();
        initWidget(rootPanel);
        setVisible(false);
    }
    
    /**
     * Checks the possibility of setting a new fixed time for selected waypoint
     * 
     * @return false if new fixed time for waypoint is after the fixed time of any of the following waypoints or before
     *         any of the previous ones
     */
    private boolean isSettingFixedTimePossible(Timer timer, StringMessages stringMessages) {
        Integer selectedWaypointIndex = waypointSelectionModel.getSelectedObject().getA();
        for (Integer waypointIndex : currentCompetitorEdits.keySet()) {
            Date waypointDate = currentCompetitorEdits.get(waypointIndex);
            if ((waypointIndex < selectedWaypointIndex && waypointDate.after(timer.getTime()))
                    || (waypointIndex > selectedWaypointIndex && waypointDate.before(timer.getTime()))) {
                return false;
            }
        }
        return true;
    }
   
    @Override
    public void setVisible(boolean visible) {
        processCompetitorSelectionChange(visible);
        refreshWaypoints();
        super.setVisible(visible);
    }

    @Override
    public void addedToSelection(CompetitorDTO competitor) {
        processCompetitorSelectionChange(isVisible());
    }

    @Override
    public void removedFromSelection(CompetitorDTO competitor) {
        processCompetitorSelectionChange(isVisible());
    }

    private void processCompetitorSelectionChange(boolean visible) {
        waypointSelectionModel.clear();
        if (visible && Util.size(competitorSelectionModel.getSelectedCompetitors()) == 1) {
            selectCompetitorLabel.setText("");
            refillList();
        } else {
            disableEditing();
            selectCompetitorLabel.setText(stringMessages.selectCompetitor());
        }
    }

    private void disableEditing() {
        waypointList.getList().clear();
        clearInfo();
    }
    
    /**
     * Overloaded version of refill list which accepts new mark passing created on UI.
     * Implemented due to TrackedRace inaccessibility at GWT 
     * 
     * @param markPassing - pair of waypoint (integer) and datetime of passing
     */
    private void refillList() {
        clearInfo();
        competitor = competitorSelectionModel.getSelectedCompetitors().iterator().next();
        // Get current mark passings
        sailingService.getCompetitorMarkPassings(raceIdentifier, competitor, true, new AsyncCallback<Map<Integer, Date>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorTryingToObtainMarkPassing(caught.getMessage()));
            }

            @Override
            public void onSuccess(Map<Integer, Date> result) {
                List<Util.Pair<Integer, Date>> newMarkPassings = new ArrayList<>();
                for (WaypointDTO waypoint : currentWaypoints) {
                    int index = currentWaypoints.indexOf(waypoint);
                    newMarkPassings.add(new Util.Pair<Integer, Date>(index, result.get(index)));
                }
                waypointList.getList().clear();
                waypointList.getList().addAll(newMarkPassings);
                // Get current edits
                
                final LeaderboardNameRaceColumnNameAndFleetName leaderboardNameRaceColumnNameAndFleetName =
                        raceIdentifierToLeaderboardRaceColumnAndFleetMapper.getLeaderboardNameAndRaceColumnNameAndFleetName(raceIdentifier);
                if (leaderboardNameRaceColumnNameAndFleetName != null) {
                    sailingService.getCompetitorRaceLogMarkPassingData(leaderboardNameRaceColumnNameAndFleetName.getLeaderboardName(),
                            leaderboardNameRaceColumnNameAndFleetName.getRaceColumnName(),
                            leaderboardNameRaceColumnNameAndFleetName.getFleetName(), competitor, new AsyncCallback<Map<Integer, Date>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorTryingToObtainRaceLogMarkPassingData(caught.getMessage()));
                        }
    
                        @Override
                        public void onSuccess(Map<Integer, Date> result) {
                            for (Entry<Integer, Date> data : result.entrySet()) {
                                if (data.getValue() == null) {
                                    zeroBasedIndexOfFirstSuppressedWaypoint = data.getKey();
                                } else {
                                    currentCompetitorEdits.put(data.getKey(), data.getValue());
                                }
                            }
                            enableButtons();
                            wayPointSelectionTable.redraw();
                        }
                    });
                }
            }
        });
    }

    /**
     * Removes all competitor selection-specific data, particularly the fixed and suppressed mark
     * passings and the reference to the competitor selected.
     */
    private void clearInfo() {
        currentCompetitorEdits.clear();
        competitor = null;
        zeroBasedIndexOfFirstSuppressedWaypoint = null;
    }

    private void refreshWaypoints() {
        sailingService.getRaceCourse(raceIdentifier, new Date(), new AsyncCallback<RaceCourseDTO>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorTryingToObtainRaceCourse(caught.getMessage()));
            }

            @Override
            public void onSuccess(RaceCourseDTO result) {
                currentWaypoints = result.waypoints;
            }
        });
    }

    private void enableButtons() {
        setTimeAsMarkPassingsButton.setEnabled(false);
        removeFixedMarkPassingsButton.setEnabled(false);
        suppressPassingsButton.setEnabled(false);
        removeSuppressedPassingButton.setEnabled(false);
        if (Util.size(competitorSelectionModel.getSelectedCompetitors()) == 1) {
            if (zeroBasedIndexOfFirstSuppressedWaypoint != null) {
                removeSuppressedPassingButton.setEnabled(true);
            }
            Pair<Integer, Date> selectedWaypoint = waypointSelectionModel.getSelectedObject();
            if (selectedWaypoint != null) {
                setTimeAsMarkPassingsButton.setEnabled(true);
                suppressPassingsButton.setEnabled(true);
                if (currentCompetitorEdits.containsKey(selectedWaypoint.getA())) {
                    removeFixedMarkPassingsButton.setEnabled(true);
                }
            }
        }
    }

    public void setLeaderboard(LeaderboardDTO leaderboard) {
        this.raceIdentifierToLeaderboardRaceColumnAndFleetMapper.setLeaderboard(leaderboard);
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    public SettingsDialogComponent<AbstractSettings> getSettingsDialogComponent() {
        return null;
    }

    @Override
    public void updateSettings(AbstractSettings newSettings) {
    }

    @Override
    public String getLocalizedShortName() {
        return null;
    }

    @Override
    public String getDependentCssClassName() {
        return null;
    }

    @Override
    public void filterChanged(FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> oldFilterSet,
            FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> newFilterSet) {
    }

    @Override
    public void competitorsListChanged(Iterable<CompetitorDTO> competitors) {
    }

    @Override
    public void filteredCompetitorsListChanged(Iterable<CompetitorDTO> filteredCompetitors) {
    }

    @Override
    public AbstractSettings getSettings() {
        return null;
    }

    @Override
    public String getId() {
        return "EditMarkPassingsPanel";
    }

}
