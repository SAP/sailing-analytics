package com.sap.sailing.gwt.home.communication.event;

import com.sap.sailing.gwt.common.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.common.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.start.StageEventType;

public class EventLinkAndMetadataDTO extends EventMetadataDTO {

    private static final long serialVersionUID = 6512995854737456123L;

    private String baseURL;
    private boolean isOnRemoteServer;
    
    public StageEventType getStageType() {
        return getState() == EventState.RUNNING ? StageEventType.RUNNING : StageEventType.POPULAR;
    }

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

}
