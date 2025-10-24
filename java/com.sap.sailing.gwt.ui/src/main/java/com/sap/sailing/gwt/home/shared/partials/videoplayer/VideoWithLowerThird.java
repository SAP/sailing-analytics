package com.sap.sailing.gwt.home.shared.partials.videoplayer;

import java.util.Date;

import com.sap.sailing.gwt.home.communication.event.LabelType;
import com.sap.sailing.gwt.home.shared.partials.lowerthird.LowerThird;
import com.sap.sse.gwt.client.media.TakedownNoticeService;
import com.sap.sse.gwt.client.media.VideoDTO;

/**
 * Video player with a lower third containing title, subtitle and an optional {@link LabelType}.
 */
public class VideoWithLowerThird extends VideoPlayer {
    private final LowerThird lowerThird = new LowerThird();
    private String videoSourceRef;
    private Date videoCreatedAt;

    public VideoWithLowerThird(TakedownNoticeService takedownNoticeService, String eventName) {
        this(true, false, takedownNoticeService, eventName);
    }

    public VideoWithLowerThird(boolean fullHeightWidth, boolean autoplay, TakedownNoticeService takedownNoticeService, String eventName) {
        super(fullHeightWidth, autoplay, takedownNoticeService, eventName);
    }

    @Override
    protected void initialize() {
        lowerThird.setVisible(false);
        panel.add(lowerThird);
    }

    @Override
    protected void onPlay() {
        super.onPlay();
        lowerThird.setVisible(false);
    }

    public void setVideo(VideoDTO video) {
        super.setVideo(video);
        this.videoSourceRef = video.getSourceRef();
        this.videoCreatedAt = video.getCreatedAtDate();
        if (video.getTitle() != null && !video.getTitle().isEmpty()) {
            lowerThird.setVisible(paused());
            lowerThird.setData(video.getTitle(), video.getSubtitle(), LabelType.NONE);
        } else {
            lowerThird.setVisible(false);
        }
    }

    public boolean isVideoSet(VideoDTO video) {
        return video != null && video.getSourceRef().equals(videoSourceRef)
                && video.getCreatedAtDate().equals(videoCreatedAt);
    }
}
