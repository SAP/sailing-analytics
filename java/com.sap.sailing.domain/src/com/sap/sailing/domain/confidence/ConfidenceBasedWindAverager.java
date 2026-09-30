package com.sap.sailing.domain.confidence;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.confidence.impl.ScalableWind;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.confidence.ConfidenceBasedAverager;
import com.sap.sse.common.scalablevalue.HasConfidenceAndIsScalable;

public interface ConfidenceBasedWindAverager<RelativeTo> extends ConfidenceBasedAverager<ScalableWind, Wind, RelativeTo>{
    @Override
    WindWithConfidence<RelativeTo> getAverage(
            Iterable<? extends HasConfidenceAndIsScalable<ScalableWind, Wind, RelativeTo>> values, RelativeTo at);
}
