package com.sap.sailing.domain.racelog.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Timed;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.tracking.Track;
import com.sap.sailing.domain.tracking.impl.PartialNavigableSetView;
import com.sap.sailing.domain.tracking.impl.TrackImpl;
import com.sap.sailing.util.impl.ArrayListNavigableSet;

/**
 * {@link Track} implementation for {@link RaceLogEvent}s.
 * 
 * <p>
 * "Fix" validity is decided based on the {@link #getCurrentPassId() current pass}. The validity is not cached.
 * </p>
 * 
 * <p>
 * {@link TrackImpl#getDummyFix(com.sap.sailing.domain.common.TimePoint)} is not overridden, see
 * {@link RaceLogEventComparator} for sorting when interface methods like
 * {@link Track#getFirstFixAfter(com.sap.sailing.domain.common.TimePoint)} are used.
 * </p>
 * 
 */
public class RaceLogImpl extends TrackImpl<RaceLogEvent> implements RaceLog {
    private static final long serialVersionUID = -176745401321893502L;
    private static final String DefaultLockName = RaceLogImpl.class.getName() + ".lock";
    private final static Logger logger = Logger.getLogger(RaceLogImpl.class.getName());

    /**
     * Clients can use the {@link #add(RaceLogEvent, UUID)} method
     */
    private transient Map<UUID, Set<RaceLogEvent>> eventsDeliveredToClient = new HashMap<UUID, Set<RaceLogEvent>>();

    private final Serializable id;
    private transient Set<RaceLogEventVisitor> listeners;
    private int currentPassId;

    /**
     * Initializes a new {@link RaceLogImpl} with the default lock name.
     */
    public RaceLogImpl(Serializable identifier) {
        this(DefaultLockName, identifier);
    }

    /**
     * Initializes a new {@link RaceLogImpl}.
     * 
     * @param nameForReadWriteLock
     *            name of lock.
     */
    public RaceLogImpl(String nameForReadWriteLock, Serializable identifier) {
        super(new ArrayListNavigableSet<Timed>(RaceLogEventComparator.INSTANCE), nameForReadWriteLock);

        this.listeners = new HashSet<RaceLogEventVisitor>();
        this.currentPassId = DefaultPassId;
        this.id = identifier;
    }

    @Override
    public Serializable getId() {
        return this.id;
    }

    @Override
    public int getCurrentPassId() {
        return currentPassId;
    }

    /**
     * Sets a new active pass id. Ignored if new and current are equal.
     * 
     * @param newPassId
     *            to be set.
     */
    public void setCurrentPassId(int newPassId) {
        if (newPassId != this.currentPassId) {
            logger.finer(String.format("Changing pass id to %d", newPassId));
            this.currentPassId = newPassId;
        }
    }

    @Override
    public boolean add(RaceLogEvent event) {
        boolean isAdded = false;
        lockForWrite();
        try {
            isAdded = getInternalRawFixes().add(event);
        } finally {
            unlockAfterWrite();
        }
        if (isAdded) {
            logger.finer(String.format("%s (%s) was added to log.", event, event.getClass().getName()));
            // FIXME with out-of-order delivery would destroy currentPassId; need to check at least the createdAt time
            // point
            setCurrentPassId(Math.max(event.getPassId(), this.currentPassId));
            notifyListenersAboutReceive(event);
        } else {
            logger.warning(String.format("%s (%s) was not added to log. Ignoring", event, event.getClass().getName()));
        }
        return isAdded;
    }

    @Override
    public boolean load(RaceLogEvent event) {
        boolean isAdded = false;
        lockForWrite();
        try {
            isAdded = getInternalRawFixes().add(event);
        } finally {
            unlockAfterWrite();
        }
        if (isAdded) {
            logger.finer(String.format("%s (%s) was loaded into log.", event, event.getClass().getName()));
            setCurrentPassId(Math.max(event.getPassId(), this.currentPassId));
        } else {
            logger.warning(String
                    .format("%s (%s) was not loaded into log. Ignoring", event, event.getClass().getName()));
        }
        return isAdded;
    }

    @Override
    public Iterable<RaceLogEvent> add(RaceLogEvent event, UUID clientId) {
        boolean isAdded = false;
        lockForWrite();
        try {
            isAdded = getInternalRawFixes().add(event);
        } finally {
            unlockAfterWrite();
        }
        if (isAdded) {
            logger.finer(String.format("%s (%s) was added to log.", event, event.getClass().getName()));
            // FIXME with out-of-order delivery would destroy currentPassId; need to check at least the createdAt time
            // point
            setCurrentPassId(Math.max(event.getPassId(), this.currentPassId));
            notifyListenersAboutReceive(event);
        } else {
            logger.warning(String.format("%s (%s) was not added to log. Ignoring", event, event.getClass().getName()));
        }
        return getEventsToDeliver(clientId, event);
    }
    
    @Override
    public Iterable<RaceLogEvent> getEventsToDeliver(UUID clientId) {
        return getEventsToDeliver(clientId, null);
    }

    protected Iterable<RaceLogEvent> getEventsToDeliver(UUID clientId, RaceLogEvent suppressedEvent) {
        // FIXME lock for read getInternalRawFixes?
        LinkedHashSet<RaceLogEvent> stillToDeliverToClient = new LinkedHashSet<RaceLogEvent>(getInternalRawFixes());
        stillToDeliverToClient.remove(suppressedEvent);
        Set<RaceLogEvent> deliveredToClient = eventsDeliveredToClient.get(clientId);
        if (deliveredToClient != null) {
            stillToDeliverToClient.removeAll(deliveredToClient);
        } else {
            deliveredToClient = new HashSet<RaceLogEvent>();
            eventsDeliveredToClient.put(clientId, deliveredToClient);
        }
        deliveredToClient.addAll(stillToDeliverToClient);
        deliveredToClient.add(suppressedEvent);
        return stillToDeliverToClient;
    }

    protected void notifyListenersAboutReceive(RaceLogEvent event) {
        Set<RaceLogEventVisitor> workingListeners = new HashSet<RaceLogEventVisitor>();
        synchronized (listeners) {
            workingListeners.addAll(listeners);
        }
        for (RaceLogEventVisitor listener : workingListeners) {
            try {
                event.accept(listener);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "RaceLogEventVisitor " + listener + " threw exception " + t.getMessage());
                logger.throwing(RaceLogImpl.class.getName(), "notifyListenersAboutReceive(RaceLogEvent)", t);
                t.printStackTrace();
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return getFirstRawFix() == null;
    }

    @Override
    public void addListener(RaceLogEventVisitor listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(RaceLogEventVisitor listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    protected NavigableSet<RaceLogEvent> getInternalFixes() {
        return new PartialNavigableSetView<RaceLogEvent>(super.getInternalFixes()) {
            @Override
            protected boolean isValid(RaceLogEvent e) {
                return e.getPassId() == getCurrentPassId();
            }
        };
    }

    /**
     * When deserializing, needs to initialize empty set of listeners.
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        listeners = new HashSet<RaceLogEventVisitor>();
        eventsDeliveredToClient = new HashMap<UUID, Set<RaceLogEvent>>();
    }

    @Override
    public Iterable<RaceLogEvent> getRawFixesDescending() {
        return getRawFixes().descendingSet();
    }

    @Override
    public Iterable<RaceLogEvent> getFixesDescending() {
        return getFixes().descendingSet();
    }

    @Override
    public HashSet<RaceLogEventVisitor> removeAllListeners() {
        synchronized (listeners) {
            HashSet<RaceLogEventVisitor> clonedListeners = new HashSet<RaceLogEventVisitor>(listeners);
            listeners = new HashSet<RaceLogEventVisitor>();
            return clonedListeners;
        }
    }

    @Override
    public void addAllListeners(HashSet<RaceLogEventVisitor> listeners) {
        synchronized (listeners) {
            this.listeners.addAll(listeners);
        }
    }

    @Override
    public Iterable<RaceLogEventVisitor> getAllListeners() {
        return this.listeners;
    }

    @Override
    public Iterable<RaceLogEvent> getRawFixes(UUID clientId) {
        assertReadLock();
        NavigableSet<RaceLogEvent> result = getRawFixes();
        Set<RaceLogEvent> edtc = eventsDeliveredToClient.get(clientId);
        if (edtc == null) {
            edtc = new HashSet<RaceLogEvent>();
            eventsDeliveredToClient.put(clientId, edtc);
        }
        edtc.addAll(result);
        return result;
    }
}
