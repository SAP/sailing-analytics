package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.confidence.impl.ScalableWind;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.confidence.impl.HasConfidenceImpl;

public class WindWithConfidenceImpl<RelativeTo> extends HasConfidenceImpl<ScalableWind, Wind, RelativeTo> implements WindWithConfidence<RelativeTo> {
    private static final long serialVersionUID = -644015582690775666L;
    private final boolean useSpeed;
    
    public WindWithConfidenceImpl(Wind object, double confidence, RelativeTo relativeTo, boolean useSpeed) {
        super(object, confidence, relativeTo);
        this.useSpeed = useSpeed;
    }

    @Override
    public ScalableWind getScalableValue() {
        return new ScalableWind(getObject(), useSpeed());
    }

    @Override
    public String toString() {
        return (getObject() != null ? getObject().toString() : "null")+"@"+getConfidence()+", useSpeed="+useSpeed();
    }

    @Override
    public boolean useSpeed() {
        return useSpeed;
    }
}
