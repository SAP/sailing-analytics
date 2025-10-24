package com.sap.sailing.aiagent.gateway.impl;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.aiagent.interfaces.AIAgent;
import com.sap.sse.util.ServiceTrackerFactory;

public class RestServletContainer extends com.sap.sailing.shared.server.gateway.jaxrs.RestServletContainer {
    private static final long serialVersionUID = -8877374150158641006L;

    public static final String AI_AGENT_TRACKER_NAME = "aiAgentServiceTracker";
    
    private ServiceTracker<AIAgent, AIAgent> aiAgentTracker;

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
       aiAgentTracker = ServiceTrackerFactory.createAndOpen(context, AIAgent.class);
       config.getServletContext().setAttribute(AI_AGENT_TRACKER_NAME, aiAgentTracker);
   }
}
