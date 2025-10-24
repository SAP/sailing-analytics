package com.sap.sailing.gwt.ui.shared;

import com.google.gwt.core.shared.GwtIncompatible;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;

public class TrackingConnectorInfoDTO implements IsSerializable {
    private String trackingConnectorName;
    private String trackingConnectorDefaultUrl;
    private String webUrl;

    @Deprecated
    TrackingConnectorInfoDTO() {} // GWT serialization

    @GwtIncompatible
    public TrackingConnectorInfoDTO(TrackingConnectorInfo trackingConnectorInfo) {
        this.trackingConnectorName = trackingConnectorInfo.getTrackingConnectorName();
        this.trackingConnectorDefaultUrl = trackingConnectorInfo.getTrackingConnectorDefaultUrl() == null ? null
                : trackingConnectorInfo.getTrackingConnectorDefaultUrl().toString();
        this.webUrl = trackingConnectorInfo.getWebUrl() == null ? null : trackingConnectorInfo.getWebUrl().toString();
    }

    public String getWebUrl() {
        return webUrl;
    }
    
    public String getTrackingConnectorName() {
        return trackingConnectorName;
    }
    
    public String getTrackingConnectorDefaultUrl() {
        return trackingConnectorDefaultUrl;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((trackingConnectorDefaultUrl == null) ? 0 : trackingConnectorDefaultUrl.hashCode());
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
        TrackingConnectorInfoDTO other = (TrackingConnectorInfoDTO) obj;
        if (trackingConnectorDefaultUrl == null) {
            if (other.trackingConnectorDefaultUrl != null)
                return false;
        } else if (!trackingConnectorDefaultUrl.equals(other.trackingConnectorDefaultUrl))
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
