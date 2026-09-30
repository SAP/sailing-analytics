package com.sap.sailing.domain.base.impl;

import com.sap.sailing.domain.base.PositionWithConfidence;
import com.sap.sse.common.Position;
import com.sap.sse.common.confidence.impl.HasConfidenceImpl;
import com.sap.sse.common.scalablevalue.IsScalable;
import com.sap.sse.common.scalablevalue.impl.ScalablePosition;

public class PositionWithConfidenceImpl<RelativeTo> extends HasConfidenceImpl<ScalablePosition, Position, RelativeTo> implements
        PositionWithConfidence<RelativeTo>, IsScalable<ScalablePosition, Position> {
    private static final long serialVersionUID = -3857039006152257909L;

    public PositionWithConfidenceImpl(Position position, double confidence, RelativeTo relativeTo) {
        super(position, confidence, relativeTo);
    }

    @Override
    public ScalablePosition getScalableValue() {
        return new ScalablePosition(getObject());
    }
}
