package com.sap.sailing.server.impl.preferences.model;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorStore;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;

public class CompetitorNotificationPreference extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -6510535362114348707L;

    private transient CompetitorSetting competitor;
    private transient BooleanSetting notifyAboutResults;

    public CompetitorNotificationPreference(CompetitorStore competitorStore) {
        competitor = new CompetitorSetting("competitor", this, competitorStore);
        notifyAboutResults = new BooleanSetting("notifyAboutResults", this, false);
    }

    public CompetitorNotificationPreference(CompetitorStore competitorStore, Competitor competitor,
            boolean notifyAboutResults) {
        this(competitorStore);
        this.competitor.setValue(competitor);
        this.notifyAboutResults.setValue(notifyAboutResults);
    }

    @Override
    protected void addChildSettings() {
        // We do not create the Setting instances here, because access to the RacingEventService would not be given.
        // Doing this, Java/GWT Serialization isn't working anymore. Because the preferences are only serialized as JSON
        // in the backend an transferred as DTO to the frontend, this isn't a problem. Due to usage of BoatClass and
        // Competitor domain objects, it wouldn't be GWT compatible anyway.
        // The usage of Java Serialization isn't planned by now, either.
    }

    public Competitor getCompetitor() {
        return competitor.getValue();
    }

    public boolean isNotifyAboutResults() {
        return Boolean.TRUE.equals(notifyAboutResults.getValue());
    }
}
