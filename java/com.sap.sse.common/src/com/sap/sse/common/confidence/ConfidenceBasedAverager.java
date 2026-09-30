package com.sap.sse.common.confidence;

import java.util.Iterator;

import com.sap.sse.common.scalablevalue.HasConfidenceAndIsScalable;

public interface ConfidenceBasedAverager<ValueType, BaseType, RelativeTo> {
    /**
     * If a non-<code>null</code> weigher has been set for this averager, <code>at</code> must be a valid reference
     * point to which the weigher will determine the difference and from it the confidence for each of the
     * <code>values</code>. Otherwise, the <code>at</code> argument is ignored.
     * 
     * @return <code>null</code> if <code>values==null</code> or <code>values</code> is empty; otherwise, a non-
     *         <code>null</code> average with confidence is returned. Note, however, that the
     *         {@link HasConfidence#getObject() object} in the {@link HasConfidence} result may be <code>null</code>,
     *         e.g., if the combined confidence went to 0.0 and the averager was unable to determine a reasonable
     *         average value. The result's confidence is the arithmetic average of the confidences of all values
     *         that contributed to the result.
     */
    HasConfidence<ValueType, BaseType, RelativeTo> getAverage(
            Iterable<? extends HasConfidenceAndIsScalable<ValueType, BaseType, RelativeTo>> values, RelativeTo at);

    HasConfidence<ValueType, BaseType, RelativeTo> getAverage(
            Iterator<? extends HasConfidenceAndIsScalable<ValueType, BaseType, RelativeTo>> values, RelativeTo at);
}
