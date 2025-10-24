package com.sap.sailing.gwt.ui.client.media.popup;

import com.google.gwt.user.client.Window;
import com.sap.sse.gwt.client.StringMessages;
import com.sap.sse.security.ui.client.AbstractSecureEntryPoint;

public abstract class AbstractPopupWindow extends AbstractSecureEntryPoint<StringMessages> {

    @Override
    public void doOnModuleLoad() {
        super.doOnModuleLoad();
        initializePlayer();
    }

    @Override
    protected StringMessages createStringMessages() {
        return StringMessages.INSTANCE;
    }
    
    protected abstract void initializePlayer();

    native void addCallbackMethods() /*-{
		var that = this;
		$wnd.videoPlayer = {
			play : function() {
				that.@com.sap.sailing.gwt.ui.client.media.popup.AbstractPopupWindow::play()();
			},
			pause : function() {
				that.@com.sap.sailing.gwt.ui.client.media.popup.AbstractPopupWindow::pause()();
			},
			setTime : function(time) {
				that.@com.sap.sailing.gwt.ui.client.media.popup.AbstractPopupWindow::setTime(D)(time);
			},
			setMuted : function(muted) {
				that.@com.sap.sailing.gwt.ui.client.media.popup.AbstractPopupWindow::setMuted(Z)(muted);
			},
			isPaused : function() {
				return that.@com.sap.sailing.gwt.ui.client.media.popup.AbstractPopupWindow::isPaused()();
			},
			getDuration : function() {
				return that.@com.sap.sailing.gwt.ui.client.media.popup.AbstractPopupWindow::getDuration()();
			},
			getTime : function() {
				return that.@com.sap.sailing.gwt.ui.client.media.popup.AbstractPopupWindow::getTime()();
			},
			setPlaybackSpeed : function(newPlaySpeedFactor) {
				that.@com.sap.sailing.gwt.ui.client.media.popup.AbstractPopupWindow::setPlaybackSpeed(D)(newPlaySpeedFactor);
			}
		};
    }-*/;

    public abstract void play();

    public abstract void pause();

    public abstract void setTime(double time);

    public abstract void setMuted(boolean muted);

    public abstract boolean isPaused();

    public abstract double getDuration();

    public abstract double getTime();

    public abstract void setPlaybackSpeed(double newPlaySpeedFactor);

    public void initPlayState(double deferredMediaTime, boolean deferredIsMuted, double deferredPlaybackSpeed,
            boolean deferredIsPlaying) {
        addCallbackMethods();
        setTime(deferredMediaTime);
        setMuted(deferredIsMuted);
        setPlaybackSpeed(deferredPlaybackSpeed);
        if (deferredIsPlaying) {
            play();
        } else {
            pause();
        }
    }

    protected void adjustWindowSize() {
        int windowWidth = Window.getClientWidth();
        int videoWidth = getVideoWidth();
        int widthDelta = videoWidth - windowWidth;
        int windowHeight = Window.getClientHeight();
        int videoHeight = getVideoHeight();
        int heightDelta = videoHeight - windowHeight;
        Window.resizeBy(widthDelta, heightDelta);
    }

    protected abstract boolean hasVideoSizes();
    
    protected abstract int getVideoWidth();
    
    protected abstract int getVideoHeight();
    
    protected abstract void setVideoSize(int width, int height);
    
}
