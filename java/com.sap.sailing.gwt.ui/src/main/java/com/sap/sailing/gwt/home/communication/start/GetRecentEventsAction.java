package com.sap.sailing.gwt.home.communication.start;

import java.net.URL;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sailing.gwt.server.HomeServiceUtil.EventVisitor;
import com.sap.sse.gwt.dispatch.shared.caching.IsClientCacheable;
import com.sap.sse.gwt.dispatch.shared.commands.ListResult;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;

/**
 * <p>
 * {@link SailingAction} implementation to load recent events to be shown in the event list overview, where
 * the amount of loaded entries can be {@link #GetRecentEventsAction(int) limited}.
 * </p>
 */
public class GetRecentEventsAction implements SailingAction<ListResult<EventQuickfinderDTO>>, IsClientCacheable {
    
    private int limit;

    protected GetRecentEventsAction() {
    }
    
    /**
     * Creates a {@link GetRecentEventsAction} instance where the loaded entries are limited to the provided amount.
     * 
     * @param limit
     *            maximum number of entries to be loaded
     */
    public GetRecentEventsAction(int limit) {
        this.limit = limit;
    }
    
    @GwtIncompatible
    public ListResult<EventQuickfinderDTO> execute(final SailingDispatchContext context) throws DispatchException {
        final SortedSet<EventQuickfinderDTO> events = new TreeSet<>();
        HomeServiceUtil.forAllPublicEventsWithReadPermission(context.getRacingEventService(), context.getRequest(),
                context.getSecurityService(), new EventVisitor() {
            @Override
            public void visit(EventBase event, boolean onRemoteServer, URL baseURL) {
                EventQuickfinderDTO dto = new EventQuickfinderDTO();
                dto.setId((UUID) event.getId());
                dto.setDisplayName(HomeServiceUtil.getEventDisplayName(event));
                dto.setOnRemoteServer(onRemoteServer);
                dto.setBaseURL(baseURL == null ? null : baseURL.toString());
                dto.setStartTimePoint(event.getStartDate());
                dto.setState(HomeServiceUtil.calculateEventState(event));
                events.add(dto);
            }
        });
        ListResult<EventQuickfinderDTO> result = new ListResult<>();
        int count = 0;
        for (EventQuickfinderDTO event : events) {
            result.addValue(event);
            count++;
            if(limit > 0 && count >= limit) {
                break;
            }
        }
        return result;
    }

    @Override
    public void cacheInstanceKey(StringBuilder key) {
        key.append(limit);
    }
}
