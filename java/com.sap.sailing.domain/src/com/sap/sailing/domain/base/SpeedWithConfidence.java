package com.sap.sailing.domain.base;

import com.sap.sse.common.Speed;
import com.sap.sse.common.scalablevalue.HasConfidenceAndIsScalable;

public interface SpeedWithConfidence<RelativeTo> extends HasConfidenceAndIsScalable<Double, Speed, RelativeTo> {
}
