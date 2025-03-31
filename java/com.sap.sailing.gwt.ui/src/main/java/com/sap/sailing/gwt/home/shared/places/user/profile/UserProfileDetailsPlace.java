package com.sap.sailing.gwt.home.shared.places.user.profile;

import com.google.gwt.place.shared.Prefix;
import com.sap.sailing.gwt.common.client.navigation.PlaceTokenPrefixes;
import com.sap.sailing.gwt.home.shared.app.HasMobileVersion;


public class UserProfileDetailsPlace extends AbstractUserProfilePlace implements HasMobileVersion {

    @Prefix(PlaceTokenPrefixes.UserProfileDetails)
    public static class Tokenizer extends AbstractUserProfilePlace.Tokenizer<UserProfileDetailsPlace> {
        @Override
        protected UserProfileDetailsPlace getRealPlace() {
            return new UserProfileDetailsPlace();
        }
    }
}
