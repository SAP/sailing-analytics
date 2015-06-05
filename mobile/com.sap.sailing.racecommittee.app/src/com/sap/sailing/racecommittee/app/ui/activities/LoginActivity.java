package com.sap.sailing.racecommittee.app.ui.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.DeviceConfigurationIdentifier;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationIdentifierImpl;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.BuildConfig;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.InMemoryDataStore;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.domain.configuration.impl.PreferencesDeviceConfigurationLoader;
import com.sap.sailing.racecommittee.app.logging.LogEvent;
import com.sap.sailing.racecommittee.app.ui.fragments.LoginListViews;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.AttachedDialogFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.DialogListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.LoginDialog.LoginType;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.CourseAreaListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.EventListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.PositionListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.CourseAreaSelectedListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.EventSelectedListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.ItemSelectedListener;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.PositionSelectedListenerHost;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sailing.racecommittee.app.utils.autoupdate.AutoUpdater;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.UUID;

public class LoginActivity extends BaseActivity
    implements EventSelectedListenerHost, CourseAreaSelectedListenerHost, PositionSelectedListenerHost, DialogListenerHost.DialogResultListener {

    private final static String CourseAreaListFragmentTag = "CourseAreaListFragmentTag";
    private final static String AreaPositionListFragmentTag = "AreaPositionListFragmentTag";

    private final static String TAG = LoginActivity.class.getName();
    private final int RQS_GooglePlayServices = 1;
    private final PositionListFragment positionFragment;
    private LoginListViews loginListViews = null;
    private Button sign_in;
    private String eventName = null;
    private String courseName = null;
    private String positionName = null;
    private View backdrop;
    private IntentReceiver mReceiver;
    private Serializable mSelectedEvent;
    private UUID mSelectedCourseAreaUUID;
    private ItemSelectedListener<EventBase> eventSelectionListener = new ItemSelectedListener<EventBase>() {

        public void itemSelected(Fragment sender, EventBase event) {
            sign_in = (Button) findViewById(R.id.login_submit);
            if (sign_in != null) {
                sign_in.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        preferences.isSetUp(true);
                        Intent intent = new Intent(LoginActivity.this, RacingActivity.class);
                        intent.putExtra(AppConstants.COURSE_AREA_UUID_KEY, mSelectedCourseAreaUUID);
                        intent.putExtra(AppConstants.EventIdTag, mSelectedEvent);
                        startActivity(intent);
                    }
                });
            }

            final Serializable eventId = event.getId();
            eventName = event.getName();
            ExLog.i(LoginActivity.this, LogEvent.EVENT_SELECTED, eventId.toString());
            preferences.setEventID(eventId);
            showCourseAreaListFragment(eventId);
            if (loginListViews != null) {
                loginListViews.closeAll();
            }

            Intent intent = new Intent(AppConstants.INTENT_ACTION_TOGGLE);
            intent.putExtra(AppConstants.INTENT_ACTION_EXTRA, AppConstants.INTENT_ACTION_TOGGLE_AREA);
            LocalBroadcastManager.getInstance(LoginActivity.this).sendBroadcast(intent);
        }
    };
    private ItemSelectedListener<CourseArea> courseAreaSelectionListener = new ItemSelectedListener<CourseArea>() {

        public void itemSelected(Fragment sender, CourseArea courseArea) {
            courseName = courseArea.getName();
            ExLog.i(LoginActivity.this, TAG, "Starting view for " + courseArea.getName());
            ExLog.i(LoginActivity.this, LogEvent.COURSE_SELECTED, courseArea.getName());
            selectCourseArea(courseArea.getId());
            if (loginListViews != null) {
                loginListViews.closeAll();
            }

            Intent intent = new Intent(AppConstants.INTENT_ACTION_TOGGLE);
            intent.putExtra(AppConstants.INTENT_ACTION_EXTRA, AppConstants.INTENT_ACTION_TOGGLE_POSITION);
            LocalBroadcastManager.getInstance(LoginActivity.this).sendBroadcast(intent);
        }
    };
    private ProgressDialog progressDialog;

    public LoginActivity() {
        positionFragment = PositionListFragment.newInstance();
    }

    private void addAreaPositionListFragment() {
        positionName = null;
        if (sign_in != null) {
            sign_in.setEnabled(false);
        }
        if (getFragmentManager().findFragmentByTag(AreaPositionListFragmentTag) == null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.position_fragment, positionFragment, AreaPositionListFragmentTag);
            transaction.commitAllowingStateLoss();
        }
        ExLog.i(this, "LoginActivity", "PositionFragment created.");
    }

    private void addCourseAreaListFragment(Serializable eventId) {
        courseName = null;
        positionName = null;
        if (sign_in != null) {
            sign_in.setEnabled(false);
        }
        mSelectedEvent = eventId;
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.area_fragment, CourseAreaListFragment.newInstance(eventId), CourseAreaListFragmentTag);
        transaction.commitAllowingStateLoss();
        ExLog.i(this, "LoginActivity", "CourseFragment created.");
    }

    private void addEventListFragment() {
        eventName = null;
        courseName = null;
        positionName = null;
        if (sign_in != null) {
            sign_in.setEnabled(false);
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.event_fragment, EventListFragment.newInstance());
        transaction.commitAllowingStateLoss();
    }

    public ItemSelectedListener<CourseArea> getCourseAreaSelectionListener() {
        return courseAreaSelectionListener;
    }

    public ItemSelectedListener<EventBase> getEventSelectionListener() {
        return eventSelectionListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.setTheme(this);

        setContentView(R.layout.login_view);
        setSupportProgressBarIndeterminateVisibility(false);

        mReceiver = new IntentReceiver();

        loginListViews = new LoginListViews();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.login_listview, loginListViews).commitAllowingStateLoss();

        UUID courseUUID = preferences.getCourseUUID();
        if (courseUUID != new UUID(0, 0)) {
            mSelectedCourseAreaUUID = courseUUID;
        }

        Serializable eventId = preferences.getEventID();
        if (eventId != null) {
            mSelectedEvent = eventId;
        }

        new AutoUpdater(this).notifyAfterUpdate();

        backdrop = findViewById(R.id.login_view_backdrop);
        if (backdrop != null) {
            backdrop.setOnClickListener(new BackdropClick());
        }
    }

    @Override
    public void onPositionSelected(LoginType type) {
        if (mSelectedCourseAreaUUID == null) {
            String toastText = getString(R.string.selected_course_area_lost);
            Toast.makeText(LoginActivity.this, toastText, Toast.LENGTH_LONG).show();
            ExLog.e(LoginActivity.this, TAG, "Course area reference was not set - cannot start racing activity.");
            return;
        }

        preferences.setLoginType(type);

        positionName = positionFragment.getAuthor().getName();
        if (loginListViews != null) {
            loginListViews.closeAll();
        }

        if (sign_in != null) {
            sign_in.setEnabled(true);
        }
    }

    @Override
    protected boolean onReset() {
        Fragment courseAreaFragment = getFragmentManager().findFragmentByTag(CourseAreaListFragmentTag);
        if (courseAreaFragment != null) {
            getFragmentManager().beginTransaction().remove(courseAreaFragment).commitAllowingStateLoss();
        }
        recreate();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AppConstants.INTENT_ACTION_RESET);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        Intent intent = new Intent();
        intent.setAction(AppConstants.INTENT_ACTION_RESET);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

        if (!BuildConfig.DEBUG) {
            if (resultCode != ConnectionResult.SUCCESS) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, RQS_GooglePlayServices).show();
            }
        }

        if (mSelectedEvent != null && preferences.isSetUp()) {
            showCourseAreaListFragment(mSelectedEvent);

            Intent message = new Intent(this, RacingActivity.class);
            message.putExtra(AppConstants.COURSE_AREA_UUID_KEY, mSelectedCourseAreaUUID);
            message.putExtra(AppConstants.EventIdTag, mSelectedEvent);
            fadeActivity(message);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private void selectCourseArea(UUID courseAreaUUID) {
        // mSelectedCourseArea = courseArea;
        mSelectedCourseAreaUUID = courseAreaUUID;
        preferences.setCourseUUID(mSelectedCourseAreaUUID);
        showAreaPositionListFragment();
    }

    private void setupDataManager() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.loading_configuration));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        ReadonlyDataManager dataManager = DataManager.create(this);
        DeviceConfigurationIdentifier identifier = new DeviceConfigurationIdentifierImpl(AppPreferences.on(getApplicationContext())
            .getDeviceIdentifier());

        LoaderCallbacks<?> configurationLoader = dataManager.createConfigurationLoader(identifier, new LoadClient<DeviceConfiguration>() {

                @Override
                public void onLoadFailed(Exception reason) {
                    progressDialog.dismiss();

                    if (reason instanceof FileNotFoundException) {
                        Toast.makeText(getApplicationContext(), getString(R.string.loading_configuration_not_found), Toast.LENGTH_LONG).show();
                        ExLog.w(LoginActivity.this, TAG, String.format("There seems to be no configuration for this device: %s", reason.toString()));
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.loading_configuration_failed), Toast.LENGTH_LONG).show();
                        ExLog.ex(LoginActivity.this, TAG, reason);
                    }
                    showLogin();
                }

                @Override
                public void onLoadSucceeded(DeviceConfiguration configuration, boolean isCached) {
                    progressDialog.dismiss();

                    // this is our 'global' configuration, let's store it in app preferences
                    PreferencesDeviceConfigurationLoader.wrap(configuration, preferences).store();

                    Toast.makeText(LoginActivity.this, getString(R.string.loading_configuration_succeded), Toast.LENGTH_LONG).show();
                    // showCourseAreaListFragment(eventId);
                    showLogin();

                }

                private void showLogin() {
                    Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            backdrop.performClick();
                        }
                    };
                    handler.postDelayed(runnable, 1000);
                }
            });

        if (!AppPreferences.on(this).isOfflineMode()) {
            // always reload the configuration...
            getLoaderManager().restartLoader(0, null, configurationLoader).forceLoad();
        } else {
            progressDialog.dismiss();
        }
    }

    private void showAreaPositionListFragment() {
        addAreaPositionListFragment();
    }

    private void showCourseAreaListFragment(Serializable eventId) {
        addCourseAreaListFragment(eventId);
    }

    public String getEventName() {
        return eventName;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getPositionName() {
        return positionName;
    }

    @Override
    public void onDialogNegativeButton(AttachedDialogFragment dialog) {

    }

    @Override
    public void onDialogPositiveButton(AttachedDialogFragment dialog) {

    }

    private class BackdropClick implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (view.getY() == 0) {
                long aniTime = getResources().getInteger(android.R.integer.config_longAnimTime);
                final View bottomView = findViewById(R.id.login_listview);
                View title = findViewById(R.id.backdrop_title);
                View subTitle = findViewById(R.id.backdrop_subtitle);
                View info = findViewById(R.id.technical_info);
                View settings = findViewById(R.id.settings_button);
                subTitle.setAlpha(0f);

                ObjectAnimator frameAnimation = ObjectAnimator.ofFloat(view, "y", 0, -view.getHeight() + (view.getHeight() / 5));
                ObjectAnimator titleAnimation = ObjectAnimator.ofFloat(title, "alpha", 1f, 0f);
                ObjectAnimator subTitleAnimation = ObjectAnimator.ofFloat(subTitle, "alpha", 0f, 1f);
                ObjectAnimator infoAnimation = ObjectAnimator.ofFloat(info, "alpha", 0f, 1f);
                ObjectAnimator settingsAnimation = ObjectAnimator.ofFloat(settings, "alpha", 0f, 1f);

                ValueAnimator heightAnimation = ValueAnimator.ofInt(0, view.getHeight() - (view.getHeight() / 5));
                heightAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        int val = (Integer) valueAnimator.getAnimatedValue();
                        ViewGroup.LayoutParams layoutParams = bottomView.getLayoutParams();
                        layoutParams.height = val;
                        bottomView.setLayoutParams(layoutParams);
                    }
                });

                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(heightAnimation, frameAnimation, titleAnimation, subTitleAnimation, infoAnimation, settingsAnimation);
                animatorSet.setDuration(aniTime);
                animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
                animatorSet.start();
            }
        }
    }

    private class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            InMemoryDataStore.INSTANCE.reset();
            setupDataManager();

            addEventListFragment();

            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.area_fragment, new Fragment());
            transaction.replace(R.id.position_fragment, new Fragment());
            transaction.commit();
        }
    }
}
