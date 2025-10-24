package com.sap.sse.security.ui.userprofile.shared.userheader;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.sap.sse.security.ui.authentication.app.AuthenticationContext;
import com.sap.sse.security.ui.authentication.app.NeedsAuthenticationContext;
import com.sap.sse.security.ui.authentication.resource.SharedAuthenticationResources;

/**
 * Base view class of the user header. This class implements the shared logic of the desktop and mobile
 * version of the partial.
 * 
 * {@link UiField}s and {@link UiHandler}s are intentionally marked as public to make it visible to UiBinder in concrete
 * subclasses. These fields should not be accessed manually.
 */
public class AbstractUserHeader extends Composite implements NeedsAuthenticationContext {

    @UiField public Element titleUi;
    @UiField public DivElement imageUi;
    @UiField public DivElement subtitleUi;
    
    public AbstractUserHeader() {
    }
    
    @Override
    public void setAuthenticationContext(AuthenticationContext authenticationContext) {
//        UserDTO currentUser = authenticationContext.getCurrentUser();
        // TODO use image from user when field is available
        imageUi.getStyle().setBackgroundImage("url('" + SharedAuthenticationResources.INSTANCE.userdefault().getSafeUri().asString() + "')");
        
        titleUi.setInnerText(authenticationContext.getUserTitle());
        subtitleUi.setInnerText(authenticationContext.getUserSubtitle());
    }

}
