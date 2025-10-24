package com.sap.sailing.gwt.home.mobile.partials.stage;

import com.sap.sailing.gwt.home.communication.event.EventLinkAndMetadataDTO;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class LiveEventStageTeaser extends StageTeaser {

    private final LiveStageTeaserBand teaserBand;

    public LiveEventStageTeaser(EventLinkAndMetadataDTO event, MobilePlacesNavigator placeNavigator, TakedownNoticeService takedownNoticeService) {
        super(event, takedownNoticeService);
        title.setInnerText(event.getDisplayName());
        subtitle.setInnerText(event.getLocationAndVenue());
        bandCount.setAttribute("data-bandcount", "1");
        teaserBand = new LiveStageTeaserBand(event, placeNavigator);
        stageTeaserBandsPanel.getElement().appendChild(teaserBand.getElement());
    }

    @Override
    protected void handleUserAction() {
        teaserBand.actionLinkClicked(null);
    }

}
