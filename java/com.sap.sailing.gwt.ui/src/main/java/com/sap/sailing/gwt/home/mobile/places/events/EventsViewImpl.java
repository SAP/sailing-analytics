package com.sap.sailing.gwt.home.mobile.places.events;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.eventlist.EventListEventDTO;
import com.sap.sailing.gwt.home.communication.eventlist.EventListViewDTO;
import com.sap.sailing.gwt.home.communication.eventlist.EventListYearDTO;
import com.sap.sailing.gwt.home.mobile.partials.recents.EventsOverviewRecent;
import com.sap.sailing.gwt.home.mobile.partials.upcoming.EventsOverviewUpcoming;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class EventsViewImpl extends Composite implements EventsView {
    private static StartPageViewUiBinder uiBinder = GWT.create(StartPageViewUiBinder.class);

    interface StartPageViewUiBinder extends UiBinder<Widget, EventsViewImpl> {
    }

    @UiField(provided = true)
    protected EventsOverviewUpcoming upcomingUi;
    @UiField(provided = true)
    protected EventsOverviewRecent recentsUi;

    public EventsViewImpl(Presenter presenter) {
        upcomingUi = new EventsOverviewUpcoming(presenter.getNavigator());
        recentsUi = new EventsOverviewRecent(presenter.getNavigator());
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void setEvents(EventListViewDTO eventListView, TakedownNoticeService takedownNoticeService) {
        ArrayList<EventListEventDTO> upcomingEvents = eventListView.getUpcomingEvents();
        upcomingUi.updateEvents(upcomingEvents);
        ArrayList<EventListYearDTO> recentEvents = eventListView.getRecentEvents();
        recentsUi.updateEvents(recentEvents, takedownNoticeService);
    }
}
