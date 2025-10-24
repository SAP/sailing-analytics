package com.sap.sailing.domain.shared.tracking.impl;

import com.sap.sailing.domain.shared.tracking.MappedTrack;
import com.sap.sse.common.Timed;
import com.sap.sse.shared.util.impl.ArrayListNavigableSet;

/**
 * Default implementation of {@link MappedTrack} interface. 
 *
 * @param <ItemType>
 *            the type of item this track is mapped to
 * @param <FixType>
 *            the type of fix that is contained in this track
 */
public class MappedTrackImpl<ItemType, FixType extends Timed> extends TrackImpl<FixType>
        implements MappedTrack<ItemType, FixType> {

    private static final long serialVersionUID = 6165693342087329096L;
    
    private final ItemType trackedItem;

    /** @see TrackImpl#TrackImpl(String) */
    public MappedTrackImpl(ItemType trackedItem, String nameForReadWriteLock) {
        super(nameForReadWriteLock);
        this.trackedItem = trackedItem;
    }
    
    /** @see TrackImpl#TrackImpl(ArrayListNavigableSet, String) */
    protected MappedTrackImpl(ItemType trackedItem, ArrayListNavigableSet<Timed> fixes, String nameForReadWriteLock) {
        super(fixes, nameForReadWriteLock);
        this.trackedItem = trackedItem;
    }

    @Override
    public ItemType getTrackedItem() {
        return trackedItem;
    }

}
