package com.sap.sailing.gwt.home.mobile.partials.stage;

import com.google.gwt.event.dom.client.ClickEvent;
import com.sap.sailing.gwt.common.client.i18n.TextMessages;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sailing.gwt.home.shared.utils.EventDatesFormatterUtil;
import com.sap.sailing.gwt.ui.shared.general.EventLinkAndMetadataDTO;

/**
 * Teaser band for a live event on the homepage stage
 * @author Frank
 *
 */
public class LiveStageTeaserBand extends StageTeaserBand {

    public LiveStageTeaserBand(EventLinkAndMetadataDTO event, MobilePlacesNavigator placeNavigator) {
        super(event, placeNavigator);
 
        bandTitle.setInnerText(event.getDisplayName());
        bandSubtitle.setInnerText(EventDatesFormatterUtil.formatDateRangeWithYear(event.getStartDate(), event.getEndDate()));

        actionLink.setVisible(true);
        actionLink.setText(TextMessages.INSTANCE.watchNow());
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
