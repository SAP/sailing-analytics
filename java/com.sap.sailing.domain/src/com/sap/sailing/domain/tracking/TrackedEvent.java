package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.TimePoint;
import com.sap.sailing.domain.common.NoWindException;

/**
 * Manages a set of {@link TrackedRace} objects that belong to the same {@link Event} (regatta, sailing event for a
 * single boat class). It therefore represents the entry point into the tracking-related objects for such an event.
 * Allows clients to find a {@link TrackedRace} by the {@link RaceDefinition} for which it holds the tracking data.
 * <p>
 * 
 * Please note that the result of calling {@link #getEvent()}.{@link Event#getAllRaces() getAllRaces()} is not
 * guaranteed to match up with the races obtained by calling {@link TrackedRace#getRace()} on all {@link TrackedRaces}
 * resulting from {@link #getTrackedRaces()}. In other words, the processes for adding and removing races to the
 * server do not guarantee to update the master and tracking data for races atomically.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public interface TrackedEvent {
    Event getEvent();

    Iterable<TrackedRace> getTrackedRaces();

    Iterable<TrackedRace> getTrackedRaces(BoatClass boatClass);

    /**
     * Creates a {@link TrackedRace} based on the parameter specified and {@link #addTrackedRace(TrackedRace) adds} it
     * to this tracked event. Afterwards, calling {@link #getTrackedRace(RaceDefinition) getTrackedRace(raceDefinition)}
     * will return the result of this method call.
     * 
     * @param raceDefinitionSetToUpdate
     *            if not <code>null</code>, after creating the {@link TrackedRace}, the <code>raceDefinition</code> is
     *            {@link DynamicRaceDefinitionSet#addRaceDefinition(RaceDefinition) added} to that object.
     */
    TrackedRace createTrackedRace(RaceDefinition raceDefinition, WindStore windStore,
            long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
            DynamicRaceDefinitionSet raceDefinitionSetToUpdate);

    /**
     * Obtains the tracked race for <code>race</code>. Blocks until the tracked race has been created
     * and added to this tracked event (see {@link #addTrackedRace(TrackedRace)}).
     */
    TrackedRace getTrackedRace(RaceDefinition race);

    /**
     * Non-blocking call that returns <code>null</code> if no tracking information currently exists
     * for <code>race</code>. See also {@link #getTrackedRace(RaceDefinition)} for a blocking variant.
     */
    TrackedRace getExistingTrackedRace(RaceDefinition race);
    
    void addTrackedRace(TrackedRace trackedRace);

    void removeTrackedRace(TrackedRace trackedRace);

    /**
     * Listener will be notified when {@link #addTrackedRace(TrackedRace)} is called and
     * upon registration for each tracked race already known. Therefore, the listener
     * won't miss any tracked race.
     */
    void addRaceListener(RaceListener listener);
    
    int getNetPoints(Competitor competitor, TimePoint timePoint) throws NoWindException;

    void removeTrackedRace(RaceDefinition raceDefinition);

}