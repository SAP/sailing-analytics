package com.sap.sailing.domain.confidence;

import com.sap.sailing.domain.common.confidence.Weigher;
import com.sap.sailing.domain.confidence.impl.ConfidenceFactoryImpl;
import com.sap.sailing.domain.tracking.WindWithConfidence;

public interface ConfidenceFactory extends com.sap.sailing.domain.common.confidence.ConfidenceFactory {
    ConfidenceFactory INSTANCE = new ConfidenceFactoryImpl();
    
    /**
     * Produces a specialized averaged which can deal with the special case that {@link WindWithConfidence} objects have
     * an internal <code>useSpeed</code> flag which may, when set to <code>false</code> suppress the consideration of
     * the wind fix's speed (not the bearing) in computing the average. For this to work, the averager has to maintain a
     * separate confidence sum for the speed values considered.
     */
    <RelativeTo> ConfidenceBasedWindAverager<RelativeTo> createWindAverager(Weigher<RelativeTo> weigher);

}
