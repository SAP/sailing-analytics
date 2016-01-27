package com.sap.sailing.server.gateway.jaxrs;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.RacingEventService;
import com.sap.sse.InvalidDateException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.util.DateParser;
import com.sun.jersey.api.core.ResourceContext;

public abstract class AbstractSailingServerResource {
    @Context ServletContext servletContext;
    @Context ResourceContext resourceContext;
    
    protected ServletContext getServletContext() {
        return servletContext;
    }
    
    protected ResourceContext getResourceContext() {
        return resourceContext;
    }
    
    protected <T> T getService(Class<T> clazz) {
        BundleContext context = (BundleContext) servletContext
                .getAttribute(RestServletContainer.OSGI_RFC66_WEBBUNDLE_BUNDLECONTEXT_NAME);
        ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(context, clazz, null);
        tracker.open();
        T service = tracker.getService();
        tracker.close();
        return service;
    }

    public RacingEventService getService() {
        @SuppressWarnings("unchecked")
        ServiceTracker<RacingEventService, RacingEventService> tracker = (ServiceTracker<RacingEventService, RacingEventService>) servletContext.getAttribute(RestServletContainer.RACING_EVENT_SERVICE_TRACKER_NAME);
        return tracker.getService(); 
    }
    
    protected Regatta findRegattaByName(String regattaName) {
        return getService().getRegattaByName(regattaName);
    }

    protected RaceDefinition findRaceByName(Regatta regatta, String raceName) {
        RaceDefinition result = null;
        if (regatta != null) {
            result = regatta.getRaceByName(raceName);
        }
        return result;
    }

    protected TimePoint parseTimePoint(String isoTime, Long timeAsMillis, TimePoint defaultTime) throws InvalidDateException {
        TimePoint timePoint;
        if (isoTime != null && isoTime.length() > 0) {
            timePoint = new MillisecondsTimePoint(DateParser.parse(isoTime).getTime());
        } else {
            timePoint = timeAsMillis != null ? new MillisecondsTimePoint(timeAsMillis) : defaultTime;
        }
        return timePoint;
    }

    protected TrackedRace findTrackedRace(String regattaName, String raceName) {
        Regatta regatta = findRegattaByName(regattaName);
        RaceDefinition race = findRaceByName(regatta, raceName);
        TrackedRace trackedRace = null;
        if (regatta != null && race != null) {
            DynamicTrackedRegatta trackedRegatta = getService().getTrackedRegatta(regatta);
            if (trackedRegatta != null) {
                trackedRace = trackedRegatta.getExistingTrackedRace(race);
            }
        }
        return trackedRace;
    }
    
    protected static Double roundDouble(Double value, int places) {
        BigDecimal bigDecimal = new BigDecimal(value);
        bigDecimal = bigDecimal.setScale(places, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }
}
