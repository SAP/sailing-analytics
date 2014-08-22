package com.sap.sailing.gwt.home.client.shared.error;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.client.i18n.TextMessages;

public class ErrorMessage extends Composite {

    interface ErrorUiBinder extends UiBinder<Widget, ErrorMessage> {
    }
    
    private static ErrorUiBinder uiBinder = GWT.create(ErrorUiBinder.class);

    @UiField DivElement errorMessage;
    @UiField DivElement errorMessageDetail;
    @UiField Anchor reloadPageAnchor;
    
    public ErrorMessage(String detailMessage, Throwable errorReason) {
        ErrorMessageResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        
        this.setHeight(Window.getClientHeight() + "px");
        errorMessage.setInnerText(TextMessages.INSTANCE.errorMessageLoadingData());
        
        Window.setStatus(detailMessage);
    }
    
    @UiHandler("reloadPageAnchor")
    void reloadPage(ClickEvent e) {
        Window.Location.reload();
    }
}
