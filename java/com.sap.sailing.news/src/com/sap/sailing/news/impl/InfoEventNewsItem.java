package com.sap.sailing.news.impl;

import java.net.URL;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class InfoEventNewsItem extends AbstractEventNewsItem {
    private Map<Locale, String> titles;
    private Map<Locale, String> messages;
    
    public InfoEventNewsItem(UUID eventId, String title, String message, Date createdAtDate, URL relatedItemLink,
            Map<Locale, String> titles, Map<Locale, String> messages) {
        super(eventId, title, message, createdAtDate, relatedItemLink);
        this.titles = titles;
        this.messages = messages;
    }
    
    public String getTitle(Locale locale) {
        if(titles.containsKey(locale)) {
            return titles.get(locale);
        }
        return super.getTitle();
    }
    
    public String getMessage(Locale locale) {
        if(messages.containsKey(locale)) {
            return messages.get(locale);
        }
        return super.getMessage();
    }

}
