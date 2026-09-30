package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_UPDATE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.courseCreation.CourseTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkRoleDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkTemplateDTO;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.ErrorReporter;
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
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

public class CourseTemplatePanel extends FlowPanel implements FilterablePanelProvider<CourseTemplateDTO>{
    private static AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);

    private final SailingServiceWriteAsync sailingService;
    private final LabeledAbstractFilterablePanel<CourseTemplateDTO> filterableCourseTemplatePanel;
    private List<CourseTemplateDTO> allCourseTemplates;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private FlushableCellTable<CourseTemplateDTO> courseTemplateTable;
    private ListDataProvider<CourseTemplateDTO> courseTemplateListDataProvider = new ListDataProvider<>();
    private RefreshableMultiSelectionModel<CourseTemplateDTO> refreshableSelectionModel;
    private List<MarkRoleDTO> allMarkRoles;
    private List<MarkTemplateDTO> allMarkTemplates;

    public CourseTemplatePanel(SailingServiceWriteAsync sailingServiceWrite, ErrorReporter errorReporter,
            StringMessages stringMessages, final UserService userService) {
        this.sailingService = sailingServiceWrite;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        AccessControlledButtonPanel buttonAndFilterPanel = new AccessControlledButtonPanel(userService,
                SecuredDomainType.MARK_TEMPLATE);
        add(buttonAndFilterPanel);
        allCourseTemplates = new ArrayList<>();
        Label lblFilterRaces = new Label(stringMessages.filterCourseTemplateByName() + ":");
        lblFilterRaces.setWordWrap(false);
        this.filterableCourseTemplatePanel = new LabeledAbstractFilterablePanel<CourseTemplateDTO>(lblFilterRaces,
                allCourseTemplates, courseTemplateListDataProvider, stringMessages) {
            @Override
            public List<String> getSearchableStrings(CourseTemplateDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(t.getUuid().toString());
                return strings;
            }

            @Override
            public AbstractCellTable<CourseTemplateDTO> getCellTable() {
                return courseTemplateTable;
            }
        };
        filterableCourseTemplatePanel.getTextBox().ensureDebugId("CourseTemplateFilterTextBox");
        createCourseTemplateTable(userService);
        buttonAndFilterPanel.addUnsecuredAction(stringMessages.refresh(), this::loadCourseTemplates);
        buttonAndFilterPanel.addCreateAction(stringMessages.add(),
                () -> openEditCourseTemplateDialog(new CourseTemplateDTO(), userService, true));
        buttonAndFilterPanel.addRemoveAction(stringMessages.remove(), refreshableSelectionModel, true,
                () -> {final List<UUID> uuids = refreshableSelectionModel.getSelectedSet().stream().map(courseTemplateDTO -> courseTemplateDTO.getUuid()).collect(Collectors.toList());
            removeCourseTemplates(uuids);
        });
        buttonAndFilterPanel.addUnsecuredWidget(lblFilterRaces);
        buttonAndFilterPanel.addUnsecuredWidget(filterableCourseTemplatePanel);
        filterableCourseTemplatePanel
                .setUpdatePermissionFilterForCheckbox(event -> userService.hasPermission(event, DefaultActions.UPDATE));
    }

    private void removeCourseTemplates(Collection<UUID> courseTemplatesUuids) {
        if (!courseTemplatesUuids.isEmpty()) {
            sailingService.removeCourseTemplates(courseTemplatesUuids, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error trying to remove course teamplates:" + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    refreshCourseTemplates();
                }
            });
        }
    }

    public void loadCourseTemplates() {
        courseTemplateListDataProvider.getList().clear();
        sailingService.getCourseTemplates(new AsyncCallback<List<CourseTemplateDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.toString());
            }

            @Override
            public void onSuccess(List<CourseTemplateDTO> result) {
                courseTemplateListDataProvider.getList().clear();
                courseTemplateListDataProvider.getList().addAll(result);
                filterableCourseTemplatePanel.updateAll(courseTemplateListDataProvider.getList());
                courseTemplateListDataProvider.refresh();
            }
        });
    }

    private void loadMarkRoles() {
        sailingService.getMarkRoles(new AsyncCallback<List<MarkRoleDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.toString());
            }

            @Override
            public void onSuccess(List<MarkRoleDTO> markRoles) {
                allMarkRoles = markRoles;
            }
        });
    }

    private void loadMarkTemplates() {
        sailingService.getMarkTemplates(new AsyncCallback<List<MarkTemplateDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.toString());
            }

            @Override
            public void onSuccess(List<MarkTemplateDTO> markTemplateDTOs) {
                allMarkTemplates = markTemplateDTOs;
            }
        });
    }

    private void createCourseTemplateTable(final UserService userService) {
        courseTemplateTable = new FlushableCellTable<>(1000, tableResources);
        courseTemplateTable.setWidth("100%");
        // Attach a column sort handler to the ListDataProvider to sort the list.
        ListHandler<CourseTemplateDTO> sortHandler = new ListHandler<>(courseTemplateListDataProvider.getList());
        courseTemplateTable.addColumnSortHandler(sortHandler);
        // Initialize the columns.
        initTableColumns(sortHandler, userService);
        courseTemplateListDataProvider.addDataDisplay(courseTemplateTable);
        add(courseTemplateTable);
        allCourseTemplates.clear();
        allCourseTemplates.addAll(courseTemplateListDataProvider.getList());
    }

    /**
     * Add the columns to the table.
     */
    private void initTableColumns(final ListHandler<CourseTemplateDTO> sortHandler, final UserService userService) {
        final SelectionCheckboxColumn<CourseTemplateDTO> checkColumn = new SelectionCheckboxColumn<CourseTemplateDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(), new EntityIdentityComparator<CourseTemplateDTO>() {
                    @Override
                    public boolean representSameEntity(CourseTemplateDTO dto1, CourseTemplateDTO dto2) {
                        return dto1.getUuid().equals(dto2.getUuid());
                    }
                    @Override
                    public int hashCode(CourseTemplateDTO t) {
                        return t.getUuid().hashCode();
                    }
                }, filterableCourseTemplatePanel.getAllListDataProvider());
        final Header<Boolean> selectAllHeader = checkColumn.createHeader();
        courseTemplateTable.addColumn(checkColumn, selectAllHeader);
        courseTemplateTable.setColumnWidth(checkColumn, 40, Unit.PX);
        // id
        Column<CourseTemplateDTO, String> idColumn = new Column<CourseTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(CourseTemplateDTO courseTemplate) {
                return courseTemplate.getUuid().toString();
            }
        };
        // name
        Column<CourseTemplateDTO, String> nameColumn = new Column<CourseTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(CourseTemplateDTO courseTemplate) {
                return courseTemplate.getName();
            }
        };
        // url
        Column<CourseTemplateDTO, String> urlColumn = new Column<CourseTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(CourseTemplateDTO courseTemplate) {
                return courseTemplate.getOptionalImageUrl().orElse("");
            }
        };
        // tags
        Column<CourseTemplateDTO, String> tagsColumn = new Column<CourseTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(CourseTemplateDTO courseTemplate) {
                return String.join(", ", courseTemplate.getTags());
            }
        };
        // # Waypoint Templates
        Column<CourseTemplateDTO, String> waypointTemplateCountColumn = new Column<CourseTemplateDTO, String>(
                new TextCell()) {
            @Override
            public String getValue(CourseTemplateDTO courseTemplate) {
                return Integer.toString(courseTemplate.getWaypointTemplates().size());
            }
        };
        nameColumn.setSortable(true);
        sortHandler.setComparator(nameColumn, new Comparator<CourseTemplateDTO>() {
            public int compare(CourseTemplateDTO courseTemplate1, CourseTemplateDTO courseTemplate2) {
                return courseTemplate1.getName().compareTo(courseTemplate2.getName());
            }
        });
        courseTemplateTable.addColumn(nameColumn, stringMessages.name());
        courseTemplateTable.addColumn(urlColumn, stringMessages.url());
        courseTemplateTable.addColumn(tagsColumn, stringMessages.tags());
        courseTemplateTable.addColumn(waypointTemplateCountColumn, stringMessages.waypoints());
        SecuredDTOOwnerColumn.configureOwnerColumns(courseTemplateTable, sortHandler, stringMessages);
        final HasPermissions type = SecuredDomainType.COURSE_TEMPLATE;
        final AccessControlledActionsColumn<CourseTemplateDTO, DefaultActionsImagesBarCell> actionsColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        final EditOwnershipDialog.DialogConfig<CourseTemplateDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, courseTemplateDTO -> courseTemplateListDataProvider.refresh(), stringMessages);
        final EditACLDialog.DialogConfig<CourseTemplateDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, courseTemplate -> courseTemplate.getAccessControlList(),
                stringMessages);
        actionsColumn.addAction(ACTION_DELETE, DELETE, e -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveCourseTemplate(e.getName()))) {
                sailingService.removeCourseTemplates(Collections.singletonList(e.getUuid()), new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.couldNotRemoveCourseTemplate(caught.getMessage()));
                    }

                    @Override
                    public void onSuccess(Void result) {
                        refreshCourseTemplates();
                    }
                });
            }
        });
        actionsColumn.addAction(ACTION_UPDATE, UPDATE, e -> openEditCourseTemplateDialog(e, userService, false));
        actionsColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, configOwnership::openOwnershipDialog);
        actionsColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                courseTemplate -> configACL.openDialog(courseTemplate));
        courseTemplateTable.addColumn(idColumn, stringMessages.id());
        courseTemplateTable.addColumn(actionsColumn, stringMessages.actions());
        refreshableSelectionModel = checkColumn.getSelectionModel();
        courseTemplateTable.setSelectionModel(refreshableSelectionModel, checkColumn.getSelectionManager());
    }
    
    public void refreshCourseTemplates() {
        loadCourseTemplates();
        loadMarkRoles();
        loadMarkTemplates();
    }

    void openEditCourseTemplateDialog(final CourseTemplateDTO originalCourseTemplate, final UserService userService,
            final boolean isNew) {
        final CourseTemplateEditDialog dialog = new CourseTemplateEditDialog(sailingService, stringMessages,
                originalCourseTemplate, allMarkRoles, allMarkTemplates, new DialogCallback<CourseTemplateDTO>() {
                    @Override
                    public void ok(CourseTemplateDTO courseTemplate) {
                        sailingService.createOrUpdateCourseTemplate(courseTemplate,
                                new AsyncCallback<CourseTemplateDTO>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError(
                                                "Error trying to store course template: " + caught.getMessage());
                                    }

                                    @Override
                                    public void onSuccess(CourseTemplateDTO updatedCourseTemplate) {
                                        final int editedCourseTemplateIndex;
                                        if (originalCourseTemplate != null) {
                                            editedCourseTemplateIndex = filterableCourseTemplatePanel
                                                    .indexOf(originalCourseTemplate);
                                            filterableCourseTemplatePanel.remove(originalCourseTemplate);
                                        } else {
                                            editedCourseTemplateIndex = -1;
                                        }
                                        if (editedCourseTemplateIndex >= 0) {
                                            filterableCourseTemplatePanel.add(editedCourseTemplateIndex,
                                                    updatedCourseTemplate);
                                        } else {
                                            filterableCourseTemplatePanel.add(updatedCourseTemplate);
                                        }
                                        courseTemplateListDataProvider.refresh();
                                    }
                                });
                    }

                    @Override
                    public void cancel() {
                    }
                },
                isNew);
        dialog.ensureDebugId("CourseTemplateEditDialog");
        dialog.show();
    }

    @Override
    public AbstractFilterablePanel<CourseTemplateDTO> getFilterablePanel() {
        return filterableCourseTemplatePanel;
    }

}
