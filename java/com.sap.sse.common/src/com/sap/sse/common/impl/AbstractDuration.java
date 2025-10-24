package com.sap.sse.common.impl;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;

public abstract class AbstractDuration implements Duration {
    private static final long serialVersionUID = -7217998647218524638L;

    @Override
    public String toString() {
        return Double.isInfinite(asSeconds()) || Double.isNaN(asSeconds()) ? Double.toString(asSeconds()) :
               ((Math.signum(asHours()) < 0 ? "-" : "")+
                Util.padPositiveValue((int) Math.abs(asHours()), 2, 0, /* round */ false)+":"+
                Util.padPositiveValue(Math.abs(asMinutes())%60, 2, 0, /* round */ false)+":"+
                Util.padPositiveValue(Math.abs(asSeconds())%60, 2, 3, /* round */ true));
    }
}
