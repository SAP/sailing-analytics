package com.sap.sailing.news;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import com.sap.sse.common.Util;

/**
 * An interface for a generic news item. The natural ordering, implementing the {@link Comparable} interface,
 * is defined by the {@link #getCreatedAtDate() creation time point}.
 * 
 * @author Frank Mittag
 *
 */
public interface NewsItem extends Comparable<NewsItem> {
    UUID getId();
    
    String getTitle();
    
    String getMessage();
    
    URL getRelatedItemLink();
    
    Date getCreatedAtDate();
    
    @Override
    default int compareTo(NewsItem o) {
        final Date otherCreatedAtDate = o.getCreatedAtDate();
        return -Util.compareToWithNull(getCreatedAtDate(), otherCreatedAtDate, /* nullIsLess */ true);
    }
}
