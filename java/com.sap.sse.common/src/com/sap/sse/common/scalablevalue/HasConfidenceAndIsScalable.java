package com.sap.sse.common.scalablevalue;

import com.sap.sse.common.confidence.HasConfidence;

public interface HasConfidenceAndIsScalable<ValueType, BaseType, RelativeTo> extends IsScalable<ValueType, BaseType>,
        HasConfidence<ValueType, BaseType, RelativeTo> {
}
