package com.sap.sailing.gwt.home.client.shared.media;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.client.shared.placeholder.InfoPlaceholder;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.media.GalleryImageHolder;
import com.sap.sailing.gwt.ui.client.media.VideoJSPlayer;
import com.sap.sailing.gwt.ui.client.media.VideoThumbnail;
import com.sap.sailing.gwt.ui.shared.media.ImageMetadataDTO;
import com.sap.sailing.gwt.ui.shared.media.MediaDTO;
import com.sap.sailing.gwt.ui.shared.media.VideoMetadataDTO;

public class MediaPage extends Composite {
    private static MediaPageUiBinder uiBinder = GWT.create(MediaPageUiBinder.class);

    interface MediaPageUiBinder extends UiBinder<Widget, MediaPage> {
    }

    @UiField
    SharedResources res;
    @UiField
    MediaPageResources local_res;
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
    StringMessages i18n;
    private final SimplePanel contentPanel;
    private VideoJSPlayer videoDisplayUi;

    public MediaPage() {
        MediaPageResources.INSTANCE.css().ensureInjected();
        contentPanel = new SimplePanel();
        initWidget(contentPanel);
    }

    public void setMedia(MediaDTO media) {
        Widget mediaUi = uiBinder.createAndBindUi(this);
        int photosCount = media.getPhotos().size();
        if (photosCount > 0) {
            photoSectionUi.getStyle().clearDisplay();
            String photoCss = null;

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

            for (ImageMetadataDTO holder : media.getPhotos()) {
                if (holder.getSourceRef() != null) {

                    GalleryImageHolder gih = new GalleryImageHolder(holder);
                    gih.addStyleName(photoCss);
                    gih.addStyleName(res.mediaCss().columns());

                    photoListOuterBoxUi.add(gih);
                }
            }
        }
        int videoCount = media.getVideos().size();
        if (videoCount > 0) {
            videoSectionUi.getStyle().clearDisplay();
            if (videoCount == 1) {
                videoDisplayOuterBoxUi.addClassName(res.mediaCss().large12());
                videoListOuterBoxUi.getStyle().setDisplay(Display.NONE);
            } else if (videoCount > 1) {
                videoDisplayOuterBoxUi.addClassName(res.mediaCss().large9());
                videoListOuterBoxUi.addClassName(res.mediaCss().large3());
            }
            boolean first = true;
            for (final VideoMetadataDTO videoCandidateInfo : media.getVideos()) {
                if (first) {
                    putVideoOnDisplay(videoCandidateInfo, false);
                    first = false;
                }
                if (videoCount > 1) {
                    VideoThumbnail thumbnail = new VideoThumbnail(videoCandidateInfo);
                    thumbnail.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            putVideoOnDisplay(videoCandidateInfo, true);
                        }
                    });
                    videosListUi.add(thumbnail);
                }
            }
        }
        if (photosCount == 0 && videoCount == 0) {
            contentPanel.setWidget(new InfoPlaceholder(i18n.mediaNoContent()));
        } else {
            contentPanel.setWidget(mediaUi);
        }
    }

    private void putVideoOnDisplay(final VideoMetadataDTO videoCandidateInfo, boolean autoplay) {
        videoDisplayUi = new VideoJSPlayer(true, autoplay);
        videoDisplayUi.setSource(videoCandidateInfo.getSourceRef(), videoCandidateInfo.getMimeType());
        videoDisplayHolderUi.setWidget(videoDisplayUi);
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                if (videoCandidateInfo.getThumbnailRef() == null) {
                    GWT.log(videoDisplayUi.getThumbnailData());
                }
            }
        });
    }
}
