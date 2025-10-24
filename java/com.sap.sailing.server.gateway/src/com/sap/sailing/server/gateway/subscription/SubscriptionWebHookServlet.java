package com.sap.sailing.server.gateway.subscription;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.sailing.server.gateway.SailingServerHttpServlet;

public class SubscriptionWebHookServlet extends SailingServerHttpServlet {
    private static final long serialVersionUID = -6953289975184826299L;
    private static final Logger logger = Logger.getLogger(SubscriptionWebHookServlet.class.getName());

    public SubscriptionWebHookServlet() {
        super();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final String path = getRequestHandlerPath(request);
        final SubscriptionWebHookHandler handler = SubscriptionWebHookHandlerFactory.getInstance().getHandlerForPath(path,
                this);
        if (handler != null) {
            handler.handle(request, response);
        } else {
            logger.warning(() -> "Unable to get subscription webhook handler for request /" + path);
        }
    }

    private String getRequestHandlerPath(HttpServletRequest request) {
        final String pathInfo = request.getPathInfo();
        final String[] pathParts = pathInfo.split("/");
        return pathParts[pathParts.length - 1];
    }
}
