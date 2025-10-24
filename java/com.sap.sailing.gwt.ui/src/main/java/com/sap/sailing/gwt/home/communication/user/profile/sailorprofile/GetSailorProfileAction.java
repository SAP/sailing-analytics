package com.sap.sailing.gwt.home.communication.user.profile.sailorprofile;

import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileDTO;
import com.sap.sailing.server.preferences.SailorProfilePreference;
import com.sap.sailing.server.preferences.SailorProfilePreferences;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;

/**
 * {@link SailingAction} implementation to load a sailor profile with a specific uuid for the currently logged in user
 * to bee shown on the sailor profile details page, preparing the appropriate data structure.
 */
public class GetSailorProfileAction implements SailingAction<SailorProfileDTO>, SailorProfileConverter {

    private UUID uuid;

    public GetSailorProfileAction(UUID uuid) {
        this.uuid = uuid;
    }

    protected GetSailorProfileAction() {
    }

    @Override
    @GwtIncompatible
    public SailorProfileDTO execute(SailingDispatchContext ctx) throws DispatchException {
        CompetitorAndBoatStore store = ctx.getRacingEventService().getCompetitorAndBoatStore();
        SailorProfilePreferences prefs = ctx.getPreferenceForCurrentUser(SailorProfilePreferences.PREF_NAME);
        SailorProfilePreference pref = findSailorProfile(prefs);
        return convertSailorProfilePreferenceToDto(pref, store, ctx.getRacingEventService());
    }

    /** find the SailorProfilePreference in SailorProfilePreferences from the {@link #uuid} */
    @GwtIncompatible
    private SailorProfilePreference findSailorProfile(SailorProfilePreferences prefs) {
        SailorProfilePreference result = null;
        if (prefs != null) {
            for (SailorProfilePreference p : prefs.getSailorProfiles()) {
                if (p.getUuid().equals(uuid)) {
                    result = p;
                    break;
                }
            }
        }
        return result;
    }
}
