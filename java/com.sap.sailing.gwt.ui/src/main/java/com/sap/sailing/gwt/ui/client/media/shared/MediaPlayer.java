package com.sap.sailing.gwt.ui.client.media.shared;

import java.util.Date;

import com.sap.sailing.domain.common.media.MediaTrackWithSecurityDTO;


public interface MediaPlayer {
    boolean isMediaPaused();

    void pauseMedia();

    void playMedia();

    void raceTimeChanged(Date raceTime);

    MediaTrackWithSecurityDTO getMediaTrack();
    
    double getDuration();
    
    double getCurrentMediaTime();
    
    void setCurrentMediaTime(double mediaTime);
    
    long getCurrentMediaTimeMillis();
    
    void setPlaybackSpeed(double newPlaySpeedFactor);

    void setMuted(boolean isToBeMuted);

    void shutDown();

    boolean isCoveringCurrentRaceTime();
}
