package com.sap.sailing.domain.base;

import com.sap.sse.common.DoubleTriple;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.confidence.HasConfidence;

public interface SpeedWithBearingWithConfidence<RelativeTo> extends
        HasConfidence<DoubleTriple, SpeedWithBearing, RelativeTo> {
    SpeedWithBearing getObject();
}
