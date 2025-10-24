package com.sap.sailing.gwt.home.desktop.partials.eventstage;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.common.client.SharedResources.MediaCss;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewStageContentDTO;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewStageDTO;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewTickerStageDTO;
import com.sap.sailing.gwt.home.communication.event.eventoverview.EventOverviewVideoStageDTO;
import com.sap.sailing.gwt.home.communication.event.eventoverview.GetEventOverviewStageAction;
import com.sap.sailing.gwt.home.communication.event.news.GetEventOverviewNewsAction;
import com.sap.sailing.gwt.home.communication.event.news.NewsEntryDTO;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO;
import com.sap.sailing.gwt.home.desktop.partials.updates.UpdatesBox;
import com.sap.sailing.gwt.home.desktop.places.event.EventView;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.countdown.Countdown;
import com.sap.sailing.gwt.home.shared.partials.countdown.Countdown.CountdownNavigationProvider;
import com.sap.sailing.gwt.home.shared.partials.message.Message;
import com.sap.sailing.gwt.home.shared.partials.video.Video;
import com.sap.sailing.gwt.home.shared.refresh.RefreshManager;
import com.sap.sailing.gwt.home.shared.refresh.RefreshableWidget;
import com.sap.sse.gwt.dispatch.shared.commands.ListResult;

public class EventOverviewStage extends Composite {
    
    private final RefreshableWidget<EventOverviewStageDTO> refreshable = new RefreshableWidget<EventOverviewStageDTO>() {
        @Override
        public void setData(EventOverviewStageDTO data) {
            setStageData(data);
        }
    };
    private final RefreshableWidget<ListResult<NewsEntryDTO>> newsRefreshable = new RefreshableWidget<ListResult<NewsEntryDTO>>() {
        @Override
        public void setData(ListResult<NewsEntryDTO> data) {
            setNews(data.getValues());
        }
    };
    
    private static StageUiBinder uiBinder = GWT.create(StageUiBinder.class);

    interface StageUiBinder extends UiBinder<Widget, EventOverviewStage> {
    }
    
    private final MediaCss mediaCss = SharedResources.INSTANCE.mediaCss();
    
    @UiField SimplePanel stage;
    @UiField Message message;
    @UiField DivElement updatesWrapperUi;
    @UiField(provided = true) UpdatesBox updatesUi;
    
    private Widget lastContent;

    private final EventView.Presenter presenter;
    private final StageCountdownNavigationProvider countdownNavigationProvider = new StageCountdownNavigationProvider();
    private RefreshManager refreshManager;
    
    public EventOverviewStage(EventView.Presenter presenter) {
        this.presenter = presenter;
        updatesUi = new UpdatesBox(presenter);
        initWidget(uiBinder.createAndBindUi(this));
    }
    
    public void setupRefresh(RefreshManager refreshManager) {
        this.refreshManager = refreshManager;
        EventViewDTO eventDTO = presenter.getEventDTO();
        refreshManager.add(refreshable, new GetEventOverviewStageAction(eventDTO.getId(), false));
        if (presenter.getEventDTO().getState() == EventState.RUNNING) {
            refreshManager.add(newsRefreshable, new GetEventOverviewNewsAction(presenter.getEventDTO().getId(), 15));
        } else {
            updatesUi.removeFromParent();
            updatesWrapperUi.removeFromParent();
        }
    }

    private void setStageData(EventOverviewStageDTO stageData) {
        message.setMessage(stageData.getEventMessage());
        EventOverviewStageContentDTO data = stageData.getStageContent();
        if (data instanceof EventOverviewVideoStageDTO) {
            if (!(lastContent instanceof Video) || ((Video) lastContent).shouldBeReplaced(((EventOverviewVideoStageDTO) data).getVideo().getSourceRef())) {
                lastContent = new Video(presenter.getUserService(), presenter.getEventDTO().getName());
                ((Video) lastContent).setData((EventOverviewVideoStageDTO) data);
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
    
    private void setNews(List<NewsEntryDTO> news) {
        if (lastContent == null) {
            setStageData(new EventOverviewStageDTO(null, new EventOverviewTickerStageDTO(null, null, null)));
        }
        if (news == null || news.isEmpty()) {
            updatesWrapperUi.getStyle().setDisplay(Display.NONE);
            stage.removeStyleName(mediaCss.medium7());
            stage.removeStyleName(mediaCss.large8());
        } else {
            updatesWrapperUi.getStyle().clearDisplay();
            updatesUi.setData(news, refreshManager.getDispatchSystem().getCurrentServerTime());
            stage.addStyleName(mediaCss.medium7());
            stage.addStyleName(mediaCss.large8());
        }
    }
    
    private class StageCountdownNavigationProvider implements CountdownNavigationProvider {
        @Override
        public PlaceNavigation<?> getRegattaNavigation(String regattaName) {
            return presenter.getRegattaNavigation(regattaName);
        }
    }
}
