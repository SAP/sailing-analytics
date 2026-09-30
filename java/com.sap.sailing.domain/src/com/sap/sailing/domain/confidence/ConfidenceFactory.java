package com.sap.sailing.domain.confidence;

import com.sap.sailing.domain.confidence.impl.ConfidenceFactoryImpl;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.confidence.Weigher;

public interface ConfidenceFactory extends com.sap.sse.common.confidence.ConfidenceFactory {
    ConfidenceFactory INSTANCE = new ConfidenceFactoryImpl();
    
    /**
     * Produces a specialized averaged which can deal with the special case that {@link WindWithConfidence} objects have
     * an internal <code>useSpeed</code> flag which may, when set to <code>false</code> suppress the consideration of
     * the wind fix's speed (not the bearing) in computing the average. For this to work, the averager has to maintain a
     * separate confidence sum for the speed values considered.
     * 
     * @param weigher
     *            If <code>null</code>, 1.0 will be assumed as default confidence for all values provided, regardless
     *            the reference point relative to which the average is to be computed
     */
    <RelativeTo> ConfidenceBasedWindAverager<RelativeTo> createWindAverager(Weigher<RelativeTo> weigher);

}
