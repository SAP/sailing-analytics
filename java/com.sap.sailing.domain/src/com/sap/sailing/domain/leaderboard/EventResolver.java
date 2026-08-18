package com.sap.sailing.domain.leaderboard;

import java.io.Serializable;

import com.sap.sailing.domain.base.Event;

/**
 * Manages a set of events and can resolve one by the event's ID. Real, non-testing implementations
 * must override the {@link #addEventResolverListener(Listener)} and {@link #removeEventResolverListener(Listener)}
 * default methods which are default-implemented here to do nothing.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface EventResolver {
    public static interface Listener {
        void eventAdded(Event event);
        void eventRemoved(Event event);
    }
    
    default void addEventResolverListener(Listener listener) {
    }
    
    default void removeEventResolverListener(Listener listener) {
    }
    
    /**
     * Returns the event with given id. When no event is found, <b>null</b> is returned.
     * 
     * @param id
     *                  The id of the event.
     * @return The event with given id.
     */
    Event getEvent(Serializable id);
    
    Iterable<Event> getAllEvents();
}
