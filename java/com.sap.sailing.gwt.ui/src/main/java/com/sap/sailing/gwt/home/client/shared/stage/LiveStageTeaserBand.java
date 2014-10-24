package com.sap.sailing.gwt.home.client.shared.stage;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.sap.sailing.gwt.common.client.i18n.TextMessages;
import com.sap.sailing.gwt.home.client.app.PlaceNavigator;
import com.sap.sailing.gwt.home.client.shared.EventDatesFormatterUtil;
import com.sap.sailing.gwt.ui.shared.EventBaseDTO;

/**
 * Teaser band for a live event on the homepage stage
 * @author Frank
 *
 */
public class LiveStageTeaserBand extends StageTeaserBand {

    public LiveStageTeaserBand(EventBaseDTO event, PlaceNavigator placeNavigator) {
        super(event, placeNavigator);
 
        bandTitle.setInnerText(event.getName());
        bandSubtitle.setInnerText(EventDatesFormatterUtil.formatDateRangeWithYear(event.startDate, event.endDate));

        isLiveDiv.getStyle().setDisplay(Display.INLINE_BLOCK);
        actionLink.setVisible(true);
        actionLink.setText(TextMessages.INSTANCE.watchNow());
        actionLink.setHref(getEventNavigation().getTargetUrl());
    }

    @Override
    public void actionLinkClicked(ClickEvent e) {
        getPlaceNavigator().goToPlace(getEventNavigation());
        e.preventDefault();
    }
}
