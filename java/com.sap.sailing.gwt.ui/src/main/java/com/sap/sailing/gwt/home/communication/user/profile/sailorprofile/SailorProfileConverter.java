package com.sap.sailing.gwt.home.communication.user.profile.sailorprofile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.BadgeDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileDTO;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.impl.preferences.model.SailorProfilePreference;

public interface SailorProfileConverter {
    /**
     * @param notFoundOnServer
     * @return converted SailorProfileDTO from SailorProfilePreference
     */
    @GwtIncompatible
    default SailorProfileDTO convertSailorProfilePreferenceToDto(final SailorProfilePreference pref,
            final CompetitorAndBoatStore store, RacingEventService racingEventService) {
        SailorProfileDTO result;
        if (pref == null) {
            result = new SailorProfileDTO(true);
        } else {
            result = new SailorProfileDTO(pref.getUuid(), pref.getName(),
                    convertCompetitorsToDTOs(pref.getCompetitors()), new ArrayList<BadgeDTO>(),
                    getCorrespondingBoatDTOs(pref.getCompetitors(), store, racingEventService));
        }
        return result;

    }

    /** @return converted list of SimpleCompetitorWithIdDTOs from Competitors */
    @GwtIncompatible
    default List<SimpleCompetitorWithIdDTO> convertCompetitorsToDTOs(final Iterable<Competitor> comps) {
        return StreamSupport.stream(comps.spliterator(), false).filter(c -> c != null)
                .map(c -> new SimpleCompetitorWithIdDTO(c)).collect(Collectors.toList());
    }

    /** get the corresponding boat for each competitor in the list of competitors */
    @GwtIncompatible
    default Set<BoatClassDTO> getCorrespondingBoatDTOs(final Iterable<Competitor> comps,
            final CompetitorAndBoatStore store, RacingEventService racingEventService) {
        final Set<BoatClassDTO> boatclasses = new HashSet<>();
        for (Competitor c : comps) {
            if (c != null) {
                CompetitorWithBoat cwd = store.getExistingCompetitorWithBoatById(c.getId());
                if (cwd != null && cwd.hasBoat() && cwd.getBoat().getBoatClass() != null) {
                    BoatClassDTO dto = convertBoatClassToDTO(cwd.getBoat().getBoatClass());
                    boatclasses.add(dto);
                } else {
                    BoatClass boatclass = getBoatClassForCompetitorWithoutBoatClass(racingEventService, c);
                    if (boatclass != null) {
                        boatclasses.add(convertBoatClassToDTO(boatclass));
                    }
                }
            }
        }
        return boatclasses;
    }

    @GwtIncompatible
    default BoatClass getBoatClassForCompetitorWithoutBoatClass(RacingEventService racingEventService, Competitor c) {
        for (Event event : racingEventService.getAllEvents()) {
            for (LeaderboardGroup leaderboardGroup : event.getLeaderboardGroups()) {
                for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    for (Competitor competitor : leaderboard.getCompetitors()) {
                        if (competitor.getId().equals(c.getId())) {
                            if (leaderboard.getBoatClass() != null) {
                                return leaderboard.getBoatClass();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /** @return converted BoatClassDTO from BoatClass */
    @GwtIncompatible
    default BoatClassDTO convertBoatClassToDTO(BoatClass bc) {
        return new BoatClassDTO(bc.getName(), bc.getDisplayName(), bc.getHullLength(), bc.getHullBeam());
    }
}
