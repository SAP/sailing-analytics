package com.sap.sailing.gwt.ui.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.VideoMetadataDTO;
import com.sap.sailing.domain.common.media.MediaTrack;

public interface MediaService extends RemoteService {

    Iterable<MediaTrack> getMediaTracksForRace(RegattaAndRaceIdentifier regattaAndRaceIdentifier);

    Iterable<MediaTrack> getMediaTracksInTimeRange(RegattaAndRaceIdentifier regattaAndRaceIdentifier);

    Iterable<MediaTrack> getAllMediaTracks();

    String addMediaTrack(MediaTrack mediaTrack);

    void deleteMediaTrack(MediaTrack mediaTrack);

    void updateTitle(MediaTrack mediaTrack);

    void updateUrl(MediaTrack mediaTrack);

    void updateStartTime(MediaTrack mediaTrack);

    void updateDuration(MediaTrack mediaTrack);

    void updateRace(MediaTrack mediaTrack);

    VideoMetadataDTO checkMetadata(byte[] start, byte[] end, Long skipped);

    VideoMetadataDTO checkMetadata(String url);
    
    MediaTrack getMediaTrackByUrl(String url);
}
