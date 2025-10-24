package com.sap.sailing.gwt.home.shared.partials.video;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewVideoStageDTO;
import com.sap.sailing.gwt.home.shared.partials.videoplayer.VideoWithLowerThird;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class Video extends Composite {

    private static LivestreamUiBinder uiBinder = GWT.create(LivestreamUiBinder.class);

    interface LivestreamUiBinder extends UiBinder<Widget, Video> {
    }
    
    private String source;
    @UiField(provided = true) VideoWithLowerThird videoPlayer;

    public Video(TakedownNoticeService takedownNoticeService, String eventName) {
        VideoResources.INSTANCE.css().ensureInjected();
        videoPlayer = new VideoWithLowerThird(false, false, takedownNoticeService, eventName);
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void setData(EventOverviewVideoStageDTO data) {
        source = data.getVideo().getSourceRef();
        videoPlayer.setVideo(data.getVideo());
    }
    
    public boolean shouldBeReplaced(String newSource) {
        return (videoPlayer == null || source == null || (!source.equals(newSource) && canCurrentVideoBeReplaced()));
    }
    
    private boolean canCurrentVideoBeReplaced() {
        return !videoPlayer.isFullscreen() && videoPlayer.paused();
    }
}
