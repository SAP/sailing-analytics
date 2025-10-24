package com.sap.sailing.domain.shared.tracking.impl;

import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;

public class TrackingConnectorInfoImpl implements TrackingConnectorInfo {
    private static final long serialVersionUID = 7970268841592389145L;
    private final String trackingConnectorName;
    private final String TtackingConnectorDefaultUrl;
    private final String webUrl;

    public TrackingConnectorInfoImpl(String trackingConnectorName, String trackingConnectorDefaultUrl,  String webUrl) {
        super();
        this.trackingConnectorName = trackingConnectorName;
        TtackingConnectorDefaultUrl = trackingConnectorDefaultUrl;
        this.webUrl = webUrl;
    }

    public String getTrackingConnectorDefaultUrl() {
        return TtackingConnectorDefaultUrl;
    }

    public String getTrackingConnectorName() {
        return trackingConnectorName;
    }

    public String getWebUrl() {
        return webUrl;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((TtackingConnectorDefaultUrl == null) ? 0 : TtackingConnectorDefaultUrl.hashCode());
        result = prime * result + ((trackingConnectorName == null) ? 0 : trackingConnectorName.hashCode());
        result = prime * result + ((webUrl == null) ? 0 : webUrl.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TrackingConnectorInfoImpl other = (TrackingConnectorInfoImpl) obj;
        if (TtackingConnectorDefaultUrl == null) {
            if (other.TtackingConnectorDefaultUrl != null)
                return false;
        } else if (!TtackingConnectorDefaultUrl.equals(other.TtackingConnectorDefaultUrl))
            return false;
        if (trackingConnectorName == null) {
            if (other.trackingConnectorName != null)
                return false;
        } else if (!trackingConnectorName.equals(other.trackingConnectorName))
            return false;
        if (webUrl == null) {
            if (other.webUrl != null)
                return false;
        } else if (!webUrl.equals(other.webUrl))
            return false;
        return true;
    }
}
