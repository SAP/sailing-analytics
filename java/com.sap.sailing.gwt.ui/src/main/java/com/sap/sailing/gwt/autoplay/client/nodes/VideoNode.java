package com.sap.sailing.gwt.autoplay.client.nodes;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.nodes.base.FiresPlaceNode;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.video.VideoPlace;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.client.media.VideoDTO;

public class VideoNode extends FiresPlaceNode {
    private final AutoPlayClientFactory cf;
    private int lastPlayed = -1;
    private VideoDTO currentVideo = null;
    private Consumer<Integer> durationConsumer;

    public VideoNode(AutoPlayClientFactory cf) {
        super(VideoNode.class.getName());
        this.cf = cf;
    }

    @Override
    public void onStart() {
        final EventDTO event = cf.getAutoPlayCtxSignalError().getEvent();
        final List<VideoDTO> videos = event.getVideos().stream().filter(v -> v.hasTag(MediaTagConstants.BIGSCREEN.getName()))
                .collect(Collectors.toList());
        if (videos.size() == 0) {
            lastPlayed = -1;
            currentVideo = null;
            // prevents switching to the next node while this node is being initialized. This prevents stack overflows in case multiple nodes do this.
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    durationConsumer.accept(0);
                }
            });
            return;
        }
        int nextVideo = lastPlayed + 1;
        if (nextVideo > videos.size() - 1) {
            nextVideo = 0;
        }
        currentVideo = videos.get(nextVideo);
        lastPlayed = nextVideo;
        setPlaceToGo(new VideoPlace(currentVideo, durationConsumer));
        firePlaceChangeAndStartTimer();

    }

    @Override
    public void customDurationHook(Consumer<Integer> consumer) {
        this.durationConsumer = consumer;
    }
}
