package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.shared.tracking.AddResult;

/**
 * Listener to observe a {@link com.sap.sailing.domain.tracking.SensorFixTrack} to get informed whenever a new fix is
 * being added to the track.
 *
 * @param <ItemType>
 *            the type of item the {@link com.sap.sailing.domain.tracking.SensorFixTrack} is mapped to
 * @param <FixType>
 *            the type of fix the {@link com.sap.sailing.domain.tracking.SensorFixTrack} contains
 */
public interface SensorFixTrackListener<ItemType, FixType extends SensorFix> extends TrackListener {
    
    /**
     * Called whenever a new fix is added to the {@link com.sap.sailing.domain.tracking.SensorFixTrack} the listener observes.
     * 
     * @param fix the new fix
     * @param item the item the track is mapped to
     * @param trackName the name of the track
     * @param firstFixInTrack <code>true</code> if the new fix is the first one that's added to the track, <code>false</code> otherwise.
     * @param addedOrReplaced whether the fix was {@link AddResult#ADDED added} or {@link AddResult#REPLACED replaced}
     */
    void fixReceived(FixType fix, ItemType item, String trackName, boolean firstFixInTrack, AddResult addedOrReplaced);

}
