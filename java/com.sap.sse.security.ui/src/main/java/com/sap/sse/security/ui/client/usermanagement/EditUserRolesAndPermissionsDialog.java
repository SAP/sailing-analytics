package com.sap.sse.security.ui.client.usermanagement;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.MultiSelectionModel;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.shared.dto.RolesAndPermissionsForUserDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.client.usermanagement.permissions.WildcardPermissionPanel;
import com.sap.sse.security.ui.client.usermanagement.roles.UserRoleDefinitionPanel;

/**
 * Shows the permissions and roles for a selected user which cannot be displayed in the grid since the current user
 * might not have the read permission for the selected user.<br/>
 * <br/>
 * This is to enable the current user to grant/revoke permissions and roles to a different user he does not see.
 */
public class EditUserRolesAndPermissionsDialog extends DataEntryDialog<Void> {

    private static final StringMessages stringMessages = StringMessages.INSTANCE;

    private WildcardPermissionPanel wildcardPermissionPanel;
    private UserRoleDefinitionPanel userRoleDefinitionPanel;

    public EditUserRolesAndPermissionsDialog(final String selectedUsername, final UserService userService,
            final ErrorReporter errorReporter, final CellTableWithCheckboxResources tableResources,
            final DialogCallback<Void> callback) {
        super(stringMessages.editRolesAndPermissionsForUser(selectedUsername), null, stringMessages.close(), null,
                /* validator */ null, /* animationEnabled */true, callback);
        ensureDebugId("EditUserRolesAndPermissionsDialog");
        final MultiSelectionModel<UserDTO> selectionAdapter = new MultiSelectionModel<>();
        final Runnable updater = new Runnable() {
            @Override
            public void run() {
                selectionAdapter.clear();
                userService.getUserManagementService().getRolesAndPermissionsForUser(selectedUsername,
                        new AsyncCallback<RolesAndPermissionsForUserDTO>() {
                            @Override
                            public void onSuccess(RolesAndPermissionsForUserDTO result) {
                                selectionAdapter.clear();
                                selectionAdapter.setSelected(createUserAdapter(selectedUsername, result), true);
                                wildcardPermissionPanel.updatePermissionList();
                                userRoleDefinitionPanel.updateRoles();
                                Scheduler.get().scheduleDeferred(() -> EditUserRolesAndPermissionsDialog.this.center());
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(caught.getMessage());
                            }
                        });
            }
        };
        wildcardPermissionPanel = new WildcardPermissionPanel(userService, stringMessages, errorReporter,
                tableResources, selectionAdapter, updater, oracle->createSuggestBox(oracle));
        userRoleDefinitionPanel = new UserRoleDefinitionPanel(userService, stringMessages, errorReporter,
                tableResources, selectionAdapter, updater, oracle->createSuggestBox(oracle), ()->createTextBox(""));
        updater.run();
    }

    private UserDTO createUserAdapter(final String selectedUsername, final RolesAndPermissionsForUserDTO dto) {
        return new UserDTO(selectedUsername, null, null, null, null, false, null, dto.getRoles(), null,
                dto.getPermissions(), null, null);
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return null;
    }

    @Override
    protected Void getResult() {
        return null;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel panel = new VerticalPanel();
        panel.add(userRoleDefinitionPanel);
        panel.add(wildcardPermissionPanel);
        return panel;
    }

}
