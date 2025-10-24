package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.shared.tracking.impl.MappedTrackImpl;
import com.sap.sailing.domain.tracking.DynamicMappedTrack;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sse.common.Timed;

/**
 * Default implementation of {@link DynamicMappedTrack} interface. 
 *
 * @param <ItemType>
 *            the type of item this track is mapped to
 * @param <FixType>
 *            the type of fix that is contained in this track
 */
public class DynamicMappedTrackImpl<ItemType, FixType extends Timed> extends MappedTrackImpl<ItemType, FixType> 
        implements DynamicMappedTrack<ItemType, FixType> {

    private static final long serialVersionUID = -8705397260656473352L;

    public DynamicMappedTrackImpl(ItemType trackedItem, String nameForReadWriteLock) {
        super(trackedItem, nameForReadWriteLock);
    }

    /**
     * Simply makes the superclass method public as now we need to expose it via the {@link DynamicTrack} interface.
     */
    @Override
    public boolean add(FixType fix) {
        return super.add(fix);
    }

    @Override
    public boolean add(FixType fix, boolean replace) {
        return super.add(fix, replace);
    }
}
