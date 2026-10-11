package com.sap.sailing.android.shared.ui.fragments;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.sap.sailing.android.shared.R;
import com.sap.sailing.android.shared.data.BaseCheckinData;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.AbstractStartActivity;
import com.sap.sailing.android.shared.ui.activities.BarcodeCaptureActivity;
import com.sap.sailing.android.shared.ui.adapters.AbstractRegattaAdapter;
import com.sap.sailing.android.shared.util.BaseAppPreferences;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

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
        ((AbstractStartActivity<?>) getActivity()).showErrorPopup(R.string.no_qr_code_popup_title,
                R.string.no_qr_code_popup_message);
    }

    private void requestQRCodeScan() {
        Intent intent = new Intent(getContext(), BarcodeCaptureActivity.class);
        startActivityForResult(intent, requestCodeQRCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != requestCodeQRCode) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (resultCode == CommonStatusCodes.SUCCESS && data != null) {
            Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
            handleQRCode(barcode.displayValue);
        } else {
            ExLog.e(getActivity(), TAG, CommonStatusCodes.getStatusCodeString(resultCode));
        }
    }

    protected void handleQRCode(String qrCode) {
        ExLog.i(getActivity(), TAG, "Parsing URI: " + qrCode);
        Uri uri = Uri.parse(qrCode);
        // if we use the app (and not an external QR code reader) to scan a QR code which contains
        // a branch.io deeplink, we have to extract the checkin url from this deeplink here
        String checkinUrl = uri.getQueryParameter(DeviceMappingConstants.URL_CHECKIN_URL);
        if (checkinUrl != null) {
            handleScannedOrUrlMatchedUri(Uri.parse(checkinUrl));
            return;
        }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
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
