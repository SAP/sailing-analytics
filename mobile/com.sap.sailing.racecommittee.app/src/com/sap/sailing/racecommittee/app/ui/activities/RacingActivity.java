package com.sap.sailing.racecommittee.app.ui.activities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.internal.widget.TintImageView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.CollectionUtils;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.logging.LogEvent;
import com.sap.sailing.racecommittee.app.services.RaceStateService;
import com.sap.sailing.racecommittee.app.ui.adapters.racelist.RaceListDataType;
import com.sap.sailing.racecommittee.app.ui.adapters.racelist.RaceListDataTypeHeader;
import com.sap.sailing.racecommittee.app.ui.adapters.racelist.RaceListDataTypeRace;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceInfoFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceListFragment.RaceListCallbacks;
import com.sap.sailing.racecommittee.app.ui.fragments.WelcomeFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RaceFinishingFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RaceFlagViewerFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RaceSummaryFragment;
import com.sap.sailing.racecommittee.app.utils.BitmapHelper;
import com.sap.sailing.racecommittee.app.utils.RaceHelper;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RacingActivity extends SessionActivity implements RaceListCallbacks {
    private static final String TAG = RacingActivity.class.getName();
    private static final String WIND = "wind";
    private static final String RACE = "race";

    private static final int RacesLoaderId = 2;
    private static ProgressBar mProgressSpinner;

    private IntentReceiver mReceiver;
    private ReadonlyDataManager dataManager;
    private RaceInfoFragment infoFragment;
    private Wind mWind;
    private RaceListFragment mRaceList;
    private ManagedRace mSelectedRace;
    private TimePoint startTime;

    private Serializable getCourseAreaIdFromIntent() {
        if (getIntent() == null || getIntent().getExtras() == null) {
            ExLog.e(this, TAG, "Expected an intent carrying event extras.");
            return null;
        }

        final Serializable courseId = getIntent().getExtras().getSerializable(AppConstants.COURSE_AREA_UUID_KEY);
        if (courseId == null) {
            ExLog.e(this, TAG, "Expected an intent carrying the course area id.");
            return null;
        }
        return courseId;
    }

    private Serializable getEventIdFromIntent() {
        if (getIntent() == null || getIntent().getExtras() == null) {
            ExLog.e(this, TAG, "Expected an intent carrying event extras.");
        }

        final Serializable eventId = getIntent().getExtras().getSerializable(AppConstants.EventIdTag);
        if (eventId == null) {
            ExLog.e(this, TAG, "Expected an intent carrying the event id.");
            return null;
        }
        return eventId;
    }

    private void loadRaces(final CourseArea courseArea) {
        setProgressSpinnerVisibility(true);

        ExLog.i(this, TAG, "Issuing loading of managed races from data manager");
        getLoaderManager().initLoader(RacesLoaderId, null, dataManager.createRacesLoader(courseArea.getId(), new RaceLoadClient(courseArea)));
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.racing_view_container);
        if (!(fragment instanceof RaceInfoFragment || fragment instanceof WelcomeFragment)) {
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStackImmediate();
                getFragmentManager().beginTransaction().commit();

                // fix for filled out RaceInfoFragment
                if (infoFragment != null && infoFragment.isFragmentUIActive() && mSelectedRace != null) {
                    ExLog.i(this, this.getClass().getCanonicalName(), "Returning to RaceInfoFragment");

                    getFragmentManager().popBackStackImmediate();
                    onRaceItemClicked(mSelectedRace);
                }
            }
        } else {
            logoutSession();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // features must be requested before anything else
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.racing_view);

        dataManager = DataManager.create(this);
        mRaceList = (RaceListFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            if (AppUtils.with(this).is10inch()) {
                setOverflowIcon();
                toolbar.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.biggerActionBarSize));
            }
            setSupportActionBar(toolbar);
            mProgressSpinner = (ProgressBar) findViewById(R.id.progress_spinner);
        }

        Serializable courseAreaId = getCourseAreaIdFromIntent();
        if (courseAreaId == null) {
            throw new IllegalStateException("There was no course area id transmitted...");
        }
        ExLog.i(this, this.getClass().toString(), "trying to load courseArea via id: " + courseAreaId);
        CourseArea courseArea = dataManager.getDataStore().getCourseArea(courseAreaId);

        Serializable eventId = getEventIdFromIntent();
        if (eventId == null) {
            throw new IllegalStateException("There was no event id transmitted...");
        }
        EventBase event = dataManager.getDataStore().getEvent(eventId);
        if (event == null) {
            ExLog.e(this, TAG, "Noooo the event is null :/");
            PackageManager pm = getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(getPackageName());
            startActivity(intent);
            finish();
        } else {
            if (courseArea != null) {
                loadRaces(courseArea);
                ExLog.i(this, this.getClass().toString(), "did load courseArea!");
            } else {
                ExLog.i(this, this.getClass().toString(), "courseArea == null :(");
                Toast.makeText(this, getString(R.string.racing_course_area_missing), Toast.LENGTH_LONG).show();
            }
            loadNavDrawer(courseArea);
            if (savedInstanceState != null) {
                String raceID = (String) savedInstanceState.getSerializable(RACE);
                if (raceID != null) {
                    onRaceItemClicked(dataManager.getDataStore().getRace(raceID));
                    return;
                }
            }
            loadWelcomeFragment();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mReceiver = new IntentReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AppConstants.INTENT_ACTION_SHOW_MAIN_CONTENT);
        filter.addAction(AppConstants.INTENT_ACTION_SHOW_SUMMARY_CONTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private void loadNavDrawer(CourseArea courseArea) {
        mRaceList.setUp((DrawerLayout) findViewById(R.id.drawer_layout), courseArea.getName(), preferences.getAuthor().getName());
    }

    private void loadWelcomeFragment() {
        getFragmentManager().beginTransaction().replace(R.id.racing_view_container, WelcomeFragment.newInstance()).commit();
    }

    public TimePoint getStartTime() {
        return startTime;
    }

    public void setStartTime(TimePoint startTime) {
        this.startTime = startTime;
    }

    @Override
    public void onRaceListItemSelected(RaceListDataType selectedItem) {
        if (selectedItem instanceof RaceListDataTypeRace) {
            RaceListDataTypeRace selectedElement = (RaceListDataTypeRace) selectedItem;
            selectedElement.setUpdateIndicatorVisible(false);

            ManagedRace managedRace = selectedElement.getRace();
            ExLog.i(this, LogEvent.RACE_SELECTED_ELEMENT, managedRace.getId() + " " + managedRace.getStatus());
            onRaceItemClicked(managedRace);
        } else if (selectedItem instanceof RaceListDataTypeHeader) {
            // This is for logging purposes only!
            RaceListDataTypeHeader selectedTitle = (RaceListDataTypeHeader) selectedItem;
            ExLog.i(this, LogEvent.RACE_SELECTED_TITLE, selectedTitle.toString());
        }
    }

    public void onRaceItemClicked(ManagedRace managedRace) {
        onRaceItemClicked(managedRace, false);
    }

    public void onRaceItemClicked(ManagedRace managedRace, boolean forcedChange) {
        if (forcedChange || mSelectedRace != managedRace) {
            mSelectedRace = managedRace;
            infoFragment = new RaceInfoFragment();
            infoFragment.setArguments(RaceFragment.createArguments(managedRace));

            setupActionBar(managedRace);

            getFragmentManager().beginTransaction().replace(R.id.racing_view_container, infoFragment).commit();
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mWind = (Wind) savedInstanceState.getSerializable(WIND);
        if (mWind != null) {
            onWindEntered(mWind);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WIND, mWind);
        if (mSelectedRace != null) {
            outState.putSerializable(RACE, mSelectedRace.getId());
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.options_menu_reset);
            if (item != null) {
                item.setVisible(mSelectedRace != null);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.options_menu_reset:
                ExLog.i(this, TAG, "Clicked RESET RACE");
                resetRace();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onWindEntered(Wind windFix) {
        TextView windValue = (TextView) findViewById(R.id.wind_value);
        if (windFix != null) {
            if (windValue != null) {
                windValue.setText(String.format(getString(R.string.wind_info), windFix.getKnots(), windFix.getBearing().reverse().toString()));
            }
            if (mSelectedRace != null) {
                mSelectedRace.getState().setWindFix(MillisecondsTimePoint.now(), windFix, /* isMagnetic */ true);
            }

            mWind = windFix;
        } else {
            if (windValue != null) {
                windValue.setText(getString(R.string.wind_unknown));
            }
        }

        getFragmentManager().popBackStackImmediate();

        if (infoFragment != null && infoFragment.isFragmentUIActive()) {
            ExLog.i(this, this.getClass().getCanonicalName(), "Returning to RaceInfoFragment from WindFragment");
            getFragmentManager().popBackStackImmediate();

            onRaceItemClicked(mSelectedRace);
        }
    }

    private void registerOnService(final Collection<ManagedRace> races) {
        // since the service is the long-living component
        // he should decide whether these races are already
        // registered or not.
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (ManagedRace race : races) {
                    Intent registerIntent = new Intent(RacingActivity.this, RaceStateService.class);
                    registerIntent.setAction(AppConstants.INTENT_ACTION_REGISTER_RACE);
                    registerIntent.putExtra(AppConstants.RACE_ID_KEY, race.getId());
                    RacingActivity.this.startService(registerIntent);
                }
            }
        }).run();
    }

    public void setProgressSpinnerVisibility(boolean visible) {
        if (mProgressSpinner != null) {
            if (visible) {
                mProgressSpinner.setVisibility(View.VISIBLE);
            } else {
                mProgressSpinner.setVisibility(View.GONE);
            }
        }

        View progress = findViewById(R.id.progress);
        if (progress != null) {
            if (visible) {
                progress.setVisibility(View.VISIBLE);
            } else {
                progress.setVisibility(View.GONE);
            }
        }
    }

    private void setupActionBar(ManagedRace race) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(RaceHelper.getRaceName(race, " / "));
        }
    }

    public void resetRace() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_AlertDialog);
        builder.setTitle(getString(R.string.race_reset_confirmation_title));
        builder.setMessage(getString(R.string.race_reset_message));
        builder.setCancelable(true);
        builder.setPositiveButton(getString(R.string.race_reset_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ExLog.i(RacingActivity.this, LogEvent.RACE_RESET_YES, mSelectedRace.getId());
                ExLog.w(RacingActivity.this, TAG, String.format("Race %s is selected for reset.", mSelectedRace.getId()));
                mSelectedRace.getState().setAdvancePass(MillisecondsTimePoint.now());
            }
        });
        builder.setNegativeButton(getString(R.string.race_reset_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mSelectedRace != null) {
                    ExLog.i(RacingActivity.this, LogEvent.RACE_RESET_NO, mSelectedRace.getId());
                }
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    public void openDrawer() {
        if (mRaceList != null) {
            mRaceList.openDrawer();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setOverflowIcon() {
        // Required to set overflow icon
        final String overflowDescription = getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Find the overflow button
                final ArrayList<View> outViews = new ArrayList<>();
                decorView.findViewsWithText(outViews, overflowDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                if (outViews.isEmpty()) {
                    return;
                }
                // Actual replacement of the icon
                TintImageView overflow = (TintImageView) outViews.get(0);
                overflow.setMinimumWidth(getResources().getDimensionPixelSize(R.dimen.bigger_over_flow_width));
                overflow.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.bigger_over_flow_height));
                Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromResource(getResources(), R.drawable.ic_more_vert_white_48dp, overflow.getMinimumWidth(), overflow.getMinimumHeight());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    overflow.setScaleType(ImageView.ScaleType.FIT_END);
                }
                overflow.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                // Remove listener on layout
                removeOnGlobalLayoutListener(decorView, this);
            }
        });
    }

    // @SuppressWarnings, but it is handled correctly
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        } else {
            v.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        }
    }

    public void processIntent(Intent intent) {
        String action = intent.getAction();
        Bundle args = new Bundle();
        args.putSerializable(AppConstants.RACE_ID_KEY, mSelectedRace.getId());
        Fragment contentFragment;
        Fragment editFragment = null;

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (AppConstants.INTENT_ACTION_SHOW_MAIN_CONTENT.equals(action)) {
            if (findViewById(R.id.race_edit) != null) {
                editFragment = getFragmentManager().findFragmentById(R.id.race_edit);
                if (editFragment != null) {
                    transaction.remove(editFragment);
                }
            }
            if (editFragment == null && findViewById(R.id.race_content) != null) {
                if (mSelectedRace.getStatus() != RaceLogRaceStatus.FINISHING) {
                    contentFragment = RaceFlagViewerFragment.newInstance();
                } else {
                    contentFragment = RaceFinishingFragment.newInstance();
                }
                contentFragment.setArguments(args);
                transaction.replace(R.id.race_content, contentFragment);
            }
        }

        if (AppConstants.INTENT_ACTION_SHOW_SUMMARY_CONTENT.equals(action)) {
            if (findViewById(R.id.finished_edit) != null) {
                editFragment = getFragmentManager().findFragmentById(R.id.finished_edit);
                if (editFragment != null) {
                    transaction.remove(editFragment);
                }
            }
            if (editFragment == null && findViewById(R.id.finished_content) != null) {
                contentFragment = RaceSummaryFragment.newInstance(args);
                transaction.replace(R.id.finished_content, contentFragment);
            }
        }
        transaction.commit();
    }

    private class RaceLoadClient implements LoadClient<Collection<ManagedRace>> {

        private CourseArea courseArea;
        private Collection<ManagedRace> lastSeenRaces = null;

        public RaceLoadClient(CourseArea courseArea) {
            this.courseArea = courseArea;
        }

        @Override
        public void onLoadFailed(Exception ex) {
            setProgressSpinnerVisibility(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(RacingActivity.this, R.style.AppTheme_AlertDialog);
            builder.setMessage(String.format(getString(R.string.generic_load_failure), ex.getMessage())).setTitle(getString(R.string.loading_failure))
                .setIcon(R.drawable.ic_warning_grey_600_36dp).setCancelable(true)
                .setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setProgressSpinnerVisibility(true);

                        ExLog.i(RacingActivity.this, TAG, "Issuing a reload of managed races");
                        getLoaderManager().restartLoader(RacesLoaderId, null, dataManager.createRacesLoader(courseArea.getId(), RaceLoadClient.this));
                        dialog.cancel();
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            builder.create().show();
        }

        @Override
        public void onLoadSucceeded(Collection<ManagedRace> data, boolean isCached) {
            // Let's do the setup stuff only when the data is changed (or its the first time)
            if (lastSeenRaces != null && CollectionUtils.isEqualCollection(data, lastSeenRaces)) {
                ExLog.i(RacingActivity.this, TAG, "Same races are already loaded...");
            } else {
                lastSeenRaces = data;

                registerOnService(data);
                mRaceList.setupOn(data);

                Toast.makeText(RacingActivity.this, String.format(getString(R.string.racing_load_success), data.size()), Toast.LENGTH_SHORT).show();
            }

            setProgressSpinnerVisibility(false);
        }
    }

    private class IntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            processIntent(intent);
        }
    }
}
