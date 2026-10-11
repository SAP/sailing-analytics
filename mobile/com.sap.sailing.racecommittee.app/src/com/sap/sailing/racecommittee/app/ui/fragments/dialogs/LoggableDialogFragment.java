package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import com.sap.sailing.android.shared.logging.LifecycleLogger;

import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

/**
 * Simple base class for {@link DialogFragment}'s to add some logging.
 */
public abstract class LoggableDialogFragment extends DialogFragment {

    private LifecycleLogger lifeLogger;

    public LoggableDialogFragment() {
        this.lifeLogger = new LifecycleLogger();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifeLogger.onCreate(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        lifeLogger.onStart(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        lifeLogger.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        lifeLogger.onPause(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        lifeLogger.onStop(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifeLogger.onDestroy(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Fragment fragment = getTargetFragment();
        if (fragment != null && !fragment.isAdded()) {
            setTargetFragment(null, -1);
        }

        super.onSaveInstanceState(outState);
    }
}
