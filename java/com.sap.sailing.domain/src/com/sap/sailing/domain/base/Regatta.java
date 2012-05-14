package com.sap.sailing.domain.base;

import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.Named;
import com.sap.sailing.domain.tracking.TrackedRegatta;

/**
 * The name shall be unique across all regattas tracked concurrently. In particular, if you want to
 * keep apart regattas in different boat classes, make sure the boat class name becomes part of the
 * regatta name.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface Regatta extends Named {
    /**
     * Please note that the {@link RaceDefinition}s of the {@link Regatta} are not necessarily in sync with the
     * {@link TrackedRace}s of the {@link TrackedRegatta} whose {@link TrackedRegatta#getRegatta() regatta} is this regatta.
     * For example, it may be the case that a {@link RaceDefinition} is returned by this method for which no
     * {@link TrackedRace} exists in the corresponding {@link TrackedRegatta}. This could be the case, e.g., during
     * the initialization of the tracker as well as during removing a race from the server.<p>
     * 
     * Callers iterating over the result that anticipate concurrent modifications shall synchronize the iteration
     * on the result.
     */
    Iterable<RaceDefinition> getAllRaces();
    
    /**
     * Please note that the set of {@link RaceDefinition}s contained by this regatta may not match up with the 
     * {@link TrackedRace}s of the {@link TrackedRegatta} corresponding to this regatta. See also {@link #getAllRaces()}.
     * 
     * @return <code>null</code>, if this regatta does not contain a race (see {@link #getAllRaces}) whose
     * {@link RaceDefinition#getName()} equals <code>raceName</code>
     */
    RaceDefinition getRaceByName(String raceName);
    
    BoatClass getBoatClass();
    
    Iterable<Competitor> getCompetitors();

    void addRace(RaceDefinition race);

    void removeRace(RaceDefinition raceDefinition);
 
    void addRegattaListener(RegattaListener listener);
    
    void removeRegattaListener(RegattaListener listener);

    RegattaIdentifier getRegattaIdentifier();

    /**
     * A regatta name may be composed, e.g., from an overall regatta name and the boat class name. A factory or constructor
     * may require the base name to which the boat class name will be appended. This method emits the base name.
     */
    String getBaseName();
}
