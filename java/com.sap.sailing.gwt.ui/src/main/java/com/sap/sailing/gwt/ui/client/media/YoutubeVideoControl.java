package com.sap.sailing.gwt.ui.client.media;

import com.google.gwt.core.client.JavaScriptObject;

public class YoutubeVideoControl  {

    private JavaScriptObject youtubePlayer;

    private boolean deferredIsPlaying;

    private double deferredCurrentTime;

    private boolean deferredIsMuted;

    private boolean deferredIsControlsVisible;

    private double deferredPlaybackSpeed;

    YoutubeVideoControl(String videoUrl, String videoContainerId) {
        if (!isYoutubeApiInitialized()) {
            loadInitialYoutubePlayer(videoUrl, videoContainerId);
        } else {
            loadYoutubePlayer(videoUrl, videoContainerId);
        }
    }

    private native boolean isYoutubeApiInitialized() /*-{
        return $wnd.youtubeApiInitialized != null;
    }-*/;

    private native void setYoutubeApiInitialized() /*-{
        $wnd.youtubeApiInitialized = true;
    }-*/;

    // Inspired by https://developers.google.com/youtube/iframe_api_reference#Examples
    // See also: https://developers.google.com/youtube/js_api_reference
    // Code Playground: https://code.google.com/apis/ajax/playground/?exp=youtube#chromeless_player
    // Extended with API-initialization control to support multiple players on the same page. 
    private native void loadInitialYoutubePlayer(String videoUrl, String videoContainerId) /*-{
                var that = this;
                // This function creates an <iframe> containing a YouTube player after the API code downloads.
                var player;
                $wnd.onYouTubeIframeAPIReady = function() {
                        that.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::setYoutubeApiInitialized()();
                        that.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::loadYoutubePlayer(Ljava/lang/String;Ljava/lang/String;)(videoUrl, videoContainerId);
                }
                // Use script tag trick to cope with browser's cross domain restrictions
                var tag = $doc.createElement('script');
                tag.src = "//www.youtube.com/iframe_api"; // This is a protocol-relative URL as described here: http://paulirish.com/2010/the-protocol-relative-url/
                var firstScriptTag = $doc.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
    }-*/;

    private native void loadYoutubePlayer(String videoUrl, String videoContainerId) /*-{
        var that = this;
        var player = new $wnd.YT.Player(videoContainerId, {
                videoId : videoUrl,
                //height: '480', //see https://developers.google.com/youtube/iframe_api_reference?hl=en#Playback_quality
                //width: '853',
                events: { //https://developers.google.com/youtube/iframe_api_reference?hl=en#Events
                    'onReady': function(event) {
                                    that.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::initPlayState(Lcom/google/gwt/core/client/JavaScriptObject;)(player);
                               }
                },
                host: 'https://www.youtube-nocookie.com',
                playerVars : {
                        'autoplay' : 0,
                        'disablekb': 1,
                        rel: 0,
                        modestbranding: 1
                }
        });
    }-*/;

    public void initPlayState(JavaScriptObject youtubePlayer) {
        this.youtubePlayer = youtubePlayer;
        nativeSetPlaybackSpeed(deferredPlaybackSpeed);
        nativeSetControlsVisible(deferredIsControlsVisible);
        nativeSetCurrentTime(deferredCurrentTime);
        nativeSetMuted(deferredIsMuted);
        if (deferredIsPlaying) {
            nativePlay();
        } else {
            nativePause();
        }
    }
        
    public void play() {
        if (this.youtubePlayer != null) {
            nativePlay();
        } else {
            this.deferredIsPlaying = true;
        }
    }
    
    private native void nativePlay() /*-{
                var player = this.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::youtubePlayer;
                player.playVideo();
    }-*/;
    
    public void pause() {
        if (this.youtubePlayer != null) {
            nativePause();
        } else {
            this.deferredIsPlaying = false;
        }
    }

    private native void nativePause() /*-{
                var player = this.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::youtubePlayer;
                player.pauseVideo();
    }-*/;

    public void setCurrentTime(double time) {
        if (this.youtubePlayer != null) {
            nativeSetCurrentTime(time);
        } else {
            this.deferredCurrentTime = time;
        }
    }
    
    private native void nativeSetCurrentTime(double time) /*-{
        var player = this.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::youtubePlayer;
        var allowSeekAhead = true; 
        var duration = player.getDuration();
        player.seekTo(Math.min(time, duration), allowSeekAhead);
    }-*/;

    public void setMuted(boolean muted) {
        if (this.youtubePlayer != null) {
            nativeSetMuted(muted);
        } else {
            this.deferredIsMuted = muted;
        }
    }
    
    private native void nativeSetMuted(boolean muted) /*-{
                var player = this.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::youtubePlayer;
                if (muted) {
                    player.mute();
                } else {
                    player.unMute()
                }
    }-*/;

    public boolean isPaused() {
        boolean isPaused = youtubePlayer == null ? !deferredIsPlaying : nativeIsPaused();
        return isPaused;
    }
    
    private native boolean nativeIsPaused() /*-{
                var player = this.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::youtubePlayer;
                return player.getPlayerState() != $wnd.YT.PlayerState.PLAYING;
    }-*/;

    public double getDuration() {
        return youtubePlayer == null ? Double.NaN : nativeGetDuration(); 
    }
    
    private native double nativeGetDuration() /*-{
                var player = this.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::youtubePlayer;
                return player.getDuration();
    }-*/;

    public double getCurrentTime() {
        return youtubePlayer == null ? deferredCurrentTime : nativeGetCurrentTime(); 
    }
    
    private native double nativeGetCurrentTime() /*-{
                var player = this.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::youtubePlayer;
                return player.getCurrentTime();
    }-*/;

    public void setPlaybackSpeed(double newPlaySpeedFactor) {
        if (this.youtubePlayer != null) {
            nativeSetPlaybackSpeed(newPlaySpeedFactor);
        } else {
            this.deferredPlaybackSpeed = newPlaySpeedFactor;
        }
    }
    
    private native void nativeSetPlaybackSpeed(double newPlaySpeedFactor) /*-{
                var player = this.@com.sap.sailing.gwt.ui.client.media.YoutubeVideoControl::youtubePlayer;
                player.setPlaybackRate(newPlaySpeedFactor);
    }-*/;

    public void setControlsVisible(boolean isVisible) {
        if (this.youtubePlayer != null) {
            nativeSetControlsVisible(isVisible);
        } else {
            this.deferredIsControlsVisible = isVisible;
        }
    }
    
    native void nativeSetControlsVisible(boolean isVisible) /*-{
        //TODO: find respsective property in the player API
    }-*/;

}
