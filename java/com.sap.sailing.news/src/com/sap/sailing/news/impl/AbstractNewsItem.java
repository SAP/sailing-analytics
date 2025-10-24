package com.sap.sailing.news.impl;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import com.sap.sailing.news.NewsItem;

public abstract class AbstractNewsItem implements NewsItem {
    private final UUID id;
    
    private String title;
    
    private String message;
    
    private URL relatedItemLink;
    
    private Date createdAtDate;

    protected AbstractNewsItem(String title, String message, Date createdAtDate, URL relatedItemLink) {
        this.title = title;
        this.message = message;
        this.createdAtDate = createdAtDate;
        this.relatedItemLink = relatedItemLink;
        this.id = UUID.randomUUID();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public URL getRelatedItemLink() {
        return relatedItemLink;
    }

    @Override
    public Date getCreatedAtDate() {
        return createdAtDate;
    }

    @Override
    public UUID getId() {
        return id;
    }
}
