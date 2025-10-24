package com.sap.sailing.gwt.ui.client.media;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SourceElement;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.media.client.Audio;
import com.google.gwt.media.client.Video;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.media.MediaSubType;
import com.sap.sse.common.media.MediaType;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

/**
 * video.js (http://videojs.com/) wrapper as GWT widget.
 */
public class VideoJSPlayer extends Composite implements RequiresResize {
    private static VideoJSPlayerUiBinder uiBinder = GWT.create(VideoJSPlayerUiBinder.class);
    private static final int RESIZE_CHECK = 250;

    interface VideoJSPlayerUiBinder extends UiBinder<Widget, VideoJSPlayer> {
    }

    interface VideoJSStyle extends CssResource {
        String invertedVideoPlayer();

        String player();
    }

    @UiField
    VideoJSStyle style;

    @UiField
    HTMLPanel playerHolder;
    
    @UiField(provided=true)
    MediaMenuIcon videoPlayerMenuButton;

    private final boolean fullHeightWidth;
    private final Timer resizeChecker = new Timer() {
        @Override
        public void run() {
            VideoJSPlayer.this.onResize();
        }
    };

    private String elementId;
    private JavaScriptObject player;

    private boolean autoplay;
    private boolean panorama;
    private boolean controls = true;

    public VideoJSPlayer(boolean fullHeightWidth, boolean autoplay, TakedownNoticeService takedownNoticeService, String takedownNoticeMessageKey) {
        SharedResources.INSTANCE.mainCss().ensureInjected();
        this.autoplay = autoplay;
        this.fullHeightWidth = fullHeightWidth;
        videoPlayerMenuButton = new MediaMenuIcon(takedownNoticeService, takedownNoticeMessageKey);
        initWidget(uiBinder.createAndBindUi(this));
    }

    public HandlerRegistration addPlayHandler(PlayEvent.Handler handler) {
        return addHandler(handler, PlayEvent.getType());
    }

    public HandlerRegistration addPauseHandler(PauseEvent.Handler handler) {
        return addHandler(handler, PauseEvent.getType());
    }

    public void setVideo(MimeType mimeType, String source, String eventName) {
        final Widget videoElement;
        if (mimeType.mediaType == MediaType.audio) {
            videoElement = Audio.createIfSupported();
        } else {
            videoElement = Video.createIfSupported();
        }
        playerHolder.add(videoElement);
        videoPlayerMenuButton.setData(eventName, source);
        videoElement.addStyleName(style.player());
        videoElement.addStyleName("video-js");
        videoElement.addStyleName("vjs-default-skin");
        videoElement.addStyleName("vjs-big-play-centered");
        videoElement.getElement().setAttribute("preload", "auto");
        videoElement.getElement().setId(elementId = "videojs_" + Document.get().createUniqueId());
        if (fullHeightWidth) {
            videoElement.addStyleName("video-js-fullscreen");
        }
        videoElement.getElement().setAttribute("controls", "");
        this.panorama = mimeType.isPanorama();
        if (this.panorama) {
            videoElement.getElement().setAttribute("crossorigin", "anonymous");
        }
        if (mimeType.isFlippedPanorama()) {
            videoElement.addStyleName(style.invertedVideoPlayer());
        }
        if (isAttached()) {
            prepareDependencies();
        }
        String type = null;
        if (mimeType.mediaSubType == MediaSubType.youtube) {
            type = "video/youtube";
            controls = false;
        } else if (mimeType.mediaSubType == MediaSubType.vimeo) {
            type = "video/vimeo";
        } else if (mimeType.mediaSubType == MediaSubType.mp4) {
            type = "video/mp4";
        } else if (mimeType == MimeType.mp3) {
            type = "audio/mp3";
        }
        if (type != null) {
            SourceElement se = Document.get().createSourceElement();
            se.setSrc(source);
            se.setType(type);
            videoElement.getElement().appendChild(se);
        }
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        if (elementId != null) {
            prepareDependencies();
        }
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        resizeChecker.cancel();
    }

    private native void setupPlayer(String elementId, boolean autoplay, boolean withPanorama,
            String messageThreeSixty) /*-{
        var that = this;

        var player = $wnd.videojs(elementId, {
            techOrder: ['vimeo', 'youtube', 'html5'],
            "playsInline" : true,
            "customControlsOnMobile" : true,
            youtube: {
                enablePrivacyEnhancedMode: true,
                customVars: { 
                    rel: 0, 
                    modestbranding: 0,
                    controls: 1,
                    fs: 1 }
                },
            vimeo: {
                "dnt": 1,
            }
        });
        player.ready(function() {
            this.on('play', function() {
                that.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::onPlay()();
            });
            this.on('pause', function() {
                that.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::onPause()();
            });

            console.log("play: " + autoplay);
            if (autoplay) {
                this.play();
            }
        });
        if (withPanorama) {
            player.panorama({
                showNotice : true,
                autoMobileOrientation : true,
                clickAndDrag : true,
                clickToToggle : false,
                NoticeMessage : messageThreeSixty,
                backToVerticalCenter : false,
                backToHorizonCenter : false,
            });
        }

        this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player = player;
    }-*/;

    private native void disposeNative() /*-{
        var player = this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player;
        player.dispose();
    }-*/;

    private native void handleResize() /*-{
        var player = this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player;
        var canvas = player.getChild('Canvas');
        if (canvas) {
            canvas.handleResize();
        }
    }-*/;

    /**
     * Set the visibility of video.js controls. E.g. for YouTube player the video.js controls have to be deactivated so
     * that the native YouTube controls can be used.
     * 
     * @param controls
     *            if video.js controls are visible or not
     */
    private native void setControls(boolean controls) /*-{
        var player = this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player;
        if (player) {
            player.controls(controls);
        }
    }-*/;

    private void prepareDependencies() {
        final Command setupPlayerCommand = () -> {
            setupPlayer(elementId, autoplay, panorama, StringMessages.INSTANCE.threeSixtyVideoHint());
            resizeChecker.scheduleRepeating(RESIZE_CHECK);
        };
        if (panorama) {
            final Callback<Void, Exception> callback = new Callback<Void, Exception>() {
                @Override
                public void onSuccess(Void result) {
                    setupPlayerCommand.execute();
                }

                @Override
                public void onFailure(Exception reason) {
                }
            };
            ScriptInjector.fromUrl("js/three.js").setWindow(ScriptInjector.TOP_WINDOW).setCallback(callback).inject();
        } else {
            setupPlayerCommand.execute();
        }
    }

    /**
     * Get the length of the video in seconds
     *
     * @return duration in seconds
     */
    public int getDuration() {
        if (player == null) {
            return 0;
        } else {
            return getNativeDuration();
        }
    }

    private native int getNativeDuration() /*-{
        var player = this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player;
        return player.duration();
    }-*/;

    /**
     * Get the current time in seconds
     *
     * @return duration in seconds
     */
    public int getCurrentTime() {
        if (player == null) {
            return 0;
        } else {
            return getNativeCurrentTime();
        }
    }

    private native int getNativeCurrentTime() /*-{
        return this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player.currentTime();
    }-*/;

    /**
     * Get the current time (in seconds)
     * 
     * @return duration in seconds
     */
    public void setCurrentTime(int currentTime) {
        if (player == null) {
            return;
        } else {
            setNativeCurrentTime(currentTime);
        }
    }

    private native void setNativeCurrentTime(int currentTime) /*-{
        return this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player.currentTime(currentTime);
    }-*/;

    public void play() {
        if (player == null) {
            autoplay = true;
        } else {
            nativePlay();
            setControls(controls);
        }
    }

    private native void nativePlay() /*-{
        return this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player.play();
    }-*/;

    /**
     * Check whether or not the player is running in full screen mode
     * 
     * @return <code>true</code> if the player is running in full screen mode, <code>false</code> otherwise
     */
    public native boolean isFullscreen() /*-{
        if (this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player == null) {
            return false;
        }
        return this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player.isFullscreen();
    }-*/;

    /**
     * Check whether the player is currently paused or playing.
     * 
     * @return <code>true</code> if the player is paused, <code>false</code> if it is playing
     */
    public native boolean paused() /*-{
        if (this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player == null) {
            return true;
        }
        return this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player.paused();
    }-*/;

    private void onPlay() {
        fireEvent(new PlayEvent());
    }

    private void onPause() {
        fireEvent(new PauseEvent());
    }

    public native void pause() /*-{
        if (this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player == null) {
            return;
        }
        return this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player.pause();
    }-*/;

    public int getVideoWidth() {
        return -1;
    }

    public int getVideoHeight() {
        return -1;
    }

    public void setMuted(boolean muted) {
        if (muted) {
            setVolume(0f);
        } else {
            setVolume(1f);
        }
    }

    public void setVolume(float volume) {
        if (player != null) {
            setVolumeNative(volume);
        }
    }

    private native void setVolumeNative(float volume) /*-{
        this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player.volume(volume);
    }-*/;

    public void setPlaybackRate(double newPlaySpeedFactor) {
    }

    public void setControlsVisible(boolean isVisible) {
        setControls(isVisible);
    }

    @Override
    public void onResize() {
        if (player != null) {
            handleResize();
        }
    }

    public void disposeIf2D() {
        if (!panorama) {
            if (player != null) {
                disposeNative();
                player = null;
            }
        }
    }

}
