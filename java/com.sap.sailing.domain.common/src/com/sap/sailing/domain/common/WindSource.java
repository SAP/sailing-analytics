package com.sap.sailing.domain.common;

import java.io.Serializable;

import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;



/**
 * Value-object identifying possible sources for wind data. Used to key and select between different {@link WindTrack}s.
 * Objects of this class have their {@link Object#equals(Object)} and {@link Object#hashCode()} methods defined
 * accordingly.
 * <p>
 * 
 * While a wind source may represent, e.g., a wind sensor which may move around, such as on a coach boat, separate wind
 * sources should be used to identify separate measurement spots. For example, the wind data from a GRIB file should
 * have a separate wind source per position in the GRIB file's grid. Imagine a wind source as something that could be
 * displayed as a small wind arrow on a map. Consider using {@link WindSourceWithAdditionalID} to keep wind sources of
 * the same type apart by giving them distinct IDs.
 * <p>
 * 
 * Conceptually, this would also work for {@link WindSourceType#MANEUVER_BASED_ESTIMATION}, but then every competitor
 * would have to be its own wind source. While theoretically possible, this may not pay off given the ultra-low
 * confidence of that specific source, and hence it seems okay that the respective virtual wind track combines all fixes
 * inferred from maneuvers into the same single wind source of type {@link WindSourceType#MANEUVER_BASED_ESTIMATION}.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface WindSource extends Serializable {
    WindSourceType getType();
    
    /**
     * @return {@link #getType()}.{@link WindSourceType#canBeStored() canBeStored()}
     */
    boolean canBeStored();

    /**
     * Usually the name of this wind source's {@link #getType() type}, optionally suffixed by an identifier, e.g., in case
     * there are multiple sources of the same type.
     */
    String name();

    /**
     * An optional ID. May be <code>null</code>. Can be used to distinguish different wind sources of the same type.
     */
    Object getId();
    
    /**
     * "Serializes" the {@link #getType type} and the {@link #getId() ID} into a single string that can be used
     * to produce a {@link WindSource} again using the {@link #fromTypeAndId(String)} factory method.
     */
    default String getTypeAndId() {
        return getType().name() + (getId() == null ? "" : " ("+getId().toString()+")");
    }
    
    /**
     * Produces a {@link WindSource} from a string as returned by {@link #getTypeAndId()}.
     * 
     * @param typeAndId
     *            if {@code null}, the result will be {@code null}.
     */
    static WindSource fromTypeAndId(String typeAndId) {
        final WindSource result;
        if (typeAndId == null) {
            result = null;
        } else {
            final int space = typeAndId.indexOf(' ');
            if (space == -1) {
                result = new WindSourceImpl(WindSourceType.valueOf(typeAndId));
            } else {
                final WindSourceType type = WindSourceType.valueOf(typeAndId.substring(0, space));
                final String id = typeAndId.substring(typeAndId.indexOf('(')+1, typeAndId.indexOf(')'));
                result = new WindSourceWithAdditionalID(type, id);
            }
        }
        return result;
    }
}
