package com.sap.sailing.gwt.home.communication;

import java.util.Date;
import java.util.Locale;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.news.EventNewsService;
import com.sap.sailing.server.RacingEventService;
import com.sap.sse.gwt.dispatch.client.system.DispatchContext;

/**
 * {@link DispatchContext} implementation, which is passed to backend-side {@link SailingAction} executions to provide
 * access to the required services related to the sailing domain.
 */
public interface SailingDispatchContext extends DispatchContext {
    @GwtIncompatible
    RacingEventService getRacingEventService();

    @GwtIncompatible
    EventNewsService getEventNewsService();

    @GwtIncompatible
    String getClientLocaleName();

    @GwtIncompatible
    Locale getClientLocale();

    @GwtIncompatible
    Date getCurrentClientTime();
    
}
