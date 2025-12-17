package com.sap.sse.security.ui.client.usermanagement;

import static com.sap.sse.security.shared.impl.SecuredSecurityTypes.USER;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.Callback;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.UserActions;
import com.sap.sse.security.ui.client.UserManagementWriteServiceAsync;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.CreateUserDialog;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.client.usermanagement.permissions.WildcardPermissionPanel;
import com.sap.sse.security.ui.client.usermanagement.roles.UserRoleDefinitionPanel;
import com.sap.sse.security.ui.shared.SuccessInfo;

public class UserManagementPanel<TR extends CellTableWithCheckboxResources> extends DockPanel {

    private final List<UserCreatedEventHandler> userCreatedHandlers = new ArrayList<>();
    private final List<UserDeletedEventHandler> userDeletedHandlers = new ArrayList<>();

    protected final UserTableWrapper<RefreshableMultiSelectionModel<UserDTO>, TR> userList;
    private final RefreshableMultiSelectionModel<UserDTO> userSelectionModel;
    private final TextBox userNameTextbox;
    private final UserRoleDefinitionPanel userRoleDefinitionPanel;

    public UserManagementPanel(final UserService userService, final StringMessages stringMessages,
            ErrorReporter errorReporter, TR tableResources) {
        final UserManagementWriteServiceAsync userManagementWriteService = userService.getUserManagementWriteService();
        final VerticalPanel west = new VerticalPanel();
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, USER);
        west.add(buttonPanel);
        userList = new UserTableWrapper<>(userService, stringMessages, errorReporter, /* multiSelection */ true,
                /* enablePager */ true, tableResources);
        buttonPanel.addUnsecuredAction(stringMessages.refresh(),
                () -> userList.refreshUserList((Callback<Iterable<UserDTO>, Throwable>) null, false));
        Button createUserButton = buttonPanel.addCreateActionWithoutServerCreateObjectPermissionCheck(stringMessages.createUser(),
                () -> new CreateUserDialog(stringMessages, userManagementWriteService, userCreatedHandlers, userService)
                        .show());
        createUserButton.ensureDebugId("CreateUserButton");
        userNameTextbox = buttonPanel.addUnsecuredTextBox(stringMessages.username());
        userNameTextbox.ensureDebugId("UserNameTextbox");
        final Button editRolesAndPermissionsForUserButton = buttonPanel.addUnsecuredAction(
                stringMessages.editRolesAndPermissionsForUser(""),
                () -> showRolesAndPermissionsEditDialog(userService, tableResources, errorReporter));
        editRolesAndPermissionsForUserButton.ensureDebugId("EditRolesAndPermissionsForUserButton");
        userNameTextbox.addKeyUpHandler(
                e -> editRolesAndPermissionsForUserButton.setEnabled(!userNameTextbox.getText().isEmpty()));
        editRolesAndPermissionsForUserButton.setEnabled(false);
        userSelectionModel = userList.getSelectionModel();
        final Button removeButton = buttonPanel.addRemoveAction(stringMessages.remove(), userSelectionModel, false, () -> {
            assert userSelectionModel.getSelectedSet().size() > 0;
            final Set<UserDTO> usersToDelete = new HashSet<>();
            final Set<String> usernamesToDelete = new HashSet<>();
            for (UserDTO userToDelete : userSelectionModel.getSelectedSet()) {
                usersToDelete.add(userToDelete);
                usernamesToDelete.add(userToDelete.getName());
            }
            if (Window.confirm(usernamesToDelete.size() == 1
                    ? stringMessages.doYouReallyWantToDeleteUser(usernamesToDelete.iterator().next())
                    : stringMessages.doYouReallyWantToDeleteNUsers(usernamesToDelete.size()))) {
                userManagementWriteService.deleteUsers(usernamesToDelete, new AsyncCallback<Set<SuccessInfo>>() {
                    @Override
                    public void onSuccess(Set<SuccessInfo> result) {
                        for (UserDTO userToDelete : usersToDelete) {
                            for (UserDeletedEventHandler userDeletedHandler : userDeletedHandlers) {
                                userDeletedHandler.onUserDeleted(userToDelete);
                            }
                        }
                        for (SuccessInfo successInfo : result) {
                            Notification.notify(successInfo.getMessage(), NotificationType.SUCCESS);
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorDeletingUser(usernamesToDelete.iterator().next(),
                                caught.getMessage()));
                    }
                });
            }
        });
        removeButton.ensureDebugId("DeleteUserButton");
        final boolean hasManageLockPermissionOnAnyUser = userService.hasCurrentUserAnyPermission(USER.getPermission(UserActions.MANAGE_LOCK), null);
        if (hasManageLockPermissionOnAnyUser) {
            final Button unlockButton = buttonPanel.addCountingAction("Unlock", userSelectionModel, () -> true,
                    () -> onCountingUnlockButtonClick(stringMessages, userManagementWriteService, errorReporter));
            unlockButton.ensureDebugId("UnlockUserButton");
        }
        ScrollPanel scrollPanel = new ScrollPanel(userList.asWidget());
        LabeledAbstractFilterablePanel<UserDTO> filterBox = userList.getFilterField();
        filterBox.getElement().setPropertyString("placeholder", stringMessages.filterUsers());
        west.add(filterBox);
        west.add(scrollPanel);
        add(west, DockPanel.WEST);
        addUserCreatedEventHandler(new UserCreatedEventHandler() {
            @Override
            public void onUserCreated(UserDTO user) {
                if (user != null) {
                    userList.getFilterField().add(user);
                    userSelectionModel.clear();
                    userSelectionModel.setSelected(user, true);
                }
            }
        });
        addUserDeletedEventHandler(new UserDeletedEventHandler() {
            @Override
            public void onUserDeleted(UserDTO user) {
                userList.getFilterField().remove(user);
            }
        });
        updateUsers();
        final HorizontalPanel detailsPanel = new HorizontalPanel();
        // add details panel for user roles
        userRoleDefinitionPanel = new UserRoleDefinitionPanel(userService, stringMessages, errorReporter,
                tableResources, userList.getSelectionModel(), () -> updateUsers(), SuggestBox::new, TextBox::new);
        detailsPanel.add(userRoleDefinitionPanel);
        // add details panel for user permissions
        final WildcardPermissionPanel userPermissionPanel = new WildcardPermissionPanel(userService, stringMessages,
                errorReporter, tableResources, userList.getSelectionModel(), () -> updateUsers(), SuggestBox::new);
        detailsPanel.add(userPermissionPanel);
        west.add(detailsPanel);
    }

    private void onCountingUnlockButtonClick(final StringMessages stringMessages,
            final UserManagementWriteServiceAsync userManagementWriteService, final ErrorReporter errorReporter) {
        final Set<UserDTO> selectedUsers = userSelectionModel.getSelectedSet();
        final Set<String> selectedUsernames = new HashSet<String>();
        selectedUsers.forEach((u) -> selectedUsernames.add(u.getName()));
        final boolean didConfirm = Window.confirm(stringMessages.doYouReallyWantToUnlockNUsers(selectedUsers.size()));
        if (didConfirm) {
            // run api, collect results
            final Set<String> successUserNames = new HashSet<String>();
            final Set<String> failureUserNames = new HashSet<String>();
            userManagementWriteService.unlockUsers(selectedUsernames, new AsyncCallback<Set<SuccessInfo>>() {
                @Override
                public void onSuccess(Set<SuccessInfo> result) {
                    for (SuccessInfo si : result) {
                        final String username = si.getExtra();
                        if (si.isSuccessful()) {
                            successUserNames.add(username);
                        } else {
                            failureUserNames.add(username);
                        }
                    }
                    // update locked until entries for successful unlocks
                    final LabeledAbstractFilterablePanel<UserDTO> filterField = userList.getFilterField();
                    final List<UserDTO> allUsersWithUpdatedEntries = new ArrayList<UserDTO>();
                    for (UserDTO user : filterField.getAll()) {
                        final boolean didSucceed = successUserNames.contains(user.getName());
                        if (didSucceed) {
                            allUsersWithUpdatedEntries.add(user.copyWithTimePoint(null));
                        } else {
                            allUsersWithUpdatedEntries.add(user);
                        }
                    }
                    filterField.updateAll(allUsersWithUpdatedEntries);
                    // create alert dialog
                    String text = "";
                    if (successUserNames.size() != 0) {
                        text += stringMessages.unlockedSuccessfully() + ":\n";
                        for (String name : successUserNames) {
                            text += " • " + name + "\n";
                        }
                        text += "\n";
                    }
                    if (failureUserNames.size() != 0) {
                        text += stringMessages.failedToUnlock() + ":\n";
                        for (String name : failureUserNames) {
                            text += " • " + name + "\n";
                        }
                    }
                    if(!text.equals("")) {
                        Window.alert(text);
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError(stringMessages.failedToUnlock() + ": " + caught.getMessage());
                }
            });
        }
    }
    
    /** shows the edit dialog if the user exists */
    private void showRolesAndPermissionsEditDialog(UserService userService,
            final CellTableWithCheckboxResources tableResources, final ErrorReporter errorReporter) {
        userService.getUserManagementService().userExists(userNameTextbox.getText(), new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    new EditUserRolesAndPermissionsDialog(userNameTextbox.getText(), userService, errorReporter,
                            tableResources, new DialogCallback<Void>() {
                                @Override
                                public void ok(Void editedObject) {
                                    updateUsers();
                                }

                                @Override
                                public void cancel() {
                                    updateUsers();
                                }
                            }).show();
                }
                else {
                    Notification.notify(StringMessages.INSTANCE.userNotFound(userNameTextbox.getText()),
                            NotificationType.ERROR);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
            }
        });
    }

    public void refreshSuggests() {
        userRoleDefinitionPanel.refreshSuggest();
    }

    public void updateUsers() {
        userList.refreshUserList((Callback<Iterable<UserDTO>, Throwable>) null);
    }

    public void addUserCreatedEventHandler(UserCreatedEventHandler handler){
        this.userCreatedHandlers.add(handler);
    }

    public void removeUserCreatedEventHandler(UserCreatedEventHandler handler){
        this.userCreatedHandlers.remove(handler);
    }

    public static interface UserCreatedEventHandler extends EventHandler {
        void onUserCreated(UserDTO user);
    }

    public void addUserDeletedEventHandler(UserDeletedEventHandler handler){
        this.userDeletedHandlers.add(handler);
    }

    public void removeUserDeletedEventHandler(UserDeletedEventHandler handler){
        this.userDeletedHandlers.remove(handler);
    }

    public static interface UserDeletedEventHandler extends EventHandler {
        void onUserDeleted(UserDTO user);
    }
}
