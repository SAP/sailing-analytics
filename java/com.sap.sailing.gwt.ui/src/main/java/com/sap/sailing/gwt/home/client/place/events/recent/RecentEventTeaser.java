package com.sap.sailing.gwt.home.client.place.events.recent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.client.shared.event.EventTeaser;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.ui.shared.eventlist.EventListEventSeriesDTO;
import com.sap.sailing.gwt.ui.shared.general.EventMetadataDTO;
import com.sap.sailing.gwt.ui.shared.general.LabelType;

public class RecentEventTeaser extends Composite {

    private static RecentEventTeaserUiBinder uiBinder = GWT.create(RecentEventTeaserUiBinder.class);

    interface RecentEventTeaserUiBinder extends UiBinder<Widget, RecentEventTeaser> {
    }
    
    @UiField(provided = true) EventTeaser eventTeaser;

    public RecentEventTeaser(final PlaceNavigation<?> placeNavigation, final EventMetadataDTO event, LabelType labelType) {
        eventTeaser = new EventTeaser(placeNavigation, event, labelType);
        initWidget(uiBinder.createAndBindUi(this));
    }
    
    public void setSeriesInformation(PlaceNavigation<?> seriesNavigation, EventListEventSeriesDTO eventSeries) {
        eventTeaser.setSeriesInformation(seriesNavigation, eventSeries);
    }

    public void hideImage(boolean hide) {
        eventTeaser.hideImage(hide);
    }
}
