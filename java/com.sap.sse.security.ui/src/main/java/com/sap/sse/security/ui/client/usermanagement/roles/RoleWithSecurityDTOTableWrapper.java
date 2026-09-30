package com.sap.sse.security.ui.client.usermanagement.roles;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.AbstractSortableTextColumn;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSingleSelectionModel;
import com.sap.sse.gwt.client.celltable.TableWrapper;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.dto.RoleWithSecurityDTO;
import com.sap.sse.security.shared.dto.StrippedUserDTO;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.client.usermanagement.PermissionAndRoleImagesBarCell;
import com.sap.sse.security.ui.shared.SuccessInfo;

/**
 * A wrapper for a CellTable displaying the roles associated with the selected user. The table shows the name of the
 * role and a button to delete it from the user.
 */
public class RoleWithSecurityDTOTableWrapper extends
        TableWrapper<RoleWithSecurityDTO, RefreshableSingleSelectionModel<RoleWithSecurityDTO>, StringMessages, CellTableWithCheckboxResources> {

    private final LabeledAbstractFilterablePanel<RoleWithSecurityDTO> filterField;
    private final SingleSelectionModel<UserDTO> userSelectionModel;

    public RoleWithSecurityDTOTableWrapper(final UserService userService, final StringMessages stringMessages,
            final ErrorReporter errorReporter, boolean enablePager, final CellTableWithCheckboxResources tableResources,
            final SingleSelectionModel<UserDTO> userSelectionModel, final Runnable updateUsers) {
        super(stringMessages, errorReporter, false, enablePager, new EntityIdentityComparator<RoleWithSecurityDTO>() {
            @Override
            public boolean representSameEntity(RoleWithSecurityDTO dto1, RoleWithSecurityDTO dto2) {
                return dto1.getIdentifier().equals(dto2.getIdentifier());
            }

            @Override
            public int hashCode(RoleWithSecurityDTO t) {
                return t.getIdentifier().hashCode();
            }
        }, tableResources);
        this.userSelectionModel = userSelectionModel;
        this.userSelectionModel.addSelectionChangeHandler(e -> refreshRoleList());
        final ListHandler<RoleWithSecurityDTO> userColumnListHandler = getColumnSortHandler();
        // users table
        final TextColumn<RoleWithSecurityDTO> userGroupWithSecurityDTONameColumn = new AbstractSortableTextColumn<RoleWithSecurityDTO>(
                dto -> dto.toString(), userColumnListHandler);
        final TextColumn<RoleWithSecurityDTO> userGroupWithSecurityDTOTransitiveColumn = new AbstractSortableTextColumn<RoleWithSecurityDTO>(
                dto -> dto.isTransitive() ? stringMessages.yes() : stringMessages.no(), userColumnListHandler);
        // add action column
        final AccessControlledActionsColumn<RoleWithSecurityDTO, PermissionAndRoleImagesBarCell> userActionColumn = create(
                new PermissionAndRoleImagesBarCell(stringMessages), userService);
        userActionColumn.addAction(ACTION_DELETE, DELETE, selectedRole -> {
            UserDTO selectedObject = userSelectionModel.getSelectedObject();
            if (selectedObject != null) {
                StrippedUserGroupDTO qualifiedForTenant = selectedRole.getQualifiedForTenant();
                StrippedUserDTO qualifiedForUser = selectedRole.getQualifiedForUser();
                userService.getUserManagementWriteService().removeRoleFromUser(selectedObject.getName(),
                        qualifiedForUser != null ? qualifiedForUser.getName() : null,
                        selectedRole.getRoleDefinition().getId(),
                        qualifiedForTenant == null ? null : qualifiedForTenant.getName(),
                        selectedRole.isTransitive(), new MarkedAsyncCallback<SuccessInfo>(new AsyncCallback<SuccessInfo>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Window.alert(stringMessages.couldNotRemoveRoleFromUser(selectedObject.getName(),
                                        selectedRole.toString()));
                            }

                            @Override
                            public void onSuccess(SuccessInfo result) {
                                if (result.isSuccessful()) {
                                    selectedObject.removeRole(selectedRole);
                                    updateUsers.run();
                                } else {
                                    Window.alert(result.getMessage());
                                }
                            }
                        }));
            } else {
                Window.alert(stringMessages.pleaseSelect());
            }
        });
        final HasPermissions type = SecuredSecurityTypes.PERMISSION_ASSOCIATION;
        final EditOwnershipDialog.DialogConfig<RoleWithSecurityDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, permission -> refreshRoleList(), stringMessages);
        final EditACLDialog.DialogConfig<RoleWithSecurityDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, user -> user.getAccessControlList(), stringMessages);
        userActionColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, configOwnership::openOwnershipDialog);
        userActionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                permission -> configACL.openDialog(permission));
        // filter field configuration
        filterField = new LabeledAbstractFilterablePanel<RoleWithSecurityDTO>(new Label(stringMessages.filterRoles()),
                new ArrayList<RoleWithSecurityDTO>(), dataProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(RoleWithSecurityDTO t) {
                List<String> string = new ArrayList<String>();
                string.add(t.toString());
                return string;
            }

            @Override
            public AbstractCellTable<RoleWithSecurityDTO> getCellTable() {
                return table;
            }
        };
        filterField.setUpdatePermissionFilterForCheckbox(role -> userService.hasPermission(role, DefaultActions.UPDATE));
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        mainPanel.insert(filterField, 0);
        // setup table
        table.addColumnSortHandler(userColumnListHandler);
        table.addColumn(userGroupWithSecurityDTONameColumn, stringMessages.roleName());
        table.addColumn(userGroupWithSecurityDTOTransitiveColumn, stringMessages.transitive());
        table.addColumn(userActionColumn);
        table.ensureDebugId("RoleWithSecurityDTOTable");
    }

    public LabeledAbstractFilterablePanel<RoleWithSecurityDTO> getFilterField() {
        return filterField;
    }

    public void refreshRoleList() {
        UserDTO selectedObject = userSelectionModel.getSelectedObject();
        if (selectedObject != null) {
            filterField.updateAll(selectedObject.getRoles());
        }
    }
}
