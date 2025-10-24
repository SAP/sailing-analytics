package com.sap.sailing.server.impl.preferences.model;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.SettingsList;

/** Holds a list of {@link StoredDataMiningReportPreferences}. */
public class StoredDataMiningReportPreferences extends AbstractGenericSerializableSettings {

    public StoredDataMiningReportPreferences() {
        super();
    }

    private static final long serialVersionUID = -9158880266803106327L;
    public static final String PREF_NAME = SailingPreferences.STORED_DATAMINING_REPORT_PREFERENCES;

    private transient SettingsList<StoredDataMiningReportPreference> storedReports;

    @Override
    protected void addChildSettings() {
        storedReports = new SettingsList<>("storedReports", this, StoredDataMiningReportPreference::new);
    }

    public Iterable<StoredDataMiningReportPreference> getStoredReports() {
        return storedReports.getValues();
    }

    public void setStoredReports(Iterable<StoredDataMiningReportPreference> storedReports) {
        this.storedReports.setValues(storedReports);
    }
}
