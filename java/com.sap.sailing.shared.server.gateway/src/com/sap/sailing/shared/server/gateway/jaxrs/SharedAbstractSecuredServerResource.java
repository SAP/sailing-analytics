package com.sap.sailing.shared.server.gateway.jaxrs;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.osgi.CachedOsgiTypeBasedServiceFinderFactory;
import com.sap.sse.rest.StreamingOutputUtil;
import com.sun.jersey.api.core.ResourceContext;

public class SharedAbstractSecuredServerResource extends StreamingOutputUtil {
    @Context ServletContext servletContext;
    @Context ResourceContext resourceContext;
    
    protected ServletContext getServletContext() {
        return servletContext;
    }
    
    protected ResourceContext getResourceContext() {
        return resourceContext;
    }
    
    public <T> T getService(Class<T> clazz) {
        BundleContext context = getBundleContext();
        ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(context, clazz, null);
        tracker.open();
        T service = tracker.getService();
        tracker.close();
        return service;
    }

    protected BundleContext getBundleContext() {
        BundleContext context = (BundleContext) servletContext
                .getAttribute(RestServletContainer.OSGI_RFC66_WEBBUNDLE_BUNDLECONTEXT_NAME);
        return context;
    }
    
    protected TypeBasedServiceFinderFactory getServiceFinderFactory () {
        return new CachedOsgiTypeBasedServiceFinderFactory(getBundleContext());
    }
}
