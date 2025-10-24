package com.sap.sailing.gwt.home.communication.search;

import java.util.Date;
import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.shared.DTO;

public class SearchResultEventInfoDTO implements DTO, Comparable<SearchResultEventInfoDTO> {
    
    private UUID id;
    private String name;
    private String venueName;
    private Date startDate;
    private Date endDate;

    @SuppressWarnings("unused")
    private SearchResultEventInfoDTO() {
    }

    @GwtIncompatible
    public SearchResultEventInfoDTO(EventBase event) {
        this.id = (UUID) event.getId();
        this.name = event.getName();
        this.venueName = event.getVenue() != null ? event.getVenue().getName() : null;
        this.startDate = event.getStartDate() != null ? event.getStartDate().asDate() : null;
        this.endDate = event.getEndDate() != null ? event.getEndDate().asDate() : null;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVenueName() {
        return venueName;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }
    
    @Override
    public int compareTo(SearchResultEventInfoDTO other) {
        return new NaturalComparator().compare(this.name, other.name);
    }

}
