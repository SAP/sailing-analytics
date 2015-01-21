package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.confidence.HasConfidenceAndIsScalable;
import com.sap.sailing.domain.tracking.impl.ScalableWind;
import com.sap.sse.common.TimePoint;

/**
 * In order to scale a wind value, a specific type is used: {@link ScalableWind}.
 * 
 * @author Axel Uhl (d043530)
 * 
 * @param <RelativeTo>
 *            Typical candidates are {@link TimePoint}, {@link Position} or a combination thereof, such as
 *            <code>Pair&lt;TimePoint, Position&gt;</code>
 */
public interface WindWithConfidence<RelativeTo> extends HasConfidenceAndIsScalable<ScalableWind, Wind, RelativeTo> {
    boolean useSpeed();
}
