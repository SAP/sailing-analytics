package com.sap.sailing.android.buoy.positioning.app.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.sap.sailing.android.buoy.positioning.app.BuildConfig;
import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.ui.fragments.RegattaFragment;
import com.sap.sailing.android.buoy.positioning.app.util.AboutHelper;
import com.sap.sailing.android.buoy.positioning.app.util.AppPreferences;
import com.sap.sailing.android.buoy.positioning.app.util.CheckinManager;
import com.sap.sailing.android.buoy.positioning.app.util.DatabaseHelper;
import com.sap.sailing.android.buoy.positioning.app.util.MarkerUtils;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.CheckinData;
import com.sap.sailing.android.shared.data.AbstractCheckinData;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.services.sending.MessageSendingService;
import com.sap.sailing.android.shared.ui.activities.AbstractRegattaActivity;
import com.sap.sailing.android.shared.ui.customviews.OpenSansToolbar;

public class RegattaActivity extends AbstractRegattaActivity {

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
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            toolbar.setNavigationIcon(R.drawable.sap_logo_64_sq);
            toolbar.setPadding(20, 0, 0, 0);
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            getSupportActionBar().setTitle(leaderboardName);
        }
        RegattaFragment regattaFragment = new RegattaFragment();
        replaceFragment(R.id.content_frame, regattaFragment);

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
                checkOut();
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
    public void onCheckinDataAvailable(AbstractCheckinData checkinData) {
        CheckinData data = (CheckinData) checkinData;
        try {
            DatabaseHelper.getInstance().deleteRegattaFromDatabase(this, data.checkinDigest);
            DatabaseHelper.getInstance().storeCheckinRow(this, data.marks, data.getLeaderboard(), data.getCheckinUrl(),
                    data.pings);
            getRegattaFragment().getAdapter().notifyDataSetChanged();
        } catch (DatabaseHelper.GeneralDatabaseHelperException e) {
            ExLog.e(this, TAG, "Batch insert failed: " + e.getMessage());
            displayDatabaseError();
            return;
        }

        if (BuildConfig.DEBUG) {
            ExLog.i(this, TAG, "Batch-insert of checkinData completed.");
        }
    }

    private void checkOut(){
        DatabaseHelper.getInstance().deleteRegattaFromDatabase(this, checkinDigest);
        finish();
    }

    public String getCheckinDigest() {
        return checkinDigest;
    }

    public RegattaFragment getRegattaFragment() {
        return (RegattaFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
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
