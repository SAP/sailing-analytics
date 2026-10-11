package com.sap.sailing.android.shared.ui.fragments.preference;

import android.os.Bundle;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;

import com.sap.sailing.android.shared.R;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.adapters.ArrayRemoveAdapter;
import com.sap.sailing.android.shared.ui.views.EditSetPreference;

import java.util.ArrayList;
import java.util.Set;

public class EditSetPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private static final String TAG = EditSetPreferenceDialogFragmentCompat.class.getName();

    private ArrayRemoveAdapter<String> adapter;
    private Set<String> currentValues;
    private Set<String> exampleValues;

    public static EditSetPreferenceDialogFragmentCompat newInstance(Preference preference) {
        EditSetPreferenceDialogFragmentCompat fragment = new EditSetPreferenceDialogFragmentCompat();
        Bundle bundle = new Bundle(1);
        bundle.putString("key", preference.getKey());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final AutoCompleteTextView inputView = view.findViewById(R.id.edit_set_preference_input);
        final Button addButton = view.findViewById(R.id.edit_set_preference_add);
        final ListView listView = view.findViewById(R.id.edit_set_preference_list);

        DialogPreference preference = getPreference();
        if (preference instanceof EditSetPreference) {
            currentValues = ((EditSetPreference) preference).getCurrentValues();
            exampleValues = ((EditSetPreference) preference).getExampleValues();
        }

        // setup list view
        adapter = new ArrayRemoveAdapter<>(view.getContext(), new ArrayList<>(currentValues));
        listView.setEmptyView(view.findViewById(R.id.edit_set_preference_list_empty));
        listView.setAdapter(adapter);

        // setup inputView
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(view.getContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(exampleValues));
        inputView.setAdapter(autoCompleteAdapter);
        inputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                boolean invalid = s.length() == 0 || currentValues.contains(s.toString());
                addButton.setEnabled(!invalid);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });

        // setup add button
        addButton.setEnabled(false);
        addButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Editable text = inputView.getText();
                adapter.add(text.toString());
                text.clear();
            }
        });
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        DialogPreference preference = getPreference();
        Set<String> values = adapter.getItems();
        if (positiveResult && (preference instanceof EditSetPreference)
                && preference.callChangeListener(values)) {
            ExLog.i(getContext(), TAG, "Storing result...");
            preference.persistStringSet(values);
        }
    }
}
