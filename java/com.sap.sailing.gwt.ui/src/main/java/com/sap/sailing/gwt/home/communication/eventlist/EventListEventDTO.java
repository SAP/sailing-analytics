package com.sap.sailing.gwt.home.communication.eventlist;

import com.sap.sailing.gwt.common.communication.event.EventSeriesMetadataDTO;
import com.sap.sailing.gwt.home.communication.event.EventLinkAndMetadataDTO;

public class EventListEventDTO extends EventLinkAndMetadataDTO {

    private static final long serialVersionUID = 7729841556203148870L;

    private EventSeriesMetadataDTO eventSeries;

    @Override
    public EventSeriesMetadataDTO getEventSeries() {
        return eventSeries;
    }

    public void setEventSeries(EventSeriesMetadataDTO eventSeries) {
        this.eventSeries = eventSeries;
    }
    
}
