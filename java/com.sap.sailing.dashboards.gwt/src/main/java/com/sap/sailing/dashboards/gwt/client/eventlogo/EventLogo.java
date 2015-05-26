package com.sap.sailing.dashboards.gwt.client.eventlogo;

import java.util.UUID;

import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.shared.EventDTO;

public class EventLogo extends Image {

    protected EventLogo() {
        super();
        this.getElement().addClassName("eventLogo");
    }

    public static EventLogo getEventLogoFromEventId(SailingServiceAsync sailingServiceAsync, String eventId) {
        final EventLogo eventLogo = new EventLogo();
        try {
            final UUID eventUUID = UUID.fromString(eventId);
            sailingServiceAsync.getEventById(eventUUID, true, new AsyncCallback<EventDTO>() {
                @Override
                public void onSuccess(final EventDTO event) {
                    if (event != null) {
                        final String logoUrl = event.getLogoImageURL();
                        if (logoUrl != null) {
                            eventLogo.getElement().setAttribute("src", logoUrl);
                        } else {
                            eventLogo.getElement().getStyle().setVisibility(Visibility.HIDDEN);
                        }
                        if (event.getName() != null) {
                            Window.setTitle(event.getName() + " Dashboard");
                        }
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    eventLogo.getElement().getStyle().setVisibility(Visibility.HIDDEN);
                }
            });
        } catch (IllegalArgumentException e) {
            eventLogo.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        }
        return eventLogo;
    }
}
