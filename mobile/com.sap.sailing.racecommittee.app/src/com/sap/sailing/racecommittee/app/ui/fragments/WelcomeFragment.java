package com.sap.sailing.racecommittee.app.ui.fragments;

import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.activities.RacingActivity;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class WelcomeFragment extends Fragment implements OnClickListener {

    private RacingActivity mActivity;
    private ActionBar mActionBar;

    public WelcomeFragment() {
    }

    public static WelcomeFragment newInstance() {
        WelcomeFragment fragment = new WelcomeFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (RacingActivity) activity;
        if (mActivity != null) {
            mActivity.openDrawer();
            mActionBar = mActivity.getSupportActionBar();
            if (mActionBar != null) {
                mActionBar.setTitle(getString(R.string.race_select));
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mActionBar = null;
        mActivity = null;
    }

    @Override
    public void onClick(View v) {
        RacingActivity activity = (RacingActivity) getActivity();
        if (activity != null) {
            activity.logoutSession();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.welcome, container, false);
        return view;
    }
}
