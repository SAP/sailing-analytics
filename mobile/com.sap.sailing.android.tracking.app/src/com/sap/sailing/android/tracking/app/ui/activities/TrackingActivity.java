package com.sap.sailing.android.tracking.app.ui.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.sap.sailing.android.shared.data.LeaderboardInfo;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.services.sending.MessageSendingService;
import com.sap.sailing.android.shared.services.sending.MessageSendingService.APIConnectivity;
import com.sap.sailing.android.shared.services.sending.MessageSendingService.APIConnectivityListener;
import com.sap.sailing.android.shared.services.sending.MessageSendingService.MessageSendingBinder;
import com.sap.sailing.android.shared.ui.customviews.GPSQuality;
import com.sap.sailing.android.shared.util.LocationHelper;
import com.sap.sailing.android.tracking.app.BuildConfig;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.services.TrackingService;
import com.sap.sailing.android.tracking.app.services.TrackingService.GPSQualityListener;
import com.sap.sailing.android.tracking.app.services.TrackingService.TrackingBinder;
import com.sap.sailing.android.tracking.app.ui.fragments.CompassFragment;
import com.sap.sailing.android.tracking.app.ui.fragments.SpeedFragment;
import com.sap.sailing.android.tracking.app.ui.fragments.TrackingFragment;
import com.sap.sailing.android.tracking.app.ui.fragments.TrackingTimeFragment;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;
import com.sap.sailing.android.tracking.app.utils.DatabaseHelper;
import com.sap.sailing.android.tracking.app.utils.ServiceHelper;
import com.sap.sailing.android.tracking.app.valueobjects.EventInfo;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.viewpagerindicator.CirclePageIndicator;

public class TrackingActivity extends BaseActivity implements GPSQualityListener, APIConnectivityListener {

    private TrackingService trackingService;
    private boolean trackingServiceBound;

    private MessageSendingService messageSendingService;
    boolean messageSendingServiceBound;

    private final static String TAG = TrackingActivity.class.getName();
    private final static String SIS_TRACKING_FRAGMENT = "trackingFragment";
    private final static String SIS_LAST_VIEWPAGER_ITEM = "lastViewPagerItem";
    private final static String SIS_LAST_SPEED_TEXT = "lastSpeedText";
    private final static String SIS_LAST_COMPASS_TEXT = "lastCompassText";

    private final static int REQUEST_PERMISSIONS_REQUEST_CODE = 1119;

    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private AppPreferences prefs;

    private String checkinDigest;

    private TrackingFragment trackingFragment;

    private int lastViewPagerItem;

    /**
     * This isn't nice. The callbacks for fragments inside a view pager are unreliable, but I want the values to be
     * displayed immediately after device rotation. Thus they are cached here and the fragments can pick them up.
     */
    public String lastSpeedIndicatorTextWithoutUnit;
    public String lastCompassIndicatorText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            lastCompassIndicatorText = savedInstanceState.getString(SIS_LAST_COMPASS_TEXT,
                    getString(R.string.initial_hyphen_degrees));
            lastSpeedIndicatorTextWithoutUnit = savedInstanceState.getString(SIS_LAST_SPEED_TEXT,
                    getString(R.string.initial_hyphen));
        } else {
            lastCompassIndicatorText = getString(R.string.initial_hyphen_degrees);
            lastSpeedIndicatorTextWithoutUnit = getString(R.string.initial_hyphen);
        }
        super.onCreate(savedInstanceState);

        prefs = new AppPreferences(this);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            checkinDigest = extras.getString(getString(R.string.tracking_activity_checkin_digest_parameter));
        } else {
            checkinDigest = prefs.getTrackerIsTrackingCheckinDigest();
        }
        setContentView(R.layout.activity_tracking);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        if (getSupportActionBar() != null) {
            EventInfo eventInfo = DatabaseHelper.getInstance().getEventInfo(this, checkinDigest);
            LeaderboardInfo leaderboardInfo = DatabaseHelper.getInstance().getLeaderboard(this, checkinDigest);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(leaderboardInfo.displayName);
            getSupportActionBar().setSubtitle(getString(R.string.tracking_colon) + " " + eventInfo.name);
            ColorDrawable backgroundDrawable = new ColorDrawable(getResources().getColor(R.color.toolbar_background));
            getSupportActionBar().setBackgroundDrawable(backgroundDrawable);
            if (toolbar != null) {
                toolbar.setNavigationIcon(R.drawable.sap_logo_64dp);
                int sidePadding = (int) getResources().getDimension(R.dimen.toolbar_left_padding);
                toolbar.setPadding(sidePadding, 0, 0, 0);
            }
        }

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        if (mPagerAdapter == null) {
            mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        }
        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(0);

        if (savedInstanceState != null) {
            Fragment tFragment = getSupportFragmentManager().getFragment(savedInstanceState, SIS_TRACKING_FRAGMENT);
            if (tFragment != null) {
                trackingFragment = (TrackingFragment) tFragment;
            } else {
                trackingFragment = new TrackingFragment();
            }
            lastViewPagerItem = savedInstanceState.getInt(SIS_LAST_VIEWPAGER_ITEM);
        } else {
            trackingFragment = new TrackingFragment();
        }

        // Bind the title indicator to the adapter
        CirclePageIndicator titleIndicator = (CirclePageIndicator) findViewById(R.id.title_page_indicator);
        titleIndicator.setViewPager(mPager);
        titleIndicator.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int arg0) {
                lastViewPagerItem = arg0;
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });

        Button stopTracking = (Button) findViewById(R.id.stop_tracking);
        stopTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStopTrackingConfirmationDialog();
            }
        });

        replaceFragment(R.id.tracking_linear_layout, trackingFragment);
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSIONS_REQUEST_CODE);
            return;
        }
        ServiceHelper.getInstance().startTrackingService(this, checkinDigest);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SIS_LAST_VIEWPAGER_ITEM, lastViewPagerItem);
        outState.putString(SIS_LAST_SPEED_TEXT, lastSpeedIndicatorTextWithoutUnit);
        outState.putString(SIS_LAST_COMPASS_TEXT, lastCompassIndicatorText);

        if (trackingFragment.isAdded()) {
            getSupportFragmentManager().putFragment(outState, SIS_TRACKING_FRAGMENT, trackingFragment);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent messageSendingServiceIntent = new Intent(this, MessageSendingService.class);
        bindService(messageSendingServiceIntent, messageSendingServiceConnection, Context.BIND_AUTO_CREATE);
        Intent trackingServiceIntent = new Intent(this, TrackingService.class);
        bindService(trackingServiceIntent, trackingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (trackingServiceBound) {
            trackingService.unregisterGPSQualityListener();
            unbindService(trackingServiceConnection);
            trackingServiceBound = false;

            if (BuildConfig.DEBUG) {
                ExLog.i(this, TAG, "Unbound tracking Service");
            }
        }

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
    protected void onPause() {
        super.onPause();

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPagerAdapter == null) {
            mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        }

        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(lastViewPagerItem);
    }

    @Override
    public void gpsQualityAndAccuracyUpdated(final GPSQuality quality, final float accuracy, final Bearing bearing,
            final Speed speed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (trackingFragment.isAdded()) {
                    trackingFragment.setGPSQualityAndAccuracy(quality, accuracy);
                }

                ScreenSlidePagerAdapter viewPagerAdapter = getViewPagerAdapter();

                if (viewPagerAdapter != null) {
                    SpeedFragment speedFragment = viewPagerAdapter.getSpeedFragment();
                    if (speedFragment != null && speedFragment.isAdded()) {
                        speedFragment.setSpeed(speed);
                    }

                    CompassFragment compassFragment = viewPagerAdapter.getCompassFragment();
                    if (compassFragment != null && compassFragment.isAdded()) {
                        compassFragment.setBearing(bearing);
                    }
                }
            }
        });
    }

    @Override
    public void apiConnectivityUpdated(APIConnectivity apiConnectivity) {
        if (trackingFragment.isAdded()) {
            trackingFragment.setAPIConnectivityStatus(apiConnectivity);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode != REQUEST_PERMISSIONS_REQUEST_CODE) {
            return;
        }
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted
            ServiceHelper.getInstance().startTrackingService(this, checkinDigest);
        } else {
            LocationHelper.showNoGPSError(this, getString(R.string.enable_gps));
        }
        return;
    }

    @Override
    public void setUnsentGPSFixesCount(int count) {
        trackingFragment.setUnsentGPSFixesCount(count);
    }

    @Override
    public void onBackPressed() {
        trackingFragment.userTappedBackButton();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection messageSendingServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            MessageSendingBinder binder = (MessageSendingBinder) service;
            messageSendingService = binder.getService();
            messageSendingServiceBound = true;
            messageSendingService.registerAPIConnectivityListener(TrackingActivity.this);
            if (BuildConfig.DEBUG) {
                ExLog.i(TrackingActivity.this, TAG, "connected to message sending service");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            messageSendingServiceBound = false;
        }
    };

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection trackingServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            TrackingBinder binder = (TrackingBinder) service;
            trackingService = binder.getService();
            trackingServiceBound = true;
            trackingService.registerGPSQualityListener(TrackingActivity.this);
            if (BuildConfig.DEBUG) {
                ExLog.i(TrackingActivity.this, TAG, "connected to tracking service");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            trackingServiceBound = false;
        }
    };

    private ScreenSlidePagerAdapter getViewPagerAdapter() {
        return (ScreenSlidePagerAdapter) mPager.getAdapter();
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        public final static int VIEW_PAGER_FRAGMENT_STOP_BUTTON = 0;
        public final static int VIEW_PAGER_FRAGMENT_COMPASS = 1;
        public final static int VIEW_PAGER_FRAGMENT_SPEED = 2;

        private TrackingTimeFragment trackingTimeFragment;
        private CompassFragment cFragment;
        private SpeedFragment sFragment;

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
            case VIEW_PAGER_FRAGMENT_STOP_BUTTON:
                trackingTimeFragment = new TrackingTimeFragment();
                return trackingTimeFragment;
            case VIEW_PAGER_FRAGMENT_COMPASS:
                cFragment = new CompassFragment();
                return cFragment;
            case VIEW_PAGER_FRAGMENT_SPEED:
                sFragment = new SpeedFragment();
                return sFragment;
            default:
                return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        public CompassFragment getCompassFragment() {
            if (cFragment != null && cFragment.isAdded()) {
                return cFragment;
            } else
                return null;
        }

        public SpeedFragment getSpeedFragment() {
            if (sFragment != null && sFragment.isAdded()) {
                return sFragment;
            } else
                return null;
        }
    }

    private boolean hasPermissions() {
        boolean fine = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine && coarse;
    }

    public void showStopTrackingConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.please_confirm).setMessage(R.string.do_you_really_want_to_stop_tracking)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        stopTracking();
                    }
                }).setNegativeButton(android.R.string.no, null).create();

        dialog.show();
    }

    public void stopTracking() {
        prefs.setTrackingTimerStarted(0);
        ServiceHelper.getInstance().stopTrackingService(this);
        finish();
    }
}
