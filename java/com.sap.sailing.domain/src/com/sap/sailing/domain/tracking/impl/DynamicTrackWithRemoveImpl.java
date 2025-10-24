package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.tracking.DynamicTrackWithRemove;
import com.sap.sse.common.Timed;
import com.sap.sse.shared.util.NavigableSetWithRemove;

public class DynamicTrackWithRemoveImpl<FixType extends Timed> extends DynamicTrackImpl<FixType> implements DynamicTrackWithRemove<FixType> {
    private static final long serialVersionUID = 2124397433912003485L;

    public DynamicTrackWithRemoveImpl(String nameForReadWriteLock) {
        super(nameForReadWriteLock);
    }

    @Override
    protected NavigableSetWithRemove<FixType> getInternalRawFixes() {
        return (NavigableSetWithRemove<FixType>) super.getInternalRawFixes();
    }

    @Override
    protected NavigableSetWithRemove<FixType> getInternalFixes() {
        return (NavigableSetWithRemove<FixType>) super.getInternalFixes();
    }

    @Override
    public boolean remove(FixType fix) {
        return getInternalFixes().remove(fix);
    }
    
    @Override
    public void removeAllUpToExcluding(FixType fix) {
        getInternalFixes().removeAllLessThan(fix);
    }
}
