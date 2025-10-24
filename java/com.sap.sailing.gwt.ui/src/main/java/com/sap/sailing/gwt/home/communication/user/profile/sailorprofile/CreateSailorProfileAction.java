package com.sap.sailing.gwt.home.communication.user.profile.sailorprofile;

import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.server.preferences.SailorProfilePreference;

/**
 * {@link UpdateSailorProfileAction} implementation to create a new sailor profile for the currently logged in user with
 * a UUID and a name.
 */
public class CreateSailorProfileAction extends UpdateSailorProfileAction {

    private String name;

    public CreateSailorProfileAction(UUID uuid, String name) {
        super(uuid);
        this.name = name;
    }

    protected CreateSailorProfileAction() {
    }

    @GwtIncompatible
    @Override
    protected SailorProfilePreference updatePreference(CompetitorAndBoatStore store, SailorProfilePreference p) {
        return new SailorProfilePreference(store, uuid, name);
    }

}
