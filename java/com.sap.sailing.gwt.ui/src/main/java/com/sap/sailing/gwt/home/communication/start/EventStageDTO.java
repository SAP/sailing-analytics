package com.sap.sailing.gwt.home.communication.start;

import java.util.Set;

import com.sap.sailing.gwt.home.communication.event.EventLinkAndMetadataDTO;
import com.sap.sailing.gwt.home.communication.eventview.TrackingConnectorInfoDTO;

public class EventStageDTO extends EventLinkAndMetadataDTO {

    private StageEventType stageType;
    private String stageImageURL;
    private Set<TrackingConnectorInfoDTO> trackingConnectorInfo;

    @Override
    public StageEventType getStageType() {
        return stageType;
    }

    public void setStageType(StageEventType stageType) {
        this.stageType = stageType;
    }

    public String getStageImageURL() {
        return stageImageURL;
    }

    public void setStageImageURL(String stageImageURL) {
        this.stageImageURL = stageImageURL;
    }

    public Set<TrackingConnectorInfoDTO> getTrackingConnectorInfos() {
        return trackingConnectorInfo;
    }

    public void setTrackingConnectorInfos(Set<TrackingConnectorInfoDTO> trackingConnectorInfo) {
        this.trackingConnectorInfo = trackingConnectorInfo;
    }
}
