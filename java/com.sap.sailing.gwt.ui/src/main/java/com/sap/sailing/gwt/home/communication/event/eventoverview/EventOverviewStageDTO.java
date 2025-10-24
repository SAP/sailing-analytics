package com.sap.sailing.gwt.home.communication.event.eventoverview;

import com.sap.sse.gwt.shared.DTO;

public class EventOverviewStageDTO implements DTO {

    private String eventMessage;
    private EventOverviewStageContentDTO stageContent;

    @SuppressWarnings("unused")
    private EventOverviewStageDTO() {
    }

    public EventOverviewStageDTO(String eventMessage, EventOverviewStageContentDTO stageContent) {
        super();
        this.eventMessage = eventMessage;
        this.stageContent = stageContent;
    }

    public String getEventMessage() {
        return eventMessage;
    }
    
    public EventOverviewStageContentDTO getStageContent() {
        return stageContent;
    }
}
