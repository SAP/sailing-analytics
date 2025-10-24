package com.sap.sailing.gwt.ui.client.media.popup;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;
import com.sap.sailing.domain.common.media.MediaTrackWithSecurityDTO;
import com.sap.sailing.gwt.ui.client.media.MediaPlayerContainer;
import com.sap.sailing.gwt.ui.client.media.shared.AbstractMediaPlayer;
import com.sap.sailing.gwt.ui.client.media.shared.MediaPlayer;

public abstract class PopoutWindowPlayer extends AbstractMediaPlayer implements MediaPlayer, MediaPlayerContainer {

    public interface PlayerCloseListener {
        void playerClosed();

        void setVideoContainer(MediaPlayerContainer videoContainer);
    }

    private final JavaScriptObject playerWindow;
    private final PlayerCloseListener popupCloseHandler;

    protected PopoutWindowPlayer(MediaTrackWithSecurityDTO mediaTrack, PlayerCloseListener popupCloseListener, String playerWindowUrl) {
        super(mediaTrack);
        this.popupCloseHandler = popupCloseListener;
        String codesvr = Window.Location.getParameter("gwt.codesvr");
        if (codesvr != null) {
            playerWindowUrl = playerWindowUrl + "&gwt.codesvr=" + codesvr;  
        }
        playerWindow = openWindow(playerWindowUrl);
        registerNativeStuff();
    }
    
    native JavaScriptObject openWindow(String url) /*-{
        return $wnd
                .open(
                        url,
                        '_blank',
                        "resizable=yes, scrollbars=no,  toolbar=no,  menubar=no, status=no, location=no, directories=no, personalbar=no, width=660, height=380");
    }-*/;

    @Override
    public native void shutDown() /*-{
        var playerWindow = this.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::playerWindow;
        if (!playerWindow.closed) {
            playerWindow.close();
        }
    }-*/;

    native JavaScriptObject registerNativeStuff() /*-{
        var that = this;
        var window = that.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::playerWindow;
        window.onbeforeunload = function() {
            that.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::onClosingPopup()();
        }
        window.parent.deferredPlayState = {
            deferredIsPlaying : false,
            deferredIsMuted : true,
            deferredMediaTime : 0,
            playbackSpeed : 1
        };
    }-*/;

    private void onClosingPopup() {
        pauseMedia();
        popupCloseHandler.playerClosed();
    }

    @Override
    public native void playMedia() /*-{
        var window = this.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::playerWindow;
        if (!window.parent.videoPlayer) {
            window.parent.deferredPlayState.deferredIsPlaying = true;
        } else {
            window.parent.videoPlayer.play();
        }

    }-*/;

    @Override
    public native void pauseMedia() /*-{
        var window = this.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::playerWindow;
        if (!window.parent.videoPlayer) {
            window.parent.deferredPlayState.deferredIsPlaying = false;
        } else {
            window.parent.videoPlayer.pause();
        }

    }-*/;

    @Override
    public native void setCurrentMediaTime(double mediaTime) /*-{
        var window = this.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::playerWindow;
        if (!window.parent.videoPlayer) {
            window.parent.deferredPlayState.deferredMediaTime = mediaTime;
        } else {
            window.parent.videoPlayer.setTime(mediaTime);
        }

    }-*/;

    @Override
    public native void setMuted(boolean muted) /*-{
        var window = this.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::playerWindow;
        if (!window.parent.videoPlayer) {
            window.parent.deferredPlayState.deferredIsMuted = muted;
        } else {
            window.parent.videoPlayer.setMuted(muted);
        }
    }-*/;

    @Override
    public native boolean isMediaPaused() /*-{
        var window = this.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::playerWindow;
        if (!window.parent.videoPlayer) {
            return !window.parent.deferredPlayState.deferredIsPlaying;
        } else {
            return window.parent.videoPlayer.isPaused();
        }
    }-*/;

    @Override
    public native double getDuration() /*-{
        var window = this.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::playerWindow;
        if (!window.parent.videoPlayer) {
            return NaN;
        } else {
            return window.parent.videoPlayer.getDuration();
        }
    }-*/;

    @Override
    public native double getCurrentMediaTime() /*-{
        var window = this.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::playerWindow;
        if (!window.parent.videoPlayer) {
            return window.parent.deferredPlayState.deferredMediaTime;
        } else {
            return window.parent.videoPlayer.getTime();
        }
    }-*/;

    @Override
    public native void setPlaybackSpeed(double playbackSpeed) /*-{
        var window = this.@com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer::playerWindow;
        if (!window.parent.videoPlayer) {
            window.parent.deferredPlayState.deferredPlaybackSpeed = playbackSpeed;
        } else {
            window.parent.videoPlayer.setPlaybackSpeed(playbackSpeed);
        }
    }-*/;

    @Override
    public MediaPlayer getMediaPlayer() {
        return this;
    }

}
