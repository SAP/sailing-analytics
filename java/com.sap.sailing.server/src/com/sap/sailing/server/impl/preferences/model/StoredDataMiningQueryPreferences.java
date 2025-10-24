package com.sap.sailing.server.impl.preferences.model;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.SettingsList;

/** Holds a list of {@link StoredDataMiningQueryPreferences}. */
public class StoredDataMiningQueryPreferences extends AbstractGenericSerializableSettings {

    public StoredDataMiningQueryPreferences() {
        super();
    }

    private static final long serialVersionUID = -8088467604778160161L;
    public static final String PREF_NAME = SailingPreferences.STORED_DATAMINING_QUERY_PREFERENCES;

    private transient SettingsList<StoredDataMiningQueryPreference> storedQueries;

    @Override
    protected void addChildSettings() {
        storedQueries = new SettingsList<>("storedQueries", this, StoredDataMiningQueryPreference::new);
    }

    public Iterable<StoredDataMiningQueryPreference> getStoredQueries() {
        return storedQueries.getValues();
    }

    public void setStoredQueries(Iterable<StoredDataMiningQueryPreference> storedQueries) {
        this.storedQueries.setValues(storedQueries);
    }
}
