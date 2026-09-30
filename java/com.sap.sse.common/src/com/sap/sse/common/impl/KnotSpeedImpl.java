package com.sap.sse.common.impl;

import com.sap.sse.common.AbstractSpeedImpl;
import com.sap.sse.common.Speed;

public class KnotSpeedImpl extends AbstractSpeedImpl implements Speed {
    private static final long serialVersionUID = 5150851454271610069L;
    private final double knots;
    
    public KnotSpeedImpl(double knots) {
        this.knots = knots;
    }
    
    @Override
    public double getKnots() {
        return knots;
    }

    /**
     * Saves extra work by only converting the other object's speed to knots; useful in particular
     * if both speeds are knot-based speeds
     */
    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Speed)) {
            return false;
        }
        return getKnots() == ((Speed) object).getKnots();
    }
}
