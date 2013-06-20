package com.sap.sailing.domain.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.TimingConstants;

public class RaceColumnDTO extends NamedDTO implements Serializable {
    private static final long serialVersionUID = -3228244237400937852L;
    private boolean medalRace;
    private List<FleetDTO> fleets;
    private Map<FleetDTO, RegattaAndRaceIdentifier> trackedRaceIdentifiersPerFleet;
    private Map<FleetDTO, RaceDTO> racesPerFleet;
    private Boolean isValidInTotalScore;
    private Double explicitFactor;
    private double effectiveFactor;
    
    /**
     * If the column has tracked races attached, this field tells the latest time point when one of those races was
     * still running. It is necessary for the race to have GPS and wind data for the time point to be recorded here. If
     * a tracked race has started but hasn't ended yet, the query time point is used for this field if it is after the
     * time point when that race started, assuming that at the query time the race was still running. If the race hasn't
     * started at the query time, the race isn't considered for setting this field. If the end of the race is known, it
     * is used for this field if later than any other value set for this field.
     * <p>
     * 
     * If no tracked race is attached to this column or none of the tracked races attached has started at the query time
     * point, this field is <code>null</code>.<p>
     * 
     * Note that the {@link Date} objects in here are in "server time" and must never be directly compared to an
     * uncorrected client time because the client's clock may have a significant offset to the server time.
     */
    private Map<FleetDTO, Date> whenLastTrackedRaceWasLiveByFleet;

    RaceColumnDTO() {} // for GWT serialization
    
    public RaceColumnDTO(Boolean isValidInTotalScore) {
        this.isValidInTotalScore = isValidInTotalScore;
        trackedRaceIdentifiersPerFleet = new HashMap<FleetDTO, RegattaAndRaceIdentifier>();
        racesPerFleet = new HashMap<FleetDTO, RaceDTO>();
        fleets = new ArrayList<FleetDTO>();
        whenLastTrackedRaceWasLiveByFleet = new HashMap<FleetDTO, Date>();
    }
    
    public boolean isValidInTotalScore() {
        return isValidInTotalScore;
    }
    
    public String getRaceColumnName() {
        return getName();
    }
    
    public Double getExplicitFactor() {
        return explicitFactor;
    }
    
    public void setExplicitFactor(Double explicitFactor) {
        this.explicitFactor = explicitFactor;
    }

    public boolean hasTrackedRace(RaceIdentifier raceIdentifier) {
        return trackedRaceIdentifiersPerFleet.values().contains(raceIdentifier);
    }
    
    public boolean isMedalRace() {
        return medalRace;
    }

    public void setMedalRace(boolean medalRace) {
        this.medalRace = medalRace;
    }

    public boolean isTrackedRace(FleetDTO fleet) {
        return trackedRaceIdentifiersPerFleet.get(fleet) != null;
    }

    public void setRaceIdentifier(FleetDTO fleet, RegattaAndRaceIdentifier raceIdentifier) {
        this.trackedRaceIdentifiersPerFleet.put(fleet, raceIdentifier);
    }

    /**
     * @return a non-<code>null</code> race identifier if this column represents a <em>tracked</em> race. Such a race's
     *         data can be obtained from the server in great detail, as opposed to non-tracked races for which only
     *         result points may have been entered manually.
     */
    public RegattaAndRaceIdentifier getRaceIdentifier(FleetDTO fleet) {
        return trackedRaceIdentifiersPerFleet.get(fleet);
    }

    /**
     * Returns an object with data (e.g. start date or places) for the RaceInLeaderboardDTO. Is <code>null</code>, if
     * the method {@link RaceColumnDTO#isTrackedRace(String)} returns <code>false</code>.
     * 
     * @return An Object with additional data, or <code>null</code> if the race isn't tracked
     */
    public RaceDTO getRace(FleetDTO fleet) {
        return racesPerFleet.get(fleet);
    }

    public void setRace(FleetDTO fleet, RaceDTO race) {
        this.racesPerFleet.put(fleet, race);
    }
    
    public Iterable<FleetDTO> getFleets() {
        return fleets;
    }
    
    /**
     * @return The start of race, or the start of tracking if the start of race is <code>null</code>, or
     *         <code>null</code> if no start date is available.
     */
    public Date getStartDate(FleetDTO fleet) {
        Date start = null;
        RaceDTO RaceDTO = racesPerFleet.get(fleet);
        if (RaceDTO != null) {
            start = RaceDTO.startOfRace;
            if(start == null && RaceDTO.isTracked) {
                start = RaceDTO.trackedRace.startOfTracking;
            }
        }
        return start;
    }
    
    /**
     * @return The {@link PlacemarkOrderDTO places} or <code>null</code>, if no places are available
     */
    public PlacemarkOrderDTO getPlaces() {
        PlacemarkOrderDTO places = null;
        for (RaceDTO race : racesPerFleet.values()) {
            if (race != null) {
                if (places == null) {
                    places = race.places;
                } else {
                    places.add(race.places);
                }
            }
        }
        return places;
    }
    
    /**
     * @return <code>true</code> if the startOfTracking is after the current date and there's no end of the race
     */
    public boolean isLive(FleetDTO fleet) {
        boolean result = false;
        // TODO bug 1351: never use System.currentTimeMillis() on the client when trying to compare anything with "server time"
        final long now = System.currentTimeMillis();
        Date whenLastTrackedRaceWasLive = getWhenLastTrackedRaceWasLive(fleet);
        if (whenLastTrackedRaceWasLive != null && whenLastTrackedRaceWasLive.getTime() > now - TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS) {
            result = true;
        }
        return result;
    }

    public boolean containsRace(RaceIdentifier preSelectedRace) {
        return trackedRaceIdentifiersPerFleet.values().contains(preSelectedRace);
    }

    public boolean hasTrackedRaces() {
        Set<RegattaAndRaceIdentifier> raceIdentifiers = new HashSet<RegattaAndRaceIdentifier>(trackedRaceIdentifiersPerFleet.values());
        raceIdentifiers.remove(null);
        return !raceIdentifiers.isEmpty();
    }

    public void addFleet(FleetDTO fleet) {
        fleets.add(fleet);
    }

    private Date getWhenLastTrackedRaceWasLive(FleetDTO fleet) {
        return whenLastTrackedRaceWasLiveByFleet.get(fleet);
    }

    public void setWhenLastTrackedRaceWasLive(FleetDTO fleet, Date whenLastTrackedRaceWasLive) {
        this.whenLastTrackedRaceWasLiveByFleet.put(fleet, whenLastTrackedRaceWasLive);
    }

    public boolean hasGPSData() {
        for (RaceDTO race : racesPerFleet.values()) {
            if (race != null && race.trackedRace != null && race.trackedRace.hasGPSData) {
                return true;
            }
        }
        return false;
    }

    public boolean hasWindData() {
        for (RaceDTO race : racesPerFleet.values()) {
            if (race != null && race.trackedRace != null && race.trackedRace.hasWindData) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(effectiveFactor);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((explicitFactor == null) ? 0 : explicitFactor.hashCode());
        result = prime * result + ((fleets == null) ? 0 : fleets.hashCode());
        result = prime * result + ((isValidInTotalScore == null) ? 0 : isValidInTotalScore.hashCode());
        result = prime * result + (medalRace ? 1231 : 1237);
        result = prime * result + ((racesPerFleet == null) ? 0 : racesPerFleet.hashCode());
        result = prime * result
                + ((trackedRaceIdentifiersPerFleet == null) ? 0 : trackedRaceIdentifiersPerFleet.hashCode());
        result = prime * result
                + ((whenLastTrackedRaceWasLiveByFleet == null) ? 0 : whenLastTrackedRaceWasLiveByFleet.hashCode());
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
        RaceColumnDTO other = (RaceColumnDTO) obj;
        if (Double.doubleToLongBits(effectiveFactor) != Double.doubleToLongBits(other.effectiveFactor))
            return false;
        if (explicitFactor == null) {
            if (other.explicitFactor != null)
                return false;
        } else if (!explicitFactor.equals(other.explicitFactor))
            return false;
        if (fleets == null) {
            if (other.fleets != null)
                return false;
        } else if (!fleets.equals(other.fleets))
            return false;
        if (isValidInTotalScore == null) {
            if (other.isValidInTotalScore != null)
                return false;
        } else if (!isValidInTotalScore.equals(other.isValidInTotalScore))
            return false;
        if (medalRace != other.medalRace)
            return false;
        if (racesPerFleet == null) {
            if (other.racesPerFleet != null)
                return false;
        } else if (!racesPerFleet.equals(other.racesPerFleet))
            return false;
        if (trackedRaceIdentifiersPerFleet == null) {
            if (other.trackedRaceIdentifiersPerFleet != null)
                return false;
        } else if (!trackedRaceIdentifiersPerFleet.equals(other.trackedRaceIdentifiersPerFleet))
            return false;
        if (whenLastTrackedRaceWasLiveByFleet == null) {
            if (other.whenLastTrackedRaceWasLiveByFleet != null)
                return false;
        } else if (!whenLastTrackedRaceWasLiveByFleet.equals(other.whenLastTrackedRaceWasLiveByFleet))
            return false;
        return true;
    }
    
    public void setEffectiveFactor(double effectiveFactor) {
        this.effectiveFactor = effectiveFactor;
    }

    public double getEffectiveFactor() {
        return effectiveFactor;
    }
}
