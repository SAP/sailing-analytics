package com.sap.sse.security.ui.editprofile;

import static com.sap.sse.common.HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER;
import static com.sap.sse.common.HttpRequestHeaderConstants.HEADER_FORWARD_TO_REPLICA;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SubmitButton;
import com.google.gwt.user.client.ui.TextBox;
import com.sap.sse.gwt.client.EntryPointHelper;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserManagementService;
import com.sap.sse.security.ui.client.UserManagementServiceAsync;
import com.sap.sse.security.ui.client.UserManagementWriteService;
import com.sap.sse.security.ui.client.UserManagementWriteServiceAsync;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.client.component.AbstractUserDialog.UserData;
import com.sap.sse.security.ui.client.component.EditEmailDialogWithDefaultCallback;
import com.sap.sse.security.ui.client.component.NewAccountValidator;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class EditProfileEntryPoint implements EntryPoint {
    private final StringMessages stringMessages = GWT.create(StringMessages.class);

    @Override
    public void onModuleLoad() {
        UserManagementServiceAsync userManagementService = GWT.create(UserManagementService.class);
        EntryPointHelper.registerASyncService((ServiceDefTarget) userManagementService,
                com.sap.sse.security.ui.client.RemoteServiceMappingConstants.userManagementServiceRemotePath,
                HEADER_FORWARD_TO_REPLICA);
        UserManagementWriteServiceAsync userManagementWriteService = GWT.create(UserManagementWriteService.class);
        EntryPointHelper.registerASyncService((ServiceDefTarget) userManagementWriteService,
                com.sap.sse.security.ui.client.RemoteServiceMappingConstants.userManagementServiceRemotePath,
                HEADER_FORWARD_TO_MASTER);
        final UserService userService = new UserService(userManagementService, userManagementWriteService);
        
        final NewAccountValidator validator = new NewAccountValidator(stringMessages);
        final String usernameFromURL = Window.Location.getParameter("u");
        final String emailFromURL = Window.Location.getParameter("e");
        final String passwordResetSecret = Window.Location.getParameter("s");
        RootLayoutPanel rootPanel = RootLayoutPanel.get();
        DockLayoutPanel dockPanel = new DockLayoutPanel(Unit.PX);
        rootPanel.add(dockPanel);
        FlowPanel fp = new FlowPanel();
        final Label errorLabel = new Label();
        fp.add(errorLabel);
        final Label nameLabel = new Label(stringMessages.username());
        fp.add(nameLabel);
        final TextBox nameText = new TextBox();
        nameText.setEnabled(false);
        fp.add(nameText);
        nameText.setEnabled(false);
        if (usernameFromURL != null) {
            nameText.setText(usernameFromURL);
        }
        HorizontalPanel rolesPanel = new HorizontalPanel();
        rolesPanel.add(new Label(stringMessages.roles()));
        final Label rolesLabel = new Label();
        rolesPanel.add(rolesLabel);
        rolesPanel.add(new Label(stringMessages.permissions()));
        final Label permissionsLabel = new Label();
        rolesPanel.add(permissionsLabel);
        fp.add(rolesPanel);
        final Label emailLabel = new Label(stringMessages.email());
        fp.add(emailLabel);
        HorizontalPanel emailTextBoxAndButtonPanel = new HorizontalPanel();
        final TextBox emailText = new TextBox();
        emailText.setEnabled(false);
        if (emailFromURL != null) {
            emailText.setText(emailFromURL);
        }
        emailTextBoxAndButtonPanel.add(emailText);
        final Button updateEmailButton = new Button(stringMessages.editEmail());
        emailTextBoxAndButtonPanel.add(updateEmailButton);
        fp.add(emailTextBoxAndButtonPanel);
        final TextBox currentPasswordText;
        if (passwordResetSecret == null) {
            Label currentPasswordLabel = new Label(stringMessages.currentPassword());
            fp.add(currentPasswordLabel);
            currentPasswordText = new PasswordTextBox();
            fp.add(currentPasswordText);
        } else {
            currentPasswordText = null;
        }
        updateEmailButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new EditEmailDialogWithDefaultCallback(stringMessages, userManagementWriteService, userService.getCurrentUser(),
                        new AsyncCallback<UserData>() {
                            @Override
                            public void onSuccess(UserData result) {
                                emailText.setText(result.getEmail());
                            }
                            @Override public void onFailure(Throwable caught) {}
                }).show();
            }
        });
        // when a username is provided through the URL, don't update the user display based on the signed-in user
        if (usernameFromURL == null) {
            userService.addUserStatusEventHandler(new UserStatusEventHandler() {
                @Override
                public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
                    if (user == null) {
                        nameText.setText("");
                        emailText.setText("");
                        rolesLabel.setText("");
                    } else {
                        nameText.setText(user.getName());
                        emailText.setText(user.getEmail());
                        rolesLabel.setText(user.getRoles().toString());
                        permissionsLabel.setText(user.getPermissions().toString());
                    }
                }
            });
        }
        Label pwLabel = new Label(stringMessages.password());
        fp.add(pwLabel);
        final PasswordTextBox pwText = new PasswordTextBox();
        fp.add(pwText);
        Label pw2Label = new Label(stringMessages.passwordRepeat());
        fp.add(pw2Label);
        final PasswordTextBox pw2Text = new PasswordTextBox();
        fp.add(pw2Text);
        final SubmitButton submit = new SubmitButton(stringMessages.changePassword());
        fp.add(submit);
        KeyUpHandler keyUpHandler = new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                String errorMessage = validator.validateUsernameAndPassword(nameText.getText(), pwText.getText(),
                        pw2Text.getText(), /* reallyUseLeadingOrTrailingSpacesInUsername */ true);
                if (errorMessage == null) {
                    errorLabel.setText("");
                    submit.setEnabled(true);
                } else {
                    errorLabel.setText(errorMessage);
                    submit.setEnabled(false);
                }
            }
        };
        keyUpHandler.onKeyUp(null); // enable/disable submit button and fill error label
        nameText.addKeyUpHandler(keyUpHandler);
        pwText.addKeyUpHandler(keyUpHandler);
        pw2Text.addKeyUpHandler(keyUpHandler);
        FormPanel formPanel = new FormPanel();
        formPanel.addSubmitHandler(new SubmitHandler() {
            @Override
            public void onSubmit(SubmitEvent event) {
                userManagementWriteService.updateSimpleUserPassword(nameText.getText(),
                        currentPasswordText==null?null:currentPasswordText.getText(),
                                passwordResetSecret, pwText.getText(), new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log(caught.getMessage());
                        if (caught instanceof UserManagementException) {
                            String message = ((UserManagementException) caught).getMessage();
                            if (UserManagementException.PASSWORD_DOES_NOT_MEET_REQUIREMENTS.equals(message)) {
                                Notification.notify(stringMessages.passwordDoesNotMeetRequirements(), NotificationType.ERROR);
                            } else if (UserManagementException.INVALID_CREDENTIALS.equals(message)) {
                                Notification.notify(stringMessages.invalidCredentials(), NotificationType.ERROR);
                            } else if (UserManagementException.PASSWORD_AUTHENTICATION_CURRENTLY_LOCKED_FOR_USER.equals(message)) {
                                Notification.notify(stringMessages.passwordAuthenticationCurrentlyLockedForUser(), NotificationType.ERROR);
                            } else {
                                Notification.notify(stringMessages.errorChangingPassword(caught.getMessage()), NotificationType.ERROR);
                            }
                        } else {
                            Notification.notify(stringMessages.errorChangingPassword(caught.getMessage()), NotificationType.ERROR);
                        }
                    }

                    @Override
                    public void onSuccess(Void result) {
                        Notification.notify(stringMessages.passwordSuccessfullyChanged(), NotificationType.SUCCESS);
                    }
                });
            }
        });
        final Button logout = new Button(stringMessages.signOut());
        logout.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                userService.logout();
                Window.Location.reload();
            }
        });
        fp.add(logout);
        formPanel.add(fp);
        dockPanel.add(formPanel);
    }
}
