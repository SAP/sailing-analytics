package com.sap.sailing.gwt.home.communication.start;

import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.ui.shared.EventReferenceDTO;
import com.sap.sse.common.TimePoint;
import com.sap.sse.gwt.shared.DTO;

public class EventQuickfinderDTO extends EventReferenceDTO implements DTO, Comparable<EventQuickfinderDTO> {
    private String baseURL;
    private boolean isOnRemoteServer;
    private TimePoint startTimePoint;
    private EventState state;

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public boolean isOnRemoteServer() {
        return isOnRemoteServer;
    }

    public void setOnRemoteServer(boolean isOnRemoteServer) {
        this.isOnRemoteServer = isOnRemoteServer;
    }

    public TimePoint getStartTimePoint() {
        return startTimePoint;
    }

    public void setStartTimePoint(TimePoint startTimePoint) {
        this.startTimePoint = startTimePoint;
    }

    public EventState getState() {
        return state;
    }

    public void setState(EventState state) {
        this.state = state;
    }
    
    @Override
    public int compareTo(EventQuickfinderDTO o) {
        if(state != o.state) {
            if(state == EventState.RUNNING) {
                return -1;
            }
            if(o.state == EventState.RUNNING) {
                return 1;
            }
            if(state == EventState.UPCOMING || state == EventState.PLANNED) {
                return -1;
            }
            if(o.state == EventState.UPCOMING || o.state == EventState.PLANNED) {
                return 1;
            }
        }
        return -startTimePoint.compareTo(o.startTimePoint);
    }
}
