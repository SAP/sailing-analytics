package com.sap.sailing.domain.base;

import com.sap.sailing.domain.common.Named;
import com.sap.sailing.domain.common.Renamable;
import com.sap.sailing.domain.common.WithID;

/**
 * An event is a group of {@link Regatta regattas} carried out at a common venue within a common time frame. For
 * example, Kiel Week 2011 is an event, and the International German Championship 2011 held, e.g., in Travem�nde, is an event,
 * too.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public interface Event extends Named, Renamable, WithID, EventData {
    /**
     * @return a non-<code>null</code> venue for this event
     */
    Venue getVenue();
    
    String getPublicationUrl();

    void setPublicationUrl(String publicationUrl);

    boolean isPublic();

    void setPublic(boolean isPublic);

    Iterable<Regatta> getRegattas();
    
    void addRegatta(Regatta regatta);
    
    void removeRegatta(Regatta regatta);
}
