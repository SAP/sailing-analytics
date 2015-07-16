package com.sap.sailing.gwt.home.mobile.partials.message;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface MessageResources extends ClientBundle {
    public static final MessageResources INSTANCE = GWT.create(MessageResources.class);

    @Source("Message.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String message();
        String message_icon();
        String message_iconnotification();
        String message_iconclose();
        String message_text();
    }
}
