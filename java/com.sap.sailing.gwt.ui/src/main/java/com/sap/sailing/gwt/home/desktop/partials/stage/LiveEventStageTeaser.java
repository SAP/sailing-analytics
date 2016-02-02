package com.sap.sailing.gwt.home.desktop.partials.stage;

import com.sap.sailing.gwt.home.communication.start.EventStageDTO;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;

public class LiveEventStageTeaser extends StageTeaser {

    private final LiveStageTeaserBand teaserBand;

    public LiveEventStageTeaser(EventStageDTO event, DesktopPlacesNavigator placeNavigator) {
        super(event);
        title.setInnerText(event.getDisplayName());
        subtitle.setInnerText(event.getLocationAndVenue());
        countdownTimerUi.setVisible(false);
        bandCount.setAttribute("data-bandcount", "1");
        teaserBand = new LiveStageTeaserBand(event, placeNavigator);
        stageTeaserBandsPanel.getElement().appendChild(teaserBand.getElement());
    }

    @Override
    protected void handleUserAction() {
        teaserBand.actionLinkClicked(null);
    }

}
