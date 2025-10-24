package com.sap.sailing.shared.server.gateway.jaxrs;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import com.sap.sailing.server.gateway.interfaces.SailingServerFactory;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.shared.server.SharedSailingData;
import com.sap.sse.datamining.DataMiningServer;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.security.SecurityService;
import com.sap.sse.util.ServiceTrackerFactory;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class RestServletContainer extends ServletContainer {
    private static final long serialVersionUID = -3132655822281021061L;

    public static final String OSGI_RFC66_WEBBUNDLE_BUNDLECONTEXT_NAME = "osgi-bundlecontext"; 

    public static final String RACING_EVENT_SERVICE_TRACKER_NAME = "racingEventServiceTracker";
    
    public static final String SHARED_SAILING_DATA_TRACKER_NAME = "sharedSailingDataTracker";

    public static final String REPLICATION_SERVICE_TRACKER_NAME = "replicationServiceTracker";

    public static final String DATA_MINING_SERVER_TRACKER_NAME = "dataMiningServerTracker";

    public static final String SECURITY_SERVICE_TRACKER_NAME = "securityServiceTracker";
    
    public static final String SAILING_SERVER_FACTORY_TRACKER_NAME = "sailingServerFactoryTracker";

    private ServiceTracker<RacingEventService, RacingEventService> racingEventServiceTracker;

    private ServiceTracker<SharedSailingData, SharedSailingData> sharedSailingDataTracker;

    private ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker;
    
    private ServiceTracker<SecurityService, SecurityService> securityServiceTracker;
    
    private ServiceTracker<SailingServerFactory, SailingServerFactory> sailingServerFactoryTracker;
    
    private ServiceTracker<DataMiningServer, DataMiningServer> dataMiningServerTracker;
    
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
       BundleContext context = getBundleContext(config);
       racingEventServiceTracker = ServiceTrackerFactory.createAndOpen(context, RacingEventService.class);
       sharedSailingDataTracker = ServiceTrackerFactory.createAndOpen(context, SharedSailingData.class);
       replicationServiceTracker = ServiceTrackerFactory.createAndOpen(context, ReplicationService.class);
       securityServiceTracker = ServiceTrackerFactory.createAndOpen(context, SecurityService.class);
       sailingServerFactoryTracker = ServiceTrackerFactory.createAndOpen(context, SailingServerFactory.class);
       dataMiningServerTracker = ServiceTrackerFactory.createAndOpen(context, DataMiningServer.class);
       config.getServletContext().setAttribute(RACING_EVENT_SERVICE_TRACKER_NAME, racingEventServiceTracker);
       config.getServletContext().setAttribute(SHARED_SAILING_DATA_TRACKER_NAME, sharedSailingDataTracker);
       config.getServletContext().setAttribute(REPLICATION_SERVICE_TRACKER_NAME, replicationServiceTracker);
       config.getServletContext().setAttribute(SECURITY_SERVICE_TRACKER_NAME, securityServiceTracker);
       config.getServletContext().setAttribute(SAILING_SERVER_FACTORY_TRACKER_NAME, sailingServerFactoryTracker);
       config.getServletContext().setAttribute(DATA_MINING_SERVER_TRACKER_NAME, dataMiningServerTracker);
   }

    protected BundleContext getBundleContext(ServletConfig config) {
        return (BundleContext) config.getServletContext().getAttribute(OSGI_RFC66_WEBBUNDLE_BUNDLECONTEXT_NAME);
    }
}
