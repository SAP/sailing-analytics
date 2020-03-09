package com.sap.sailing.domain.coursetemplate.impl;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.coursetemplate.FixedPositioning;

public class FixedPositioningImpl implements FixedPositioning {
    private static final long serialVersionUID = 267606077936062645L;

    private final Position fixedPosition;
    
    public FixedPositioningImpl(Position fixedPosition) {
        super();
        this.fixedPosition = fixedPosition;
    }

    @Override
    public Position getFixedPosition() {
        return fixedPosition;
    }
}
