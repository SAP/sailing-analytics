package com.sap.sailing.gwt.home.mobile.partials.stage;

import com.google.gwt.dom.client.Style.Display;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sailing.gwt.ui.shared.start.EventStageDTO;

public class PopularEventStageTeaser extends StageTeaser {

    private final PopularEventStageTeaserBand teaserBand;

    public PopularEventStageTeaser(EventStageDTO event, MobilePlacesNavigator placeNavigator) {
        super(event);

        title.setInnerText(event.getDisplayName());
        subtitle.setInnerText(event.getLocationAndVenue());

        countdown.getStyle().setDisplay(Display.NONE);

        bandCount.setAttribute("data-bandcount", "1");
        teaserBand = new PopularEventStageTeaserBand(event, placeNavigator);
        stageTeaserBandsPanel.getElement().appendChild(teaserBand.getElement());
    }

    @Override
    protected void handleUserAction() {
        teaserBand.actionLinkClicked(null);
    }

}
