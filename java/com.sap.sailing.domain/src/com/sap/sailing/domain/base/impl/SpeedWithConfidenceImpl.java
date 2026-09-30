package com.sap.sailing.domain.base.impl;

import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sse.common.Speed;
import com.sap.sse.common.confidence.impl.HasConfidenceImpl;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.common.scalablevalue.impl.ScalableSpeed;

public class SpeedWithConfidenceImpl<RelativeTo> extends HasConfidenceImpl<Double, Speed, RelativeTo> implements SpeedWithConfidence<RelativeTo> {
    private static final long serialVersionUID = -3122580369653833748L;

    public SpeedWithConfidenceImpl(Speed object, double confidence, RelativeTo relativeTo) {
        super(object, confidence, relativeTo);
    }

    @Override
    public ScalableValue<Double, Speed> getScalableValue() {
        return new ScalableSpeed(getObject());
    }
}
