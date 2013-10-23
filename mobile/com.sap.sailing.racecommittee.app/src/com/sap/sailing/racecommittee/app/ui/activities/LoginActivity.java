package com.sap.sailing.racecommittee.app.ui.activities;

import java.io.Serializable;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.DeviceConfigurationIdentifier;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationIdentifierImpl;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.OnlineDataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.logging.ExLog;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.AttachedDialogFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.DialogListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.LoginDialog;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.CourseAreaListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.EventListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.CourseAreaSelectedListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.EventSelectedListenerHost;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.selection.ItemSelectedListener;
import com.sap.sailing.racecommittee.app.utils.DeviceConfigurationHelper;

public class LoginActivity extends BaseActivity implements EventSelectedListenerHost, CourseAreaSelectedListenerHost,
        DialogListenerHost {
    private final static String TAG = LoginActivity.class.getName();

    private final static String CourseAreaListFragmentTag = "CourseAreaListFragmentTag";

    private LoginDialog loginDialog;
    private CourseArea selectedCourseArea;

    public LoginActivity() {
        this.loginDialog = new LoginDialog();
        this.selectedCourseArea = null;
    }

    @Override
    protected boolean onReset() {
        Fragment courseAreaFragment = getFragmentManager().findFragmentByTag(CourseAreaListFragmentTag);
        if (courseAreaFragment != null) {
            getFragmentManager().beginTransaction().remove(courseAreaFragment).commit();
        }
        recreate();
        return true;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // features must be requested before anything else
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.login_view);
        setProgressBarIndeterminateVisibility(false);

        // on first create add event list fragment
        if (savedInstanceState == null) {
            ExLog.i(TAG, "Seems to be first start. Creating event fragment.");
            addEventListFragment();
        }
    }

    private void addEventListFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.login_view_left_container, new EventListFragment());
        transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
        transaction.commit();
    }

    private void addCourseAreaListFragment(Serializable eventId) {
        Bundle args = new Bundle();
        args.putSerializable(AppConstants.EventIdTag, eventId);

        Fragment fragment = new CourseAreaListFragment();
        fragment.setArguments(args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);
        transaction.replace(R.id.login_view_right_container, fragment, CourseAreaListFragmentTag);
        transaction.commitAllowingStateLoss();
        ExLog.i("LoginActivity", "CourseFragment created.");
    }

    private ItemSelectedListener<EventBase> eventSelectionListener = new ItemSelectedListener<EventBase>() {

        public void itemSelected(Fragment sender, EventBase event) {
            final Serializable eventId = event.getId();
            ExLog.i(ExLog.EVENT_SELECTED, eventId.toString(), getBaseContext());

            setProgressBarIndeterminateVisibility(true);
            final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage(getString(R.string.loading_configuration));
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();

            ReadonlyDataManager dataManager = OnlineDataManager.create(LoginActivity.this);
            DeviceConfigurationIdentifier identifier = new DeviceConfigurationIdentifierImpl(
                    AppPreferences.getAndroidIdentifier(getApplicationContext()));
            getLoaderManager().restartLoader(0, null,
                    dataManager.createConfigurationLoader(identifier, new LoadClient<DeviceConfiguration>() {

                        @Override
                        public void onLoadSucceded(DeviceConfiguration configuration, boolean isCached) {
                            setProgressBarIndeterminateVisibility(false);
                            progressDialog.dismiss();

                            DeviceConfigurationHelper.apply(getApplicationContext(), configuration);
                            Toast.makeText(getApplicationContext(), getString(R.string.loading_configuration_succeded),
                                    Toast.LENGTH_LONG).show();
                            showCourseAreaListFragment(eventId);
                        }

                        @Override
                        public void onLoadFailed(Exception reason) {
                            setProgressBarIndeterminateVisibility(false);
                            progressDialog.dismiss();

                            Toast.makeText(getApplicationContext(), getString(R.string.loading_configuration_failed),
                                    Toast.LENGTH_LONG).show();
                            ExLog.ex(TAG, reason);

                            showCourseAreaListFragment(eventId);
                        }
                    })).forceLoad();
        }
    };

    public ItemSelectedListener<EventBase> getEventSelectionListener() {
        return eventSelectionListener;
    }

    private void showCourseAreaListFragment(Serializable eventId) {
        addCourseAreaListFragment(eventId);
    }

    private ItemSelectedListener<CourseArea> courseAreaSelectionListener = new ItemSelectedListener<CourseArea>() {

        public void itemSelected(Fragment sender, CourseArea courseArea) {
            ExLog.i(TAG, "Starting view for " + courseArea.getName());
            ExLog.i(ExLog.COURSE_SELECTED, courseArea.getName(), getBaseContext());
            selectCourseArea(courseArea);
        }
    };

    public ItemSelectedListener<CourseArea> getCourseAreaSelectionListener() {
        return courseAreaSelectionListener;
    }

    private void selectCourseArea(CourseArea courseArea) {
        selectedCourseArea = courseArea;
        loginDialog.show(getFragmentManager(), "LoginDialog");
    }

    @Override
    public DialogResultListener getListener() {
        return new DialogResultListener() {

            @Override
            public void onDialogPositiveButton(AttachedDialogFragment dialog) {
                switch (loginDialog.getSelectedLoginType()) {
                case OFFICER:
                    ExLog.i(TAG, "Communication with backend is active.");
                    AppPreferences.setSendingActive(LoginActivity.this, true);
                    break;
                case VIEWER:
                    ExLog.i(TAG, "Communication with backend is inactive.");
                    AppPreferences.setSendingActive(LoginActivity.this, false);
                    break;
                default:
                    ExLog.i(TAG, "An invalid log type, e.g. NONE, was selected");
                    Toast.makeText(LoginActivity.this, getString(R.string.please_select_a_login_type),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (selectedCourseArea == null) {
                    Toast.makeText(LoginActivity.this, "The selected course area was lost.", Toast.LENGTH_LONG).show();
                    ExLog.e(TAG, "Course area reference was not set - cannot start racing activity.");
                    return;
                }

                Intent message = new Intent(LoginActivity.this, RacingActivity.class);
                message.putExtra(AppConstants.COURSE_AREA_UUID_KEY, selectedCourseArea.getId());
                fadeActivity(message);
            }

            @Override
            public void onDialogNegativeButton(AttachedDialogFragment dialog) {
                /* nothing here... */
            }
        };
    }

}
