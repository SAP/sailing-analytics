package com.sap.sailing.gwt.home.shared.places.fakeseries;

import com.sap.sailing.gwt.home.communication.fakeseries.EventSeriesViewDTO;
import com.sap.sailing.gwt.home.communication.media.MediaDTO;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.EventSeriesAnalyticsDataManager;

/**
 * Common context used by the different tabs in the series place.
 * 
 */
public class SeriesContext {

    private String seriesId;
    private EventSeriesViewDTO seriesDTO;
    private MediaDTO media;
    private EventSeriesAnalyticsDataManager analyticsManager;

    public SeriesContext() {
    }

    public SeriesContext(SeriesContext ctx) {
        updateContext(ctx.getSeriesDTO());
        withMedia(ctx.media);
        withAnalyticsManager(ctx.analyticsManager);
    }

    public SeriesContext withId(String eventId) {
        this.seriesId = eventId;
        return this;
    }

    /**
     * Used to update context with dto instance
     * 
     * @param dto
     * @return
     */
    public SeriesContext updateContext(EventSeriesViewDTO dto) {
        this.seriesDTO = dto;
        if (seriesDTO == null) {
            withId(null);
        } else {
            withId(dto.getId().toString());
        }
        return this;
    }

    public EventSeriesViewDTO getSeriesDTO() {
        return seriesDTO;
    }

    public String getSeriesId() {
        return seriesId;
    }
    
    public MediaDTO getMedia() {
        return media;
    }

    public SeriesContext withMedia(MediaDTO media) {
        this.media = media;
        return this;
    }
    
    public EventSeriesAnalyticsDataManager getAnalyticsManager() {
        return analyticsManager;
    }

    public SeriesContext withAnalyticsManager(EventSeriesAnalyticsDataManager regattaAnalyticsManager) {
        this.analyticsManager = regattaAnalyticsManager;
        return this;
    }
}
