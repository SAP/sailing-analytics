package com.sap.sailing.gwt.regattaoverview.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.CalendarUtil;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.gwt.settings.client.EntryPointWithSettingsLinkFactory;
import com.sap.sailing.gwt.settings.client.raceboard.RaceBoardPerspectiveOwnSettings;
import com.sap.sailing.gwt.settings.client.raceboard.RaceboardContextDefinition;
import com.sap.sailing.gwt.settings.client.regattaoverview.RegattaRaceStatesSettings;
import com.sap.sailing.gwt.ui.client.AnchorCell;
import com.sap.sailing.gwt.ui.client.EntryPointLinkFactory;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.ClickableSafeHtmlCell;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RaceCourseDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupSeriesDTO;
import com.sap.sailing.gwt.ui.shared.RaceInfoDTO;
import com.sap.sailing.gwt.ui.shared.RegattaOverviewEntryDTO;
import com.sap.sailing.gwt.ui.shared.WaypointDTO;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.DateTimeUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.shared.components.AbstractCompositeComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

/**
 * This component shows a table displaying the current state of races for a given event. Which races are shown depends
 * on the setting {@link RegattaRaceStatesSettings}. Each entry shows what flags are currently displayed, what start
 * time the race has and additional information, e.g. for Gate start.
 */
public class RegattaRaceStatesComponent extends AbstractCompositeComponent<RegattaRaceStatesSettings> {
    public interface EntryHandler {
        void onEntryClicked(RegattaOverviewEntryDTO entry);

        void onEntryUpdated(RegattaOverviewEntryDTO entry);
    }

    private List<RegattaOverviewEntryDTO> allEntries;
    // private CellTable<RegattaOverviewEntryDTO> regattaOverviewTable;
    private ListDataProvider<RegattaOverviewEntryDTO> regattaOverviewDataProvider;
    private final VerticalPanel mainPanel;
    private final DateTimeFormat timeFormatter = DateTimeFormat.getFormat("HH:mm:ss");
    private final DateTimeFormat dateFormatter = DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT);
    private final SailingServiceAsync sailingService;
    private final StringMessages stringMessages;
    private final UUID eventId;
    private EventDTO eventDTO;
    private List<RaceGroupDTO> raceGroupDTOs;
    private TextColumn<RegattaOverviewEntryDTO> seriesNameColumn;
    private TextColumn<RegattaOverviewEntryDTO> fleetNameColumn;
    private final RegattaRaceStatesSettings settings;
    private final RaceStateFlagsInterpreter flagInterpreter;
    private final Timer timerToSynchronize;
    private static RegattaRaceStatesTableResources tableRes = GWT.create(RegattaRaceStatesTableResources.class);
    private EntryHandler entryClickedHandler;
    private TextColumn<RegattaOverviewEntryDTO> courseAreaColumn;
    private Column<RegattaOverviewEntryDTO, Anchor> regattaNameColumn;
    private FlowPanel repeatedInfoLabel;
    private Column<RegattaOverviewEntryDTO, SafeHtml> raceCourseColumn;
    private TextColumn<RegattaOverviewEntryDTO> boatClass;
    private TextColumn<RegattaOverviewEntryDTO> startTimeColumn;
    private TextColumn<RegattaOverviewEntryDTO> finishingTimeColumn;
    private TextColumn<RegattaOverviewEntryDTO> finishedTimeColumn;
    private SimplePanel tableHolder = new SimplePanel();
    private final long _1_HOUR = 60 /* seconds */* 60 /* minutes */* 1 /* hour */;
    private final long HIDE_COL_TIME_THRESHOLD = _1_HOUR;
    private TextColumn<RegattaOverviewEntryDTO> lastUpdateColumn;
    private TextColumn<RegattaOverviewEntryDTO> endOfProtestTime;
    private TextColumn<RegattaOverviewEntryDTO> raceAdditionalInformationColumn;
    private Column<RegattaOverviewEntryDTO, SafeHtml> flagColumn;
    private Column<RegattaOverviewEntryDTO, SafeHtml> raceStatusColumn;
    private Column<RegattaOverviewEntryDTO, Anchor> raceNameColumn;
    private ListHandler<RegattaOverviewEntryDTO> regattaOverviewListHandler;
    private CellTable<RegattaOverviewEntryDTO> table;
    boolean hasAnyRaceGroupASeries = false;
    boolean hasAnyRaceGroupAFleet = false;

    public void setEntryClickedHandler(EntryHandler handler) {
        this.entryClickedHandler = handler;
    }

    /**
     * @param timerToSynchronize
     *            Whenever this component makes a service call and receives an update on the current server time, the
     *            timer passed for this argument will be synchronized.
     */
    public RegattaRaceStatesComponent(Component<?> parent, ComponentContext<?> componentContext,
            final SailingServiceAsync sailingService,
            ErrorReporter errorReporter,
            final StringMessages stringMessages, UUID eventId, EventDTO eventDTO, List<RaceGroupDTO> raceGroupDTOs, RegattaRaceStatesSettings settings,
            Timer timerToSynchronize) {
        super(parent, componentContext);
        this.eventId = eventId;
        this.sailingService = sailingService;
        this.stringMessages = stringMessages;
        this.allEntries = new ArrayList<RegattaOverviewEntryDTO>();
        this.timerToSynchronize = timerToSynchronize;
        this.eventDTO = null;
        this.raceGroupDTOs = null;
        this.flagInterpreter = new RaceStateFlagsInterpreter(stringMessages);
        this.settings = new RegattaRaceStatesSettings();
        mainPanel = new VerticalPanel();
        mainPanel.getElement().getStyle().setWidth(100, Unit.PCT);
        regattaOverviewDataProvider = new ListDataProvider<RegattaOverviewEntryDTO>();
        // regattaOverviewTable = createRegattaTable(true);
        createColumns();
        mainPanel.add(tableHolder);
        initWidget(mainPanel);
        getElement().getStyle().setWidth(100, Unit.PCT);
        this.eventDTO = eventDTO;
        setRaceGroups(raceGroupDTOs);
        updateSettings(settings);
    }

    public void onUpdateServer() {
        loadAndUpdateEventLog();
    }

    @SuppressWarnings("unchecked")
    private void updateTable(List<RegattaOverviewEntryDTO> newEntries) {
        allEntries = newEntries;
        String firstCourseAreaName = null;
        String firstRegattaName = null;
        String firstCourseName = null;
        RaceInfoDTO firstCourseRegattaOverviewEntry = null;
        RegattaOverviewEntryDTO firstRegattaNameEntry = null;
        String lastBoatClass = null;
        boolean canRemoveCourseArea = true;
        boolean canRemoveCourse = true;
        boolean canRemoveRegatta = true;
        boolean canRemoveBoatClass = true;
        boolean canRemoveLastUpdate = true;
        boolean canRemoveProtestTime = true;
        boolean canRemoveFinishingTime = true;
        boolean canRemoveFinishedTime = true;
        boolean first = true;
        for (RegattaOverviewEntryDTO loopEntryDTO : allEntries) {
            final RaceInfoDTO loopRaceInfo = loopEntryDTO.raceInfo;
            if (canRemoveLastUpdate && timePassedInSeconds(loopRaceInfo.lastUpdateTime) <= HIDE_COL_TIME_THRESHOLD) {
                canRemoveLastUpdate = false;
            }
            if (canRemoveFinishingTime && loopRaceInfo.finishingTime != null
                    && timePassedInSeconds(loopRaceInfo.finishingTime) <= HIDE_COL_TIME_THRESHOLD) {
                canRemoveFinishingTime = false;
                canRemoveFinishedTime = false;
            }
            if (canRemoveFinishedTime && loopRaceInfo.finishedTime != null) {
                canRemoveFinishedTime = false;
            }
            if (canRemoveProtestTime && loopRaceInfo.protestFinishTime != null
                    && timePassedInSeconds(loopRaceInfo.protestFinishTime) <= HIDE_COL_TIME_THRESHOLD) {
                canRemoveProtestTime = false;
            }
            if (first) {
                firstRegattaName = loopEntryDTO.regattaDisplayName;
                firstRegattaNameEntry = loopEntryDTO;
                firstCourseAreaName = loopEntryDTO.courseAreaName;
                firstCourseName = loopRaceInfo.lastCourseName;
                firstCourseRegattaOverviewEntry = loopRaceInfo;
                lastBoatClass = loopEntryDTO.boatClassName;
            } else {
                if (canRemoveRegatta && !Util.equalsWithNull(firstRegattaName, loopEntryDTO.regattaDisplayName)) {
                    canRemoveRegatta = false;
                }
                if (canRemoveCourseArea && !Util.equalsWithNull(firstCourseAreaName, loopEntryDTO.courseAreaName)) {
                    canRemoveCourseArea = false;
                }
                // a course must be shown individually for a race if its name or its course design differs
                if (canRemoveCourse && (!Util.equalsWithNull(firstCourseName, loopRaceInfo.lastCourseName)
                        || !Util.equalsWithNull(firstCourseRegattaOverviewEntry.lastCourseDesign,
                                loopRaceInfo.lastCourseDesign))) {
                    canRemoveCourse = false;
                }
                if (canRemoveBoatClass && !Util.equalsWithNull(lastBoatClass, loopEntryDTO.boatClassName)) {
                    canRemoveBoatClass = false;
                }
            }
            first = false;
        }
        // final RegattaOverviewEntryDTO entryForRepeatedInfos = firstEntry;
        repeatedInfoLabel.clear();
        boolean isAppending = false;
        final RegattaOverviewEntryDTO finalFirstRegattaNameEntry = firstRegattaNameEntry;
        isAppending |= collectRepeatedInfos(stringMessages.regatta(), firstRegattaName, canRemoveRegatta,
                repeatedInfoLabel, isAppending, new Command() {
                    @Override
                    public void execute() {
                        Window.open(createRegattaLink(finalFirstRegattaNameEntry), "_blank", "");
                    }
                });
        isAppending |= collectRepeatedInfos(stringMessages.courseArea(), firstCourseAreaName, canRemoveCourseArea,
                repeatedInfoLabel, isAppending, null);
        final RaceInfoDTO _lastCourseEntry = firstCourseRegattaOverviewEntry;
        isAppending |= collectRepeatedInfos(stringMessages.course(), firstCourseName, canRemoveCourse,
                repeatedInfoLabel, isAppending, new Command() {
                    @Override
                    public void execute() {
                        raceCourseClicked(_lastCourseEntry);
                    }
                });
        isAppending |= collectRepeatedInfos(stringMessages.boatClass(), lastBoatClass, canRemoveBoatClass,
                repeatedInfoLabel,
                isAppending, null);
        LinkedList<ColumnSortInfo> sortInfos = new LinkedList<ColumnSortList.ColumnSortInfo>();
        if (table != null) {
            ColumnSortList columnSortList = table.getColumnSortList();
            for (int i = 0; i < columnSortList.size(); i++) {
                sortInfos.addFirst(columnSortList.get(i));
            }
            table.removeFromParent();
            while (table.getColumnCount() > 0) {
                table.removeColumn(0);
            }
            regattaOverviewDataProvider.removeDataDisplay(table);
        } else {
            sortInfos.add(new ColumnSortInfo(lastUpdateColumn, false));
        }
        table = new BaseCelltable<RegattaOverviewEntryDTO>(/* pageSize */10000, tableRes);
        tableHolder.setWidget(table);
        regattaOverviewDataProvider.getList().clear();
        regattaOverviewDataProvider.addDataDisplay(table);
        table.setWidth("100%");
        if (!canRemoveRegatta) {
            table.addColumn(regattaNameColumn, stringMessages.regatta());
        }
        if (!canRemoveCourseArea) {
            table.addColumn(courseAreaColumn, stringMessages.courseArea());
        }
        if (!canRemoveCourse) {
            table.addColumn(raceCourseColumn, stringMessages.course());
        }
        table.addColumn(raceNameColumn, stringMessages.race());
        if (!canRemoveBoatClass) {
            table.addColumn(boatClass, stringMessages.boatClass());
        }
        if (hasAnyRaceGroupAFleet) {
            table.addColumn(fleetNameColumn, stringMessages.fleet());
            table.setColumnWidth(fleetNameColumn, 115, Unit.PX);
        }
        table.addColumn(seriesNameColumn, stringMessages.series());
        table.addColumn(flagColumn, stringMessages.flags());
        table.setColumnWidth(flagColumn, 95, Unit.PX);
        table.addColumn(raceStatusColumn, stringMessages.status());
        if (!canRemoveLastUpdate) {
            table.addColumn(lastUpdateColumn, stringMessages.lastUpdate());
        }
        table.addColumn(startTimeColumn, stringMessages.startTime());
        if (!canRemoveFinishingTime) {
            table.addColumn(finishingTimeColumn, stringMessages.finishingTime());
        }
        table.addColumn(finishedTimeColumn, stringMessages.finishTimeString());
        if (!canRemoveProtestTime) {
            table.addColumn(endOfProtestTime, stringMessages.protestTime());
        }
        table.addColumn(raceAdditionalInformationColumn, stringMessages.additionalInformation());
        table.addColumnSortHandler(regattaOverviewListHandler);
        regattaOverviewDataProvider.getList().clear();
        regattaOverviewDataProvider.getList().addAll(allEntries);
        if (entryClickedHandler != null) {
            for (RegattaOverviewEntryDTO entry : allEntries) {
                entryClickedHandler.onEntryUpdated(entry);
            }
        }
        boolean isSortedOk = false;
        if (sortInfos.size() > 0) {
            GWT.log("Using existing sort info");
            for (ColumnSortInfo columnSortInfo : sortInfos) {
                if (table.getColumnIndex((Column<RegattaOverviewEntryDTO, ?>) columnSortInfo.getColumn()) >= 0) {
                    GWT.log("Pushed: "
                            + table.getHeader(
                                    table.getColumnIndex((Column<RegattaOverviewEntryDTO, ?>) columnSortInfo
                                            .getColumn())).getValue() + ", "
                            + (columnSortInfo.isAscending() ? "ascending" : "descending"));
                    table.getColumnSortList().push(columnSortInfo);
                    isSortedOk = true;
                }
            }
        }
        if (!isSortedOk) {
            ColumnSortInfo initialSortinfo = new ColumnSortInfo(courseAreaColumn, /* ascending */false);
            table.getColumnSortList().push(initialSortinfo);
        }
        ColumnSortEvent.fire(table, table.getColumnSortList());
    }

    /**
     * @param info
     *            the text to append; if {@code null} or {@link String#isEmpty() empty}, nothing will be appended, not
     *            even a ", " if {@code append==true}.
     * @param canRemove
     *            only when this is {@code true} will {@code info} be appended, and only if it's not {@code null} and
     *            not {@link String#isEmpty() empty}.
     * @param panel
     *            whether to append a ", " to the string before appending the actual {@code info}; use
     *            {@code append==true} to append any but the first element.
     * @param linkAction
     *            if non-{@code null}, the {@code info} will be rendered as an anchor that, when clicked, will execute
     *            this action
     * @return whether we're now appending, or in other words, returns {@code false} when the next call to this method
     *         will still be producing the first element because this call was not {@code append}ing and didn't append
     *         anything either; {@code true} otherwise
     */
    private boolean collectRepeatedInfos(String label, String info, boolean canRemove, FlowPanel panel, boolean append,
            final Command linkAction) {
        if (canRemove && info != null && !info.isEmpty()) {
            if (append) {
                panel.add(new InlineLabel(", "));
            }
            if (linkAction == null) {
                panel.add(new InlineLabel(new StringBuilder(label).append(": ").append(info).toString()));
            } else {
                panel.add(new InlineLabel(new StringBuilder(label).append(": ").toString()));
                Anchor anchor = new Anchor(info);
                anchor.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        linkAction.execute();
                    }
                });
                panel.add(anchor);
            }
            return true;
        }
        return append;
    }

    protected void loadAndUpdateEventLog() {
        if (eventDTO == null || raceGroupDTOs == null) {
            return;
        }
        final long clientTimeWhenRequestWasSent = System.currentTimeMillis();
        final Duration clientTimeZoneOffset = DateTimeUtil.getClientTimezoneOffsetFromUTC();
        sailingService.getRaceStateEntriesForRaceGroup(eventId, Util.asList(settings.getVisibleCourseAreas()),
                Util.asList(settings.getVisibleRegattas()), settings.isShowOnlyCurrentlyRunningRaces(), settings
                .isShowOnlyRacesOfSameDay(),
                clientTimeZoneOffset, new MarkedAsyncCallback<List<RegattaOverviewEntryDTO>>(
                        new AsyncCallback<List<RegattaOverviewEntryDTO>>() {
                            @Override
                            public void onFailure(Throwable cause) {
                                // ignore errors as state can recover
                            }

                            @Override
                            public void onSuccess(List<RegattaOverviewEntryDTO> result) {
                                final long clientTimeWhenResponseWasReceived = System.currentTimeMillis();
                                Date serverTimeDuringRequest = null;
                                for (RegattaOverviewEntryDTO entryDTO : result) {
                                    if (entryDTO.currentServerTime != null) {
                                        serverTimeDuringRequest = entryDTO.currentServerTime;
                                    }
                                }
                                updateTable(result);
                                timerToSynchronize.adjustClientServerOffset(clientTimeWhenRequestWasSent,
                                        serverTimeDuringRequest, clientTimeWhenResponseWasReceived);
                            }
                        }));
    }

    private String formatTime(Date time) {
        String result = "-";
        if (time != null) {
            result = "";
            if (!CalendarUtil.isSameDate(time, new Date())) {
                result += dateFormatter.format(time) + " ";
            }
            result += timeFormatter.format(time);
        }
        return result;
    }


    private void createColumns() {
        regattaOverviewListHandler = new ListHandler<RegattaOverviewEntryDTO>(regattaOverviewDataProvider.getList());
        courseAreaColumn = new TextColumn<RegattaOverviewEntryDTO>() {
            @Override
            public String getValue(RegattaOverviewEntryDTO entryDTO) {
                return entryDTO.courseAreaName;
            }
        };
        courseAreaColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(courseAreaColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                return new NaturalComparator().compare(left.courseAreaName, right.courseAreaName);
            }
        });
        AnchorCell leaderboardAnchorCell = new AnchorCell();
        regattaNameColumn = new Column<RegattaOverviewEntryDTO, Anchor>(leaderboardAnchorCell) {
            @Override
            public Anchor getValue(RegattaOverviewEntryDTO entryDTO) {
                String leaderboardLink = createRegattaLink(entryDTO);
                Anchor result = new Anchor(entryDTO.regattaDisplayName, leaderboardLink);
                return result;
            }


        };
        regattaNameColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(regattaNameColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                return new NaturalComparator().compare(left.regattaDisplayName, right.regattaDisplayName);
            }
        });
        seriesNameColumn = new TextColumn<RegattaOverviewEntryDTO>() {
            @Override
            public String getValue(RegattaOverviewEntryDTO entryDTO) {
                return entryDTO.raceInfo.seriesName.equals(LeaderboardNameConstants.DEFAULT_SERIES_NAME) ? "-"
                        : entryDTO.raceInfo.seriesName;
            }
        };
        seriesNameColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(seriesNameColumn, new Comparator<RegattaOverviewEntryDTO>() {

            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                return new NaturalComparator().compare(left.raceInfo.seriesName, right.raceInfo.seriesName);
            }
        });
        // seriesNameColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        fleetNameColumn = new TextColumn<RegattaOverviewEntryDTO>() {
            @Override
            public String getValue(RegattaOverviewEntryDTO entryDTO) {
                return entryDTO.raceInfo.fleetName.equals(LeaderboardNameConstants.DEFAULT_FLEET_NAME) ? "-"
                        : entryDTO.raceInfo.fleetName;
            }
        };
        fleetNameColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(fleetNameColumn, new Comparator<RegattaOverviewEntryDTO>() {

            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                return new NaturalComparator().compare(left.raceInfo.fleetName, right.raceInfo.fleetName);
            }
        });
        // fleetNameColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        AnchorCell raceCell = new AnchorCell();
        raceNameColumn = new Column<RegattaOverviewEntryDTO, Anchor>(raceCell) {
            @Override
            public Anchor getValue(RegattaOverviewEntryDTO entryDTO) {
                String raceBoardLink = createRaceLink(entryDTO);
                String raceName = entryDTO.raceInfo.raceName;
                Anchor result = new Anchor(raceName);
                result.setEnabled(raceBoardLink != null);
                if (raceBoardLink != null) {
                    result.setHref(raceBoardLink);
                }
                return result;
            }
        };
        raceNameColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(raceNameColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                return new NaturalComparator().compare(left.raceInfo.raceName, right.raceInfo.raceName);
            }
        });
        startTimeColumn = new TextColumn<RegattaOverviewEntryDTO>() {
            @Override
            public String getValue(RegattaOverviewEntryDTO entryDTO) {
                return formatTime(entryDTO.raceInfo.startTime);
            }
        };
        // raceStartTimeColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        startTimeColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(startTimeColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                int result = 0;
                if (left.raceInfo.startTime != null && right.raceInfo.startTime != null) {
                    result = left.raceInfo.startTime.compareTo(right.raceInfo.startTime);
                } else if (left.raceInfo.startTime == null && right.raceInfo.startTime == null) {
                    result = 0;
                } else if (left.raceInfo.startTime == null) {
                    result = 1;
                } else if (right.raceInfo.startTime == null) {
                    result = -1;
                } else {
                    result = 0;
                }
                return result;
            }
        });
        finishingTimeColumn = new TextColumn<RegattaOverviewEntryDTO>() {
            @Override
            public String getValue(RegattaOverviewEntryDTO entryDTO) {
                return formatTime(entryDTO.raceInfo.finishingTime);
            }
        };
        finishingTimeColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(finishingTimeColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                int result = 0;
                if (left.raceInfo.finishingTime != null && right.raceInfo.finishingTime != null) {
                    result = left.raceInfo.finishingTime.compareTo(right.raceInfo.finishingTime);
                } else if (left.raceInfo.finishingTime == null && right.raceInfo.finishingTime == null) {
                    result = 0;
                } else if (left.raceInfo.finishingTime == null) {
                    result = 1;
                } else if (right.raceInfo.finishingTime == null) {
                    result = -1;
                }
                return result;
            }
        });
        finishedTimeColumn = new TextColumn<RegattaOverviewEntryDTO>() {
            @Override
            public String getValue(RegattaOverviewEntryDTO entryDTO) {
                return formatTime(entryDTO.raceInfo.finishedTime);
            }
        };
        finishedTimeColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(finishedTimeColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                int result = 0;
                if (left.raceInfo.finishedTime != null && right.raceInfo.finishedTime != null) {
                    result = left.raceInfo.finishedTime.compareTo(right.raceInfo.finishedTime);
                } else if (left.raceInfo.finishedTime == null && right.raceInfo.finishedTime == null) {
                    result = 0;
                } else if (left.raceInfo.finishedTime == null) {
                    result = 1;
                } else if (right.raceInfo.finishedTime == null) {
                    result = -1;
                }
                return result;
            }
        });
        raceStatusColumn = new Column<RegattaOverviewEntryDTO, SafeHtml>(new ClickableSafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final RegattaOverviewEntryDTO entryDTO) {
                String status = flagInterpreter.getMeaningOfRaceStateAndFlags(entryDTO.raceInfo.lastStatus,
                        entryDTO.raceInfo.lastUpperFlag, entryDTO.raceInfo.lastLowerFlag,
                        entryDTO.raceInfo.lastFlagsAreDisplayed);
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                sb.appendHtmlConstant("<a class=\"pointeredLink\">");
                sb.appendEscaped(status);
                sb.appendHtmlConstant("</a>");
                return sb.toSafeHtml();
            }
        };
        raceStatusColumn.setFieldUpdater(new FieldUpdater<RegattaOverviewEntryDTO, SafeHtml>() {
            @Override
            public void update(int index, RegattaOverviewEntryDTO object, SafeHtml value) {
                if (entryClickedHandler != null) {
                    entryClickedHandler.onEntryClicked(object);
                }
            }
        });
        raceStatusColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(raceStatusColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                return left.raceInfo.lastStatus.compareTo(right.raceInfo.lastStatus);
            }
        });
        // raceStatusColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        raceCourseColumn = new Column<RegattaOverviewEntryDTO, SafeHtml>(new ClickableSafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final RegattaOverviewEntryDTO entryDTO) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                if (entryDTO.raceInfo.lastCourseName != null) {
                    String courseName = entryDTO.raceInfo.lastCourseName;
                    sb.appendHtmlConstant("<a class=\"pointeredLink\">");
                    sb.appendEscaped(courseName);
                    sb.appendHtmlConstant("</a>");
                    return sb.toSafeHtml();
                } else {
                    return sb.toSafeHtml();
                }
            }
        };
        raceCourseColumn.setSortable(true);
        raceCourseColumn.setFieldUpdater(new FieldUpdater<RegattaOverviewEntryDTO, SafeHtml>() {
            @Override
            public void update(int index, RegattaOverviewEntryDTO object, SafeHtml value) {
                raceCourseClicked(object.raceInfo);
            }
        });
        // raceCourseColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        regattaOverviewListHandler.setComparator(raceCourseColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                String cnLeft = left.raceInfo.lastCourseName == null ? "" : left.raceInfo.lastCourseName;
                String cnRight = right.raceInfo.lastCourseName == null ? "" : right.raceInfo.lastCourseName;
                int firstCompare = cnLeft.compareTo(cnRight);
                if (firstCompare != 0) {
                    return firstCompare;
                }
                return left.boatClassName.compareTo(right.boatClassName);
            }
        });
        SafeHtmlCell flagsCell = new SafeHtmlCell();
        flagColumn = new Column<RegattaOverviewEntryDTO, SafeHtml>(flagsCell) {
            @Override
            public SafeHtml getValue(RegattaOverviewEntryDTO entryDTO) {
                String tooltip = "";
                if (entryDTO.raceInfo.lastUpperFlag != null && entryDTO.raceInfo.lastLowerFlag != null) {
                    tooltip = entryDTO.raceInfo.lastUpperFlag.name();
                    if (!entryDTO.raceInfo.lastLowerFlag.equals(Flags.NONE)) {
                        tooltip += " over " + entryDTO.raceInfo.lastLowerFlag.name();
                    }
                    tooltip += " "
                            + FlagsMeaningExplanator.getFlagsMeaning(stringMessages, entryDTO.raceInfo.lastUpperFlag,
                                    entryDTO.raceInfo.lastLowerFlag, entryDTO.raceInfo.lastFlagsAreDisplayed);
                }
                return SailingFlagsBuilder.render(entryDTO.raceInfo.lastUpperFlag, entryDTO.raceInfo.lastLowerFlag,
                        entryDTO.raceInfo.lastFlagsAreDisplayed, entryDTO.raceInfo.lastFlagsDisplayedStateChanged,
                        tooltip);
            }
        };
        flagColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(flagColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO o1, RegattaOverviewEntryDTO o2) {
                NaturalComparator comp = new NaturalComparator();
                int upper = comp.compare(o1.raceInfo.lastUpperFlag.name(), o2.raceInfo.lastUpperFlag.name());
                return upper != 0 ? upper : comp.compare(o1.raceInfo.lastLowerFlag.name(), o2.raceInfo.lastLowerFlag.name());
            }
            
        });
        flagColumn.setCellStyleNames(tableRes.cellTableStyle().flagColumn());
        boatClass = new TextColumn<RegattaOverviewEntryDTO>() {
            @Override
            public String getValue(RegattaOverviewEntryDTO entryDTO) {
                return entryDTO.boatClassName;
            }
        };
        boatClass.setSortable(true);
        regattaOverviewListHandler.setComparator(boatClass, new Comparator<RegattaOverviewEntryDTO>() {

            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                return new NaturalComparator().compare(left.boatClassName, right.boatClassName);
            }
            
        });
        lastUpdateColumn = new TextColumn<RegattaOverviewEntryDTO>() {
            private final NumberFormat FMT = NumberFormat.getFormat("00");

            @Override
            public String getValue(RegattaOverviewEntryDTO entryDTO) {
                if (entryDTO.raceInfo.lastUpdateTime == null) {
                    return "-";
                }
                final long lastUpdateInSeconds = timePassedInSeconds(entryDTO.raceInfo.lastUpdateTime);
                if (lastUpdateInSeconds > HIDE_COL_TIME_THRESHOLD) {
                    return timeFormatter.format(entryDTO.raceInfo.lastUpdateTime);
                }
                final StringBuilder sb = new StringBuilder();
                if (lastUpdateInSeconds < 60) {
                    sb.append(lastUpdateInSeconds).append("s");
                } else if (lastUpdateInSeconds < _1_HOUR) {
                    sb.append(lastUpdateInSeconds / 60).append("m");
                } else {
                    long hours = lastUpdateInSeconds / _1_HOUR;
                    long minutes = (lastUpdateInSeconds - (hours * _1_HOUR)) / 60;
                    if (hours > HIDE_COL_TIME_THRESHOLD) {
                        return "";
                    }
                    sb.append(FMT.format(hours)).append(":").append(FMT.format(minutes));
                }
                return sb.toString();
            }
        };
        lastUpdateColumn.setDefaultSortAscending(false);
        lastUpdateColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(lastUpdateColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
                boolean nullGreater = false;
                Date leftLastUpdateTime = left.raceInfo.lastUpdateTime;
                Date rightLastUpdateTime = right.raceInfo.lastUpdateTime;
                if (leftLastUpdateTime == rightLastUpdateTime) {
                    return 0;
                } else if (leftLastUpdateTime == null) {
                    return (nullGreater ? 1 : -1);
                } else if (rightLastUpdateTime == null) {
                    return (nullGreater ? -1 : 1);
                }
                return leftLastUpdateTime.compareTo(rightLastUpdateTime);
            }
        });
        raceAdditionalInformationColumn = new TextColumn<RegattaOverviewEntryDTO>() {
            @Override
            public String getValue(RegattaOverviewEntryDTO entryDTO) {
                StringBuffer additionalInformation = new StringBuffer();
                boolean isInfoBefore = false;
                if (entryDTO.raceInfo.lastLowerFlag != null && entryDTO.raceInfo.lastLowerFlag.equals(Flags.ALPHA)) {
                    additionalInformation.append(stringMessages.noMoreRacingToday());
                    isInfoBefore = true;
                } else if (entryDTO.raceInfo.lastLowerFlag != null
                        && entryDTO.raceInfo.lastLowerFlag.equals(Flags.HOTEL)) {
                    additionalInformation.append(stringMessages.furtherSignalsAshore());
                    isInfoBefore = true;
                } else if (entryDTO.raceInfo.lastUpperFlag != null
                        && entryDTO.raceInfo.lastUpperFlag.equals(Flags.XRAY)
                        && entryDTO.raceInfo.lastFlagsAreDisplayed) {
                    additionalInformation.append(stringMessages.earlyStarters());
                    isInfoBefore = true;
                }
                if (entryDTO.raceInfo.finishedTime != null) {
                    if (isInfoBefore) {
                        additionalInformation.append("  /  ");
                    }
                    final Duration finishDuration;
                    if (entryDTO.raceInfo.finishingTime != null) {
                        finishDuration = new MillisecondsDurationImpl(
                                entryDTO.raceInfo.finishedTime.getTime()
                                        - entryDTO.raceInfo.finishingTime.getTime());
                    } else {
                        finishDuration = new MillisecondsDurationImpl(0);
                    }
                    additionalInformation.append(stringMessages.finishDuration(formatDuration(finishDuration)));
                    isInfoBefore = true;
                }
                return additionalInformation.toString();
            }
        };
        raceAdditionalInformationColumn.setSortable(true);
        regattaOverviewListHandler.setComparator(raceAdditionalInformationColumn, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO o1, RegattaOverviewEntryDTO o2) {
                return new NaturalComparator().compare(raceAdditionalInformationColumn.getValue(o1), raceAdditionalInformationColumn.getValue(o2));
            }
        });
        endOfProtestTime = new TextColumn<RegattaOverviewEntryDTO>() {
            @Override
            public String getValue(RegattaOverviewEntryDTO entryDTO) {
                StringBuffer additionalInformation = new StringBuffer();
                if (entryDTO.raceInfo.protestFinishTime != null) {
                    final Date now = new Date();
                    if (entryDTO.raceInfo.protestFinishTime.after(now)) {
                        additionalInformation.append(stringMessages.protestTimeFinishesAt());
                        additionalInformation.append(" ");
                    } else {
                        additionalInformation.append(stringMessages.protestTimeFinishedAt());
                        additionalInformation.append(" ");
                    }
                    additionalInformation.append(timeFormatter.format(entryDTO.raceInfo.protestFinishTime));
                }
                return additionalInformation.toString();
            }
        };
        endOfProtestTime.setSortable(true);
        regattaOverviewListHandler.setComparator(endOfProtestTime, new Comparator<RegattaOverviewEntryDTO>() {
            @Override
            public int compare(RegattaOverviewEntryDTO o1, RegattaOverviewEntryDTO o2) {
                return new NaturalComparator().compare(o1.raceInfo.protestFinishTime.toString(), o2.raceInfo.protestFinishTime.toString());
            }
        });
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public SettingsDialogComponent<RegattaRaceStatesSettings> getSettingsDialogComponent(RegattaRaceStatesSettings settings) {
        return new RegattaRaceStatesSettingsDialogComponent(settings, stringMessages,
                Collections.unmodifiableList(eventDTO.getVenue().getCourseAreas()),
                Collections.unmodifiableList(raceGroupDTOs));
    }

    @Override
    public void updateSettings(RegattaRaceStatesSettings newSettings) {
        setDefaultCourseAreas();
        settings.setVisibleCourseAreas(newSettings.getVisibleCourseAreas());
        setDefaultRegattas();
        settings.setVisibleRegattas(newSettings.getVisibleRegattas());
        settings.setShowOnlyRaceOfSameDay(newSettings.isShowOnlyRacesOfSameDay());
        settings.setShowOnlyCurrentlyRunningRaces(newSettings.isShowOnlyCurrentlyRunningRaces());
        refreshTableWithNewSettings();
    }

    @Override
    public RegattaRaceStatesSettings getSettings() {
        return settings;
    }

    private void refreshTableWithNewSettings() {
        if (eventDTO != null && raceGroupDTOs != null) {
            loadAndUpdateEventLog();
        }
    }

    private void setDefaultRegattas() {
        settings.setDefaultRegattas(raceGroupDTOs);
    }

    private void setDefaultCourseAreas() {
        if (eventDTO != null) {
            settings.setDefaultCourseAreas(eventDTO.getVenue().getCourseAreas());
        }
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.regattaOverview();
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    private void setRaceGroups(List<RaceGroupDTO> raceGroups) {
        raceGroupDTOs = raceGroups;
        GWT.log("onRaceGroupsUpdated");
        if (raceGroupDTOs != null) {
            search: for (RaceGroupDTO raceGroup : raceGroupDTOs) {
                for (RaceGroupSeriesDTO series : raceGroup.getSeries()) {
                    if (!series.getName().equals(LeaderboardNameConstants.DEFAULT_SERIES_NAME)) {
                        hasAnyRaceGroupASeries = true;
                    }
                    if (!hasAnyRaceGroupAFleet) { // don't re-check fleet, we already found one
                        for (FleetDTO fleet : series.getFleets()) {
                            if (!fleet.getName().equals(LeaderboardNameConstants.DEFAULT_FLEET_NAME)) {
                                hasAnyRaceGroupAFleet = true;
                                break;
                            }
                        }
                    }
                    if (hasAnyRaceGroupASeries && hasAnyRaceGroupAFleet) {
                        break search;
                    }
                }
            }
        }
    }

    private DialogBox createCourseViewDialogBox(RaceInfoDTO raceInfoDTO) {
        final DialogBox dialogBox = new DialogBox();
        dialogBox.setText(stringMessages.courseLayout());
        // Create a table to layout the content
        VerticalPanel dialogContents = new VerticalPanel();
        dialogContents.setSpacing(4);
        dialogBox.setWidget(dialogContents);
        Label courseNameLabel = new Label(raceInfoDTO.lastCourseName);
        dialogContents.add(courseNameLabel);
        Grid waypointGrid = new Grid(raceInfoDTO.lastCourseDesign.waypoints.size(), 1);
        dialogContents.add(waypointGrid);
        for (int i = 0; i < raceInfoDTO.lastCourseDesign.waypoints.size(); i++) {
            WaypointDTO waypoint = raceInfoDTO.lastCourseDesign.waypoints.get(i);
            waypointGrid.setText(i, 0, getWaypointNameLabel(waypoint));
        }
        Button closeButton = new Button(stringMessages.ok());
        closeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                dialogBox.hide();
            }
        });
        dialogContents.add(closeButton);
        dialogContents.setCellHorizontalAlignment(closeButton, HasHorizontalAlignment.ALIGN_CENTER);
        return dialogBox;
    }

    private String getWaypointNameLabel(WaypointDTO waypointDTO) {
        String result = waypointDTO.getName();
        result += (waypointDTO.passingInstructions == null) ? "" : ","
                + getPassingInstructionsAsText(waypointDTO.passingInstructions);
        return result;
    }

    private void raceCourseClicked(RaceInfoDTO raceInfo) {
        RaceCourseDTO courseDTO = raceInfo.lastCourseDesign;
        if (courseDTO != null && courseDTO.waypoints.size() > 0) {
            DialogBox courseViewDialogBox = createCourseViewDialogBox(raceInfo);
            courseViewDialogBox.center();
            courseViewDialogBox.setGlassEnabled(true);
            courseViewDialogBox.setAnimationEnabled(true);
            courseViewDialogBox.show();
        }
    }

    private String getPassingInstructionsAsText(PassingInstruction passingInstructions) {
        switch (passingInstructions) {
        case Single_Unknown:
            return stringMessages.toSide() + " " + stringMessages.unknown();
        case Port:
            return stringMessages.toSide() + " " + stringMessages.portSide();
        case Starboard:
            return stringMessages.toSide() + " " + stringMessages.starboardSide();
        case Gate:
            return stringMessages.gate();
        case Line:
            return stringMessages.line();
        case Offset:
            return stringMessages.offset();
        default:
            return "";
        }
    }

    private final long timePassedInSeconds(Date dateTime) {
        if (dateTime == null)
            return 0;
        return (new Date().getTime() - dateTime.getTime()) / 1000;
    }

    private String formatDuration(Duration duration) {
        long seconds = (long) Math.round(duration.asSeconds());
        final long days = seconds / (long) Duration.ONE_DAY.asSeconds();
        seconds %= 24 * 3600;
        final long hours = seconds / (long) Duration.ONE_HOUR.asSeconds();
        seconds %= 3600;
        final long minutes = seconds / (long) Duration.ONE_MINUTE.asSeconds();
        seconds %= 60;
        final StringJoiner sj = new StringJoiner(":");
        if (days > 0) {
            sj.add(String.valueOf(days));
        }
        if (days > 0 && hours < 10) {
            sj.add("0" + String.valueOf(hours));
        } else {
            sj.add(String.valueOf(hours));
        }
        if (minutes < 10) {
            sj.add("0" + String.valueOf(minutes));
        } else {
            sj.add(String.valueOf(minutes));
        }
        if (seconds < 10) {
            sj.add("0" + String.valueOf(seconds));
        } else {
            sj.add(String.valueOf(seconds));
        }
        return sj.toString();
    }

    @Override
    public String getDependentCssClassName() {
        return "regattaRaceStates";
    }

    private String createRaceLink(RegattaOverviewEntryDTO entryDTO) {
        if (entryDTO.raceInfo.raceIdentifier != null && entryDTO.raceInfo.isTracked) {
            RaceboardContextDefinition raceboardContext = new RaceboardContextDefinition(entryDTO.raceInfo.raceIdentifier.getRegattaName(),
                    entryDTO.raceInfo.raceIdentifier.getRaceName(), entryDTO.leaderboardName, null, null, null, null);
            RaceBoardPerspectiveOwnSettings perspectiveOwnSettings = RaceBoardPerspectiveOwnSettings
                    .createDefaultWithCanReplayDuringLiveRaces(true);
            
            PerspectiveCompositeSettings<RaceBoardPerspectiveOwnSettings> settings = new PerspectiveCompositeSettings<>(
                    perspectiveOwnSettings, Collections.emptyMap());

            return EntryPointWithSettingsLinkFactory.createRaceBoardLink(raceboardContext, settings);
        }
        return null;
    }

    private String createRegattaLink(RegattaOverviewEntryDTO entryDTO) {
        String leaderboardLink = EntryPointLinkFactory.createLeaderboardTabLink(eventId.toString(), entryDTO.leaderboardName);
        return leaderboardLink;
    }
    public void setRepeatedInfoLabel(FlowPanel repeatedInfoLabel) {
        this.repeatedInfoLabel = repeatedInfoLabel;
    }

    @Override
    public String getId() {
        return RegattaRaceStatesComponentLifecycle.ID;
    }

}
