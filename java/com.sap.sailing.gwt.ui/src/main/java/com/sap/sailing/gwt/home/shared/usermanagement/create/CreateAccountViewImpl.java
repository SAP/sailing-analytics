package com.sap.sailing.gwt.home.shared.usermanagement.create;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class CreateAccountViewImpl extends Composite implements CreateAccountView {
    
    interface CreateAccountViewImplUiBinder extends UiBinder<Widget, CreateAccountViewImpl> {
    }
    
    private static CreateAccountViewImplUiBinder uiBinder = GWT.create(CreateAccountViewImplUiBinder.class);
    
    @UiField TextBox emailUi;
    @UiField TextBox usernameUi;
    @UiField PasswordTextBox passwordUi;
    @UiField PasswordTextBox passwordConfirmationUi;
    @UiField Anchor createAccountUi;
    @UiField Anchor signInUi;
    
    @UiField DivElement formErrorUi;

    private Presenter presenter;
    
    public CreateAccountViewImpl() {
        initWidget(uiBinder.createAndBindUi(this));
        
        StringMessages i18n = StringMessages.INSTANCE;
        setPlaceholder(emailUi, i18n.email());
        setPlaceholder(usernameUi, i18n.username());
        setPlaceholder(passwordUi, i18n.password());
        setPlaceholder(passwordConfirmationUi, i18n.passwordRepeat());
    }
    
    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }
    
    @Override
    public void setErrorMessage(String errorMessage) {
        formErrorUi.setInnerText(errorMessage);
    }
    
    @UiHandler("createAccountUi")
    void onCreateAccountUiControlClicked(ClickEvent event) {
        String username = usernameUi.getValue(), email = emailUi.getValue();
        String password = passwordUi.getValue(), passwordConfirmation = passwordConfirmationUi.getValue();
        presenter.createAccount(username, email, password, passwordConfirmation);
    }
    
    @UiHandler("signInUi")
    void onSignInControlUiClicked(ClickEvent event) {
        presenter.signIn();
    }
    
    private void setPlaceholder(Widget widget, String placeholderText) {
        widget.getElement().setAttribute("placeholder", placeholderText);
    }
    
}
