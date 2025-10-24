package com.sap.sailing.gwt.home.shared.app;

import com.google.gwt.place.shared.PlaceController;
import com.sap.sailing.gwt.home.desktop.places.aboutus.AboutUsPlace;
import com.sap.sailing.gwt.home.desktop.places.contact.ContactPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.overviewtab.RegattaOverviewPlace;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.shared.places.event.AbstractEventPlace;
import com.sap.sailing.gwt.home.shared.places.event.EventDefaultPlace;
import com.sap.sailing.gwt.home.shared.places.events.EventsPlace;
import com.sap.sailing.gwt.home.shared.places.fakeseries.AbstractSeriesPlace;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesContext;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesDefaultPlace;
import com.sap.sailing.gwt.home.shared.places.imprint.ImprintPlace;
import com.sap.sailing.gwt.home.shared.places.morelogininformation.MoreLoginInformationPlace;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace.SolutionsNavigationTabs;
import com.sap.sailing.gwt.home.shared.places.start.StartPlace;
import com.sap.sailing.gwt.home.shared.places.subscription.SubscriptionPlace;
import com.sap.sailing.gwt.home.shared.places.user.confirmation.ConfirmationPlace;
import com.sap.sailing.gwt.home.shared.places.user.confirmation.ConfirmationPlace.Action;
import com.sap.sailing.gwt.home.shared.places.user.passwordreset.PasswordResetPlace;
import com.sap.sailing.gwt.home.shared.places.user.profile.AbstractUserProfilePlace;
import com.sap.sailing.gwt.home.shared.places.user.profile.UserProfileDefaultPlace;
import com.sap.sailing.gwt.home.shared.places.user.profile.preferences.UserProfilePreferencesPlace;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.SailorProfilePlace;
import com.sap.sailing.gwt.home.shared.places.user.profile.settings.UserProfileSettingsPlace;
import com.sap.sailing.gwt.home.shared.places.user.profile.subscriptions.UserProfileSubscriptionsPlace;

public class HomePlacesNavigator extends AbstractPlaceNavigator {

    protected HomePlacesNavigator(PlaceController placeController, boolean isStandaloneServer) {
        super(placeController, isStandaloneServer);
    }

    public PlaceNavigation<StartPlace> getHomeNavigation() {
        return createGlobalPlaceNavigation(new StartPlace());
    }

    public PlaceNavigation<EventsPlace> getEventsNavigation() {
        return createGlobalPlaceNavigation(new EventsPlace());
    }
    
    public PlaceNavigation<MoreLoginInformationPlace> getMoreLoginInfo() {
        return createGlobalPlaceNavigation(new MoreLoginInformationPlace());
    }

    public PlaceNavigation<SolutionsPlace> getSolutionsNavigation(SolutionsNavigationTabs navigationTab) {
        return createLocalPlaceNavigation(new SolutionsPlace(navigationTab));
    }
    
    public PlaceNavigation<SubscriptionPlace> getSubscriptionsNavigation() {
        return createLocalPlaceNavigation(new SubscriptionPlace());
    }

    public PlaceNavigation<WhatsNewPlace> getWhatsNewNavigation(WhatsNewNavigationTabs navigationTab) {
        return createLocalPlaceNavigation(new WhatsNewPlace(navigationTab));
    }

    public PlaceNavigation<AboutUsPlace> getAboutUsNavigation() {
        return createGlobalPlaceNavigation(new AboutUsPlace());
    }

    public PlaceNavigation<ContactPlace> getContactNavigation() {
        return createGlobalPlaceNavigation(new ContactPlace());
    }

    public PlaceNavigation<ImprintPlace> getImprintNavigation() {
        return createGlobalPlaceNavigation(new ImprintPlace((String) null /* empty place token */));
    }

    public PlaceNavigation<EventDefaultPlace> getEventNavigation(String eventUuidAsString, String baseUrl,
            boolean isOnRemoteServer) {
        EventDefaultPlace eventPlace = new EventDefaultPlace(eventUuidAsString);
        return createPlaceNavigation(baseUrl, isOnRemoteServer, eventPlace);
    }

    public <P extends AbstractEventPlace> PlaceNavigation<P> getEventNavigation(P place, String baseUrl,
            boolean isOnRemoteServer) {
        return createPlaceNavigation(baseUrl, isOnRemoteServer, place);
    }

    public PlaceNavigation<SeriesDefaultPlace> getEventSeriesNavigation(SeriesContext ctx, String baseUrl,
            boolean isOnRemoteServer) {
        SeriesDefaultPlace place = new SeriesDefaultPlace(ctx);
        return createPlaceNavigation(baseUrl, isOnRemoteServer, place);
    }

    public <P extends AbstractSeriesPlace> PlaceNavigation<P> getSeriesNavigation(P place, String baseUrl,
            boolean isOnRemoteServer) {
        return createPlaceNavigation(baseUrl, isOnRemoteServer, place);
    }
    
    public PlaceNavigation<RegattaOverviewPlace> getRegattaNavigation(String eventUuidAsString,
            String leaderboardIdAsNameString, String baseUrl, boolean isOnRemoteServer) {
        return getEventNavigation(new RegattaOverviewPlace(eventUuidAsString, leaderboardIdAsNameString), baseUrl,
                isOnRemoteServer);
    }

    public PlaceNavigation<ConfirmationPlace> getMailVerifiedConfirmationNavigation() {
        return createLocalPlaceNavigation(new ConfirmationPlace(Action.MAIL_VERIFIED, null, null));
    }

    public PlaceNavigation<ConfirmationPlace> getPasswordResettedConfirmationNavigation(String username) {
        return createLocalPlaceNavigation(new ConfirmationPlace(Action.RESET_EXECUTED, username));
    }

    public PlaceNavigation<PasswordResetPlace> getPasswordResetNavigation() {
        return createLocalPlaceNavigation(new PasswordResetPlace());
    }

    public PlaceNavigation<? extends AbstractUserProfilePlace> getUserProfileNavigation() {
        return createLocalPlaceNavigation(new UserProfileDefaultPlace());
    }

    public PlaceNavigation<? extends AbstractUserProfilePlace> getUserPreferencesNavigation() {
        return createLocalPlaceNavigation(new UserProfilePreferencesPlace());
    }
    
    public PlaceNavigation<? extends AbstractUserProfilePlace> getUserSettingsNavigation() {
        return createLocalPlaceNavigation(new UserProfileSettingsPlace());
    }

    public PlaceNavigation<? extends AbstractUserProfilePlace> getSailorProfilesNavigation() {
        return createLocalPlaceNavigation(new SailorProfilePlace());
    }

    public PlaceNavigation<? extends AbstractUserProfilePlace> getUserSubscriptionsNavigation() {
        return createLocalPlaceNavigation(new UserProfileSubscriptionsPlace());
    }

}
