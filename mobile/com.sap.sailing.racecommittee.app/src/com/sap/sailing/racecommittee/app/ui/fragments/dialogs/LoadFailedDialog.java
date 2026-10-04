package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import com.sap.sailing.racecommittee.app.R;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;

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
    protected AlertDialog.Builder createDialog(AlertDialog.Builder builder) {
        return builder.setMessage(getString(R.string.generic_load_failure, getArguments().getString(ARGS_ERROR_MSG)))
                .setTitle(getString(R.string.loading_failure));
    }
}
