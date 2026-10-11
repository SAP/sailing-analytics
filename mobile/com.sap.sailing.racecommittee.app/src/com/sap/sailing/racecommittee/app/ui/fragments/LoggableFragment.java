package com.sap.sailing.racecommittee.app.ui.fragments;

import com.sap.sailing.android.shared.logging.LifecycleLogger;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

public abstract class LoggableFragment extends Fragment {

    private LifecycleLogger lifeLogger;

    public LoggableFragment() {
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

}
