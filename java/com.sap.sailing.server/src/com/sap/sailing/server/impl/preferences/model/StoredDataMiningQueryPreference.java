package com.sap.sailing.server.impl.preferences.model;

import java.util.UUID;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.common.settings.generic.UUIDSetting;

/** Preference object which contains a stored data mining query with a name and a unique id. */
public class StoredDataMiningQueryPreference extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -7100595551754668437L;

    private transient StringSetting name;
    private transient UUIDSetting id;
    private transient StringSetting serializedQuery;

    public StoredDataMiningQueryPreference() {
        super();
    }

    public StoredDataMiningQueryPreference(String name, UUID uuid, String serializedQuery) {
        this();
        this.name.setValue(name);
        this.id.setValue(uuid);
        this.serializedQuery.setValue(serializedQuery);
    }

    @Override
    protected void addChildSettings() {
        name = new StringSetting("name", this);
        id = new UUIDSetting("id", this);
        serializedQuery = new StringSetting("serializedQuery", this);
    }

    public String getName() {
        return name.getValue();
    }

    public UUID getId() {
        return id.getValue();
    }

    public String getSerializedQuery() {
        return serializedQuery.getValue();
    }
}
