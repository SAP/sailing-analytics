package com.sap.sailing.domain.confidence;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.confidence.ConfidenceBasedAverager;
import com.sap.sailing.domain.common.confidence.HasConfidenceAndIsScalable;
import com.sap.sailing.domain.common.confidence.impl.ScalableWind;
import com.sap.sailing.domain.tracking.WindWithConfidence;

public interface ConfidenceBasedWindAverager<RelativeTo> extends ConfidenceBasedAverager<ScalableWind, Wind, RelativeTo>{
    @Override
    WindWithConfidence<RelativeTo> getAverage(
            Iterable<? extends HasConfidenceAndIsScalable<ScalableWind, Wind, RelativeTo>> values, RelativeTo at);
}
