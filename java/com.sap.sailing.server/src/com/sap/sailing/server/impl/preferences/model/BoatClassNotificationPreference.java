package com.sap.sailing.server.impl.preferences.model;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;

public class BoatClassNotificationPreference extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -6510535362114348707L;

    private transient BoatClassSetting boatClass;
    private transient BooleanSetting notifyAboutUpcomingRaces;
    private transient BooleanSetting notifyAboutResults;

    public BoatClassNotificationPreference(DomainFactory domainFactory) {
        super();
        boatClass = new BoatClassSetting("boatClass", this, domainFactory);
        notifyAboutUpcomingRaces = new BooleanSetting("notifyAboutUpcomingRaces", this, false);
        notifyAboutResults = new BooleanSetting("notifyAboutResults", this, false);
    }

    public BoatClassNotificationPreference(DomainFactory domainFactory, BoatClass boatClass, boolean notifyAboutRaces,
            boolean notifyAboutResults) {
        this(domainFactory);
        this.boatClass.setValue(boatClass);
        this.notifyAboutUpcomingRaces.setValue(notifyAboutRaces);
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

    public BoatClass getBoatClass() {
        return boatClass.getValue();
    }

    public boolean isNotifyAboutUpcomingRaces() {
        return Boolean.TRUE.equals(notifyAboutUpcomingRaces.getValue());
    }

    public boolean isNotifyAboutResults() {
        return Boolean.TRUE.equals(notifyAboutResults.getValue());
    }
}
