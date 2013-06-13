package com.sap.sailing.domain.common.dto;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.Tack;

/**
 * Holds a single competitor's scoring details for a single race. It may optionally contain
 * a list of {@link LegEntryDTO} objects providing details about the individual legs sailed
 * during the race.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class LeaderboardEntryDTO implements Serializable {
    private static final long serialVersionUID = -4678693781217869837L;

    /**
     * Identifies the race in which the competitor achieved this score. This makes it possible to find out in which
     * fleet the competitor started in this column.
     */
    public RaceIdentifier race;
    
    /**
     * Either <code>null</code> in case no max points, or one of "DNS", "DNF", "OCS", "DND", "RAF", "BFD", "DNC", or "DSQ"
     */
    public MaxPointsReason reasonForMaxPoints;
    
    public Double netPoints;
    
    /**
     * Tells if the net points have been overridden by a score correction. Can be used to render differently in editing environment.
     */
    public boolean netPointsCorrected;
    
    public Double totalPoints;
    
    public boolean discarded;
    
    public Double windwardDistanceToOverallLeaderInMeters;
    
    public Double averageCrossTrackErrorInMeters;
    
    public Double distanceToStartLineAtStartOfRaceInMeters;
    
    public Double speedOverGroundAtStartOfRaceInKnots;
    
    public Double speedOverGroundAtPassingStartWaypointInKnots;
    
    public Double distanceToStarboardSideOfStartLineInMeters;
    
    public Tack startTack;

    /**
     * If <code>null</code>, no leg details are known yet, the race is not being tracked or the details
     * haven't been requested from the server yet. Otherwise, the list holds one entry per <code>Leg</code> of the
     * <code>Course</code> being sailed in the race for which this object holds the scoring details.
     */
    public List<LegEntryDTO> legDetails;

    /**
     * <code>null</code>, if the fleet couldn't be determined, e.g., because the tracked race isn't known and therefore
     * the link to the fleet is not known; otherwise the description of the fleet in which the competitor scored this
     * entry
     */
    public FleetDTO fleet;

    public LeaderboardEntryDTO() { }
    
    public boolean hasScoreCorrection() {
        return netPointsCorrected || (reasonForMaxPoints != null && reasonForMaxPoints != MaxPointsReason.NONE);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((averageCrossTrackErrorInMeters == null) ? 0 : averageCrossTrackErrorInMeters.hashCode());
        result = prime * result + (discarded ? 1231 : 1237);
        result = prime
                * result
                + ((distanceToStarboardSideOfStartLineInMeters == null) ? 0
                        : distanceToStarboardSideOfStartLineInMeters.hashCode());
        result = prime
                * result
                + ((distanceToStartLineAtStartOfRaceInMeters == null) ? 0 : distanceToStartLineAtStartOfRaceInMeters
                        .hashCode());
        result = prime * result + ((fleet == null) ? 0 : fleet.hashCode());
        result = prime * result + ((legDetails == null) ? 0 : legDetails.hashCode());
        result = prime * result + ((netPoints == null) ? 0 : netPoints.hashCode());
        result = prime * result + (netPointsCorrected ? 1231 : 1237);
        result = prime * result + ((race == null) ? 0 : race.hashCode());
        result = prime * result + ((reasonForMaxPoints == null) ? 0 : reasonForMaxPoints.hashCode());
        result = prime
                * result
                + ((speedOverGroundAtPassingStartWaypointInKnots == null) ? 0
                        : speedOverGroundAtPassingStartWaypointInKnots.hashCode());
        result = prime * result
                + ((speedOverGroundAtStartOfRaceInKnots == null) ? 0 : speedOverGroundAtStartOfRaceInKnots.hashCode());
        result = prime * result + ((startTack == null) ? 0 : startTack.hashCode());
        result = prime * result + ((totalPoints == null) ? 0 : totalPoints.hashCode());
        result = prime
                * result
                + ((windwardDistanceToOverallLeaderInMeters == null) ? 0 : windwardDistanceToOverallLeaderInMeters
                        .hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LeaderboardEntryDTO other = (LeaderboardEntryDTO) obj;
        if (averageCrossTrackErrorInMeters == null) {
            if (other.averageCrossTrackErrorInMeters != null)
                return false;
        } else if (!averageCrossTrackErrorInMeters.equals(other.averageCrossTrackErrorInMeters))
            return false;
        if (discarded != other.discarded)
            return false;
        if (distanceToStarboardSideOfStartLineInMeters == null) {
            if (other.distanceToStarboardSideOfStartLineInMeters != null)
                return false;
        } else if (!distanceToStarboardSideOfStartLineInMeters.equals(other.distanceToStarboardSideOfStartLineInMeters))
            return false;
        if (distanceToStartLineAtStartOfRaceInMeters == null) {
            if (other.distanceToStartLineAtStartOfRaceInMeters != null)
                return false;
        } else if (!distanceToStartLineAtStartOfRaceInMeters.equals(other.distanceToStartLineAtStartOfRaceInMeters))
            return false;
        if (fleet == null) {
            if (other.fleet != null)
                return false;
        } else if (!fleet.equals(other.fleet))
            return false;
        if (legDetails == null) {
            if (other.legDetails != null)
                return false;
        } else if (!legDetails.equals(other.legDetails))
            return false;
        if (netPoints == null) {
            if (other.netPoints != null)
                return false;
        } else if (!netPoints.equals(other.netPoints))
            return false;
        if (netPointsCorrected != other.netPointsCorrected)
            return false;
        if (race == null) {
            if (other.race != null)
                return false;
        } else if (!race.equals(other.race))
            return false;
        if (reasonForMaxPoints != other.reasonForMaxPoints)
            return false;
        if (speedOverGroundAtPassingStartWaypointInKnots == null) {
            if (other.speedOverGroundAtPassingStartWaypointInKnots != null)
                return false;
        } else if (!speedOverGroundAtPassingStartWaypointInKnots
                .equals(other.speedOverGroundAtPassingStartWaypointInKnots))
            return false;
        if (speedOverGroundAtStartOfRaceInKnots == null) {
            if (other.speedOverGroundAtStartOfRaceInKnots != null)
                return false;
        } else if (!speedOverGroundAtStartOfRaceInKnots.equals(other.speedOverGroundAtStartOfRaceInKnots))
            return false;
        if (startTack != other.startTack)
            return false;
        if (totalPoints == null) {
            if (other.totalPoints != null)
                return false;
        } else if (!totalPoints.equals(other.totalPoints))
            return false;
        if (windwardDistanceToOverallLeaderInMeters == null) {
            if (other.windwardDistanceToOverallLeaderInMeters != null)
                return false;
        } else if (!windwardDistanceToOverallLeaderInMeters.equals(other.windwardDistanceToOverallLeaderInMeters))
            return false;
        return true;
    }
    
}
