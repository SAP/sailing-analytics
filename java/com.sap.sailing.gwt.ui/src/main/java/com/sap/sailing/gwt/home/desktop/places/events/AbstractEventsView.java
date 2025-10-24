package com.sap.sailing.gwt.home.desktop.places.events;

import com.google.gwt.user.client.ui.Composite;
import com.sap.sailing.gwt.home.communication.eventlist.EventListViewDTO;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public abstract class AbstractEventsView extends Composite implements EventsView {
    protected EventListViewDTO eventListView;
    
    @Override
    public void setEvents(EventListViewDTO eventListView, TakedownNoticeService takedownNoticeService) {
        this.eventListView = eventListView;
        updateEventsUI(takedownNoticeService);
    }

    protected abstract void updateEventsUI(TakedownNoticeService takedownNoticeService);
}
