package com.sap.sailing.gwt.home.communication.user.profile;

import java.util.TreeSet;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.server.impl.preferences.model.BoatClassNotificationPreference;
import com.sap.sailing.server.impl.preferences.model.BoatClassNotificationPreferences;
import com.sap.sailing.server.impl.preferences.model.CompetitorNotificationPreference;
import com.sap.sailing.server.impl.preferences.model.CompetitorNotificationPreferences;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;

/**
 * {@link SailingAction} implementation to load favorites for the currently logged in user to bee shown on the
 * preferences page, preparing the appropriate data structure.
 */
public class GetFavoritesAction implements SailingAction<FavoritesResult> {

    @Override
    @GwtIncompatible
    public FavoritesResult execute(SailingDispatchContext ctx) throws DispatchException {
        return new FavoritesResult(getFavoriteBoatClasses(ctx), getFavoriteCompetitors(ctx));
    }

    @GwtIncompatible
    private FavoriteBoatClassesDTO getFavoriteBoatClasses(SailingDispatchContext ctx) {
        BoatClassNotificationPreferences preferences = ctx
                .getPreferenceForCurrentUser(BoatClassNotificationPreferences.PREF_NAME);
        TreeSet<BoatClassDTO> selected = new TreeSet<>();
        boolean notifyAboutUpcomingRaces = false, notifyAboutResults = false;
        if (preferences != null) {
            for (BoatClassNotificationPreference pref : preferences.getBoatClasses()) {
                String name = pref.getBoatClass().getName();
                selected.add(new BoatClassDTO(name, pref.getBoatClass().getHullLength(), pref.getBoatClass().getHullBeam()));
                notifyAboutUpcomingRaces |= pref.isNotifyAboutUpcomingRaces();
                notifyAboutResults |= pref.isNotifyAboutResults();
            }
        }
        return new FavoriteBoatClassesDTO(selected, notifyAboutUpcomingRaces, notifyAboutResults);
    }
    
    @GwtIncompatible
    private FavoriteCompetitorsDTO getFavoriteCompetitors(SailingDispatchContext ctx) {
        CompetitorNotificationPreferences preferences = ctx
                .getPreferenceForCurrentUser(CompetitorNotificationPreferences.PREF_NAME);
        TreeSet<SimpleCompetitorWithIdDTO> selected = new TreeSet<>();
        boolean notifyAboutResults = false;
        if (preferences != null) {
            for (CompetitorNotificationPreference pref : preferences.getCompetitors()) {
                final String competitorId = pref.getCompetitorId();
                final RacingEventService racingEventService = ctx.getRacingEventService();
                final CompetitorAndBoatStore competitorAndBoatStore = racingEventService.getCompetitorAndBoatStore();
                DynamicCompetitor competitor = competitorAndBoatStore.getExistingCompetitorByIdAsString(competitorId);
                selected.add(competitor == null ?
                        new SimpleCompetitorWithIdDTO(competitorId,"Not found","ID:" + competitorId, null, null) 
                        : new SimpleCompetitorWithIdDTO(competitor));
                notifyAboutResults |= pref.isNotifyAboutResults();
            }
        }
        return new FavoriteCompetitorsDTO(selected, notifyAboutResults);
    }

}
