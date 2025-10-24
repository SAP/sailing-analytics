package com.sap.sailing.gwt.home.communication.media;

import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sailing.gwt.ui.client.shared.SailingVideoDTO;
import com.sap.sailing.gwt.ui.shared.EventLinkDTO;
import com.sap.sailing.gwt.ui.shared.EventReferenceDTO;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.gwt.dispatch.shared.caching.IsClientCacheable;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;

/**
 * <p>
 * {@link SailingAction} implementation to load images and videos to be shown on the media page for the
 * {@link #GetMediaForEventAction(UUID) given event-id}, preparing the appropriate data structure.
 * </p>
 */
public class GetMediaForEventAction implements SailingAction<MediaDTO>, IsClientCacheable {
    
    private UUID eventId;
    
    @SuppressWarnings("unused")
    private GetMediaForEventAction() {
    }

    /**
     * Creates a {@link GetMediaForEventAction} instance for the given event-id.
     * 
     * @param eventId
     *            {@link UUID} of the event to load media for
     */
    public GetMediaForEventAction(UUID eventId) {
        this.eventId = eventId;
    }

    @Override
    @GwtIncompatible
    public MediaDTO execute(SailingDispatchContext ctx) throws DispatchException {
        Event event = ctx.getRacingEventService().getEvent(eventId);
        EventLinkDTO eventLink = HomeServiceUtil.convertToEventLinkDTO(event, event.getBaseURL(), /* on remote server */ false);
        EventReferenceDTO eventRef = new EventReferenceDTO(event);

        String eventName = event.getName();
        MediaDTO media = new MediaDTO();
        for (ImageDescriptor image : HomeServiceUtil.getPhotoGalleryImages(event)) {
            SailingImageDTO imageDTO = new SailingImageDTO(eventLink, image.getURL().toString(),
                    image.getCreatedAtDate().asDate());
            imageDTO.setSizeInPx(image.getWidthInPx(), image.getHeightInPx());
            imageDTO.setTitle(image.getTitle() != null ? image.getTitle(): eventName);
            imageDTO.setSubtitle(image.getSubtitle());
            imageDTO.setTags(image.getTags());
            imageDTO.setCopyright(image.getCopyright());
            imageDTO.setLocale(image.getLocale() != null ? image.getLocale().toString() : null);
            media.addPhoto(imageDTO);
        }
        for (VideoDescriptor video : event.getVideos()) {
            MimeType type = video.getMimeType();
            if (MediaTagConstants.SUPPORTED_VIDEO_TYPES.contains(type)) {
                SailingVideoDTO videoDTO = HomeServiceUtil.toSailingVideoDTO(eventRef, video);
                media.addVideo(videoDTO);
            }
        }
        return media;
    }

    @Override
    public void cacheInstanceKey(StringBuilder key) {
        key.append(eventId);
    }

}
