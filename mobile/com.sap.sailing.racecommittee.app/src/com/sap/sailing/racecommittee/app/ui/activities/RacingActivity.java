package com.sap.sailing.racecommittee.app.ui.activities;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.content.Loader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinderResult;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.ConfigurableStartModeFlagRacingProcedure;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.configuration.impl.RegattaConfigurationImpl;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.domain.base.racegroup.RaceGroupSeriesFleet;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.DataStore;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.data.loaders.DataLoaderResult;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.domain.configuration.impl.PreferencesRegattaConfigurationLoader;
import com.sap.sailing.racecommittee.app.logging.LogEvent;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceListFragment.RaceListCallbacks;
import com.sap.sailing.racecommittee.app.ui.fragments.WelcomeFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.chooser.RaceInfoFragmentChooser;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.BaseFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RaceFinishingFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RaceFlagViewerFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RaceSummaryFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.TrackingListFragment;
import com.sap.sailing.racecommittee.app.utils.PreferenceHelper;
import com.sap.sailing.racecommittee.app.utils.RaceHelper;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class RacingActivity extends SessionActivity implements RaceListCallbacks {
    private static final String TAG = RacingActivity.class.getName();
    private static final String RACE = "race";

    private static final int RacesLoaderId = 2;

    private ProgressBar mProgressSpinner;

    private IntentReceiver mReceiver;
    private ReadonlyDataManager dataManager;
    private RaceListFragment mRaceList;
    private ManagedRace mSelectedRace;
    private TimePoint startTime;
    private Loader<DataLoaderResult<Collection<ManagedRace>>> raceLoader;
    private Collection<ManagedRace> lastSeenRaces = null;

    private Serializable getCourseAreaIdFromIntent() {
        if (getIntent() == null || getIntent().getExtras() == null) {
            ExLog.e(this, TAG, "Expected an intent carrying event extras.");
            return null;
        }

        final Serializable courseId = getIntent().getExtras().getSerializable(AppConstants.EXTRA_COURSE_UUID);
        if (courseId == null) {
            ExLog.e(this, TAG, "Expected an intent carrying the course area id.");
            return null;
        }
        return courseId;
    }

    private Serializable getEventIdFromIntent() {
        if (getIntent() == null || getIntent().getExtras() == null) {
            ExLog.e(this, TAG, "Expected an intent carrying event extras.");
            return null;
        }

        final Serializable eventId = getIntent().getExtras().getSerializable(AppConstants.EXTRA_EVENT_ID);
        if (eventId == null) {
            ExLog.e(this, TAG, "Expected an intent carrying the event id.");
            return null;
        }
        return eventId;
    }

    private void loadRaces(final CourseArea courseArea) {
        setProgressSpinnerVisibility(true);

        ExLog.i(this, TAG, "Issuing loading of managed races from data manager");
        if (raceLoader == null) {
            raceLoader = getSupportLoaderManager().initLoader(RacesLoaderId, null,
                    dataManager.createRacesLoader(courseArea.getId(), new RaceLoadClient(courseArea)));
        } else {
            raceLoader.forceLoad();
        }
    }

    private boolean resetEditFragments(@IdRes int id, String action) {
        if (findViewById(id) != null) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(id);
            if (fragment == null) {
                fragment = getSupportFragmentManager().findFragmentById(R.id.protest_time_fragment);
            } else {
                if (fragment instanceof BaseFragment) {
                    BaseFragment base = (BaseFragment) fragment;
                    if (base.onBackPressed()) {
                        return true;
                    }
                }
            }
            if (fragment != null) {
                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
                manager.sendBroadcast(new Intent(action));
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (resetEditFragments(R.id.race_edit, AppConstants.ACTION_SHOW_MAIN_CONTENT)) {
            return;
        }
        if (resetEditFragments(R.id.finished_edit, AppConstants.ACTION_SHOW_SUMMARY_CONTENT)) {
            return;
        }

        if (mRaceList.isDrawerOpen()) {
            mRaceList.closeDrawer();
            return;
        }

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.racing_view_container);
        if (fragment instanceof WelcomeFragment) {
            logoutSession();
        } else {
            loadWelcomeFragment();
            mSelectedRace = null;
            mRaceList.resetSelectedRace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // features must be requested before anything else
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.racing_view);

        dataManager = DataManager.create(this);
        mRaceList = (RaceListFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            if (AppUtils.with(this).is10inch()) {
                setOverflowIcon();
                toolbar.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.biggerActionBarSize));
            }
            setSupportActionBar(toolbar);
            mProgressSpinner = findViewById(R.id.progress_spinner);
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
                loadNavDrawer(courseArea);
                ExLog.i(this, this.getClass().toString(), "did load courseArea!");
            } else {
                ExLog.i(this, this.getClass().toString(), "courseArea == null :(");
                Toast.makeText(this, getString(R.string.racing_course_area_missing), Toast.LENGTH_LONG).show();
            }
            if (savedInstanceState != null) {
                String raceId = savedInstanceState.getString(RACE);
                if (raceId != null) {
                    onRaceItemClicked(dataManager.getDataStore().getRace(raceId));
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
        filter.addAction(AppConstants.ACTION_SHOW_MAIN_CONTENT);
        filter.addAction(AppConstants.ACTION_SHOW_SUMMARY_CONTENT);
        filter.addAction(AppConstants.ACTION_SHOW_WELCOME);
        filter.addAction(AppConstants.ACTION_REMOVE_PROTEST);
        filter.addAction(AppConstants.ACTION_RELOAD_RACES);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private void loadNavDrawer(CourseArea courseArea) {
        mRaceList.setUp(findViewById(R.id.drawer_layout), courseArea.getName(),
                preferences.getAuthor().getName());
    }

    private void loadWelcomeFragment() {
        preferences = AppPreferences.on(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.racing_view_container, WelcomeFragment.newInstance())
                .commitAllowingStateLoss();
    }

    public TimePoint getStartTime() {
        return startTime;
    }

    public void setStartTime(TimePoint startTime) {
        this.startTime = startTime;
    }

    @Nullable
    private ManagedRace findPreviousRace(@NonNull final ManagedRace managedRace) {
        final LinkedHashMap<RaceGroupSeriesFleet, List<ManagedRace>> racesByGroup = mRaceList.getRacesByGroup();
        final RaceGroupSeriesFleet group = new RaceGroupSeriesFleet(managedRace);
        final List<ManagedRace> races = racesByGroup.get(group);
        if (races != null) {
            final int index = races.indexOf(managedRace);
            if (index >= 1) {
                return races.get(index - 1);
            }
        }
        return null;
    }

    @Override
    public void onRaceSelected(ManagedRace race) {
        ManagedRace previousRace = findPreviousRace(race);
        ExLog.i(this, LogEvent.RACE_SELECTED_ELEMENT, race.getId() + " " + race.getStatus());
        if (previousRace != null && race.getStatus() == RaceLogRaceStatus.UNSCHEDULED) {
            final RaceState currentRaceState = race.getState();
            final RacingProcedure racingProcedure = previousRace.getState().getRacingProcedure();
            currentRaceState.setRacingProcedure(MillisecondsTimePoint.now(), racingProcedure.getType());
            final RacingProcedure newRacingProcedure = currentRaceState.getRacingProcedure();
            if (racingProcedure instanceof ConfigurableStartModeFlagRacingProcedure && newRacingProcedure instanceof ConfigurableStartModeFlagRacingProcedure) {
                ConfigurableStartModeFlagRacingProcedure configurableFlag = (ConfigurableStartModeFlagRacingProcedure) racingProcedure;
                ((ConfigurableStartModeFlagRacingProcedure) newRacingProcedure).setStartModeFlag(MillisecondsTimePoint.now(), configurableFlag.getStartModeFlag());
            }
            final CourseBase courseDesign = previousRace.getCourseDesign();
            if (courseDesign != null) {
                currentRaceState.setCourseDesign(MillisecondsTimePoint.now(), courseDesign, CourseDesignerMode.BY_NAME);
            }

            final Wind windFix = previousRace.getState().getWindFix();
            if (windFix != null) {
                currentRaceState.setWindFix(MillisecondsTimePoint.now(), windFix, preferences.isMagnetic());
            }
        }
        onRaceItemClicked(race);
    }

    private void onRaceItemClicked(ManagedRace managedRace) {
        onRaceItemClicked(managedRace, false);
    }

    public void onRaceItemClicked(ManagedRace managedRace, boolean forcedChange) {
        if (forcedChange || mSelectedRace != managedRace) {
            preferences = AppPreferences.on(this,
                    PreferenceHelper.getRegattaPrefFileName(managedRace.getRaceGroup().getName()));
            mSelectedRace = managedRace;
            setupActionBar(managedRace);
            switchToFragment();
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final String raceId = savedInstanceState.getString(RACE);
        if (raceId != null) {
            mSelectedRace = dataManager.getDataStore().getRace(raceId);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedRace != null) {
            outState.putString(RACE, mSelectedRace.getId());
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
        final int id = item.getItemId();
        if (id == R.id.options_menu_reset) {
            ExLog.i(this, TAG, "Clicked RESET RACE");
            resetRace();
            return true;
        } else if (id == R.id.options_menu_role) {
            logoutSession();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void setProgressSpinnerVisibility(boolean visible) {
        if (mProgressSpinner != null) {
            if (visible) {
                mProgressSpinner.setVisibility(View.VISIBLE);
            } else {
                mProgressSpinner.setVisibility(View.GONE);
            }
        }

        if (mRaceList != null) {
            if (visible) {
                mRaceList.showSpinner(true);
            } else {
                mRaceList.showSpinner(false);
            }
        }
    }

    private void setupActionBar(ManagedRace race) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(RaceHelper.getReverseRaceName(race, " / "));
        }
    }

    private void resetRace() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.race_reset_confirmation_title));
        builder.setMessage(getString(R.string.race_reset_message));
        builder.setCancelable(true);
        builder.setPositiveButton(getString(R.string.race_reset_confirm), (dialog, which) -> {
            ExLog.i(RacingActivity.this, LogEvent.RACE_RESET_YES, mSelectedRace.getId());
            ExLog.w(RacingActivity.this, TAG,
                    String.format("Race %s is selected for reset.", mSelectedRace.getId()));
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.race_content);
            if (fragment != null) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.remove(fragment);
                transaction.commit();
            }
            mSelectedRace.getState().setAdvancePass(MillisecondsTimePoint.now());
        });
        builder.setNegativeButton(getString(R.string.race_reset_cancel), (dialog, which) -> {
            if (mSelectedRace != null) {
                ExLog.i(RacingActivity.this, LogEvent.RACE_RESET_NO, mSelectedRace.getId());
            }
            dialog.cancel();
        });
        builder.create().show();
    }

    public void openDrawer() {
        if (mRaceList != null) {
            mRaceList.openDrawer();
        }
    }

    public LinkedHashMap<RaceGroupSeriesFleet, List<ManagedRace>> getRacesByGroup() {
        if (mRaceList != null) {
            return mRaceList.getRacesByGroup();
        }
        return null;
    }

    public List<ManagedRace> getRacesWithStartTimeImmediatelyDependingOn(ManagedRace currentRace,
                                                                         @Nullable RaceLogRaceStatus[] state) {
        StartTimeFinderResult result;
        SimpleRaceLogIdentifier identifier;
        ArrayList<ManagedRace> list = new ArrayList<>();
        DataStore dataStore = dataManager.getDataStore();
        for (ManagedRace race : lastSeenRaces) {
            result = race.getState().getStartTimeFinderResult();
            if (result.isDependentStartTime()) {
                identifier = Util.get(result.getDependingOnRaces(), 0); // immediately depending on
                if (dataStore.getRace(identifier).equals(currentRace)) {
                    if (state == null) {
                        list.add(race);
                    } else {
                        for (RaceLogRaceStatus status : state) {
                            if (status.equals(race.getStatus())) {
                                list.add(race);
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    public LinkedHashMap<String, ManagedRace> getRunningRaces() {
        LinkedHashMap<String, ManagedRace> list = new LinkedHashMap<>();
        for (ManagedRace race : lastSeenRaces) {
            if (RaceLogRaceStatus.RUNNING.equals(race.getStatus())) {
                if (!list.containsKey(race.getId())) {
                    list.put(race.getId(), race);
                }
            }
        }
        return list;
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
                AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
                overflow.setMinimumWidth(getResources().getDimensionPixelSize(R.dimen.bigger_over_flow_width));
                overflow.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.bigger_over_flow_height));
                Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromResource(getResources(),
                        R.drawable.ic_more_vert_white_48dp, ViewCompat.getMinimumWidth(overflow),
                        ViewCompat.getMinimumHeight(overflow));
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

    private void processIntent(final Intent intent) {
        final Bundle args = new Bundle();
        if (mSelectedRace != null) {
            args.putSerializable(AppConstants.EXTRA_RACE_ID, mSelectedRace.getId());
        }

        // artificial delay (to prevent "java.lang.IllegalStateException: Can not perform this action after
        // onSaveInstanceState")
        runOnUiThread(() -> {
            Fragment content;
            Fragment extra = null;
            View view;
            String action = intent.getAction();

            final Fragment parentFragment = getSupportFragmentManager().findFragmentById(R.id.racing_view_container);
            if (parentFragment == null) {
                return;
            }
            final FragmentManager fragmentManager = getSupportFragmentManager();
            final FragmentTransaction transaction = fragmentManager.beginTransaction();

            if (AppConstants.ACTION_SHOW_MAIN_CONTENT.equals(action)) {
                final Fragment fragment = fragmentManager.findFragmentById(R.id.race_edit);
                if (fragment != null) {
                    transaction.remove(fragment);
                }

                view = findViewById(R.id.race_edit);
                if (view != null) {
                    ViewHelper.setSiblingsVisibility(view, View.VISIBLE);
                    extra = getSupportFragmentManager().findFragmentById(R.id.race_edit);
                    if (extra != null) {
                        transaction.remove(extra);
                    }
                }

                if (findViewById(R.id.race_content) != null && mSelectedRace != null) {
                    if (mSelectedRace.getStatus() != RaceLogRaceStatus.FINISHING) {
                        content = RaceFlagViewerFragment.newInstance();
                    } else {
                        boolean forced = intent.getBooleanExtra(AppConstants.ACTION_EXTRA_FORCED, false);
                        if (forced) {
                            content = RaceFinishingFragment.newInstance();
                        } else {
                            RacingProcedureType procedureType = mSelectedRace.getState()
                                    .getRacingProcedure().getType();
                            if (preferences.getRacingProcedureIsResultEntryEnabled(procedureType)) {
                                content = TrackingListFragment.newInstance(args, 1);
                            } else {
                                content = RaceFinishingFragment.newInstance();
                            }
                        }
                    }
                    content.setArguments(args);
                    transaction.replace(R.id.race_content, content);
                }
            }

            if (AppConstants.ACTION_SHOW_SUMMARY_CONTENT.equals(action)) {
                final Fragment fragment = fragmentManager.findFragmentById(R.id.race_edit);
                if (fragment != null) {
                    transaction.remove(fragment);
                }

                view = findViewById(R.id.finished_edit);
                if (view != null) {
                    ViewHelper.setSiblingsVisibility(view, View.VISIBLE);
                    extra = fragmentManager.findFragmentById(R.id.finished_edit);
                    if (extra != null) {
                        transaction.remove(extra);
                    }
                }
                if (extra == null && findViewById(R.id.finished_content) != null) {
                    content = RaceSummaryFragment.newInstance(args);
                    transaction.replace(R.id.finished_content, content);
                }
            }

            if (AppConstants.ACTION_REMOVE_PROTEST.equals(action)) {
                view = findViewById(R.id.protest_time_fragment);
                if (view != null) {
                    ViewHelper.setSiblingsVisibility(view, View.VISIBLE);
                    extra = fragmentManager.findFragmentById(R.id.protest_time_fragment);
                    if (extra != null) {
                        transaction.remove(extra);
                    }
                }
            }

            if (AppConstants.ACTION_SHOW_WELCOME.equals(action)) {
                loadWelcomeFragment();
            }

            if (AppConstants.ACTION_RELOAD_RACES.equals(action)) {
                loadRaces(dataManager.getDataStore().getCourseArea(getCourseAreaIdFromIntent()));
            }

            transaction.commit();
        });
    }

    private void setupRegattaSpecificConfiguration() {
        Set<RaceGroup> raceGroups = new HashSet<>();
        for (ManagedRace race : lastSeenRaces) {
            raceGroups.add(race.getRaceGroup());
        }

        for (RaceGroup raceGroup : raceGroups) {
            String regattaPreference = PreferenceHelper.getRegattaPrefFileName(raceGroup.getName());

            // reset regatta preferences
            PreferenceHelper helper = new PreferenceHelper(this, regattaPreference);
            helper.clearPreferences();

            RegattaConfiguration configuration = raceGroup.getRegattaConfiguration();
            if (configuration instanceof RegattaConfigurationImpl) {
                AppPreferences preferences = AppPreferences.on(this, regattaPreference, false);
                PreferencesRegattaConfigurationLoader preferencesLoader = new PreferencesRegattaConfigurationLoader(
                        configuration, preferences);
                preferencesLoader.store();
            }
        }
    }

    private void switchToFragment() {
        final Fragment fragment = RaceInfoFragmentChooser.choose(this, mSelectedRace);
        ExLog.i(this, TAG, String.format("Switched to %s fragment for race %s with status %s",
                fragment.getClass().getName(), mSelectedRace.getId(), mSelectedRace.getStatus()));
        fragment.setArguments(RaceFragment.createArguments(mSelectedRace));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.racing_view_container, fragment)
                .commit();
    }

    private class RaceLoadClient implements LoadClient<Collection<ManagedRace>> {

        private CourseArea courseArea;

        public RaceLoadClient(CourseArea courseArea) {
            this.courseArea = courseArea;
        }

        @Override
        public void onLoadFailed(Exception ex) {
            setProgressSpinnerVisibility(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(RacingActivity.this);
            builder.setMessage(String.format(getString(R.string.generic_load_failure), ex.getMessage()))
                    .setTitle(getString(R.string.loading_failure)).setCancelable(true)
                    .setPositiveButton(getString(R.string.retry), (dialog, id) -> {
                        setProgressSpinnerVisibility(true);

                        ExLog.i(RacingActivity.this, TAG, "Issuing a reload of managed races");
                        getSupportLoaderManager().restartLoader(RacesLoaderId, null,
                                dataManager.createRacesLoader(courseArea.getId(), RaceLoadClient.this));
                        dialog.cancel();
                    }).setNegativeButton(getString(R.string.cancel), (dialog, id) -> dialog.cancel());
            builder.create().show();
        }

        @Override
        public void onLoadSucceeded(Collection<ManagedRace> data, boolean isCached) {
            // need to be a new instance, because of Activity restart after background kill
            // more information see bug 3741
            lastSeenRaces = new ArrayList<>(data);
            mRaceList.setupOn(data);
            setupRegattaSpecificConfiguration();
            if (!isCached) {
                // close current race, if no longer on server
                if (!data.contains(mSelectedRace)) {
                    BroadcastManager.getInstance(RacingActivity.this)
                            .addIntent(new Intent(AppConstants.ACTION_SHOW_WELCOME));
                    mSelectedRace = null;
                }
                Toast.makeText(RacingActivity.this, String.format(getString(R.string.racing_load_success), data.size()),
                        Toast.LENGTH_SHORT).show();
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
