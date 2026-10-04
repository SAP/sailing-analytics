package com.sap.sse.security.ui.client.usermanagement.permissions;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;

import java.util.ArrayList;
import java.util.Collection;
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
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.dto.WildcardPermissionWithSecurityDTO;
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
public class WildcardPermissionWithSecurityDTOTableWrapper extends
        TableWrapper<WildcardPermissionWithSecurityDTO, RefreshableSingleSelectionModel<WildcardPermissionWithSecurityDTO>, StringMessages, CellTableWithCheckboxResources> {

    private final LabeledAbstractFilterablePanel<WildcardPermissionWithSecurityDTO> filterField;
    private final SingleSelectionModel<UserDTO> userSelectionModel;

    public WildcardPermissionWithSecurityDTOTableWrapper(final UserService userService,
            final StringMessages stringMessages, final ErrorReporter errorReporter, boolean enablePager,
            final CellTableWithCheckboxResources tableResources, final SingleSelectionModel<UserDTO> userSelectionModel,
            final Runnable updateUsers) {
        super(stringMessages, errorReporter, false, enablePager,
                new EntityIdentityComparator<WildcardPermissionWithSecurityDTO>() {
                    @Override
                    public boolean representSameEntity(WildcardPermissionWithSecurityDTO dto1,
                            WildcardPermissionWithSecurityDTO dto2) {
                        return dto1.getIdentifier().equals(dto2.getIdentifier());
                    }

                    @Override
                    public int hashCode(WildcardPermissionWithSecurityDTO t) {
                        return t.getIdentifier().hashCode();
                    }
                }, tableResources);
        this.userSelectionModel = userSelectionModel;
        this.userSelectionModel.addSelectionChangeHandler(e -> refreshPermissionList());
        final ListHandler<WildcardPermissionWithSecurityDTO> userColumnListHandler = getColumnSortHandler();
        // users table
        final TextColumn<WildcardPermissionWithSecurityDTO> userGroupWithSecurityDTONameColumn = new AbstractSortableTextColumn<WildcardPermissionWithSecurityDTO>(
                dto -> dto.toString(), userColumnListHandler);
        final AccessControlledActionsColumn<WildcardPermissionWithSecurityDTO, PermissionAndRoleImagesBarCell> userActionColumn = create(
                new PermissionAndRoleImagesBarCell(stringMessages), userService);
        userActionColumn.addAction(ACTION_DELETE, DELETE, selectedPermission -> {
            UserDTO selectedObject = userSelectionModel.getSelectedObject();
            if (selectedObject != null) {
                userService.getUserManagementWriteService().removePermissionFromUser(selectedObject.getName(),
                        selectedPermission, new MarkedAsyncCallback<SuccessInfo>(new AsyncCallback<SuccessInfo>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Window.alert(stringMessages.couldNotRemovePermissionFromUser(selectedObject.getName(),
                                        selectedPermission.toString()));
                            }

                            @Override
                            public void onSuccess(SuccessInfo result) {
                                if (result.isSuccessful()) {
                                    selectedObject.removePermission(selectedPermission);
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
        final EditOwnershipDialog.DialogConfig<WildcardPermissionWithSecurityDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, permission -> refreshPermissionList(),
                        stringMessages);
        final EditACLDialog.DialogConfig<WildcardPermissionWithSecurityDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, user -> user.getAccessControlList(), stringMessages);
        userActionColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, configOwnership::openOwnershipDialog);
        userActionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                permission -> configACL.openDialog(permission));
        // filter field configuration
        filterField = new LabeledAbstractFilterablePanel<WildcardPermissionWithSecurityDTO>(
                new Label(stringMessages.filterPermission()), new ArrayList<WildcardPermissionWithSecurityDTO>(),
                dataProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(WildcardPermissionWithSecurityDTO t) {
                List<String> string = new ArrayList<String>();
                string.add(t.toString());
                return string;
            }

            @Override
            public AbstractCellTable<WildcardPermissionWithSecurityDTO> getCellTable() {
                return table;
            }
        };
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        filterField
                .setUpdatePermissionFilterForCheckbox(permission -> userService.hasPermission(permission, DefaultActions.UPDATE));
        mainPanel.insert(filterField, 0);
        // setup table
        table.ensureDebugId("WildcardPermissionWithSecurityDTOTable");
        table.addColumnSortHandler(userColumnListHandler);
        table.addColumn(userGroupWithSecurityDTONameColumn, stringMessages.permission());
        table.addColumn(userActionColumn, stringMessages.actions());
    }

    public LabeledAbstractFilterablePanel<WildcardPermissionWithSecurityDTO> getFilterField() {
        return filterField;
    }

    public void refreshPermissionList() {
        UserDTO selectedObject = userSelectionModel.getSelectedObject();
        if (selectedObject != null) {
            Collection<WildcardPermissionWithSecurityDTO> permissions = new ArrayList<>();
            for (WildcardPermission permission : selectedObject.getPermissions()) {
                permissions.add((WildcardPermissionWithSecurityDTO) permission);
            }
            filterField.updateAll(permissions);
        }
    }
}
