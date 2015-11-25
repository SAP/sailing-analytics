package com.sap.sailing.gwt.home.mobile.places.events;

import java.util.UUID;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.common.client.i18n.TextMessages;
import com.sap.sailing.gwt.home.communication.event.GetEventListViewAction;
import com.sap.sailing.gwt.home.communication.eventlist.EventListViewDTO;
import com.sap.sailing.gwt.home.mobile.app.MobileApplicationClientFactory;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sailing.gwt.home.mobile.places.events.EventsView.Presenter;
import com.sap.sailing.gwt.home.shared.app.ActivityCallback;
import com.sap.sailing.gwt.home.shared.partials.placeholder.Placeholder;
import com.sap.sailing.gwt.home.shared.places.events.EventsPlace;

public class EventsActivity extends AbstractActivity implements Presenter {
    private final MobileApplicationClientFactory clientFactory;
    private final EventsPlace place;

    public EventsActivity(EventsPlace place, MobileApplicationClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        this.place = place;
    }

    @Override
    public void start(final AcceptsOneWidget panel, EventBus eventBus) {
        // TODO: create mobile placeholder
        panel.setWidget(new Placeholder());
        Window.setTitle(TextMessages.INSTANCE.events());
        final EventsView view = new EventsViewImpl(this);

        clientFactory.getDispatch().execute(new GetEventListViewAction(),
                new ActivityCallback<EventListViewDTO>(clientFactory, panel) {
            @Override
            public void onSuccess(EventListViewDTO eventListView) {
                panel.setWidget(view.asWidget());
                Window.setTitle(place.getTitle());
                view.setEvents(eventListView);
            }
        });
    }

    @Override
    public MobilePlacesNavigator getNavigator() {
        return clientFactory.getNavigator();
    }

    @Override
    public void gotoTheEvent(UUID eventId) {
        clientFactory //
                .getNavigator() //
                .getEventNavigation("", "", false)//
                .goToPlace();
    }
}
