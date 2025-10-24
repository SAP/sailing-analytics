package com.sap.sailing.gwt.home.desktop.partials.media;

import java.util.Collection;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO;
import com.sap.sailing.gwt.home.communication.media.MediaDTO;
import com.sap.sailing.gwt.home.desktop.partials.uploadpopup.DesktopMediaUploadPopup;
import com.sap.sailing.gwt.home.shared.partials.videoplayer.VideoWithLowerThird;
import com.sap.sailing.gwt.ui.client.SailingServiceHelper;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.media.GalleryImageHolder;
import com.sap.sailing.gwt.ui.client.media.VideoThumbnail;
import com.sap.sailing.gwt.ui.shared.ManageMediaModel;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.security.ui.authentication.AuthenticationContextEvent;
import com.sap.sse.security.ui.client.UserService;

/**
 * Desktop page to show videos and images as a gallery.
 */
public class MediaPage extends Composite {
    private static MediaPageUiBinder uiBinder = GWT.create(MediaPageUiBinder.class);

    private final ManageMediaModel manageMediaModel;

    interface MediaPageUiBinder extends UiBinder<Widget, MediaPage> {
    }

    @UiField
    SharedResources res;
    @UiField
    MediaPageResources local_res;
    @UiField
    DivElement addButtonArea;
    @UiField
    DivElement noContent;
    @UiField
    DivElement videoSectionUi;
    @UiField
    DivElement videoDisplayOuterBoxUi;
    @UiField
    DivElement videoListOuterBoxUi;
    @UiField
    FlowPanel videosListUi;
    @UiField
    SimplePanel videoDisplayHolderUi;
    @UiField
    DivElement photoSectionUi;
    @UiField
    FlowPanel photoListOuterBoxUi;
    @UiField
    Button videoSettingsButton;
    @UiField
    Button photoSettingsButton;
    @UiField
    Button mediaAddButton;

    private final SimplePanel contentPanel;
    private final FlowPanel popupHolder;
    private final StringMessages stringMessages;
    private boolean manageVideos;
    private boolean managePhotos;
    private VideoWithLowerThird videoDisplayUi;

    private final UserService userService;

    private final SailingServiceWriteAsync sailingServiceWrite;

    @UiHandler("videoSettingsButton")
    public void handleVideoSettingsButtonClick(ClickEvent e) {
        manageVideos = !manageVideos;
        setVideosManaged(manageVideos);
    }

    private void setVideosManaged(boolean managed) {
        if (videoSettingsButton != null) {
            if (managed) {
                videoSettingsButton.addStyleName(local_res.css().active());
            } else {
                videoSettingsButton.removeStyleName(local_res.css().active());
            }
        }
        if (videosListUi != null) {
            for (int i = 0; i < videosListUi.getWidgetCount(); i++) {
                if (videosListUi.getWidget(i) instanceof VideoThumbnail) {
                    final VideoThumbnail thumb = (VideoThumbnail) videosListUi.getWidget(i);
                    thumb.setManageable(managed);
                }
            }
        }
        updateVideoDisplay();
    }

    @UiHandler("photoSettingsButton")
    public void handlePhotoSettingsButtonClick(ClickEvent e) {
        managePhotos = !managePhotos;
        setPhotosManaged(managePhotos);
    }

    private void setPhotosManaged(boolean managed) {
        if (photoSettingsButton != null) {
            if (managed) {
                photoSettingsButton.addStyleName(local_res.css().active());
            } else {
                photoSettingsButton.removeStyleName(local_res.css().active());
            }
        }
        if (photoListOuterBoxUi != null) {
            for (int i = 0; i < photoListOuterBoxUi.getWidgetCount(); i++) {
                if (photoListOuterBoxUi.getWidget(i) instanceof GalleryImageHolder) {
                    GalleryImageHolder gih = (GalleryImageHolder) photoListOuterBoxUi.getWidget(i);
                    gih.setManageable(managed);
                }
            }
        }
    }

    @UiHandler("mediaAddButton")
    public void handleMediaAddButtonClick(ClickEvent e) {
        popupHolder.clear();
        final DesktopMediaUploadPopup popup = new DesktopMediaUploadPopup(
                (images, videos) -> manageMediaModel.addImagesAndVideos(images, videos, eventDto -> updateMedia(eventDto.getName())), sailingServiceWrite);
        popupHolder.add(popup);
        popup.center();
    }

    public MediaPage(IsWidget initialView, EventBus eventBus, UserService userService, EventViewDTO eventViewDto) {
        this.userService = userService;
        MediaPageResources.INSTANCE.css().ensureInjected();
        stringMessages = StringMessages.INSTANCE;
        this.sailingServiceWrite = SailingServiceHelper.createSailingServiceWriteInstance();
        manageMediaModel = new ManageMediaModel(sailingServiceWrite, userService, eventViewDto, stringMessages);
        contentPanel = new SimplePanel();
        contentPanel.setWidget(initialView);
        initWidget(contentPanel);
        popupHolder = new FlowPanel();
        eventBus.addHandler(AuthenticationContextEvent.TYPE, event -> {
            // for some reason this event is only send after logout. Never the less it will also handle login.
            setMediaManaged(manageMediaModel.hasPermissions());
        });
    }

    public void setMedia(final MediaDTO media, EventViewDTO eventDTO) {
        manageMediaModel.setMedia(media);
        updateMedia(eventDTO.getName());
    }

    private void updateMedia(String eventName) {
        contentPanel.setWidget(uiBinder.createAndBindUi(this));
        setMediaManaged(manageMediaModel.hasPermissions());
        int photosCount = manageMediaModel.getImages().size();
        photoListOuterBoxUi.clear();
        if (photosCount > 0) {
            photoSectionUi.getStyle().clearDisplay();
            String photoCss = null;
            // To make the image gallery look good, we use different styling if there are only few (<7) images
            // available. If there are more images, always 4 images are shown in a row.
            switch (photosCount) {
            case 1:
                photoCss = res.mediaCss().medium12();
                break;
            case 2:
                photoCss = res.mediaCss().medium6();
                break;
            case 3:
                photoCss = res.mediaCss().medium4();
                break;
            case 4:
                photoCss = res.mediaCss().medium6();
                break;
            case 5:
            case 6:
                photoCss = res.mediaCss().medium4();
                break;
            default:
                photoCss = res.mediaCss().medium3();
                break;
            }
            for (final ImageDTO holder : manageMediaModel.getImages()) {
                if (holder.getSourceRef() != null) {
                    GalleryImageHolder gih = new GalleryImageHolder(holder, getDeleteImageHandler(holder), userService, eventName);
                    gih.addStyleName(photoCss);
                    gih.addStyleName(res.mediaCss().columns());
                    photoListOuterBoxUi.add(gih);
                    gih.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            if (!managePhotos) {
                                Collection<SailingImageDTO> sailingImageDTOs = manageMediaModel.getImages().stream()
                                        .map(imageDto -> {
                                            if (imageDto instanceof SailingImageDTO) {
                                                return (SailingImageDTO) imageDto;
                                            }
                                            return new SailingImageDTO(null, imageDto);
                                        }).collect(Collectors.toList());
                                final SailingImageDTO showImage = sailingImageDTOs.stream()
                                        .filter(sailingImageDto -> sailingImageDto.compareTo(holder) == 0).findFirst()
                                        .orElse(new SailingImageDTO(null, holder));
                                new SailingFullscreenViewer(userService).show(showImage, sailingImageDTOs);
                            }
                        }
                    });
                }
            }
        }
        final int videoCount = manageMediaModel.getVideos().size();
        videosListUi.clear();
        if (videoCount > 0) {
            boolean first = true;
            for (final VideoDTO videoCandidateInfo : manageMediaModel.getVideos()) {
                if (first) {
                    putVideoOnDisplay(videoCandidateInfo, false);
                    first = false;
                }
                if (videoCount > 0) {
                    VideoThumbnail thumbnail = new VideoThumbnail(videoCandidateInfo,
                            eventName, getDeleteVideoHandler(videoCandidateInfo), /* edit handler */ null, this.userService);
                    thumbnail.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            if (!manageVideos) {
                                putVideoOnDisplay(videoCandidateInfo, true);
                            }
                        }
                    });
                    videosListUi.add(thumbnail);
                }
            }
        }
        if (photosCount == 0 && videoCount == 0) {
            noContent.getStyle().setDisplay(Display.BLOCK);
        }
        if (photosCount > 0) {
            photoSectionUi.getStyle().setDisplay(Display.BLOCK);
        }
        if (videoCount > 0) {
            videoSectionUi.getStyle().setDisplay(Display.BLOCK);
        }
    }

    private void updateVideoDisplay() {
        final int videoCount = manageMediaModel.getVideos().size();
        videoListOuterBoxUi.removeClassName(res.mediaCss().large3());
        videoListOuterBoxUi.getStyle().clearDisplay();
        videoDisplayOuterBoxUi.removeClassName(res.mediaCss().large9());
        videoDisplayOuterBoxUi.removeClassName(res.mediaCss().large12());
        if (videoCount == 1 && !manageVideos) {
            videoDisplayOuterBoxUi.addClassName(res.mediaCss().large12());
            videoListOuterBoxUi.getStyle().setDisplay(Display.NONE);
        } else if (videoCount > 1 || videoCount == 1 && manageVideos) {
            videoDisplayOuterBoxUi.addClassName(res.mediaCss().large9());
            videoListOuterBoxUi.addClassName(res.mediaCss().large3());
        }
    }

    private ClickHandler getDeleteVideoHandler(VideoDTO videoCandidateInfo) {
        return new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (Window.confirm(stringMessages.confirmDeleteVideo())) {
                    manageMediaModel.deleteVideo(videoCandidateInfo, eventDto -> updateMedia(eventDto.getName()));
                }
            }
        };
    }

    private ClickHandler getDeleteImageHandler(ImageDTO imageCandidateInfo) {
        return new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                event.stopPropagation();
                if (Window.confirm(stringMessages.confirmDeleteImage())) {
                    manageMediaModel.deleteImage(imageCandidateInfo, eventDto -> updateMedia(eventDto.getName()));
                }
            }
        };
    }

    /**
     * Shows a selected video in the big viewer.
     * 
     * @param video
     *            the video to show in the big viewer
     * @param autoplay
     *            true, if the video should play automatically, false otherwise
     */
    private void putVideoOnDisplay(final VideoDTO video, boolean autoplay) {
        videoDisplayUi = new VideoWithLowerThird(true, autoplay, userService, manageMediaModel.getEventName());
        videoDisplayUi.setVideo(video);
        try {
            videoDisplayHolderUi.setWidget(videoDisplayUi);
        } catch (Exception e) {
            final MimeType mimeType;
            if (video != null) {
                mimeType = video.getMimeType();
            } else {
                mimeType = MimeType.unknown;
            }
            GWT.log("Could not setup video player video with mime type: " + mimeType, e);
        }
    }

    private void setMediaManaged(boolean managed) {
        if (mediaAddButton != null) {
            mediaAddButton.setVisible(managed);
            photoSettingsButton.setVisible(managed);
            videoSettingsButton.setVisible(managed);
            managePhotos = false;
            setPhotosManaged(managePhotos);
            manageVideos = false;
            setVideosManaged(manageVideos);
        }
        updateVideoDisplay();
    }
}
