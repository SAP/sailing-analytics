package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.sap.sailing.android.shared.logging.ExLog;

public abstract class AttachedDialogFragment extends LoggableDialogFragment {
    private final static String TAG = AttachedDialogFragment.class.getName();

    protected abstract CharSequence getNegativeButtonLabel();

    protected abstract CharSequence getPositiveButtonLabel();

    protected abstract AlertDialog.Builder createDialog(AlertDialog.Builder builder);

    protected abstract DialogListenerHost getListenerHost();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return createDialog(new AlertDialog.Builder(requireContext())
                .setNegativeButton(getNegativeButtonLabel(), (dialog, which) -> onNegativeButton())
                .setPositiveButton(getPositiveButtonLabel(), (dialog, which) -> onPositiveButton()))
                .create();
    }

    protected void onNegativeButton() {
        if (getListenerHost() != null) {
            final DialogListenerHost.DialogResultListener listener = getListenerHost().getListener();
            if (listener != null) {
                listener.onDialogNegativeButton(this);
            }
        } else {
            ExLog.w(getActivity(), TAG, "Dialog host was null.");
        }
    }

    protected void onPositiveButton() {
        if (getListenerHost() != null && getListenerHost().getListener() != null) {
            final DialogListenerHost.DialogResultListener listener = getListenerHost().getListener();
            if (listener != null) {
                listener.onDialogPositiveButton(this);
            }
        } else {
            ExLog.w(getActivity(), TAG, "Dialog host was null.");
        }
    }
}
