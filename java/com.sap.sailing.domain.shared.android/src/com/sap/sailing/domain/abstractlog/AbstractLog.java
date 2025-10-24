package com.sap.sailing.domain.abstractlog;

import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.UUID;

import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.WithID;

/**
 * Special kind of {@link Track} for recording {@link AbstractLogEvent}s.
 * 
 * <p>
 * Keeps track of the {@link AbstractLogEvent}'s pass and returns only the events of the current pass on
 * {@link #getFixes()}. Use {@link #getRawFixes()} to receive all events in a {@link AbstractLog}.
 * </p>
 * 
 * The visitor pattern presents a problem in combination with the generic event type: such events
 * that
 * 
 * @param <VisitorT> Event visitor type - must be able to visit all events of type {@link EventT}
 * @param <EventT> Event type
 */
public interface AbstractLog<EventT extends AbstractLogEvent<VisitorT>, VisitorT>
extends Track<EventT>, WithID {

    /**
     * Adds a {@link AbstractLogEvent} to the AbstractLog.
     * 
     * @param event
     *            {@link AbstractLogEvent} to be added.
     * @return <code>true</code> if the element was added, <code>false</code> otherwise.
     */
    boolean add(EventT event);

    /**
     * Add a {@link VisitorT} as a listener for additions. Listeners won't be serialized together with this log.
     */
    void addListener(VisitorT listener);

    /**
     * Remove a listener.
     */
    void removeListener(VisitorT listener);
    
    /**
     * Removes all listeners
     * @return 
     */
    HashSet<VisitorT> removeAllListeners();
    
    /**
     * Checks if the log is empty.
     */
    boolean isEmpty();

    /**
     * Callers that want to iterate over the collection returned need to use {@link #lockForRead()} and
     * {@link #unlockAfterRead()} to avoid {@link ConcurrentModificationException}s.
     */
    Iterable<EventT> getRawFixesDescending();

    /**
     * Callers that want to iterate over the collection returned need to use {@link #lockForRead()} and
     * {@link #unlockAfterRead()} to avoid {@link ConcurrentModificationException}s.
     */
    Iterable<EventT> getFixesDescending();

    void addAllListeners(Iterable<VisitorT> listeners);

    Iterable<VisitorT> getAllListeners();

    /**
     * Adds an event to this log and returns {@link #getEventsToDeliver(UUID)} 
     * (excluding the new <code>event</code>)
     */
    Iterable<EventT> add(EventT event, UUID clientId);
    
    /**
     * Returns a superset of all log events that were added to this log but not yet returned to 
     * the client with ID <code>clientId</code> by this method. In general, the list returned is not a true 
     * superset but equals exactly those events not yet delivered to the client. However, if the server 
     * was re-started since the client last called this method, and since the underlying data structures 
     * are not durably stored, the entire set of all log events would be delivered to the client once.
     */
    Iterable<EventT> getEventsToDeliver(UUID clientId);
    
    /**
     * Returns all {@link #getRawFixes() raw fixes} and marks them as delivered to the client identified by <code>clientId</code>
     * so that when that ID appears in a subsequent call to {@link #add(AbstractLogEvent, UUID)}, the fixes returned by this call
     * are already considered delivered to the client identified by <code>clientId</code>.
     */
    Iterable<EventT> getRawFixes(UUID clientId);

    /**
     * Like {@link #add(AbstractLogEvent)}, only that no events are triggered. Use this method only when loading a log,
     * e.g., from a replication or master data import or when loading from the database.
     * 
     * @return <code>true</code> if the event was actually added which is the case if there was no equal event contained
     *         in this log yet
     */
    boolean load(EventT event);
    
    /**
     * Search for the event by its {@link AbstractLogEvent#getId() id}.
     * Caller needs to hold the read lock.
     */
    EventT getEventById(Serializable id);
    
    /**
     * Get a {@link NavigableSet} of unrevoked events regardless of the {@code pass}. Events are sorted by their
     * {@link TimePoint} and the oldest is returned first. Callers that want to iterate over the collection returned
     * need to use {@link #lockForRead()} and {@link #unlockAfterRead()} to avoid
     * {@link ConcurrentModificationException}s.
     */
    NavigableSet<EventT> getUnrevokedEvents();
    
    /**
     * Get a {@link NavigableSet} of unrevoked events regardless of the {@code pass}. Callers that want to iterate over
     * the collection returned need to use {@link #lockForRead()} and {@link #unlockAfterRead()} to avoid
     * {@link ConcurrentModificationException}s.
     */
    NavigableSet<EventT> getUnrevokedEventsDescending();

    /**
     * Merges all events from the <code>other</code> log into this.
     */
    void merge(AbstractLog<EventT, VisitorT> other);
    
    /**
     * Inserts a {@link RevokeEvent} for {@code toRevoke}, if latter is revokable, exists in the racelog and has not yet
     * been revoked and the {@code author} has a {@link AbstractLogEventAuthor#getPriority() priority} that is at least
     * as high as that of {@code toRevoke}'s author (numerically less or equal).
     * 
     * @param author
     *            The author for the {@code RevokeEvent}.
     * @param toRevoke
     *            the event to revoke
     * 
     * @exception NotRevokableException if {@code toRevoke} is not a {@link Revokable} event or {@code author} doesn't
     * have sufficient priority 
     */
    void revokeEvent(AbstractLogEventAuthor author, EventT toRevoke, String reason) throws NotRevokableException;
    void revokeEvent(AbstractLogEventAuthor author, EventT toRevoke) throws NotRevokableException;
}
