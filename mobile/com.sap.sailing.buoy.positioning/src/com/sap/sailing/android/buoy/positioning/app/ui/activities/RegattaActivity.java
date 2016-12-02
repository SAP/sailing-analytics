package com.sap.sailing.android.buoy.positioning.app.ui.activities;

import com.sap.sailing.android.buoy.positioning.app.BuildConfig;
import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.ui.fragments.RegattaFragment;
import com.sap.sailing.android.buoy.positioning.app.util.AboutHelper;
import com.sap.sailing.android.buoy.positioning.app.util.AppPreferences;
import com.sap.sailing.android.buoy.positioning.app.util.CheckinManager;
import com.sap.sailing.android.buoy.positioning.app.util.DatabaseHelper;
import com.sap.sailing.android.buoy.positioning.app.util.MarkerUtils;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.CheckinData;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.services.sending.MessageSendingService;
import com.sap.sailing.android.shared.ui.activities.AbstractRegattaActivity;
import com.sap.sailing.android.shared.ui.customviews.OpenSansToolbar;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class RegattaActivity extends AbstractRegattaActivity<CheckinData> {

    private String leaderboardName;
    private String checkinDigest;
    private final String TAG = RegattaActivity.class.getName();
    private String checkinUrl;

    private MessageSendingService messageSendingService;
    private boolean messageSendingServiceBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPreferences prefs = new AppPreferences(this);
        prefs.setLastScannedQRCode(null);
        Intent intent = getIntent();

        checkinDigest = intent.getStringExtra(getString(R.string.checkin_digest));
        leaderboardName = intent.getStringExtra(getString(R.string.leaderboard_name));

        checkinUrl = DatabaseHelper.getInstance().getCheckinUrl(this, checkinDigest).urlString;

        setContentView(R.layout.fragment_container);
        OpenSansToolbar toolbar = (OpenSansToolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.hideSubtitle();
            toolbar.setTitleSize(20);
            setSupportActionBar(toolbar);
            int sidePadding = (int) getResources().getDimension(R.dimen.toolbar_left_padding);
            toolbar.setPadding(sidePadding, 0, 0, 0);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            ColorDrawable backgroundDrawable = new ColorDrawable(getResources().getColor(R.color.toolbar_background));
            getSupportActionBar().setBackgroundDrawable(backgroundDrawable);
            toolbar.setNavigationIcon(R.drawable.sap_logo_64dp);
            getSupportActionBar().setTitle(leaderboardName);
        }
        replaceFragment(R.id.content_frame, RegattaFragment.newInstance());

        MarkerUtils.withContext(this).startMarkerService(checkinUrl);
    }

    @Override
    protected void onResume() {
        super.onResume();
        leaderboardName = (String) getIntent().getExtras().get(getString(R.string.leaderboard_name));
        setTitle(leaderboardName);
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent messageSendingServiceIntent = new Intent(this, MessageSendingService.class);
        bindService(messageSendingServiceIntent, messageSendingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (messageSendingServiceBound) {
            messageSendingService.unregisterAPIConnectivityListener();
            unbindService(messageSendingServiceConnection);

            messageSendingServiceBound = false;

            if (BuildConfig.DEBUG) {
                ExLog.i(this, TAG, "Unbound transmitting Service");
            }
        }
    }

    @Override
    protected void onDestroy() {
        MarkerUtils.withContext(this).stopMarkerService();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.regatta_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                ExLog.i(this, TAG, "Clicked REFRESH.");
                CheckinManager manager = new CheckinManager(checkinUrl, this);
                manager.callServerAndGenerateCheckinData();
                return true;
            case R.id.check_out:
                displayCheckoutConfirmationDialog();
                return true;
            case R.id.about:
                AboutHelper.showInfoActivity(this);
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingActivity.class));
                return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getOptionsMenuResId() {
        // Set to 0 to avoid redundant menu inflation
        return 0;
    }

    @Override
    public void onCheckinDataAvailable(CheckinData checkinData) {
        if (checkinData != null && checkinData instanceof CheckinData) {
            CheckinData data = (CheckinData) checkinData;
            try {
                DatabaseHelper.getInstance().updateMarks(this, data);
            } catch (DatabaseHelper.GeneralDatabaseHelperException e) {
                ExLog.e(this, TAG, "Batch insert failed: " + e.getMessage());
                displayDatabaseError();
                return;
            }
    
            if (BuildConfig.DEBUG) {
                ExLog.i(this, TAG, "Batch-insert of checkinData completed.");
            }
        } else {
            ExLog.i(this, TAG, "checkinData is null");
        }
    }

    private void displayCheckoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_AlertDialog);
        builder.setTitle(R.string.warning);
        builder.setMessage(R.string.checkout_warning_message);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkOut();
            }
        });
        builder.setNegativeButton(android.R.string.no, null);
        builder.show();
    }

    private void checkOut(){
        DatabaseHelper.getInstance().deleteRegattaFromDatabase(this, checkinDigest);
        finish();
    }

    public String getCheckinDigest() {
        return checkinDigest;
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection messageSendingServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            MessageSendingService.MessageSendingBinder binder = (MessageSendingService.MessageSendingBinder) service;
            messageSendingService = binder.getService();
            messageSendingServiceBound = true;
            if (BuildConfig.DEBUG) {
                ExLog.i(RegattaActivity.this, TAG, "connected to message sending service");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            messageSendingServiceBound = false;
        }
    };

}
