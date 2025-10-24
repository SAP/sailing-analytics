package com.sap.sailing.gwt.home.desktop.partials.stage;

import com.sap.sailing.gwt.home.communication.start.EventStageDTO;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class PopularEventStageTeaser extends StageTeaser {

    private final PopularEventStageTeaserBand teaserBand;
    
    public PopularEventStageTeaser(EventStageDTO event, DesktopPlacesNavigator placeNavigator, TakedownNoticeService takedownNoticeService) {
        super(event, takedownNoticeService);
        title.setInnerText(event.getDisplayName());
        subtitle.setInnerText(event.getLocationAndVenue());
        countdownTimerUi.setVisible(false);
        bandCount.setAttribute("data-bandcount", "1");
        teaserBand = new PopularEventStageTeaserBand(event, placeNavigator);
        stageTeaserBandsPanel.getElement().appendChild(teaserBand.getElement());
    }

    @Override
    protected void handleUserAction() {
        teaserBand.actionLinkClicked(null);
    }

}
