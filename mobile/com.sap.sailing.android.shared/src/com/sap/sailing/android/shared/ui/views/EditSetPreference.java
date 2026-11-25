package com.sap.sailing.android.shared.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.preference.DialogPreference;
import android.util.AttributeSet;

import com.sap.sailing.android.shared.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EditSetPreference extends DialogPreference {

    private static final Set<String> defaultFallbackValue = new HashSet<>();

    private static final int mDialogLayoutResId = R.layout.edit_set_preference;

    private Set<String> currentValues;

    private Set<String> exampleValues;

    public EditSetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.exampleValues = new HashSet<>();
    }

    public void setExampleValues(String[] values) {
        this.exampleValues.addAll(Arrays.asList(values));
    }

    public Set<String> getExampleValues() {
        return exampleValues;
    }

    public Set<String> getCurrentValues() {
        return currentValues;
    }

    @Override
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            currentValues = getPersistedStringSet(defaultFallbackValue);
        } else {
            // Set default state from the XML attribute
            currentValues = new HashSet<>((Set<String>) defaultValue);
            persistStringSet(currentValues);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        Set<String> defaultValue = new HashSet<>();
        for (CharSequence chars : a.getTextArray(index)) {
            defaultValue.add(chars.toString());
        }
        return defaultValue;
    }

    @Override
    public Set<String> getPersistedStringSet(Set<String> defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }

        Set<String> fallbackDefault = defaultReturnValue == null ? null : new HashSet<>(defaultReturnValue);
        Set<String> value = getSharedPreferences().getStringSet(getKey(), fallbackDefault);

        return value == null ? null : new HashSet<>(value);
    }

    @Override
    public boolean persistStringSet(Set<String> values) {
        currentValues = values;
        return super.persistStringSet(values);
    }
}
