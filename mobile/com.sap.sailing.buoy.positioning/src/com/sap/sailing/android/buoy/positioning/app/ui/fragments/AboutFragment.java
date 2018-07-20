package com.sap.sailing.android.buoy.positioning.app.ui.fragments;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.EulaHelper;
import com.sap.sailing.android.shared.util.LicenseHelper;
import com.sap.sailing.android.shared.ui.fragments.BaseFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.model.Notices;

public class AboutFragment extends BaseFragment {

    public static AboutFragment newInstance() {
        AboutFragment fragment = new AboutFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        view.findViewById(R.id.license_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLicenseDialog();
            }
        });
        view.findViewById(R.id.eula_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EulaHelper.with(getActivity()).openEulaPage();
            }
        });
        TextView versionTextView = (TextView) view.findViewById(R.id.system_information_application_version);
        versionTextView.setText(AppUtils.with(getActivity()).getBuildInfo());
        return view;
    }

    private void showLicenseDialog() {
        Notices notices = new Notices();
        LicenseHelper licenseHelper = new LicenseHelper();
        notices.addNotice(licenseHelper.getAndroidSupportNotice(getActivity()));
        notices.addNotice(licenseHelper.getOpenSansNotice());
        notices.addNotice(licenseHelper.getJsonSimpleNotice());
        notices.addNotice(licenseHelper.getDialogNotice(getActivity()));
        LicensesDialog.Builder builder = new LicensesDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.license_information));
        builder.setThemeResourceId(R.style.AppTheme_AlertDialog);
        builder.setNotices(notices);
        builder.build().show();
    }
}
