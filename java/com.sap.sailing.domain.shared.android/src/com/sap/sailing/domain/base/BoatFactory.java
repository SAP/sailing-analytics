package com.sap.sailing.domain.base;

import java.io.Serializable;

import com.sap.sse.common.Color;

/**
 * Base interface for classes managing a set of {@link Boat} objects.
 */
public interface BoatFactory {
    /**
     * If a valid boat is returned and the caller has information available that could be used to update the boat,
     * the caller must check the result of {@link #isBoatToUpdateDuringGetOrCreate(Boat)}, and if <code>true</code>,
     * must call {@link #getOrCreateBoat()} to cause an update of the boat's values.
     */
    Boat getExistingBoatById(Serializable boatId);

    /**
     * Checks if the <code>boat</code> shall be updated from the default provided by, e.g., a tracking infrastructure.
     * Callers of {@link #getExistingBoatById(Serializable)} or {@link #getExistingBoatByIdAsString(String)}
     * must call this method in case they retrieve a valid boat by ID and have data available that can be used to update
     * the .
     */
    boolean isBoatToUpdateDuringGetOrCreate(Boat result);

    Boat getOrCreateBoat(Serializable id, String name, BoatClass boatClass, String sailId, Color color);

    Boat getOrCreateBoat(Competitor competitor, String name, BoatClass boatClass, String sailId, Color color);
    
    Boat getOrCreateBoat(LeaderboardGroupBase leaderboardGroup, String name, BoatClass boatClass, String sailId, Color color);
}
