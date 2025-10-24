package com.sap.sailing.gwt.home.mobile.places.event.overview;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewStageContentDTO;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewStageDTO;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewTickerStageDTO;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewVideoStageDTO;
import com.sap.sailing.gwt.home.mobile.places.event.EventViewBase;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.countdown.Countdown;
import com.sap.sailing.gwt.home.shared.partials.countdown.Countdown.CountdownNavigationProvider;
import com.sap.sailing.gwt.home.shared.partials.message.Message;
import com.sap.sailing.gwt.home.shared.partials.video.Video;
import com.sap.sailing.gwt.home.shared.refresh.RefreshableWidget;

public class EventOverviewStage extends Composite implements RefreshableWidget<EventOverviewStageDTO> {
    
    private static StageUiBinder uiBinder = GWT.create(StageUiBinder.class);

    interface StageUiBinder extends UiBinder<Widget, EventOverviewStage> {
    }
    
    @UiField SimplePanel stage;
    @UiField Message message;
    
    private final EventViewBase.Presenter presenter;
    private final StageCountdownNavigationProvider countdownNavigationProvider = new StageCountdownNavigationProvider();
    private Widget lastContent;
    
    public EventOverviewStage(EventViewBase.Presenter presenter) {
        this.presenter = presenter;
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void setData(EventOverviewStageDTO stageData) {
        message.setMessage(stageData.getEventMessage());
        EventOverviewStageContentDTO data = stageData.getStageContent();
        if(data instanceof EventOverviewVideoStageDTO) {
            EventOverviewVideoStageDTO videoStageDTO = (EventOverviewVideoStageDTO) data;
            if (!(lastContent instanceof Video)
                    || ((Video) lastContent).shouldBeReplaced(videoStageDTO.getVideo().getSourceRef())) {
                lastContent = new Video(presenter.getUserService(), presenter.getEventDTO().getName());
                ((Video) lastContent).setData(videoStageDTO);
            }
        } else if (data instanceof EventOverviewTickerStageDTO) {
            if (!(lastContent instanceof Countdown)) {
                lastContent = new Countdown(countdownNavigationProvider, presenter.getUserService());
            }
            ((Countdown) lastContent).setData((EventOverviewTickerStageDTO) data);
        } else {
            lastContent = null;
        }
        stage.setWidget(lastContent);
    }

    private class StageCountdownNavigationProvider implements CountdownNavigationProvider {
        @Override
        public PlaceNavigation<?> getRegattaNavigation(String regattaName) {
            return presenter.getRegattaOverviewNavigation(regattaName);
        }
    }
}
