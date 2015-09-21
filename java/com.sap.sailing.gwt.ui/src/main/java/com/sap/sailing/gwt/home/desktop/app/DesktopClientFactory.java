package com.sap.sailing.gwt.home.desktop.app;

import com.sap.sailing.gwt.home.client.place.aboutus.AboutUsClientFactory;
import com.sap.sailing.gwt.home.client.place.contact.ContactClientFactory;
import com.sap.sailing.gwt.home.client.place.error.ErrorClientFactory;
import com.sap.sailing.gwt.home.client.place.event.EventClientFactory;
import com.sap.sailing.gwt.home.client.place.event.legacy.SeriesClientFactory;
import com.sap.sailing.gwt.home.client.place.events.EventsClientFactory;
import com.sap.sailing.gwt.home.client.place.searchresult.SearchResultClientFactory;
import com.sap.sailing.gwt.home.client.place.solutions.SolutionsClientFactory;
import com.sap.sailing.gwt.home.client.place.sponsoring.SponsoringClientFactory;
import com.sap.sailing.gwt.home.client.place.start.StartClientFactory;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewClientFactory;

public interface DesktopClientFactory extends AboutUsClientFactory, ContactClientFactory, EventClientFactory,
    EventsClientFactory, StartClientFactory, SponsoringClientFactory, SolutionsClientFactory, SearchResultClientFactory,
 WhatsNewClientFactory, SeriesClientFactory, ErrorClientFactory {
    DesktopPlacesNavigator getHomePlacesNavigator();
}
