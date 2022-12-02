package com.sap.sailing.gwt.home.mobile.places.event.media;

import java.util.Collection;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.media.MediaDTO;
import com.sap.sailing.gwt.home.desktop.partials.media.MediaPageResources;
import com.sap.sailing.gwt.home.mobile.partials.imagegallery.ImageGallery;
import com.sap.sailing.gwt.home.mobile.partials.uploadpopup.MobileMediaUploadPopup;
import com.sap.sailing.gwt.home.mobile.partials.videogallery.VideoGallery;
import com.sap.sailing.gwt.home.mobile.places.event.AbstractEventView;
import com.sap.sailing.gwt.ui.client.SailingServiceHelper;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.ManageMediaModel;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.security.ui.authentication.AuthenticationContextEvent;
import com.sap.sse.security.ui.client.UserService;

public class MediaViewImpl extends AbstractEventView<MediaView.Presenter> implements MediaView {

    private Logger logger = Logger.getLogger(getClass().getName());

    private static MediaViewImplUiBinder uiBinder = GWT.create(MediaViewImplUiBinder.class);

    interface MediaViewImplUiBinder extends UiBinder<Widget, MediaViewImpl> {
    }

    @UiField
    Label noContentInfoUi;
    @UiField
    VideoGallery videoGalleryUi;
    @UiField
    ImageGallery imageGalleryUi;
    @UiField
    Button addMediaButtonUi;

    private MobileMediaUploadPopup mobileMediaUploadPopup;
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final UserService userService;
    private final ManageMediaModel manageMediaModel;

    public MediaViewImpl(MediaView.Presenter presenter) {
        super(presenter, false, true, false);
        this.sailingServiceWrite = SailingServiceHelper.createSailingServiceWriteInstance();
        this.userService = presenter.getUserService();
        final StringMessages stringMessages = StringMessages.INSTANCE;
        this.manageMediaModel = new ManageMediaModel(sailingServiceWrite, userService, presenter.getEventDTO(),
                stringMessages);
        MediaViewResources.INSTANCE.css().ensureInjected();
        setViewContent(uiBinder.createAndBindUi(this));
        MediaPageResources.INSTANCE.css().ensureInjected();
        mobileMediaUploadPopup = new MobileMediaUploadPopup(videos -> {
            manageMediaModel.addVideos(videos, eventDto -> updateMedia());
        }, image -> {
            manageMediaModel.addImages(image, eventDto -> updateMedia());
        });
        addMediaButtonUi.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                mobileMediaUploadPopup.show();
                mobileMediaUploadPopup.openFileUpload();
            }
        });
        presenter.getEventBus().addHandler(AuthenticationContextEvent.TYPE, event -> {
            logger.info("Sign out");
            // for some reason this event is only send after logout. Never the less it will also handle login.
            setMediaManaged(manageMediaModel.hasPermissions());
        });
    }

    @Override
    public void setMedia(MediaDTO media) {
        manageMediaModel.setMedia(media);
        updateMedia();
    }

    public void updateMedia() {
        setMediaManaged(manageMediaModel.hasPermissions());
        Collection<ImageDTO> images = manageMediaModel.getImages();
        Collection<VideoDTO> videos = manageMediaModel.getVideos();
        noContentInfoUi.setVisible(videos.isEmpty() && images.isEmpty());
        videoGalleryUi.setVideos(videos, video -> deleteVideo(video));
        videoGalleryUi.setVisible(!videos.isEmpty());
        imageGalleryUi.setImages(images, image -> deleteImage(image));
        imageGalleryUi.setVisible(!images.isEmpty());
        imageGalleryUi.setMediaManaged(false);
        videoGalleryUi.setMediaManaged(false);
    }

    private void setMediaManaged(boolean managed) {
        logger.info("Set manage media to: " + managed);
        addMediaButtonUi.setVisible(managed);
        videoGalleryUi.setManageButtonsVisible(managed);
        imageGalleryUi.setManageButtonsVisible(managed);
        if (!managed) {
            videoGalleryUi.setMediaManaged(managed);
            imageGalleryUi.setMediaManaged(managed);
        }
    }

    private void deleteImage(ImageDTO imageDto) {
        manageMediaModel.deleteImage(imageDto, eventDto -> updateMedia());
    }

    private void deleteVideo(VideoDTO videoDto) {
        manageMediaModel.deleteVideo(videoDto, eventDto -> updateMedia());
    }

}
