package com.sap.sailing.domain.base.impl;

import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sse.common.DoubleTriple;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.confidence.impl.HasConfidenceImpl;
import com.sap.sse.common.scalablevalue.IsScalable;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.common.scalablevalue.impl.ScalableSpeedWithBearing;

public class SpeedWithBearingWithConfidenceImpl<RelativeTo> extends
        HasConfidenceImpl<DoubleTriple, SpeedWithBearing, RelativeTo> implements
        SpeedWithBearingWithConfidence<RelativeTo>, IsScalable<DoubleTriple, SpeedWithBearing> {
    private static final long serialVersionUID = -4811576094614673625L;

    public SpeedWithBearingWithConfidenceImpl(SpeedWithBearing speedWithBearing, double confidence, RelativeTo relativeTo) {
        super(speedWithBearing, confidence, relativeTo);
    }

    /**
     * The scalable value used for averaging confidence-based objects of this type is a triple whose first component
     * holds the speed with a confidence while the second and third element are the sine and cosine values of the bearing's
     * angle.
     */
    @Override
    public ScalableValue<DoubleTriple, SpeedWithBearing> getScalableValue() {
        return new ScalableSpeedWithBearing(getObject());
    }
}
