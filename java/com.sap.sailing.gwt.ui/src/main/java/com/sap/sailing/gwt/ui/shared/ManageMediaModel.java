package com.sap.sailing.gwt.ui.shared;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.security.SecuredDomainType.EventActions;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO;
import com.sap.sailing.gwt.home.communication.media.MediaDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.SailingVideoDTO;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.media.AbstractMediaDTO;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.UserService;

public class ManageMediaModel {

    private Logger logger = Logger.getLogger(getClass().getName());

    protected final SailingServiceWriteAsync sailingServiceWrite;
    protected final UserService userService;
    private final EventViewDTO eventViewDto;
    private final StringMessages i18n;
    private MediaDTO mediaDto;

    private Collection<SailingImageDTO> images = new LinkedHashSet<>();
    private Collection<VideoDTO> videos = new LinkedHashSet<>();

    public ManageMediaModel(SailingServiceWriteAsync sailingServiceWrite, UserService userService, 
            EventViewDTO eventViewDto, StringMessages i18n) {
        this.sailingServiceWrite = sailingServiceWrite;
        this.userService = userService;
        this.eventViewDto = eventViewDto;
        this.i18n = i18n;
    }

    private void setEventDto(EventDTO eventDto) {
        setVideos(eventDto.getVideos());
        setImages(eventDto.getImages());
    }

    public Collection<SailingImageDTO> getImages() {
        return images;
    }

    public Collection<VideoDTO> getVideos() {
        return videos;
    }
    
    public void setMedia(MediaDTO mediaDto) {
        this.mediaDto = mediaDto;
        setVideos(mediaDto.getVideos());
        setImages(mediaDto.getPhotos());
    }

    private void setVideos(Collection<? extends VideoDTO> videos) {
        this.videos = new LinkedHashSet<VideoDTO>(
                videos.stream()
                        .sorted(Comparator.comparing(AbstractMediaDTO::getCreatedAtDate).reversed())
                        .collect(Collectors.toList()));
    }

    private void setImages(Collection<SailingImageDTO> images) {
        this.images = new LinkedHashSet<>(
                images.stream().filter(video -> video.hasTag(MediaTagConstants.GALLERY.getName()))
                        .sorted(Comparator.comparing(AbstractMediaDTO::getCreatedAtDate).reversed())
                        .collect(Collectors.toList()));
    }

    public void deleteImage(ImageDTO imageDto, Consumer<EventDTO> callback) {
        loadEventData(eventDto -> {
            Collection<SailingImageDTO> toRemove = eventDto.getImages().stream()
                    .filter(image -> image.getSourceRef().equals(imageDto.getSourceRef())
                            && image.getCreatedAtDate().equals(imageDto.getCreatedAtDate()))
                    .collect(Collectors.toList());
            toRemove.forEach(eventDto::removeImage);
            updateEventDto(eventDto, callback);
            mediaDto.getPhotos().stream()
                    .filter(photo -> photo.getSourceRef().equals(imageDto.getSourceRef())
                            && photo.getCreatedAtDate().equals(imageDto.getCreatedAtDate()))
                    .forEach(photo -> mediaDto.removePhoto(photo));
        });
    }

    public void deleteVideo(VideoDTO videoDto, Consumer<EventDTO> callback) {
        loadEventData(eventDto -> {
            Collection<SailingVideoDTO> toRemove = eventDto.getVideos().stream()
                    .filter(video -> video.getSourceRef().equals(videoDto.getSourceRef())
                            && video.getCreatedAtDate().equals(videoDto.getCreatedAtDate()))
                    .collect(Collectors.toList());
            toRemove.forEach(eventDto::removeVideo);
            updateEventDto(eventDto, callback);
            mediaDto.getVideos().stream()
                    .filter(video -> video.getSourceRef().equals(videoDto.getSourceRef())
                            && video.getCreatedAtDate().equals(videoDto.getCreatedAtDate()))
                    .forEach(video -> mediaDto.removeVideo(video));
        });
    }

    public void addImages(List<SailingImageDTO> imageList, Consumer<EventDTO> callback) {
        loadEventData(eventDto -> {
            for (SailingImageDTO image: imageList) {
                eventDto.addImage(image);
                mediaDto.addPhoto(new SailingImageDTO(null, image));
            }
            updateEventDto(eventDto, callback);
        });
    }

    public void addVideos(List<SailingVideoDTO> videoList, Consumer<EventDTO> callback) {
        loadEventData(eventDto -> {
            for (SailingVideoDTO video: videoList) {
                eventDto.getVideos().add(video);
                mediaDto.addVideo(video);
            }
            updateEventDto(eventDto, callback);
        });
    }

    public void addImagesAndVideos(List<ImageDTO> imageList, List<VideoDTO> videoList, Consumer<EventDTO> callback) {
        loadEventData(eventDto -> {
            for (ImageDTO image : imageList) {
                mediaDto.addPhoto(eventDto.addImage(image));
            }
            for (VideoDTO video: videoList) {
                mediaDto.addVideo(eventDto.addVideo(video));
            }
            updateEventDto(eventDto, callback);
        });
    }

    public void reloadMedia(Consumer<EventDTO> callback) {
        loadEventData(eventDto -> {
            setVideos(eventDto.getVideos());
            setImages(eventDto.getImages());
            callback.accept(eventDto);
        });
    }

    private void loadEventData(Consumer<EventDTO> callback) {
        sailingServiceWrite.getEventById(eventViewDto.getId(), true, new AsyncCallback<EventDTO>() {
            @Override
            public void onSuccess(EventDTO eventDto) {
                callback.accept(eventDto);
            }

            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(i18n.errorWhileUpdatingEvent(), NotificationType.ERROR);
                logger.log(Level.SEVERE, "Cannot update event.", caught);
            }
        });
    }

    private void updateEventDto(EventDTO eventDto, Consumer<EventDTO> callback) {
        if (hasPermissions()) {
            sailingServiceWrite.updateEvent(eventDto, new AsyncCallback<EventDTO>() {
                @Override
                public void onSuccess(EventDTO eventDto) {
                    setEventDto(eventDto);
                    callback.accept(eventDto);
                }

                @Override
                public void onFailure(Throwable caught) {
                    Notification.notify(i18n.error(), NotificationType.ERROR);
                    logger.log(Level.SEVERE, "Cannot update event. Video not added.", caught);
                }
            });
        }
    }

    /**
     * Check permission on default object (eventViewDTO from init).
     */
    public boolean hasPermissions() {
        return hasEventMediaPermissions(eventViewDto, userService);
    }

    /**
     * Check permission on current EventDTO.
     */
    public boolean hasPermissions(EventDTO eventDto) {
        return hasEventMediaPermissions(eventDto, userService);
    }
    
    public String getEventName() {
        return eventViewDto.getName();
    }
    
    public static boolean hasEventMediaPermissions(SecuredDTO securedDTO, UserService userService) {
        final boolean hasPermission;
        if (userService.hasPermission(securedDTO, HasPermissions.DefaultActions.UPDATE) || userService.hasPermission(securedDTO, EventActions.UPLOAD_MEDIA)) {
            hasPermission = true;
        } else {
            hasPermission = false;
        }
        return hasPermission;
    }
}
