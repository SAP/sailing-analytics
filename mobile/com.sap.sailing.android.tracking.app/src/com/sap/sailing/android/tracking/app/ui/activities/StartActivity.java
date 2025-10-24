package com.sap.sailing.android.tracking.app.ui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.sap.sailing.android.shared.data.BaseCheckinData;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.AbstractStartActivity;
import com.sap.sailing.android.shared.ui.fragments.AbstractHomeFragment;
import com.sap.sailing.android.shared.util.NotificationHelper;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.ui.fragments.HomeFragment;
import com.sap.sailing.android.tracking.app.utils.AboutHelper;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;
import com.sap.sailing.android.tracking.app.utils.CheckinManager;
import com.sap.sailing.android.tracking.app.utils.DatabaseHelper;
import com.sap.sailing.android.tracking.app.valueobjects.BoatCheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.CheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.CompetitorCheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.MarkCheckinData;

import java.util.List;

public class StartActivity extends AbstractStartActivity<CheckinData> {

    private AppPreferences prefs;
    private final String TAG = StartActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        prefs = new AppPreferences(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_activity_start));
            getSupportActionBar().setHomeButtonEnabled(false);
            ColorDrawable backgroundDrawable = new ColorDrawable(getResources().getColor(R.color.toolbar_background));
            getSupportActionBar().setBackgroundDrawable(backgroundDrawable);
            int sidePadding = (int) getResources().getDimension(R.dimen.toolbar_left_padding);
            toolbar.setPadding(sidePadding, 0, 0, 0);
        }
        if (savedInstanceState == null) {
            replaceFragment(R.id.content_frame, new HomeFragment());
        }
        refreshDatabase();

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        int smallIcon = R.drawable.ic_directions_boat;
        CharSequence title = getText(R.string.app_name);
        NotificationHelper.prepareNotificationWith(largeIcon, smallIcon);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (prefs.getTrackerIsTracking()) {
            String checkinDigest = prefs.getTrackerIsTrackingCheckinDigest();
            // TODO: Type in preferences write / read
            startRegatta(checkinDigest);
        }
    }

    @Override
    public AbstractHomeFragment getHomeFragment() {
        return (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }

    private void startRegatta(String checkinDigest) {
        // TODO: Check which activity to be called
        Intent intent = new Intent(this, RegattaActivity.class);
        intent.putExtra(getString(R.string.checkin_digest), checkinDigest);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.options_menu_settings:
            ExLog.i(this, TAG, "Clicked SETTINGS.");
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.options_menu_info:
            AboutHelper.showInfoActivity(this);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getOptionsMenuResId() {
        return R.menu.options_menu;
    }

    @Override
    public void onCheckinDataAvailable(CheckinData data) {
        if (data != null) {
            if (!data.isUpdate()) {
                getHomeFragment().displayUserConfirmationScreen(data);
            } else if (data.isUpdate()) {
                updateRegatta(data);
            }
        }
    }

    private void updateRegatta(BaseCheckinData data) {
        if (data instanceof CheckinData) {
            CheckinData checkinData = (CheckinData) data;
            if (checkinData instanceof CompetitorCheckinData) {
                CompetitorCheckinData competitorCheckinData = (CompetitorCheckinData) checkinData;
                try {
                    DatabaseHelper.getInstance().deleteRegattaFromDatabase(this,
                            checkinData.getCheckinUrl().checkinDigest);
                    DatabaseHelper.getInstance().storeCompetitorCheckinRow(this, competitorCheckinData);
                } catch (DatabaseHelper.GeneralDatabaseHelperException e) {
                    ExLog.e(this, TAG, "Batch insert failed: " + e.getMessage());
                    displayDatabaseError();
                }
            } else if (checkinData instanceof MarkCheckinData) {
                MarkCheckinData markCheckinData = (MarkCheckinData) checkinData;
                try {
                    DatabaseHelper.getInstance().deleteRegattaFromDatabase(this,
                            checkinData.getCheckinUrl().checkinDigest);
                    DatabaseHelper.getInstance().storeMarkCheckinRow(this, markCheckinData);
                } catch (DatabaseHelper.GeneralDatabaseHelperException e) {
                    ExLog.e(this, TAG, "Batch insert failed: " + e.getMessage());
                    displayDatabaseError();
                }
            } else if (checkinData instanceof BoatCheckinData) {
                BoatCheckinData boatCheckinData = (BoatCheckinData) checkinData;
                try {
                    DatabaseHelper.getInstance().deleteRegattaFromDatabase(this,
                            checkinData.getCheckinUrl().checkinDigest);
                    DatabaseHelper.getInstance().storeBoatCheckinRow(this, boatCheckinData);
                } catch (DatabaseHelper.GeneralDatabaseHelperException e) {
                    ExLog.e(this, TAG, "Batch insert failed: " + e.getMessage());
                    displayDatabaseError();
                }
            }
        }
    }

    private void refreshDatabase() {
        List<String> checkinUrls = DatabaseHelper.getInstance().getCheckinUrls(this);
        for (String checkinUrl : checkinUrls) {
            CheckinManager manager = new CheckinManager(checkinUrl, this, true);
            manager.callServerAndGenerateCheckinData();
        }
    }
}
