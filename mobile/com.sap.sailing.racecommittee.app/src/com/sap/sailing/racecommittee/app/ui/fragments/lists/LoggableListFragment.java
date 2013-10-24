package com.sap.sailing.racecommittee.app.ui.fragments.lists;

import android.app.ListFragment;
import android.os.Bundle;

import com.sap.sailing.racecommittee.app.logging.LifecycleLogger;

public abstract class LoggableListFragment extends ListFragment {
    
private LifecycleLogger lifeLogger;
    
    public LoggableListFragment() {
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
