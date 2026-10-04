package com.sap.sailing.racecommittee.app.ui.fragments;

import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DemoOverlayFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.demo_overlay, container, false);

        AppPreferences preferences = AppPreferences.on(inflater.getContext());
        if (AppConstants.AUTHOR_TYPE_VIEWER.equals(preferences.getAuthor().getName())) {
            layout.findViewById(R.id.watermark).setVisibility(View.VISIBLE);
        }

        return layout;
    }

}
