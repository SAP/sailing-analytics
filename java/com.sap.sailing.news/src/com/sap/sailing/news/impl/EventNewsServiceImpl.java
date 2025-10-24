package com.sap.sailing.news.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.news.EventNewsItem;
import com.sap.sailing.news.EventNewsProvider;
import com.sap.sailing.news.EventNewsProviderRegistry;
import com.sap.sailing.news.EventNewsService;

public class EventNewsServiceImpl implements EventNewsService {
    private final static int LIMIT = 25;
    private final EventNewsProviderRegistry providerRegistry;
    
    public EventNewsServiceImpl(EventNewsProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    public List<EventNewsItem> getNews(Event event) {
        final List<EventNewsItem> result = new ArrayList<>();
        for (EventNewsProvider provider : providerRegistry.getEventNewsProvider()) {
            Collection<? extends EventNewsItem> news = provider.getNews(event);
            result.addAll(news);
        }
        Collections.sort(result);
        return result.subList(0, Math.min(LIMIT, result.size()));
    }
}
