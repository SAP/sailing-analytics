package com.sap.sse.security.ui.authentication;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event to be fired if the user needs to see the authentication UI.
 */
public class AuthenticationRequestEvent extends GwtEvent<AuthenticationRequestEvent.Handler> {
    public static final Type<Handler> TYPE = new Type<AuthenticationRequestEvent.Handler>();

    public interface Handler extends EventHandler {
        void onUserManagementRequestEvent(AuthenticationRequestEvent event);
    }
    

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onUserManagementRequestEvent(this);
    }
}
