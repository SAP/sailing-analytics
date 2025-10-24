package com.sap.sailing.news;

import java.util.List;

import com.sap.sailing.domain.base.Event;

public interface EventNewsService {
    List<EventNewsItem> getNews(Event event);
}
