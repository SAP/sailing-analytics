package com.sap.sailing.racecommittee.app.ui.adapters;

import com.sap.sailing.racecommittee.app.ui.fragments.panels.SetupPanelFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.panels.TimePanelFragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class PanelsAdapter extends FragmentStatePagerAdapter {

    private Bundle mArguments;

    public PanelsAdapter(FragmentManager fragmentManager, Bundle args) {
        super(fragmentManager);
        mArguments = args;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new Fragment();
        switch (position) {
        case 0:
            fragment = TimePanelFragment.newInstance(mArguments);
            break;

        case 1:
            fragment = SetupPanelFragment.newInstance(mArguments, 0);
            break;

        case 2:
            fragment = SetupPanelFragment.newInstance(mArguments, 1);
            break;
        }

        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
