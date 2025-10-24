package com.sap.sse.security.ui.registration;

import static com.sap.sse.security.shared.UserManagementException.USER_ALREADY_EXISTS;

import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.PasswordTextBoxWithWatermark;
import com.sap.sse.gwt.client.controls.TextBoxWithWatermark;
import com.sap.sse.gwt.client.dialog.DialogUtils;
import com.sap.sse.gwt.shared.ClientConfiguration;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.EntryPointLinkFactory;
import com.sap.sse.security.ui.client.UserManagementWriteServiceAsync;
import com.sap.sse.security.ui.client.component.NewAccountValidator;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class RegisterView extends Composite {
    private static RegisterViewUiBinder uiBinder = GWT.create(RegisterViewUiBinder.class);

    interface RegisterViewUiBinder extends UiBinder<Widget, RegisterView> {
    }

    private final UserManagementWriteServiceAsync userManagementWriteService;
    private final StringMessages stringMessages;
    
    @UiField Label appNameLabel;
    @UiField Label errorMessageLabel;
    @UiField Button signUpButton;
    @UiField TextBoxWithWatermark usernameTextBox;
    @UiField TextBoxWithWatermark emailTextBox;
    @UiField PasswordTextBoxWithWatermark passwordTextBox;
    @UiField PasswordTextBoxWithWatermark password2TextBox;
    @UiField ImageElement logoImage;

    public RegisterView(UserManagementWriteServiceAsync userManagementService, StringMessages stringMessages, String appName) {
        this.userManagementWriteService = userManagementService;
        this.stringMessages = stringMessages;

        RegisterViewResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        
        appNameLabel.setText(appName);
        final NewAccountValidator validator = new NewAccountValidator(stringMessages);
        KeyUpHandler keyUpHandler = new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                String errorMessage = validator.validateUsernameAndPassword(usernameTextBox.getText(),
                        passwordTextBox.getText(), password2TextBox.getText(),
                        /* reallyUseLeadingOrTrailingSpacesInUsername */ true);
                if (errorMessage == null) {
                    errorMessageLabel.setText("");
                    signUpButton.setEnabled(true);
                    signUpButton.removeStyleName(RegisterViewResources.INSTANCE.css().buttoninactive());
                } else {
                    errorMessageLabel.setText(errorMessage);
                    signUpButton.setEnabled(false);
                    signUpButton.addStyleName(RegisterViewResources.INSTANCE.css().buttoninactive());
                }
            }
        };
        keyUpHandler.onKeyUp(null); // enable/disable submit button and fill error label
        usernameTextBox.addKeyUpHandler(keyUpHandler);
        passwordTextBox.addKeyUpHandler(keyUpHandler);
        password2TextBox.addKeyUpHandler(keyUpHandler);
        
        DialogUtils.linkEnterToButton(signUpButton, usernameTextBox, emailTextBox, passwordTextBox, password2TextBox);
        if (!ClientConfiguration.getInstance().isBrandingActive()) {
            logoImage.getStyle().setDisplay(Display.NONE);
        }
    }

    @UiHandler("signUpButton")
    void signUpButtonClicked(ClickEvent e) {
        userManagementWriteService.createSimpleUser(usernameTextBox.getText(), emailTextBox.getText(), passwordTextBox.getText(),
                /* fullName */ null, /* company */ null, LocaleInfo.getCurrentLocale().getLocaleName(),
                EntryPointLinkFactory.createEmailValidationLink(new HashMap<String, String>()),
                new AsyncCallback<UserDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        final String message = caught.getMessage();
                                if (caught instanceof UserManagementException) {
                                    if (Util.hasLength(message) && message.equals(USER_ALREADY_EXISTS)) {
                                        Notification.notify(stringMessages.userAlreadyExists(usernameTextBox.getText()), NotificationType.ERROR);
                                    } else if (Util.hasLength(message) && message.equals(UserManagementException.CLIENT_CURRENTLY_LOCKED_FOR_USER_CREATION)) {
                                        Notification.notify(stringMessages.clientCurrentlyLockedForUserCreation(), NotificationType.ERROR);
                                    } else {
                                        Notification.notify(stringMessages.errorCreatingUser(usernameTextBox.getText(),
                                                message == null ? "" : message), NotificationType.ERROR);
                                    }
                                } else {
                                    Notification.notify(stringMessages.errorCreatingUser(usernameTextBox.getText(), message), NotificationType.ERROR);
                                }
                            }
    
                    @Override
                    public void onSuccess(UserDTO result) {
                        if (result != null) {
                            Notification.notify(stringMessages.signedUpSuccessfully(result.getName()), NotificationType.SUCCESS);
                            closeWindow();
                        } else {
                            Notification.notify(stringMessages.unknownErrorCreatingUser(usernameTextBox.getText()), NotificationType.ERROR);
                        }
                    }
        });
    }
    
    public native void closeWindow()
    /*-{
        $wnd.close();
    }-*/;
}
