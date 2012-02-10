package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.domain.common.EventNameAndRaceName;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.EventDisplayer;
import com.sap.sailing.gwt.ui.client.RaceSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.RaceSelectionModel;
import com.sap.sailing.gwt.ui.client.RaceSelectionProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.URLFactory;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.RaceInLeaderboardDTO;

public class LeaderboardConfigPanel extends FormPanel implements EventDisplayer, RaceSelectionChangeListener,
        TrackedRaceChangedListener {

    private final TrackedEventsComposite trackedEventsComposite;

    private final StringMessages stringMessages;

    private final SailingServiceAsync sailingService;

    private final ListDataProvider<LeaderboardDTO> leaderboardList;

    private final ListDataProvider<RaceInLeaderboardDTO> raceColumnList;

    private final ErrorReporter errorReporter;

    private final CellTable<LeaderboardDTO> leaderboardTable;

    private final CellTable<RaceInLeaderboardDTO> raceColumnTable;

    private LeaderboardDTO selectedLeaderboard;

    private RaceInLeaderboardDTO selectedRaceInLeaderboard;

    private final Button addColumnButton;

    private final Button columnMoveUpButton;

    private final Button columnMoveDownButton;

    private final CaptionPanel selectedLeaderBoardPanel;
    private final CaptionPanel trackedRacesCaptionPanel;

    private TextBox filterLeaderboardTextbox;

    final SingleSelectionModel<RaceInLeaderboardDTO> raceColumnTableSelectionModel;

    private List<LeaderboardDTO> availableLeaderboardList;

    private final SingleSelectionModel<LeaderboardDTO> tableSelectionModel;

    private final RaceSelectionProvider raceSelectionProvider;

    public static class AnchorCell extends AbstractCell<SafeHtml> {

        @Override
        public void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml safeHtml, SafeHtmlBuilder sb) {
            sb.append(safeHtml);
        }
    }

    interface AnchorTemplates extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<a href=\"{0}\">{1}</a>")
        SafeHtml cell(String url, String displayName);
    }

    private static AnchorTemplates ANCHORTEMPLATE = GWT.create(AnchorTemplates.class);

    public LeaderboardConfigPanel(SailingServiceAsync sailingService, AdminConsoleEntryPoint adminConsole,
            final ErrorReporter errorReporter, StringMessages theStringConstants) {
        this.stringMessages = theStringConstants;
        this.sailingService = sailingService;
        leaderboardList = new ListDataProvider<LeaderboardDTO>();
        raceColumnList = new ListDataProvider<RaceInLeaderboardDTO>();
        this.errorReporter = errorReporter;
        this.availableLeaderboardList = new ArrayList<LeaderboardDTO>();
        VerticalPanel mainPanel = new VerticalPanel();
        this.setWidget(mainPanel);

        //Create leaderboards list and functionality
        CaptionPanel leaderboardsCaptionPanel = new CaptionPanel(stringMessages.leaderboards());
        leaderboardsCaptionPanel.setStyleName("bold");
        leaderboardsCaptionPanel.setWidth("50%");
        mainPanel.add(leaderboardsCaptionPanel);
        
        VerticalPanel leaderboardsPanel = new VerticalPanel();
        leaderboardsCaptionPanel.add(leaderboardsPanel);

        Label lblFilterEvents = new Label(stringMessages.filterLeaderboardsByName() + ": ");
        HorizontalPanel filterPanel = new HorizontalPanel();
        filterPanel.setSpacing(5);
        filterPanel.add(lblFilterEvents);
        filterPanel.setCellVerticalAlignment(lblFilterEvents, HasVerticalAlignment.ALIGN_MIDDLE);
        filterLeaderboardTextbox = new TextBox();
        filterLeaderboardTextbox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                fillRaceListFromAvailableLeaderboardsApplyingFilter();
            }
        });
        filterPanel.add(filterLeaderboardTextbox);
        leaderboardsPanel.add(filterPanel);

        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        leaderboardTable = new CellTable<LeaderboardDTO>(/* pageSize */200, tableRes);
        ListHandler<LeaderboardDTO> leaderboardColumnListHandler = new ListHandler<LeaderboardDTO>(
                leaderboardList.getList());

        AnchorCell anchorCell = new AnchorCell();
        Column<LeaderboardDTO, SafeHtml> linkColumn = new Column<LeaderboardDTO, SafeHtml>(anchorCell) {
            @Override
            public SafeHtml getValue(LeaderboardDTO object) {
                String debugParam = Window.Location.getParameter("gwt.codesvr");
                String link = URLFactory.INSTANCE.encode("/gwt/Leaderboard.html?name=" + object.name
                        + (debugParam != null && !debugParam.isEmpty() ? "&gwt.codesvr=" + debugParam : ""));
                return ANCHORTEMPLATE.cell(link, object.name);
            }

        };
        linkColumn.setSortable(true);
        leaderboardColumnListHandler.setComparator(linkColumn, new Comparator<LeaderboardDTO>() {

            @Override
            public int compare(LeaderboardDTO o1, LeaderboardDTO o2) {
                boolean ascending = isSortedAscending();
                if (o1.name.equals(o2.name)) {
                    return 0;
                }
                int val = -1;
                val = (o1 != null && o2 != null && ascending) ? (o1.name.compareTo(o2.name)) : -(o2.name
                        .compareTo(o1.name));
                return val;
            }

            private boolean isSortedAscending() {
                ColumnSortList sortList = leaderboardTable.getColumnSortList();
                return sortList.size() > 0 & sortList.get(0).isAscending();
            }
        });
        TextColumn<LeaderboardDTO> discardingOptionsColumn = new TextColumn<LeaderboardDTO>() {
            @Override
            public String getValue(LeaderboardDTO leaderboard) {
                String result = "";
                if (leaderboard.discardThresholds != null) {
                    for (int discardThreshold : leaderboard.discardThresholds) {
                        result += discardThreshold + " ";
                    }
                }
                return result;
            }
        };
        ImagesBarColumn<LeaderboardDTO, LeaderboardConfigImagesBarCell> leaderboardActionColumn = new ImagesBarColumn<LeaderboardDTO, LeaderboardConfigImagesBarCell>(
                new LeaderboardConfigImagesBarCell(stringMessages));
        leaderboardActionColumn.setFieldUpdater(new FieldUpdater<LeaderboardDTO, String>() {
            @Override
            public void update(int index, LeaderboardDTO object, String value) {
                if ("ACTION_REMOVE".equals(value)) {
                    if (Window.confirm("Do you really want to remove the leaderboard: '" + object.name + "' ?")) {
                        removeLeaderboard(object);
                    }
                } else if ("ACTION_EDIT".equals(value)) {
                    final String oldLeaderboardName = object.name;
                    List<LeaderboardDTO> otherExistingLeaderboard = new ArrayList<LeaderboardDTO>();
                    otherExistingLeaderboard.addAll(availableLeaderboardList);
                    otherExistingLeaderboard.remove(object);
                    LeaderboardEditDialog dialog = new LeaderboardEditDialog(Collections
                            .unmodifiableCollection(otherExistingLeaderboard), object, stringMessages, errorReporter,
                            new AsyncCallback<LeaderboardDTO>() {
                                @Override
                                public void onFailure(Throwable arg0) {
                                }

                                @Override
                                public void onSuccess(LeaderboardDTO result) {
                                    updateLeaderboard(oldLeaderboardName, result);
                                }
                            });
                    dialog.show();
                } else if ("ACTION_EDIT_SCORES".equals(value)) {
                    Window.open("/gwt/LeaderboardEditing.html?name=" + object.name, "_blank", null);
                }
            }
        });
        leaderboardTable.addColumn(linkColumn, "Name");
        leaderboardTable.addColumn(discardingOptionsColumn, "Discarding");
        leaderboardTable.addColumn(leaderboardActionColumn, "Actions");
        leaderboardTable.addColumnSortHandler(leaderboardColumnListHandler);
        leaderboardTable.setWidth("500px");
        tableSelectionModel = new SingleSelectionModel<LeaderboardDTO>();
        leaderboardTable.setSelectionModel(tableSelectionModel);
        tableSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            public void onSelectionChange(SelectionChangeEvent event) {
                selectedLeaderboard = tableSelectionModel.getSelectedObject();
                leaderboardSelectionChanged();
            }
        });
        leaderboardList.addDataDisplay(leaderboardTable);
        leaderboardsPanel.add(leaderboardTable);
        HorizontalPanel leaderboardButtonPanel = new HorizontalPanel();
        leaderboardButtonPanel.setSpacing(5);
        leaderboardsPanel.add(leaderboardButtonPanel);
        Button btnNew = new Button("Create Leaderboard...");
        leaderboardButtonPanel.add(btnNew);
        btnNew.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                addNewLeaderboard();
            }
        });

        mainPanel.add(new Grid(1, 1));

        // caption panels for the selected leaderboard and tracked races
        HorizontalPanel splitPanel = new HorizontalPanel();
        mainPanel.add(splitPanel);

        selectedLeaderBoardPanel = new CaptionPanel(stringMessages.leaderboard());
        selectedLeaderBoardPanel.setWidth("50%");
        splitPanel.add(selectedLeaderBoardPanel);

        VerticalPanel vPanel = new VerticalPanel();
        vPanel.setWidth("100%");
        selectedLeaderBoardPanel.setContentWidget(vPanel);

        trackedRacesCaptionPanel = new CaptionPanel(stringMessages.trackedRaces());
        trackedRacesCaptionPanel.setWidth("50%");
        splitPanel.add(trackedRacesCaptionPanel);

        VerticalPanel trackedRacesPanel = new VerticalPanel();
        trackedRacesPanel.setWidth("100%");
        trackedRacesCaptionPanel.setContentWidget(trackedRacesPanel);
        trackedRacesCaptionPanel.setStyleName("bold");

        raceSelectionProvider = new RaceSelectionModel();
        trackedEventsComposite = new TrackedEventsComposite(sailingService, errorReporter, adminConsole,
                raceSelectionProvider, stringMessages, /* multiselection */false);
        trackedRacesPanel.add(trackedEventsComposite);
        trackedEventsComposite.addTrackedRaceChangeListener(this);
        raceSelectionProvider.addRaceSelectionChangeListener(this);

        HorizontalPanel hPanel = new HorizontalPanel();
        hPanel.setSpacing(5);
        vPanel.add(hPanel);

        Button stressTestButton = new Button(stringMessages.stressTest());
        hPanel.add(stressTestButton);
        stressTestButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                performStressTestForSelectedLeaderboard();
            }
        });

        // ------------ races of the selected leaderboard ----------------
        AnchorCell raceAnchorCell = new AnchorCell();
        Column<RaceInLeaderboardDTO, SafeHtml> raceLinkColumn = new Column<RaceInLeaderboardDTO, SafeHtml>(raceAnchorCell) {
            @Override
            public SafeHtml getValue(RaceInLeaderboardDTO raceInLeaderboardDTO) {
                if(raceInLeaderboardDTO.getRaceIdentifier() != null) {
                    EventNameAndRaceName raceIdentifier = (EventNameAndRaceName) raceInLeaderboardDTO.getRaceIdentifier();
                    String debugParam = Window.Location.getParameter("gwt.codesvr");
                    String link = URLFactory.INSTANCE.encode("/gwt/RaceBoard.html?leaderboardName="
                            + selectedLeaderboard.name + "&raceName=" + raceIdentifier.getRaceName() + "&eventName="
                            + raceIdentifier.getEventName()
                            + (debugParam != null && !debugParam.isEmpty() ? "&gwt.codesvr=" + debugParam : ""));
                    return ANCHORTEMPLATE.cell(link, raceInLeaderboardDTO.getRaceColumnName());
                } else {
                    return SafeHtmlUtils.fromString(raceInLeaderboardDTO.getRaceColumnName());
                }
            }
        };

        Column<RaceInLeaderboardDTO, Boolean> isMedalRaceCheckboxColumn = new Column<RaceInLeaderboardDTO, Boolean>(
                new CheckboxCell()) {
            @Override
            public Boolean getValue(RaceInLeaderboardDTO race) {
                return race.isMedalRace();
            }
        };
        isMedalRaceCheckboxColumn.setFieldUpdater(new FieldUpdater<RaceInLeaderboardDTO, Boolean>() {
            @Override
            public void update(int index, RaceInLeaderboardDTO object, Boolean value) {
                setIsMedalRace(selectedLeaderboard.name, object, value);
            }
        });
        isMedalRaceCheckboxColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        TextColumn<RaceInLeaderboardDTO> isLinkedRaceColumn = new TextColumn<RaceInLeaderboardDTO>() {
            @Override
            public String getValue(RaceInLeaderboardDTO race) {
                boolean isTrackedRace = race.isTrackedRace();
                return isTrackedRace ? stringMessages.yes() : stringMessages.no();
            }
        };

        ImagesBarColumn<RaceInLeaderboardDTO, LeaderboardRaceConfigImagesBarCell> raceActionColumn = new ImagesBarColumn<RaceInLeaderboardDTO, LeaderboardRaceConfigImagesBarCell>(
                new LeaderboardRaceConfigImagesBarCell(stringMessages));

        raceActionColumn.setFieldUpdater(new FieldUpdater<RaceInLeaderboardDTO, String>() {
            @Override
            public void update(int index, RaceInLeaderboardDTO object, String value) {
                if ("ACTION_REMOVE".equals(value)) {
                    if (Window.confirm(stringMessages.reallyRemoveRace(object.getRaceColumnName()))) {
                        removeRaceColumn(object);
                    }
                } else if ("ACTION_EDIT".equals(value)) {
                    editRaceColumnOfLeaderboard(object);
                } else if ("ACTION_UNLINK".equals(value)) {
                    unlinkRaceColumnFromTrackedRace(object.getRaceColumnName());
                }
            }
        });

        Label lblRaceNamesIn = new Label(stringMessages.races());
        vPanel.add(lblRaceNamesIn);

        raceColumnTable = new CellTable<RaceInLeaderboardDTO>(/* pageSize */200, tableRes);
        raceColumnTable.addColumn(raceLinkColumn, stringMessages.name());
        raceColumnTable.addColumn(isMedalRaceCheckboxColumn, stringMessages.medalRace());
        raceColumnTable.addColumn(isLinkedRaceColumn, stringMessages.islinked());
        raceColumnTable.addColumn(raceActionColumn, stringMessages.actions());
        raceColumnList.addDataDisplay(raceColumnTable);
        raceColumnTable.setWidth("500px");
        raceColumnTableSelectionModel = new SingleSelectionModel<RaceInLeaderboardDTO>();

        raceColumnTable.setSelectionModel(raceColumnTableSelectionModel);

        raceColumnTableSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            public void onSelectionChange(SelectionChangeEvent event) {
                leaderboardRaceColumnSelectionChanged();
            }
        });

        vPanel.add(raceColumnTable);

        HorizontalPanel selectedLeaderboardRaceButtonPanel = new HorizontalPanel();
        selectedLeaderboardRaceButtonPanel.setSpacing(5);
        vPanel.add(selectedLeaderboardRaceButtonPanel);

        addColumnButton = new Button("Add race...");
        selectedLeaderboardRaceButtonPanel.add(addColumnButton);
        addColumnButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addRaceColumnToLeaderboard();
            }
        });

        columnMoveUpButton = new Button(stringMessages.columnMoveUp());
        selectedLeaderboardRaceButtonPanel.add(columnMoveUpButton);
        columnMoveUpButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveSelectedRaceColumnUp();
            }
        });
        columnMoveDownButton = new Button(stringMessages.columnMoveDown());
        selectedLeaderboardRaceButtonPanel.add(columnMoveDownButton);
        columnMoveDownButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveSelectedRaceColumnDown();
            }
        });

        loadAndRefreshAllData();
    }

    public void loadAndRefreshAllData() {
        sailingService.getLeaderboards(new AsyncCallback<List<LeaderboardDTO>>() {
            @Override
            public void onSuccess(List<LeaderboardDTO> leaderboards) {
                leaderboardList.getList().clear();
                availableLeaderboardList.clear();
                leaderboardList.getList().addAll(leaderboards);
                availableLeaderboardList.addAll(leaderboards);
                
                leaderboardSelectionChanged();
                leaderboardRaceColumnSelectionChanged();
            }

            @Override
            public void onFailure(Throwable t) {
                LeaderboardConfigPanel.this.errorReporter.reportError("Error trying to obtain list of leaderboards: "
                        + t.getMessage());
            }
        });

    }
    
    private void performStressTestForSelectedLeaderboard() {
        if (selectedLeaderboard != null) {
            final String leaderboardName = selectedLeaderboard.name;
            sailingService.stressTestLeaderboardByName(leaderboardName, 100, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error during leaderboard stress test for " + leaderboardName + ": "
                            + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    errorReporter.reportError("Stress test for " + leaderboardName + " finished successfully ");
                }
            });
        }
    }

    private void unlinkRaceColumnFromTrackedRace(final String raceColumn) {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        sailingService.disconnectLeaderboardColumnFromTrackedRace(selectedLeaderboardName, raceColumn,
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to unlink tracked race from column " + raceColumn
                                + " from leaderboard " + selectedLeaderboardName + ": " + t.getMessage());
                    }

                    @Override
                    public void onSuccess(Void arg0) {
                        trackedEventsComposite.clearSelection();
                        getSelectedRaceInLeaderboard().setRaceIdentifier(null);
                        raceColumnList.refresh();
                    }
                });
    }

    private void removeRaceColumn(final RaceInLeaderboardDTO raceColumn) {
        final String raceColumnString = raceColumn.getRaceColumnName();
        sailingService.removeLeaderboardColumn(getSelectedLeaderboardName(), raceColumnString,
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to remove leaderboard race column " + raceColumn
                                + " in leaderboard " + getSelectedLeaderboardName() + ": " + t.getMessage());
                    }

                    @Override
                    public void onSuccess(Void arg0) {
                        raceColumnList.getList().remove(raceColumn);
                        if (raceColumnTableSelectionModel.isSelected(raceColumn)) {
                            raceColumnTableSelectionModel.setSelected(raceColumn, false);
                        }
                        // selectedLeaderboard.raceNamesAndMedalRaceAndTracked.remove(raceColumn);
                        selectedLeaderboard.removeRace(raceColumnString);
                        selectedLeaderboard.invalidateCompetitorOrdering();
                    }
                });
    }

    private void moveSelectedRaceColumnDown() {
        final String selectedRaceColumnName = raceColumnTableSelectionModel.getSelectedObject().getRaceColumnName();

        sailingService.moveLeaderboardColumnDown(getSelectedLeaderboardName(), selectedRaceColumnName,
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to move leaderboard race column "
                                + selectedRaceColumnName + " in leaderboard " + getSelectedLeaderboardName()
                                + " down: " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        int rowIndex = getRowIndexOfSelectedRaceColumn();
                        if (rowIndex < raceColumnList.getList().size() - 1) {
                            Collections.swap(raceColumnList.getList(), rowIndex, rowIndex + 1);
                            selectedLeaderboard.moveRaceDown(selectedRaceColumnName);
                        }
                    }
                });
    }

    private int getRowIndexOfSelectedRaceColumn() {
        final RaceInLeaderboardDTO selectedRaceColumnName = raceColumnTableSelectionModel.getSelectedObject();
        int rowIndex = -1;

        if (selectedRaceColumnName != null) {
            int listSize = raceColumnList.getList().size();
            for (int i = 0; i < listSize; i++) {
                if (selectedRaceColumnName.equals(raceColumnList.getList().get(i))) {
                    rowIndex = i;
                    break;
                }
            }
        }
        return rowIndex;
    }

    private void moveSelectedRaceColumnUp() {
        final String selectedRaceColumnName = raceColumnTableSelectionModel.getSelectedObject().getRaceColumnName();

        sailingService.moveLeaderboardColumnUp(getSelectedLeaderboardName(), selectedRaceColumnName,
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to move leaderboard race column "
                                + selectedRaceColumnName + " in leaderboard " + getSelectedLeaderboardName() + " up: "
                                + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        int rowIndex = getRowIndexOfSelectedRaceColumn();
                        if (rowIndex > 0) {
                            Collections.swap(raceColumnList.getList(), rowIndex, rowIndex - 1);
                            selectedLeaderboard.moveRaceUp(selectedRaceColumnName);
                        }
                    }
                });
    }

    private void leaderboardRaceColumnSelectionChanged() {
        selectedRaceInLeaderboard = getSelectedRaceInLeaderboard();
        if (selectedRaceInLeaderboard != null) {
            columnMoveUpButton.setEnabled(true);
            columnMoveDownButton.setEnabled(true);
            reloadRaceInLeaderboardRow(selectedRaceInLeaderboard.getRaceColumnName(), selectedRaceInLeaderboard);
            selectTrackedRaceInRaceList();
        } else {
            columnMoveUpButton.setEnabled(false);
            columnMoveDownButton.setEnabled(false);
            trackedEventsComposite.clearSelection();
        }
    }

    private void selectTrackedRaceInRaceList() {
        final String selectedLeaderboardName = getSelectedLeaderboardName();
        final String selectedRaceColumnName = getSelectedRaceInLeaderboard().getRaceColumnName();
        sailingService.getEventAndRaceNameOfTrackedRaceConnectedToLeaderboardColumn(selectedLeaderboardName,
                selectedRaceColumnName, new AsyncCallback<Pair<String, String>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to determine tracked race linked to race column "
                                + selectedRaceColumnName + " in leaderboard " + selectedLeaderboardName + ": "
                                + t.getMessage());
                    }

                    @Override
                    public void onSuccess(Pair<String, String> eventAndRaceName) {
                        if (eventAndRaceName != null) {
                            selectRaceInList(eventAndRaceName.getA(), eventAndRaceName.getB());
                        } else {
                            trackedEventsComposite.clearSelection();
                        }
                    }
                });
    }

    private void selectRaceInList(String eventName, String raceName) {
        EventNameAndRaceName raceIdentifier = new EventNameAndRaceName(eventName, raceName);  
        trackedEventsComposite.selectRaceByIdentifier(raceIdentifier);
    }

    private RaceInLeaderboardDTO getSelectedRaceInLeaderboard() {
        RaceInLeaderboardDTO raceInLeaderboard = raceColumnTableSelectionModel.getSelectedObject();
        return raceInLeaderboard;
    }

    private void editRaceColumnOfLeaderboard(final RaceInLeaderboardDTO raceInLeaderboard) {
        final String oldRaceName = raceInLeaderboard.getRaceColumnName();
        List<RaceInLeaderboardDTO> existingRacesWithoutThisRace = new ArrayList<RaceInLeaderboardDTO>();
        existingRacesWithoutThisRace.addAll(raceColumnList.getList());
        existingRacesWithoutThisRace.remove(raceInLeaderboard);
        final RaceInLeaderboardDialog raceDialog = new RaceInLeaderboardDialog(existingRacesWithoutThisRace,
                raceInLeaderboard, stringMessages, new AsyncCallback<RaceInLeaderboardDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                    }

                    @Override
                    public void onSuccess(final RaceInLeaderboardDTO result) {
                        sailingService.renameLeaderboardColumn(getSelectedLeaderboardName(), oldRaceName,
                                result.getRaceColumnName(), new AsyncCallback<Void>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                    }

                                    @Override
                                    public void onSuccess(Void v) {
                                        sailingService.updateIsMedalRace(getSelectedLeaderboardName(),
                                                result.getRaceColumnName(), result.isMedalRace(),
                                                new AsyncCallback<Void>() {

                                                    @Override
                                                    public void onFailure(Throwable caught) {
                                                    }

                                                    @Override
                                                    public void onSuccess(Void v) {
                                                        raceInLeaderboard.setMedalRace(result.isMedalRace());
                                                        reloadRaceInLeaderboardRow(oldRaceName, result);
                                                    }
                                                });
                                    }
                                });
                    }

                });
        raceDialog.show();
    }

    private void reloadRaceInLeaderboardRow(String oldRaceName, RaceInLeaderboardDTO raceInLeaderboard) {
        int index = -1;
        for (int i = 0; i < raceColumnList.getList().size(); i++) {
            RaceInLeaderboardDTO race = raceColumnList.getList().get(i);
            if (race.getRaceColumnName().equals(oldRaceName)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            raceColumnList.getList().set(index, raceInLeaderboard);
        }
        raceColumnList.refresh();
    }

    private void setIsMedalRace(String leaderboardName, final RaceInLeaderboardDTO raceInLeaderboard,
            final boolean isMedalRace) {
        sailingService.updateIsMedalRace(leaderboardName, raceInLeaderboard.getRaceColumnName(), isMedalRace,
                new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable caught) {
                    }

                    @Override
                    public void onSuccess(Void result) {
                        selectedLeaderboard.setIsMedalRace(raceInLeaderboard.getRaceColumnName(), isMedalRace);
                    }
                });
    }

    private void addRaceColumnToLeaderboard() {
        final RaceInLeaderboardDTO raceInLeaderboard = new RaceInLeaderboardDTO();
        final String leaderboardName = getSelectedLeaderboardName();
        final RaceInLeaderboardDialog raceDialog = new RaceInLeaderboardDialog(raceColumnList.getList(),
                raceInLeaderboard, stringMessages, new AsyncCallback<RaceInLeaderboardDTO>() {

                    @Override
                    public void onFailure(Throwable caught) {
                    }

                    @Override
                    public void onSuccess(final RaceInLeaderboardDTO result) {
                        sailingService.addColumnToLeaderboard(result.getRaceColumnName(), leaderboardName,
                                result.isMedalRace(), new AsyncCallback<Void>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError("Error trying to add column "
                                                + result.getRaceColumnName() + " to leaderboard " + leaderboardName
                                                + ": " + caught.getMessage());

                                    }

                                    @Override
                                    public void onSuccess(Void v) {
                                        // columnNamesInSelectedLeaderboardListBox.addItem(columnNameAndMedalRace.getA());
                                        raceColumnList.getList().add(result);
                                        // selectedLeaderboard.raceNamesAndMedalRaceAndTracked.put(columnNameAndMedalRace.getA(),
                                        // new Pair<Boolean, Boolean>(/* medal race */ columnNameAndMedalRace.getB(),
                                        // /* tracked */ false));
                                        selectedLeaderboard.addRace(result.getRaceColumnName(), result.isMedalRace(),
                                                null);
                                        selectedLeaderboard.invalidateCompetitorOrdering();
                                    }
                                });
                    }
                });
        raceDialog.show();
    }

    private String getSelectedLeaderboardName() {
        return selectedLeaderboard != null ? selectedLeaderboard.name : null;
    }

    private void leaderboardSelectionChanged() {
        // make sure that clearing the selection doesn't cause an unlinking of the selected tracked race
        raceSelectionProvider.removeRaceSelectionChangeListener(this);
        trackedEventsComposite.clearSelection();
        // add listener again using a scheduled command which is executed when the browser's event loop re-gains
        // control; we assume that at that point in time the selection updates have already been performed
        Scheduler.get().scheduleFinally(new ScheduledCommand() {
            @Override
            public void execute() {
                raceSelectionProvider.addRaceSelectionChangeListener(LeaderboardConfigPanel.this);
            }
        });
        if (selectedLeaderboard != null) {
                    raceColumnList.getList().clear();
                    raceColumnList.getList().addAll(selectedLeaderboard.getRaceList());
                    selectedLeaderBoardPanel.setVisible(true);
                    trackedRacesCaptionPanel.setVisible(true);
                    selectedLeaderBoardPanel.setCaptionText("Details of leaderboard '" + selectedLeaderboard.name + "'");
                    addColumnButton.setEnabled(true);
        } else {
            selectedLeaderBoardPanel.setVisible(false);
            trackedRacesCaptionPanel.setVisible(false);
            selectedLeaderboard = null;
            addColumnButton.setEnabled(false);
            leaderboardRaceColumnSelectionChanged();
        }
    }

    @Override
    public void fillEvents(List<EventDTO> result) {
        trackedEventsComposite.fillEvents(result);
    }

    @Override
    public void changeTrackingRace(EventNameAndRaceName eventNameAndRaceName, boolean isTracked) {
        for (RaceInLeaderboardDTO race : raceColumnList.getList()) {
            if (race.getRaceColumnName().equals(eventNameAndRaceName.getRaceName())) {
                race.setRaceIdentifier(eventNameAndRaceName);
            }
        }
        raceColumnList.refresh();
    }

    private void addNewLeaderboard() {
        LeaderboardCreateDialog dialog = new LeaderboardCreateDialog(Collections.unmodifiableCollection(availableLeaderboardList)
                , stringMessages, errorReporter, new AsyncCallback<LeaderboardDTO>() {
            @Override
            public void onFailure(Throwable arg0) {
            }

            @Override
            public void onSuccess(LeaderboardDTO result) {
                createNewLeaderboard(result);
            }
        });
        dialog.show();
    }

    private void createNewLeaderboard(final LeaderboardDTO newLeaderboard) {
        sailingService.createLeaderboard(newLeaderboard.name, newLeaderboard.discardThresholds,
                new AsyncCallback<LeaderboardDTO>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to create new leaderboard " + newLeaderboard.name
                                + ": " + t.getMessage());
                    }

                    @Override
                    public void onSuccess(LeaderboardDTO result) {
                        leaderboardList.getList().add(result);
                        availableLeaderboardList.add(result);
                        selectedLeaderboard = result;
                        leaderboardSelectionChanged();
                    }
                });
    }

    private void updateLeaderboard(final String oldLeaderboardName, final LeaderboardDTO leaderboardToUdate) {
        sailingService.updateLeaderboard(oldLeaderboardName, leaderboardToUdate.name,
                leaderboardToUdate.discardThresholds, new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to update leaderboard " + oldLeaderboardName + ": "
                                + t.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        for (int i = 0; i < leaderboardList.getList().size(); i++) {
                            LeaderboardDTO dao = leaderboardList.getList().get(i);
                            if (dao.name.equals(oldLeaderboardName)) {
                                leaderboardList.getList().set(i, leaderboardToUdate);
                                availableLeaderboardList.set(i, leaderboardToUdate);
                            }
                        }
                        leaderboardList.refresh();
                    }
                });
    }

    private void removeLeaderboard(final LeaderboardDTO leaderBoard) {
        sailingService.removeLeaderboard(leaderBoard.name, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error trying to remove leaderboard " + leaderBoard.name + ": "
                        + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                // check if the removed leaderboard was the selected one
                leaderboardList.getList().remove(leaderBoard);
                availableLeaderboardList.remove(leaderBoard);

                if (selectedLeaderboard != null && selectedLeaderboard.name.equals(leaderBoard.name)) {
                    selectedLeaderboard = null;
                    leaderboardSelectionChanged();
                }
            }
        });
    }

    @Override
    public void onRaceSelectionChange(List<RaceIdentifier> selectedRaces) {
        // if no leaderboard column is selected, ignore the race selection change
        RaceInLeaderboardDTO selectedRaceInLeaderboard = getSelectedRaceInLeaderboard();
        if (selectedRaceInLeaderboard != null) {
            if (selectedRaces.isEmpty()) {
                unlinkRaceColumnFromTrackedRace(selectedRaceInLeaderboard.getRaceColumnName());
            } else {
                linkTrackedRaceToSelectedRaceColumn(selectedRaceInLeaderboard, selectedRaces.iterator().next());
            }
        }
    }

    private void linkTrackedRaceToSelectedRaceColumn(final RaceInLeaderboardDTO selectedRaceInLeaderboard,
            final RaceIdentifier selectedRace) {
        sailingService.connectTrackedRaceToLeaderboardColumn(getSelectedLeaderboardName(), selectedRaceInLeaderboard
                .getRaceColumnName(), selectedRace,
                new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to link tracked race " + selectedRace + " to race column named "
                                + selectedRaceInLeaderboard.getRaceColumnName() + " of leaderboard "
                                + getSelectedLeaderboardName() + ": " + t.getMessage());
                        trackedEventsComposite.clearSelection();
                    }

                    @Override
                    public void onSuccess(Boolean success) {
                        if (success) {
                            // TODO consider enabling the Unlink button
                            selectedRaceInLeaderboard.setRaceIdentifier(selectedRace);
                            raceColumnList.refresh();
                        }
                    }
                });
    }

    private void fillRaceListFromAvailableLeaderboardsApplyingFilter() {
        String text = filterLeaderboardTextbox.getText();
        List<String> wordsToFilter = Arrays.asList(text.split(" "));
        leaderboardList.getList().clear();
        if (text != null && !text.isEmpty()) {
            for (LeaderboardDTO dao : availableLeaderboardList) {
                boolean failed = false;
                for (String word : wordsToFilter) {
                    String textAsUppercase = word.toUpperCase().trim();
                    if (!dao.name.toUpperCase().contains(textAsUppercase)) {
                        failed = true;
                        break;
                    }
                }
                if (!failed) {
                    leaderboardList.getList().add(dao);
                }
            }
        } else {
            leaderboardList.getList().addAll(availableLeaderboardList);
        }
        // now sort again according to selected criterion
        ColumnSortEvent.fire(leaderboardTable, leaderboardTable.getColumnSortList());
    }
}