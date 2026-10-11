package com.sap.sailing.android.tracking.app.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sap.sailing.android.shared.ui.fragments.BaseFragment;
import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.EulaHelper;
import com.sap.sailing.android.shared.util.LicenseHelper;
import com.sap.sailing.android.tracking.app.R;

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        setLink((TextView) view.findViewById(R.id.about_partnership));
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
        notices.addNotice(licenseHelper.getViewPageIndicator(getActivity()));
        notices.addNotice(licenseHelper.getDialogNotice(getActivity()));
        LicensesDialog.Builder builder = new LicensesDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.license_information));
        builder.setNotices(notices);
        builder.build().show();
    }

    private void setLink(TextView textView) {
        String message = textView.getText().toString();
        final String url = getString(R.string.about_partnership_clickable);

        SpannableString spannableString = new SpannableString(message);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        };

        spannableString.setSpan(clickableSpan, message.indexOf(url), message.indexOf(url) + url.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}