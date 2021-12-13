package com.sap.sailing.landscape.gateway.jaxrs;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.landscape.LandscapeService;
import com.sap.sse.util.ServiceTrackerFactory;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class RestServletContainer extends ServletContainer {
    private static final long serialVersionUID = 3129261471808425410L;

    private static final String OSGI_RFC66_WEBBUNDLE_BUNDLECONTEXT_NAME = "osgi-bundlecontext";

    public static final String LANDSCAPE_SERVICE_TRACKER_NAME = "landscapeServiceTracker";

    private ServiceTracker<LandscapeService, LandscapeService> landscapeServiceTracker;

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
        BundleContext context = (BundleContext) config.getServletContext().getAttribute(OSGI_RFC66_WEBBUNDLE_BUNDLECONTEXT_NAME);  
        landscapeServiceTracker = ServiceTrackerFactory.createAndOpen(context, LandscapeService.class);
        landscapeServiceTracker.open();
        config.getServletContext().setAttribute(LANDSCAPE_SERVICE_TRACKER_NAME, landscapeServiceTracker);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (landscapeServiceTracker != null) {
            landscapeServiceTracker.close();
        }
    }

}
