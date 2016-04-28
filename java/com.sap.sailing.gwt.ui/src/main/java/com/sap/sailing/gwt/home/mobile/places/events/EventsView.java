package com.sap.sailing.gwt.home.mobile.places.events;

import java.util.UUID;

import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.eventlist.EventListViewDTO;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;

public interface EventsView {

    Widget asWidget();

    void setEvents(EventListViewDTO eventListView);
    
    public interface Presenter {
        void gotoTheEvent(UUID eventId);

        MobilePlacesNavigator getNavigator();
    }

}

