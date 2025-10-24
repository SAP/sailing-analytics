package com.sap.sailing.gwt.home.shared.partials.videoplayer;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.sap.sailing.gwt.home.desktop.partials.mainmedia.MainMedia.MutualExclusionPlayHandler;
import com.sap.sailing.gwt.ui.client.media.PauseEvent;
import com.sap.sailing.gwt.ui.client.media.PlayEvent;
import com.sap.sailing.gwt.ui.client.media.VideoJSPlayer;
import com.sap.sse.gwt.client.media.TakedownNoticeService;
import com.sap.sse.gwt.client.media.VideoDTO;

/**
 * Video player with custom play button.
 */
public class VideoPlayer extends Composite {
    private final VideoPlayerResources.LocalCss style = VideoPlayerResources.INSTANCE.css();
    private VideoJSPlayer videoJSPlayer;
    protected final FlowPanel panel;
    private final PlayButton playButton = new PlayButton();
    
    private boolean initialized = false;
    private final String eventName;
    
    public VideoPlayer(TakedownNoticeService takedownNoticeService, String eventName) {
        this(true, false, takedownNoticeService, eventName);
    }
    
    public VideoPlayer(MutualExclusionPlayHandler exclusionPlayer, TakedownNoticeService takedownNoticeService, String eventName) {
        this(takedownNoticeService, eventName);
        exclusionPlayer.register(videoJSPlayer);
    }
    
    public VideoPlayer(boolean fullHeightWidth, boolean autoplay, TakedownNoticeService takedownNoticeService, String eventName) {
        style.ensureInjected();
        this.eventName = eventName;
        panel = new FlowPanel();
        panel.addStyleName(style.videoPlayer());
        videoJSPlayer = new VideoJSPlayer(fullHeightWidth, autoplay, takedownNoticeService, "takedownRequestForEventGalleryVideo");
        videoJSPlayer.addPlayHandler(new PlayEvent.Handler() {
            @Override
            public void onStart(PlayEvent event) {
                onPlay();
            }
        });
        videoJSPlayer.addPauseHandler(new PauseEvent.Handler() {
            @Override
            public void onPause(PauseEvent event) {
                onPaused();
            }
        });
        panel.add(videoJSPlayer);
        playButton.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                videoJSPlayer.play();
                playButton.setVisible(false);
            }
        }, ClickEvent.getType());
        panel.add(playButton);
        initWidget(panel);
    }
    
    protected void initialize() {
    }
    
    protected void onPlay() {
        playButton.setVisible(false);
    }
    
    protected void onPaused() {
        playButton.setVisible(true);
    }

    public void setVideo(VideoDTO video) {
        if (!initialized) {
            initialize();
        }
        videoJSPlayer.setVideo(video.getMimeType(), video.getSourceRef(), eventName);
    }
    
    public boolean isFullscreen() {
        return videoJSPlayer.isFullscreen();
    }

    public boolean paused() {
        return videoJSPlayer.paused();
    }

    public void play() {
        videoJSPlayer.play();
    }
}
