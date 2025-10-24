package com.sap.sailing.domain.igtimiadapter.gateway.impl;

import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sse.security.jaxrs.AbstractSecurityResource;

public class AbstractRiotServerResource extends AbstractSecurityResource {
    public RiotServer getRiotService() {
        @SuppressWarnings("unchecked")
        ServiceTracker<RiotServer, RiotServer> tracker = (ServiceTracker<RiotServer, RiotServer>) servletContext
                .getAttribute(RestServletContainer.RIOT_SERVICE_TRACKER_NAME);
        return tracker.getService();
    }
}
