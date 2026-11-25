package com.sap.sailing.android.shared.ui.fragments;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    protected float dpToPx(float dp) {
        float density = getActivity().getResources().getDisplayMetrics().density;
        return dp * density;
    }

    protected float pxToDp(float px) {
        float density = getActivity().getResources().getDisplayMetrics().density;
        return px / density;
    }
}
