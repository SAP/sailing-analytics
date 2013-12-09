package com.sap.sailing.racecommittee.app.utils.autoupdate;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.logging.ExLog;
import com.sap.sailing.racecommittee.app.ui.activities.SettingsActivity;
import com.sap.sailing.racecommittee.app.ui.fragments.preference.GeneralPreferenceFragment;

public class AutoUpdater {

    private static final String TAG = AutoUpdater.class.getName();

    private final static String HIDDEN_PREFERENCE_UPDATED_FLAG = "hiddenPrefUpdatedFlag";

    private final Context context;
    private final AppPreferences preferences;

    public AutoUpdater(Context context) {
        this.context = context;
        this.preferences = AppPreferences.on(context);
    }

    public void checkForUpdate(boolean forceUpdate) {
        String serverUrl = preferences.getServerBaseURL();
        try {
            new AutoUpdaterChecker(context, this, forceUpdate).check(new URL(serverUrl));
        } catch (MalformedURLException e) {
            // ServerBaseURL in preferences is defect? App will crash...
            ExLog.ex(TAG, e);
        }
    }

    public void updateFromFile(File result) {
        // install it!
        ExLog.i(TAG, String.format("Installing auto-update file %s.", result.getAbsolutePath()));

        setWasUpdated(getUpdatedPreferences(), true);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(result), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void notifyAfterUpdate() {
        boolean wasUpdated = getUpdatedPreferences().getBoolean(HIDDEN_PREFERENCE_UPDATED_FLAG, false);
        if (wasUpdated) {
            clearUpdateCache();
            showUpdatedNotification();
        }
        setWasUpdated(getUpdatedPreferences(), false);
    }

    protected void showUpdatedNotification() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        OnClickListener okListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(context, SettingsActivity.class);
                intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, GeneralPreferenceFragment.class.getName());
                context.startActivity(intent);
                dialog.dismiss();
            }
        };

        OnClickListener cancelListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };

        builder.setTitle("Auto-Update completed")
                .setMessage(
                        "The automatic update has been completed. You should now re-synchronize your connection settings with the server.")
                .setPositiveButton("Take me there", okListener)
                .setNegativeButton(android.R.string.cancel, cancelListener).create().show();
    }

    private void setWasUpdated(SharedPreferences updatedPreferences, boolean wasUpdated) {
        updatedPreferences.edit().putBoolean(HIDDEN_PREFERENCE_UPDATED_FLAG, wasUpdated).commit();
    }

    private SharedPreferences getUpdatedPreferences() {
        return context.getSharedPreferences(HIDDEN_PREFERENCE_UPDATED_FLAG, Context.MODE_PRIVATE);
    }

    private void clearUpdateCache() {
        for (File file : AppConstants.getExternalApplicationFolder().listFiles()) {
            if (file.getName().startsWith("auto-update-") &&
                    file.getName().endsWith(".apk")) {
                boolean result = file.delete();
                if (result) {
                    ExLog.i(TAG, String.format("Deleted old update file %s", file.getName()));
                }
            }
        }
    }

    public File createApkTargetFile() throws IOException {
        return File.createTempFile("auto-update-", ".apk", AppConstants.getExternalApplicationFolder());
    }

}
