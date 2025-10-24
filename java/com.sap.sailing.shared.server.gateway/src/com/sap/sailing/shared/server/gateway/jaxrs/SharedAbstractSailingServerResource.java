package com.sap.sailing.shared.server.gateway.jaxrs;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.interfaces.SailingServerFactory;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.shared.server.SharedSailingData;
import com.sap.sse.InvalidDateException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.security.SecurityService;
import com.sap.sse.util.DateParser;

public abstract class SharedAbstractSailingServerResource extends SharedAbstractSecuredServerResource {
    private static final String SLASH_ENCODING = "__";
    public RacingEventService getService() {
        @SuppressWarnings("unchecked")
        ServiceTracker<RacingEventService, RacingEventService> tracker = (ServiceTracker<RacingEventService, RacingEventService>) servletContext.getAttribute(RestServletContainer.RACING_EVENT_SERVICE_TRACKER_NAME);
        return tracker.getService(); 
    }
    
    protected SharedSailingData getSharedSailingData() {
        @SuppressWarnings("unchecked")
        ServiceTracker<SharedSailingData, SharedSailingData> tracker = (ServiceTracker<SharedSailingData, SharedSailingData>) servletContext.getAttribute(RestServletContainer.SHARED_SAILING_DATA_TRACKER_NAME);
        return tracker.getService(); 
    }
    
    protected SecurityService getSecurityService() {
        @SuppressWarnings("unchecked")
        ServiceTracker<SecurityService, SecurityService> tracker = (ServiceTracker<SecurityService, SecurityService>) servletContext.getAttribute(RestServletContainer.SECURITY_SERVICE_TRACKER_NAME);
        return tracker.getService(); 
    }
    
    protected ReplicationService getReplicationService() {
        @SuppressWarnings("unchecked")
        ServiceTracker<ReplicationService, ReplicationService> tracker = (ServiceTracker<ReplicationService, ReplicationService>) servletContext.getAttribute(RestServletContainer.REPLICATION_SERVICE_TRACKER_NAME);
        return tracker.getService(); 
    }
    
    protected SailingServerFactory getSailingServerFactory() {
        @SuppressWarnings("unchecked")
        ServiceTracker<SailingServerFactory, SailingServerFactory> tracker = (ServiceTracker<SailingServerFactory, SailingServerFactory>) servletContext.getAttribute(RestServletContainer.SAILING_SERVER_FACTORY_TRACKER_NAME);
        return tracker.getService(); 
    }
    
    protected Regatta findRegattaByName(String regattaName) {
        Regatta regatta = getService().getRegattaByName(regattaName);
        if (regatta == null && regattaName.contains(SLASH_ENCODING)) {
            regatta = getService().getRegattaByName(regattaName.replaceAll(SLASH_ENCODING, "/"));
        }
        return regatta;
    }
    
    protected RaceColumn findRaceColumnByName(Regatta regatta, String raceColumnName) {
        RaceColumn raceColumn = regatta.getRaceColumnByName(raceColumnName);
        if (raceColumn == null && raceColumnName.contains(SLASH_ENCODING)) {
            raceColumn = regatta.getRaceColumnByName(raceColumnName.replaceAll(SLASH_ENCODING, "/"));
        }
        return raceColumn;
    }
    
    protected Fleet findFleetByName(RaceColumn raceColumn, String fleetName) {
        Fleet fleet = raceColumn.getFleetByName(fleetName);
        if (fleet == null && fleetName.contains(SLASH_ENCODING)) {
            fleet = raceColumn.getFleetByName(fleetName.replaceAll(SLASH_ENCODING, "/"));
        }
        return fleet;
    }

    protected RaceDefinition findRaceByName(Regatta regatta, String raceName) {
        RaceDefinition result = null;
        if (regatta != null) {
            result = regatta.getRaceByName(raceName);
            if (result == null && raceName.contains(SLASH_ENCODING)) {
                result = regatta.getRaceByName(raceName.replaceAll(SLASH_ENCODING, "/"));
            }
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
    
    protected <T> T[] getServices(Class<T> clazz) {
        final ServiceTracker<T, T> tracker = getServiceTracker(clazz);
        final Object[] objectServices = tracker.getServices();
        @SuppressWarnings("unchecked")
        final T[] services = (T[]) Array.newInstance(clazz, objectServices.length);
        System.arraycopy(objectServices, 0, services, 0, services.length);
        tracker.close();
        return services;
    }

    protected <T> ServiceTracker<T, T> getServiceTracker(Class<T> clazz) {
        final BundleContext context = getBundleContext();
        final ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(context, clazz, null);
        tracker.open();
        return tracker;
    }

    protected TrackedRace findTrackedRace(Regatta regatta, String raceName) {
        final TrackedRace trackedRace;
        final RaceDefinition race = findRaceByName(regatta, raceName);
        if (race != null) {
            DynamicTrackedRegatta trackedRegatta = getService().getTrackedRegatta(regatta);
            if (trackedRegatta != null) {
                trackedRace = trackedRegatta.getExistingTrackedRace(race);
            } else {
                trackedRace = null;
            }
        } else {
            trackedRace = null;
        }
        return trackedRace;
    }

    public RaceLogTrackingAdapter getRaceLogTrackingAdapter() {
        return getService(RaceLogTrackingAdapterFactory.class).getAdapter(getService().getBaseDomainFactory());
    }

    protected static Double roundDouble(Double value, int places) {
        BigDecimal bigDecimal = new BigDecimal(value);
        bigDecimal = bigDecimal.setScale(places, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }
}
