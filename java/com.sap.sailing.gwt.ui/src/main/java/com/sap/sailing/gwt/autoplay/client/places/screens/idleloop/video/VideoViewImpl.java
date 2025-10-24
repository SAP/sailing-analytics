package com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.video;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.media.VideoJSPlayer;
import com.sap.sse.gwt.client.media.VideoDTO;

public class VideoViewImpl extends Composite implements VideoView {

    private static final int WAIT_FOR_VIDEO_LOAD_MAX_TIME = 5;
    private static final int MAX_VIDEO_DURATION = 120;

    private static IdleNextUpViewImplUiBinder uiBinder = GWT.create(IdleNextUpViewImplUiBinder.class);

    @UiField
    SimplePanel mainPanelUi;

    private VideoPresenter currentPresenter;

    private VideoJSPlayer player;

    interface IdleNextUpViewImplUiBinder extends UiBinder<Widget, VideoViewImpl> {
    }

    public VideoViewImpl() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void startingWith(VideoPresenter currentPresenter, AcceptsOneWidget panel) {
        this.currentPresenter = currentPresenter;
        panel.setWidget(this);
    }
    
    @Override
    protected void onDetach() {
        super.onDetach();
        player.pause();
        player.disposeIf2D();
        player.removeFromParent();
    }

    @Override
    public void playVideo(VideoDTO video) {
        player = new VideoJSPlayer(true, true, currentPresenter.getUserService(), "takedownRequestForEventGalleryVideo");
        player.setVideo(video.getMimeType(), video.getSourceRef(), currentPresenter.getEventDTO().getName());
        currentPresenter.publishDuration(WAIT_FOR_VIDEO_LOAD_MAX_TIME);
        RepeatingCommand durationDeterminator = new RepeatingCommand() {
            @Override
            public boolean execute() {
                if (player.isVisible() && mainPanelUi.isAttached()) {
                    int duration = player.getDuration();
                    if (duration > 0) {
                        // prevent autoplay from only playing videos, if a very long video is tagged, adjust if
                        // necessary
                        if (duration > MAX_VIDEO_DURATION) {
                            duration = MAX_VIDEO_DURATION;
                        }
                        currentPresenter.publishDuration(duration);
                        player.setCurrentTime(0);
                        player.play();
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        };
        Scheduler.get().scheduleFixedDelay(durationDeterminator, 1000);
        mainPanelUi.clear();
        mainPanelUi.add(player);
    }

}
