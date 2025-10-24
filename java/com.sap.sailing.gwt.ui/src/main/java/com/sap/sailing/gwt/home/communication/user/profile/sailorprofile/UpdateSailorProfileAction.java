package com.sap.sailing.gwt.home.communication.user.profile.sailorprofile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileDTO;
import com.sap.sailing.server.preferences.SailorProfilePreference;
import com.sap.sailing.server.preferences.SailorProfilePreferences;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.dispatch.shared.commands.HasWriteAction;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;

/**
 * {@link SailingAction} implementation to update attributes of a sailor profile identified by its UUID for the
 * currently logged in user. Returns an updated SailorProfileDTO to bee shown on the sailor profile details page,
 * preparing the appropriate data structure.
 */
public abstract class UpdateSailorProfileAction implements SailingAction<SailorProfileDTO>, SailorProfileConverter, HasWriteAction {

    protected UUID uuid;

    public UpdateSailorProfileAction(UUID uuid) {
        this.uuid = uuid;
    }

    protected UpdateSailorProfileAction() {
    }

    @Override
    @GwtIncompatible
    public SailorProfileDTO execute(SailingDispatchContext ctx) throws DispatchException {
        CompetitorAndBoatStore store = ctx.getRacingEventService().getCompetitorAndBoatStore();

        SailorProfilePreferences prefs = ctx.getPreferenceForCurrentUser(SailorProfilePreferences.PREF_NAME);
        if (prefs == null) {
            prefs = new SailorProfilePreferences(store);
        }
        Pair<SailorProfilePreferences, SailorProfilePreference> pair = findAndUpdateCorrectPreference(store, prefs);
        prefs = pair.getA();
        ctx.setPreferenceForCurrentUser(SailorProfilePreferences.PREF_NAME, prefs);
        return pair.getB() != null
                ? convertSailorProfilePreferenceToDto(pair.getB(), store, ctx.getRacingEventService())
                : null;
    }

    @GwtIncompatible
    private Pair<SailorProfilePreferences, SailorProfilePreference> findAndUpdateCorrectPreference(
            CompetitorAndBoatStore store, SailorProfilePreferences prefs) {
        List<SailorProfilePreference> sailorProfilePreferences = new ArrayList<>();
        SailorProfilePreference sp = null;
        if (prefs == null) {
            throw new NullPointerException("no sailor profile present");
        } else {
            // 1) copy existing SailorProfilePreference objects from SailorProfilePreferences into list except the
            // changed
            // SailorProfilePreference
            // 2) add the new version of the SailorProfilePreference to the list
            // 3) add the list to a new SailorProfilePreferences
            for (SailorProfilePreference p : prefs.getSailorProfiles()) {
                if (!p.getUuid().equals(uuid)) {
                    sailorProfilePreferences.add(p);
                } else {
                    sp = updatePreference(store, p);
                    if (sp != null) {
                        sailorProfilePreferences.add(sp);
                    }
                }
            }

            // if might be a new SailorProfile (unknown UUID), so create a new preference for it
            if (sp == null) {
                sp = updatePreference(store, null);
                if (sp != null) {
                    sailorProfilePreferences.add(sp);
                }
            }

            prefs = new SailorProfilePreferences(store);
            prefs.setSailorProfiles(sailorProfilePreferences);
        }
        return new Pair<SailorProfilePreferences, SailorProfilePreference>(prefs, sp);
    }

    /**
     * @return the updated SailorProfilePreference from the original {@link p}. If this returns null, p will be removed
     */
    @GwtIncompatible
    protected SailorProfilePreference updatePreference(CompetitorAndBoatStore store, SailorProfilePreference p) {
        return null;
    }

}
