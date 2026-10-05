package com.sap.sailing.racecommittee.app.ui.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.sap.sailing.android.shared.data.http.UnauthorizedException;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.SendingServiceAwareActivity;
import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.AuthCheckTask;
import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.RaceApplication;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.domain.BackPressListener;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Base activity for all race committee cockpit activities enabling basic menu functionality.
 */
public class BaseActivity extends SendingServiceAwareActivity implements AuthCheckTask.AuthCheckTaskListener {
    private static final String TAG = BaseActivity.class.getName();

    protected AppPreferences preferences;
    private BackPressListener mBackPressListener;

    @Override
    protected int getOptionsMenuResId() {
        return R.menu.options_menu;
    }

    @Override
    protected Drawable getMenuItemLiveDrawable() {
        return ContextCompat.getDrawable(this, R.drawable.ic_share_white_36dp);
    }

    @Override
    protected Drawable getMenuItemLiveErrorDrawable() {
        return BitmapHelper.getTintedDrawable(this, R.drawable.ic_share_white_36dp,
                ThemeHelper.getColor(this, R.attr.sap_red_1));
    }

    @Override
    protected int getStatusTextResId() {
        return R.string.events_waiting_to_be_sent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppUtils.lockOrientation(this);

        preferences = AppPreferences.on(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.options_menu_live) {
            if (AppPreferences.on(this).getAccessToken() == null) {
                onException(null);
            } else {
                try {
                    final AuthCheckTask task = new AuthCheckTask(this, AppPreferences.on(this).getServerBaseURL(), this);
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (MalformedURLException e) {
                    ExLog.e(this, TAG,
                            "Error: Failed to perform check-in due to a MalformedURLException: " + e.getMessage());
                }
            }
            return true;
        } else if (id == R.id.options_menu_settings) {
            ExLog.i(this, TAG, "Clicked SETTINGS");
            startActivity(new Intent(this, PreferenceActivity.class));
            return true;
        } else if (id == R.id.options_menu_info) {
            ExLog.i(this, TAG, "Clicked INFO");
            startActivity(new Intent(this, SystemInformationActivity.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * {@link #resetDataManager() Resets the data manager} (which all redefinitions must do) and then fades this
     * activity.
     */
    protected boolean onReset() {
        resetDataManager();
        RaceApplication.getInstance().restart();
        return true;
    }

    protected void resetDataManager() {
        DataManager dataManager = (DataManager) DataManager.create(this);
        dataManager.resetAll();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (preferences.wakelockEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public AppPreferences getPreferences() {
        if (preferences == null) {
            preferences = AppPreferences.on(this);
        }
        return preferences;
    }

    public void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                ExLog.ex(this, TAG, e);
            }
        }
    }

    public void setBackPressListener(BackPressListener listener) {
        mBackPressListener = listener;
    }

    @Override
    public void onBackPressed() {
        if (mBackPressListener != null) {
            if (!mBackPressListener.handleBackPress()) {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onResultReceived(Boolean authenticated) {
        Toast.makeText(this, getLiveIconText(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onException(Exception exception) {
        if (exception == null || exception instanceof UnauthorizedException) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.sending_exception_title));
            builder.setMessage(getString(R.string.sending_exception_message));
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                AppPreferences.on(BaseActivity.this).setAccessToken(null);
                startActivity(new Intent(BaseActivity.this, PasswordActivity.class));
                finish();
            });
            builder.setNegativeButton(android.R.string.no, null);
            builder.show();
        } else {
            onResultReceived(true);
        }
    }
}
