package com.sap.sailing.gwt.home.shared.places.fakeseries;

import java.util.UUID;

import com.google.gwt.core.client.GWT;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.EventSeriesAnalyticsDataManager;
import com.sap.sailing.gwt.home.shared.places.ShareablePlaceContext;

/**
 * Common context used by the different tabs in the series place.
 * 
 */
public class SeriesContext implements ShareablePlaceContext {
    private final UUID seriesId;
    private UUID leaderboardGroupId;
    
    private EventSeriesAnalyticsDataManager analyticsManager;

    public static final SeriesContext createWithSeriesId(UUID seriesId) {
        return new SeriesContext(seriesId, null);
    }
    
    public static final SeriesContext createWithLeaderboardGroupId(UUID leaderboardGroupId) {
        return new SeriesContext(null, leaderboardGroupId);
    }
    
    public static final SeriesContext createErrorContext() {
        return new SeriesContext(null, null);
    }

    private SeriesContext(UUID seriesId, UUID leaderboardGroupId) {
        this.seriesId = seriesId;
        this.leaderboardGroupId = leaderboardGroupId;
    }

    public SeriesContext(SeriesContext ctx) {
        this.seriesId = ctx.seriesId;
        this.leaderboardGroupId = ctx.leaderboardGroupId;
        withAnalyticsManager(ctx.analyticsManager);
    }
    
    public UUID getLeaderboardGroupId() {
        return leaderboardGroupId;
    }

    public UUID getSeriesId() {
        if(leaderboardGroupId != null) {
            GWT.log("Access to seriesid! Use leaderboardgroupid instead"
                    + " seriesId:" + seriesId + "; lid: " + leaderboardGroupId);
        }
        return seriesId;
    }
    
    public EventSeriesAnalyticsDataManager getAnalyticsManager() {
        return analyticsManager;
    }

    public SeriesContext withAnalyticsManager(EventSeriesAnalyticsDataManager regattaAnalyticsManager) {
        this.analyticsManager = regattaAnalyticsManager;
        return this;
    }

    public void updateLeaderboardGroupId(UUID leaderboardGroupUUID) {
        this.leaderboardGroupId = leaderboardGroupUUID;
    }
    
    @Override
    public String getContextAsPathParameters() {
        if(leaderboardGroupId != null) {
            return "/series/" + leaderboardGroupId;
        }else {
            return null;
        }
    }
}
