package com.sap.sailing.domain.abstractlog.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.AbstractLog;
import com.sap.sailing.domain.abstractlog.AbstractLogEvent;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.Revokable;
import com.sap.sailing.domain.abstractlog.RevokeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEventComparator;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sailing.domain.shared.tracking.impl.PartialNavigableSetView;
import com.sap.sailing.domain.shared.tracking.impl.TrackImpl;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.shared.util.impl.ArrayListNavigableSet;

/**
 * {@link Track} implementation for {@link AbstractLogEvent}s.
 *
 * <p>
 * {@link TrackImpl#getDummyFix(com.sap.sailing.domain.common.TimePoint)} is not overridden, see
 * {@link RaceLogEventComparator} for sorting when interface methods like
 * {@link Track#getFirstFixAfter(com.sap.sailing.domain.common.TimePoint)} are used.
 * </p>
 *
 */
public abstract class AbstractLogImpl<EventT extends AbstractLogEvent<VisitorT>, VisitorT>
extends TrackImpl<EventT> implements AbstractLog<EventT, VisitorT> {
    private static final long serialVersionUID = -176745401321893502L;
    private static final String DefaultLockName = AbstractLogImpl.class.getName() + ".lock";
    private final static Logger logger = Logger.getLogger(AbstractLogImpl.class.getName());
    private Set<Serializable> revokedEventIds = new HashSet<Serializable>();

    /**
     * Clients can use the {@link #add(RaceLogEvent, UUID)} method
     */
    private transient Map<UUID, Set<EventT>> eventsDeliveredToClient = new HashMap<UUID, Set<EventT>>();

    private Map<Serializable, EventT> eventsById = new HashMap<Serializable, EventT>();

    private final Serializable id;
    private transient Set<VisitorT> listeners;

    /**
     * Initializes a new {@link RaceLogImpl} with the default lock name.
     */
    public AbstractLogImpl(Serializable identifier, Comparator<Timed> comparator) {
        this(DefaultLockName, identifier, comparator);
    }

    /**
     * Initializes a new {@link RaceLogImpl}.
     *
     * @param nameForReadWriteLock
     *            name of lock.
     */
    public AbstractLogImpl(String nameForReadWriteLock, Serializable identifier, Comparator<Timed> comparator) {
        super(new ArrayListNavigableSet<Timed>(comparator), nameForReadWriteLock);
        this.listeners = new HashSet<VisitorT>();
        this.id = identifier;
    }

    @Override
    public Serializable getId() {
        return this.id;
    }

    protected void onSuccessfulAdd(EventT event, boolean notifyListeners) {
        revokeIfNecessary(event);
        eventsById.put(event.getId(), event);
        if (notifyListeners) {
            notifyListenersAboutReceive(event);
        }
    }

    protected boolean add(EventT event, boolean notifyListeners) {
        boolean isAdded = false;
        lockForWrite();
        try {
            isAdded = getInternalRawFixes().add(event);
        } finally {
            unlockAfterWrite();
        }
        if (isAdded) {
            logger.finer(String.format("%s (%s) was added to log %s.", event, event.getClass().getName(), getId()));
            onSuccessfulAdd(event, notifyListeners);
        } else {
            logger.finer(String.format("%s (%s) was not added to race log %s because it already existed there.", event, event.getClass().getName(), getId()));
        }
        return isAdded;
    }

    @Override
    public boolean add(EventT event) {
        return add(event, true);
    }

    @Override
    public boolean load(EventT event) {
        return add(event, false);
    }

    private void revokeIfNecessary(EventT newEvent) {
        if (newEvent instanceof RevokeEvent) {
            RevokeEvent<?> revokeEvent = (RevokeEvent<?>) newEvent;
            try {
                checkIfSuccessfullyRevokes(revokeEvent);
                lockForWrite();
                try {
                    revokedEventIds.add(revokeEvent.getRevokedEventId());
                } finally {
                    unlockAfterWrite();
                }
            } catch (NotRevokableException e) {
                logger.log(Level.WARNING, e.getMessage());
            }
        }
    }

    private void checkIfSuccessfullyRevokes(RevokeEvent<?> revokeEvent) throws NotRevokableException {
        lockForRead();
        EventT revokedEvent;
        try {
            revokedEvent = getEventById(revokeEvent.getRevokedEventId());
        } finally {
            unlockAfterRead();
        }
        if (revokedEvent == null) {
            // it can happen that the event that has been revoked is not yet loaded - as we assume
            // that race log events never get removed we can safely continue and assume that
            // the event will be loaded later
            logger.warning("RevokeEvent for "+revokeEvent.getShortInfo()+" added, that refers to non-existent event to be revoked. Could also happen that the revoke event is before the event to be revoked.");
        } else {
            if (! (revokedEvent instanceof Revokable)) {
                throw new NotRevokableException("RevokeEvent trying to revoke non-revokable event");
            }

            // make sure to compare only author priorities - assuming that revoke events are
            // independent of passes and times
            if (revokeEvent.getAuthor().getPriority() > revokedEvent.getAuthor().getPriority()) {
                throw new NotRevokableException("RevokeEvent does not have sufficient priority");
            }
        }
    }

    @Override
    public Iterable<EventT> add(EventT event, UUID clientId) {
        add(event);
        return getEventsToDeliver(clientId, event);
    }

    @Override
    public Iterable<EventT> getEventsToDeliver(UUID clientId) {
        return getEventsToDeliver(clientId, null);
    }

    protected Iterable<EventT> getEventsToDeliver(UUID clientId, EventT suppressedEvent) {
        final LinkedHashSet<EventT> stillToDeliverToClient;
        lockForRead();
        try {
            stillToDeliverToClient = new LinkedHashSet<EventT>(getInternalRawFixes());
        } finally {
            unlockAfterRead();
        }
        stillToDeliverToClient.remove(suppressedEvent);
        Set<EventT> deliveredToClient = eventsDeliveredToClient.get(clientId);
        if (deliveredToClient != null) {
            stillToDeliverToClient.removeAll(deliveredToClient);
        } else {
            deliveredToClient = new HashSet<EventT>();
            eventsDeliveredToClient.put(clientId, deliveredToClient);
        }
        deliveredToClient.addAll(stillToDeliverToClient);
        deliveredToClient.add(suppressedEvent);
        return stillToDeliverToClient;
    }

    protected void notifyListenersAboutReceive(EventT event) {
        Set<VisitorT> workingListeners = new HashSet<VisitorT>();
        synchronized (listeners) {
            workingListeners.addAll(listeners);
        }
        for (VisitorT listener : workingListeners) {
            try {
                event.accept(listener);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "RaceLogEventVisitor " + listener + " threw exception " + t.getMessage(), t);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return getFirstRawFix() == null;
    }

    @Override
    public void addListener(VisitorT listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(Object listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * When deserializing, needs to initialize empty set of listeners. Furthermore, as a migration effort, when the
     * {@link #eventsById} field was introduced, old clients get <code>null</code> as its value when deserializing which
     * leads to NPEs later on. However, since the map is redundant to the contents of the <code>fixes</code> collection,
     * it can be reconstructed here.
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        listeners = new HashSet<VisitorT>();
        eventsDeliveredToClient = new HashMap<UUID, Set<EventT>>();
        if (eventsById == null) {
            eventsById = new HashMap<Serializable, EventT>();
            lockForRead();
            try {
                for (EventT event : getRawFixes()) {
                    eventsById.put(event.getId(), event);
                }
            } finally {
                unlockAfterRead();
            }
        }
        if (revokedEventIds == null) {
            revokedEventIds = new HashSet<Serializable>();
            lockForRead();
            try {
                for (EventT event : getRawFixes()) {
                    if (event instanceof RevokeEvent) {
                        revokedEventIds.add(((RevokeEvent<?>)event).getRevokedEventId());
                    }
                }
            } finally {
                unlockAfterRead();
            }
        }
    }

    @Override
    public Iterable<EventT> getRawFixesDescending() {
        return getRawFixes().descendingSet();
    }

    @Override
    public Iterable<EventT> getFixesDescending() {
        return getFixes().descendingSet();
    }

    @Override
    public HashSet<VisitorT> removeAllListeners() {
        synchronized (listeners) {
            HashSet<VisitorT> clonedListeners = new HashSet<VisitorT>(listeners);
            listeners = new HashSet<VisitorT>();
            return clonedListeners;
        }
    }

    @Override
    public void addAllListeners(Iterable<VisitorT> listeners) {
        synchronized (listeners) {
            Util.addAll(listeners, this.listeners);
        }
    }

    @Override
    public Iterable<VisitorT> getAllListeners() {
        return this.listeners;
    }

    @Override
    public Iterable<EventT> getRawFixes(UUID clientId) {
        assertReadLock();
        NavigableSet<EventT> result = getRawFixes();
        Set<EventT> edtc = eventsDeliveredToClient.get(clientId);
        if (edtc == null) {
            edtc = new HashSet<EventT>();
            eventsDeliveredToClient.put(clientId, edtc);
        }
        edtc.addAll(result);
        return result;
    }

    @Override
    public EventT getEventById(Serializable id) {
        assertReadLock();
        return eventsById.get(id);
    }

    @Override
    public NavigableSet<EventT> getUnrevokedEvents() {
        return new FilteredPartialNavigableSetView<>(super.getInternalFixes(), new RevokedValidator<>(revokedEventIds));
    }

    @Override
    public NavigableSet<EventT> getUnrevokedEventsDescending() {
        return new FilteredPartialNavigableSetView<EventT>(super.getInternalFixes().descendingSet(), new RevokedValidator<>(revokedEventIds));
    }

    @Override
    public void merge(AbstractLog<EventT, VisitorT> other) {
        lockForWrite();
        other.lockForRead();
        try {
            RaceLogEventComparator comparator = new RaceLogEventComparator();
            Iterator<EventT> thisIter = getRawFixes().iterator();
            Iterator<EventT> otherIter = other.getRawFixes().iterator();
            EventT thisEvent = null;
            EventT otherEvent = null;
            while (otherIter.hasNext() || otherEvent != null) {
                if (thisEvent == null && thisIter.hasNext()) {
                    thisEvent = thisIter.next();
                }
                if (otherEvent == null) {
                    otherEvent = otherIter.next();
                }
                if (thisEvent == null) {
                    // All events of this race log have been consumed; simply keep adding the events
                    // from the other race log to this race log.
                    // otherEvent has to be non-null because if thisIter didn't have a next, otherIter must have had a next
                    add(otherEvent);
                    otherEvent = null; // "consumed" otherEvent; try to grab next if a next element exists in otherIter
                } else {
                    final int comparison = comparator.compare(thisEvent, otherEvent);
                    if (comparison < 0) {
                        thisEvent = null; // skip the "lesser" race log event on this race log
                    } else if (comparison == 0) {
                        // the race log event from the other log is already contained in this log; skip both
                        thisEvent = null;
                        otherEvent = null;
                    } else {
                        // comparison > 0; we skipped on this race log until we found a "greater" event on this race log; insert otherEvent
                        add(otherEvent);
                        otherEvent = null; // "consumed"
                    }
                }
            }
        } finally {
            other.unlockAfterRead();
            unlockAfterWrite();
        }
    }

    @Override
    public void revokeEvent(AbstractLogEventAuthor author, EventT toRevoke) throws NotRevokableException {
        revokeEvent(author, toRevoke, null);
    }

    protected abstract EventT createRevokeEvent(AbstractLogEventAuthor author, EventT toRevoke, String reason);

    @Override
    public void revokeEvent(AbstractLogEventAuthor author, EventT toRevoke, String reason) throws NotRevokableException {
        if (toRevoke == null) {
            throw new NotRevokableException("Received null as event to revoke");
        }
        EventT revokeEvent = createRevokeEvent(author, toRevoke, reason);
        checkIfSuccessfullyRevokes((RevokeEvent<?>) revokeEvent);
        add(revokeEvent);
    }

    @FunctionalInterface
    public interface NavigableSetViewValidator<T> {
        boolean isValid(T item);
    }

    /**
     * Considers an event valid if it is not a {@link RevokeEvent} and is not contained in the set of events passed to
     * the constructor that have been revoked.
     */
    public static class RevokedValidator<EventT extends AbstractLogEvent<VisitorT>, VisitorT> implements NavigableSetViewValidator<EventT> {
        final Set<Serializable> revokedEventIds;

        public RevokedValidator(Set<Serializable> revokedEventIds) {
            this.revokedEventIds = revokedEventIds;
        }

        @Override
        public boolean isValid(EventT item) {
            return !(item instanceof RevokeEvent) && !revokedEventIds.contains(item.getId());
        }
    }

    public static class FilteredPartialNavigableSetView<T> extends PartialNavigableSetView<T> {
        private final NavigableSetViewValidator<T> validator;

        public FilteredPartialNavigableSetView(NavigableSet<T> set, final NavigableSetViewValidator<T> validator) {
            super(set);
            if (validator == null) {
                throw new NullPointerException();
            }
            this.validator = validator;
        }

        @Override
        protected boolean isValid(T t) {
            return validator.isValid(t);
        }
    }
}
