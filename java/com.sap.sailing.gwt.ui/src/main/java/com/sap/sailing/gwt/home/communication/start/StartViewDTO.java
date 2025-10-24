package com.sap.sailing.gwt.home.communication.start;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.sap.sailing.gwt.home.communication.eventlist.EventListEventDTO;
import com.sap.sailing.gwt.ui.client.shared.SailingVideoDTO;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;
import com.sap.sse.gwt.dispatch.shared.commands.Result;

public class StartViewDTO implements Result {

    private ArrayList<EventStageDTO> stageEvents = new ArrayList<EventStageDTO>();

    private ArrayList<EventListEventDTO> recentEvents = new ArrayList<EventListEventDTO>();
    
    private ArrayList<SailingImageDTO> photos = new ArrayList<>();
    private HashSet<SailingVideoDTO> videos = new HashSet<>();

    public List<EventStageDTO> getStageEvents() {
        return stageEvents;
    }
    
    public void addStageEvent(EventStageDTO event) {
        this.stageEvents.add(event);
    }

    public List<EventListEventDTO> getRecentEvents() {
        return recentEvents;
    }
    
    public void addRecentEvent(EventListEventDTO event) {
        this.recentEvents.add(event);
    }
    
    public ArrayList<SailingImageDTO> getPhotos() {
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
}
