package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sse.common.Timed;

public interface DynamicTrack<FixType extends Timed> extends Track<FixType> {
    /**
     * Tries to add {@code fix} to this track. If an equal (by definition of the comparator used for the fixes set) fix
     * already exists in this track, the track remains unchanged and {@code false} will be returned.
     * 
     * @return <code>true</code> if the element was added, <code>false</code> otherwise.
     */
    boolean add(FixType fix);

    /**
     * Tries to add {@code fix} to this track. If an equal (by definition of the comparator used for the fixes set) fix
     * already exists in this track and {@code replace} is {@code false}, the track remains unchanged and {@code false}
     * will be returned. If {@code replace} is {@code true}, an equal (by definition of the comparator used for the
     * fixes set) fix will replace the fix contained in this track.
     * 
     * @param replace
     *            whether or not to replace an existing fix in the track that is equal to {@link #fix} as defined by the
     *            comparator used for the {@link #fixesConsideredAffectedByFinder} set. By default this is a comparator only comparing the fixes'
     *            time stamps. Subclasses may use different comparator implementations.
     * 
     * @return <code>true</code> if the element was added, <code>false</code> otherwise.
     */
    boolean add(FixType fix, boolean replace);
}
