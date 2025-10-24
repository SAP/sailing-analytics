package com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.video;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayPresenterConfigured;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.security.ui.client.UserService;

public class VideoPresenterImpl extends AutoPlayPresenterConfigured<VideoPlace>
        implements VideoView.VideoPresenter {
    private VideoView view;

    public VideoPresenterImpl(VideoPlace place, AutoPlayClientFactory clientFactory,
            VideoView slide2ViewImpl) {
        super(place, clientFactory);
        this.view = slide2ViewImpl;
    }

    @Override
    public void startConfigured(AcceptsOneWidget panel) {
        view.startingWith(this, panel);
        GWT.log("Play video " + getPlace().getVideoToPlay().getTitle());
        view.playVideo(getPlace().getVideoToPlay());
    }

    @Override
    public void publishDuration(int durationInSeconds) {
        getPlace().publishDuration(durationInSeconds);
    }
    
    @Override
    public UserService getUserService() {
        return getClientFactory().getUserService();
    }
    
    @Override
    public EventDTO getEventDTO() {
        return getClientFactory().getAutoPlayCtxSignalError().getEvent();
    }
}
