package com.sap.sailing.gwt.home.shared.usermanagement.signin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.shared.ExperimentalFeatures;
import com.sap.sailing.gwt.home.shared.usermanagement.UserManagementResources;

public class SignInViewImpl extends Composite implements SignInView {
    
    interface SignInViewImplUiBinder extends UiBinder<Widget, SignInViewImpl> {
    }
    
    private static SignInViewImplUiBinder uiBinder = GWT.create(SignInViewImplUiBinder.class);
    
    @UiField TextBox loginNameUi;
    @UiField PasswordTextBox passwordUi;
    @UiField Anchor forgotPasswordUi;
    @UiField Anchor createAccountUi;
    @UiField Anchor signInUi;
    @UiField Anchor loginFacebookUi;
    @UiField Anchor loginGoogleUi;
    
    @UiField DivElement formErrorUi;
    @UiField DivElement socialLoginUi;

    private Presenter presenter;

    public SignInViewImpl() {
        UserManagementResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        
        if (!ExperimentalFeatures.SHOW_SOCIAL_LOGINS_FOR_USER_MANGEMENT) {
            socialLoginUi.removeFromParent();
        }
    }
    
    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }
    
    @Override
    public void setErrorMessage(String errorMessage) {
        formErrorUi.setInnerText(errorMessage);
        selectAll(passwordUi.getValue().isEmpty() ? loginNameUi : passwordUi);
    }
    
    @Override
    protected void onLoad() {
        selectAll(loginNameUi);
    }
    
    @UiHandler("forgotPasswordUi")
    void onForgotPasswordUiControlClicked(ClickEvent event) {
        presenter.forgotPassword();
    }
    
    @UiHandler("createAccountUi")
    void onCreateAccountUiControlClicked(ClickEvent event) {
        presenter.createAccount();
    }
    
    @UiHandler("signInUi")
    void onSignInControlUiClicked(ClickEvent event) {
        triggerLogin();
    }
    
    @UiHandler({"loginNameUi","passwordUi"}) 
    void onSignInKeyPressed(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            triggerLogin();
        }
    }
    
    @UiHandler("loginFacebookUi")
    void onLoginFacebookUiClicked(ClickEvent event) {
        presenter.loginWithFacebook();
    }
    
    @UiHandler("loginGoogleUi")
    void onLoginGoogleUiClicked(ClickEvent event) {
        presenter.loginWithGoogle(); 
    }
    
    private void triggerLogin() {
        presenter.login(loginNameUi.getValue(), passwordUi.getValue());
    }
    
    private void selectAll(TextBox textBox) {
        textBox.setFocus(true);
        textBox.selectAll();
    }
    
}
