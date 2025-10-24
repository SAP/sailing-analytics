package com.sap.sailing.gwt.home.communication.user.profile.sailorprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.ParticipatedEventDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.ParticipatedRegattaDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileEventsDTO;
import com.sap.sailing.server.preferences.SailorProfilePreference;
import com.sap.sailing.server.preferences.SailorProfilePreferences;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;

/**
 * {@link SailingAction} implementation to load all events, the competitors in a sailor profile with a specific uuid for
 * the currently logged in user have participated in to be shown on the sailor profile details page in the events
 * container.
 */
public class GetEventsForSailorProfileAction implements SailingAction<SailorProfileEventsDTO>, SailorProfileConverter {

    private UUID uuid;

    public GetEventsForSailorProfileAction(UUID uuid) {
        this.uuid = uuid;
    }

    public GetEventsForSailorProfileAction() {
    }

    @Override
    @GwtIncompatible
    public SailorProfileEventsDTO execute(SailingDispatchContext ctx) throws DispatchException {
        final CompetitorAndBoatStore store = ctx.getRacingEventService().getCompetitorAndBoatStore();
        final SailorProfilePreferences prefs = ctx.getPreferenceForCurrentUser(SailorProfilePreferences.PREF_NAME);
        final SailorProfilePreference pref = findSailorProfile(store, prefs);
        final Collection<ParticipatedEventDTO> participatedEvents = new ArrayList<>();
        // iterate over all existing events
        for (Event event : ctx.getRacingEventService().getAllEvents()) {
            Collection<ParticipatedRegattaDTO> participatedRegattas = new ArrayList<>();
            // iterate over the leaderboard groups
            for (LeaderboardGroup leaderboardGroup : event.getLeaderboardGroups()) {
                if (leaderboardGroup.hasOverallLeaderboard()) {
                    leaderboardGroup.getOverallLeaderboard();
                }
                // iterate over the leaderboards in each leaderboard group
                for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    // check if this leaderboard contains at least one of the selected competitors
                    final Collection<Competitor> containedCompetitors = new ArrayList<>();
                    for (Competitor competitor : pref.getCompetitors()) {
                        if (leaderboard.getCompetitors() != null
                                && Util.contains(leaderboard.getCompetitors(), competitor)) {
                            containedCompetitors.add(competitor);
                        }
                    }
                    // skip if none of the selected competitors is in this leaderboard
                    if (containedCompetitors.size() == 0) {
                        continue;
                    }
                    // create and add ParticipatedRegattaDTO for each of the selected competitors who was in this
                    // leaderboard
                    for (Competitor competitor : containedCompetitors) {
                        int rank = leaderboard.getTotalRankOfCompetitor(competitor, MillisecondsTimePoint.now());
                        // final String leaderboardName = leaderboard.getDisplayName();
                        final double points = leaderboard.getNetPoints(competitor, MillisecondsTimePoint.now());
                        // regatta name is equal to the leaderboard name
                        final String regattaName = leaderboard.getName();
                        final Regatta regatta = ctx.getRacingEventService().getRegattaByName(regattaName);
                        if (regatta == null) {
                            continue;
                        }
                        // skip, if the regatta is not part of this event (e.g. shared leaderboard group)
                        if (!leaderboard.isPartOfEvent(event)) {
                            // skip, if overall leaderboard is not part of this event, don't skip if this is a regatta
                            // during an event which is not part of an overall leaderboard
                            if (leaderboardGroup.hasOverallLeaderboard()
                                    && !leaderboardGroup.getOverallLeaderboard().isPartOfEvent(event))
                                continue;
                        }
                        participatedRegattas.add(
                                new ParticipatedRegattaDTO(regattaName, rank, new SimpleCompetitorWithIdDTO(competitor),
                                        "" + regatta.getId(), "" + event.getId(), points));
                    }
                }
            }
            if (participatedRegattas.size() > 0) {
                participatedEvents
                        .add(new ParticipatedEventDTO(event.getName(), event.getId().toString(), participatedRegattas));
            }
        }
        return new SailorProfileEventsDTO(participatedEvents);
    }

    /** @return find SailorProfilePreference in SailorProfilePreferences by using {@link #uuid} */
    @GwtIncompatible
    private SailorProfilePreference findSailorProfile(CompetitorAndBoatStore store, SailorProfilePreferences prefs) {
        if (prefs == null) {
            throw new NullPointerException("no sailor profile present");
        } else {
            for (SailorProfilePreference p : prefs.getSailorProfiles()) {
                if (p.getUuid().equals(uuid)) {
                    return p;
                }
            }
            return null;
        }
    }
}
