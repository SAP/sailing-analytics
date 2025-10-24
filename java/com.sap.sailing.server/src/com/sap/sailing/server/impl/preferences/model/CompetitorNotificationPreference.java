package com.sap.sailing.server.impl.preferences.model;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.common.settings.generic.StringSetting;

public class CompetitorNotificationPreference extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -6510535362114348707L;

    private transient StringSetting competitorId;
    private transient StringSetting competitorName;
    private transient BooleanSetting notifyAboutResults;

    public CompetitorNotificationPreference() {
        super();
        competitorId = new StringSetting("competitor", this);
        competitorName = new StringSetting("competitorName", this);
        notifyAboutResults = new BooleanSetting("notifyAboutResults", this, false);
    }

    public CompetitorNotificationPreference(String competitorIdAsString, String competitorName, boolean notifyAboutResults) {
        this();
        this.competitorId.setValue(competitorIdAsString);
        this.notifyAboutResults.setValue(notifyAboutResults);
        this.competitorName.setValue(competitorName);
    }

    @Override
    protected void addChildSettings() {
        // We do not create the Setting instances here, because access to the RacingEventService would not be given.
        // Doing this, Java/GWT Serialization isn't working anymore. Because the preferences are only serialized as JSON
        // in the backend an transferred as DTO to the frontend, this isn't a problem. Due to usage of BoatClass and
        // Competitor domain objects, it wouldn't be GWT compatible anyway.
        // The usage of Java Serialization isn't planned by now, either.
    }

    public String getCompetitorIdAsString() {
        return competitorId.getValue();
    }

    public boolean isNotifyAboutResults() {
        return Boolean.TRUE.equals(notifyAboutResults.getValue());
    }
    
    public String getCompetitorName() {
        return competitorName.getValue();
    }
    
    public void setCompetitorName(String name) {
        this.competitorName.setValue(name);
    }
}
