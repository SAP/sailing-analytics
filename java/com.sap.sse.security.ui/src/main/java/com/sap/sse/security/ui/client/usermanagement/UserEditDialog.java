package com.sap.sse.security.ui.client.usermanagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.AccountDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.dto.WildcardPermissionWithSecurityDTO;
import com.sap.sse.security.ui.client.UserManagementWriteServiceAsync;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AbstractUserDialog.UserData;
import com.sap.sse.security.ui.client.component.ChangePasswordDialog;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.oauth.client.SocialUserDTO;
import com.sap.sse.security.ui.shared.UsernamePasswordAccountDTO;

/**
 * Edits a {@link UserDTO} object. {@link Role}s and {@link WildcardPermission}s are set via the associated detail
 * panels.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class UserEditDialog extends DataEntryDialog<UserDTO> {
    private final UserDTO userToEdit;
    private final TextBox username;
    private final TextBox fullName;
    private final TextBox company;
    private final TextBox email;
    private final VerticalPanel accountPanels;

    private final UserService userService;
    private static final StringMessages stringMessages = StringMessages.INSTANCE;
    
    /**
     * The class creates the UI-dialog to type in the Data about a competitor.
     * 
     * @param userToEdit
     *            The 'userToEdit' parameter contains the user which should be changed or initialized.
     */
    public UserEditDialog(final UserDTO userToEdit, final DialogCallback<UserDTO> callback,
            final UserService userService, final ErrorReporter errorReporter) {
        super(stringMessages.editUser(), null, stringMessages.ok(), stringMessages
                .cancel(), /* validator */ null, /* animationEnabled */true, callback);
        this.ensureDebugId("UserEditDialog");
        this.userService = userService;
        this.userToEdit = userToEdit;
        this.username = createTextBox(userToEdit.getName(), 70);
        username.ensureDebugId("UsernameTextBox");
        username.setEnabled(false); // the username is key and cannot be changed
        this.email = createTextBox(userToEdit.getEmail(), 70);
        this.fullName = createTextBox(userToEdit.getFullName(), 70);
        this.company = createTextBox(userToEdit.getCompany(), 70);
        this.accountPanels = new VerticalPanel();
        for (AccountDTO a : userToEdit.getAccounts()) {
            DecoratorPanel accountPanelDecorator = new DecoratorPanel();
            FlowPanel accountPanelContent = new FlowPanel();
            accountPanelDecorator.setWidget(accountPanelContent);
            accountPanelContent.add(new Label(stringMessages.account(a.getAccountType())));
            if (a instanceof UsernamePasswordAccountDTO) {
                final Button changePasswordButton = new Button(stringMessages.changePassword());
                changePasswordButton.ensureDebugId("ChangePasswordButton");
                accountPanelContent.add(changePasswordButton);
                changePasswordButton.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        final ChangePasswordDialog changePasswordDialog = new ChangePasswordDialog(stringMessages, getUserManagementWriteService(), userToEdit, new DataEntryDialog.DialogCallback<UserData>() {
                            @Override
                            public void ok(UserData userData) {
                                getUserManagementWriteService().updateSimpleUserPassword(userToEdit.getName(), /* admin doesn't need to provide old password */ null,
                                        /* resetPasswordSecret */ null, userData.getPassword(), new MarkedAsyncCallback<Void>(
                                        new AsyncCallback<Void>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                GWT.log(caught.getMessage());
                                                if (caught instanceof UserManagementException) {
                                                    String message = ((UserManagementException) caught).getMessage();
                                                    if (UserManagementException.PASSWORD_DOES_NOT_MEET_REQUIREMENTS.equals(message)) {
                                                        errorReporter.reportError(stringMessages.passwordDoesNotMeetRequirements());
                                                    } else if (UserManagementException.INVALID_CREDENTIALS.equals(message)) {
                                                        errorReporter.reportError(stringMessages.invalidCredentials());
                                                    } else {
                                                        errorReporter.reportError(stringMessages.errorChangingPassword(caught.getMessage()));
                                                    }
                                                } else {
                                                    errorReporter.reportError(stringMessages.errorChangingPassword(caught.getMessage()));
                                                }
                                            }

                                            @Override
                                            public void onSuccess(Void result) {
                                                Notification.notify(stringMessages.passwordSuccessfullyChanged(), NotificationType.SUCCESS);
                                            }
                                        }));
                            }
                            @Override public void cancel() { }
                        });
                        changePasswordDialog.ensureDebugId("ChangePasswordDialog");
                        changePasswordDialog.show();
                    }
                });
            } else if (a instanceof SocialUserDTO) {
                SocialUserDTO sua = (SocialUserDTO) a;
                FlexTable table = new FlexTable();
                int i = 0;
                for (Entry<String, String> e : sua.getProperties().entrySet()) {
                    if (e.getValue() != null) {
                        table.setText(i, 0, e.getKey().toLowerCase().replace('_', ' '));
                        table.setText(i, 1, e.getValue());
                        i++;
                    }
                }
                accountPanelContent.add(table);
            }
            accountPanels.add(accountPanelDecorator);
        }
    }

    private UserManagementWriteServiceAsync getUserManagementWriteService() {
        return userService.getUserManagementWriteService();
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return email;
    }

    @Override
    protected UserDTO getResult() {
        final Collection<WildcardPermissionWithSecurityDTO> permissions = new ArrayList<>();
        for (WildcardPermission permission : userToEdit.getPermissions()) {
            if (permission instanceof WildcardPermissionWithSecurityDTO)
                permissions.add((WildcardPermissionWithSecurityDTO) permission);
        }
        final UserDTO user = new UserDTO(userToEdit.getName(), email.getText(), fullName.getText(), company.getText(),
                userToEdit.getLocale(), userToEdit.isEmailValidated(), userToEdit.getAccounts(),
                userToEdit.getRoles(), userToEdit.getDefaultTenant(), permissions,
                userToEdit.getUserGroups(), userToEdit.getLockedUntil());
        return user;
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid result = new Grid(5, 2);
        result.setWidget(0, 0, new Label(stringMessages.username()));
        result.setWidget(0, 1, username);
        result.setWidget(1, 0, new Label(stringMessages.name()));
        result.setWidget(1, 1, fullName);
        result.setWidget(2, 0, new Label(stringMessages.email()));
        result.setWidget(2, 1, email);
        result.setWidget(3, 0, new Label(stringMessages.company()));
        result.setWidget(3, 1, company);
        result.setWidget(4, 0, accountPanels);
        return result;
    }

}
