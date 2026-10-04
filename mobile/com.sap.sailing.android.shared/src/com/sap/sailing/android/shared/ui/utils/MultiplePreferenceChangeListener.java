package com.sap.sailing.android.shared.ui.utils;

import java.util.HashSet;
import java.util.Set;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

public class MultiplePreferenceChangeListener implements OnPreferenceChangeListener {

    private final Set<OnPreferenceChangeListener> listeners;

    public MultiplePreferenceChangeListener() {
        this.listeners = new HashSet<Preference.OnPreferenceChangeListener>();
    }

    public void addOnPreferenceChangeListener(OnPreferenceChangeListener listener) {
        listeners.add(listener);
    }

    public void removeOnPreferenceChangeListener(OnPreferenceChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = true;
        for (OnPreferenceChangeListener listener : listeners) {
            if (!listener.onPreferenceChange(preference, newValue)) {
                result = false;
            }
        }
        return result;
    }

}
