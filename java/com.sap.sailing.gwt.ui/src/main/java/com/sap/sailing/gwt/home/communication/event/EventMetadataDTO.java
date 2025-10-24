package com.sap.sailing.gwt.home.communication.event;

import java.util.Date;

import com.sap.sailing.gwt.home.communication.eventlist.EventListEventSeriesDTO;
import com.sap.sailing.gwt.ui.shared.EventReferenceDTO;

public class EventMetadataDTO extends EventReferenceDTO {

    private EventState state;
    private String venue;
    private String location;
    private Date startDate;
    private Date endDate;
    private String thumbnailImageURL;
    
    private Date currentServerTime;
    
    public EventMetadataDTO() {
        initCurrentServerTime();
    }

    public boolean isRunning() {
        return state == EventState.RUNNING;
    }
    
    public boolean isStarted() {
        return state.compareTo(EventState.RUNNING) >= 0;
    }
    
    public boolean isFinished() {
        return state == EventState.FINISHED;
    }

    private void initCurrentServerTime() {
        currentServerTime = new Date();
    }

    public Date getCurrentServerTime() {
        return currentServerTime;
    }

    public EventState getState() {
        return state;
    }
    
    public void setState(EventState state) {
        this.state = state;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getLocation() {
        return location;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getThumbnailImageURL() {
        return thumbnailImageURL;
    }

    public void setThumbnailImageURL(String thumbnailImageURL) {
        this.thumbnailImageURL = thumbnailImageURL;
    }

    public String getLocationOrVenue() {
        if(location != null && !location.isEmpty()) {
            return location;
        }
        return venue;
    }
    
    public String getLocationOrDisplayName() {
        if(location != null && !location.isEmpty()) {
            return location;
        }
        return getDisplayName();
    }
    
    public String getLocationAndVenue() {
        if(location != null && !location.isEmpty()) {
            return location + ", " + venue;
        }
        return venue;
    }
    
    public EventListEventSeriesDTO getEventSeries() {
        return null;
    }
}
