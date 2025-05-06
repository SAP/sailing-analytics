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
    
    public VideoPlayer(TakedownNoticeService takedownNoticeService) {
        this(true, false, takedownNoticeService);
    }
    
    public VideoPlayer(MutualExclusionPlayHandler exclusionPlayer, TakedownNoticeService takedownNoticeService) {
        this(takedownNoticeService);
        exclusionPlayer.register(videoJSPlayer);
    }
    
    public VideoPlayer(boolean fullHeightWidth, boolean autoplay, TakedownNoticeService takedownNoticeService) {
        style.ensureInjected();
        panel = new FlowPanel();
        panel.addStyleName(style.videoPlayer());
        videoJSPlayer = new VideoJSPlayer(fullHeightWidth, autoplay, takedownNoticeService);
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
        if(!initialized) {
            initialize();
        }
        videoJSPlayer.setVideo(video.getMimeType(), video.getSourceRef());
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
