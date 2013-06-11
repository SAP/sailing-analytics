package com.sap.sailing.gwt.ui.raceboard;

import com.google.gwt.http.client.URL;
import com.sap.sailing.domain.common.media.MediaTrack;

public class VideoWindowPlayer extends PopupWindowPlayer {
    
    public VideoWindowPlayer(MediaTrack mediaTrack, PopupCloseListener popCloseListener) {
        super(mediaTrack, popCloseListener);
    }

    @Override
    protected String getPlayerWindowUrl() {
        String videoUrl = getMediaTrack().url;
        String title = getMediaTrack().title;
        
        String videoPlayerUrl = "/gwt/VideoPopup.html?url=" + URL.encodeQueryString(videoUrl) + "&title=" + URL.encodeQueryString(title);
        
        return videoPlayerUrl;

    }
}
