package com.sap.sse.common.confidence;

import java.io.Serializable;

/**
 * A weigher can compute a confidence value for a distance of two <code>RelativeTo</code> objects.
 * 
 * @author Axel Uhl (d043530)
 */
public interface Weigher<RelativeTo> extends Serializable {
    /**
     * Computes a confidence, based on some weighed notion of "distance" between the two <code>RelativeTo</code> objects.
     * 
     * @param fix the reference point of some object for which to determine the confidence value
     * @param request the reference point provided with some aggregation request; the confidence value returned for the fix is
     * relative to the request's reference point.
     */
    double getConfidence(RelativeTo fix, RelativeTo request);
}
