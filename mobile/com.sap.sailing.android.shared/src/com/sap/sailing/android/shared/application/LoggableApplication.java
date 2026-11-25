package com.sap.sailing.android.shared.application;

import java.lang.ref.WeakReference;

import com.sap.sailing.android.shared.BuildConfig;
import com.sap.sailing.android.shared.R;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.logging.LifecycleLogger;
import com.sap.sailing.android.shared.logging.LoggingExceptionHandler;
import com.sap.sailing.android.shared.ui.utils.SystemBarInsetsHandler;
import com.sap.sailing.android.shared.util.PrefUtils;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import io.branch.referral.Branch;

/**
 * <p>
 * Registers an additional exception handler for uncaught exception to have some crash logging.
 * </p>
 * <p>
 * Offers a static {@link StringContext} to handle i18n in ugly cases.
 * </p>
 * <p>
 * Sets the default preference values (if not set)
 * </p>
 */
public class LoggableApplication extends Application {

    private final static String TAG = LoggableApplication.class.getSimpleName();

    private static StringContext stringContext;

    public static StringContext getStringContext() {
        return stringContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ExLog.i(this, TAG, "Application is starting.");

        Thread.setDefaultUncaughtExceptionHandler(
                new LoggingExceptionHandler(Thread.getDefaultUncaughtExceptionHandler(), this));

        LifecycleLogger.enableLifecycleLogging(PrefUtils.getBoolean(this,
                R.string.preference_enableLifecycleLogging_key, R.bool.preference_enableLifecycleLogging_default));
        stringContext = new StringContext(new WeakReference<Context>(getApplicationContext()));

        if (BuildConfig.DEBUG) {
            Branch.enableLogging();
        }
        Branch.getAutoInstance(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            registerActivityLifecycleCallbacks(new SystemBarInsetsHandler());
        }
    }

    public static void restartApp(Context context) {
        Intent i = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(i);
    }

}
