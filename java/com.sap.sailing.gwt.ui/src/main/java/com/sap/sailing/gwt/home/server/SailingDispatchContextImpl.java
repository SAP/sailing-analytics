package com.sap.sailing.gwt.home.server;

import java.net.URL;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sailing.news.EventNewsService;
import com.sap.sailing.server.RacingEventService;
import com.sap.sse.gwt.dispatch.client.exceptions.DispatchException;

@GwtIncompatible
public class SailingDispatchContextImpl implements SailingDispatchContext {
    private final RacingEventService racingEventService;
    private final EventNewsService eventNewsService;
    private final Date currentClientTime;
//    private final Date currentServerTime = new Date();
    private String clientLocaleName;
    private final HttpServletRequest request;

    public  SailingDispatchContextImpl(Date currentClientTime, RacingEventService racingEventService, EventNewsService eventNewsService, String clientLocaleName, HttpServletRequest request) {
        this.currentClientTime = currentClientTime;
        this.racingEventService = racingEventService;
        this.eventNewsService = eventNewsService;
        this.clientLocaleName = clientLocaleName;
        this.request = request;
    }
    
    @Override
    public RacingEventService getRacingEventService() {
        return racingEventService;
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
    public URL getRequestBaseURL() throws DispatchException {
        return HomeServiceUtil.getRequestBaseURL(request);
    }
    
}
