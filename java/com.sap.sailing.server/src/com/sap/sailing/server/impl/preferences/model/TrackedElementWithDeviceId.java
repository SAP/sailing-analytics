package com.sap.sailing.server.impl.preferences.model;

import java.util.UUID;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.common.settings.generic.UUIDSetting;

public class TrackedElementWithDeviceId extends AbstractGenericSerializableSettings {

    private static final long serialVersionUID = 234711768869820003L;

    private transient StringSetting deviceId;
    private transient UUIDSetting trackedCompetitorId;
    private transient UUIDSetting trackedBoatId;
    private transient UUIDSetting trackedMarkId;

    public TrackedElementWithDeviceId() {
        super();
    }

    /** copy constructor */
    public TrackedElementWithDeviceId(TrackedElementWithDeviceId other) {
        this();
        this.deviceId.setValue(other.getDeviceId());
        this.trackedBoatId.setValue(other.getTrackedBoatId());
        this.trackedCompetitorId.setValue(other.getTrackedCompetitorId());
        this.trackedMarkId.setValue(other.getTrackedMarkId());
    }

    public TrackedElementWithDeviceId(String deviceId, UUID trackedBoatId, UUID trackedCompetitorId,
            UUID trackedMarkId) {
        this();
        this.deviceId.setValue(deviceId);
        this.trackedBoatId.setValue(trackedBoatId);
        this.trackedCompetitorId.setValue(trackedCompetitorId);
        this.trackedMarkId.setValue(trackedMarkId);
    }

    @Override
    protected void addChildSettings() {
        deviceId = new StringSetting("deviceId", this);
        trackedCompetitorId = new UUIDSetting("trackedCompetitorId", this);
        trackedBoatId = new UUIDSetting("trackedBoatId", this);
        trackedMarkId = new UUIDSetting("trackedMarkId", this);
    }

    public String getDeviceId() {
        return deviceId.getValue();
    }

    public UUID getTrackedBoatId() {
        return trackedBoatId.getValue();
    }

    public UUID getTrackedCompetitorId() {
        return trackedCompetitorId.getValue();
    }

    public UUID getTrackedMarkId() {
        return trackedMarkId.getValue();
    }
}
