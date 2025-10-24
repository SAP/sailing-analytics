package com.sap.sailing.datamining.impl.data;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.datamining.data.HasLeaderboardGroupContext;
import com.sap.sailing.datamining.data.SailorProfile;
import com.sap.sailing.datamining.data.SailorProfiles;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.server.preferences.SailorProfilePreference;
import com.sap.sailing.server.preferences.SailorProfilePreferences;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.User;

public class LeaderboardGroupWithContext implements HasLeaderboardGroupContext {
    private final LeaderboardGroup leaderboardGroup;
    private final PolarDataService polarDataService;
    private final DomainFactory baseDomainFactory;
    private final SecurityService securityService;
    
    /**
     * Access has to be synchronized; it is constructed lazily by the {@link #getSailorProfiles()} method.
     */
    private SailorProfiles sailorProfiles;

    public LeaderboardGroupWithContext(LeaderboardGroup leaderboardGroup, PolarDataService polarDataService, DomainFactory baseDomainFactory, SecurityService securityService) {
        this.leaderboardGroup = leaderboardGroup;
        this.polarDataService = polarDataService;
        this.baseDomainFactory = baseDomainFactory;
        this.securityService = securityService;
    }

    @Override
    public LeaderboardGroup getLeaderboardGroup() {
        return leaderboardGroup;
    }

    @Override
    public PolarDataService getPolarDataService() {
        return polarDataService;
    }

    @Override
    public DomainFactory getBaseDomainFactory() {
        return baseDomainFactory;
    }
    
    @Override
    public SecurityService getSecurityService() {
        return securityService;
    }
    
    @Override
    public synchronized SailorProfiles getSailorProfiles() {
        if (sailorProfiles == null) {
            final User currentUser = getSecurityService().getCurrentUser();
            if (currentUser == null) {
                throw new NullPointerException("No user session in DataMining. Thread pool context problems? You may need to attach a user session to the thread.");
            }
            final SailorProfilePreferences sailorProfilePreferences = getSecurityService().getPreferenceObject(currentUser.getName(), SailorProfilePreferences.PREF_NAME);
            if (sailorProfilePreferences != null) {
                final List<SailorProfile> theSailorProfiles = new ArrayList<>();
                for (final SailorProfilePreference sailorProfilePreference : sailorProfilePreferences.getSailorProfiles()) {
                    final SailorProfile sailorProfile = new SailorProfileImpl(sailorProfilePreference.getUuid(), sailorProfilePreference.getName(), sailorProfilePreference.getCompetitors());
                    theSailorProfiles.add(sailorProfile);
                }
                sailorProfiles = new SailorProfilesImpl(theSailorProfiles);
            }
        }
        return sailorProfiles;
    }
}
