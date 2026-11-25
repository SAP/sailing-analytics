package com.sap.sailing.racecommittee.app.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.sap.sailing.android.shared.data.LoginData;
import com.sap.sailing.android.shared.data.http.UnauthorizedException;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.BarcodeCaptureActivity;
import com.sap.sailing.android.shared.util.AuthCheckTask;
import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.android.shared.util.EulaHelper;
import com.sap.sailing.android.shared.util.LoginTask;
import com.sap.sailing.android.shared.util.LoginTask.LoginTaskListener;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.common.impl.DeviceConfigurationQRCodeUtils;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.BackPressListener;
import com.sap.sailing.racecommittee.app.ui.activities.BaseActivity;
import com.sap.sailing.racecommittee.app.ui.activities.LoginActivity;
import com.sap.sailing.racecommittee.app.ui.activities.PreferenceActivity;
import com.sap.sailing.racecommittee.app.ui.activities.SystemInformationActivity;
import com.sap.sailing.racecommittee.app.utils.QRHelper;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;

import java.net.MalformedURLException;

public class LoginBackdrop extends Fragment implements BackPressListener {

    private static final String TAG = LoginBackdrop.class.getName();
    private static final String SHOW_BACKDROP_TEXT = "SHOW_BACKDROP_TEXT";

    private IntentReceiver receiver;
    private View login;
    private View onboarding;
    private AuthCheckTask.AuthCheckTaskListener mAuthCheckTaskListener;
    private LoginTaskListener loginTaskListener;

    public static LoginBackdrop newInstance() {
        final LoginBackdrop fragment = new LoginBackdrop();
        final Bundle args = new Bundle();
        args.putBoolean(SHOW_BACKDROP_TEXT, false);
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.login_backdrop, container, false);

        ImageView refresh = ViewHelper.get(layout, R.id.refresh_data);
        if (refresh != null) {
            refresh.setOnClickListener(v -> refreshData());
        }

        ImageView settings = ViewHelper.get(layout, R.id.settings_button);
        if (settings != null) {
            settings.setOnClickListener(v -> openSettings());
        }

        ImageView info = ViewHelper.get(layout, R.id.technical_info);
        if (info != null) {
            info.setOnClickListener(v -> openInfo());
        }

        ImageView more = ViewHelper.get(layout, R.id.more);
        if (more != null) {
            more.setOnClickListener(new OverFlowButton());
        }

        setupOnboarding(layout);
        setupLogin(layout);

        mAuthCheckTaskListener = new AuthCheckTask.AuthCheckTaskListener() {
            @Override
            public void onResultReceived(Boolean authenticated) {
                if (authenticated) {
                    BroadcastManager.getInstance(getActivity())
                            .addIntent(new Intent(AppConstants.ACTION_VALID_DATA));
                } else {
                    Toast.makeText(getActivity(), "User is not authenticated", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onException(Exception exception) {
                LoginBackdrop.this.onException(exception);
            }
        };
        loginTaskListener = new LoginTaskListener() {
            @Override
            public void onResultReceived(String accessToken) {
                if (LoginBackdrop.this.login != null) {
                    LoginBackdrop.this.login.setVisibility(View.GONE);
                }
                if (isAdded()) {
                    AppPreferences.on(getActivity()).setAccessToken(accessToken);
                    BroadcastManager.getInstance(getActivity())
                            .addIntent(new Intent(AppConstants.ACTION_VALID_DATA));
                }
            }

            @Override
            public void onException(Exception exception) {
                LoginBackdrop.this.onException(exception);
            }
        };
        receiver = new IntentReceiver();

        return layout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof BaseActivity) {
            BaseActivity baseActivity = (BaseActivity) context;
            baseActivity.setBackPressListener(this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (getActivity() instanceof BaseActivity) {
            BaseActivity baseActivity = (BaseActivity) getActivity();
            baseActivity.setBackPressListener(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null && getView() != null && !getArguments().getBoolean(SHOW_BACKDROP_TEXT, true)) {
            View view = getView().findViewById(R.id.backdrop_title);
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AppConstants.ACTION_CHECK_LOGIN);
        filter.addAction(AppConstants.ACTION_SHOW_LOGIN);
        filter.addAction(AppConstants.ACTION_SHOW_ONBOARDING);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver);
    }

    private void refreshData() {
        Intent intent = new Intent(AppConstants.ACTION_RESET);
        BroadcastManager.getInstance(getActivity()).addIntent(intent);
    }

    private void openInfo() {
        Intent intent = new Intent(getActivity(), SystemInformationActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        startActivity(new Intent(requireContext(), PreferenceActivity.class));
    }

    private void requestQRCodeScan() {
        Intent intent = new Intent(getActivity(), BarcodeCaptureActivity.class);
        requireActivity().startActivityForResult(intent, LoginActivity.REQUEST_CODE_QR_CODE);
    }

    private void setupOnboarding(View layout) {
        onboarding = ViewHelper.get(layout, R.id.login_onboarding);

        TextView link = ViewHelper.get(layout, R.id.get_started);
        if (link != null) {
            formatText(link);
            link.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.get_started_url)));
                startActivity(intent);
            });
        }

        Button scan = ViewHelper.get(layout, R.id.scanQr);
        if (scan != null) {
            scan.setOnClickListener(v -> requestQRCodeScan());
        }

        Button manual = ViewHelper.get(layout, R.id.manual_input);
        if (manual != null) {
            manual.setOnClickListener(v -> LocalBroadcastManager.getInstance(requireContext())
                    .sendBroadcast(new Intent(AppConstants.ACTION_SHOW_LOGIN)));
        }
    }

    private void setupLogin(View layout) {
        login = ViewHelper.get(layout, R.id.login_form);

        View change = ViewHelper.get(layout, R.id.change_server);
        if (change != null) {
            change.setOnClickListener(v -> {
                final AppPreferences pref = AppPreferences.on(v.getContext());
                final String serverUrl = pref.getServerBaseURL();
                final View view = View.inflate(v.getContext(), R.layout.login_onboarding_edit, null);
                final EditText url = view.findViewById(R.id.url);
                if (TextUtils.isEmpty(serverUrl)) {
                    url.setText(getString(R.string.preference_server_url_default));
                } else {
                    url.setText(serverUrl);
                }
                final EditText device_id = view.findViewById(R.id.device_id);
                device_id.setText(pref.getDeviceConfigurationName(null));

                new AlertDialog.Builder(v.getContext())
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            if (QRHelper.with(getActivity())
                                    .saveData(url.getText().toString() + "#"
                                            + DeviceConfigurationQRCodeUtils.deviceIdentifierKey + "="
                                            + device_id.getText().toString())) {
                                BroadcastManager.getInstance(requireContext())
                                        .addIntent(new Intent(AppConstants.ACTION_SHOW_LOGIN));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            });
        }

        TextView url = ViewHelper.get(layout, R.id.server_url);
        if (url != null) {
            final String server = AppPreferences.on(getActivity()).getServerBaseURL();
            url.setText(server);
        }

        final EditText userName = ViewHelper.get(layout, R.id.user_name);
        final EditText userPassword = ViewHelper.get(layout, R.id.user_password);
        Button login = ViewHelper.get(layout, R.id.login_request);
        if (login != null) {
            login.setOnClickListener(v -> {
                LoginTask task;
                try {
                    task = new LoginTask(getActivity(), AppPreferences.on(getActivity()).getServerBaseURL(),
                            loginTaskListener);
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                            new LoginData(userName.getText().toString(), userPassword.getText().toString()));
                } catch (Exception e) {
                    ExLog.e(getActivity(), TAG,
                            "Error: Failed to perform checkin due to a MalformedURLException: " + e.getMessage());
                }
            });
        }
    }

    private void formatText(TextView textView) {
        if (textView != null) {
            SpannableString string = new SpannableString(textView.getText());
            string.setSpan(new UnderlineSpan(), 0, string.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            string.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.constant_sap_blue_1)),
                    0, string.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(string);
        }
    }

    private void onException(Exception exception) {
        if (login != null) {
            if (login.getVisibility() == View.VISIBLE) { // login call
                if (exception instanceof UnauthorizedException) {
                    Toast.makeText(getActivity(), R.string.wrong_credentials, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
                }
            } else { // hello call
                if (exception instanceof UnauthorizedException) { // wrong credentials (access token)
                    AppPreferences.on(requireContext()).setAccessToken(null);
                    LocalBroadcastManager.getInstance(requireContext())
                            .sendBroadcast(new Intent(AppConstants.ACTION_SHOW_LOGIN));
                } else { // connection error
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle(R.string.hello_call_error_title);
                    builder.setMessage(R.string.hello_call_error_message);
                    builder.setPositiveButton(R.string.hello_call_error_positive, (dialog, which) ->
                            LocalBroadcastManager.getInstance(requireContext())
                                    .sendBroadcast(new Intent(AppConstants.ACTION_CHECK_LOGIN)));
                    builder.setNegativeButton(R.string.hello_call_error_negative, (dialog, which) ->
                            requireActivity().finish());
                    builder.setNeutralButton(R.string.hello_call_error_neutral, (dialog, which) ->
                            LocalBroadcastManager.getInstance(requireContext())
                                    .sendBroadcast(new Intent(AppConstants.ACTION_SHOW_ONBOARDING)));
                    builder.setCancelable(false);
                    builder.show();
                }
            }
        }
    }

    @Override
    public boolean handleBackPress() {
        if (login != null && login.getVisibility() == View.VISIBLE) {
            LocalBroadcastManager.getInstance(requireContext())
                    .sendBroadcast(new Intent(AppConstants.ACTION_SHOW_ONBOARDING));
            return true;
        }
        return false;
    }

    private class OverFlowButton implements View.OnClickListener {

        // Because of massive usage of reflection (try {} catch ())
        // Don't know how to fix the warning a better way
        @Override
        public void onClick(View view) {
            if (view.getVisibility() == View.VISIBLE && view.getAlpha() == 1) {
                PopupMenu popupMenu;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    popupMenu = new PopupMenu(getActivity(), view, Gravity.RIGHT);
                } else {
                    popupMenu = new PopupMenu(getActivity(), view);
                }
                popupMenu.inflate(R.menu.login_menu);
                popupMenu.setOnMenuItemClickListener(item -> {
                    final int id = item.getItemId();
                    if (id == R.id.technical_info) {
                        openInfo();
                    } else if (id == R.id.settings_button) {
                        openSettings();
                    } else {
                        refreshData();
                    }
                    return true;
                });
                popupMenu.show();
                ThemeHelper.positioningPopupMenu(getActivity(), popupMenu, view);
            }
        }
    }

    private class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (AppConstants.ACTION_CHECK_LOGIN.equals(action)) {
                final AppPreferences pref = AppPreferences.on(getActivity());
                if (TextUtils.isEmpty(pref.getAccessToken())) {
                    if (login.getVisibility() == View.GONE && EulaHelper.with(requireContext()).isEulaAccepted()) {
                        onboarding.setVisibility(View.VISIBLE);
                    }
                    return;
                }
                try {
                    new AuthCheckTask(getActivity(), pref.getServerBaseURL(), mAuthCheckTaskListener)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (MalformedURLException e) {
                    ExLog.e(getActivity(), TAG,
                            "Error: Failed to perform check-in due to a MalformedURLException: " + e.getMessage());
                }
            } else if (AppConstants.ACTION_SHOW_ONBOARDING.equals(action)) {
                setVisibility(onboarding, View.VISIBLE);
                setVisibility(login, View.GONE);
            } else if (AppConstants.ACTION_SHOW_LOGIN.equals(action)) {
                setupLogin(getView());
                setVisibility(onboarding, View.GONE);
                setVisibility(login, View.VISIBLE);
            }
        }

        private void setVisibility(View view, int visibility) {
            if (view != null) {
                view.setVisibility(visibility);
            }
        }
    }
}
