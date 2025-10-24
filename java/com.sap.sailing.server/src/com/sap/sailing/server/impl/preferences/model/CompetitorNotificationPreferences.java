package com.sap.sailing.server.impl.preferences.model;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.SettingsList;

public class CompetitorNotificationPreferences extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -3682996540081614053L;
    
    public static final String PREF_NAME = SailingPreferences.NOTIFICATION_PREFERENCES_PREFIX + "competitors";

    private transient SettingsList<CompetitorNotificationPreference> competitors;

    public CompetitorNotificationPreferences() {
        super();
        competitors = new SettingsList<>("competitors", this,
                () -> new CompetitorNotificationPreference());
    }

    @Override
    protected void addChildSettings() {
        // We do not create the Setting instances here, because access to the RacingEventService would not be given.
        // Doing this, Java/GWT Serialization isn't working anymore. Because the preferences are only serialized as JSON
        // in the backend an transferred as DTO to the frontend, this isn't a problem. Due to usage of BoatClass and
        // Competitor domain objects, it wouldn't be GWT compatible anyway.
        // The usage of Java Serialization isn't planned by now, either.
    }

    public Iterable<CompetitorNotificationPreference> getCompetitors() {
        return competitors.getValues();
    }

    public void setCompetitors(Iterable<CompetitorNotificationPreference> competitors) {
        this.competitors.setValues(competitors);
    }
}
