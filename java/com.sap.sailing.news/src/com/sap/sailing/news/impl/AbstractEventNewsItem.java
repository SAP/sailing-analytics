package com.sap.sailing.news.impl;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import com.sap.sailing.news.EventNewsItem;

public abstract class AbstractEventNewsItem extends AbstractNewsItem implements EventNewsItem {
    private UUID eventId;
    
    protected AbstractEventNewsItem(UUID eventId, String title, String message, Date createdAtDate, URL relatedItemLink) {
        super(title, message, createdAtDate, relatedItemLink);
        this.eventId = eventId;
    }
    
    @Override
    public UUID getEventUUID() {
        return eventId;
    }
}
