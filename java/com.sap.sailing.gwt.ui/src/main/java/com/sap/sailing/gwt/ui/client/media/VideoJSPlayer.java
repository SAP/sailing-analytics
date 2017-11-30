package com.sap.sailing.gwt.ui.client.media;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SourceElement;
import com.google.gwt.dom.client.VideoElement;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.media.MediaSubType;
import com.sap.sse.common.media.MediaType;
import com.sap.sse.common.media.MimeType;

/**
 * video.js (http://videojs.com/) wrapper as GWT widget.
 */
public class VideoJSPlayer extends Widget implements RequiresResize{
    private static VideoJSPlayerUiBinder uiBinder = GWT.create(VideoJSPlayerUiBinder.class);
    private static final int RESIZE_CHECK = 250;

    interface VideoJSPlayerUiBinder extends UiBinder<Element, VideoJSPlayer> {
    }

    @UiField VideoElement videoElement;
    
    private final String elementId;
    private JavaScriptObject player;
    private boolean autoplay;

    private Boolean panorama;

    private Timer resizeToDivtimer;

    private Timer resizeChecker;

    public HandlerRegistration addPlayHandler(PlayEvent.Handler handler) {
        return addHandler(handler, PlayEvent.getType());
    }
    
    public HandlerRegistration addPauseHandler(PauseEvent.Handler handler) {
        return addHandler(handler, PauseEvent.getType());
    }

    public VideoJSPlayer(boolean fullHeightWidth, boolean autoplay) {
        this.autoplay = autoplay;
        setElement(uiBinder.createAndBindUi(this));
        videoElement.setId(elementId = "videojs_" + Document.get().createUniqueId());
        if (fullHeightWidth) {
            videoElement.addClassName("video-js-fullscreen");
        }
        videoElement.setAttribute("controls", "");
        
        resizeChecker = new com.google.gwt.user.client.Timer() {
            
            @Override
            public void run() {
                onResize();
            }
        };
    }

    public void setVideo(MimeType mimeType, String source) {
        this.panorama = mimeType.isPanorama();
        if(isAttached()){
            _onLoad(autoplay, panorama, StringMessages.INSTANCE.threeSixtyVideoHint());
            resizeChecker.scheduleRepeating(RESIZE_CHECK);
        }
        if (mimeType == null || mimeType.mediaType != MediaType.video) {
            return;
        }
        String type = null;
        if (mimeType.mediaSubType == MediaSubType.youtube) {
            type = "video/youtube";
        } else if (mimeType.mediaSubType == MediaSubType.vimeo) {
            type = "video/vimeo";
        } else if (mimeType.mediaSubType == MediaSubType.mp4) {
            type = "video/mp4";
        }
        if (type != null) {
            SourceElement se = Document.get().createSourceElement();
            se.setSrc(source);
            se.setType(type);
            videoElement.appendChild(se);
        }
    }
    
    @Override
    protected void onLoad() {
        super.onLoad();
        if(panorama != null){
                _onLoad(autoplay, panorama, StringMessages.INSTANCE.threeSixtyVideoHint());
                resizeChecker.scheduleRepeating(RESIZE_CHECK);
        }
    }
    
    public VideoElement getVideoElement() {
        return videoElement;
    }
    
   public int getDuration(){
        if (player == null) {
            return 0;
        } else {
            return getNativeDuration();
        }
   }
    
    /**
     * Get the length in time of the video in seconds
     *
     * @return duration in seconds
     */
    private native int getNativeDuration() /*-{
        var player = this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player;
        return player.duration();
    }-*/;

    /**
     * Get the current time (in seconds)
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
    
    @Override
    protected void onDetach() {
        super.onDetach();
        resizeChecker.cancel();
    }
    
    private native void setNativeCurrentTime(int currentTime) /*-{
        return this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player.currentTime(currentTime);
    }-*/;
    
    public void play(){
        if(player == null){
            autoplay = true;
        }else{
            nativePlay();
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
        if(this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player == null) {
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
        if(this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player == null) {
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
    
    /**
     * JSNI wrapper that does setup the video player
     *
     * @param uniqueId
     */
    native void _onLoad(boolean autoplay, boolean withPanorama, String messageThreeSixty) /*-{
        var that = this;
        var elemid = this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::elementId;
        
        var player = $wnd.videojs(
            elemid,
            {
                "playsInline" : true,
                "customControlsOnMobile" : true
            }).ready(function() {
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
       if(withPanorama){     
           player.panorama({
              showNotice:true,
              autoMobileOrientation: true,
              clickAndDrag: true,
              clickToToggle: false,
              NoticeMessage: messageThreeSixty,
            });
        }
        
        this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player = player;
    }-*/;
    
    native void _onUnload() /*-{
       var player = this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player;
       player.dispose();     
    }-*/;

    private native void handleResize() /*-{
        var player = this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player;
        var canvas = player.getChild('Canvas');
        if(canvas){
            canvas.handleResize();
        }
    }-*/;
    
    public native void pause() /*-{
        if(this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player == null) {
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
        if(player != null){
            setVolumeNative(volume);
        }
    }
    
    private native void setVolumeNative(float volume) /*-{
        this.@com.sap.sailing.gwt.ui.client.media.VideoJSPlayer::player.volume(volume);
    }-*/;

    public void setPlaybackRate(double newPlaySpeedFactor) {
        // TODO Auto-generated method stub
        
    }

    public void setControllsVisible(boolean isVisible) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onResize() {
        if (player != null) {
            handleResize();
        }
    }

}
