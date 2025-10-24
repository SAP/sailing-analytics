package com.sap.sailing.server.impl.preferences.model;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.SettingsList;

public class TrackedEventPreferences extends AbstractGenericSerializableSettings {

    private static final long serialVersionUID = -290209497308646446L;

    public static final String PREF_NAME = SailingPreferences.TRACKED_EVENTS_PREFERENCES;

    private transient SettingsList<TrackedEventPreference> trackedEvents;

    public TrackedEventPreferences() {
        super();
    }

    @Override
    protected void addChildSettings() {
        trackedEvents = new SettingsList<>("trackedEvents", this, TrackedEventPreference::new);
    }

    public Iterable<TrackedEventPreference> getTrackedEvents() {
        return trackedEvents.getValues();
    }

    public void setTrackedEvents(Iterable<TrackedEventPreference> trackedEvents) {
        this.trackedEvents.setValues(trackedEvents);
    }
}
