package com.sap.sse.security.ui.userprofile.desktop.userdetails;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.gwt.common.CommonSharedResources;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.authentication.resource.SharedAuthenticationResources;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.userprofile.shared.userdetails.AbstractUserDetails;

/**
 * Desktop implementation of {@link AbstractUserDetails}.
 */
public class UserAccountDetails extends AbstractUserDetails {
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, UserAccountDetails> {
    }
    
    @UiField DivElement editImageLinkUi;
    
    @UiField(provided = true) final CommonSharedResources res;
    
    @UiField Label validationStatus;
    
    public UserAccountDetails(CommonSharedResources res) {
        this.res = res;
        UseraccountDetailsResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
    }
    
    @Override
    public void setUser(UserDTO currentUser) {
        super.setUser(currentUser);
        // TODO use image from user when field is available
        if (currentUser.isEmailValidated()) {
            validationStatus.setText(StringMessages.INSTANCE.emailValidated());
        } else {
            validationStatus.setText(StringMessages.INSTANCE.emailNotValidated());
        }
        editImageLinkUi.getStyle().setBackgroundImage("url('" + SharedAuthenticationResources.INSTANCE.userdefault().getSafeUri().asString() + "')");
    }
}
