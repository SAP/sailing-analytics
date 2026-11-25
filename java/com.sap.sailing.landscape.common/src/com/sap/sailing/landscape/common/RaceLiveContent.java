package com.sap.sailing.landscape.common;

import java.io.Serializable;

import com.sap.sse.common.TimePoint;

/** The identifying and tracking-time information for a live tracked race. */
public final class RaceLiveContent implements Serializable {
    private static final long serialVersionUID = 8789083472887861191L;
    private final String regattaName;
    private final String raceName;
    private final TimePoint trackingStart;
    private final TimePoint trackingEnd;

    public RaceLiveContent(final String regattaName, final String raceName, final TimePoint trackingStart,
            final TimePoint trackingEnd) {
        this.regattaName = regattaName;
        this.raceName = raceName;
        this.trackingStart = trackingStart;
        this.trackingEnd = trackingEnd;
    }

    public String getRegattaName() {
        return regattaName;
    }

    public String getRaceName() {
        return raceName;
    }

    public TimePoint getTrackingStart() {
        return trackingStart;
    }

    public TimePoint getTrackingEnd() {
        return trackingEnd;
    }
}
