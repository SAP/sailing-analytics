package com.sap.sse.security.jaxrs;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.osgi.util.tracker.ServiceTracker;

import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.rest.StreamingOutputUtil;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SecurityUrlPathProvider;

public abstract class AbstractSecurityResource extends StreamingOutputUtil {
    @Context
    protected ServletContext servletContext;

    public SecurityService getSecurityService() {
        @SuppressWarnings("unchecked")
        ServiceTracker<SecurityService, SecurityService> tracker = (ServiceTracker<SecurityService, SecurityService>) servletContext
                .getAttribute(RestServletContainer.SECURITY_SERVICE_TRACKER_NAME);
        return tracker.getService();
    }

    public SecurityUrlPathProvider getSecurityUrlPathProvider(String type) {
        @SuppressWarnings("unchecked")
        TypeBasedServiceFinder<SecurityUrlPathProvider> securityUrlPathFinder = (TypeBasedServiceFinder<SecurityUrlPathProvider>) servletContext
                .getAttribute(RestServletContainer.SECURITY_URL_PATH_PROVIDER_NAME);
        return securityUrlPathFinder.findService(type);
    }
}
