package com.sap.sse.security.ui.client.component.usergroup.users;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.shared.impl.SecuredSecurityTypes.USER_GROUP;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.TableWrapper;
import com.sap.sse.security.shared.dto.StrippedUserDTO;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.UserManagementWriteServiceAsync;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.UserGroupListDataProvider;
import com.sap.sse.security.ui.client.component.UserGroupListDataProvider.UserGroupListDataProviderChangeHandler;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class UserGroupDetailPanel extends Composite
        implements Handler, UserGroupListDataProviderChangeHandler {

    private final UserGroupUserResources userGroupUserResources = GWT.create(UserGroupUserResources.class);
    private final MultiSelectionModel<UserGroupDTO> userGroupSelectionModel;
    private final UserGroupUsersTableWrapper tenantUsersTable;

    public UserGroupDetailPanel(MultiSelectionModel<UserGroupDTO> refreshableSelectionModel,
            UserGroupListDataProvider tenantListDataProvider, UserService userService, StringMessages stringMessages,
            ErrorReporter errorReporter, CellTableWithCheckboxResources tableResources) {
        userGroupUserResources.css().ensureInjected();
        refreshableSelectionModel.addSelectionChangeHandler(this);
        this.userGroupSelectionModel = refreshableSelectionModel;
        tenantListDataProvider.addChangeHandler(this);
        this.tenantUsersTable = new UserGroupUsersTableWrapper(stringMessages, errorReporter, tableResources,
                userService, userGroupSelectionModel, () -> updateUserList());
        // add buttons, filter and listbox to panel
        final VerticalPanel addUserToGroupPanel = new VerticalPanel();
        final Widget buttonPanel = createButtonPanel(userService, stringMessages);
        this.userGroupSelectionModel.addSelectionChangeHandler(event -> {
            buttonPanel.setVisible(userService.hasPermission(TableWrapper.getSingleSelectedObjectOrNull(userGroupSelectionModel), UPDATE));
        });
        addUserToGroupPanel.add(buttonPanel);
        // addUserToGroupPanel.add(tenantUsersPanelCaption);
        addUserToGroupPanel.add(tenantUsersTable);
        initWidget(addUserToGroupPanel);
        this.ensureDebugId(this.getClass().getSimpleName());
    }

    /** Creates the button bar with add/remove/refresh buttons and the SuggestBox. */
    private Widget createButtonPanel(final UserService userService, final StringMessages stringMessages) {
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, USER_GROUP);
        final UserManagementWriteServiceAsync userManagementService = userService.getUserManagementWriteService();
        final TextBox userBox = new TextBox();
        userBox.addStyleName(userGroupUserResources.css().userDefinitionSuggest());
        userBox.getElement().setPropertyString("placeholder", stringMessages.enterUsername());
        userBox.ensureDebugId("UserSuggestion");
        // add suggest
        buttonPanel.insertWidgetAtPosition(userBox, 0);
        // add add button
        final Button addButton = buttonPanel.addUpdateAction(stringMessages.addUser(), () -> {
            final String selectedUsername = userBox.getValue();
            if (!getSelectedUserGroupUsernames().contains(selectedUsername)) {
                final UserGroupDTO selectedUserGroup = TableWrapper.getSingleSelectedObjectOrNull(userGroupSelectionModel);
                if (selectedUserGroup != null) {
                    userManagementService.addUserToUserGroup(selectedUserGroup.getId().toString(), selectedUsername,
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    Window.alert(stringMessages.couldNotAddUserToUserGroup(selectedUsername,
                                            selectedUserGroup.getName(), caught.getMessage()));
                                }
    
                                @Override
                                public void onSuccess(Void result) {
                                    selectedUserGroup.add(new StrippedUserDTO(selectedUsername));
                                    updateUserList();
                                    userBox.setText("");
                                }
                            });
                }
            }
        });
        addButton.ensureDebugId("AddUserButton");
        // add remove button
        // Removing a user from a group is semantically an UPDATE to the UserGroup, not a per-user DELETE.
        buttonPanel.addCountingActionWithParentPermission(stringMessages.actionRemove(),
                tenantUsersTable.getSelectionModel(),
                () -> (SecuredDTO) TableWrapper.getSingleSelectedObjectOrNull(userGroupSelectionModel), UPDATE, () -> {
            final Set<UserGroupDTO> selectedUserGroups = userGroupSelectionModel.getSelectedSet();
            if (selectedUserGroups != null && selectedUserGroups.size() == 1) {
                final UserGroupDTO selectedUserGroup = selectedUserGroups.iterator().next();
                final RefreshableMultiSelectionModel<StrippedUserDTO> usersSelectionModel = tenantUsersTable.getSelectionModel();
                for (final StrippedUserDTO user : usersSelectionModel.getSelectedElements()) {
                    final String username = user.getName();
                    userManagementService.removeUserFromUserGroup(selectedUserGroup.getId().toString(), username,
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    Window.alert(stringMessages.couldNotRemoveUserFromUserGroup(username,
                                            selectedUserGroup.getName(), caught.getMessage()));
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    StrippedUserDTO userToRemoveFromTenant = null;
                                    for (final StrippedUserDTO userInTenant : selectedUserGroup.getUsers()) {
                                        if (Util.equalsWithNull(userInTenant.getName(), username)) {
                                            userToRemoveFromTenant = userInTenant;
                                            break;
                                        }
                                    }
                                    if (userToRemoveFromTenant != null) {
                                        selectedUserGroup.remove(userToRemoveFromTenant);
                                    }
                                    updateUserList();
                                }
                            });
                }
            }
        });
        return buttonPanel;
    }

    @Override
    public void onSelectionChange(SelectionChangeEvent event) {
        updateUserList();
    }

    public void updateUserList() {
        tenantUsersTable.getFilterPanel().updateAll(userGroupSelectionModel.getSelectedSet().isEmpty() || userGroupSelectionModel.getSelectedSet().size() > 1 ?
                Collections.emptySet() : userGroupSelectionModel.getSelectedSet().iterator().next().getUsers());
    }

    private List<String> getSelectedUserGroupUsernames() {
        final List<String> result;
        final UserGroupDTO tenant = TableWrapper.getSingleSelectedObjectOrNull(userGroupSelectionModel);
        if (tenant != null) {
            result = Util.asList(tenant.getUsers()).stream().map(StrippedUserDTO::getName).collect(Collectors.toList());
        } else {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public void onChange() {
        
    }
}
