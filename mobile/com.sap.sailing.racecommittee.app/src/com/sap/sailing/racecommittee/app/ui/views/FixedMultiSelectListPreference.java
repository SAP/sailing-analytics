/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sap.sailing.racecommittee.app.ui.views;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import com.sap.sailing.android.shared.logging.ExLog;

import android.content.Context;
import android.preference.Preference;
import androidx.preference.MultiSelectListPreference;
import android.util.AttributeSet;

/**
 * Applies the fix of
 * https://github.com/android/platform_frameworks_base/commit/cd9ea08d9cb68004b2d5f69302cddf53dc034e7b for older Android
 * versions.
 */
public class FixedMultiSelectListPreference extends MultiSelectListPreference {

    private static final String TAG = FixedMultiSelectListPreference.class.getName();
    private static final int VERSION_CODE_JELLY_BEAN = 16;

    private boolean fixNeeded;
    private boolean injectionSuccessful;

    private Field mValuesField;
    private Set<String> mValuesReference;
    private Method persistStringSetMethod;

    public FixedMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        obtainReflectionInformation();
    }

    public FixedMultiSelectListPreference(Context context) {
        super(context);
        obtainReflectionInformation();
    }

    private void obtainReflectionInformation() {
        fixNeeded = false;
        if (android.os.Build.VERSION.SDK_INT < VERSION_CODE_JELLY_BEAN) {
            fixNeeded = true;
            injectionSuccessful = false;
            try {
                mValuesField = MultiSelectListPreference.class.getDeclaredField("mValues");
                mValuesField.setAccessible(true);

                @SuppressWarnings("unchecked")
                Set<String> valueReference = (Set<String>) mValuesField.get(this);
                mValuesReference = valueReference;

                persistStringSetMethod = Preference.class.getDeclaredMethod("persistStringSet", java.util.Set.class);
                persistStringSetMethod.setAccessible(true);

                injectionSuccessful = true;
                ExLog.i(getContext(), TAG, "Obtained all reflection information needed for fix.");
            } catch (Exception e) {
                ExLog.e(getContext(), TAG,
                        "Exception while trying to obtain reflection information for MultiSelectList-fix.. Falling back.");
                ExLog.ex(getContext(), TAG, e);
            }
        }
    }

    @Override
    public void setValues(Set<String> values) {
        if (fixNeeded && injectionSuccessful) {
            try {
                fixedSetValues(values);
                return;
            } catch (Exception e) {
                ExLog.e(getContext(), TAG, "Exception while trying to execute MultiSelectList-fix. Falling back.");
                ExLog.ex(getContext(), TAG, e);
            }
        }
        super.setValues(values);
    }

    private void fixedSetValues(Set<String> values)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        mValuesReference.clear();
        mValuesReference.addAll(values);

        persistStringSetMethod.invoke(this, values);
    }

}