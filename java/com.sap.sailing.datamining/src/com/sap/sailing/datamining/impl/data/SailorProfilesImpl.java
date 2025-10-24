package com.sap.sailing.datamining.impl.data;

import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.datamining.data.SailorProfile;
import com.sap.sailing.datamining.data.SailorProfiles;
import com.sap.sailing.domain.base.Competitor;

public class SailorProfilesImpl implements SailorProfiles {
    private final Map<Competitor, SailorProfile> profilesForCompetitor;
    
    /**
     * Constructs the mapping from {@link Competitor} to {@link SailorProfile} based on the
     * {@link SailorProfile#compareTo(SailorProfile) ordering} of the {@link SailorProfile}s. For each competitor,
     * {@link #getProfileForCompetitor(Competitor)} will return the "greatest" profile found that
     * {@link SailorProfile#getCompetitors() contains} that competitor.
     */
    public SailorProfilesImpl(final Iterable<SailorProfile> sailorProfiles) {
        this.profilesForCompetitor = new HashMap<>();
        for (final SailorProfile sailorProfile : sailorProfiles) {
            for (final Competitor competitor : sailorProfile.getCompetitors()) {
                final SailorProfile existingSailorProfileForCompetitor = profilesForCompetitor.get(competitor);
                if (existingSailorProfileForCompetitor == null || existingSailorProfileForCompetitor.compareTo(sailorProfile) < 0) {
                    profilesForCompetitor.put(competitor, sailorProfile);
                }
            }
        }
    }
    
    @Override
    public SailorProfile getProfileForCompetitor(Competitor competitor) {
        return profilesForCompetitor.get(competitor);
    }
}
