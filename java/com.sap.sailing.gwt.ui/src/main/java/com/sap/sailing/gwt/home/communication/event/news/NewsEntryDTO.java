package com.sap.sailing.gwt.home.communication.event.news;

import java.net.URL;
import java.util.Date;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sse.gwt.shared.DTO;

public abstract class NewsEntryDTO implements DTO {
    
    private Date newsTimestamp;
    
    private String title;

    private String externalURL;

    protected NewsEntryDTO() {
    }
    
    @GwtIncompatible
    public NewsEntryDTO(String title, Date timestamp, URL externalURL) {
        this(title, timestamp, externalURL == null ? null : externalURL.toString());
    }
    
    @GwtIncompatible
    private NewsEntryDTO(String title, Date newsTimestamp, String externalURL) {
        this.title = title;
        this.newsTimestamp = newsTimestamp;
        this.externalURL = externalURL;
    }

    public String getTitle() {
        return title;
    }

    public abstract String getMessage();
    
    public abstract String getBoatClass();
    
    public Date getTimestamp() {
        return newsTimestamp;
    }
    
    public String getExternalURL() {
        return externalURL;
    }
}
