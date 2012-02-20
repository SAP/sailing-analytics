package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardConfigPanel.AnchorCell;
import com.sap.sailing.gwt.ui.client.AbstractEventPanel;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.EventRefresher;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.URLFactory;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RaceInLeaderboardDTO;

public class LeaderboardGroupConfigPanel extends AbstractEventPanel {

    interface AnchorTemplates extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<a href=\"{0}\">{1}</a>")
        SafeHtml cell(String url, String displayName);
    }

    private static final AnchorTemplates ANCHORTEMPLATE = GWT.create(AnchorTemplates.class);
    
    private VerticalPanel mainPanel;
    private HorizontalPanel splitPanel;
    private CaptionPanel groupDetailsCaptionPanel;
    
    private TextBox groupsFilterTextBox;
    private CellTable<LeaderboardGroupDTO> groupsTable;
    private SingleSelectionModel<LeaderboardGroupDTO> groupsSelectionModel;
    private ListDataProvider<LeaderboardGroupDTO> groupsProvider;
    
    private CellTable<LeaderboardDTO> groupDetailsTable;
    private MultiSelectionModel<LeaderboardDTO> groupDetailsSelectionModel;
    private ListDataProvider<LeaderboardDTO> groupDetailsProvider;
    private Button editDescriptionButton;
    private Button abortDescriptionButton;
    private Button saveDescriptionButton;
    private TextArea descriptionTextArea;
    private Button leaderboardUpButton;
    private Button leaderboardDownButton;
    
    private TextBox leaderboardsFilterTextBox;
    private CellTable<LeaderboardDTO> leaderboardsTable;
    private MultiSelectionModel<LeaderboardDTO> leaderboardsSelectionModel;
    private ListDataProvider<LeaderboardDTO> leaderboardsProvider;
    
    private Button moveToLeaderboardsButton;
    private Button moveToGroupButton;
    
    private ArrayList<LeaderboardGroupDTO> availableLeaderboardGroups;
    private ArrayList<LeaderboardDTO> availableLeaderboards;

    public LeaderboardGroupConfigPanel(SailingServiceAsync sailingService, EventRefresher eventRefresher,
            ErrorReporter errorReporter, StringMessages stringMessages) {
        super(sailingService, eventRefresher, errorReporter, stringMessages);
        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        availableLeaderboardGroups = new ArrayList<LeaderboardGroupDTO>();
        availableLeaderboards = new ArrayList<LeaderboardDTO>();
        
        //Build GUI
        mainPanel = new VerticalPanel();
        mainPanel.setSpacing(5);
        mainPanel.setWidth("95%");
        add(mainPanel);
        
        mainPanel.add(createLeaderboardGroupsGUI(tableRes));
        
        splitPanel = new HorizontalPanel();
        splitPanel.setSpacing(5);
        splitPanel.setWidth("100%");
        splitPanel.setVisible(false);
        mainPanel.add(splitPanel);

        splitPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
        splitPanel.add(createLeaderboardGroupDetailsGUI(tableRes));
        splitPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        splitPanel.add(createSwitchLeaderboardsGUI());
        splitPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        splitPanel.add(createLeaderboardsGUI(tableRes));
        
        //Load Data
        loadGroups();
        loadLeaderboards();
    }

    private Widget createSwitchLeaderboardsGUI() {
        VerticalPanel switchLeaderboardsPanel = new VerticalPanel();
        switchLeaderboardsPanel.setSpacing(5);
        switchLeaderboardsPanel.setWidth("5%");
        
        moveToGroupButton = new Button("<-");
        moveToGroupButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveToGroup();
            }
        });
        moveToGroupButton.setEnabled(false);
        switchLeaderboardsPanel.add(moveToGroupButton);
        
        moveToLeaderboardsButton = new Button("->");
        moveToLeaderboardsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveToLeaderboards();
            }
        });
        moveToLeaderboardsButton.setEnabled(false);
        switchLeaderboardsPanel.add(moveToLeaderboardsButton);
        
        return switchLeaderboardsPanel;
    }

    private Widget createLeaderboardsGUI(Resources tableRes) {
        CaptionPanel leaderboardsCaptionPanel = new CaptionPanel(stringConstants.leaderboards());
        leaderboardsCaptionPanel.setWidth("95%");
        
        VerticalPanel leaderboardsPanel = new VerticalPanel();
        leaderboardsCaptionPanel.add(leaderboardsPanel);
        
        //Create leaderboards functional elements
        HorizontalPanel leaderboardsFunctionPanel = new HorizontalPanel();
        leaderboardsFunctionPanel.setSpacing(5);
        leaderboardsPanel.add(leaderboardsFunctionPanel);
        
        Label filterLeaderboardsLabel = new Label(stringConstants.filterLeaderboardsByName() + ":");
        leaderboardsFunctionPanel.add(filterLeaderboardsLabel);
        
        leaderboardsFilterTextBox = new TextBox();
        leaderboardsFilterTextBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                leaderboardsFilterChanged();
            }
        });
        leaderboardsFunctionPanel.add(leaderboardsFilterTextBox);
        
        Button refreshLeaderboardsButton = new Button(stringConstants.refresh());
        refreshLeaderboardsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                refreshLeaderboardsList();
            }
        });
        leaderboardsFunctionPanel.add(refreshLeaderboardsButton);
        
        //Create leaderboards table
        leaderboardsProvider = new ListDataProvider<LeaderboardDTO>();
        ListHandler<LeaderboardDTO> leaderboardsListHandler = new ListHandler<LeaderboardDTO>(leaderboardsProvider.getList());
        
        TextColumn<LeaderboardDTO> leaderboardsNameColumn = new TextColumn<LeaderboardDTO>() {
            @Override
            public String getValue(LeaderboardDTO leaderboard) {
                return leaderboard.name;
            }
        };
        leaderboardsNameColumn.setSortable(true);
        leaderboardsListHandler.setComparator(leaderboardsNameColumn, new Comparator<LeaderboardDTO>() {
            @Override
            public int compare(LeaderboardDTO l1, LeaderboardDTO l2) {
                return l1.name.compareTo(l2.name);
            }
        });
        
        TextColumn<LeaderboardDTO> leaderboardsRacesColumn = new TextColumn<LeaderboardDTO>() {
            @Override
            public String getValue(LeaderboardDTO leaderboard) {
                String result = "";
                boolean first = true;
                for (RaceInLeaderboardDTO race : leaderboard.getRaceList()) {
                    if (!first) {
                        result += "; ";
                    }
                    result += race.getRaceColumnName();
                    first = false;
                }
                return result;
            }
        };

        leaderboardsTable = new CellTable<LeaderboardDTO>(200, tableRes);
        leaderboardsTable.setWidth("100%");
        leaderboardsTable.addColumnSortHandler(leaderboardsListHandler);
        leaderboardsTable.addColumn(leaderboardsNameColumn, stringConstants.leaderboardName());
        leaderboardsTable.addColumn(leaderboardsRacesColumn, stringConstants.races());

        leaderboardsSelectionModel = new MultiSelectionModel<LeaderboardDTO>();
        leaderboardsSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Set<LeaderboardDTO> selectedLeaderboards = leaderboardsSelectionModel.getSelectedSet();
                moveToGroupButton.setEnabled(selectedLeaderboards != null && selectedLeaderboards.size() > 0);
            }
        });
        leaderboardsTable.setSelectionModel(leaderboardsSelectionModel);

        leaderboardsProvider.addDataDisplay(leaderboardsTable);
        leaderboardsPanel.add(leaderboardsTable);
        
        return leaderboardsCaptionPanel;
    }

    private Widget createLeaderboardGroupDetailsGUI(Resources tableRes) {
        groupDetailsCaptionPanel = new CaptionPanel();
        groupDetailsCaptionPanel.setWidth("95%");
        
        VerticalPanel groupDetailsPanel = new VerticalPanel();
        groupDetailsPanel.setSpacing(7);
        groupDetailsCaptionPanel.add(groupDetailsPanel);
        
        //Create description area
        CaptionPanel descriptionCaptionPanel = new CaptionPanel(stringConstants.description());
        groupDetailsPanel.add(descriptionCaptionPanel);
        
        VerticalPanel descriptionPanel = new VerticalPanel();
        descriptionCaptionPanel.add(descriptionPanel);
        
        descriptionTextArea = new TextArea();
        descriptionTextArea.setCharacterWidth(60);
        descriptionTextArea.setVisibleLines(8);
        descriptionTextArea.getElement().getStyle().setProperty("resize", "none");
        descriptionTextArea.setReadOnly(true);
        descriptionPanel.add(descriptionTextArea);
        
        HorizontalPanel descriptionFunctionsPanel = new HorizontalPanel();
        descriptionFunctionsPanel.setSpacing(5);
        descriptionPanel.add(descriptionFunctionsPanel);
        
        editDescriptionButton = new Button(stringConstants.edit());
        editDescriptionButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                setDescriptionEditable(true);
            }
        });
        descriptionFunctionsPanel.add(editDescriptionButton);
        
        abortDescriptionButton = new Button(stringConstants.abort());
        abortDescriptionButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
                setDescriptionEditable(false);
                descriptionTextArea.setText(selectedGroup.description);
            }
        });
        abortDescriptionButton.setVisible(false);
        abortDescriptionButton.setEnabled(false);
        descriptionFunctionsPanel.add(abortDescriptionButton);
        
        saveDescriptionButton = new Button(stringConstants.save());
        saveDescriptionButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                saveDescriptionChanges();
            }
        });
        saveDescriptionButton.setEnabled(false);
        saveDescriptionButton.setVisible(false);
        descriptionFunctionsPanel.add(saveDescriptionButton);
        
        //Create leaderboard table
        TextColumn<LeaderboardDTO> groupDetailsNameColumn = new TextColumn<LeaderboardDTO>() {
            @Override
            public String getValue(LeaderboardDTO leaderboard) {
                return leaderboard.name;
            }
        };
        
        TextColumn<LeaderboardDTO> groupDetailsRacesColumn = new TextColumn<LeaderboardDTO>() {
            @Override
            public String getValue(LeaderboardDTO leaderboard) {
                String result = "";
                boolean first = true;
                for (RaceInLeaderboardDTO race : leaderboard.getRaceList()) {
                    if (!first) {
                        result += "; ";
                    }
                    result += race.getRaceColumnName();
                    first = false;
                }
                return result;
            }
        };

        groupDetailsTable = new CellTable<LeaderboardDTO>(200, tableRes);
        groupDetailsTable.setWidth("100%");
        groupDetailsTable.addColumn(groupDetailsNameColumn, stringConstants.leaderboardName());
        groupDetailsTable.addColumn(groupDetailsRacesColumn, stringConstants.races());
        
        groupDetailsSelectionModel = new MultiSelectionModel<LeaderboardDTO>();
        groupDetailsSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Set<LeaderboardDTO> selectedLeaderboardsInGroup = groupDetailsSelectionModel.getSelectedSet();
                moveToLeaderboardsButton.setEnabled(selectedLeaderboardsInGroup != null && !selectedLeaderboardsInGroup.isEmpty());
                leaderboardDownButton.setEnabled(selectedLeaderboardsInGroup != null && selectedLeaderboardsInGroup.size() == 1);
                leaderboardUpButton.setEnabled(selectedLeaderboardsInGroup != null && selectedLeaderboardsInGroup.size() == 1);
            }
        });
        groupDetailsTable.setSelectionModel(groupDetailsSelectionModel);
        
        groupDetailsProvider = new ListDataProvider<LeaderboardDTO>();
        groupDetailsProvider.addDataDisplay(groupDetailsTable);
        groupDetailsPanel.add(groupDetailsTable);
        
        //Create details functionality
        HorizontalPanel groupDetailsFunctionPanel = new HorizontalPanel();
        groupDetailsFunctionPanel.setSpacing(5);
        groupDetailsPanel.add(groupDetailsFunctionPanel);
        
        leaderboardUpButton = new Button(stringConstants.columnMoveUp());
        leaderboardUpButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveLeaderboardInGroupUp();
            }
        });
        leaderboardUpButton.setEnabled(false);
        groupDetailsFunctionPanel.add(leaderboardUpButton);
        
        leaderboardDownButton = new Button(stringConstants.columnMoveDown());
        leaderboardDownButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                moveLeaderboardInGroupDown();
            }
        });
        leaderboardDownButton.setEnabled(false);
        groupDetailsFunctionPanel.add(leaderboardDownButton);
        
        return groupDetailsCaptionPanel;
    }

    private Widget createLeaderboardGroupsGUI(Resources tableRes) {
        CaptionPanel leaderboardGroupsCaptionPanel = new CaptionPanel(stringConstants.leaderboardGroups());
        
        VerticalPanel leaderboardsGroupPanel = new VerticalPanel();
        leaderboardGroupsCaptionPanel.add(leaderboardsGroupPanel);
        
        //Create functional elements for the leaderboard groups
        HorizontalPanel leaderboardGroupsFunctionPanel = new HorizontalPanel();
        leaderboardGroupsFunctionPanel.setSpacing(5);
        leaderboardsGroupPanel.add(leaderboardGroupsFunctionPanel);
        
        Label filterLeaderboardGroupsLbl = new Label(stringConstants.filterLeaderboardGroupsByName() + ":");
        leaderboardGroupsFunctionPanel.add(filterLeaderboardGroupsLbl);
        
        groupsFilterTextBox = new TextBox();
        groupsFilterTextBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                groupsFilterChanged();
            }
        });
        leaderboardGroupsFunctionPanel.add(groupsFilterTextBox);
        
        Button createGroupButton = new Button(stringConstants.createNewLeaderboardGroup());
        createGroupButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addNewGroup();
            }
        });
        leaderboardGroupsFunctionPanel.add(createGroupButton);
        
        //Create table for leaderboard groups
        groupsProvider = new ListDataProvider<LeaderboardGroupDTO>();
        ListHandler<LeaderboardGroupDTO> leaderboardGroupsListHandler = new ListHandler<LeaderboardGroupDTO>(groupsProvider.getList());
        
        AnchorCell anchorCell = new AnchorCell();
        Column<LeaderboardGroupDTO, SafeHtml> groupNameColumn = new Column<LeaderboardGroupDTO, SafeHtml>(anchorCell) {
            @Override
            public SafeHtml getValue(LeaderboardGroupDTO group) {
                String debugParam = Window.Location.getParameter("gwt.codesvr");
                String link = URLFactory.INSTANCE.encode("/gwt/Spectator.html?leaderboardGroupName=" + group.name
                        + (debugParam != null && !debugParam.isEmpty() ? "&gwt.codesvr=" + debugParam : ""));
                return ANCHORTEMPLATE.cell(link, group.name);
            }
        };
        groupNameColumn.setSortable(true);
        leaderboardGroupsListHandler.setComparator(groupNameColumn, new Comparator<LeaderboardGroupDTO>() {
            @Override
            public int compare(LeaderboardGroupDTO group1, LeaderboardGroupDTO group2) {
                return group1.name.compareTo(group2.name);
            }
        });

        TextColumn<LeaderboardGroupDTO> groupDescriptionColumn = new TextColumn<LeaderboardGroupDTO>() {
            @Override
            public String getValue(LeaderboardGroupDTO group) {
                return group.description.length() <= 100 ? group.description : group.description.substring(0, 98) + "...";
            }
        };

        ImagesBarColumn<LeaderboardGroupDTO, LeaderboardGroupConfigImagesBarCell> groupActionsColumn = new ImagesBarColumn<LeaderboardGroupDTO, LeaderboardGroupConfigImagesBarCell>(
                new LeaderboardGroupConfigImagesBarCell(stringConstants));
        groupActionsColumn.setFieldUpdater(new FieldUpdater<LeaderboardGroupDTO, String>() {
            @Override
            public void update(int index, LeaderboardGroupDTO group, String command) {
                if (command.equals("ACTION_EDIT")) {
                    final String oldGroupName = group.name;
                    ArrayList<LeaderboardGroupDTO> otherExistingGroups = new ArrayList<LeaderboardGroupDTO>(availableLeaderboardGroups);
                    otherExistingGroups.remove(group);
                    LeaderboardGroupEditDialog dialog = new LeaderboardGroupEditDialog(group, otherExistingGroups, stringConstants, new AsyncCallback<LeaderboardGroupDTO>() {
                        @Override
                        public void onFailure(Throwable t) {}
                        @Override
                        public void onSuccess(LeaderboardGroupDTO group) {
                            updateGroup(oldGroupName, group);
                        }
                    });
                    dialog.show();
                } else if (command.equals("ACTION_REMOVE")) {
                    if (Window.confirm("Do you really want to remove the leaderboard group: '" + group.name + "' ?")) {
                        removeLeaderboardGroup(group);
                    }
                }
            }
        });

        groupsTable = new CellTable<LeaderboardGroupDTO>(200, tableRes);
        groupsTable.setWidth("100%");
        groupsTable.addColumn(groupNameColumn, stringConstants.name());
        groupsTable.addColumn(groupDescriptionColumn, stringConstants.description());
        groupsTable.addColumn(groupActionsColumn, stringConstants.actions());
        groupsTable.addColumnSortHandler(leaderboardGroupsListHandler);
        
        groupsSelectionModel = new SingleSelectionModel<LeaderboardGroupDTO>();
        groupsSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                groupSelectionChanged();
            }
        });
        groupsTable.setSelectionModel(groupsSelectionModel);
        
        groupsProvider.addDataDisplay(groupsTable);
        leaderboardsGroupPanel.add(groupsTable);
        
        return leaderboardGroupsCaptionPanel;
    }
    
    private void loadGroups() {
        sailingService.getLeaderboardGroups(new AsyncCallback<List<LeaderboardGroupDTO>>() {
            @Override
            public void onSuccess(List<LeaderboardGroupDTO> groups) {
                availableLeaderboardGroups.clear();
                if (groups != null) {
                    availableLeaderboardGroups.addAll(groups);
                }
                groupsProvider.getList().clear();
                groupsProvider.getList().addAll(availableLeaderboardGroups);
            }
            @Override
            public void onFailure(Throwable t) {
            }
        });
    }
    
    private void loadLeaderboards() {
        sailingService.getLeaderboards(new AsyncCallback<List<LeaderboardDTO>>() {
            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError("Error trying to obtain list of leaderboards: " + t.getMessage());
            }
            @Override
            public void onSuccess(List<LeaderboardDTO> leaderboards) {
                availableLeaderboards.clear();
                if (leaderboards != null) {
                    availableLeaderboards.addAll(leaderboards);
                }
                leaderboardsProvider.getList().clear();
                leaderboardsProvider.getList().addAll(availableLeaderboards);
            }
        });
    }
    
    private void refreshLeaderboardsList() {
        final LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
        final Set<LeaderboardDTO> selectedLeaderboards = leaderboardsSelectionModel.getSelectedSet();
        sailingService.getLeaderboards(new AsyncCallback<List<LeaderboardDTO>>() {
            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError("Error trying to obtain list of leaderboards: " + t.getMessage());
            }
            @Override
            public void onSuccess(List<LeaderboardDTO> leaderboards) {
                availableLeaderboards.clear();
                if (leaderboards != null) {
                    availableLeaderboards.addAll(leaderboards);
                }
                leaderboardsProvider.getList().clear();
                leaderboardsProvider.getList().addAll(availableLeaderboards);
                if (selectedGroup != null) {
                    leaderboardsProvider.getList().removeAll(selectedGroup.leaderboards);
                }
                leaderboardsFilterTextBox.setText("");
                leaderboardsSelectionModel.clear();
                for (LeaderboardDTO leaderboard : selectedLeaderboards) {
                    leaderboardsSelectionModel.setSelected(leaderboard, true);
                }
            }
        });
    }

    private void addNewGroup() {
        LeaderboardGroupCreateDialog dialog = new LeaderboardGroupCreateDialog(
                Collections.unmodifiableCollection(availableLeaderboardGroups), stringConstants,
                new AsyncCallback<LeaderboardGroupDTO>() {
            @Override
            public void onFailure(Throwable t) {}
            @Override
            public void onSuccess(LeaderboardGroupDTO newGroup) {
                createNewGroup(newGroup);
            }
        });
        dialog.show();
    }
    
    private void createNewGroup(final LeaderboardGroupDTO newGroup) {
        sailingService.createLeaderboardGroup(newGroup.name, newGroup.description, new AsyncCallback<LeaderboardGroupDTO>() {
            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError("Error trying to create new leaderboard group" + newGroup.name
                        + ": " + t.getMessage());
            }
            @Override
            public void onSuccess(LeaderboardGroupDTO newGroup) {
                availableLeaderboardGroups.add(newGroup);
                groupsProvider.getList().add(newGroup);
                groupsSelectionModel.setSelected(newGroup, true);
                groupSelectionChanged();
            }
        });
    }
    
    private void updateGroup(final String oldGroupName, final LeaderboardGroupDTO groupToUpdate) {
        sailingService.updateLeaderboardGroup(oldGroupName, groupToUpdate.name, groupToUpdate.description, groupToUpdate.leaderboards, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError("Error trying to update leaderboard group " + oldGroupName + ": "
                        + t.getMessage());
            }
            @Override
            public void onSuccess(Void v) {
                //Update the availableLeaderboardGroups and the list of displayed groups
                for (int i = 0; i < availableLeaderboardGroups.size(); i++) {
                    LeaderboardGroupDTO group = availableLeaderboardGroups.get(i);
                    if (oldGroupName.equals(group.name)) {
                        availableLeaderboardGroups.set(i, groupToUpdate);
                        int displayedIndex = groupsProvider.getList().indexOf(group);
                        if (displayedIndex != -1) {
                            groupsProvider.getList().set(displayedIndex, groupToUpdate);
                        }
                    }
                }
                groupsProvider.refresh();
            }
        });
    }
    private void updateGroup(final LeaderboardGroupDTO group) {
        updateGroup(group.name, group);
    }

    private void removeLeaderboardGroup(final LeaderboardGroupDTO group) {
        sailingService.removeLeaderboardGroup(group.name, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError("Error trying to remove leaderboard group " + group.name + ": "
                        + t.getMessage());
            }
            @Override
            public void onSuccess(Void v) {
                availableLeaderboardGroups.remove(group);
                groupsProvider.getList().remove(group);
                
                //Check if the removed group was the selected one
                LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
                if (selectedGroup != null && selectedGroup.name.equals(group.name)) {
                    groupsSelectionModel.setSelected(null, true);
                }
            }
        });
    }
    
    private void groupsFilterChanged() {
        List<String> filter = Arrays.asList(groupsFilterTextBox.getText().split("\\s"));
        groupsProvider.getList().clear();
        for (LeaderboardGroupDTO group : availableLeaderboardGroups) {
            if (!textContainingStringsToCheck(filter, group.name)) {
                groupsProvider.getList().add(group);
            }
        }
        //Now sort again according to selected criterion
        ColumnSortEvent.fire(groupsTable, groupsTable.getColumnSortList());
    }
    
    private void groupSelectionChanged() {
        final LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
        splitPanel.setVisible(selectedGroup != null);
        if (selectedGroup != null) {
            sailingService.getLeaderboardGroupByName(selectedGroup.name, new AsyncCallback<LeaderboardGroupDTO>() {
                @Override
                public void onFailure(Throwable t) {
                    errorReporter.reportError("Error trying to obtain the leaderboard group " + selectedGroup.name + ": " + t.getMessage());
                }
                @Override
                public void onSuccess(LeaderboardGroupDTO result) {
                    //Updating the data lists
                    availableLeaderboardGroups.set(availableLeaderboardGroups.indexOf(selectedGroup), result);
                    groupsProvider.getList().clear();
                    groupsProvider.getList().addAll(availableLeaderboardGroups);
                    groupsSelectionModel.setSelected(result, true);

                    //Display details of the group
                    groupDetailsCaptionPanel.setCaptionText(stringConstants.detailsOfLeaderboardGroup() + " '" + result.name + "'");
                    descriptionTextArea.setText(result.description);
                    setDescriptionEditable(false);
                    
                    groupDetailsSelectionModel.clear();
                    groupDetailsProvider.getList().clear();
                    groupDetailsProvider.getList().addAll(result.leaderboards);
                    
                    //Reload available leaderboards and remove leaderboards of the group from the list
                    leaderboardsSelectionModel.clear();
                    leaderboardsFilterTextBox.setText("");
                    leaderboardsProvider.getList().clear();
                    leaderboardsProvider.getList().addAll(availableLeaderboards);
                    leaderboardsProvider.getList().removeAll(result.leaderboards);
                }
            });
        }
    }
    
    private void leaderboardsFilterChanged() {
        LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
        List<String> filter = Arrays.asList(leaderboardsFilterTextBox.getText().split("\\s"));
        leaderboardsProvider.getList().clear();
        for (LeaderboardDTO leaderboard : availableLeaderboards) {
            if (!textContainingStringsToCheck(filter, leaderboard.name) && !selectedGroup.leaderboards.contains(leaderboard)) {
                leaderboardsProvider.getList().add(leaderboard);
            } else {
                leaderboardsSelectionModel.setSelected(leaderboard, false);
            }
        }
        //Now sort again according to selected criterion
        ColumnSortEvent.fire(leaderboardsTable, leaderboardsTable.getColumnSortList());
    }
    
    private void moveToLeaderboards() {
        LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
        Set<LeaderboardDTO> selectedLeaderboards = groupDetailsSelectionModel.getSelectedSet();
        if (selectedGroup != null && selectedLeaderboards != null && selectedLeaderboards.size() > 0) {
            for (LeaderboardDTO leaderboard : selectedLeaderboards) {
                selectedGroup.leaderboards.remove(leaderboard);
                groupDetailsProvider.getList().remove(leaderboard);
                groupDetailsSelectionModel.setSelected(leaderboard, false);
                leaderboardsProvider.getList().add(leaderboard);
            }
            updateGroup(selectedGroup);
            //Refilters the leaderboards list (hides the moved leaderboards if they don't fit to the filter) and resorts the list
            leaderboardsFilterChanged();
        }
    }
    
    private void moveToGroup() {
        LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
        Set<LeaderboardDTO> selectedLeaderboards = leaderboardsSelectionModel.getSelectedSet();
        if (selectedGroup != null && selectedLeaderboards != null && selectedLeaderboards.size() > 0) {
            for (LeaderboardDTO leaderboard : selectedLeaderboards) {
                if (!selectedGroup.leaderboards.contains(leaderboard)) {
                    selectedGroup.leaderboards.add(leaderboard);
                    groupDetailsProvider.getList().add(leaderboard);
                    leaderboardsProvider.getList().remove(leaderboard);
                    leaderboardsSelectionModel.setSelected(leaderboard, false);
                }
            }
            updateGroup(selectedGroup);
        }
    }
    
    private void moveLeaderboardInGroupUp() {
        LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject(); 
        Set<LeaderboardDTO> selectedLeaderboards = groupDetailsSelectionModel.getSelectedSet();
        if (selectedLeaderboards != null && selectedLeaderboards.size() == 1) {
            LeaderboardDTO selectedLeaderboard = selectedLeaderboards.iterator().next();
            moveLeaderboardInGroup(selectedGroup, selectedLeaderboard, -1);
        }
    }
    
    private void moveLeaderboardInGroupDown() {
        LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject(); 
        Set<LeaderboardDTO> selectedLeaderboards = groupDetailsSelectionModel.getSelectedSet();
        if (selectedLeaderboards != null && selectedLeaderboards.size() == 1) {
            LeaderboardDTO selectedLeaderboard = selectedLeaderboards.iterator().next();
            moveLeaderboardInGroup(selectedGroup, selectedLeaderboard, 1);
        }
    }
    
    private void moveLeaderboardInGroup(LeaderboardGroupDTO group, LeaderboardDTO leaderboard, int direction) {
        int index = group.leaderboards.indexOf(leaderboard);
        int destIndex = index + direction;
        if (destIndex >= 0 && destIndex < group.leaderboards.size()) {
            LeaderboardDTO temp = group.leaderboards.get(destIndex);
            group.leaderboards.set(destIndex, leaderboard);
            group.leaderboards.set(index, temp);
            groupDetailsProvider.getList().clear();
            groupDetailsProvider.getList().addAll(group.leaderboards);
            
            updateGroup(group);
        }
    }

    @Override
    public void fillEvents(List<EventDTO> result) {
    }

    private void setDescriptionEditable(boolean isEditable) {
        LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
        if (selectedGroup != null) {
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
        LeaderboardGroupDTO selectedGroup = groupsSelectionModel.getSelectedObject();
        if (newDescription != null && newDescription.length() > 0) {
            selectedGroup.description = newDescription;
            setDescriptionEditable(false);
            updateGroup(selectedGroup);
        } else {
            Window.alert(stringConstants.pleaseEnterNonEmptyDescription() + ".");
            descriptionTextArea.setText(selectedGroup.description);
        }
    }
    
}
