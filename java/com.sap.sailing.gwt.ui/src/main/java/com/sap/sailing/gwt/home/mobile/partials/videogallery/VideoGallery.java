package com.sap.sailing.gwt.home.mobile.partials.videogallery;

import java.util.Collection;
import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.sap.sailing.gwt.home.mobile.partials.section.MobileSection;
import com.sap.sailing.gwt.home.mobile.partials.sectionHeader.SectionHeaderContent;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.media.TakedownNoticeService;
import com.sap.sse.gwt.client.media.VideoDTO;

public class VideoGallery extends Composite {

    private static VideoGalleryUiBinder uiBinder = GWT.create(VideoGalleryUiBinder.class);

    private boolean managed;

    interface VideoGalleryUiBinder extends UiBinder<MobileSection, VideoGallery> {
    }
    
    @UiField SectionHeaderContent sectionHeaderUi;
    private final MobileSection mobileSection;
    
    public VideoGallery() {
        VideoGalleryResources.INSTANCE.css().ensureInjected();
        initWidget(mobileSection = uiBinder.createAndBindUi(this));
        sectionHeaderUi.setSectionTitle(StringMessages.INSTANCE.videos());
        sectionHeaderUi.initCollapsibility(mobileSection.getContentContainerElement(), true);
        sectionHeaderUi.addManageButtonClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                setMediaManaged(!managed);
                event.stopPropagation();
            }
        });
    }

    public void setVideos(Collection<? extends VideoDTO> videos, Consumer<VideoDTO> deleteVideo, TakedownNoticeService takedownNoticeService, String eventName) {
        sectionHeaderUi.setInfoText(StringMessages.INSTANCE.videosCount(videos.size()));
        mobileSection.clearContent();
        for (VideoDTO video : videos) {
            mobileSection.addContent(new VideoGalleryVideo(video, deleteVideo, takedownNoticeService, eventName));
        }
    }

    public void setManageButtonsVisible(boolean visible) {
        sectionHeaderUi.setManageButtonVisible(visible);
    }

    public void setMediaManaged(boolean managed) {
        this.managed = managed;
        for (int i = 0; i < mobileSection.getWidgetCount(); i++) {
            if (mobileSection.getWidget(i) instanceof VideoGalleryVideo) {
                VideoGalleryVideo item = (VideoGalleryVideo) mobileSection.getWidget(i);
                item.manageMedia(managed);
            }
        }
        sectionHeaderUi.setManageButtonActive(managed);
    }
}
