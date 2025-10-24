package com.sap.sailing.gwt.ui.spectator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.PlacemarkOrderDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.settings.client.spectator.SpectatorContextDefinition;
import com.sap.sailing.gwt.settings.client.spectator.SpectatorSettings;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardConfigPanel.AnchorCell;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.RaceBoardPanel;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.URLEncoder;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.shared.components.CollapsablePanel;
import com.sap.sse.gwt.client.shared.components.LinkWithSettingsGenerator;

/**
 * 
 * @author Lennart Hensler (D054527)
 * 
 */
public class LeaderboardGroupOverviewPanel extends FormPanel {

    interface AnchorTemplates extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<a href=\"{0}\">{1}</a>")
        SafeHtml anchor(SafeUri url, String displayName);
    }

    public static final String STYLE_NAME_PREFIX = "groupOverviewPanel-";
    private static final AnchorTemplates ANCHORTEMPLATE = GWT.create(AnchorTemplates.class);
    
    private final SailingServiceAsync sailingService;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;

    private TextBox locationTextBox;
    private TextBox nameTextBox;
    private TextBox fromTextBox;
    private TextBox untilTextBox;
    private CheckBox onlyLiveCheckBox;
    
    private CellTable<LeaderboardGroupDTO> groupsTable;
    private ListDataProvider<LeaderboardGroupDTO> groupsDataProvider;
    private SingleSelectionModel<LeaderboardGroupDTO> groupsSelectionModel;
    
    private CollapsablePanel collapsableDetailsPanel;
    private Label noGroupSelectedLabel;
    
    private FlowPanel groupDetailsPanel;
    private HTML groupDescriptionHTML;
    private CellTable<StrippedLeaderboardDTO> leaderboardsTable;
    private ListDataProvider<StrippedLeaderboardDTO> leaderboardsDataProvider;
    private SingleSelectionModel<StrippedLeaderboardDTO> leaderboardsSelectionModel;
    
    private FlowPanel leaderboardDetailsPanel;
    private CellTable<RaceColumnDTO> racesTable;
    private NoSelectionModel<RaceColumnDTO> racesSelectionModel;
    private ListDataProvider<RaceColumnDTO> racesDataProvider;
    
    private List<LeaderboardGroupDTO> availableGroups;
    private final boolean showRaceDetails;
    private final Timer timerForClientServerOffset;

    public LeaderboardGroupOverviewPanel(SailingServiceAsync sailingService, ErrorReporter errorReporter,
            StringMessages stringMessages, boolean showRaceDetails) {
        this.showRaceDetails = showRaceDetails;
        this.sailingService = sailingService;
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        this.timerForClientServerOffset = new Timer(PlayModes.Replay);
        availableGroups = new ArrayList<LeaderboardGroupDTO>();
        
        LeaderboardGroupOverviewTableResources tableResources = GWT.create(LeaderboardGroupOverviewTableResources.class);

        FlowPanel mainPanel = new FlowPanel();
        this.setWidget(mainPanel);
        mainPanel.setSize("100%", "100%");
        
        mainPanel.add(createSearchGUI());
        mainPanel.add(createGroupsGUI(tableResources));
        mainPanel.add(createDetailsGUI(tableResources));
        
        loadGroups();
    }
    
    private CollapsablePanel createSearchGUI() {
        FlowPanel searchPanel = new FlowPanel();
        searchPanel.setWidth("100%");
        
        CollapsablePanel collapsableSearchPanel = new CollapsablePanel(this.stringMessages.searchEvents(), false);
        collapsableSearchPanel.setContent(searchPanel);
        collapsableSearchPanel.setCollapsingEnabled(false);
        collapsableSearchPanel.setOpen(true);
        collapsableSearchPanel.setWidth("100%");
        
        Label locationLabel = new Label(this.stringMessages.location() + ":");
        locationLabel.setStyleName(STYLE_NAME_PREFIX + "searchLabel");
        locationLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
        searchPanel.add(locationLabel);
        locationTextBox = new TextBox();
        locationTextBox.setStyleName(STYLE_NAME_PREFIX + "searchInputField");
        locationTextBox.getElement().getStyle().setFloat(Style.Float.LEFT);
        locationTextBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                onSearchCriteriaChange();
            }
        });
        searchPanel.add(locationTextBox);
        
        Label nameLabel = new Label(this.stringMessages.name() + ":");
        nameLabel.setStyleName(STYLE_NAME_PREFIX + "searchLabel");
        nameLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
        searchPanel.add(nameLabel);
        nameTextBox = new TextBox();
        nameTextBox.setStyleName(STYLE_NAME_PREFIX + "searchInputField");
        nameTextBox.getElement().getStyle().setFloat(Style.Float.LEFT);
        nameTextBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                onSearchCriteriaChange();
            }
        });
        searchPanel.add(nameTextBox);
        
        onlyLiveCheckBox = new CheckBox(this.stringMessages.onlyLiveEvents());
        onlyLiveCheckBox.setStyleName(STYLE_NAME_PREFIX + "searchCheckBox");
        onlyLiveCheckBox.getElement().getStyle().setFloat(Style.Float.LEFT);
        onlyLiveCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                onCheckBoxLiveChange();
            }
        });
        searchPanel.add(onlyLiveCheckBox);
        
        Label fromDateLabel = new Label(this.stringMessages.from() + ":");
        fromDateLabel.setStyleName(STYLE_NAME_PREFIX + "searchLabel");
        fromDateLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
        searchPanel.add(fromDateLabel);
        fromTextBox = new TextBox();
        fromTextBox.setStyleName(STYLE_NAME_PREFIX + "searchInputField");
        fromTextBox.getElement().getStyle().setFloat(Style.Float.LEFT);
        fromTextBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                onSearchCriteriaChange();
            }
        });
        searchPanel.add(fromTextBox);
        
        Label toDateLabel = new Label(this.stringMessages.until() + ":");
        toDateLabel.setStyleName(STYLE_NAME_PREFIX + "searchLabel");
        toDateLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
        searchPanel.add(toDateLabel);
        untilTextBox = new TextBox();
        untilTextBox.setStyleName(STYLE_NAME_PREFIX + "searchInputField");
        untilTextBox.getElement().setPropertyString("clear", "right");
        untilTextBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                onSearchCriteriaChange();
            }
        });
        searchPanel.add(untilTextBox);
        
        return collapsableSearchPanel;
    }
    
    private CollapsablePanel createGroupsGUI(LeaderboardGroupOverviewTableResources tableResources) {
        FlowPanel groupPanel = new FlowPanel();
        groupPanel.setStyleName(STYLE_NAME_PREFIX + "groupPanel");
        
        CollapsablePanel collapsableGroupPanel = new CollapsablePanel(this.stringMessages.leaderboardGroups(), false);
        collapsableGroupPanel.setContent(groupPanel);
        collapsableGroupPanel.setOpen(true);
        collapsableGroupPanel.setWidth("100%");
        
        TextColumn<LeaderboardGroupDTO> groupsLocationColumn = new TextColumn<LeaderboardGroupDTO>() {
            @Override
            public String getValue(LeaderboardGroupDTO group) {
                List<PlacemarkOrderDTO> groupPlaces = group.getGroupPlaces();
                return groupPlaces.isEmpty() ? LeaderboardGroupOverviewPanel.this.stringMessages.notAvailable()
                        : PlacemarkOrderDTO.placemarksOfAllOrderAsSeperatedString(groupPlaces, true);
            }
        };
        groupsLocationColumn.setSortable(true);
        
        AnchorCell groupsNameAnchorCell = new AnchorCell();
        Column<LeaderboardGroupDTO, SafeHtml> groupsNameColumn = new Column<LeaderboardGroupDTO, SafeHtml>(groupsNameAnchorCell) {
            @Override
            public SafeHtml getValue(LeaderboardGroupDTO group) {
                String link = new LinkWithSettingsGenerator<SpectatorSettings>(
                        new SpectatorContextDefinition(group.getId().toString()))
                                .createUrl(new SpectatorSettings(showRaceDetails));
                return ANCHORTEMPLATE.anchor(UriUtils.fromString(link), group.getName().toString());
            }
        };
        groupsNameColumn.setSortable(true);
        
        TextColumn<LeaderboardGroupDTO> groupsLeaderboardsColumn = new TextColumn<LeaderboardGroupDTO>() {
            @Override
            public String getValue(LeaderboardGroupDTO group) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (StrippedLeaderboardDTO leaderboard : group.leaderboards) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(leaderboard.getName());
                    first = false;
                }
                return sb.toString();
            }
        };
        groupsLeaderboardsColumn.setSortable(false);
        
        TextColumn<LeaderboardGroupDTO> groupsStartDateColumn = new TextColumn<LeaderboardGroupDTO>() {
            @Override
            public String getValue(LeaderboardGroupDTO group) {
                Date startDate = group.getGroupStartDate();
                return startDate == null ? LeaderboardGroupOverviewPanel.this.stringMessages.untracked()
                        : DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT).format(startDate);
            }
        };
        groupsStartDateColumn.setSortable(true);
        
        groupsTable = new BaseCelltable<LeaderboardGroupDTO>(10000, tableResources);
        groupsTable.setWidth("100%");
        groupsSelectionModel = new SingleSelectionModel<LeaderboardGroupDTO>();
        groupsSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                onGroupSelectionChanged();
            }
        });
        groupsTable.setSelectionModel(groupsSelectionModel);
        
        groupsTable.addColumn(groupsLocationColumn, this.stringMessages.location());
        groupsTable.addColumn(groupsNameColumn, this.stringMessages.name());
        groupsTable.addColumn(groupsLeaderboardsColumn, this.stringMessages.leaderboards());
        groupsTable.addColumn(groupsStartDateColumn, this.stringMessages.startDate());
        groupPanel.add(groupsTable);
        
        groupsDataProvider = new ListDataProvider<LeaderboardGroupDTO>();
        groupsDataProvider.addDataDisplay(groupsTable);

        ListHandler<LeaderboardGroupDTO> groupsListHandler = new ListHandler<LeaderboardGroupDTO>(groupsDataProvider.getList());
        groupsListHandler.setComparator(groupsLocationColumn, new Comparator<LeaderboardGroupDTO>() {
            @Override
            public int compare(LeaderboardGroupDTO g1, LeaderboardGroupDTO g2) {
                String places1 = PlacemarkOrderDTO.placemarksOfAllOrderAsSeperatedString(g1.getGroupPlaces(), true);
                String places2 = PlacemarkOrderDTO.placemarksOfAllOrderAsSeperatedString(g2.getGroupPlaces(), true);
                return places1.compareTo(places2);
            }
        });
        groupsListHandler.setComparator(groupsNameColumn, new Comparator<LeaderboardGroupDTO>() {
            @Override
            public int compare(LeaderboardGroupDTO g1, LeaderboardGroupDTO g2) {
                return new NaturalComparator(false).compare(g1.getName(), g2.getName());
            }
        });
        groupsListHandler.setComparator(groupsStartDateColumn, new Comparator<LeaderboardGroupDTO>() {
            @Override
            public int compare(LeaderboardGroupDTO g1, LeaderboardGroupDTO g2) {
                Date d1 = g1.getGroupStartDate();
                Date d2 = g2.getGroupStartDate();
                if (d1 == null || d2 == null) {
                    return d1 == null && d2 == null ? 0 : d1 == null ? -1 : 1;
                } else {
                    return g1.getGroupStartDate().compareTo(g2.getGroupStartDate());
                }
            }
        });
        groupsTable.addColumnSortHandler(groupsListHandler);
        
        return collapsableGroupPanel;
    }

    private CollapsablePanel createDetailsGUI(LeaderboardGroupOverviewTableResources tableResources) {
        FlowPanel detailsPanel = new FlowPanel();
        detailsPanel.setStyleName(STYLE_NAME_PREFIX + "detailsPanel");
        
        collapsableDetailsPanel = new CollapsablePanel(this.stringMessages.details(), false);
        collapsableDetailsPanel.setContent(detailsPanel);
        collapsableDetailsPanel.setWidth("100%");
        
        noGroupSelectedLabel = new Label(this.stringMessages.noGroupSelected());
        detailsPanel.add(noGroupSelectedLabel);
        
        //Build group details GUI
        groupDetailsPanel = new FlowPanel();
        groupDetailsPanel.setVisible(false);
        groupDetailsPanel.setSize("100%", "100%");
        groupDetailsPanel.setStyleName(STYLE_NAME_PREFIX + "groupDetailsPanel");
        detailsPanel.add(groupDetailsPanel);
        
        groupDescriptionHTML = new HTML();
        groupDescriptionHTML.setStyleName(STYLE_NAME_PREFIX + "groupDescription");
        groupDetailsPanel.add(groupDescriptionHTML);
        
        Label groupDetailsLabel = new Label(stringMessages.leaderboardsInGroup()+ ": ");
        groupDetailsLabel.getElement().getStyle().setPadding(5, Unit.PX);
        groupDetailsPanel.add(groupDetailsLabel);
        
        TextColumn<StrippedLeaderboardDTO> leaderboardsLocationColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                PlacemarkOrderDTO leaderboardPlaces = leaderboard.getPlaces();
                return leaderboardPlaces == null ? LeaderboardGroupOverviewPanel.this.stringMessages
                        .notAvailable() : leaderboardPlaces.placemarksAsString();
            }
        };
        
        AnchorCell leaderboardsNameAnchorCell = new AnchorCell();
        Column<StrippedLeaderboardDTO, SafeHtml> leaderboardsNameColumn = new Column<StrippedLeaderboardDTO, SafeHtml>(leaderboardsNameAnchorCell) {
            @Override
            public SafeHtml getValue(StrippedLeaderboardDTO leaderboard) {
                LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
                String debugParam = Window.Location.getParameter("gwt.codesvr");
                String link = "/gwt/Leaderboard.html?name=" + URLEncoder.encodeQueryString(leaderboard.getName())
                        + (showRaceDetails ? "&showRaceDetails=true" : "") + "&root=overview" + "&leaderboardGroupId="
                        + selectedGroup.getId().toString()
                        + (debugParam != null && !debugParam.isEmpty() ? "&gwt.codesvr=" + URLEncoder.encodeQueryString(debugParam) : "");
                return ANCHORTEMPLATE.anchor(UriUtils.fromString(link), leaderboard.getName());
            }
        };
        
        TextColumn<StrippedLeaderboardDTO> leaderboardsRacesColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (RaceColumnDTO race : leaderboard.getRaceList()) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(race.getRaceColumnName());
                    first = false;
                }
                return sb.toString();
            }
        };
        
        TextColumn<StrippedLeaderboardDTO> leaderboardsStartDateColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                Date leaderboardStart = leaderboard.getStartDate();
                return leaderboardStart == null ? LeaderboardGroupOverviewPanel.this.stringMessages.untracked()
                        : DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT).format(leaderboardStart);
            }
        };
        
        leaderboardsTable = new BaseCelltable<StrippedLeaderboardDTO>(10000, tableResources);
        leaderboardsTable.setWidth("100%");
        leaderboardsSelectionModel = new SingleSelectionModel<StrippedLeaderboardDTO>();
        leaderboardsSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                onLeaderboardSelectionChanged();
            }
        });
        leaderboardsTable.setSelectionModel(leaderboardsSelectionModel);
        
        leaderboardsTable.addColumn(leaderboardsLocationColumn, this.stringMessages.location());
        leaderboardsTable.addColumn(leaderboardsNameColumn, this.stringMessages.name());
        leaderboardsTable.addColumn(leaderboardsRacesColumn, this.stringMessages.races());
        leaderboardsTable.addColumn(leaderboardsStartDateColumn, this.stringMessages.startDate());
        groupDetailsPanel.add(leaderboardsTable);
        
        leaderboardsDataProvider = new ListDataProvider<StrippedLeaderboardDTO>();
        leaderboardsDataProvider.addDataDisplay(leaderboardsTable);

        //Build leaderboard details GUI
        leaderboardDetailsPanel = new FlowPanel();
        leaderboardDetailsPanel.setSize("100%", "100%");
        leaderboardDetailsPanel.setStyleName(STYLE_NAME_PREFIX + "leaderboardDetailsPanel");
        leaderboardDetailsPanel.setVisible(false);
        leaderboardDetailsPanel.getElement().setPropertyString("clear", "right");
        detailsPanel.add(leaderboardDetailsPanel);
        
        Label leaderboardDetailsLabel = new Label(stringMessages.racesInLeaderboard() + ":");
        leaderboardDetailsLabel.getElement().getStyle().setPadding(5, Unit.PX);
        leaderboardDetailsPanel.add(leaderboardDetailsLabel);
        
        TextColumn<RaceColumnDTO> racesLocationColumn = new TextColumn<RaceColumnDTO>() {
            @Override
            public String getValue(RaceColumnDTO race) {
                PlacemarkOrderDTO racePlaces = race.getPlaces();
                return racePlaces == null ? LeaderboardGroupOverviewPanel.this.stringMessages.locationNotAvailable() : racePlaces.placemarksAsString();
            }
        };
        
        AnchorCell racesNameAnchorCell = new AnchorCell();
        Column<RaceColumnDTO, SafeHtml> racesNameColumn = new Column<RaceColumnDTO, SafeHtml>(racesNameAnchorCell) {
            @Override
            public SafeHtml getValue(RaceColumnDTO race) {
                SafeHtml name = null;
                Iterable<FleetDTO> fleets = race.getFleets();
                boolean singleFleet = Util.size(fleets) < 2;
                for (FleetDTO fleet : fleets) {
                    String raceDisplayName;
                    if (singleFleet) {
                        raceDisplayName = race.getRaceColumnName();
                    } else {
                        raceDisplayName = race.getRaceColumnName() + "("+fleet+")";
                    }
                    if (race.getRaceIdentifier(fleet) != null) {
                        LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
                        StrippedLeaderboardDTO selectedLeaderboard = leaderboardsSelectionModel.getSelectedObject();
                        RegattaNameAndRaceName raceId = (RegattaNameAndRaceName) race.getRaceIdentifier(fleet);
                        String debugParam = Window.Location.getParameter("gwt.codesvr");
                        String link = RaceBoardPanel.RACEBOARD_PATH+"?leaderboardName="
                                + URLEncoder.encodeQueryString(selectedLeaderboard.getName()) + "&raceName=" + URLEncoder.encodeQueryString(raceId.getRaceName()) + "&regattaName="
                                + URLEncoder.encodeQueryString(raceId.getRegattaName()) + "&leaderboardGroupId=" + selectedGroup.getId().toString()
                                + "&root=overview"
                                + (debugParam != null && !debugParam.isEmpty() ? "&gwt.codesvr=" + URLEncoder.encodeQueryString(debugParam) : "");
                        name = ANCHORTEMPLATE.anchor(UriUtils.fromString(link), raceDisplayName);
                    } else {
                        name = new SafeHtmlBuilder().appendHtmlConstant(raceDisplayName).toSafeHtml();
                    }
                }
                return name;
            }
        };
        
        TextColumn<RaceColumnDTO> racesStartDateColumn = new TextColumn<RaceColumnDTO>() {
            @Override
            public String getValue(RaceColumnDTO race) {
                StringBuilder result = new StringBuilder();
                boolean first = true;
                for (FleetDTO fleet : race.getFleets()) {
                    Date raceStart = race.getStartDate(fleet);
                    if (first) {
                        first = false;
                    } else {
                        result.append(", ");
                    }
                    result.append(raceStart == null ? LeaderboardGroupOverviewPanel.this.stringMessages.untracked()
                            : DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT).format(raceStart));
                }
                return result.toString();
            }
        };
        
        racesTable = new CellTable<RaceColumnDTO>(200, tableResources);
        racesTable.setWidth("100%");
        racesSelectionModel = new NoSelectionModel<RaceColumnDTO>();
        racesTable.setSelectionModel(racesSelectionModel);
        
        racesTable.addColumn(racesLocationColumn, this.stringMessages.location());
        racesTable.addColumn(racesNameColumn, this.stringMessages.name());
        racesTable.addColumn(racesStartDateColumn, this.stringMessages.startDate());
        leaderboardDetailsPanel.add(racesTable);
        
        racesDataProvider = new ListDataProvider<RaceColumnDTO>();
        racesDataProvider.addDataDisplay(racesTable);
        
        return collapsableDetailsPanel;
    }

    private void loadGroups() {
        final long clientTimeWhenRequestWasSent = System.currentTimeMillis();
        sailingService.getLeaderboardGroups(true /*withGeoLocationData*/, new AsyncCallback<List<LeaderboardGroupDTO>>() {
            @Override
            public void onSuccess(List<LeaderboardGroupDTO> result) {
                final long clientTimeWhenResponseWasReceived = System.currentTimeMillis();
                if (result != null && !result.isEmpty()) {
                    timerForClientServerOffset.adjustClientServerOffset(clientTimeWhenRequestWasSent, result
                            .get(result.size() - 1).getCurrentServerTime(), clientTimeWhenResponseWasReceived);
                }
                availableGroups = result == null ? new ArrayList<LeaderboardGroupDTO>() : result;
                groupsDataProvider.getList().clear();
                groupsDataProvider.getList().addAll(availableGroups);
            }
            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError(stringMessages.errorLoadingLeaderBoardGroups(t.getMessage()));
            }
        });
    }
    
    private void onGroupSelectionChanged() {
        LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
        StrippedLeaderboardDTO selectedLeaderboard = leaderboardsSelectionModel.getSelectedObject();
        
        if (selectedGroup != null) {
            setGroupDetailsVisible(true);
            if (selectedLeaderboard != null) {
                setLeaderboardDetailsVisible(selectedGroup.leaderboards.contains(selectedLeaderboard));
            }
            fillGroupDetails(selectedGroup);
        } else {
            setGroupDetailsVisible(false);
            setLeaderboardDetailsVisible(false);
        }
    }
    
    private void setGroupDetailsVisible(boolean visible) {
        noGroupSelectedLabel.setVisible(!visible);
        groupDetailsPanel.setVisible(visible);
        if (visible) {
            collapsableDetailsPanel.setOpen(true);
        }
    }
    
    private void fillGroupDetails(LeaderboardGroupDTO group) {
        groupDescriptionHTML.setHTML(new SafeHtmlBuilder().appendEscapedLines(group.getDescription()).toSafeHtml());
        leaderboardsDataProvider.getList().clear();
        leaderboardsDataProvider.getList().addAll(group.leaderboards);
    }
    
    private void onLeaderboardSelectionChanged() {
        StrippedLeaderboardDTO selectedLeaderboard = leaderboardsSelectionModel.getSelectedObject();
        setLeaderboardDetailsVisible(selectedLeaderboard != null);
        if (selectedLeaderboard != null) {
            fillLeaderboardDetails(selectedLeaderboard);
        }
    }
    
    private void setLeaderboardDetailsVisible(boolean visible) {
        leaderboardDetailsPanel.setVisible(visible);
    }
    
    private void fillLeaderboardDetails(StrippedLeaderboardDTO leaderboard) {
        racesDataProvider.getList().clear();
        racesDataProvider.getList().addAll(leaderboard.getRaceList());
    }
    
    private void onCheckBoxLiveChange() {
        if (onlyLiveCheckBox.getValue()) {
            String today = DateTimeFormat.getFormat("dd.MM.yyyy").format(new Date()).toString();
            
            fromTextBox.setText(today);
            fromTextBox.setEnabled(false);
            untilTextBox.setText("");
            untilTextBox.setEnabled(false);
        } else {
            fromTextBox.setText("");
            fromTextBox.setEnabled(true);
            untilTextBox.setText("");
            untilTextBox.setEnabled(true);
        }
        onSearchCriteriaChange();
    }
    
    private void onSearchCriteriaChange() {
        //Get search criteria
        String location = locationTextBox.getText();
        String name = nameTextBox.getText();
        boolean onlyLive = onlyLiveCheckBox.getValue();
        
        Date from = null;
        Date until = null;
        try {
            from = DateTimeFormat.getFormat("dd.MM.yyyy").parse(fromTextBox.getText());
            until = DateTimeFormat.getFormat("dd.MM.yyyy").parse(untilTextBox.getText());
            //Adding 24 hours to until, so that it doesn't result in an empty table if 'from' and 'until' are equal.
            //Instead you'll have a 24 hour range
            long time = until.getTime() + 24 * 60 * 60 * 1000;
            until = new Date(time);
        } catch (IllegalArgumentException e) {}

        groupsDataProvider.getList().clear();
        for (LeaderboardGroupDTO group : availableGroups) {
            if (checkSearchCriteria(group, location, name, onlyLive, from, until)) {
                groupsDataProvider.getList().add(group);
            }
        }
        //Now sort again according to selected criterion
        ColumnSortEvent.fire(groupsTable, groupsTable.getColumnSortList());
    }
    
    private boolean checkSearchCriteria(LeaderboardGroupDTO forGroup, String location, String name, boolean onlyLive, Date from, Date until) {
        boolean result = true;
        if (result && location != null && location.length() > 0) {
            result = textContainsStringsToCheck(
                    PlacemarkOrderDTO.placemarksOfAllOrderAsSeperatedString(forGroup.getGroupPlaces(), true),
                    location.split("\\s"));
        }
        if (result && name != null && name.length() > 0) {
            result = textContainsStringsToCheck(forGroup.getName(), name.split("\\s"));
        }
        if (result && onlyLiveCheckBox.getValue()) {
            result = forGroup.hasLiveRace(timerForClientServerOffset.getLiveTimePointInMillis());
        }else if (result) {
            Date startDate = forGroup.getGroupStartDate();
            if (startDate != null) {
                if (from != null && until != null) {
                    result = from.before(startDate) && until.after(startDate);
                } else if (from != null) {
                    result = from.before(startDate);
                } else if (until != null) {
                    result = until.after(startDate);
                }
            }
        }
        
        return result;
    }
    
    private boolean textContainsStringsToCheck(String text, String[] stringsToCheck) {
        boolean contains = false;
        for (String stringToCheck : stringsToCheck) {
            contains = text.toLowerCase().contains(stringToCheck.toLowerCase());
            if (!contains) {
                break;
            }
        }
        return contains;
    }

}
