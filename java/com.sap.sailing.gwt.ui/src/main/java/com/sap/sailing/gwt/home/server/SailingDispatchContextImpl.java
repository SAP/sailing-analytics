package com.sap.sailing.gwt.home.server;

import java.net.URL;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authz.AuthorizationException;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.windfinder.WindFinderTrackerFactory;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sailing.news.EventNewsService;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.statistics.TrackedRaceStatisticsCache;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;
import com.sap.sse.gwt.dispatch.shared.exceptions.ServerDispatchException;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.User;

@GwtIncompatible
public class SailingDispatchContextImpl implements SailingDispatchContext {
    private final RacingEventService racingEventService;
    private final EventNewsService eventNewsService;
    private final Date currentClientTime;
    private String clientLocaleName;
    private final HttpServletRequest request;
    private final SecurityService securityService;
    private final TrackedRaceStatisticsCache trackedRaceStatisticsCache;
    private final WindFinderTrackerFactory windFinderTrackerFactory;

    public SailingDispatchContextImpl(Date currentClientTime, RacingEventService racingEventService,
            WindFinderTrackerFactory windFinderTrackerFactory, EventNewsService eventNewsService,
            SecurityService securityService, TrackedRaceStatisticsCache trackedRaceStatisticsCache,
            String clientLocaleName, HttpServletRequest request) {
        this.currentClientTime = currentClientTime;
        this.racingEventService = racingEventService;
        this.windFinderTrackerFactory = windFinderTrackerFactory;
        this.eventNewsService = eventNewsService;
        this.securityService = securityService;
        this.trackedRaceStatisticsCache = trackedRaceStatisticsCache;
        this.clientLocaleName = clientLocaleName;
        this.request = request;
    }
    
    @Override
    public RacingEventService getRacingEventService() {
        return racingEventService;
    }
    
    @Override
    public WindFinderTrackerFactory getWindFinderTrackerFactory() {
        return windFinderTrackerFactory;
    }

    public EventNewsService getEventNewsService() {
        return eventNewsService;
    }
    
    @Override
    public Date getCurrentClientTime() {
        return currentClientTime;
    }
    
//    public Date getCurrentServerTime() {
//        return currentServerTime;
//    }
    
    @Override
    public String getClientLocaleName() {
        return clientLocaleName;
    }
    
    @Override
    public Locale getClientLocale() {
        return Locale.forLanguageTag(clientLocaleName);
    }
    
    @Override
    public HttpServletRequest getRequest() {
        return request;
    }
    
    @Override
    public SecurityService getSecurityService() {
        return securityService;
    }

    @Override
    public URL getRequestBaseURL() throws DispatchException {
        return HomeServiceUtil.getRequestBaseURL(request);
    }
    
    @Override
    public <T> T getPreferenceForCurrentUser(String preferenceKey) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser != null) {
            return securityService.getPreferenceObject(currentUser.getName(), preferenceKey);
        }
        return null;
    }
    
    @Override
    public void setPreferenceForCurrentUser(String preferenceKey, Object preference) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser != null) {
            try {
                securityService.setPreferenceObject(currentUser.getName(), preferenceKey, preference);
            } catch (AuthorizationException e) {
                throw new ServerDispatchException(e);
            }
        }
    }
    
    @Override
    public TrackedRaceStatisticsCache getTrackedRaceStatisticsCache() {
        return trackedRaceStatisticsCache;
    }
}
