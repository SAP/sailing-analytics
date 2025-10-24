package com.sap.sailing.domain.shared.tracking;

import com.sap.sse.common.Timed;

/**
 * {@link Track} specialization, which is mapped to a specific type of items.
 * 
 * @param <ItemType>
 *            the type of item this track is mapped to
 * @param <FixType>
 *            the type of fix that is contained in this track
 */
public interface MappedTrack<ItemType, FixType extends Timed> extends Track<FixType> {
    
    /**
     * @return the item this track is mapped to.
     */
    ItemType getTrackedItem();

}
