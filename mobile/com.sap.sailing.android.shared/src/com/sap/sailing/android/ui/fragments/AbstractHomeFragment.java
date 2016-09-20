package com.sap.sailing.android.ui.fragments;

import java.util.List;

import com.sap.sailing.android.shared.R;
import com.sap.sailing.android.shared.data.BaseCheckinData;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.AbstractStartActivity;
import com.sap.sailing.android.shared.ui.adapters.AbstractRegattaAdapter;
import com.sap.sailing.android.shared.util.BaseAppPreferences;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public abstract class AbstractHomeFragment extends BaseFragment {
    private final static String TAG = AbstractHomeFragment.class.getName();
    protected AbstractRegattaAdapter adapter;
    protected final static int REGATTA_LOADER = 1;
    protected BaseAppPreferences prefs;
    protected int requestCodeQRCode = 442;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Button scanButton = (Button) view.findViewById(R.id.scanQr);
        if (scanButton != null) {
            scanButton.setOnClickListener(new ClickListener());
        }

        Button noQrCodeButton = (Button) view.findViewById(R.id.noQrCode);
        if (noQrCodeButton != null) {
            noQrCodeButton.setOnClickListener(new ClickListener());
        }

        return view;
    }

    private void showNoQRCodeMessage() {
        ((AbstractStartActivity) getActivity()).showErrorPopup(R.string.no_qr_code_popup_title,
                R.string.no_qr_code_popup_message);
    }

    private boolean requestQRCodeScan() {
        boolean result;
        PackageManager manager = getActivity().getPackageManager();
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        if (infos.size() != 0) {
            try {
                startActivityForResult(intent, requestCodeQRCode);
                result = true;
            } catch (Exception ex) {
                requestQRCodeScannerInstallation();
                result = false;
            }
        } else {
            requestQRCodeScannerInstallation();
            result = false;
        }
        return result;
    }

    private void requestQRCodeScannerInstallation() {
        Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
        startActivity(marketIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String scanResult = data.getStringExtra("SCAN_RESULT");
            prefs.setLastScannedQRCode(scanResult);
            // handleQRCode is called in onResume()
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(getActivity(), getString(R.string.scanning_cancelled), Toast.LENGTH_LONG).show();
        } else {
            String templateString = getString(R.string.error_scanning_qrcode);
            Toast.makeText(getActivity(), templateString.replace("{result-code}", String.valueOf(resultCode)),
                    Toast.LENGTH_LONG).show();
        }
    }

    protected void handleQRCode(String qrCode) {
        ExLog.i(getActivity(), TAG, "Parsing URI: " + qrCode);
        Uri uri = Uri.parse(qrCode);
        handleScannedOrUrlMatchedUri(uri);
    }

    public abstract void handleScannedOrUrlMatchedUri(Uri uri);

    /**
     * Display a confirmation-dialog in which the user confirms his full name and sail-id.
     *
     * @param checkinData
     */
    public abstract void displayUserConfirmationScreen(final BaseCheckinData data);

    protected void clearScannedQRCodeInPrefs() {
        prefs.setLastScannedQRCode(null);
    }

    /**
     * Shows a pop-up-dialog that informs the user than an API-call has failed and recommends a retry.
     */
    protected void displayAPIErrorRecommendRetry() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_AlertDialog);
        builder.setMessage(getString(R.string.notify_user_api_call_failed));
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }

        });
        builder.show();
    }

    private class ClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.scanQr) {
                requestQRCodeScan();
            } else if (id == R.id.noQrCode) {
                showNoQRCodeMessage();
            }
        }
    }
}
