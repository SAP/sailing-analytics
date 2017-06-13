package com.sap.sailing.racecommittee.app.ui.fragments;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import com.sap.sailing.android.shared.data.LoginData;
import com.sap.sailing.android.shared.data.http.UnauthorizedException;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.AuthCheckTask;
import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.android.shared.util.LoginTask;
import com.sap.sailing.android.shared.util.LoginTask.LoginTaskListener;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.common.impl.DeviceConfigurationQRCodeUtils;
import com.sap.sailing.domain.common.impl.DeviceConfigurationQRCodeUtils.URLDecoder;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.BackPressListener;
import com.sap.sailing.racecommittee.app.ui.activities.BaseActivity;
import com.sap.sailing.racecommittee.app.ui.activities.PreferenceActivity;
import com.sap.sailing.racecommittee.app.ui.activities.SystemInformationActivity;
import com.sap.sailing.racecommittee.app.ui.fragments.preference.GeneralPreferenceFragment;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sailing.racecommittee.app.utils.UrlHelper;
import com.sap.sailing.racecommittee.app.utils.autoupdate.AutoUpdater;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

public class LoginBackdrop extends Fragment implements BackPressListener {

    private static final String TAG = LoginBackdrop.class.getName();
    private static final int requestCodeQR = 45392;
    private static final String SHOW_BACKDROP_TEXT = "SHOW_BACKDROP_TEXT";

    private IntentReceiver receiver;
    private View login;
    private View onboarding;
    private boolean useBack;
    private String server;

    public static LoginBackdrop newInstance() {

        Bundle args = new Bundle();
        args.putBoolean(SHOW_BACKDROP_TEXT, false);
        LoginBackdrop fragment = new LoginBackdrop();
        fragment.setArguments(args);
        return fragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.login_backdrop, container, false);

        ImageView settings = ViewHelper.get(layout, R.id.settings_button);
        if (settings != null) {
            settings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSettings();
                }
            });
        }

        ImageView info = ViewHelper.get(layout, R.id.technical_info);
        if (info != null) {
            info.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openInfo();
                }
            });
        }

        ImageView refresh = ViewHelper.get(layout, R.id.refresh_data);
        if (refresh != null) {
            refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refreshData();
                }
            });
        }

        ImageView more = ViewHelper.get(layout, R.id.more);
        if (more != null) {
            more.setOnClickListener(new OverFlowButton());
        }

        setupOnboarding(layout);
        setupLogin(layout);

        receiver = new IntentReceiver();

        return layout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof BaseActivity) {
            BaseActivity baseActivity = (BaseActivity) activity;
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

        IntentFilter filter = new IntentFilter();
        filter.addAction(AppConstants.INTENT_ACTION_CHECK_LOGIN);
        filter.addAction(AppConstants.INTENT_ACTION_SHOW_LOGIN);
        filter.addAction(AppConstants.INTENT_ACTION_SHOW_ONBOARDING);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);

        if (getArguments() != null && getView() != null && !getArguments().getBoolean(SHOW_BACKDROP_TEXT, true)) {
            View view = getView().findViewById(R.id.backdrop_title);
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

    private void refreshData() {
        Intent intent = new Intent(AppConstants.INTENT_ACTION_RESET);
        intent.putExtra(AppConstants.EXTRA_FORCE_REFRESH, true);
        BroadcastManager.getInstance(getActivity()).addIntent(intent);
    }

    private void openInfo() {
        Intent intent = new Intent(getActivity(), SystemInformationActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        Intent intent = new Intent(getActivity(), PreferenceActivity.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, GeneralPreferenceFragment.class.getName());
        startActivity(intent);
    }

    private void setupOnboarding(View layout) {
        onboarding = ViewHelper.get(layout, R.id.login_onboarding);

        TextView link = ViewHelper.get(layout, R.id.get_started);
        if (link != null) {
            formatText(link);
            link.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.get_started_url)));
                    startActivity(intent);
                }
            });
        }

        Button scan = ViewHelper.get(layout, R.id.scanQr);
        if (scan != null) {
            scan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                        startActivityForResult(intent, requestCodeQR);
                    } catch (Exception e) {
                        Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                        startActivity(marketIntent);
                    }
                }
            });
        }

        Button manual = ViewHelper.get(layout, R.id.manual_input);
        if (manual != null) {
            manual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppPreferences pref = AppPreferences.on(v.getContext());
                    View view = View.inflate(v.getContext(), R.layout.login_onboarding_edit, null);
                    final EditText url = (EditText) view.findViewById(R.id.url);
                    if (TextUtils.isEmpty(pref.getServerBaseURL())) {
                        url.setText(getString(R.string.preference_server_url_default));
                    } else {
                        url.setText(pref.getServerBaseURL());
                    }
                    final EditText device_id = (EditText) view.findViewById(R.id.device_id);
                    device_id.setText(pref.getDeviceIdentifier(null));

                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext(), R.style.AppTheme_AlertDialog);
                    builder.setView(view);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (saveData(
                                url.getText().toString() + "#" + DeviceConfigurationQRCodeUtils.deviceIdentifierKey + "=" + device_id.getText()
                                    .toString())) {
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(AppConstants.INTENT_ACTION_CHECK_LOGIN));
                            }
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    AlertDialog dialog = builder.show();

                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            });
        }
    }

    private void setupLogin(View layout) {
        login = ViewHelper.get(layout, R.id.login_form);

        View change = ViewHelper.get(layout, R.id.change_server);
        if (change != null) {
            change.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    useBack = true;
                    BroadcastManager.getInstance(getActivity()).addIntent(new Intent(AppConstants.INTENT_ACTION_SHOW_ONBOARDING));
                }
            });
        }

        TextView url = ViewHelper.get(layout, R.id.server_url);
        if (url != null) {
            if ("\"SAP\"".equals(server)) {
                server = null;
            }
            if (TextUtils.isEmpty(server)) {
                server = AppPreferences.on(getActivity()).getServerBaseURL();
            }
            if (TextUtils.isEmpty(server)) {
                server = getString(R.string.not_available);
            }
            url.setText(server.replace("\"", ""));
        }

        final EditText userName = ViewHelper.get(layout, R.id.user_name);
        final EditText userPassword = ViewHelper.get(layout, R.id.user_password);
        Button login = ViewHelper.get(layout, R.id.login_request);
        if (login != null) {
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LoginTask task;
                    try {
                        task = new LoginTask(getActivity(), AppPreferences.on(getActivity()).getServerBaseURL(),
                                new LoginTaskListener() {
                                    @Override
                                    public void onResultReceived(String accessToken) {
                                        if (LoginBackdrop.this.login != null) {
                                            LoginBackdrop.this.login.setVisibility(View.GONE);
                                        }
                                        if (isAdded()) {
                                            AppPreferences.on(getActivity()).setAccessToken(accessToken);
                                            BroadcastManager.getInstance(getActivity()).addIntent(new Intent(AppConstants.INTENT_ACTION_VALID_DATA));
                                        }
                                    }
                                    
                                    @Override
                                    public void onException(Exception exception) {
                                        LoginBackdrop.this.onException(exception);
                                    }
                                });
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new LoginData(userName.getText().toString(), userPassword.getText()
                                .toString()));
                    } catch (Exception e) {
                        ExLog.e(getActivity(), TAG, "Error: Failed to perform checkin due to a MalformedURLException: " + e.getMessage());
                    }
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != requestCodeQR) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        switch (resultCode) {
            case Activity.RESULT_CANCELED:
                break;

            case Activity.RESULT_OK:
                if (saveData(data.getStringExtra("SCAN_RESULT"))) {
                    BroadcastManager.getInstance(getActivity()).addIntent(new Intent(AppConstants.INTENT_ACTION_CHECK_LOGIN));
                }
                break;

            default:
                Toast.makeText(getActivity(), getString(R.string.error_scanning_qr, resultCode), Toast.LENGTH_LONG).show();
        }
    }

    private boolean saveData(String content) {
        try {
            DeviceConfigurationQRCodeUtils.DeviceConfigurationDetails connectionConfiguration = DeviceConfigurationQRCodeUtils
                .splitQRContent(content, new URLDecoder() {
                    @Override
                    public String decode(String encodedURL) {
                        try {
                            return java.net.URLDecoder.decode(encodedURL, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            ExLog.w(getActivity(), TAG, "Couldn't resolve encoding UTF-8");
                            return encodedURL;
                        }
                    }
                });

            final String identifier = connectionConfiguration.getDeviceIdentifier();
            final URL apkUrl = UrlHelper.tryConvertToURL(connectionConfiguration.getApkUrl());
            final String accessToken = connectionConfiguration.getAccessToken();

            if (apkUrl != null) {
                String serverUrl = UrlHelper.getServerUrl(apkUrl);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.putString(getString(R.string.preference_identifier_key), identifier);
                editor.putString(getString(R.string.preference_server_url_key), serverUrl);
                editor.putString(getString(R.string.preference_access_token_key), accessToken);
                editor.commit();

                new AutoUpdater(getActivity()).checkForUpdate(false);
                return true;
            } else {
                Toast.makeText(getActivity(), getString(R.string.error_scanning_qr_malformed), Toast.LENGTH_LONG).show();
            }
        } catch (IllegalArgumentException e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void formatText(TextView textView) {
        if (textView != null) {
            SpannableString string = new SpannableString(textView.getText());
            string.setSpan(new UnderlineSpan(), 0, string.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            string.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.constant_sap_blue_1)), 0, string
                .length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(string);
        }
    }

    private void onException(Exception exception) {
        if (login != null) {
            if (login.getVisibility() == View.VISIBLE) {
                if (exception instanceof UnauthorizedException) {
                    Toast.makeText(getActivity(), R.string.wrong_credentials, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
                }
            } else {
                server = null;
                if (exception instanceof UnauthorizedException) {
                    server = exception.getMessage().split("=")[1];
                }
                BroadcastManager.getInstance(getActivity()).addIntent(new Intent(AppConstants.INTENT_ACTION_SHOW_LOGIN));
            }
        }
    }

    @Override
    public boolean handleBackPress() {
        if (useBack) {
            BroadcastManager.getInstance(getActivity()).addIntent(new Intent(AppConstants.INTENT_ACTION_SHOW_LOGIN));
            useBack = false;
            return true;
        } else {
            return false;
        }
    }

    private class OverFlowButton implements View.OnClickListener {

        //Because of massive usage of reflection (try {} catch ())
        //Don't know how to fix the warning a better way
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
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.technical_info:
                                openInfo();
                                break;

                            case R.id.settings_button:
                                openSettings();
                                break;

                            default:
                                refreshData();
                        }
                        return true;
                    }
                });
                popupMenu.show();
                ThemeHelper.positioningPopupMenu(getActivity(), popupMenu, view);
            }
        }
    }

    private class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            setVisibility(onboarding, View.GONE);
            setVisibility(login, View.GONE);
            if (AppConstants.INTENT_ACTION_CHECK_LOGIN.equals(action)) {
                AppPreferences pref = AppPreferences.on(getActivity());
                if (TextUtils.isEmpty(pref.getServerBaseURL())) {
                    BroadcastManager.getInstance(getActivity()).addIntent(new Intent(AppConstants.INTENT_ACTION_SHOW_ONBOARDING));
                } else {
                    try {
                        AuthCheckTask task = new AuthCheckTask(getActivity(), pref.getServerBaseURL(), new AuthCheckTask.AuthCheckTaskListener() {
                            @Override
                            public void onResultReceived(Boolean authenticated) {
                                if (authenticated) {
                                    BroadcastManager.getInstance(getActivity()).addIntent(new Intent(AppConstants.INTENT_ACTION_VALID_DATA));
                                } else {
                                    Toast.makeText(getActivity(), "User is not authenticated", Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onException(Exception exception) {
                                LoginBackdrop.this.onException(exception);
                            }
                        });
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } catch (MalformedURLException e) {
                        ExLog.e(getActivity(), TAG, "Error: Failed to perform check-in due to a MalformedURLException: " + e.getMessage());
                    }
                }
            } else if (AppConstants.INTENT_ACTION_SHOW_ONBOARDING.equals(action)) {
                setVisibility(onboarding, View.VISIBLE);
            } else if (AppConstants.INTENT_ACTION_SHOW_LOGIN.equals(action)) {
                setupLogin(getView());
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
