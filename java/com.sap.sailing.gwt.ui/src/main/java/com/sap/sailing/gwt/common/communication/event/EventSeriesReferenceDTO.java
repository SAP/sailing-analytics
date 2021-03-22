package com.sap.sailing.gwt.common.communication.event;

import java.util.UUID;

import com.google.gwt.user.client.rpc.IsSerializable;

public class EventSeriesReferenceDTO implements IsSerializable {

    private String seriesDisplayName;
    private UUID seriesLeaderboardGroupId;

    protected EventSeriesReferenceDTO() {
    }

    public EventSeriesReferenceDTO(final String seriesDisplayName, final UUID seriesLeaderboardGroupId) {
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
