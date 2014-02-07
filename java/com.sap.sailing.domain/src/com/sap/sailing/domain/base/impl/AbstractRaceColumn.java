package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.RaceLogIdentifierTemplate;
import com.sap.sailing.domain.racelog.RaceLogInformation;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.racelog.impl.RaceLogIdentifierImpl;
import com.sap.sailing.domain.tracking.TrackedRace;

public abstract class AbstractRaceColumn extends SimpleAbstractRaceColumn implements RaceColumn {
    private static final long serialVersionUID = -7801617988982540470L;

    private final Map<Fleet, TrackedRace> trackedRaces;
    private final Map<Fleet, RaceIdentifier> raceIdentifiers;

    private final Map<Fleet, RaceLog> raceLogs;
    
    /**
     * holds the race log identifier template needed to create the appropriate RaceLogIdentifer that is constructed from the 
     * parent object name, name of this raceColumn and the name of a fleet of this raceColumn to access the RaceLog in the 
     * RaceLogStore for persistence purposes.
     */
    private transient RaceLogInformation raceLogInformation;
    private RaceLogIdentifierTemplate raceLogIdentifierTemplate;

    /**
     * we don't want the TrackedRaces to be serialized during a master data export. Thus, we need this thread flag which
     * is true only during master data export
     */
    private transient ThreadLocal<Boolean> ongoingMasterDataExport = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        };
    };

    public AbstractRaceColumn() {
        this.trackedRaces = new HashMap<Fleet, TrackedRace>();
        this.raceIdentifiers = new HashMap<Fleet, RaceIdentifier>();
        this.raceLogs = new HashMap<Fleet, RaceLog>();
    }

    @Override
    public synchronized void setRaceLogInformation(final RaceLogInformation information) {
        for (final Fleet fleet : getFleets()) {
            setOrReloadRaceLogInformation(information, fleet);
        }
    }

    @Override
    public RaceLog getRaceLog(Fleet fleet) {
        synchronized (raceLogs) {
            return raceLogs.get(fleet);
        }
    }
    
    @Override
    public RaceLogIdentifier getRaceLogIdentifier(Fleet fleet) {
        return new RaceLogIdentifierImpl(raceLogInformation.getIdentifierTemplate(), getName(), fleet);
    }

    @Override
    public TrackedRace getTrackedRace(Fleet fleet) {
        return trackedRaces.get(fleet);
    }

    @Override
    public void setTrackedRace(Fleet fleet, TrackedRace trackedRace) {
        TrackedRace previouslyLinkedRace = this.trackedRaces.get(fleet);
        if (trackedRace != previouslyLinkedRace) {
            releaseTrackedRace(fleet);
            synchronized (this) {
                if (trackedRace != null) {
                    this.trackedRaces.put(fleet, trackedRace);
                    this.setRaceIdentifier(fleet, trackedRace.getRaceIdentifier());
                }
            }
            if (trackedRace != null) {
                trackedRace.attachRaceLog(getRaceLog(fleet));
                getRaceColumnListeners().notifyListenersAboutTrackedRaceLinked(this, fleet, trackedRace);
            }
        }
    }

    @Override
    public RaceIdentifier getRaceIdentifier(Fleet fleet) {
        return raceIdentifiers.get(fleet);
    }

    @Override
    public void setRaceIdentifier(Fleet fleet, RaceIdentifier raceIdentifier) {
        if (raceIdentifier != null) {
            this.raceIdentifiers.put(fleet, raceIdentifier);
        }
    }

    @Override
    public synchronized void releaseTrackedRace(Fleet fleet) {
        TrackedRace previouslyLinkedRace = this.trackedRaces.get(fleet);
        this.trackedRaces.remove(fleet);
        if (previouslyLinkedRace != null && raceLogInformation != null) {
            RaceLogIdentifierImpl identifier = new RaceLogIdentifierImpl(raceLogInformation.getIdentifierTemplate(), getName(), fleet);
            previouslyLinkedRace.detachRaceLog(identifier.getIdentifier());
            getRaceColumnListeners().notifyListenersAboutTrackedRaceUnlinked(this, fleet, previouslyLinkedRace);
        }
    }
    
    @Override
    public void removeRaceIdentifier(Fleet fleet) {
        releaseTrackedRace(fleet);
        this.raceIdentifiers.remove(fleet);
    }
    
    @Override
    public Fleet getFleetOfCompetitor(Competitor competitor) {
        for (Map.Entry<Fleet, TrackedRace> e : trackedRaces.entrySet()) {
            if (Util.contains(e.getValue().getRace().getCompetitors(), competitor)) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public Fleet getFleetByName(String fleetName) {
        for (Fleet fleet : getFleets()) {
            if (fleet.getName().equals(fleetName)) {
                return fleet;
            }
        }
        return null;
    }

    @Override
    public boolean hasTrackedRaces() {
        return !trackedRaces.isEmpty();
    }

    @Override
    public TrackedRace getTrackedRace(Competitor competitor) {
        for (TrackedRace trackedRace : trackedRaces.values()) {
            if (Util.contains(trackedRace.getRace().getCompetitors(), competitor)) {
                return trackedRace;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public RaceLogInformation getRaceLogInformation() {
        return raceLogInformation;
    }

    @Override
    public void setOrReloadRaceLogInformation(RaceLogInformation information, Fleet fleetImpl) {
        synchronized(raceLogs) {
            raceLogInformation = information;
            RaceLogStore store = information.getStore();
            HashSet<RaceLogEventVisitor> listeners = new HashSet<RaceLogEventVisitor>();
            RaceLog raceLogAvailable = raceLogs.get(fleetImpl);
            if (raceLogAvailable != null) {
                store.removeListenersAddedByStoreFrom(raceLogAvailable);
                listeners = raceLogAvailable.removeAllListeners();
                raceLogs.remove(fleetImpl);
            }
            
            raceLogIdentifierTemplate = raceLogInformation.getIdentifierTemplate();
            RaceLogIdentifier identifier = raceLogIdentifierTemplate.compileRaceLogIdentifier(fleetImpl);
            RaceLog raceLog = store.getRaceLog(identifier, /*ignoreCache*/ true);
            
            if (listeners.isEmpty()) {
                RaceColumnRaceLogReplicator listener = new RaceColumnRaceLogReplicator(this, identifier);
                raceLog.addListener(listener);
            } else {
                raceLog.addAllListeners(listeners);
            }
            raceLogs.put(fleetImpl, raceLog);
        }
    }

    /**
     * When deserializing, replication listeners are registered on all race logs.
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        for (Entry<Fleet, RaceLog> entry : raceLogs.entrySet()) {
            Fleet fleet = entry.getKey();
            RaceLog raceLog = entry.getValue();
            raceLog.addListener(new RaceColumnRaceLogReplicator(this, raceLogIdentifierTemplate.compileRaceLogIdentifier(fleet)));
        }
    }

    /**
     * While set to true, any serialization in the current thread will not include the tracked races. Make sure to set
     * back to false, after serialization. (in finally block)
     * 
     * @param flagValue
     *            set to false for default behavior, set to true to exclude tracked races
     */
    public void setMasterDataExportOngoingThreadFlag(boolean flagValue) {
        ongoingMasterDataExport.set(flagValue);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (ongoingMasterDataExport.get()) {
            stream.writeObject(new HashMap<Fleet, TrackedRace>());
            stream.writeObject(raceIdentifiers);
            stream.writeObject(raceLogs);
            stream.writeObject(raceLogIdentifierTemplate);
        } else {
            stream.defaultWriteObject();
        }
    }
}
