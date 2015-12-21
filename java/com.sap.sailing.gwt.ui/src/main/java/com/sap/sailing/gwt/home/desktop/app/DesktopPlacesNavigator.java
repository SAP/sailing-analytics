package com.sap.sailing.gwt.home.desktop.app;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.place.shared.PlaceController;
import com.sap.sailing.gwt.home.desktop.places.aboutus.AboutUsPlace;
import com.sap.sailing.gwt.home.desktop.places.contact.ContactPlace;
import com.sap.sailing.gwt.home.desktop.places.event.multiregatta.regattastab.MultiregattaRegattasPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.AbstractEventRegattaPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.analyticstab.RegattaCompetitorAnalyticsPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.leaderboardtab.RegattaLeaderboardPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.overviewtab.RegattaOverviewPlace;
import com.sap.sailing.gwt.home.desktop.places.sponsoring.SponsoringPlace;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.shared.app.AbstractPlaceNavigator;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.event.AbstractEventPlace;
import com.sap.sailing.gwt.home.shared.places.event.EventDefaultPlace;
import com.sap.sailing.gwt.home.shared.places.events.EventsPlace;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesDefaultPlace;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace.SolutionsNavigationTabs;
import com.sap.sailing.gwt.home.shared.places.start.StartPlace;
import com.sap.sailing.gwt.home.shared.places.user.confirmation.ConfirmationPlace;
import com.sap.sailing.gwt.home.shared.places.user.confirmation.ConfirmationPlace.Action;
import com.sap.sailing.gwt.home.shared.places.user.passwordreset.PasswordResetPlace;
import com.sap.sailing.gwt.home.shared.places.user.profile.AbstractUserProfilePlace;
import com.sap.sailing.gwt.home.shared.places.user.profile.UserProfileDefaultPlace;
import com.sap.sailing.gwt.ui.client.EntryPointLinkFactory;

public class DesktopPlacesNavigator extends AbstractPlaceNavigator {

    protected DesktopPlacesNavigator(PlaceController placeController, boolean isStandaloneServer) {
        super(placeController, isStandaloneServer);
    }

    public PlaceNavigation<StartPlace> getHomeNavigation() {
        return createGlobalPlaceNavigation(new StartPlace());
    }

    public PlaceNavigation<EventsPlace> getEventsNavigation() {
        return createGlobalPlaceNavigation(new EventsPlace());
    }

    public PlaceNavigation<SolutionsPlace> getSolutionsNavigation(SolutionsNavigationTabs navigationTab) {
        return createLocalPlaceNavigation(new SolutionsPlace(navigationTab));
    }

    public PlaceNavigation<WhatsNewPlace> getWhatsNewNavigation(WhatsNewNavigationTabs navigationTab) {
        return createLocalPlaceNavigation(new WhatsNewPlace(navigationTab));
    }

    public String getSimulatorURL() {
        Map<String, String> parameters = new HashMap<String, String>();
        return EntryPointLinkFactory.createSimulatorLink(parameters);
    }
    
    public PlaceNavigation<SponsoringPlace> getSponsoringNavigation() {
        return createGlobalPlaceNavigation(new SponsoringPlace());
    }

    public PlaceNavigation<AboutUsPlace> getAboutUsNavigation() {
        return createGlobalPlaceNavigation(new AboutUsPlace());
    }

    public PlaceNavigation<ContactPlace> getContactNavigation() {
        return createGlobalPlaceNavigation(new ContactPlace());
    }
    
    public PlaceNavigation<ConfirmationPlace> getCreateConfirmationNavigation() {
        return createGlobalPlaceNavigation(new ConfirmationPlace(Action.MAIL_VERIFIED));
    }

    public PlaceNavigation<ConfirmationPlace> getChangeMailConfirmationNavigation() {
        return createGlobalPlaceNavigation(new ConfirmationPlace(Action.CHANGED_EMAIL));
    }
    
    public PlaceNavigation<ConfirmationPlace> getPasswordResttedConfirmationNavigation(String username) {
        return createGlobalPlaceNavigation(new ConfirmationPlace(Action.RESET_SEND, username));
    }
    
    public PlaceNavigation<PasswordResetPlace> getPasswordResetNavigation() {
        return createGlobalPlaceNavigation(new PasswordResetPlace());
    }

    public PlaceNavigation<MultiregattaRegattasPlace> getEventRegattasNavigation(String eventUuidAsString, String baseUrl, boolean isOnRemoteServer) {
        MultiregattaRegattasPlace eventPlace = new MultiregattaRegattasPlace(eventUuidAsString);
        return createPlaceNavigation(baseUrl, isOnRemoteServer, eventPlace);
    }
    
    public PlaceNavigation<SeriesDefaultPlace> getEventSeriesNavigation(String seriesId, String baseUrl, boolean isOnRemoteServer) {
        SeriesDefaultPlace place = new SeriesDefaultPlace(seriesId);
        return createPlaceNavigation(baseUrl, isOnRemoteServer, place);
    }

    public PlaceNavigation<EventDefaultPlace> getEventNavigation(String eventUuidAsString, String baseUrl, boolean isOnRemoteServer) {
        EventDefaultPlace eventPlace = new EventDefaultPlace(eventUuidAsString);
        return createPlaceNavigation(baseUrl, isOnRemoteServer, eventPlace);
    }
    
    public <P extends AbstractEventPlace> PlaceNavigation<P> getEventNavigation(P place, String baseUrl,
            boolean isOnRemoteServer) {
        return createPlaceNavigation(baseUrl, isOnRemoteServer, place);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PlaceNavigation<AbstractEventRegattaPlace> getRegattaNavigation(String eventUuidAsString, String leaderboardIdAsNameString, String baseUrl, boolean isOnRemoteServer) {
        RegattaOverviewPlace eventPlace = new RegattaOverviewPlace(eventUuidAsString, leaderboardIdAsNameString);
        return (PlaceNavigation) createPlaceNavigation(baseUrl, isOnRemoteServer, eventPlace);
    }

    public PlaceNavigation<RegattaCompetitorAnalyticsPlace> getCompetitorAnalyticsNavigation(String eventUuidAsString, String regattaId, String baseUrl, boolean isOnRemoteServer) {
        RegattaCompetitorAnalyticsPlace regattaPlace = new RegattaCompetitorAnalyticsPlace(eventUuidAsString, regattaId);
        return createPlaceNavigation(baseUrl, isOnRemoteServer, regattaPlace);
    }
    
    public PlaceNavigation<RegattaLeaderboardPlace> getLeaderboardNavigation(String eventUuidAsString, String regattaId, String baseUrl, boolean isOnRemoteServer) {
        RegattaLeaderboardPlace regattaPlace = new RegattaLeaderboardPlace(eventUuidAsString, regattaId);
        return createPlaceNavigation(baseUrl, isOnRemoteServer, regattaPlace);
    }

    public PlaceNavigation<? extends AbstractUserProfilePlace> getUserProfileNavigation() {
        return createGlobalPlaceNavigation(new UserProfileDefaultPlace());
    }
}
