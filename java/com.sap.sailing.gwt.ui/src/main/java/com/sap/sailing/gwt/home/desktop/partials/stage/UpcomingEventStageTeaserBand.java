package com.sap.sailing.gwt.home.desktop.partials.stage;

import com.google.gwt.event.dom.client.ClickEvent;
import com.sap.sailing.gwt.home.communication.start.EventStageDTO;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.shared.utils.EventDatesFormatterUtil;
import com.sap.sailing.gwt.ui.client.StringMessages;

/**
 * Teaser band for an upcoming event on the homepage stage
 * @author Frank
 *
 */
public class UpcomingEventStageTeaserBand extends StageTeaserBand {

    public UpcomingEventStageTeaserBand(EventStageDTO event, DesktopPlacesNavigator placeNavigator) {
        super(event, placeNavigator);
 
        bandTitle.setInnerText(event.getDisplayName());
        bandSubtitle.setInnerText(EventDatesFormatterUtil.formatDateRangeWithYear(event.getStartDate(), event.getEndDate()));

        actionLink.setVisible(true);
        actionLink.setText(StringMessages.INSTANCE.moreInfo());
        actionLink.setHref(getEventNavigation().getTargetUrl());
    }

    @Override
    public void actionLinkClicked(ClickEvent e) {
        getPlaceNavigator().goToPlace(getEventNavigation());
        if (e != null) {
            e.preventDefault();
        }
    }
}
