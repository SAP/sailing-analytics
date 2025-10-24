package com.sap.sailing.gwt.ui.client.media.popup;

import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.sap.sailing.gwt.ui.client.media.VideoJSPlayer;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.gwt.resources.CommonControlsCSS;

public class VideoPopupWindow extends AbstractPopupWindow implements ContextMenuHandler {

    private boolean isDebug;
    private VideoJSPlayer videoJSDelegate;

    @Override
    protected void initializePlayer() {
        RootLayoutPanel mainPanel = RootLayoutPanel.get();
        CommonControlsCSS.ensureInjected();
        String title = Window.Location.getParameter("title");
        Window.setTitle(title);
        String videoUrl = Window.Location.getParameter("url");
        String mimeType = Window.Location.getParameter("mimetype");
        isDebug = Window.Location.getParameter("gwt.codesvr") != null;
        if (videoUrl != null) {
            videoJSDelegate = new VideoJSPlayer(true, false, getUserService(), "takedownRequestForRaceVideo");
            videoJSDelegate.setVideo(MimeType.valueOf(mimeType), videoUrl, Window.Location.getParameter("eventName"));
            mainPanel.add(videoJSDelegate);
            initPlay();
        } else {
            mainPanel.add(new Label("Parameter 'url' not assigned."));
        }
    }

    native void initPlay() /*-{
        var that = this;
        var deferredPlayState = $wnd.deferredPlayState
        if (deferredPlayState && !$wnd.videoPlayer) {
            that.@com.sap.sailing.gwt.ui.client.media.popup.VideoPopupWindow::initPlayState(DZDZ)(deferredPlayState.deferredMediaTime, deferredPlayState.deferredIsMuted, deferredPlayState.deferredPlaybackSpeed, deferredPlayState.deferredIsPlaying);
        }
    }-*/;

    public void loadedmetadata() {
        adjustWindowSize();
    }

    @Override
    public void play() {
        videoJSDelegate.play();
    }

    @Override
    public void pause() {
        videoJSDelegate.pause();
    }

    @Override
    public void setTime(double time) {
        videoJSDelegate.setCurrentTime((int) Math.min(time, videoJSDelegate.getDuration()));
    }

    @Override
    public void setMuted(boolean muted) {
        videoJSDelegate.setMuted(muted);
    }

    @Override
    public boolean isPaused() {
        return videoJSDelegate.paused();
    }

    @Override
    public double getDuration() {
        return videoJSDelegate.getDuration();
    }

    @Override
    public double getTime() {
        return videoJSDelegate.getCurrentTime();
    }

    @Override
    public void setPlaybackSpeed(double newPlaySpeedFactor) {
        videoJSDelegate.setPlaybackRate(newPlaySpeedFactor);
    }

    /**
     * Suppressing context menu.
     */
    @Override
    public void onContextMenu(ContextMenuEvent event) {
        if (!isDebug) {
            event.preventDefault();
            event.stopPropagation();
        }
    }

    @Override
    protected boolean hasVideoSizes() {
        return videoJSDelegate != null;
    }

    @Override
    protected int getVideoWidth() {
        return videoJSDelegate.getVideoWidth();
    }

    @Override
    protected int getVideoHeight() {
        return videoJSDelegate.getVideoHeight();
    }

    @Override
    protected void setVideoSize(int width, int height) {
        videoJSDelegate.setPixelSize(width, height);
    }
}
