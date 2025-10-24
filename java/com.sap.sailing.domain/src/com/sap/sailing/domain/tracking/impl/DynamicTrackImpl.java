package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.shared.tracking.impl.TrackImpl;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sse.common.Timed;

public class DynamicTrackImpl<FixType extends Timed> extends TrackImpl<FixType> implements DynamicTrack<FixType> {
    private static final long serialVersionUID = 917778209274148097L;

    public DynamicTrackImpl(String nameForReadWriteLock) {
        super(nameForReadWriteLock);
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
