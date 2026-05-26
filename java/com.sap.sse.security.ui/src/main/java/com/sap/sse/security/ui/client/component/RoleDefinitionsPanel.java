package com.sap.sse.security.ui.client.component;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.shared.impl.SecuredSecurityTypes.ROLE_DEFINITION;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_UPDATE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SetSelectionModel;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.AbstractSortableTextColumn;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.RoleDefinitionDTO;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.ui.client.UserManagementWriteServiceAsync;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog.DialogConfig;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;
import com.sap.sse.security.ui.client.i18n.StringMessages;

/**
 * Displays and allows users to edit {@link Role}s. This includes creating and removing
 * roles as well as changing the sets of permissions implied by a role.<p>
 * 
 * The panel is <em>not</em> a standalone top-level AdminConsole panel but just a widget
 * that can be included, e.g., in a user management panel.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class RoleDefinitionsPanel extends VerticalPanel {
    private final FlushableCellTable<RoleDefinitionDTO> roleDefinitionsTable;
    private final ErrorReporter errorReporter;
    private final UserService userService;
    private final UserManagementWriteServiceAsync userManagementWriteService;
    private final ListDataProvider<RoleDefinitionDTO> rolesListDataProvider;
    private final StringMessages stringMessages;
    protected final LabeledAbstractFilterablePanel<RoleDefinitionDTO> filterablePanelRoleDefinitions;
    private final RefreshableMultiSelectionModel<? super RoleDefinitionDTO> refreshableRoleDefinitionMultiSelectionModel;
    
    public RoleDefinitionsPanel(StringMessages stringMessages, UserService userService,
            CellTableWithCheckboxResources tableResources, ErrorReporter errorReporter) {
        this.ensureDebugId(this.getClass().getSimpleName());
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        this.userService = userService;
        // using write service to enforce round trip going to master for consistency purposes
        this.userManagementWriteService = userService.getUserManagementWriteService();
        rolesListDataProvider = new ListDataProvider<>();
        filterablePanelRoleDefinitions = new LabeledAbstractFilterablePanel<RoleDefinitionDTO>(new Label(stringMessages.filterRoles()), new ArrayList<>(),
                rolesListDataProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(RoleDefinitionDTO roleDefinition) {
                return Arrays.asList(roleDefinition.getName(), roleDefinition.getId().toString(), roleDefinition.getPermissions().toString());
            }
            
            @Override
            public AbstractCellTable<RoleDefinitionDTO> getCellTable() {
                return roleDefinitionsTable;
            }
        };
        filterablePanelRoleDefinitions
                .setUpdatePermissionFilterForCheckbox(role -> userService.hasPermission(role, DefaultActions.UPDATE));
        roleDefinitionsTable = createRoleDefinitionsTable(tableResources);
        roleDefinitionsTable.ensureDebugId("RolesCellTable");
        filterablePanelRoleDefinitions.getTextBox().ensureDebugId("RolesFilterTextBox");
        refreshableRoleDefinitionMultiSelectionModel = (RefreshableMultiSelectionModel<? super RoleDefinitionDTO>) roleDefinitionsTable.getSelectionModel();
        @SuppressWarnings("unchecked")
        final SetSelectionModel<RoleDefinitionDTO> roleSelectionModel = (SetSelectionModel<RoleDefinitionDTO>) roleDefinitionsTable.getSelectionModel();
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, ROLE_DEFINITION);
        buttonPanel.addUnsecuredAction(stringMessages.refresh(), this::updateRoleDefinitions);
        final Button createButton = buttonPanel.addCreateActionWithoutServerCreateObjectPermissionCheck(stringMessages.add(),
                this::createRoleDefinition);
        createButton.ensureDebugId("CreateRoleButton");
        final Button removeButton = buttonPanel.addRemoveAction(stringMessages.remove(), roleSelectionModel, true,
                () -> {
            final Set<RoleDefinitionDTO> selectedRoles = new HashSet<>(getSelectedRoleDefinitions());
            filterablePanelRoleDefinitions.removeAll(selectedRoles);
        });
        removeButton.ensureDebugId("RemoveRoleButton");
        add(buttonPanel);
        add(filterablePanelRoleDefinitions);
        add(roleDefinitionsTable);
        updateRoleDefinitions();
    }
    
    private void createRoleDefinition() {
        new RoleDefinitionCreationDialog(stringMessages, getAllPermissions(), getAllRoleDefinitions(), new DialogCallback<RoleDefinitionDTO>() {
            @Override
            public void ok(RoleDefinitionDTO editedObject) {
                    userManagementWriteService.createRoleDefinition(editedObject.getId().toString(),
                                editedObject.getName(), new AsyncCallback<RoleDefinitionDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorCreatingRole(editedObject.getName(), caught.getMessage()));
                    }

                    @Override
                    public void onSuccess(RoleDefinitionDTO result) {
                        userManagementWriteService.updateRoleDefinition(editedObject, new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(stringMessages.errorEditingRoles(editedObject.getName(), caught.getMessage()));
                            }

                            @Override
                            public void onSuccess(Void result) {
                                updateRoleDefinitions();
                            }
                        });
                    }
                });
            }

            @Override
            public void cancel() {
                //no-op
            }
        }).show();
    }

    private Set<RoleDefinitionDTO> getSelectedRoleDefinitions() {
        @SuppressWarnings("unchecked")
        final Set<RoleDefinitionDTO> result = (Set<RoleDefinitionDTO>) refreshableRoleDefinitionMultiSelectionModel.getSelectedSet();
        return result;
    }
    
    private FlushableCellTable<RoleDefinitionDTO> createRoleDefinitionsTable(CellTableWithCheckboxResources tableResources) {
        final FlushableCellTable<RoleDefinitionDTO> table = new FlushableCellTable<>(/* pageSize */ 50, tableResources);
        rolesListDataProvider.addDataDisplay(table);
        final SelectionCheckboxColumn<RoleDefinitionDTO> roleSelectionCheckboxColumn = new SelectionCheckboxColumn<RoleDefinitionDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(), new EntityIdentityComparator<RoleDefinitionDTO>() {
                    @Override
                    public boolean representSameEntity(RoleDefinitionDTO roleDefinition1, RoleDefinitionDTO roleDefinition2) {
                        return roleDefinition1.getId().equals(roleDefinition2.getId());
                    }
                    @Override
                    public int hashCode(RoleDefinitionDTO t) {
                        return t.getId().hashCode();
                    }
                }, filterablePanelRoleDefinitions.getAllListDataProvider());
        final ListHandler<RoleDefinitionDTO> columnSortHandler = new ListHandler<>(rolesListDataProvider.getList());
        table.addColumnSortHandler(columnSortHandler);
        final TextColumn<RoleDefinitionDTO> roleDefinitionUUidColumn = new AbstractSortableTextColumn<RoleDefinitionDTO>(
                role -> role.getId() == null ? "<null>" : role.getId().toString(), columnSortHandler);
        final TextColumn<RoleDefinitionDTO> roleDefinitionNameColumn = new AbstractSortableTextColumn<RoleDefinitionDTO>(role->role.getName(), columnSortHandler);
        final Column<RoleDefinitionDTO, SafeHtml> permissionsColumn = new Column<RoleDefinitionDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(RoleDefinitionDTO role) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                for (Iterator<WildcardPermission> permissionIter=role.getPermissions().iterator(); permissionIter.hasNext(); ) {
                    final WildcardPermission permission = permissionIter.next();
                    builder.appendEscaped(permission.toString());
                    if (permissionIter.hasNext()) {
                        builder.appendHtmlConstant("<br>");
                    }
                }
                return builder.toSafeHtml();
            }
        };
        permissionsColumn.setSortable(true);
        columnSortHandler.setComparator(permissionsColumn, new Comparator<RoleDefinitionDTO>() {
            @Override
            public int compare(RoleDefinitionDTO r1, RoleDefinitionDTO r2) {
                return new NaturalComparator().compare(r1.getPermissions().toString(), r2.getPermissions().toString());
            }
        });
        final HasPermissions type = SecuredSecurityTypes.ROLE_DEFINITION;
        final AccessControlledActionsColumn<RoleDefinitionDTO, DefaultActionsImagesBarCell> roleActionColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        roleActionColumn.addAction(ACTION_UPDATE, UPDATE, this::editRole);
        roleActionColumn.addAction(ACTION_DELETE, DELETE, roleDefinition -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveRole(roleDefinition.getName()))) {
                removeRole(roleDefinition);
            }
        });
        final DialogConfig<RoleDefinitionDTO> config = EditOwnershipDialog.create(userManagementWriteService, type,
                roleDefinition -> updateRoleDefinitions(), stringMessages);
        roleActionColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, config::openOwnershipDialog);
        final EditACLDialog.DialogConfig<RoleDefinitionDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, roleDefinition -> updateRoleDefinitions(),
                stringMessages);
        roleActionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                configACL::openDialog);
        final Header<Boolean> selectAllHeader = roleSelectionCheckboxColumn.createHeader();
        table.addColumn(roleSelectionCheckboxColumn, selectAllHeader);
        table.addColumn(roleDefinitionNameColumn, stringMessages.name());
        table.addColumn(permissionsColumn, stringMessages.permissions());
        SecuredDTOOwnerColumn.configureOwnerColumns(table, columnSortHandler, stringMessages);
        table.addColumn(roleDefinitionUUidColumn, stringMessages.id());
        table.addColumn(roleActionColumn, stringMessages.actions());
        table.setSelectionModel(roleSelectionCheckboxColumn.getSelectionModel(), roleSelectionCheckboxColumn.getSelectionManager());
        return table;
    }

    private void removeRole(RoleDefinition roleDefinition) {
        userManagementWriteService.deleteRoleDefinition(roleDefinition.getId().toString(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorTryingToDeleteRole(roleDefinition.getName(), caught.getMessage()));
            }

            @Override
            public void onSuccess(Void result) {
                updateRoleDefinitions();
            }
        });
    }

    private void editRole(RoleDefinition role) {
        final Set<RoleDefinitionDTO> allOtherRoles = getAllOtherRoles(role);
        Set<WildcardPermission> allPermissionsAsStrings = getAllPermissions();
        new RoleDefinitionEditDialog(role, stringMessages, allPermissionsAsStrings, allOtherRoles,
                new DialogCallback<RoleDefinitionDTO>() {
                    @Override
                    public void ok(RoleDefinitionDTO editedObject) {
                        userManagementWriteService.updateRoleDefinition(editedObject, new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(stringMessages.errorEditingRoles(editedObject.getName(), caught.getMessage()));
                            }

                            @Override
                            public void onSuccess(Void result) {
                                updateRoleDefinitions();
                            }
                        });
                    }

                    @Override
                    public void cancel() {
                    }
                }).show();
    }

    private Set<WildcardPermission> getAllPermissions() {
        Set<WildcardPermission> allPermissionsAsStrings = new HashSet<>();
        for (HasPermissions permission : userService.getAllKnownPermissions()) {
            for (HasPermissions.Action action : permission.getAvailableActions()) {
                allPermissionsAsStrings.add(permission.getPermission(action));
            }
        }
        return allPermissionsAsStrings;
    }

    private Set<RoleDefinitionDTO> getAllOtherRoles(RoleDefinition role) {
        final Set<RoleDefinitionDTO> allOtherRoles = getAllRoleDefinitions();
        allOtherRoles.remove(role);
        return allOtherRoles;
    }

    private Set<RoleDefinitionDTO> getAllRoleDefinitions() {
        final Set<RoleDefinitionDTO> allOtherRoles = new HashSet<>();
        Util.addAll(filterablePanelRoleDefinitions.getAll(), allOtherRoles);
        return allOtherRoles;
    }

    public void updateRoleDefinitions() {
        userManagementWriteService.getRoleDefinitions(new AsyncCallback<ArrayList<RoleDefinitionDTO>>() {
            @Override
            public void onSuccess(ArrayList<RoleDefinitionDTO> allRoles) {
                filterablePanelRoleDefinitions.updateAll(allRoles);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorTryingToLoadRoles(caught.getMessage()));
            }
        });
    }
}
