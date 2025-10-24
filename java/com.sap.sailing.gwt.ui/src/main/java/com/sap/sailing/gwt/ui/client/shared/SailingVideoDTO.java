package com.sap.sailing.gwt.ui.client.shared;

import java.util.Date;

import com.sap.sailing.gwt.ui.shared.EventReferenceDTO;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.gwt.client.media.AbstractMediaDTO;
import com.sap.sse.gwt.client.media.VideoDTO;

public class SailingVideoDTO extends VideoDTO {
    
    private EventReferenceDTO eventRef;

    /** for GWT */
    @Deprecated
    protected SailingVideoDTO() {
    }

    public SailingVideoDTO(EventReferenceDTO eventRef, String srcReference, MimeType mimeType, Date createdAtDate) {
        super(srcReference, mimeType, createdAtDate);
        this.eventRef = eventRef;
    }

    public SailingVideoDTO(EventReferenceDTO eventRef, VideoDTO videoDto) {
        super(videoDto.getSourceRef(), videoDto.getMimeType(), videoDto.getCreatedAtDate());
        setCopyright(videoDto.getCopyright());
        setLengthInSeconds(videoDto.getLengthInSeconds());
        setLocale(videoDto.getLocale());
        setSubtitle(videoDto.getSubtitle());
        setTags(videoDto.getTags());
        setThumbnailRef(videoDto.getThumbnailRef());
        setTitle(videoDto.getTitle());
        this.eventRef = eventRef;
    }

    public EventReferenceDTO getEventRef() {
        return eventRef;
    }
    
    @Override
    public int compareTo(AbstractMediaDTO o) {
        int createdAtDateComp = compareToByCreatedAtDate(o); 
        return createdAtDateComp == 0 ? getSourceRef().compareTo(o.getSourceRef()) : createdAtDateComp;
    }
}
