package com.sap.sailing.gwt.ui.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;

import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.windfinder.SpotDTO;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.SecurityInformationDTO;

public class EventDTO extends EventBaseDTO implements SecuredDTO {
    private static final long serialVersionUID = -7100030301376959817L;

    private SecurityInformationDTO securityInformation = new SecurityInformationDTO();

    private Date currentServerTime;

    private List<LeaderboardGroupDTO> leaderboardGroups; // keeps the more specific type accessible in a type-safe way
    
    private List<String> windFinderReviewedSpotsCollectionIds;
    private List<SpotDTO> allWindFinderSpotIdsUsedByEvent;

    @Deprecated
    EventDTO() {} // for GWT RPC serialization only
    
    public EventDTO(String name) {
        this(name, new ArrayList<LeaderboardGroupDTO>());
    }

    public EventDTO(String name, List<LeaderboardGroupDTO> leaderboardGroups) {
        super(name, leaderboardGroups);
        this.leaderboardGroups = leaderboardGroups;
        initCurrentServerTime();
    }

    public boolean isFakeSeries() {
        return leaderboardGroups.size() == 1 && leaderboardGroups.get(0).hasOverallLeaderboard();
    }
    
    public boolean isRunning() {
        return getCurrentServerTime().after(startDate) && getCurrentServerTime().before(endDate);
    }

    public boolean isFinished() {
        return getCurrentServerTime().after(endDate);
    }

    private void initCurrentServerTime() {
        currentServerTime = new Date();
    }

    public Date getCurrentServerTime() {
        return currentServerTime;
    }

    public void addLeaderboardGroup(LeaderboardGroupDTO leaderboardGroup) {
        leaderboardGroups.add(leaderboardGroup);
    }
    
    /**
     * All {@link LeaderboardGroupDTO}s found {@link #getLeaderboardGroups() in this event} for which
     * their {@link LeaderboardGroupDTO#getId() ID} exists in {@code newLeaderboardGroups}, the corresponding
     * value in the {@code newLeaderboardGroups} is used to replace that {@link LeaderboardGroupDTO} in this
     * event.<p>
     * 
     * As a result, all leaderboard group objects known by this event will be aligned with
     * {@code newLeaderboardGroups}, except for those whose ID was not found in that map. Use this method
     * to keep {@link LeaderboardGroupDTO}s consistent on a client such as the AdminConsole to ensure that
     * edits to a {@link LeaderboardGroupDTO} reflect accordingly on all ends.
     */
    public void replaceLeaderboardGroupsWithSameId(final Map<UUID, LeaderboardGroupDTO> newLeaderboardGroups) {
        final ListIterator<LeaderboardGroupDTO> i = leaderboardGroups.listIterator();
        while (i.hasNext()) {
            final LeaderboardGroupDTO next = i.next();
            final LeaderboardGroupDTO replacement = newLeaderboardGroups.get(next.getId());
            if (replacement != null) {
                i.remove();
                i.add(replacement);
            }
        }
    }

    public List<LeaderboardGroupDTO> getLeaderboardGroups() {
        return leaderboardGroups;
    }

    public List<UUID> getLeaderboardGroupIds() {
        final List<UUID> updatedEventLeaderboardGroupIds = new ArrayList<>();
        for (LeaderboardGroupDTO leaderboardGroup : this.getLeaderboardGroups()) {
            updatedEventLeaderboardGroupIds.add(leaderboardGroup.getId());
        }
        return updatedEventLeaderboardGroupIds;
    }
    
    public AbstractLeaderboardDTO getLeaderboardByName(String leaderboardName) {
        for (LeaderboardGroupDTO leaderboardGroup : this.getLeaderboardGroups()) {
            for (StrippedLeaderboardDTO leaderboardDTO : leaderboardGroup.getLeaderboards()) {
                if(leaderboardName.equals(leaderboardDTO.getName())) {
                    return leaderboardDTO;
                }
            }
        }
        return null;
    }
    
    /**
     * An event may happen in the vicinity of one or more WindFinder (https://www.windfinder.com) weather
     * stations. Which ones those are can be defined using {@link #setWindFinderReviewedSpotsCollection(Iterable)},
     * and this getter returns the IDs last set.
     * 
     * @return always a valid {@link Iterable} which may, though, be empty
     */
    public List<String> getWindFinderReviewedSpotsCollectionIds() {
        final List<String> result;
        if (windFinderReviewedSpotsCollectionIds == null) {
            result = Collections.emptyList();
        } else {
            result = windFinderReviewedSpotsCollectionIds;
        }
        return result;
    }

    /**
     * Set the IDs of the reviewed WindFinder spot collections to consider during this event.
     * Setting this to a non-empty value shall lead to a corresponding display of a WindFinder
     * logo / link on the event's UI representation.
     */
    public void setWindFinderReviewedSpotsCollection(Iterable<String> reviewedSpotsCollectionId) {
        windFinderReviewedSpotsCollectionIds = new ArrayList<>();
        if (reviewedSpotsCollectionId != null) {
            Util.addAll(reviewedSpotsCollectionId, windFinderReviewedSpotsCollectionIds);
        }
    }

    /**
     * In addition to the wind finder spot collections specified by this event explicitly (see
     * {@link #getWindFinderReviewedSpotsCollectionIds()}), this method may return additional spot collection IDs based
     * on the tracked races reachable from this event's associated leaderboard groups and their wind sources. The
     * {@link WindSource#getId() wind source IDs} of all wind sources of type {@link WindSourceType#WINDFINDER} will be
     * collected and returned.
     */
    public Iterable<SpotDTO> getAllWindFinderSpotIdsUsedByEvent() {
        final Iterable<SpotDTO> result;
        if (allWindFinderSpotIdsUsedByEvent == null) {
            result = Collections.emptySet();
        } else {
            result = allWindFinderSpotIdsUsedByEvent;
        }
        return result;
    }
    
    public void setAllWindFinderSpotsUsedByEvent(Iterable<SpotDTO> windFinderSpots) {
        this.allWindFinderSpotIdsUsedByEvent = new ArrayList<>();
        if (windFinderSpots != null) {
            Util.addAll(windFinderSpots, this.allWindFinderSpotIdsUsedByEvent);
        }
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
    
    @Override
    public HasPermissions getPermissionType() {
        return SecuredDomainType.EVENT;
    }
    
    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    public TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return new TypeRelativeObjectIdentifier(getId().toString());
    }

}
