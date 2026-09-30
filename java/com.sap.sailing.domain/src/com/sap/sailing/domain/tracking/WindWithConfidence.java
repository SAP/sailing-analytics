package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.confidence.impl.ScalableWind;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.scalablevalue.HasConfidenceAndIsScalable;

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
