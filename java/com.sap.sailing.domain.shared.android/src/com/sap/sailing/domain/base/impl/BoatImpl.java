package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

import com.sap.sailing.domain.base.BoatChangeListener;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.impl.NamedImpl;
import com.sap.sse.common.Util;

public class BoatImpl extends NamedImpl implements DynamicBoat {
    private static final long serialVersionUID = 3489730487528955788L;
    private final BoatClass boatClass;
    private String sailID;
    private transient Set<BoatChangeListener> listeners;
    
    public BoatImpl(String name, BoatClass boatClass, String sailID) {
        super(name);
        this.boatClass = boatClass;
        this.sailID = sailID;
        this.listeners = new HashSet<BoatChangeListener>();
    }
    
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        listeners = new HashSet<BoatChangeListener>();
    }

    @Override
    public BoatClass getBoatClass() {
        return boatClass;
    }

    @Override
    public String getSailID() {
        return sailID;
    }

    @Override
    public void setSailId(String newSailId) {
        final String oldSailId = this.sailID;
        if (!Util.equalsWithNull(oldSailId, newSailId)) {
            this.sailID = newSailId;
            for (BoatChangeListener listener : getListeners()) {
                listener.sailIdChanged(oldSailId, newSailId);
            }
        }
    }

    @Override
    public void addBoatChangeListener(BoatChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeCompetitorChangeListener(BoatChangeListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    private Iterable<BoatChangeListener> getListeners() {
        synchronized (listeners) {
            return new HashSet<BoatChangeListener>(listeners);
        }
    }
}
