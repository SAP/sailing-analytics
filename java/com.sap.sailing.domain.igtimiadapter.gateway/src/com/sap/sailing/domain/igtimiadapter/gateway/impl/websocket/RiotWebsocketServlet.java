package com.sap.sailing.domain.igtimiadapter.gateway.impl.websocket;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class RiotWebsocketServlet extends WebSocketServlet {
    private static final long serialVersionUID = 8503012754394978512L;

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.register(RiotWebsocketHandlerImpl.class);
    }
}
