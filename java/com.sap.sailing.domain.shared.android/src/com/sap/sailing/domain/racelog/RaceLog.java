package com.sap.sailing.domain.racelog;

import java.util.HashSet;
import java.util.UUID;

import com.sap.sailing.domain.common.WithID;
import com.sap.sailing.domain.racelog.impl.RaceLogEventComparator;
import com.sap.sailing.domain.tracking.Track;

/**
 * Special kind of {@link Track} for recording {@link RaceLogEvent}s.
 * 
 * <p>
 * Keeps track of the {@link RaceLogEvent}'s pass and returns only the events of the current pass on
 * {@link RaceLog#getFixes()}. Use {@link RaceLog#getRawFixes()} to receive all events in a {@link RaceLog}.
 * </p>
 * 
 * <p>
 * Implementations should use the {@link RaceLogEventComparator} for sorting its content.
 * </p>
 */
public interface RaceLog extends Track<RaceLogEvent>, WithID {
    
    public static final int DefaultPassId = 0;
    
    /**
     * Gets the current pass id.
     * 
     * @return the pass id.
     */
    int getCurrentPassId();

    /**
     * Adds a {@link RaceLogEvent} to the {@link RaceLog}.
     * 
     * @param event
     *            {@link RaceLogEvent} to be added.
     * @return <code>true</code> if the element was added, <code>false</code> otherwise.
     */
    boolean add(RaceLogEvent event);

    /**
     * Add a {@link RaceLogEventVisitor} as a listener for additions.
     */
    void addListener(RaceLogEventVisitor listener);

    /**
     * Remove a listener.
     */
    void removeListener(RaceLogEventVisitor listener);
    
    /**
     * Removes all listeners
     * @return 
     */
    HashSet<RaceLogEventVisitor> removeAllListeners();
    
    /**
     * Checks if the race log is empty.
     */
    boolean isEmpty();

    Iterable<RaceLogEvent> getRawFixesDescending();

    Iterable<RaceLogEvent> getFixesDescending();

    void addAllListeners(HashSet<RaceLogEventVisitor> listeners);

    Iterable<RaceLogEventVisitor> getAllListeners();

    /**
     * Adds an event to this race log and returns a superset of all race log events (excluding the new
     * <code>event</code>) that were added to this race log but not yet returned to the client with ID
     * <code>clientId</code> by this method. In general, the list returned is not a true superset but
     * equals exactly those events not yet delivered to the client. However, if the server was re-started
     * since the client last called this method, and since the underlying data structures are not durably
     * stored, the entire set of all race log events would be delivered to the client once.
     */
    Iterable<RaceLogEvent> add(RaceLogEvent event, UUID clientId);
    
    /**
     * Returns all {@link #getRawFixes() raw fixes} and marks them as delivered to the client identified by <code>clientId</code>
     * so that when that ID appears in a subsequent call to {@link #add(RaceLogEvent, UUID)}, the fixes returned by this call
     * are already considered delivered to the client identified by <code>clientId</code>.
     */
    Iterable<RaceLogEvent> getRawFixes(UUID clientId);

    /**
     * Like {@link #add(RaceLogEvent)}, only that no events are triggered. Use this method only when loading a race log,
     * e.g., from a replication or master data import or when loading from the database.
     * 
     * @return <code>true</code> if the event was actually added which is the case if there was no equal event contained
     *         in this race log yet
     */
    boolean load(RaceLogEvent event);
}
