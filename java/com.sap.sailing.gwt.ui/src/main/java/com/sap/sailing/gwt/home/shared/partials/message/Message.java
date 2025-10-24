package com.sap.sailing.gwt.home.shared.partials.message;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

public class Message extends Widget {
    
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Element, Message> {
    }
    
    @UiField SpanElement text;
    
    public Message() {
        MessageResources.INSTANCE.css().ensureInjected();
        setElement(uiBinder.createAndBindUi(this));
    }

    public void setMessage(String message) {
        if (message == null || message.isEmpty()) {
            getElement().getStyle().setDisplay(Display.NONE);
        } else {
            text.setInnerText(message);
            getElement().getStyle().clearDisplay();
        }
    }

}
