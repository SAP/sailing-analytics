package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sailing.domain.common.security.SecuredDomainType.LEADERBOARD_GROUP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.settings.client.raceboard.RaceBoardPerspectiveOwnSettings;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardConfigPanel.AnchorCell;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardGroupDialog.LeaderboardGroupDescriptor;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.AbstractRegattaPanel;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.ScoringSchemeTypeFormatter;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.URLEncoder;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.AbstractSortableTextColumn;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog.DialogConfig;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

public class LeaderboardGroupConfigPanel extends AbstractRegattaPanel
        implements FilterablePanelProvider<LeaderboardGroupDTO> {
    
    /**
     * The key to put into a params map passed into {@link #setupLeaderboardGroups(Map)} used to identify
     * the leaderboard to select.
     */
    static final String LEADERBOARD_GROUP_ID = "LeaderBoardGroupId";
    
    private final Displayer<StrippedLeaderboardDTO> leaderboardsDisplayer;
    private final Displayer<LeaderboardGroupDTO> leaderboardGroupsDisplayer;
    
    interface AnchorTemplates extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<a href=\"{0}\">{1}</a>")
        SafeHtml cell(SafeUri url, String displayName);
    }

    private static AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);

    private static final AnchorTemplates ANCHORTEMPLATE = GWT.create(AnchorTemplates.class);

    private final UserService userService;

    private VerticalPanel mainPanel;
    private HorizontalPanel splitPanel;
    private CaptionPanel groupDetailsCaptionPanel;

    private LabeledAbstractFilterablePanel<LeaderboardGroupDTO> groupsFilterablePanel;
    private Button removeButton;
    private FlushableCellTable<LeaderboardGroupDTO> groupsTable;
    private RefreshableMultiSelectionModel<LeaderboardGroupDTO> refreshableGroupsSelectionModel;
    /**
     * True, if only one group is selected in the {@link #groupsSelectionModel}.
     */
    private boolean isSingleGroupSelected;
    private ListDataProvider<LeaderboardGroupDTO> groupsProvider;

    private FlushableCellTable<StrippedLeaderboardDTO> groupDetailsTable;
    private RefreshableMultiSelectionModel<StrippedLeaderboardDTO> refreshableGroupDetailsSelectionModel;
    private ListDataProvider<StrippedLeaderboardDTO> groupDetailsProvider;
    private Button editDescriptionButton;
    private Button abortDescriptionButton;
    private Button saveDescriptionButton;
    private TextArea descriptionTextArea;
    private Button leaderboardUpButton;
    private Button leaderboardDownButton;

    private LabeledAbstractFilterablePanel<StrippedLeaderboardDTO> leaderboardsFilterablePanel;
    private FlushableCellTable<StrippedLeaderboardDTO> leaderboardsTable;
    private RefreshableMultiSelectionModel<StrippedLeaderboardDTO> refreshableLeaderboardsSelectionModel;
    private ListDataProvider<StrippedLeaderboardDTO> leaderboardsProvider;

    private Button moveToLeaderboardsButton;
    private Button moveToGroupButton;

    private final ArrayList<LeaderboardGroupDTO> availableLeaderboardGroups;
    private final ArrayList<StrippedLeaderboardDTO> availableLeaderboards;
    private final Presenter presenter;

    private final Set<Widget> permissionRestrictedComponent = new HashSet<>();
    private final Label idLabel = new Label();

    public LeaderboardGroupConfigPanel(Presenter presenter, StringMessages stringMessages) {
        super(presenter, stringMessages);
        this.userService = presenter.getUserService();
        this.leaderboardsDisplayer = result->fillLeaderboards(result);
        this.leaderboardGroupsDisplayer = result->fillLeaderboardGroups(result);
        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        this.availableLeaderboardGroups = new ArrayList<LeaderboardGroupDTO>();
        this.availableLeaderboards = new ArrayList<StrippedLeaderboardDTO>();
        this.presenter = presenter;
        // Build GUI
        mainPanel = new VerticalPanel();
        mainPanel.setSpacing(5);
        mainPanel.setWidth("95%");
        add(mainPanel);
        mainPanel.add(createLeaderboardGroupsGUI(tableRes));
        splitPanel = new HorizontalPanel();
        splitPanel.ensureDebugId("LeaderboardGroupDetailsPanel");
        splitPanel.setWidth("100%");
        splitPanel.setVisible(false);
        mainPanel.add(splitPanel);
        splitPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
        splitPanel.add(createLeaderboardGroupDetailsGUI(tableRes));
        final Widget switchLeaderboardsGUI = createSwitchLeaderboardsGUI();
        this.permissionRestrictedComponent.add(switchLeaderboardsGUI);
        splitPanel.add(switchLeaderboardsGUI);
        final Widget createLeaderboardsGUI = createLeaderboardsGUI(tableRes);
        this.permissionRestrictedComponent.add(createLeaderboardsGUI);
        splitPanel.add(createLeaderboardsGUI);
    }

    private Widget createSwitchLeaderboardsGUI() {
        final VerticalPanel switchLeaderboardsPanel = new VerticalPanel();
        switchLeaderboardsPanel.setSpacing(5);
        switchLeaderboardsPanel.getElement().getStyle().setMarginTop(10, Unit.PX);
        moveToGroupButton = new Button("<-", (ClickHandler) event -> moveToGroup());
        moveToGroupButton.ensureDebugId("AddLeaderboardButton");
        moveToGroupButton.setEnabled(false);
        switchLeaderboardsPanel.add(moveToGroupButton);
        moveToLeaderboardsButton = new Button("->", (ClickHandler) event -> moveToLeaderboards());
        moveToLeaderboardsButton.ensureDebugId("RemoveLeaderboardButton");
        moveToLeaderboardsButton.setEnabled(false);
        switchLeaderboardsPanel.add(moveToLeaderboardsButton);
        return switchLeaderboardsPanel;
    }

    private Widget createLeaderboardsGUI(Resources tableRes) {
        CaptionPanel leaderboardsCaptionPanel = new CaptionPanel(stringMessages.leaderboardsExceptFromSelectedGroup());
        VerticalPanel leaderboardsPanel = new VerticalPanel();
        leaderboardsCaptionPanel.add(leaderboardsPanel);
        // Create leaderboards functional elements
        HorizontalPanel leaderboardsFunctionPanel = new HorizontalPanel();
        leaderboardsFunctionPanel.setSpacing(5);
        leaderboardsPanel.add(leaderboardsFunctionPanel);
        Label filterLeaderboardsLabel = new Label(stringMessages.filterLeaderboardsByName() + ":");
        leaderboardsFunctionPanel.add(filterLeaderboardsLabel);
        // Create leaderboards table
        leaderboardsProvider = new ListDataProvider<>();
        ListHandler<StrippedLeaderboardDTO> leaderboardsListHandler = new ListHandler<>(leaderboardsProvider.getList());
        leaderboardsTable = new FlushableCellTable<>(10000, tableRes);
        leaderboardsTable.ensureDebugId("LeaderboardsCellTable");
        leaderboardsFilterablePanel = new LabeledAbstractFilterablePanel<StrippedLeaderboardDTO>(
                filterLeaderboardsLabel, availableLeaderboards, leaderboardsProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(StrippedLeaderboardDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(t.getDisplayName());
                return strings;
            }

            @Override
            public AbstractCellTable<StrippedLeaderboardDTO> getCellTable() {
                return leaderboardsTable;
            }
        };
        leaderboardsFilterablePanel.setUpdatePermissionFilterForCheckbox(strippedLeaderboardDTO->userService.hasPermission(strippedLeaderboardDTO, DefaultActions.UPDATE));
        SelectionCheckboxColumn<StrippedLeaderboardDTO> leaderboardTableSelectionColumn =
                new SelectionCheckboxColumn<StrippedLeaderboardDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(),
                new EntityIdentityComparator<StrippedLeaderboardDTO>() {
                    @Override
                    public boolean representSameEntity(StrippedLeaderboardDTO dto1, StrippedLeaderboardDTO dto2) {
                        return dto1.getName().equals(dto2.getName());
                    }
                    @Override
                    public int hashCode(StrippedLeaderboardDTO t) {
                        return t.getName().hashCode();
                    }
                }, leaderboardsFilterablePanel.getAllListDataProvider(), leaderboardsTable);
        refreshableLeaderboardsSelectionModel = leaderboardTableSelectionColumn.getSelectionModel();
        leaderboardsTable.setSelectionModel(refreshableLeaderboardsSelectionModel, leaderboardTableSelectionColumn.getSelectionManager());
        leaderboardsFilterablePanel.getTextBox().ensureDebugId("LeaderboardsFilterTextBox");
        leaderboardsFunctionPanel.add(leaderboardsFilterablePanel);
        Button refreshLeaderboardsButton = new Button(stringMessages.refresh());
        refreshLeaderboardsButton.ensureDebugId("RefreshLeaderboardsButton");
        refreshLeaderboardsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                refreshLeaderboardsList();
            }
        });
        leaderboardsFunctionPanel.add(refreshLeaderboardsButton);
        TextColumn<StrippedLeaderboardDTO> leaderboardsNameColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.getName();
            }
        };
        leaderboardsNameColumn.setSortable(true);
        leaderboardsListHandler.setComparator(leaderboardsNameColumn, new Comparator<StrippedLeaderboardDTO>() {
            @Override
            public int compare(StrippedLeaderboardDTO l1, StrippedLeaderboardDTO l2) {
                return new NaturalComparator(false).compare(l1.getName(), l2.getName());
            }
        });
        TextColumn<StrippedLeaderboardDTO> leaderboardsRacesColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                String result = "";
                boolean first = true;
                for (RaceColumnDTO race : leaderboard.getRaceList()) {
                    if (!first) {
                        result += "; ";
                    }
                    result += race.getRaceColumnName();
                    first = false;
                }
                return result;
            }
        };
        leaderboardsTable.setWidth("100%");
        leaderboardsTable.addColumnSortHandler(leaderboardsListHandler);
        leaderboardsTable.addColumn(leaderboardTableSelectionColumn, leaderboardTableSelectionColumn.getHeader());
        leaderboardsTable.addColumn(leaderboardsNameColumn, stringMessages.leaderboardName());
        leaderboardsTable.addColumn(leaderboardsRacesColumn, stringMessages.races());
        refreshableLeaderboardsSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                final Set<StrippedLeaderboardDTO> selectedLeaderboards = refreshableLeaderboardsSelectionModel.getSelectedSet();
                moveToGroupButton.setEnabled(selectedLeaderboards != null && selectedLeaderboards.size() > 0);
            }
        });
        leaderboardsProvider.addDataDisplay(leaderboardsTable);
        leaderboardsPanel.add(leaderboardsTable);
        return leaderboardsCaptionPanel;
    }

    private Widget createLeaderboardGroupDetailsGUI(Resources tableRes) {
        groupDetailsCaptionPanel = new CaptionPanel();
        VerticalPanel groupDetailsPanel = new VerticalPanel();
        groupDetailsPanel.setSpacing(7);
        groupDetailsCaptionPanel.add(groupDetailsPanel);
        final Grid leaderboardGroupIdInfo = new Grid(1, 2);
        leaderboardGroupIdInfo.setText(0, 0, stringMessages.id() + ":");
        leaderboardGroupIdInfo.getCellFormatter().getElement(0, 0).getStyle().setFontWeight(FontWeight.BOLD);
        leaderboardGroupIdInfo.setWidget(0, 1, idLabel);
        groupDetailsPanel.add(leaderboardGroupIdInfo);
        // Create description area
        CaptionPanel descriptionCaptionPanel = new CaptionPanel(stringMessages.description());
        groupDetailsPanel.add(descriptionCaptionPanel);
        VerticalPanel descriptionPanel = new VerticalPanel();
        descriptionPanel.setWidth("100%");
        descriptionCaptionPanel.add(descriptionPanel);
        descriptionTextArea = new TextArea();
        descriptionTextArea.setWidth("100%");
        descriptionTextArea.ensureDebugId("DescriptionTextArea");
        descriptionTextArea.setCharacterWidth(60);
        descriptionTextArea.setVisibleLines(8);
        descriptionTextArea.getElement().getStyle().setProperty("resize", "none");
        descriptionTextArea.setReadOnly(true);
        descriptionPanel.add(descriptionTextArea);
        HorizontalPanel descriptionFunctionsPanel = new HorizontalPanel();
        descriptionPanel.add(descriptionFunctionsPanel);
        editDescriptionButton = new Button(stringMessages.edit());
        editDescriptionButton.ensureDebugId("EditDescriptionButton");
        editDescriptionButton.addClickHandler(event -> setDescriptionEditable(true));
        editDescriptionButton.getElement().getStyle().setMarginRight(5, Unit.PX);
        descriptionFunctionsPanel.add(editDescriptionButton);
        abortDescriptionButton = new Button(stringMessages.abort());
        abortDescriptionButton.ensureDebugId("AbortButton");
        abortDescriptionButton.addClickHandler(event -> {
            if (isSingleGroupSelected) {
                LeaderboardGroupDTO selectedGroup = getSelectedGroup();
                setDescriptionEditable(false);
                descriptionTextArea.setText(selectedGroup.getDescription());
            }
        });
        abortDescriptionButton.getElement().getStyle().setMarginRight(5, Unit.PX);
        abortDescriptionButton.setVisible(false);
        abortDescriptionButton.setEnabled(false);
        descriptionFunctionsPanel.add(abortDescriptionButton);
        saveDescriptionButton = new Button(stringMessages.save());
        saveDescriptionButton.ensureDebugId("SaveButton");
        saveDescriptionButton.addClickHandler(event -> saveDescriptionChanges());
        saveDescriptionButton.setEnabled(false);
        saveDescriptionButton.setVisible(false);
        descriptionFunctionsPanel.add(saveDescriptionButton);
        this.permissionRestrictedComponent.add(descriptionFunctionsPanel);
        // Create leaderboard table
        TextColumn<StrippedLeaderboardDTO> groupDetailsNameColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                return leaderboard.getName();
            }
        };
        TextColumn<StrippedLeaderboardDTO> groupDetailsRacesColumn = new TextColumn<StrippedLeaderboardDTO>() {
            @Override
            public String getValue(StrippedLeaderboardDTO leaderboard) {
                String result = "";
                boolean first = true;
                for (RaceColumnDTO race : leaderboard.getRaceList()) {
                    if (!first) {
                        result += "; ";
                    }
                    result += race.getRaceColumnName();
                    first = false;
                }
                return result;
            }
        };
        groupDetailsTable = new FlushableCellTable<>(10000, tableRes);
        groupDetailsTable.ensureDebugId("LeaderboardGroupsCellTable");
        groupDetailsProvider = new ListDataProvider<>();
        groupDetailsProvider.addDataDisplay(groupDetailsTable);
        SelectionCheckboxColumn<StrippedLeaderboardDTO> groupDetailsTableSelectionColumn =
                new SelectionCheckboxColumn<StrippedLeaderboardDTO>(
                        tableResources.cellTableStyle().cellTableCheckboxSelected(), tableResources.cellTableStyle().cellTableCheckboxDeselected(), 
                        tableResources.cellTableStyle().cellTableCheckboxColumnCell(), new EntityIdentityComparator<StrippedLeaderboardDTO>() {
                            @Override
                            public boolean representSameEntity(StrippedLeaderboardDTO dto1, StrippedLeaderboardDTO dto2) {
                                return dto1.getName().equals(dto2.getName());
                            }
                            @Override
                            public int hashCode(StrippedLeaderboardDTO t) {
                                return t.getName().hashCode();
                            }
                        }, groupDetailsProvider, groupDetailsTable);
        groupDetailsTable.setWidth("100%");
        groupDetailsTable.addColumn(groupDetailsTableSelectionColumn, groupDetailsTableSelectionColumn.getHeader());
        groupDetailsTable.addColumn(groupDetailsNameColumn, stringMessages.leaderboardName());
        groupDetailsTable.addColumn(groupDetailsRacesColumn, stringMessages.races());
        refreshableGroupDetailsSelectionModel = groupDetailsTableSelectionColumn.getSelectionModel();
        refreshableGroupDetailsSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Set<StrippedLeaderboardDTO> selectedLeaderboardsInGroup = refreshableGroupDetailsSelectionModel.getSelectedSet();
                moveToLeaderboardsButton.setEnabled(selectedLeaderboardsInGroup != null && !selectedLeaderboardsInGroup.isEmpty());
                leaderboardDownButton.setEnabled(selectedLeaderboardsInGroup != null && selectedLeaderboardsInGroup.size() == 1);
                leaderboardUpButton.setEnabled(selectedLeaderboardsInGroup != null && selectedLeaderboardsInGroup.size() == 1);
            }
        });
        groupDetailsTable.setSelectionModel(refreshableGroupDetailsSelectionModel, groupDetailsTableSelectionColumn.getSelectionManager());
        groupDetailsPanel.add(groupDetailsTable);
        // Create details functionality
        HorizontalPanel groupDetailsFunctionPanel = new HorizontalPanel();
        groupDetailsFunctionPanel.setSpacing(5);
        groupDetailsPanel.add(groupDetailsFunctionPanel);
        leaderboardUpButton = new Button(stringMessages.columnMoveUp());
        leaderboardUpButton.ensureDebugId("MoveLeaderboardGroupUpButton");
        leaderboardUpButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveLeaderboardInGroupUp();
            }
        });
        leaderboardUpButton.setEnabled(false);
        groupDetailsFunctionPanel.add(leaderboardUpButton);
        leaderboardDownButton = new Button(stringMessages.columnMoveDown());
        leaderboardDownButton.ensureDebugId("MoveLeaderboardGroupDownButton");
        leaderboardDownButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveLeaderboardInGroupDown();
            }
        });
        leaderboardDownButton.setEnabled(false);
        groupDetailsFunctionPanel.add(leaderboardDownButton);
        permissionRestrictedComponent.add(groupDetailsFunctionPanel);
        return groupDetailsCaptionPanel;
    }

    private Widget createLeaderboardGroupsGUI(final Resources tableRes) {
        final CaptionPanel leaderboardGroupsCaptionPanel = new CaptionPanel(stringMessages.leaderboardGroups());
        final VerticalPanel leaderboardGroupsContentPanel = new VerticalPanel();
        leaderboardGroupsCaptionPanel.add(leaderboardGroupsContentPanel);

        // Create functional elements for the leaderboard groups
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, LEADERBOARD_GROUP);
        leaderboardGroupsContentPanel.add(buttonPanel);

        Label filterLeaderboardGroupsLbl = new Label(stringMessages.filterLeaderboardGroupsByName() + ":");

        //Create table for leaderboard groups
        groupsProvider = new ListDataProvider<LeaderboardGroupDTO>();
        ListHandler<LeaderboardGroupDTO> leaderboardGroupsListHandler = new ListHandler<LeaderboardGroupDTO>(groupsProvider.getList());
        groupsTable = new FlushableCellTable<LeaderboardGroupDTO>(10000, tableRes);
        groupsTable.ensureDebugId("LeaderboardGroupsCellTable");
        groupsFilterablePanel = new LabeledAbstractFilterablePanel<LeaderboardGroupDTO>(filterLeaderboardGroupsLbl,
                availableLeaderboardGroups, groupsProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(LeaderboardGroupDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(String.valueOf(t.getId()));
                strings.add(t.getDisplayName());
                strings.add(t.getDescription());
                return strings;
            }

            @Override
            public AbstractCellTable<LeaderboardGroupDTO> getCellTable() {
                return groupsTable;
            }
        };
        groupsFilterablePanel.getTextBox().ensureDebugId("LeaderboardGroupsFilterTextBox");
        leaderboardGroupsContentPanel.add(groupsFilterablePanel);
        groupsFilterablePanel.setUpdatePermissionFilterForCheckbox(
                leaderboardGroup -> userService.hasPermission(leaderboardGroup, DefaultActions.UPDATE));
        final Button createButton = buttonPanel.addCreateAction(stringMessages.createNewLeaderboardGroup(),
                this::addNewGroup);
        createButton.ensureDebugId("CreateLeaderboardGroupButton");
        final Button refreshButton = buttonPanel.addUnsecuredAction(stringMessages.refresh(), () -> {
            presenter.getLeaderboardsRefresher().reloadAndCallFillAll();
            presenter.getLeaderboardGroupsRefresher().reloadAndCallFillAll();
        });
        refreshButton.ensureDebugId("RefreshLeaderboardGroupsButton");
        AnchorCell anchorCell = new AnchorCell();
        final TextColumn<LeaderboardGroupDTO> groupUUidColumn = new AbstractSortableTextColumn<LeaderboardGroupDTO>(
                group -> group.getId() == null ? "<null>" : group.getId().toString(), leaderboardGroupsListHandler);
        Column<LeaderboardGroupDTO, SafeHtml> groupNameColumn = new Column<LeaderboardGroupDTO, SafeHtml>(anchorCell) {
            @Override
            public SafeHtml getValue(LeaderboardGroupDTO group) {
                String debugParam = Window.Location.getParameter("gwt.codesvr");
                String link = "/gwt/Spectator.html?leaderboardGroupId=" + group.getId()+"&showRaceDetails=true&"
                        + RaceBoardPerspectiveOwnSettings.PARAM_CAN_REPLAY_DURING_LIVE_RACES + "=true"
                        + (debugParam != null && !debugParam.isEmpty() ? "&gwt.codesvr=" + URLEncoder.encodeQueryString(debugParam) : "");
                return ANCHORTEMPLATE.cell(UriUtils.fromString(link), group.getName());
            }
        };
        groupNameColumn.setSortable(true);
        leaderboardGroupsListHandler.setComparator(groupNameColumn, new Comparator<LeaderboardGroupDTO>() {
            @Override
            public int compare(LeaderboardGroupDTO group1, LeaderboardGroupDTO group2) {
                return new NaturalComparator(false).compare(group1.getName(), group2.getName());
            }
        });

        TextColumn<LeaderboardGroupDTO> groupDescriptionColumn = new TextColumn<LeaderboardGroupDTO>() {
            @Override
            public String getValue(LeaderboardGroupDTO group) {
                return group.getDescription().length() <= 100 ? group.getDescription() : group.getDescription().substring(0, 98) + "...";
            }
        };
        groupDescriptionColumn.setSortable(true);
        leaderboardGroupsListHandler.setComparator(groupDescriptionColumn, new Comparator<LeaderboardGroupDTO>() {
            @Override
            public int compare(LeaderboardGroupDTO group1, LeaderboardGroupDTO group2) {
                return new NaturalComparator(false).compare(group1.getDescription(), group2.getDescription());
            }
        });

        TextColumn<LeaderboardGroupDTO> groupDisplayNameColumn = new TextColumn<LeaderboardGroupDTO>() {
            @Override
            public String getValue(LeaderboardGroupDTO group) {
                return group.getDisplayName() == null ? "" :
                    group.getDisplayName().length() <= 100 ? group.getDisplayName() : group.getDisplayName().substring(0, 98) + "...";
            }
        };
        groupDisplayNameColumn.setSortable(true);
        leaderboardGroupsListHandler.setComparator(groupDisplayNameColumn, new Comparator<LeaderboardGroupDTO>() {
            @Override
            public int compare(LeaderboardGroupDTO group1, LeaderboardGroupDTO group2) {
                return new NaturalComparator(false).compare(group1.getDisplayName(), group2.getDisplayName());
            }
        });

        TextColumn<LeaderboardGroupDTO> hasOverallLeaderboardColumn = new TextColumn<LeaderboardGroupDTO>() {
            @Override
            public String getValue(LeaderboardGroupDTO group) {
                String result = stringMessages.no();
                if(group.hasOverallLeaderboard()) {
                    result =  stringMessages.yes() + " (" + ScoringSchemeTypeFormatter.format(group.getOverallLeaderboardScoringSchemeType(), stringMessages) +")";
                }
                return  result;
            }
        };
        hasOverallLeaderboardColumn.setSortable(true);
        leaderboardGroupsListHandler.setComparator(hasOverallLeaderboardColumn, new Comparator<LeaderboardGroupDTO>() {
            @Override
            public int compare(LeaderboardGroupDTO group1, LeaderboardGroupDTO group2) {
                return new NaturalComparator(false).compare(hasOverallLeaderboardColumn.getValue(group1),
                        hasOverallLeaderboardColumn.getValue(group2));
            }
        });

        final HasPermissions type = SecuredDomainType.LEADERBOARD_GROUP;
        final AccessControlledActionsColumn<LeaderboardGroupDTO, LeaderboardGroupConfigImagesBarCell> actionsColumn = create(
                new LeaderboardGroupConfigImagesBarCell(stringMessages), userService);
        actionsColumn.addAction(LeaderboardGroupConfigImagesBarCell.ACTION_UPDATE, UPDATE,
                this::openEditLeaderboardGroupDialog);
        actionsColumn.addAction(LeaderboardGroupConfigImagesBarCell.ACTION_DELETE, DELETE, group -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveLeaderboardGroup(group.getName()))) {
                removeLeaderboardGroup(group);
            }
        });
        final DialogConfig<LeaderboardGroupDTO> config = EditOwnershipDialog.create(
                userService.getUserManagementWriteService(), type,
                group -> {}, stringMessages);
        actionsColumn.addAction(LeaderboardGroupConfigImagesBarCell.ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP,
                e -> config.openOwnershipDialog(e));
        final EditACLDialog.DialogConfig<LeaderboardGroupDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type,
                group -> {}, stringMessages);
        actionsColumn.addAction(LeaderboardGroupConfigImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                e -> configACL.openDialog(e));
        final MigrateGroupOwnershipDialog.DialogConfig<LeaderboardGroupDTO> migrateDialogConfig = MigrateGroupOwnershipDialog
                .create(userService.getUserManagementService(), (lg, dto) -> {
                    sailingServiceWrite.updateGroupOwnerForLeaderboardGroupHierarchy(lg.getId(), dto,
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError(stringMessages.errorUpdatingOwnership(lg.getName()));
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    presenter.getLeaderboardGroupsRefresher().reloadAndCallFillAll();
                                }
                            });
                });
        actionsColumn.addAction(EventConfigImagesBarCell.ACTION_MIGRATE_GROUP_OWNERSHIP_HIERARCHY, CHANGE_OWNERSHIP,
                e -> migrateDialogConfig.openDialog(e));
        SelectionCheckboxColumn<LeaderboardGroupDTO> leaderboardTableSelectionColumn =
                new SelectionCheckboxColumn<LeaderboardGroupDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(),
                new EntityIdentityComparator<LeaderboardGroupDTO>() {
                    @Override
                    public boolean representSameEntity(LeaderboardGroupDTO dto1, LeaderboardGroupDTO dto2) {
                        return dto1.getId().equals(dto2.getId());
                    }
                    @Override
                    public int hashCode(LeaderboardGroupDTO t) {
                        return t.getId().hashCode();
                    }
                }, groupsFilterablePanel.getAllListDataProvider(), groupsTable);
        groupsTable.setWidth("100%");
        groupsTable.addColumn(leaderboardTableSelectionColumn, leaderboardTableSelectionColumn.getHeader());
        groupsTable.addColumn(groupNameColumn, stringMessages.name());
        groupsTable.addColumn(groupDescriptionColumn, stringMessages.description());
        groupsTable.addColumn(groupDisplayNameColumn, stringMessages.displayName());
        groupsTable.addColumn(hasOverallLeaderboardColumn, stringMessages.useOverallLeaderboard());
        SecuredDTOOwnerColumn.configureOwnerColumns(groupsTable, leaderboardGroupsListHandler, stringMessages);
        groupsTable.addColumn(groupUUidColumn, stringMessages.id());
        groupsTable.addColumn(actionsColumn, stringMessages.actions());
        groupsTable.addColumnSortHandler(leaderboardGroupsListHandler);
        refreshableGroupsSelectionModel = leaderboardTableSelectionColumn.getSelectionModel();
        removeButton = buttonPanel.addRemoveAction(stringMessages.remove(), refreshableGroupsSelectionModel, true,
                () -> removeLeaderboardGroups(refreshableGroupsSelectionModel.getSelectedSet()));
        removeButton.ensureDebugId("RemoveLeaderboardButton");
        refreshableGroupsSelectionModel.addSelectionChangeHandler(event -> groupSelectionChanged());
        groupsTable.setSelectionModel(refreshableGroupsSelectionModel, leaderboardTableSelectionColumn.getSelectionManager());
        groupsProvider.addDataDisplay(groupsTable);
        leaderboardGroupsContentPanel.add(groupsTable);
        return leaderboardGroupsCaptionPanel;
    }

    private void openEditLeaderboardGroupDialog(final LeaderboardGroupDTO group) {
        final UUID oldGroupId = group.getId();
        final String oldGroupName = group.getName();
        final ArrayList<LeaderboardGroupDTO> otherExistingGroups = new ArrayList<>(availableLeaderboardGroups);
        otherExistingGroups.remove(group);
        final LeaderboardGroupEditDialog dialog = new LeaderboardGroupEditDialog(group, otherExistingGroups,
                stringMessages, new DialogCallback<LeaderboardGroupDescriptor>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(LeaderboardGroupDescriptor groupDescriptor) {
                        updateGroup(oldGroupId, oldGroupName, group, groupDescriptor);
                    }
                });
        dialog.show();
    }

    public void fillLeaderboardGroups(Iterable<LeaderboardGroupDTO> groups) {
        availableLeaderboardGroups.clear();
        if (groups != null) {
            Util.addAll(groups, availableLeaderboardGroups);
        }
        groupsFilterablePanel.updateAll(availableLeaderboardGroups);
    }

    public void fillLeaderboards(Iterable<StrippedLeaderboardDTO> leaderboards) {
        availableLeaderboards.clear();
        leaderboardsProvider.getList().clear();
        if (leaderboards != null) {
            Util.addAll(leaderboards, availableLeaderboards);
        }
        final LeaderboardGroupDTO selectedGroup = getSelectedGroup();
        if(selectedGroup != null) {
            this.updateLeaderboardFilterToShowAllLeaderboardsExceptThoseOf(selectedGroup);
            refreshableLeaderboardsSelectionModel.clear();
        }
    }

    /**
     * If no leaderboard group is selected or more than one is selected, this method is a no-op because the leaderboard
     * list displayed is defined to the be list of all leaderboards available, excluding those leaderboards that are
     * already assigned to <em>the</em> selected leaderboard group. In other words, the leaderboard list is only
     * well-defined in case a single leaderboard group is selected.
     */
    private void refreshLeaderboardsList() {
        if (isSingleGroupSelected && getSelectedGroup() != null) {
            presenter.getLeaderboardsRefresher().reloadAndCallFillAll();
        }
    }

    private void addNewGroup() {
        LeaderboardGroupCreateDialog dialog = new LeaderboardGroupCreateDialog(
                Collections.unmodifiableCollection(availableLeaderboardGroups), stringMessages,
                new DialogCallback<LeaderboardGroupDescriptor>() {
                    @Override
                    public void cancel() {}
                    @Override
                    public void ok(LeaderboardGroupDescriptor newGroup) {
                        createNewGroup(newGroup);
                    }
                });
        dialog.ensureDebugId("LeaderboardGroupCreateDialog");
        dialog.show();
    }

    private void createNewGroup(final LeaderboardGroupDescriptor newGroup) {
        sailingServiceWrite.createLeaderboardGroup(newGroup.getName(), newGroup.getDescription(),
                newGroup.getDisplayName(), newGroup.isDisplayLeaderboardsInReverseOrder(),
                newGroup.getOverallLeaderboardDiscardThresholds(), newGroup.getOverallLeaderboardScoringSchemeType(), new MarkedAsyncCallback<LeaderboardGroupDTO>(
                        new AsyncCallback<LeaderboardGroupDTO>() {
                            @Override
                            public void onFailure(Throwable t) {
                                errorReporter.reportError(stringMessages.errorCreatingLeaderboardGroup(newGroup.getName())
                                                + ": " + t.getMessage());
                            }
                            @Override
                            public void onSuccess(LeaderboardGroupDTO newGroup) {
                                availableLeaderboardGroups.add(newGroup);
                                groupsFilterablePanel.updateAll(availableLeaderboardGroups);
                                refreshableGroupsSelectionModel.clear();
                                refreshableGroupsSelectionModel.setSelected(newGroup, true);
                                presenter.getLeaderboardGroupsRefresher().add(newGroup);
                                presenter.getLeaderboardGroupsRefresher().callAllFill();
                            }
                        }));
    }

    private void updateGroup(final UUID oldGroupId, final String oldGroupName, final LeaderboardGroupDTO groupToUpdate, final LeaderboardGroupDescriptor updateDescriptor) {
        List<String> leaderboardNames = new ArrayList<String>();
        for (StrippedLeaderboardDTO leaderboardDTO : groupToUpdate.leaderboards) {
            leaderboardNames.add(leaderboardDTO.getName());
        }
        sailingServiceWrite.updateLeaderboardGroup(oldGroupId, oldGroupName, updateDescriptor.getName(), updateDescriptor.getDescription(),
                updateDescriptor.getDisplayName(),
                leaderboardNames, updateDescriptor.getOverallLeaderboardDiscardThresholds(),
                updateDescriptor.getOverallLeaderboardScoringSchemeType(), new MarkedAsyncCallback<Void>(
                        new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable t) {
                                errorReporter.reportError("Error trying to update leaderboard group " + oldGroupName + ": "
                                        + t.getMessage());
                            }
                            @Override
                            public void onSuccess(Void v) {
                                // Update the availableLeaderboardGroups and the list of displayed groups
                                for (int i = 0; i < availableLeaderboardGroups.size(); i++) {
                                    LeaderboardGroupDTO group = availableLeaderboardGroups.get(i);
                                    if (oldGroupName.equals(group.getName())) {
                                        // if the leaderboard is selected, de-select equality/hashCode may change) and select again when done
                                        final boolean wasSelected = Util.equalsWithNull(groupToUpdate, getSelectedGroup());
                                        if (wasSelected) {
                                            refreshableGroupsSelectionModel.setSelected(groupToUpdate, false);
                                        }
                                        groupToUpdate.setName(updateDescriptor.getName());
                                        groupToUpdate.setDescription(updateDescriptor.getDescription());
                                        groupToUpdate.setDisplayName(updateDescriptor.getDisplayName());
                                        groupToUpdate.displayLeaderboardsInReverseOrder = updateDescriptor.isDisplayLeaderboardsInReverseOrder();
                                        groupToUpdate.setOverallLeaderboardDiscardThresholds(updateDescriptor.getOverallLeaderboardDiscardThresholds());
                                        groupToUpdate.setOverallLeaderboardScoringSchemeType(updateDescriptor.getOverallLeaderboardScoringSchemeType());
                                        availableLeaderboardGroups.set(i, groupToUpdate);
                                        int displayedIndex = groupsProvider.getList().indexOf(group);
                                        if (displayedIndex != -1) {
                                            groupsProvider.getList().set(displayedIndex, groupToUpdate);
                                        }
                                        if (wasSelected) {
                                            refreshableGroupsSelectionModel.setSelected(groupToUpdate, true);
                                        }
                                    }
                                }
                                groupsFilterablePanel.updateAll(availableLeaderboardGroups);
                                presenter.getLeaderboardGroupsRefresher().updateAndCallFillForAll(
                                        availableLeaderboardGroups,
                                        LeaderboardGroupConfigPanel.this.getLeaderboardGroupsDisplayer());
                                groupsProvider.refresh();
                            }
                        }));
    }

    /**
     * Updates a change in the group's leaderboard list to the server
     */
    private void updateGroup(final LeaderboardGroupDTO group) {
        List<String> leaderboardNames = new ArrayList<String>();
        for (StrippedLeaderboardDTO leaderboardDTO : group.leaderboards) {
            leaderboardNames.add(leaderboardDTO.getName());
        }
        sailingServiceWrite.updateLeaderboardGroup(group.getId(), group.getName(), group.getName(), group.getDescription(),
                group.getDisplayName(),
                leaderboardNames, group.getOverallLeaderboardDiscardThresholds(),
                group.getOverallLeaderboardScoringSchemeType(), new MarkedAsyncCallback<Void>(
                        new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable t) {
                                errorReporter.reportError("Error trying to update leaderboard group " + group.getName() + ": "
                                        + t.getMessage());
                            }
                            @Override
                            public void onSuccess(Void v) {
                            }
                        }));
    }
    
    private void removeLeaderboardGroups(final Collection<LeaderboardGroupDTO> groups) {
        if (!groups.isEmpty()) {
            final Set<UUID> groupIds = new HashSet<>();
            for (LeaderboardGroupDTO group : groups) {
                groupIds.add(group.getId());
            }
            sailingServiceWrite.removeLeaderboardGroups(groupIds, new MarkedAsyncCallback<Void>(
                    new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable t) {
                            errorReporter.reportError("Error trying to remove the leaderboard groups: "
                                    + t.getMessage());
                        }
                        @Override
                        public void onSuccess(Void result) {
                            for (LeaderboardGroupDTO group : groups) {
                                removeGroupFromTable(group);
                                presenter.getLeaderboardGroupsRefresher().remove(group);
                            }
                            presenter.getLeaderboardGroupsRefresher().callAllFill();
                        }
                    }));
        }
    }

    private void removeLeaderboardGroup(final LeaderboardGroupDTO group) {
        final Set<UUID> groupIds = new HashSet<>();
        groupIds.add(group.getId());
        sailingServiceWrite.removeLeaderboardGroups(groupIds, new MarkedAsyncCallback<Void>(
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to remove leaderboard group " + group.getName() + ": "
                                + t.getMessage());
                    }
                    @Override
                    public void onSuccess(Void v) {
                        removeGroupFromTable(group);
                        presenter.getLeaderboardGroupsRefresher().remove(group);
                        presenter.getLeaderboardGroupsRefresher().callAllFill();
                    }
                }));
    }


    private void removeGroupFromTable(final LeaderboardGroupDTO group) {
        availableLeaderboardGroups.remove(group);
        groupsFilterablePanel.updateAll(availableLeaderboardGroups);
        refreshableGroupsSelectionModel.setSelected(group, false);
    }

    private void groupSelectionChanged() {
        Set<LeaderboardGroupDTO> selectedLeaderboardGroups = refreshableGroupsSelectionModel.getSelectedSet();
        isSingleGroupSelected = selectedLeaderboardGroups.size() == 1;
        boolean canDeleteAllSelected = true;
        for (LeaderboardGroupDTO group : selectedLeaderboardGroups) {
            if (!userService.hasPermission(group, DefaultActions.DELETE)) {
                canDeleteAllSelected = false;
            }
        }
        removeButton.setEnabled(!selectedLeaderboardGroups.isEmpty() && canDeleteAllSelected);
        splitPanel.setVisible(isSingleGroupSelected);
        if (isSingleGroupSelected) {
            LeaderboardGroupDTO selectedGroup = selectedLeaderboardGroups.iterator().next();
            //Display details of the group
            groupDetailsCaptionPanel.setCaptionText(stringMessages.detailsOfLeaderboardGroup() + " '" + selectedGroup.getName() + "'");
            idLabel.setText(selectedGroup.getId().toString());
            descriptionTextArea.setText(selectedGroup.getDescription());
            setDescriptionEditable(false);
            groupDetailsProvider.getList().clear();
            groupDetailsProvider.getList().addAll(selectedGroup.leaderboards);
            groupDetailsProvider.refresh();
            
            final boolean userHasUpdateGroupPermission = userService.hasPermission(selectedGroup, UPDATE);

            refreshableLeaderboardsSelectionModel.clear();
            leaderboardsProvider.getList().clear();
            permissionRestrictedComponent.forEach(component -> component.setVisible(userHasUpdateGroupPermission));
            if (userHasUpdateGroupPermission) {
                this.updateLeaderboardFilterToShowAllLeaderboardsExceptThoseOf(selectedGroup);
                leaderboardsProvider.refresh();
            }
        }
    }

    private void updateLeaderboardFilterToShowAllLeaderboardsExceptThoseOf(LeaderboardGroupDTO selectedGroup) {
        final Map<String, StrippedLeaderboardDTO> allExceptOf = availableLeaderboards.stream()
                .collect(Collectors.toMap(StrippedLeaderboardDTO::getName, Function.identity()));
        selectedGroup.getLeaderboards().stream().map(StrippedLeaderboardDTO::getName).forEach(allExceptOf::remove);
        leaderboardsFilterablePanel.updateAll(allExceptOf.values());
    }

    private void moveToLeaderboards() {
        LeaderboardGroupDTO selectedGroup = getSelectedGroup();
        Set<StrippedLeaderboardDTO> selectedLeaderboards = refreshableGroupDetailsSelectionModel.getSelectedSet();
        if (isSingleGroupSelected && selectedGroup != null && selectedLeaderboards != null && selectedLeaderboards.size() > 0) {
            for (StrippedLeaderboardDTO leaderboard : selectedLeaderboards) {
                selectedGroup.leaderboards.remove(leaderboard);
                groupDetailsProvider.getList().remove(leaderboard);
                refreshableGroupDetailsSelectionModel.setSelected(leaderboard, false);
                leaderboardsProvider.getList().add(leaderboard);
            }
            updateGroup(selectedGroup);
            //Refilters the leaderboards list (hides the moved leaderboards if they don't fit to the filter) and resorts the list
            leaderboardsFilterablePanel.updateAll(leaderboardsProvider.getList());
        }
    }

    private void moveToGroup() {
        final LeaderboardGroupDTO selectedGroup = getSelectedGroup();
        ArrayList<StrippedLeaderboardDTO> selectedLeaderboards = new ArrayList<>(refreshableLeaderboardsSelectionModel.getSelectedSet());
        if (isSingleGroupSelected && selectedGroup != null && selectedLeaderboards != null && !selectedLeaderboards.isEmpty()) {
            Collections.sort(selectedLeaderboards, new Comparator<StrippedLeaderboardDTO>() {
                @Override
                public int compare(StrippedLeaderboardDTO l1, StrippedLeaderboardDTO l2) {
                    List<StrippedLeaderboardDTO> leaderboards = leaderboardsProvider.getList();
                    return ((Integer) leaderboards.indexOf(l1)).compareTo(leaderboards.indexOf(l2));
                }
            });
            for (StrippedLeaderboardDTO leaderboard : selectedLeaderboards) {
                if (!selectedGroup.leaderboards.contains(leaderboard)) {
                    selectedGroup.leaderboards.add(leaderboard);
                    groupDetailsProvider.getList().add(leaderboard);
                    leaderboardsProvider.getList().remove(leaderboard);
                    refreshableLeaderboardsSelectionModel.setSelected(leaderboard, false);
                }
            }
            updateGroup(selectedGroup);
        }
    }

    private void moveLeaderboardInGroupUp() {
        LeaderboardGroupDTO selectedGroup = getSelectedGroup(); 
        Set<StrippedLeaderboardDTO> selectedLeaderboards = refreshableGroupDetailsSelectionModel.getSelectedSet();
        if (isSingleGroupSelected && selectedGroup != null && selectedLeaderboards != null && selectedLeaderboards.size() == 1) {
            StrippedLeaderboardDTO selectedLeaderboard = selectedLeaderboards.iterator().next();
            moveLeaderboardInGroup(selectedGroup, selectedLeaderboard, -1);
        }
    }

    private void moveLeaderboardInGroupDown() {
        LeaderboardGroupDTO selectedGroup = getSelectedGroup(); 
        Set<StrippedLeaderboardDTO> selectedLeaderboards = refreshableGroupDetailsSelectionModel.getSelectedSet();
        if (isSingleGroupSelected && selectedGroup != null && selectedLeaderboards != null && selectedLeaderboards.size() == 1) {
            StrippedLeaderboardDTO selectedLeaderboard = selectedLeaderboards.iterator().next();
            moveLeaderboardInGroup(selectedGroup, selectedLeaderboard, 1);
        }
    }

    private void moveLeaderboardInGroup(LeaderboardGroupDTO group, StrippedLeaderboardDTO leaderboard, int direction) {
        int index = group.leaderboards.indexOf(leaderboard);
        int destIndex = index + direction;
        if (destIndex >= 0 && destIndex < group.leaderboards.size()) {
            StrippedLeaderboardDTO temp = group.leaderboards.get(destIndex);
            group.leaderboards.set(destIndex, leaderboard);
            group.leaderboards.set(index, temp);
            groupDetailsProvider.getList().clear();
            groupDetailsProvider.getList().addAll(group.leaderboards);

            updateGroup(group);
        }
    }

    @Override
    public void fillRegattas(Iterable<RegattaDTO> result) {
    }

    private void setDescriptionEditable(boolean isEditable) {
        LeaderboardGroupDTO selectedGroup = getSelectedGroup();
        if (isSingleGroupSelected && selectedGroup != null) {
            editDescriptionButton.setEnabled(!isEditable);
            editDescriptionButton.setVisible(!isEditable);
            abortDescriptionButton.setEnabled(isEditable);
            abortDescriptionButton.setVisible(isEditable);
            saveDescriptionButton.setEnabled(isEditable);
            saveDescriptionButton.setVisible(isEditable);
            descriptionTextArea.setReadOnly(!isEditable);
        }
    }

    private void saveDescriptionChanges() {
        String newDescription = descriptionTextArea.getText();
        LeaderboardGroupDTO selectedGroup = getSelectedGroup();
        if (isSingleGroupSelected && selectedGroup != null) {
            if (newDescription != null && newDescription.length() > 0) {
                selectedGroup.setDescription(newDescription);
                setDescriptionEditable(false);
                updateGroup(selectedGroup);
            } else {
                Notification.notify(stringMessages.pleaseEnterNonEmptyDescription() + ".", NotificationType.ERROR);
                descriptionTextArea.setText(selectedGroup.getDescription());
            }
        }
    }

    private LeaderboardGroupDTO getSelectedGroup() {
        return refreshableGroupsSelectionModel.getSelectedSet().isEmpty() ? null : refreshableGroupsSelectionModel.getSelectedSet().iterator().next();
    }

    @Override
    public AbstractFilterablePanel<LeaderboardGroupDTO> getFilterablePanel() {
        return groupsFilterablePanel;
    }
    
    public Displayer<StrippedLeaderboardDTO> getLeaderboardsDisplayer() {
        return leaderboardsDisplayer;
    }
    
    public Displayer<LeaderboardGroupDTO> getLeaderboardGroupsDisplayer() {
        return leaderboardGroupsDisplayer;
    }
    
}
