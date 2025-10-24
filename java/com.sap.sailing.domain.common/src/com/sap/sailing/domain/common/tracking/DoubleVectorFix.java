package com.sap.sailing.domain.common.tracking;

import com.sap.sse.common.Timed;

/**
 * A fix that simply holds an array of {@link Double} values. The interpretation of the data depends on the concrete
 * mapping in the RegattaLog. Asking beyond the end of the vector using {@link #get(int)} will return {@code null} but
 * not throw an exception.
 */
public interface DoubleVectorFix extends Timed {
    /**
     * Obtains a copy of the array of {@link Double} values in this fix. Changes to the array returned
     * do <em>not</em> modify this fix.
     */
    Double[] get();

    Double get(int index);
    /**
     * Tells whether at least one component is not {@code null}
     */
    boolean hasValidData();
}
