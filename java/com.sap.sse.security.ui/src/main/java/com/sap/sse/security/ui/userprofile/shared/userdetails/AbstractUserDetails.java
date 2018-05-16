package com.sap.sse.security.ui.userprofile.shared.userdetails;

import java.io.IOException;

import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.GWTLocaleUtil;
import com.sap.sse.security.shared.UserGroup;
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
    
    @UiField(provided = true)
    public ValueListBox<String> localeUi = new ValueListBox<String>(new Renderer<String>() {
        @Override
        public String render(String object) {
            return GWTLocaleUtil.getDecoratedLanguageDisplayNameWithDefaultLocaleSupport(object);
        }

        @Override
        public void render(String object, Appendable appendable) throws IOException {
            appendable.append(render(object));
        }
    });
    
    @UiField public TextBox emailUi;
    
    /**
     * The item texts are the tenant names, the values are the tenant IDs as strings
     */
    @UiField public ListBox defaultTenantUi;
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
        presenter.fillTenants(defaultTenantUi);
    }

    public void setUser(UserDTO currentUser) {
        nameUi.setValue(currentUser.getFullName());
        companyUi.setValue(currentUser.getCompany());
        usernameUi.setValue(currentUser.getName());
        emailUi.setValue(currentUser.getEmail());
        updateDefaultTenantSelection(currentUser.getDefaultTenant());
        String currentLocale = currentUser.getLocale();
        localeUi.setValue(currentLocale);
        localeUi.setAcceptableValues(GWTLocaleUtil.getAvailableLocalesAndDefault());
        clearPasswordFields();
    }

    private void updateDefaultTenantSelection(UserGroup defaultTenant) {
        boolean found = false;
        for (int i=0; i<defaultTenantUi.getItemCount(); i++) {
            final String idOfTenantFromListBoxAsString = defaultTenantUi.getValue(i);
            if (Util.equalsWithNull(defaultTenant.getId().toString(), idOfTenantFromListBoxAsString)) {
                defaultTenantUi.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        if (!found) {
            // add the missing tenant to the list
            defaultTenantUi.addItem(defaultTenant.getName(), defaultTenant.getId().toString());
            defaultTenantUi.setSelectedIndex(defaultTenantUi.getItemCount()-1);
        }
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
        presenter.handleSaveChangesRequest(nameUi.getValue(), companyUi.getValue(), localeUi.getValue(), defaultTenantUi.getSelectedValue());
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
