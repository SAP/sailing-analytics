package com.sap.sailing.domain.common.dto;

import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;

/**
 * Master data about a single race that is to be transferred to the client.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class RaceDTO extends BasicRaceDTO {
    private static final long serialVersionUID = 2613189982608149975L;

    /**
     * Tells if this race is currently being tracked, meaning that a {@link RaceTracker} is
     * listening for incoming GPS fixes, mark passings etc., to update a {@link TrackedRace} object
     * accordingly.
     */
    public boolean isTracked;

    public RaceStatusDTO status;

    public PlacemarkOrderDTO places;

    public TrackedRaceStatisticsDTO trackedRaceStatistics;

    private String regattaName;
    public String boatClass;
    
    public RaceDTO() {}

    public RaceDTO(RegattaAndRaceIdentifier raceIdentifier) {
        this(raceIdentifier, null, false);
    }

    public RaceDTO(RegattaAndRaceIdentifier raceIdentifier, TrackedRaceDTO trackedRace, boolean isCurrentlyTracked) {
        super(raceIdentifier, trackedRace);
        this.regattaName = raceIdentifier.getRegattaName();
        this.isTracked = isCurrentlyTracked;
    }

    public RegattaAndRaceIdentifier getRaceIdentifier() {
        return new RegattaNameAndRaceName(regattaName, getName());
    }

    public String getRegattaName() {
        return regattaName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((boatClass == null) ? 0 : boatClass.hashCode());
        result = prime * result + (isTracked ? 1231 : 1237);
        result = prime * result + ((places == null) ? 0 : places.hashCode());
        result = prime * result + ((regattaName == null) ? 0 : regattaName.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        RaceDTO other = (RaceDTO) obj;
        if (boatClass == null) {
            if (other.boatClass != null)
                return false;
        } else if (!boatClass.equals(other.boatClass))
            return false;
        if (isTracked != other.isTracked)
            return false;
        if (places == null) {
            if (other.places != null)
                return false;
        } else if (!places.equals(other.places))
            return false;
        if (regattaName == null) {
            if (other.regattaName != null)
                return false;
        } else if (!regattaName.equals(other.regattaName))
            return false;
        if (status == null) {
            if (other.status != null)
                return false;
        } else if (!status.equals(other.status))
            return false;
        return true;
    }
}
