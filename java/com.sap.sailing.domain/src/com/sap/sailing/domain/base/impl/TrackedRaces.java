package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * Wraps the trackedRaces map in the {@link AbstractRaceColumn} allowing to serialize an empty map when a master data
 * import is running by using the {@link ThreadLocal} flag {@link #ongoingMasterDataExport}. If the flag is
 * <code>false</code> the map is serialized as is.
 * 
 * 
 * @author Frederik Petersen (D054528)
 * 
 */
public class TrackedRaces implements Serializable {

    private static final long serialVersionUID = -5521115213267651333L;

    /**
     * we don't want the TrackedRaces to be serialized during a master data export. Thus, we need this thread flag which
     * is true only during master data export
     */
    private transient ThreadLocal<Boolean> ongoingMasterDataExport;

    private Map<Fleet, TrackedRace> trackedRaces;

    public TrackedRaces(Map<Fleet, TrackedRace> trackedRaces) {
        this.trackedRaces = trackedRaces;
        this.ongoingMasterDataExport = createOngoingMasterDataExportThreadLocal();
    }

    public TrackedRaces() {
        this(new HashMap<Fleet, TrackedRace>());
    }

    public TrackedRace get(Fleet fleet) {
        return trackedRaces.get(fleet);
    }

    public void put(Fleet fleet, TrackedRace trackedRace) {
        trackedRaces.put(fleet, trackedRace);
    }

    public void remove(Fleet fleet) {
        trackedRaces.remove(fleet);
    }

    public Set<Entry<Fleet, TrackedRace>> entrySet() {
        return trackedRaces.entrySet();
    }

    public boolean isEmpty() {
        return trackedRaces.isEmpty();
    }

    public Collection<TrackedRace> values() {
        return trackedRaces.values();
    }

    private ThreadLocal<Boolean> createOngoingMasterDataExportThreadLocal() {
        return new ThreadLocal<Boolean>() {
            protected Boolean initialValue() {
                return false;
            };
        };
    }

    public void setMasterDataExportOngoingThreadFlag(boolean flagValue) {
        ongoingMasterDataExport.set(flagValue);
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        final boolean masterDataImportOngoing = ois.readBoolean();
        if (masterDataImportOngoing) {
            trackedRaces = new HashMap<>();
        } else {
            ois.defaultReadObject();
        }
        this.ongoingMasterDataExport = createOngoingMasterDataExportThreadLocal();
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        final Boolean masterDataExportOngoing = ongoingMasterDataExport.get();
        oos.writeBoolean(masterDataExportOngoing);
        if (!masterDataExportOngoing) {
            oos.defaultWriteObject();
        }
    }

}
