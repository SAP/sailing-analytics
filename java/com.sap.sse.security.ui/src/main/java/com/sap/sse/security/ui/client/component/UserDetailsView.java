package com.sap.sse.security.ui.client.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ImageResourceRenderer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.controls.listedit.StringListEditorComposite;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.shared.DefaultRoles;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.ui.client.Resources;
import com.sap.sse.security.ui.client.StringMessages;
import com.sap.sse.security.ui.client.UserChangeEventHandler;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AbstractUserDialog.UserData;
import com.sap.sse.security.ui.client.resources.IconResources;
import com.sap.sse.security.ui.oauth.client.SocialUserDTO;
import com.sap.sse.security.ui.shared.AccountDTO;
import com.sap.sse.security.ui.shared.SuccessInfo;
import com.sap.sse.security.ui.shared.UserDTO;
import com.sap.sse.security.ui.shared.UserManagementServiceAsync;
import com.sap.sse.security.ui.shared.UsernamePasswordAccountDTO;

public class UserDetailsView extends FlowPanel {
    private List<UserChangeEventHandler> handlers = new ArrayList<>();

    private final StringMessages stringMessages;
    
    private final Label usernameLabel;
    private final Label emailLabel;
    private final StringListEditorComposite rolesEditor;
    private final VerticalPanel accountPanels;
    private UserDTO user;

    public UserDetailsView(final UserService userService, UserDTO user, final StringMessages stringMessages) {
        final UserManagementServiceAsync userManagementService = userService.getUserManagementService();
        this.stringMessages = stringMessages;
        this.user = user;
        addStyleName("userDetailsView");
        List<String> defaultRoleNames = new ArrayList<>();
        for (DefaultRoles defaultRole : DefaultRoles.values()) {
            defaultRoleNames.add(defaultRole.getRolename());
        }
        rolesEditor = new StringListEditorComposite(user==null?Collections.<String>emptySet():user.getRoles(), stringMessages, IconResources.INSTANCE.remove(), defaultRoleNames,
                stringMessages.enterRoleName());
        rolesEditor.addValueChangeHandler(new ValueChangeHandler<Iterable<String>>() {
            @Override
            public void onValueChange(ValueChangeEvent<Iterable<String>> event) {
                Iterable<String> newRoleList = event.getValue();
                userManagementService.setRolesForUser(UserDetailsView.this.user.getName(), newRoleList, new MarkedAsyncCallback<SuccessInfo>(
                        new AsyncCallback<SuccessInfo>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Window.alert(stringMessages.errorUpdatingRoles(UserDetailsView.this.user.getName(), caught.getMessage()));
                            }

                            @Override
                            public void onSuccess(SuccessInfo result) {
                                if (!result.isSuccessful()) {
                                    Window.alert(stringMessages.errorUpdatingRoles(UserDetailsView.this.user.getName(), result.getMessage()));
                                } else {
                                    userService.updateUser(/* notify other instances */ true);
                                }
                            }
                        }));
            }
        });
        usernameLabel = new Label();
        emailLabel = new Label();
        final Button changeEmail = new Button(stringMessages.edit());
        changeEmail.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final UserDTO clickedUser = UserDetailsView.this.user;
                new EditEmailDialogWithDefaultCallback(stringMessages, userManagementService, clickedUser,
                        new AsyncCallback<UserData>() {
                            @Override
                            public void onSuccess(UserData result) {
                                emailLabel.setText(result.getEmail());
                                for (UserChangeEventHandler handler : handlers) {
                                    handler.onUserChange(clickedUser);
                                }
                            }
                            @Override public void onFailure(Throwable caught) {}
                        }).show();
            }
        });
        Label title = new Label(stringMessages.userDetails());
        title.getElement().getStyle().setFontSize(25, Unit.PX);
        this.add(title);
        DecoratorPanel decoratorPanel = new DecoratorPanel();
        FlowPanel fp = new FlowPanel();
        fp.setWidth("100%");
        ImageResourceRenderer renderer = new ImageResourceRenderer();
        final ImageResource userImageResource = Resources.INSTANCE.userSmall();
        fp.add(new HTML(renderer.render(userImageResource)));
        HorizontalPanel namePanel = new HorizontalPanel();
        fp.add(namePanel);
        namePanel.add(new Label(stringMessages.name()+": "));
        namePanel.add(usernameLabel);
        HorizontalPanel emailPanel = new HorizontalPanel();
        fp.add(emailPanel);
        emailPanel.add(new Label(stringMessages.email() + ": "));
        emailPanel.add(emailLabel);
        emailPanel.add(changeEmail);
        accountPanels = new VerticalPanel();
        fp.add(accountPanels);
        decoratorPanel.setWidget(fp);
        this.add(decoratorPanel);
        this.add(rolesEditor);
        updateUser(user, userManagementService);
    }

    public void updateUser(final UserDTO user, final UserManagementServiceAsync userManagementService) {
        this.user = user;
        accountPanels.clear();
        if (user == null) {
            usernameLabel.setText("");
            emailLabel.setText("");
        } else {
            usernameLabel.setText(user.getName());
            emailLabel.setText(user.getEmail());
            for (AccountDTO a : user.getAccounts()) {
                DecoratorPanel accountPanelDecorator = new DecoratorPanel();
                FlowPanel accountPanelContent = new FlowPanel();
                accountPanelDecorator.setWidget(accountPanelContent);
                accountPanelContent.add(new Label(stringMessages.account(a.getAccountType())));
                if (a instanceof UsernamePasswordAccountDTO) {
                    final Button changePasswordButton = new Button(stringMessages.changePassword());
                    accountPanelContent.add(changePasswordButton);
                    changePasswordButton.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            new ChangePasswordDialog(stringMessages, userManagementService, UserDetailsView.this.user, new DataEntryDialog.DialogCallback<UserData>() {
                                @Override
                                public void ok(UserData userData) {
                                    userManagementService.updateSimpleUserPassword(UserDetailsView.this.user.getName(), /* admin doesn't need to provide old password */ null, userData.getPassword(), new MarkedAsyncCallback<Void>(
                                            new AsyncCallback<Void>() {
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
                                            }));
                                }
                                @Override public void cancel() { }
                            }).show();
                        }
                    });
                    // only offer password reset if the user's e-mail address has successfully been validated
                    if (UserDetailsView.this.user.isEmailValidated()) {
                        final Button resetPasswordButton = new Button(stringMessages.resetPassword());
                        accountPanelContent.add(resetPasswordButton);
                        resetPasswordButton.addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                userManagementService.resetPassword(UserDetailsView.this.user.getName(),
                                        new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                Window.alert(stringMessages.errorResettingPassword(
                                                        UserDetailsView.this.user.getName(), caught.getMessage()));
                                            }

                                            @Override
                                            public void onSuccess(Void result) {
                                                Window.alert(stringMessages
                                                        .successfullyResetPassword(UserDetailsView.this.user.getName()));
                                            }
                                        }));
                            }
                        });
                    }
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
            rolesEditor.setValue(user.getRoles());
        }
    }

    public void addUserChangeEventHandler(UserChangeEventHandler handler) {
        this.handlers.add(handler);
    }

    public void removeUserChangeEventHandler(UserChangeEventHandler handler) {
        this.handlers.remove(handler);
    }
}
