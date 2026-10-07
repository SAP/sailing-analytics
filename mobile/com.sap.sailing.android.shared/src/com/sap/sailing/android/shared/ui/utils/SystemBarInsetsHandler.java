package com.sap.sailing.android.shared.ui.utils;

import com.sap.sailing.android.shared.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

/**
 * Apps targeting Android 15 (API level 35) or higher are drawn edge-to-edge on Android 15+ devices, i.e., behind the
 * status bar, the navigation (button/gesture) bar and display cutouts; when targeting API level 36, this can no longer
 * be opted out of. The system then also stops resizing windows for the soft keyboard and drawing the status bar color.
 * <p>
 * Registered for all activities, this restores the previous layout: each activity's content view gets padded by the
 * system bar and display cutout insets (and by the keyboard insets unless the activity pans or ignores the keyboard),
 * so that no content is hidden behind them, and the status bar area is filled with the theme's
 * {@code colorPrimaryDark}, which used to be the status bar color.
 * <p>
 * Only register on devices running Android 15 or higher; older versions lay out activities below and above the system
 * bars by themselves.
 */
@TargetApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class SystemBarInsetsHandler implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityPostCreated(final Activity activity, final Bundle savedInstanceState) {
        final Window window = activity.getWindow();
        final View decorView = window.getDecorView();
        final View content = decorView.findViewById(android.R.id.content);
        final ColorDrawable statusBarBackground = new ColorDrawable(resolveStatusBarColor(activity));
        decorView.getOverlay().add(statusBarBackground);
        decorView.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight,
                oldBottom) -> statusBarBackground.setBounds(0, 0, right - left,
                        statusBarBackground.getBounds().height()));
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            final Insets bars = insets
                    .getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            final int softInputAdjust = window.getAttributes().softInputMode
                    & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;
            final boolean resizeForKeyboard = softInputAdjust != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                    && softInputAdjust != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
            final int keyboardHeight = resizeForKeyboard ? insets.getInsets(WindowInsets.Type.ime()).bottom : 0;
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboardHeight));
            statusBarBackground.setBounds(0, 0, decorView.getWidth(), bars.top);
            return WindowInsets.CONSUMED;
        });
        content.requestApplyInsets();
    }

    private int resolveStatusBarColor(final Activity activity) {
        final TypedValue typedValue = new TypedValue();
        final boolean resolved = activity.getTheme().resolveAttribute(R.attr.colorPrimaryDark, typedValue,
                /* resolveRefs */ true);
        return resolved ? typedValue.data : Color.BLACK;
    }

    @Override
    public void onActivityCreated(final Activity activity, final Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(final Activity activity) {
    }

    @Override
    public void onActivityResumed(final Activity activity) {
    }

    @Override
    public void onActivityPaused(final Activity activity) {
    }

    @Override
    public void onActivityStopped(final Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(final Activity activity, final Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(final Activity activity) {
    }
}
