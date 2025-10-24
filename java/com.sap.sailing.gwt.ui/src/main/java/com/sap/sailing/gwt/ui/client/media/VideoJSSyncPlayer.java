package com.sap.sailing.gwt.ui.client.media;

import java.util.UUID;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.media.MediaTrackWithSecurityDTO;
import com.sap.sailing.gwt.ui.client.media.shared.AbstractMediaPlayer;
import com.sap.sailing.gwt.ui.client.media.shared.MediaSynchPlayer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.client.media.TakedownNoticeService;
import com.sap.sse.gwt.client.player.Timer;

public class VideoJSSyncPlayer extends AbstractMediaPlayer implements MediaSynchPlayer, RequiresResize {
    private VideoJSPlayer videoJsDelegate;

    private EditFlag editFlag;
    private final TimePoint raceStartTime;
    private final Timer raceTimer;

    public VideoJSSyncPlayer(MediaTrackWithSecurityDTO mediaTrack, TimePoint raceStartTime, Timer raceTimer, TakedownNoticeService takedownNoticeService,
            String regattaAndRaceIdentifier, UUID eventId) {
        super(mediaTrack);
        videoJsDelegate = new VideoJSPlayer(true, false, takedownNoticeService, "takedownRequestForRaceVideo");
        this.raceStartTime = raceStartTime;
        this.raceTimer = raceTimer;
        videoJsDelegate.setVideo(mediaTrack.mimeType, mediaTrack.url, ""+eventId+"/"+regattaAndRaceIdentifier);
    }

    @Override
    public boolean isMediaPaused() {
        return videoJsDelegate.paused();
    }

    @Override
    public void pauseMedia() {
        if (!isEditing()) {
            videoJsDelegate.pause();
        }
    }

    @Override
    public void playMedia() {
        if (!isEditing()) {
            videoJsDelegate.play();
        }
    }

    @Override
    public double getDuration() {
        return videoJsDelegate.getDuration();
    }

    @Override
    public double getCurrentMediaTime() {
        return videoJsDelegate.getCurrentTime();
    }

    @Override
    public long getCurrentMediaTimeMillis() {
        return (long) (getCurrentMediaTime() * 1000);
    }

    public void setCurrentMediaTime(double mediaTime) {
        videoJsDelegate.setCurrentTime((int) mediaTime);
    }

    @Override
    public void setPlaybackSpeed(double newPlaySpeedFactor) {
        videoJsDelegate.setPlaybackRate(newPlaySpeedFactor);
    }

    @Override
    public void setMuted(boolean isToBeMuted) {
        videoJsDelegate.setMuted(isToBeMuted);
    }

    @Override
    public void shutDown() {
        videoJsDelegate.removeFromParent();
        videoJsDelegate.disposeIf2D();
    }

    @Override
    public Widget asWidget() {
        return videoJsDelegate;
    }

    @Override
    public long getOffset() {
        return getMediaTrack().startTime.asMillis() - raceStartTime.asMillis();
    }

    @Override
    public void changeOffsetBy(long delta) {
        getMediaTrack().startTime = getMediaTrack().startTime.plus(delta);
        forceAlign();
    }

    @Override
    public void setControlsVisible(boolean isVisible) {
        videoJsDelegate.setControlsVisible(isVisible);
    }

    @Override
    public void pauseRace() {
        videoJsDelegate.pause();
    }

    @Override
    public void updateOffset() {
        getMediaTrack().startTime = new MillisecondsTimePoint(
                raceTimer.getTime().getTime() - getCurrentMediaTimeMillis());

    }

    @Override
    protected void alignTime() {
        if (!isEditing()) {
            super.alignTime();
        }
    }

    @Override
    public void setEditFlag(EditFlag editFlag) {
        this.editFlag = editFlag;
    }

    private boolean isEditing() {
        return editFlag != null && editFlag.isEditing();
    }

    @Override
    public int getDefaultWidth() {
        return videoJsDelegate.getVideoWidth();
    }

    @Override
    public int getDefaultHeight() {
        return videoJsDelegate.getVideoHeight();
    }

    @Override
    public void onResize() {
        videoJsDelegate.onResize();
    }
}
