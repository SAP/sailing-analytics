package com.sap.sailing.gwt.ui.shared;

import java.util.Date;

import com.sap.sse.gwt.client.media.ImageDTO;

public class SailingImageDTO extends ImageDTO {
    
    private EventLinkDTO eventLink;

    @Deprecated
    protected SailingImageDTO() {
    }

    public SailingImageDTO(EventLinkDTO eventLink, String imageRef, Date createdAtDate) {
        super(imageRef, createdAtDate);
        this.eventLink = eventLink;
    }

    public SailingImageDTO(EventLinkDTO eventLink, ImageDTO imageDto) {
        super(imageDto.getSourceRef(), imageDto.getCreatedAtDate());
        setCopyright(imageDto.getCopyright());
        setLocale(imageDto.getLocale());
        setMimeType(imageDto.getMimeType());
        setSizeInPx(imageDto.getWidthInPx(), imageDto.getHeightInPx());
        setSubtitle(imageDto.getSubtitle());
        setTags(imageDto.getTags());
        setTitle(imageDto.getTitle());
        this.eventLink = eventLink;
    }

    public EventLinkDTO getEventLink() {
        return eventLink;
    }
}
