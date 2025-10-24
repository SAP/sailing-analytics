package com.sap.sailing.server.impl.preferences.model;

import java.util.UUID;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.common.settings.generic.UUIDSetting;

/** Preference object which contains a stored data mining report with a name and a unique id. */
public class StoredDataMiningReportPreference extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -7170211860008891488L;
    
    private transient StringSetting name;
    private transient UUIDSetting id;
    private transient StringSetting serializedReport;

    public StoredDataMiningReportPreference() {
        super();
    }

    public StoredDataMiningReportPreference(String name, UUID uuid, String serializedReport) {
        this();
        this.name.setValue(name);
        this.id.setValue(uuid);
        this.serializedReport.setValue(serializedReport);
    }

    @Override
    protected void addChildSettings() {
        name = new StringSetting("name", this);
        id = new UUIDSetting("id", this);
        serializedReport = new StringSetting("serializedReport", this);
    }

    public String getName() {
        return name.getValue();
    }

    public UUID getId() {
        return id.getValue();
    }

    public String getSerializedReport() {
        return serializedReport.getValue();
    }
}
