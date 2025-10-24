package com.sap.sailing.domain.igtimiadapter.gateway.impl;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sse.util.ServiceTrackerFactory;

public class RestServletContainer extends com.sap.sse.security.jaxrs.RestServletContainer {
    private static final long serialVersionUID = -8877374150158641006L;

    public static final String RIOT_SERVICE_TRACKER_NAME = "riotServiceTracker";
    
    private ServiceTracker<RiotServer, RiotServer> riotServiceTracker;

    public RestServletContainer() {
        super();
    }

    public RestServletContainer(Application app) {
        super(app);
    }

    public RestServletContainer(Class<? extends Application> appClass) {
        super(appClass);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {  
       super.init(config);
       final BundleContext context = getBundleContext(config);
       riotServiceTracker = ServiceTrackerFactory.createAndOpen(context, RiotServer.class);
       config.getServletContext().setAttribute(RIOT_SERVICE_TRACKER_NAME, riotServiceTracker);
   }
}
