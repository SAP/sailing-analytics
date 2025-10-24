package com.sap.sailing.gwt.home.communication.user.profile.sailorprofile;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.server.preferences.SailorProfilePreference;
import com.sap.sse.common.Util;

/**
 * {@link UpdateSailorProfileAction} implementation to update the competitors of a sailor profile identified by the UUID
 * for the currently logged in user.
 */
public class UpdateSailorProfileCompetitorsAction extends UpdateSailorProfileAction {

    private ArrayList<SimpleCompetitorWithIdDTO> competitors = new ArrayList<>();

    public UpdateSailorProfileCompetitorsAction(UUID uuid, Iterable<SimpleCompetitorWithIdDTO> competitors) {
        super(uuid);
        Util.addAll(competitors, this.competitors);
    }

    protected UpdateSailorProfileCompetitorsAction() {
    }

    /** convert SimpleCompetitorIdWithDTOs to Competitors */
    @GwtIncompatible
    private Iterable<Competitor> convertCompetitorDTOs(final Iterable<SimpleCompetitorWithIdDTO> comps,
            CompetitorAndBoatStore store) {
        return StreamSupport.stream(comps.spliterator(), false)
                .map(c -> store.getExistingCompetitorByIdAsString(c.getIdAsString())).collect(Collectors.toList());
    }

    @GwtIncompatible
    @Override
    protected SailorProfilePreference updatePreference(CompetitorAndBoatStore store, SailorProfilePreference p) {
        return new SailorProfilePreference(store, p, convertCompetitorDTOs(this.competitors, store));
    }
}
