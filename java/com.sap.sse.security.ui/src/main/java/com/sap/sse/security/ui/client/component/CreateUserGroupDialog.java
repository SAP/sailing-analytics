package com.sap.sse.security.ui.client.component;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.ui.client.UserManagementServiceAsync;
import com.sap.sse.security.ui.client.UserManagementWriteServiceAsync;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.CreateUserGroupDialog.UserGroupData;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class CreateUserGroupDialog extends DataEntryDialog<UserGroupData> {
    private final StringMessages stringMessages;
    private final TextBox nameBox;
    private final UserManagementServiceAsync userManagementService;
    
    private boolean resolvingUserGroupname = false;
    private boolean userGroupnameUnavailable = true;

    public static class UserGroupData {
        private final String name;
        
        protected UserGroupData(String name) {
            super();
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }
    
    public CreateUserGroupDialog(final StringMessages stringMessages, final UserService userService,
            final UserManagementWriteServiceAsync userManagementWriteService,
            final UserGroupListDataProvider userGroupListDataProvider, final Runnable runOnSuccess) {
        this(stringMessages, stringMessages.createUserGroup(), stringMessages.enterUserGroupName(),
                userManagementWriteService, null, new DialogCallback<UserGroupData>() {
                    @Override
                    public void ok(UserGroupData userGroupData) {
                        userManagementWriteService.createUserGroup(userGroupData.name, new AsyncCallback<UserGroupDTO>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Notification.notify(caught.getMessage(), NotificationType.ERROR);
                            }

                            @Override
                            public void onSuccess(UserGroupDTO result) {
                                userGroupListDataProvider.updateDisplays();
                                runOnSuccess.run();
                            }
                        });
                    }

                    @Override
                    public void cancel() {
                    }
                });
        this.ensureDebugId(this.getClass().getSimpleName());
    }
    
    private CreateUserGroupDialog(final StringMessages stringMessages, final String title, final String message,
            final UserManagementServiceAsync userManagementService, final UserGroupDTO tenant,
            final DialogCallback<UserGroupData> callback) {
        super(title, message, stringMessages.ok(), stringMessages.cancel(), null, callback);
        DataEntryDialog.Validator<UserGroupData> validator = new DataEntryDialog.Validator<UserGroupData>() {
            @Override
            public String getErrorMessage(UserGroupData valueToValidate) {
                return resolvingUserGroupname ? stringMessages.pleaseWaitUntilUsernameIsResolved()
                        : (userGroupnameUnavailable ? stringMessages.userAlreadyExists(nameBox.getText()) : null);
            }
        };
        super.setValidator(validator);
        nameBox = createTextBox("", 30);
        nameBox.setName("name");
        if (tenant != null) {
            nameBox.setText(tenant.getName());
        }
        this.stringMessages = stringMessages;
        this.userManagementService = userManagementService;
        nameBox.ensureDebugId("GroupNameInput");
        nameBox.addKeyUpHandler(e -> checkIfUserGroupnameExists());
    }

    private void checkIfUserGroupnameExists() {
        resolvingUserGroupname = true;
        validateAndUpdate();
        final String usergroupname = nameBox.getText();
        userManagementService.userGroupExists(usergroupname, new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                userGroupnameUnavailable = result != null ? result : false;
                resolvingUserGroupname = false;
                validateAndUpdate();
            }

            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(caught.getMessage(), NotificationType.ERROR);
            }
        });
    }
    
    protected UserManagementServiceAsync getUserManagementService() {
        return userManagementService;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return nameBox;
    }
    protected StringMessages getStringMessages() {
        return stringMessages;
    }

    protected TextBox getNameBox() {
        return nameBox;
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        Grid result = new Grid(1, 2);
        result.setWidget(0, 0, new Label(getStringMessages().name()));
        result.setWidget(0, 1, getNameBox());
        return result;
    }

    @Override
    protected UserGroupData getResult() {
        return new UserGroupData(nameBox.getText());
    }
}
