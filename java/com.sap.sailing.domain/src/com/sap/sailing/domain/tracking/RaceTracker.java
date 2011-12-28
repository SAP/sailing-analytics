package com.sap.sailing.domain.tracking;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.RaceDefinition;

/**
 * Centerpiece of a tracking adapter. A tracker is responsible for receiving tracking data for one or more
 * {@link RaceDefinition races} that are {@link Event#getAllRaces() part of} a common {@link #getEvent() Event}. Some
 * tracker architectures may not be able to deliver all data for the {@link RaceDefinition} when created or started.
 * Therefore, {@link #getRaces()} may return <code>null</code> if the race information hasn't been received by the
 * tracker yet. Through the {@link RacesHandle} returned by {@link #getRacesHandle()} it is also possible to perform a
 * {@link RacesHandle#getRaces() blocking get} for the races tracked by this tracker.
 * <p>
 * 
 * The data received by the tracker is usually fed into {@link TrackedRace} objects that {@link TrackedRace#getRace()
 * correspond} to the {@link RaceDefinition} objects for whose tracking this tracker is responsible. When the
 * {@link TrackedRace} isn't connected to its {@link TrackedEvent#getTrackedRaces() owning} {@link TrackedEvent}, a
 * tracker is assumed to no longer update the {@link TrackedRace} object, even if it hasn't been {@link #stop() stopped}.
 * <p>
 * 
 * A tracker may be {@link #stop() stopped}. In this case, it will no longer receive any data at all. Stopping a tracker
 * will not modify the {@link Event} and the {@link TrackedEvent} with regards to their ownership of their
 * {@link RaceDefiniion} and {@link TrackedRace}, respectively.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface RaceTracker {
    /**
     * Stops tracking the races.
     */
    void stop() throws MalformedURLException, IOException, InterruptedException;

    com.sap.sailing.domain.base.Event getEvent();

    /**
     * Returns the races being tracked by this tracker. Non-blocking call that returns <code>null</code> if
     * the {@link RaceDefinition} for a TracTrac Event hasn't been created yet, e.g., because the course definition
     * hasn't been received yet or the listener for receiving course information hasn't been registered (yet).
     * Also returns races that have been removed from containing structures which may lead this tracker to no
     * longer update their {@link TrackedRace} with new data.
     */
    Set<RaceDefinition> getRaces();

    RacesHandle getRacesHandle();

    DynamicTrackedEvent getTrackedEvent();
    
    WindStore getWindStore();

    /**
     * returns a unique key for this tracker which can, e.g., be used as a key in a {@link Map}
     */
    Object getID();
    
}
