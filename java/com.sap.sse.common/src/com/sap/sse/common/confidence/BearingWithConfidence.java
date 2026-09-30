package com.sap.sse.common.confidence;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.DoublePair;
import com.sap.sse.common.scalablevalue.HasConfidenceAndIsScalable;


public interface BearingWithConfidence<RelativeTo> extends HasConfidenceAndIsScalable<DoublePair, Bearing, RelativeTo> {
}
