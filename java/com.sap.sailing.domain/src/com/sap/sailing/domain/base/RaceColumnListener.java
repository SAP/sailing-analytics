package com.sap.sailing.domain.base;

import java.io.Serializable;

import com.sap.sailing.domain.tracking.TrackedRace;

public interface RaceColumnListener extends Serializable {
    void trackedRaceLinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace);
    
    void trackedRaceUnlinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace);
    
    void isMedalRaceChanged(RaceColumn raceColumn, boolean newIsMedalRace);
    
    void raceColumnAddedToContainer(RaceColumn raceColumn);
    
    void raceColumnRemovedFromContainer(RaceColumn raceColumn);
    
    /**
     * A listener can use this to specify that it must not be serialized together with other listeners.
     * Background: the collections holding race column listeners are usually non-transient. This will cause
     * the set of listeners to be serialized. However, for some listeners, serialization does not make sense.
     * Instead of making the entire listener collection transient, with this method it is possible that
     * individual listeners remove themselves from the serialization output.<p>
     * 
     * Note that it is the responsibility of the class holding a collection of objects of this type to
     * exclude listeners from the serialization that return <code>true</code> from this method.
     */
    boolean isTransient();
}
