package com.sap.sailing.racecommittee.app.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.BarcodeCaptureActivity;
import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.android.shared.util.EulaHelper;
import com.sap.sailing.android.shared.util.NetworkHelper;
import com.sap.sailing.android.shared.util.NotificationHelper;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.common.BranchIOConstants;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.BuildConfig;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.DataStore;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.domain.LoginType;
import com.sap.sailing.racecommittee.app.domain.configuration.impl.PreferencesDeviceConfigurationLoader;
import com.sap.sailing.racecommittee.app.logging.LogEvent;
import com.sap.sailing.racecommittee.app.ui.fragments.LoginListViews;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.AttachedDialogFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.DialogListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.CourseAreaListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.EventListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.PositionListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.CourseAreaSelectedListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.EventSelectedListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.ItemSelectedListener;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.PositionSelectedListenerHost;
import com.sap.sailing.racecommittee.app.utils.QRHelper;
import com.sap.sailing.racecommittee.app.utils.StringHelper;
import com.sap.sailing.racecommittee.app.utils.autoupdate.AutoUpdater;

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.Defines;

import static io.branch.referral.Defines.Jsonkey.Clicked_Branch_Link;

public class LoginActivity extends BaseActivity implements EventSelectedListenerHost, CourseAreaSelectedListenerHost,
        PositionSelectedListenerHost, DialogListenerHost.DialogResultListener {

    private final static String CourseAreaListFragmentTag = "CourseAreaListFragmentTag";
    private final static String AreaPositionListFragmentTag = "AreaPositionListFragmentTag";

    private final static String TAG = LoginActivity.class.getName();

    public final static int REQUEST_CODE_QR_CODE = 45392;

    private final static int NO_PRIORITY = -1;

    private View backdrop;
    private LoginListViews loginListViews = null;

    private Button sign_in;

    // FIXME weird data redundancy by using different field for setting values makes everything so complex and buggy
    private String eventName = null;
    private String courseAreaName = null;
    private String positionName = null;

    private Serializable mSelectedEventId;
    private UUID mSelectedCourseAreaUUID;

    private IntentReceiver mReceiver;
    private ReadonlyDataManager dataManager;
    private View progressSpinner;

    private String branchEventId;
    private String branchCourseAreaUuid;
    private int branchPriority = NO_PRIORITY;

    private final ItemSelectedListener<EventBase> eventSelectionListener = (sender, event) -> {
        final Serializable previousSelectedEventId = mSelectedEventId;
        final Serializable eventId = selectEvent(event);

        // FIXME: its weird to have this button setup in here
        setupSignInButton();

        // prepare views after the event selection

        // close all currently open list views
        if (loginListViews != null) {
            loginListViews.closeAll();
        }
        if (previousSelectedEventId != mSelectedEventId) {
            addCourseAreaListFragment(eventId);
            if (branchCourseAreaUuid == null || !mSelectedEventId.toString().equals(branchEventId)) {
                // send intent to open the course area selection list
                Intent intent = new Intent(AppConstants.ACTION_TOGGLE);
                intent.putExtra(AppConstants.EXTRA_DEFAULT, AppConstants.ACTION_TOGGLE_AREA);
                BroadcastManager.getInstance(LoginActivity.this).addIntent(intent);
            }
        }
    };
    private final ItemSelectedListener<CourseArea> courseAreaSelectionListener = (sender, courseArea) -> {
        ExLog.i(LoginActivity.this, TAG, "Starting view for " + courseArea.getName());
        ExLog.i(LoginActivity.this, LogEvent.COURSE_SELECTED, courseArea.getName());

        selectCourseArea(courseArea);

        // prepare views after area selection

        // close all currently open list views
        if (loginListViews != null) {
            loginListViews.closeAll();
        }
        addAreaPositionListFragment();
        if (branchPriority == NO_PRIORITY) {
            // send intent to open the position selection list
            Intent intent = new Intent(AppConstants.ACTION_TOGGLE);
            intent.putExtra(AppConstants.EXTRA_DEFAULT, AppConstants.ACTION_TOGGLE_POSITION);
            BroadcastManager.getInstance(LoginActivity.this).addIntent(intent);
        }
    };

    private void setupSignInButton() {
        sign_in = findViewById(R.id.login_submit);
        if (sign_in != null) {
            sign_in.setOnClickListener(v -> {
                ExLog.i(LoginActivity.this, TAG,
                        "Logged in: " + eventName + " - " + courseAreaName + " - " + positionName);
                login();
            });
        }
    }

    private void login() {
        DataStore dataStore = dataManager.getDataStore();
        dataStore.setEventUUID(mSelectedEventId);
        dataStore.setCourseAreaId(mSelectedCourseAreaUUID);
        switchToRacingActivity();
    }

    private void switchToRacingActivity() {
        final Intent intent = new Intent(this, RacingActivity.class);
        intent.putExtra(AppConstants.EXTRA_COURSE_UUID, mSelectedCourseAreaUUID);
        intent.putExtra(AppConstants.EXTRA_EVENT_ID, mSelectedEventId);
        startActivity(intent);
        finish();
    }

    private Serializable selectEvent(EventBase event) {
        final Serializable eventId = event.getId();
        ExLog.i(LoginActivity.this, LogEvent.EVENT_SELECTED, eventId.toString());
        mSelectedEventId = eventId;
        eventName = event.getName();
        // TODO: explicitly set the header text of the fragment to this name
        loginListViews.getEventContainer().setHeaderText(eventName);
        return eventId;
    }

    private void resetEvent() {
        mSelectedEventId = null;
        eventName = null;
        loginListViews.getEventContainer().setHeaderText("");
        resetCourseArea();
    }

    private boolean isEventSelected() {
        return (eventName != null && mSelectedEventId != null);
    }

    private void selectCourseArea(CourseArea courseArea) {
        courseAreaName = courseArea.getName();
        mSelectedCourseAreaUUID = courseArea.getId();
        loginListViews.getCourseAreaContainer().setHeaderText(courseAreaName);
    }

    private boolean isCourseAreaSelected() {
        return (courseAreaName != null && mSelectedCourseAreaUUID != null);
    }

    private void resetCourseArea() {
        courseAreaName = null;
        mSelectedCourseAreaUUID = null;
        loginListViews.getCourseAreaContainer().setHeaderText("");
        resetPosition();
    }

    private void selectPosition(LoginType type) {
        preferences.setLoginType(type);
        final PositionListFragment positionFragment = (PositionListFragment) getSupportFragmentManager()
                .findFragmentByTag(AreaPositionListFragmentTag);
        positionName = positionFragment.getAuthor().getName();
        String header = StringHelper.on(this).getAuthor(positionName);
        loginListViews.getPositionContainer().setHeaderText(header);
    }

    private boolean isPositionSelected() {
        return (positionName != null);
    }

    private void resetPosition() {
        positionName = null;
        loginListViews.getPositionContainer().setHeaderText("");
    }

    private void addEventListFragment() {
        resetEvent();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.event_fragment, EventListFragment.newInstance(branchEventId));
        transaction.commitAllowingStateLoss();
    }

    private void addCourseAreaListFragment(Serializable eventId) {
        resetCourseArea();
        updateSignInButtonState();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.area_fragment, CourseAreaListFragment.newInstance(eventId, branchCourseAreaUuid), CourseAreaListFragmentTag);
        transaction.commitAllowingStateLoss();
    }

    private void addAreaPositionListFragment() {
        resetPosition();
        updateSignInButtonState();
        if (getFragmentManager().findFragmentByTag(AreaPositionListFragmentTag) == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.position_fragment, PositionListFragment.newInstance(branchPriority), AreaPositionListFragmentTag);
            transaction.commitAllowingStateLoss();
        }
    }

    public ItemSelectedListener<EventBase> getEventSelectionListener() {
        return eventSelectionListener;
    }

    public ItemSelectedListener<CourseArea> getCourseAreaSelectionListener() {
        return courseAreaSelectionListener;
    }

    private void updateSignInButtonState() {
        if (sign_in == null) {
            return;
        }
        if (isValidForSignIn()) {
            sign_in.setEnabled(true);
        } else {
            sign_in.setEnabled(false);
        }
    }

    private boolean isValidForSignIn() {
        return isEventSelected() && isCourseAreaSelected() && isPositionSelected();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExLog.i(this, TAG, "Starting Login: " + AppUtils.with(this).getBuildInfo());
        String[] addresses = NetworkHelper.getInstance(this).getLocalIpAddress();
        if (addresses != null) {
            for (String address : addresses) {
                ExLog.i(this, TAG, "IP-Addresses: " + address);
            }
        }

        // This is required to reactivate the loader manager after configuration change (screen rotation)
        getLoaderManager();

        dataManager = DataManager.create(this);
        DataStore dataStore = dataManager.getDataStore();

        // Check if the user has been logged in before and if so bring him directly to the racing activity
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) == 0) {
            mSelectedCourseAreaUUID = dataStore.getCourseAreaId();
            mSelectedEventId = dataStore.getEventUUID();
            if (mSelectedEventId != null && mSelectedCourseAreaUUID != null) {
                if (preferences.getAccessToken() != null) {
                    switchToRacingActivity();
                } else {
                    startActivity(new Intent(this, PasswordActivity.class));
                    finish();
                }
            }
        } else {
            dataStore.reset();
        }

        setContentView(R.layout.login_view);

        mReceiver = new IntentReceiver();

        // setup the login list views fragment
        loginListViews = new LoginListViews();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.login_listview, loginListViews);
        transaction.commitAllowingStateLoss();

        new AutoUpdater(this).notifyAfterUpdate();

        backdrop = findViewById(R.id.login_view_backdrop);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        int smallIcon = R.drawable.ic_boat_white_24dp;
        CharSequence title = getText(R.string.app_name);
        NotificationHelper.prepareNotificationWith(largeIcon, smallIcon);

        onNewIntent(getIntent());
    }

    private final Branch.BranchReferralInitListener branchReferralInitListener = new Branch.BranchReferralInitListener() {
        @Override
        public void onInitFinished(JSONObject referringParams, BranchError error) {
            if (error != null) {
                ExLog.i(LoginActivity.this, "BRANCH SDK", error.getMessage());
                handleLegacyStart();
                return;
            }
            if (referringParams == null || referringParams.length() == 0) {
                handleLegacyStart();
                return;
            }

            //Non-branch link
            if (!referringParams.optBoolean(Clicked_Branch_Link.getKey())) {
                handleLegacyStart();
                return;
            }
            ExLog.i(LoginActivity.this, "BRANCH SDK", referringParams.toString());

            final String serverUrl = referringParams.optString(BranchIOConstants.RACEMANAGER_APP_BRANCH_PARAM_SERVER_URL, null);

            final String identifier = referringParams.optString(BranchIOConstants.RACEMANAGER_APP_BRANCH_PARAM_DEVICE_CONFIG_IDENTIFIER, null);
            final String configUuid = referringParams.optString(BranchIOConstants.RACEMANAGER_APP_BRANCH_PARAM_DEVICE_CONFIG_UUID, null);

            final String token = referringParams.optString(BranchIOConstants.RACEMANAGER_APP_BRANCH_PARAM_TOKEN, null);

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
            final SharedPreferences.Editor editor = preferences.edit();
            if (serverUrl != null) {
                editor.putString(getString(R.string.preference_server_url_key), serverUrl);
            }
            editor.putString(getString(R.string.preference_identifier_key), identifier);
            editor.putString(getString(R.string.preference_config_uuid_key), configUuid);
            if (token != null) {
                editor.putString(getString(R.string.preference_access_token_key), token);
            }
            editor.apply();

            branchEventId = referringParams.optString(BranchIOConstants.RACEMANAGER_APP_BRANCH_PARAM_EVENT_ID, null);
            branchCourseAreaUuid = referringParams.optString(BranchIOConstants.RACEMANAGER_APP_BRANCH_PARAM_COURSE_AREA_UUID, null);
            branchPriority = referringParams.optInt(BranchIOConstants.RACEMANAGER_APP_BRANCH_PARAM_PRIORITY, NO_PRIORITY);

            final String accessToken = preferences.getString(getString(R.string.preference_access_token_key), null);
            if (TextUtils.isEmpty(token) && TextUtils.isEmpty(accessToken)) {
                LocalBroadcastManager.getInstance(LoginActivity.this)
                        .sendBroadcast(new Intent(AppConstants.ACTION_SHOW_LOGIN));
            } else {
                checkLogin();
            }
        }
    };

    private void checkLogin() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(AppConstants.ACTION_CHECK_LOGIN));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AppConstants.ACTION_RESET);
        filter.addAction(AppConstants.ACTION_VALID_DATA);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (!BuildConfig.DEBUG && resultCode != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, 1).show();
        }

        EulaHelper.with(this).showEulaDialogIfNotAccepted(() -> {
            Branch.getInstance().initSession(branchReferralInitListener, getIntent().getData(), LoginActivity.this);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        dismissProgressSpinner();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_QR_CODE) {
            return;
        }

        if (resultCode == CommonStatusCodes.SUCCESS && data != null) {
            Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
            final Uri uri = Uri.parse(barcode.displayValue);
            final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Defines.Jsonkey.ForceNewBranchSession.getKey(), true);
            onNewIntent(intent);
        } else {
            Toast.makeText(this, getString(R.string.error_scanning_qr, resultCode), Toast.LENGTH_LONG).show();
        }
    }

    //TODO Can be removed in a later version
    private void handleLegacyStart() {
        String action = getIntent().getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            final Uri uri = getIntent().getData();
            if (uri != null && TextUtils.equals(uri.getPath(), "/apps/com.sap.sailing.racecommittee.app.apk")) {
                branchEventId = null;
                branchCourseAreaUuid = null;
                branchPriority = NO_PRIORITY;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.app_name);
                if (QRHelper.with(this).saveData(getIntent().getDataString())) {
                    builder.setMessage(getString(R.string.server_deeplink_message, preferences.getServerBaseURL()));
                } else {
                    builder.setMessage(getText(R.string.error_invalid_qr_code));
                }
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                final String accessToken = AppPreferences.on(this).getAccessToken();
                if (TextUtils.isEmpty(accessToken)) {
                    LocalBroadcastManager.getInstance(LoginActivity.this)
                            .sendBroadcast(new Intent(AppConstants.ACTION_SHOW_LOGIN));
                    return;
                }
            }
        }
        checkLogin();
    }

    @Override
    public void onPositionSelected(LoginType type) {

        // FIXME: this is some kind of exception handling
        /*
         * if (mSelectedCourseAreaUUID == null) { String toastText = getString(R.string.selected_course_area_lost);
         * Toast.makeText(LoginActivity.this, toastText, Toast.LENGTH_LONG).show(); ExLog.e(LoginActivity.this, TAG,
         * "Course area reference was not set - cannot start racing activity."); return; }
         */

        selectPosition(type);
        // prepare views after position selected

        if (loginListViews != null) {
            loginListViews.closeAll();
        }
        updateSignInButtonState();
    }

    private void setupDataManager() {
        showProgressSpinner();
        dataManager.getDataStore().reset();
        String deviceConfigurationName = AppPreferences.on(getApplicationContext()).getDeviceConfigurationName();
        UUID deviceConfigurationUuid = AppPreferences.on(getApplicationContext()).getDeviceConfigurationUuid();
        LoaderCallbacks<?> configurationLoader = dataManager.createConfigurationLoader(deviceConfigurationName,
                deviceConfigurationUuid, new LoadClient<DeviceConfiguration>() {
                    @Override
                    public void onLoadFailed(Exception reason) {
                        dismissProgressSpinner();

                        if (reason instanceof FileNotFoundException) {
                            Toast.makeText(getApplicationContext(), getString(R.string.loading_configuration_not_found),
                                    Toast.LENGTH_LONG).show();
                            ExLog.w(LoginActivity.this, TAG, String.format(
                                    "There seems to be no configuration for this device: %s", reason.toString()));
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.loading_configuration_failed),
                                    Toast.LENGTH_LONG).show();
                            ExLog.ex(LoginActivity.this, TAG, reason);
                        }

                        slideUpBackdrop(() -> {});
                    }

                    @Override
                    public void onLoadSucceeded(DeviceConfiguration configuration, boolean isCached) {
                        getSupportLoaderManager().destroyLoader(0);

                        dismissProgressSpinner();

                        // this is our 'global' configuration, let's store it in app preferences
                        PreferencesDeviceConfigurationLoader.wrap(configuration, preferences).store();

                        Toast.makeText(LoginActivity.this, getString(R.string.loading_configuration_succeded),
                                Toast.LENGTH_LONG).show();
                        slideUpBackdrop(() -> {});
                    }
                });

        if (!preferences.isOfflineMode()) {
            // reload the configuration if needed...
            getSupportLoaderManager().restartLoader(0, null, configurationLoader).forceLoad();
        } else {
            dismissProgressSpinner();
        }
    }

    private void showProgressSpinner() {
        if (progressSpinner == null) {
            progressSpinner = findViewById(R.id.progress_spinner);
        }
        if (progressSpinner != null) {
            progressSpinner.setVisibility(View.VISIBLE);
        }
    }

    private void dismissProgressSpinner() {
        if (progressSpinner != null && progressSpinner.getVisibility() == View.VISIBLE) {
            progressSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDialogNegativeButton(AttachedDialogFragment dialog) {

    }

    @Override
    public void onDialogPositiveButton(AttachedDialogFragment dialog) {

    }

    private void slideUpBackdrop(Runnable runnable) {
        final View loginView = findViewById(R.id.login_listview);
        // don't slide up if already up
        if (backdrop.getY() != 0) {
            runnable.run();
            return;
        }

        int upperRoom = backdrop.getHeight() + (backdrop.getHeight() / 5);
        View subTitle = ViewHelper.get(backdrop, R.id.backdrop_login);
        if (subTitle != null) {
            upperRoom = backdrop.getHeight() - subTitle.getHeight()
                    - getResources().getDimensionPixelSize(R.dimen.default_padding_half);
        }
        ObjectAnimator frameAnimation = ObjectAnimator.ofFloat(backdrop, "y", 0, -upperRoom);
        ValueAnimator heightAnimation = ValueAnimator.ofInt(0, upperRoom);
        heightAnimation.addUpdateListener(valueAnimator -> {
            int val = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams lpLogin = loginView.getLayoutParams();
            lpLogin.height = val;
            loginView.setLayoutParams(lpLogin);
        });

        Collection<Animator> animators = new ArrayList<>();
        animators.add(heightAnimation);
        animators.add(frameAnimation);
        animators.add(getAlphaRevAnimator(findViewById(R.id.backdrop_title)));
        animators.add(getAlphaAnimator(findViewById(R.id.gradient)));

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators);
        animatorSet.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorSetListener());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                runnable.run();
            }
        });
        animatorSet.start();
    }

    private ObjectAnimator getAlphaAnimator(@NonNull View target) {
        return ObjectAnimator.ofFloat(target, "alpha", 0f, 1f);
    }

    private ObjectAnimator getAlphaRevAnimator(@NonNull View target) {
        return ObjectAnimator.ofFloat(target, "alpha", 1f, 0f);
    }

    /**
     * Reset the data (reload from server)
     *
     * @param force Reload data, even if the backdrop is moved up
     */
    private void resetData(boolean force) {
        if (force) {
            if (loginListViews != null) {
                loginListViews.closeAll();
            }
            setupDataManager();
        }

        slideUpBackdrop(this::addEventListFragment);
    }

    private class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AppConstants.ACTION_RESET.equals(action)) {
                resetData(true);
            } else if (AppConstants.ACTION_VALID_DATA.equals(action)) {
                if (preferences.needConfigRefresh()) {
                    resetData(true);
                    preferences.setNeedConfigRefresh(false);
                } else {
                    resetData(false);
                }
            }
        }
    }

    private class AnimatorSetListener implements Animator.AnimatorListener {

        private View submit;

        private AnimatorSetListener() {
            submit = findViewById(R.id.login_submit);
        }

        @Override
        public void onAnimationStart(Animator animation) {
            setAlpha(submit, 0f);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (submit != null) {
                submit.animate().alpha(1f)
                        .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            // no op
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            setAlpha(submit, 0f);
        }

        private void setAlpha(View view, float alpha) {
            if (view != null) {
                view.setAlpha(alpha);
            }
        }
    }
}
