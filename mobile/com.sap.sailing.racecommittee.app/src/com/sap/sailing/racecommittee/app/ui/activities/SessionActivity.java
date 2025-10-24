package com.sap.sailing.racecommittee.app.ui.activities;

import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.racecommittee.app.R;

public abstract class SessionActivity extends BaseActivity {
    private static final String TAG = BaseActivity.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ExLog.i(this, TAG, String.format("Logging in from activity %s", this.getClass().getSimpleName()));
    }

    @Override
    protected boolean onHomeClicked() {
        return logoutSession();
    }

    @Override
    protected boolean onReset() {
        return logoutSession(); // only will call super.onReset() after user confirmation
    }

    public boolean logoutSession() {
        ExLog.i(this, TAG, String.format("Logging out from activity %s", this.getClass().getSimpleName()));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.change_dialog_title)).setMessage(getString(R.string.change_dialog_message))
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> doLogout())
                .setNegativeButton(android.R.string.cancel, null).create();
        dialog.show();
        return true;
    }

    public void forceLogout() {
        ExLog.w(this, TAG, String.format("Forcing Logout from activity %s", this.getClass().getSimpleName()));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(this.getString(R.string.data_reload_title));
        builder.setMessage(this.getString(R.string.data_reload_message));
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> doLogout());
        builder.setCancelable(false);
        builder.show();
    }

    private void doLogout() {
        ExLog.i(this, TAG, "Do logout now!");
        super.onReset(); // resets the data manager and fades the activity
        NotificationManagerCompat.from(this).cancelAll();
    }
}
