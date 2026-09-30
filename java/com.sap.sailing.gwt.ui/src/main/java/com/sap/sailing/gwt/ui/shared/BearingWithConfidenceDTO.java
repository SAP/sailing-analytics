package com.sap.sailing.gwt.ui.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.confidence.BearingWithConfidence;
import com.sap.sse.common.confidence.impl.BearingWithConfidenceImpl;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class BearingWithConfidenceDTO implements IsSerializable {
    private double bearingDeg;
    private double confidence;

    @SuppressWarnings("unused") // GWT Constructor
    private BearingWithConfidenceDTO() {}

    public BearingWithConfidenceDTO(BearingWithConfidence<Void> bearingWithConfidence) {
        this(bearingWithConfidence.getObject(), bearingWithConfidence.getConfidence());
    }

    public BearingWithConfidenceDTO(Bearing bearing, double confidence) {
        this.bearingDeg = bearing.getDegrees();
        this.confidence = confidence;
    }

    public Bearing getBearing() {
        return new DegreeBearingImpl(bearingDeg);
    }

    public double getConfidence() {
        return confidence;
    }

    public BearingWithConfidence<Void> getBearingWithConfidence() {
        return new BearingWithConfidenceImpl<Void>(getBearing(), confidence, /* relativeTo */ null);
    }
}
