package com.sap.sailing.domain.tracking;

import java.io.Serializable;

import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sse.common.WithID;

/**
 * {@link Track} that holds {@link SensorFix} instances.
 * 
 * As {@link SensorFix}es allow value name based access, the track provides the supported value names.
 *
 * @param <ItemType> the type of item this track is mapped to
 * @param <FixT> the type of fix that is contained in this track
 */
public interface SensorFixTrack<ItemType extends WithID & Serializable, FixT extends SensorFix> extends Track<FixT> {
    
    /**
     * @return the value names that fixes contained in this track support
     */
    Iterable<String> getValueNames();
    
    void addListener(SensorFixTrackListener<ItemType, FixT> listener);
    
    void removeListener(SensorFixTrackListener<ItemType, FixT> listener);
    
    /**
     * @return the item this tracked is mapped to.
     */
    ItemType getTrackedItem();
    
    /**
     * @return the associated track name by which this track can be obtained from the {@link TrackedRace}.
     */
    String getTrackName();

}
