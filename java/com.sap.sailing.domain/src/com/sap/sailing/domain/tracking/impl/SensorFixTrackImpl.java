package com.sap.sailing.domain.tracking.impl;

import java.io.Serializable;
import java.util.function.Consumer;

import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.tracking.DynamicSensorFixTrack;
import com.sap.sailing.domain.tracking.SensorFixTrack;
import com.sap.sailing.domain.tracking.SensorFixTrackListener;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.WithID;

/**
 * Implementation of {@link SensorFixTrack} and {@link DynamicSensorFixTrack}.
 *
 * @param <ItemType> the type of item this track is mapped to
 * @param <FixT> the type of fix this track holds
 */
public class SensorFixTrackImpl<ItemType extends WithID & Serializable, FixT extends SensorFix> extends
        DynamicMappedTrackImpl<ItemType, FixT> implements DynamicSensorFixTrack<ItemType, FixT> {

    private static final long serialVersionUID = 6383421895429843002L;
    
    private final String trackName;
    private final TrackListenerCollection<ItemType, FixT, SensorFixTrackListener<ItemType, FixT>> listeners;

    /**
     * @param trackedItem the item this track is mapped to
     * @param trackName the name of the track by which it can be obtained from the {@link TrackedRace}.
     * @param nameForReadWriteLock the name to use for the lock object that is used internally
     */
    public SensorFixTrackImpl(ItemType trackedItem, String trackName, String nameForReadWriteLock) {
        super(trackedItem, nameForReadWriteLock);
        this.trackName = trackName;
        this.listeners = new TrackListenerCollection<>();
    }
    
    @Override
    public boolean add(FixT fix, boolean replace) {
        final boolean result;
        lockForWrite();
        try {
            final boolean firstFixInTrack = getRawFixes().isEmpty();
            final AddResult addResult = addWithoutLocking(fix, replace);
            result = addResult == AddResult.ADDED || addResult == AddResult.REPLACED;
            if (result) {
                this.notifyListeners((listener) -> listener.fixReceived(fix, getTrackedItem(), trackName, firstFixInTrack, addResult));
            }
        } finally {
            unlockAfterWrite();
        }
        return result;
    }
    
    protected void notifyListeners(Consumer<SensorFixTrackListener<ItemType, FixT>> notification) {
        listeners.getListeners().forEach(notification);
    }

    @Override
    public void addListener(SensorFixTrackListener<ItemType, FixT> listener) {
        this.listeners.addListener(listener);
    }
    
    @Override
    public void removeListener(SensorFixTrackListener<ItemType, FixT> listener) {
        this.listeners.removeListener(listener);
    }

    @Override
    public String getTrackName() {
        return trackName;
    }

}