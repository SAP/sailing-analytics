package com.sap.sailing.gwt.home.communication.media;

import java.util.Collection;
import java.util.TreeSet;

import com.sap.sailing.gwt.ui.client.shared.SailingVideoDTO;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;
import com.sap.sse.gwt.dispatch.shared.commands.Result;

public class MediaDTO implements Result {

    private TreeSet<SailingImageDTO> photos = new TreeSet<>();
    private TreeSet<SailingVideoDTO> videos = new TreeSet<>();

    public Collection<SailingImageDTO> getPhotos() {
        return photos;
    }

    public void addPhoto(SailingImageDTO photo) {
        photos.add(photo);
    }

    public Collection<SailingVideoDTO> getVideos() {
        return videos;
    }

    public void addVideo(SailingVideoDTO video) {
        videos.add(video);
    }

    public void removeVideo(SailingVideoDTO video) {
        videos.remove(video);
    }

    public void removePhoto(SailingImageDTO photo) {
        photos.remove(photo);
    }
}
