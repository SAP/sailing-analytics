package com.sap.sse.security.jaxrs;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.osgi.CachedOsgiTypeBasedServiceFinderFactory;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SecurityUrlPathProvider;
import com.sap.sse.security.impl.SecurityUrlPathProviderDefaultImpl;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class RestServletContainer extends ServletContainer {
    private static final long serialVersionUID = 3129261471808425410L;

    private static final String OSGI_RFC66_WEBBUNDLE_BUNDLECONTEXT_NAME = "osgi-bundlecontext";

    public static final String SECURITY_SERVICE_TRACKER_NAME = "securityServiceTracker";

    public static final String SECURITY_URL_PATH_PROVIDER_NAME = "securityUrlPathProvider";

    private ServiceTracker<SecurityService, SecurityService> securityServiceTracker;

    private CachedOsgiTypeBasedServiceFinderFactory securityUrlPathFinderFactory;
    
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
        securityServiceTracker = new ServiceTracker<SecurityService, SecurityService>(context, SecurityService.class.getName(), null);
        securityServiceTracker.open();
        config.getServletContext().setAttribute(SECURITY_SERVICE_TRACKER_NAME, securityServiceTracker);
        securityUrlPathFinderFactory = new CachedOsgiTypeBasedServiceFinderFactory(context);
        TypeBasedServiceFinder<SecurityUrlPathProvider> securityUrlPathFinder = securityUrlPathFinderFactory.createServiceFinder(SecurityUrlPathProvider.class);
        securityUrlPathFinder.setFallbackService(new SecurityUrlPathProviderDefaultImpl());
        config.getServletContext().setAttribute(SECURITY_URL_PATH_PROVIDER_NAME, securityUrlPathFinder);
    }

    protected BundleContext getBundleContext(ServletConfig config) {
        return (BundleContext) config.getServletContext().getAttribute(OSGI_RFC66_WEBBUNDLE_BUNDLECONTEXT_NAME);
    }

    @Override
    public void destroy() {
        super.destroy();

        if (securityServiceTracker != null) {
            securityServiceTracker.close();
        }
        if (securityUrlPathFinderFactory != null) {
            securityUrlPathFinderFactory.close();
        }
    }

    public SecurityService getService() {
        return securityServiceTracker.getService();
    }
}
