package com.sap.sailing.gwt.home.communication.eventview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.dto.EventType;
import com.sap.sailing.gwt.home.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.home.communication.event.EventReferenceWithStateDTO;
import com.sap.sailing.gwt.home.communication.event.HasLogo;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.dispatch.shared.commands.Result;

public class EventViewDTO extends EventMetadataDTO implements Result, HasLogo {
    private TreeSet<RegattaMetadataDTO> regattas = new TreeSet<>();
    private ArrayList<EventReferenceWithStateDTO> eventsOfSeries = new ArrayList<>();
    
    private EventType type;
    private boolean hasMedia;
    private boolean hasAnalytics;
    private String seriesName;
    private ImageDTO logoImage;
    private String officialWebsiteURL;
    private String sailorsInfoWebsiteURL;
    private String description;
    private List<String> allWindFinderReviewedSpotsCollectionIdsUsedByEvent;

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Collection<RegattaMetadataDTO> getRegattas() {
        return regattas;
    }

    public void addEventToSeries(EventReferenceWithStateDTO eventRef) {
        eventsOfSeries.add(eventRef);
    }

    public List<EventReferenceWithStateDTO> getEventsOfSeriesSorted() {
        return eventsOfSeries;
    }

    public String getVenueCountry() {
        // FIXME: We need a country?
        return "";
    }
    
    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getSeriesName() {
        return seriesName;
    }
    public String getSeriesIdAsString() {
        return getId().toString();
    }

    public boolean isHasMedia() {
        return hasMedia;
    }

    public void setHasMedia(boolean hasMedia) {
        this.hasMedia = hasMedia;
    }

    public boolean isHasAnalytics() {
        return hasAnalytics;
    }

    public void setHasAnalytics(boolean hasAnalytics) {
        this.hasAnalytics = hasAnalytics;
    }

    public boolean isRegattaIDKnown(String regattaId) {
        for (RegattaMetadataDTO regatta : regattas) {
            if(regatta.getId().equals(regattaId)) {
                return true;
            }
        }
        return false;
    }

    public void setLogoImage(ImageDTO logoImage) {
        this.logoImage = logoImage;
    }
    
    public ImageDTO getLogoImage() {
        return logoImage;
    }

    public void setOfficialWebsiteURL(String officialWebsiteURL) {
        this.officialWebsiteURL = officialWebsiteURL;
    }
    
    public String getOfficialWebsiteURL() {
        return officialWebsiteURL;
    }

    public String getSailorsInfoWebsiteURL() {
        return sailorsInfoWebsiteURL;
    }

    public void setSailorsInfoWebsiteURL(String sailorsInfoWebsiteURL) {
        this.sailorsInfoWebsiteURL = sailorsInfoWebsiteURL;
    }
    
    /**
     * In addition to the wind finder spot collections specified by this event explicitly (see
     * {@link #getWindFinderReviewedSpotsCollectionIds()}), this method may return additional spot collection IDs based
     * on the tracked races reachable from this event's associated leaderboard groups and their wind sources. The
     * {@link WindSource#getId() wind source IDs} of all wind sources of type {@link WindSourceType#WINDFINDER} will be
     * collected and returned.
     */
    public Iterable<String> getAllWindFinderReviewedSpotsCollectionIdsUsedByEvent() {
        final Iterable<String> result;
        if (allWindFinderReviewedSpotsCollectionIdsUsedByEvent == null) {
            result = Collections.emptySet();
        } else {
            result = allWindFinderReviewedSpotsCollectionIdsUsedByEvent;
        }
        return result;
    }
    
    public void setAllWindFinderReviewedSpotsCollectionIdsUsedByEvent(Iterable<String> allWindFinderReviewedSpotsCollectionIdsUsedByEvent) {
        this.allWindFinderReviewedSpotsCollectionIdsUsedByEvent = new ArrayList<>();
        if (allWindFinderReviewedSpotsCollectionIdsUsedByEvent != null) {
            Util.addAll(allWindFinderReviewedSpotsCollectionIdsUsedByEvent, this.allWindFinderReviewedSpotsCollectionIdsUsedByEvent);
        }
    }

    public String getLocationAndVenueAndCountry() {
        String venue = getLocationAndVenue();
        if(getVenueCountry() != null && !getVenueCountry().isEmpty()) {
            return venue + ", " + getVenueCountry();
        }
        return venue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
