package com.sap.sailing.gwt.home.communication.event.news;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.server.EventActionUtil;
import com.sap.sailing.gwt.home.server.EventActionUtil.CalculationWithEvent;
import com.sap.sailing.news.EventNewsItem;
import com.sap.sailing.news.impl.InfoEventNewsItem;
import com.sap.sailing.news.impl.LeaderboardUpdateNewsItem;
import com.sap.sse.common.Duration;
import com.sap.sse.gwt.dispatch.shared.caching.IsClientCacheable;
import com.sap.sse.gwt.dispatch.shared.commands.ListResult;
import com.sap.sse.gwt.dispatch.shared.commands.ResultWithTTL;

/**
 * <p>
 * {@link SailingAction} implementation to load data to be shown in the news ticker section for the
 * {@link #GetEventOverviewNewsAction(UUID) given event-id}, where the amount of loaded news entries can optionally be
 * {@link #GetEventOverviewNewsAction(UUID, int) limited}.
 * </p>
 * <p>
 * The {@link ResultWithTTL result's} time to live is <i>2 minutes</i> for currently running events.
 * </p>
 * <p>
 * NOTE: Because the news ticker section is only shown in currently running events, this action returns an empty
 * {@link ResultWithTTL result} with a {@link EventActionUtil#calculateTtlForNonLiveEvent(Event, EventState)
 * state-dependent} time to live, if the {@link Event} for the given id is not currently running.
 * </p>
 */
public class GetEventOverviewNewsAction implements SailingAction<ResultWithTTL<ListResult<NewsEntryDTO>>>, IsClientCacheable {
    private UUID eventId;
    /**
     * This is number of items to deliver
     */
    private int limit = 0;
    
    @SuppressWarnings("unused")
    private GetEventOverviewNewsAction() {
    }

    /**
     * Creates a {@link GetEventOverviewNewsAction} instance for the given event-id, where the amount of loaded news
     * entries is unlimited.
     * 
     * @param eventId
     *            {@link UUID} of the {@link Event} to load news for
     */
    public GetEventOverviewNewsAction(UUID eventId) {
        this.eventId = eventId;
    }
    
    /**
     * Creates a {@link GetEventOverviewNewsAction} instance for the given event-id, where the loaded news entries are
     * limited to the provided amount.
     * 
     * @param eventId
     *            {@link UUID} of the {@link Event} to load news for
     * @param limit
     *            maximum number of news entries to be loaded
     */
    public GetEventOverviewNewsAction(UUID eventId, int limit) {
        this.eventId = eventId;
        this.limit = limit;
    }
    
    @Override
    @GwtIncompatible
    public ResultWithTTL<ListResult<NewsEntryDTO>> execute(final SailingDispatchContext context) {
        return EventActionUtil.withLiveRaceOrDefaultScheduleWithReadPermissions(context, eventId, new CalculationWithEvent<ListResult<NewsEntryDTO>>() {
            @Override
            public ResultWithTTL<ListResult<NewsEntryDTO>> calculateWithEvent(Event event) {
                return new ResultWithTTL<>(Duration.ONE_MINUTE.times(2), new ListResult<NewsEntryDTO>(getNews(context, event)));
            }
        });
    }
    
    @GwtIncompatible
    private List<NewsEntryDTO> getNews(SailingDispatchContext dispatchContext, Event event) {
        List<EventNewsItem> newsItems = dispatchContext.getEventNewsService().getNews(event);
        if (this.limit > 0 && newsItems.size() > limit) {
            newsItems = newsItems.subList(0, limit);
        }
        final List<NewsEntryDTO> news = new ArrayList<>(newsItems.size());
        for (EventNewsItem newsItem : newsItems) {
            if (newsItem instanceof InfoEventNewsItem) {
                news.add(new InfoNewsEntryDTO((InfoEventNewsItem) newsItem, dispatchContext.getClientLocale()));
            }
            if (newsItem instanceof LeaderboardUpdateNewsItem) {
                news.add(new LeaderboardNewsEntryDTO((LeaderboardUpdateNewsItem) newsItem));
            }
        }
        return news;
    }

    @Override
    public void cacheInstanceKey(StringBuilder key) {
        key.append(eventId).append("_").append(limit);
    }
}
