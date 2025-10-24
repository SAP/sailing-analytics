package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.shared.tracking.MappedTrack;
import com.sap.sse.common.Timed;

/**
 * Extension of {@link MappedTrack} interface, which also implements {@link DynamicTrack} interface.
 *
* @param <ItemType>
 *            the type of item this track is mapped to
 * @param <FixType>
 *            the type of fix that is contained in this track
 */
public interface DynamicMappedTrack<ItemType, FixType extends Timed>
        extends MappedTrack<ItemType, FixType>, DynamicTrack<FixType> {
}
