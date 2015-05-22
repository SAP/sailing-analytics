package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import java.util.Calendar;

import android.app.FragmentTransaction;
import android.support.annotation.IdRes;

import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;

public class BaseFragment extends RaceFragment {

    protected void openMainScheduleFragment() {
        replaceFragment(MainScheduleFragment.newInstance());
    }

    public void replaceFragment(RaceFragment fragment) {
        replaceFragment(fragment, R.id.racing_view_container);
    }

    public void replaceFragment(RaceFragment fragment, @IdRes int viewId) {
        if (fragment.getArguments() == null) {
            fragment.setArguments(getRecentArguments());
        } else {
            fragment.getArguments().putAll(getRecentArguments());
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(viewId, fragment).commit();
    }

    protected String calcDuration(Calendar from, Calendar to) {
        String retValue;

        long millis = to.getTimeInMillis() - from.getTimeInMillis();

        long min = millis / (1000 * 60);
        long sec = (millis - (min * 60 * 1000)) / 1000;

        retValue = String.valueOf(sec) + "\"";
        if (retValue.length() == 2) {
            retValue = "0" + retValue;
        }
        if (min > 0) {
            retValue = String.valueOf(min) + "' " + retValue;
        }

        return retValue;
    }

    protected Calendar floorTime(Calendar calendar) {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }
}
