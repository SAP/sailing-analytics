package com.sap.sailing.gwt.home.communication.race;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.shared.DTO;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.SecurityInformationDTO;

public class SimpleRaceMetadataDTO implements DTO, SecuredDTO, Comparable<SimpleRaceMetadataDTO> {
    private static final long serialVersionUID = 4572985165272271996L;

    public enum RaceViewState {
        PLANNED {       // no start time set
            @Override
            public String getLabel() {
                return StringMessages.INSTANCE.raceIsPlanned();
            }
        },
        SCHEDULED {     // the start time is set and in the future
            @Override
            public String getLabel() {
                return StringMessages.INSTANCE.raceIsScheduled(); // fallback representation
            }
        },
        RUNNING {       // start time is the past and end time is not set or in the future
            @Override
            public String getLabel() {
                return StringMessages.INSTANCE.raceIsRunning(); // fallback representation
            }
        },
        FINISHED {      // the end time is set and is in the past
            @Override
            public String getLabel() {
                return StringMessages.INSTANCE.raceIsFinished();
            }
        },
        FINISHING {      // the blue flag has gone up but has not gone down yet
            @Override
            public String getLabel() {
                return StringMessages.INSTANCE.raceIsFinishing();
            }
        },
        POSTPONED {     // the start has been postponed
            @Override
            public String getLabel() {
                return StringMessages.INSTANCE.raceIsPostponed();
            }
        },
        ABANDONED {      // the running racing has been abandoned
            @Override
            public String getLabel() {
                return StringMessages.INSTANCE.raceIsCanceled();
            }
        };

        public abstract String getLabel();
    }
    
    public enum RaceTrackingState {
        NOT_TRACKED,             // No tracking data -> probably just managed by race committee app
        TRACKED_NO_VALID_DATA,   // tracking is connected but the required data for displaying the race viewer is not available
        TRACKED_VALID_DATA       // tracking is connected and all required data for displaying the race viewer is available
    }
    
    private String leaderboardName;
    private String leaderboardGroupName;
    private UUID leaderboardGroupId;
    private RegattaAndRaceIdentifier regattaAndRaceIdentifier;
    private String raceName;
    private Date start;
    private RaceViewState state;
    private RaceTrackingState trackingState;
    private SecurityInformationDTO securityInformation = new SecurityInformationDTO();
    
    private HashSet<SimpleCompetitorDTO> competitors = new HashSet<>();
    
    protected SimpleRaceMetadataDTO() {
    }
    
    public SimpleRaceMetadataDTO(String leaderboardName, RegattaAndRaceIdentifier regattaAndRaceIdentifier, String raceName) {
        this.leaderboardName = leaderboardName;
        this.regattaAndRaceIdentifier = regattaAndRaceIdentifier;
        this.raceName = raceName;
    }
    
    public String getLeaderboardName() {
        return leaderboardName;
    }

    public RegattaAndRaceIdentifier getRegattaAndRaceIdentifier() {
        return regattaAndRaceIdentifier;
    }

    public String getRaceName() {
        return raceName;
    }
    
    public String getLeaderboardGroupName() {
        return leaderboardGroupName;
    }
    
    public void setLeaderboardGroupName(String leaderboardGroupName) {
        this.leaderboardGroupName = leaderboardGroupName;
    }
    
    public UUID getLeaderboardGroupId() {
        return leaderboardGroupId;
    }
    
    public void setLeaderboardGroupId(UUID leaderboardGroupId) {
        this.leaderboardGroupId = leaderboardGroupId;
    }
    
    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }
    
    public RaceViewState getViewState() {
        return state;
    }

    public void setViewState(RaceViewState state) {
        this.state = state;
    }

    public RaceTrackingState getTrackingState() {
        return trackingState;
    }

    public void setTrackingState(RaceTrackingState trackingState) {
        this.trackingState = trackingState;
    }
    
    public void setCompetitors(Collection<SimpleCompetitorDTO> competitors) {
        this.competitors.addAll(competitors);
    }
    
    public Collection<SimpleCompetitorDTO> getCompetitors() {
        return competitors;
    }
    
    @Override
    public int compareTo(SimpleRaceMetadataDTO o) {
        final int compareByRaceName = new NaturalComparator().compare(getRaceName(), o.getRaceName());
        return compareByRaceName != 0 ? compareByRaceName : Util.compareToWithNull(getStart(), o.getStart(), true);
    }
    
    /**
     * Convenience method to check if this {@link SimpleRaceMetadataDTO race} has valid tracking data.
     * 
     * @return <code>true</code> if the {@link #getTrackingState() tracking state} is
     *         {@link RaceTrackingState#TRACKED_VALID_DATA}, <code>false</code> otherwise
     */
    public boolean hasValidTrackingData() {
        return getTrackingState() == RaceTrackingState.TRACKED_VALID_DATA;
    }
    
    /**
     * Convenience method to check if this {@link SimpleRaceMetadataDTO race} is already finished.
     * 
     * @return <code>true</code> if the {@link #getViewState() view state} is {@link RaceViewState#FINISHED},
     *         <code>false</code> otherwise
     */
    public boolean isFinished() {
        return getViewState() == RaceViewState.FINISHED;
    }
    
    /**
     * Convenience method to check if this {@link SimpleRaceMetadataDTO race} is currently running.
     * 
     * @return <code>true</code> if the {@link #getViewState() view state} is {@link RaceViewState#RUNNING} or
     *         {@link RaceViewState#FINISHING}, <code>false</code> otherwise
     */
    public boolean isRunning() {
        return getViewState() == RaceViewState.RUNNING || getViewState() == RaceViewState.FINISHING;
    }
    
    /**
     * Convenience method to check if this {@link SimpleRaceMetadataDTO race} is scheduled.
     * 
     * @return <code>true</code> if the {@link #getViewState() view state} is {@link RaceViewState#SCHEDULED},
     *         <code>false</code> otherwise
     */
    public boolean isScheduled() {
        return getViewState() == RaceViewState.SCHEDULED;
    }

    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return regattaAndRaceIdentifier.getIdentifier();
    }

    @Override
    public HasPermissions getPermissionType() {
        return SecuredDomainType.TRACKED_RACE;
    }

    @Override
    public String getName() {
        return raceName;
    }

    @Override
    public final AccessControlListDTO getAccessControlList() {
        return securityInformation.getAccessControlList();
    }

    @Override
    public final OwnershipDTO getOwnership() {
        return securityInformation.getOwnership();
    }

    @Override
    public final void setAccessControlList(final AccessControlListDTO accessControlList) {
        this.securityInformation.setAccessControlList(accessControlList);
    }

    @Override
    public final void setOwnership(final OwnershipDTO ownership) {
        this.securityInformation.setOwnership(ownership);
    }
    
}
