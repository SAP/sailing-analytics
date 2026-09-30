package com.sap.sailing.domain.base;

import com.sap.sse.common.Position;
import com.sap.sse.common.scalablevalue.HasConfidenceAndIsScalable;
import com.sap.sse.common.scalablevalue.impl.ScalablePosition;

/**
 * A position with a confidence attached. The scalable intermediate type represents the x/y/z coordinate of the
 * position on the planet's sphere which can then be scaled and mapped back to the sphere when dividing.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface PositionWithConfidence<RelativeTo> extends HasConfidenceAndIsScalable<ScalablePosition, Position, RelativeTo> {
}
