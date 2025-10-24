package com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.video;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.security.ui.client.UserService;

public interface VideoView {

    void startingWith(VideoPresenter p, AcceptsOneWidget panel);

    void playVideo(VideoDTO video);

    public interface VideoPresenter {
        void publishDuration(int duration);

        UserService getUserService();

        EventDTO getEventDTO();
    }

}
