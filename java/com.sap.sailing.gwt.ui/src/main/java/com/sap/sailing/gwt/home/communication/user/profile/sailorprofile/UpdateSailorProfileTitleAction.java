package com.sap.sailing.gwt.home.communication.user.profile.sailorprofile;

import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.server.preferences.SailorProfilePreference;

/**
 * {@link UpdateSailorProfileAction} implementation to load update the title of a sailor profile with the UUID for the
 * currently logged in user.
 */
public class UpdateSailorProfileTitleAction extends UpdateSailorProfileAction {

    private String title;

    public UpdateSailorProfileTitleAction(UUID uuid, String title) {
        super(uuid);
        this.title = title;
    }

    protected UpdateSailorProfileTitleAction() {
    }

    @GwtIncompatible
    @Override
    protected SailorProfilePreference updatePreference(CompetitorAndBoatStore store, SailorProfilePreference p) {
        return new SailorProfilePreference(store, p, this.title);
    }

}
