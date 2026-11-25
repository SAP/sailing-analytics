package com.sap.sailing.android.shared.logging;

import android.app.Activity;
import android.content.Context;
import androidx.fragment.app.Fragment;

/**
 * Logger to track all activity and fragment lifecycle events.
 */
public class LifecycleLogger implements LifecycleCallbacks {

    private static final String TAG = LifecycleLogger.class.getName();
    private static boolean isLifecycleLoggingEnabled = false;

    public static void enableLifecycleLogging(boolean enabled) {
        isLifecycleLoggingEnabled = enabled;
    }

    private void onEvent(Object activityOrFragment, String action) {
        Context context = null;
        if (activityOrFragment instanceof Activity) {
            context = (Activity) activityOrFragment;
        } else {
            context = ((Fragment) activityOrFragment).getActivity();
        }
        if (isLifecycleLoggingEnabled) {
            ExLog.i(context, TAG, String.format("%s %s %s", action, activityOrFragment.getClass().getSimpleName(),
                    Integer.toHexString(System.identityHashCode(activityOrFragment))));
        }
    }

    @Override
    public void onStart(Object activityOrFragment) {
        onEvent(activityOrFragment, "Starting");
    }

    @Override
    public void onResume(Object activityOrFragment) {
        onEvent(activityOrFragment, "Resuming");
    }

    @Override
    public void onPause(Object activityOrFragment) {
        onEvent(activityOrFragment, "Pausing");
    }

    @Override
    public void onStop(Object activityOrFragment) {
        onEvent(activityOrFragment, "Stopping");
    }

    @Override
    public void onSaveInstanceState(Object activityOrFragment) {
        onEvent(activityOrFragment, "Saving instance");
    }

    @Override
    public void onDestroy(Object activityOrFragment) {
        onEvent(activityOrFragment, "Destroying");
    }

    @Override
    public void onCreate(Object activityOrFragment) {
        onEvent(activityOrFragment, "Creating");
    }
}
