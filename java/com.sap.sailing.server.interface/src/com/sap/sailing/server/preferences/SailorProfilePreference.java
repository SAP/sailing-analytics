package com.sap.sailing.server.preferences;

import java.util.UUID;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.common.settings.generic.UUIDSetting;

public class SailorProfilePreference extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = 8324742146089597693L;

    private transient UUIDSetting uuid;
    private transient StringSetting name;

    private transient CompetitorSetSetting competitors;

    public SailorProfilePreference(CompetitorAndBoatStore store) {
        super();
        uuid = new UUIDSetting("uuid", this);
        name = new StringSetting("name", this);
        competitors = new CompetitorSetSetting("competitors", this, store);
    }

    /** copy constructor with changed competitors */
    public SailorProfilePreference(CompetitorAndBoatStore store, SailorProfilePreference other,
            Iterable<Competitor> newCompetitors) {
        this(store);
        uuid.setValue(other.getUuid());
        name.setValue(other.getName());
        this.competitors.setValues(newCompetitors);
    }

    /** copy constructor with changed competitors */
    public SailorProfilePreference(CompetitorAndBoatStore store, UUID uuid, String name) {
        this(store);
        this.uuid.setValue(uuid);
        this.name.setValue(name);
    }

    /** copy constructor with new name */
    public SailorProfilePreference(CompetitorAndBoatStore store, SailorProfilePreference other, String newName) {
        this(store);
        uuid.setValue(other.getUuid());
        name.setValue(newName);
        this.competitors.setValues(other.getCompetitors());
    }

    @Override
    protected void addChildSettings() {
        // We do not create the Setting instances here, because access to the RacingEventService would not be given.
        // Doing this, Java/GWT Serialization isn't working anymore. Because the preferences are only serialized as JSON
        // in the backend an transferred as DTO to the frontend, this isn't a problem. Due to usage of BoatClass and
        // Competitor domain objects, it wouldn't be GWT compatible anyway.
        // The usage of Java Serialization isn't planned by now, either.
    }

    public UUID getUuid() {
        return uuid.getValue();
    }

    public String getName() {
        return name.getValue();
    }

    public Iterable<Competitor> getCompetitors() {
        return competitors.getValues();
    }
}
