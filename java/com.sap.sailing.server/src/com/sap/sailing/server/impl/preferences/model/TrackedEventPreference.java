package com.sap.sailing.server.impl.preferences.model;

import java.util.Set;
import java.util.UUID;

import com.sap.sse.common.Util;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.common.settings.generic.SettingsList;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.common.settings.generic.UUIDSetting;

public class TrackedEventPreference extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = 234711768869820003L;

    private transient UUIDSetting eventId;
    private transient StringSetting leaderboardName;
    private transient StringSetting baseUrl;
    private transient BooleanSetting isArchived;
    private transient StringSetting regattaSecret;

    private transient SettingsList<TrackedElementWithDeviceId> trackedElements;

    public TrackedEventPreference() {
        super();
    }

    /** copy constructor */
    public TrackedEventPreference(TrackedEventPreference other) {
        this(other.getEventId(), other.getLeaderboardName(),
                other.getTrackedElements(), other.getBaseUrl(),
                other.getIsArchived(), other.getRegattaSecret());
    }

    /** copy constructor with new archived state */
    public TrackedEventPreference(TrackedEventPreference other, boolean isArchived) {
        this(other.getEventId(), other.getLeaderboardName(),
                other.getTrackedElements(), other.getBaseUrl(),
                isArchived, other.getRegattaSecret());
    }

    /** copy constructor with additional tracked event */
    public TrackedEventPreference(TrackedEventPreference other, TrackedElementWithDeviceId trackedElement) {
        this(other.getEventId(), other.getLeaderboardName(),
                addTrackedElement(other.getTrackedElements(), trackedElement), other.getBaseUrl(),
                other.getIsArchived(), other.getRegattaSecret());
    }

    private static Iterable<TrackedElementWithDeviceId> addTrackedElement(
            Iterable<TrackedElementWithDeviceId> trackedElements, TrackedElementWithDeviceId trackedElement) {
        final Set<TrackedElementWithDeviceId> result = Util.asSet(trackedElements);
        result.add(trackedElement);
        return result;
    }

    public TrackedEventPreference(UUID eventId, String leaderboardName,
            Iterable<TrackedElementWithDeviceId> trackedElements, String baseUrl, boolean isArchived,
            String regattaSecret) {
        this();
        this.eventId.setValue(eventId);
        this.leaderboardName.setValue(leaderboardName);
        this.trackedElements.setValues(trackedElements);
        this.baseUrl.setValue(baseUrl);
        this.isArchived.setValue(isArchived);
        this.regattaSecret.setValue(regattaSecret);
    }

    @Override
    protected void addChildSettings() {
        eventId = new UUIDSetting("eventId", this);
        leaderboardName = new StringSetting("leaderboardName", this);
        trackedElements = new SettingsList<TrackedElementWithDeviceId>("trackedElements", this,
                () -> new TrackedElementWithDeviceId());
        baseUrl = new StringSetting("baseUrl", this);
        isArchived = new BooleanSetting("isArchived", this);
        regattaSecret = new StringSetting("regattaSecret", this);
    }

    public UUID getEventId() {
        return eventId.getValue();
    }

    public String getLeaderboardName() {
        return leaderboardName.getValue();
    }

    public Iterable<TrackedElementWithDeviceId> getTrackedElements() {
        return trackedElements.getValues();
    }

    public String getBaseUrl() {
        return baseUrl.getValue();
    }

    public boolean getIsArchived() {
        return isArchived.getValue();
    }

    public String getRegattaSecret() {
        return regattaSecret.getValue();
    }
}
