package com.sap.sailing.gwt.home.communication.user.profile.sailorprofile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfilesDTO;
import com.sap.sailing.server.preferences.SailorProfilePreferences;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;

/**
 * {@link SailingAction} implementation to load sailor profiles for the currently logged in user to bee shown on the
 * sailor profiles overview page, preparing the appropriate data structure.
 */
public class GetAllSailorProfilesAction implements SailingAction<SailorProfilesDTO>, SailorProfileConverter {

    public GetAllSailorProfilesAction() {
    }

    @Override
    @GwtIncompatible
    public SailorProfilesDTO execute(SailingDispatchContext ctx) throws DispatchException {
        SailorProfilePreferences preferences = ctx.getPreferenceForCurrentUser(SailorProfilePreferences.PREF_NAME);
        SailorProfilesDTO result;
        List<SailorProfileDTO> list = new ArrayList<>();

        // load sailor profile from preferences if available
        if (preferences != null) {
            StreamSupport.stream(preferences.getSailorProfiles().spliterator(), false)
                    .forEach(s -> list.add(convertSailorProfilePreferenceToDto(s,
                            ctx.getRacingEventService().getCompetitorAndBoatStore(), ctx.getRacingEventService())));
        }
        result = new SailorProfilesDTO(list);
        return result;
    }

}
