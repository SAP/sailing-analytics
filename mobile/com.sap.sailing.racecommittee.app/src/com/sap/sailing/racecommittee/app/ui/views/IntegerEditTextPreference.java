package com.sap.sailing.racecommittee.app.ui.views;

import android.content.Context;
import androidx.preference.EditTextPreference;
import android.util.AttributeSet;

public class IntegerEditTextPreference extends EditTextPreference {

    public IntegerEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public IntegerEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IntegerEditTextPreference(Context context) {
        super(context);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return String.valueOf(getPersistedInt(-1));
    }

    @Override
    protected boolean persistString(String value) {
        try {
            return persistInt(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
