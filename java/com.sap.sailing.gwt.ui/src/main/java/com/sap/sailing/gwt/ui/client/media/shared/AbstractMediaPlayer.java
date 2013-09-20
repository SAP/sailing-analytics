package com.sap.sailing.gwt.ui.client.media.shared;

import java.util.Date;

import com.sap.sailing.domain.common.media.MediaTrack;


public abstract class AbstractMediaPlayer implements MediaPlayer {

    private static final int TOLERATED_LAG_IN_MILLISECONDS = 4000;   
    private final MediaTrack mediaTrack;
    private long raceTimeInMillis;

    protected AbstractMediaPlayer(MediaTrack mediaTrack) {
        this.mediaTrack = mediaTrack;
    }

	public MediaTrack getMediaTrack() {
        return mediaTrack;
    }
    
    protected void onMediaTimeUpdate() {
        //default no op
    }

    public void forceAlign() {
        forceAlign(mediaTrack.startTime.getTime());
    }

    public void raceTimeChanged(Date raceTime) {
        raceTimeInMillis = raceTime.getTime();
        alignTime();
    }
    
    @Override
    public long getCurrentMediaTimeMillis() {
        return Math.round(getCurrentMediaTime() * 1000);
    }
    
    @Override
	public boolean isCoveringCurrentRaceTime() {
        double mediaTime = (raceTimeInMillis - mediaTrack.startTime.getTime()) / 1000d;
        return (mediaTime >= 0) && (mediaTime <= getDuration());
	}

    protected void alignTime() {
        long mediaStartTimeInMillis = mediaTrack.startTime.getTime();
        long mediaTimeInMillis = mediaStartTimeInMillis + getCurrentMediaTimeMillis();
        long mediaLaggingBehindRaceInMillis = raceTimeInMillis - mediaTimeInMillis;
        if (Math.abs(mediaLaggingBehindRaceInMillis) > TOLERATED_LAG_IN_MILLISECONDS) {
            forceAlign(mediaStartTimeInMillis);
        }
    }

    private void forceAlign(long mediaStartTimeInMillis) {
        double mediaTime = (raceTimeInMillis - mediaStartTimeInMillis) / 1000d;
        if (mediaTime < 0) {
            pauseMedia();
        } else if (mediaTime > getDuration()) {
            pauseMedia();
        } else {
            setCurrentMediaTime(mediaTime);
        }
    }

}
