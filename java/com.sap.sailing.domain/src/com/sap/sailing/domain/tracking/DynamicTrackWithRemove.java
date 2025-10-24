package com.sap.sailing.domain.tracking;

import com.sap.sse.common.Timed;

/**
 * A type of track that allows for dynamic updates, including the ability to remove fixes.<p>
 * 
 * Removing fixes at the beginning of the track can be expensive because the default implementation uses
 * an array list to store the fixes. Do this only if on average your fixes collection is small (e.g., less
 * than 100 fixes).<p>
 * 
 * Note also that any values inferred from the fixes in this track may becomoe obsolete when removing fixes.
 * In particular, if you work with listeners for {@link #add(Timed) adding} fixes, you will most likely
 * also need to worry about listeners for {@link #remove(Timed) removing} fixes.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 * @param <FixType>
 */
public interface DynamicTrackWithRemove<FixType extends Timed> extends DynamicTrack<FixType> {
    /**
     * Removes the specified fix from the track if it exists.
     * 
     * @param fix the fix to be removed
     * @return <code>true</code> if the fix was successfully removed, <code>false</code> otherwise
     */
    boolean remove(FixType fix);

    void removeAllUpToExcluding(FixType fix);
}
