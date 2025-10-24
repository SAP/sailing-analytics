package com.sap.sailing.gwt.home.communication.event;

import java.util.UUID;

import com.sap.sse.gwt.shared.DTO;

public class EventSeriesReferenceDTO implements DTO {

    private String seriesDisplayName;
    private UUID seriesLeaderboardGroupId;

    protected EventSeriesReferenceDTO() {
    }

    public EventSeriesReferenceDTO(String seriesDisplayName, UUID seriesLeaderboardGroupId) {
        super();
        this.seriesDisplayName = seriesDisplayName;
        this.seriesLeaderboardGroupId = seriesLeaderboardGroupId;
    }

    public String getSeriesDisplayName() {
        return seriesDisplayName;
    }

    public UUID getSeriesLeaderboardGroupId() {
        return seriesLeaderboardGroupId;
    }

}
