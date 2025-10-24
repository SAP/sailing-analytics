package com.sap.sailing.gwt.ui.client.media;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.media.MediaTrackWithSecurityDTO;
import com.sap.sailing.gwt.ui.client.MediaServiceAsync;
import com.sap.sailing.gwt.ui.client.MediaServiceWriteAsync;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.useragent.UserAgentDetails;

public interface MediaPlayerManager {
    
    public interface PlayerChangeListener {

        void notifyStateChange();

    }
    
    public enum Status {
        UNDEFINED('?'), CANNOT_PLAY('-'), NOT_REACHABLE('#'), REACHABLE('+');

        private final char symbol;

        private Status(char symbol) {
            this.symbol = symbol;
        }

        public String toString() {
            return String.valueOf(this.symbol);
        }

        public boolean isPotentiallyPlayable() {
            return this == UNDEFINED || this == REACHABLE;
        }
    }

    void addPlayerChangeListener(PlayerChangeListener playerChangeListener);
    
    void playAudio(MediaTrackWithSecurityDTO audioTrack);

    void muteAudio();
    
    void playFloatingVideo(MediaTrackWithSecurityDTO videoTrack);
    
    void closeFloatingPlayer(MediaTrackWithSecurityDTO videoTrack);
    
    void addMediaTrack();
    
    boolean deleteMediaTrack(MediaTrackWithSecurityDTO mediaTrack);
    
    boolean allowsEditing(MediaTrackWithSecurityDTO mediaTrack);

    boolean allowsCreating();

    void playDefault();

    void stopAll();

    Boolean isPlaying();

    Set<MediaTrackWithSecurityDTO> getPlayingAudioTrack();

    Set<MediaTrack> getPlayingVideoTracks();

    Collection<MediaTrackWithSecurityDTO> getAssignedMediaTracks();
    
    Collection<MediaTrackWithSecurityDTO> getOverlappingMediaTracks();

    List<MediaTrack> getAudioTracks();

    List<MediaTrack> getVideoTracks();

    UserAgentDetails getUserAgent();

    RegattaAndRaceIdentifier getCurrentRace();

    MediaServiceAsync getMediaService();

    MediaServiceWriteAsync getMediaServiceWrite();

    ErrorReporter getErrorReporter();

    Status getMediaTrackStatus(MediaTrack track);
}

