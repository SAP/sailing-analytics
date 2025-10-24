package com.sap.sailing.gwt.ui.client.media.popup;

import java.util.UUID;

import com.google.gwt.http.client.URL;
import com.sap.sailing.domain.common.media.MediaTrackWithSecurityDTO;

public class VideoJSWindowPlayer extends PopoutWindowPlayer {
    public VideoJSWindowPlayer(MediaTrackWithSecurityDTO mediaTrack, PlayerCloseListener popupCloseListener, String raceIdentifierAsString, UUID eventId) {
        super(mediaTrack, popupCloseListener, getPlayerWindowUrl(mediaTrack, raceIdentifierAsString, eventId));
    }

    private static String getPlayerWindowUrl(MediaTrackWithSecurityDTO mediaTrack, String raceIdentifierAsString, UUID eventId) {
        String videoUrl = mediaTrack.url;
        String title = mediaTrack.title;
        String mimetype = mediaTrack.mimeType == null ? "" : mediaTrack.mimeType.name();
        String videoPlayerUrl = "/gwt/VideoPopup.html?url=" + URL.encodeQueryString(videoUrl) + "&title="
                + URL.encodeQueryString(title) + "&mimetype=" + URL.encodeQueryString(String.valueOf(mimetype))
                + "&eventName=" + URL.encodeQueryString(""+eventId+"/"+raceIdentifierAsString);
        return videoPlayerUrl;
    }
}
