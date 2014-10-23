package com.sap.sse.security.ui.editprofile;

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
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.ui.client.RemoteServiceMappingConstants;
import com.sap.sse.security.ui.client.StringMessages;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.client.component.AbstractUserDialog.UserData;
import com.sap.sse.security.ui.client.component.EditEmailDialogWithDefaultCallback;
import com.sap.sse.security.ui.client.component.NewAccountValidator;
import com.sap.sse.security.ui.shared.UserDTO;
import com.sap.sse.security.ui.shared.UserManagementService;
import com.sap.sse.security.ui.shared.UserManagementServiceAsync;

public class EditProfileEntryPoint implements EntryPoint {
    private final UserManagementServiceAsync userManagementService = GWT.create(UserManagementService.class);
    private final StringMessages stringMessages = GWT.create(StringMessages.class);

    @Override
    public void onModuleLoad() {
        EntryPointHelper.registerASyncService((ServiceDefTarget) userManagementService,
                RemoteServiceMappingConstants.WEB_CONTEXT_PATH,
                RemoteServiceMappingConstants.userManagementServiceRemotePath);
        final UserService userService = new UserService(userManagementService);
        final NewAccountValidator validator = new NewAccountValidator(stringMessages);
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
        fp.add(nameText);
        HorizontalPanel rolesPanel = new HorizontalPanel();
        rolesPanel.add(new Label(stringMessages.roles()));
        final Label rolesLabel = new Label();
        rolesPanel.add(rolesLabel);
        fp.add(rolesPanel);
        final Label emailLabel = new Label(stringMessages.email());
        fp.add(emailLabel);
        HorizontalPanel emailTextBoxAndButtonPanel = new HorizontalPanel();
        final TextBox emailText = new TextBox();
        emailText.setEnabled(false);
        emailTextBoxAndButtonPanel.add(emailText);
        final Button updateEmailButton = new Button(stringMessages.editEmail());
        emailTextBoxAndButtonPanel.add(updateEmailButton);
        fp.add(emailTextBoxAndButtonPanel);
        Label currentPasswordLabel = new Label(stringMessages.currentPassword());
        fp.add(currentPasswordLabel);
        HorizontalPanel currentPasswordAndResetButtonPanel = new HorizontalPanel();
        final TextBox currentPasswordText = new PasswordTextBox();
        currentPasswordAndResetButtonPanel.add(currentPasswordText);
        final Button passwordReset = new Button(stringMessages.resetPassword());
        passwordReset.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                userManagementService.resetPassword(nameText.getText(), new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        if (caught instanceof UserManagementException) {
                            if (UserManagementException.CANNOT_RESET_PASSWORD_WITHOUT_VALIDATED_EMAIL.equals(caught.getMessage())) {
                                Window.alert(stringMessages.cannotResetPasswordWithoutValidatedEmail(nameText.getText()));
                            } else {
                                Window.alert(stringMessages.errorDuringPasswordReset(caught.getMessage()));
                            }
                        } else {
                            Window.alert(stringMessages.errorDuringPasswordReset(caught.getMessage()));
                        }
                    }

                    @Override
                    public void onSuccess(Void result) {
                        Window.alert(stringMessages.newPasswordSent(nameText.getText()));
                    }
                }));
            }
        });
        currentPasswordAndResetButtonPanel.add(passwordReset);
        fp.add(currentPasswordAndResetButtonPanel);
        updateEmailButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new EditEmailDialogWithDefaultCallback(stringMessages, userManagementService, userService.getCurrentUser(),
                        new AsyncCallback<UserData>() {
                            @Override
                            public void onSuccess(UserData result) {
                                emailText.setText(result.getEmail());
                                passwordReset.setEnabled(false); // an updated e-mail first needs to be re-validated
                            }
                            @Override public void onFailure(Throwable caught) {}
                }).show();
            }
        });
        userService.addUserStatusEventHandler(new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO user) {
                if (user == null) {
                    nameText.setText("");
                    emailText.setText("");
                    rolesLabel.setText("");
                    passwordReset.setEnabled(false);
                } else {
                    nameText.setText(user.getName());
                    emailText.setText(user.getEmail());
                    rolesLabel.setText(user.getRoles().toString());
                    passwordReset.setEnabled(user.isEmailValidated());
                }
            }
        });
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
                String errorMessage = validator.validateUsernameAndPassword(nameText.getText(), pwText.getText(), pw2Text.getText());
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
                userManagementService.updateSimpleUserPassword(nameText.getText(), currentPasswordText.getText(), pwText.getText(), new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log(caught.getMessage());
                        if (caught instanceof UserManagementException) {
                            String message = ((UserManagementException) caught).getMessage();
                            if (UserManagementException.PASSWORD_DOES_NOT_MEET_REQUIREMENTS.equals(message)) {
                                Window.alert(stringMessages.passwordDoesNotMeetRequirements());
                            } else if (UserManagementException.INVALID_CREDENTIALS.equals(message)) {
                                Window.alert(stringMessages.invalidCredentials());
                            } else {
                                Window.alert(stringMessages.errorChangingPassword(caught.getMessage()));
                            }
                        } else {
                            Window.alert(stringMessages.errorChangingPassword(caught.getMessage()));
                        }
                    }

                    @Override
                    public void onSuccess(Void result) {
                        Window.alert(stringMessages.passwordSuccessfullyChanged());
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
