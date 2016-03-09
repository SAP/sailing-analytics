package com.sap.sse.security.ui.userprofile.shared.userdetails;

import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.shared.UserDTO;

/**
 * Base view class of the user account details page. This class implements the shared logic of the desktop and mobile
 * version of the page.
 * 
 * {@link UiField}s and {@link UiHandler}s are intentionally marked as public to make it visible to UiBinder in concrete
 * subclasses. These fields should not be accessed manually.
 */
public class AbstractUserDetails extends Composite implements UserDetailsView {
    
    @UiField public InputElement usernameUi;
    @UiField public TextBox nameUi;
    @UiField public TextBox companyUi;
    @UiField public TextBox emailUi;
    @UiField public PasswordTextBox oldPasswordUi;
    @UiField public PasswordTextBox newPasswordUi;
    @UiField public PasswordTextBox newPasswordConfirmationUi;
    
    private Presenter presenter;
    
    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }
    
    protected Presenter getPresenter() {
        return presenter;
    }
    
    @Override
    protected void onLoad() {
        super.onLoad();
        StringMessages i18n = StringMessages.INSTANCE;
        setPlaceholder(oldPasswordUi, i18n.oldPasswordPlaceholder());
        setPlaceholder(newPasswordUi, i18n.newPasswordPlaceholder());
        setPlaceholder(newPasswordConfirmationUi, i18n.passwordRepeatPlaceholder());
    }

    public void setUser(UserDTO currentUser) {
        nameUi.setValue(currentUser.getFullName());
        companyUi.setValue(currentUser.getCompany());
        usernameUi.setValue(currentUser.getName());
        emailUi.setValue(currentUser.getEmail());
        
        clearPasswordFields();
    }

    public void clearPasswordFields() {
        oldPasswordUi.setValue("");
        newPasswordUi.setValue("");
        newPasswordConfirmationUi.setValue("");
    }
    
    private void setPlaceholder(Widget widget, String placeholderText) {
        widget.getElement().setAttribute("placeholder", placeholderText);
    }
    
    @UiHandler("saveChangesUi")
    public void onSaveChangesClicked(ClickEvent event) {
        presenter.handleSaveChangesRequest(nameUi.getValue(), companyUi.getValue());
    }
    
    @UiHandler("changeEmailUi")
    public void onChangeEmailClicked(ClickEvent event) {
        presenter.handleEmailChangeRequest(emailUi.getValue());
    }
    
    @UiHandler("changePasswordUi")
    public void onChangePasswordClicked(ClickEvent event) {
        presenter.handlePasswordChangeRequest(oldPasswordUi.getValue(), newPasswordUi.getValue(),
                newPasswordConfirmationUi.getValue());
    }
}
