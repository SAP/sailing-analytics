package com.sap.sailing.landscape.common;

import java.io.Serializable;

/** The identifying and tracking-time information for a live tracked race. */
public final class RaceLiveContent implements Serializable {
    private static final long serialVersionUID = 4664388720482818951L;
    private final String regattaName;
    private final String raceName;
    private final long trackingStartMillis;
    private final Long trackingEndMillis;

    public RaceLiveContent(final String regattaName, final String raceName, final long trackingStartMillis,
            final Long trackingEndMillis) {
        this.regattaName = regattaName;
        this.raceName = raceName;
        this.trackingStartMillis = trackingStartMillis;
        this.trackingEndMillis = trackingEndMillis;
    }

    public String getRegattaName() {
        return regattaName;
    }

    public String getRaceName() {
        return raceName;
    }

    public long getTrackingStartMillis() {
        return trackingStartMillis;
    }

    public Long getTrackingEndMillis() {
        return trackingEndMillis;
    }
}
