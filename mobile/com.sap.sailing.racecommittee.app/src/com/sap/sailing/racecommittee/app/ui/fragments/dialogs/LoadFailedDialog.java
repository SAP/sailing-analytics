package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import android.app.AlertDialog.Builder;
import android.os.Bundle;

import com.sap.sailing.racecommittee.app.R;

public class LoadFailedDialog extends FragmentAttachedDialogFragment {
    private static final String ARGS_ERROR_MSG = LoadFailedDialog.class.getName() + ".errorMessage";

    public static LoadFailedDialog create(String errorMessage) {
        LoadFailedDialog dialog = new LoadFailedDialog();

        Bundle args = new Bundle();
        args.putString(ARGS_ERROR_MSG, errorMessage);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    protected CharSequence getPositiveButtonLabel() {
        return getString(R.string.retry);
    }

    @Override
    protected CharSequence getNegativeButtonLabel() {
        return getString(R.string.cancel);
    }

    @Override
    protected Builder createDialog(Builder builder) {
        return builder
                .setMessage(String.format(
                        "There was an error loading the requested data:\n\"%s\"\nDo you want to retry?", 
                        getArguments().getString(ARGS_ERROR_MSG)))
                        .setTitle(getString(R.string.loading_failure))
                        .setIcon(R.drawable.ic_dialog_alert_holo_light);
    }
}
