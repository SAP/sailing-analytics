package com.sap.sailing.android.shared.ui.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;

import com.sap.sailing.android.shared.R;
import com.sap.sailing.android.shared.logging.ExLog;

public abstract class AbstractBaseActivity extends SendingServiceAwareActivity {

    private static final String TAG = AbstractBaseActivity.class.getName();

    /**
     * An object used to synchronize access to the {@link #progressDialog} field to avoid concurrency
     * issues during re-assigning a new dialog.
     */
    private final Object progressDialogMonitor = new Object();
    
    private ProgressDialog progressDialog;

    public void replaceFragment(int view, Fragment fragment) {
        ExLog.i(this, TAG, "Set new Fragment: " + fragment.toString());

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(view, fragment);
        transaction.commit();
    }

    public void showProgressDialog(String title, String message) {
        synchronized (progressDialogMonitor) {
            if (progressDialog == null || !progressDialog.isShowing()) {
                progressDialog = new ProgressDialog(this);
                progressDialog.setTitle(title);
                progressDialog.setMessage(message);
                progressDialog.show();
            }
        }
    }

    public void showProgressDialog(int string1Id, int string2Id) {
        showProgressDialog(getString(string1Id), getString(string2Id));
    }

    public void dismissProgressDialog() {
        synchronized (progressDialogMonitor) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    public void showErrorPopup(String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppTheme_AlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).create();

        dialog.show();
    }

    public void showErrorPopup(int string1Id, int string2Id) {
        showErrorPopup(getString(string1Id), getString(string2Id));
    }
}
